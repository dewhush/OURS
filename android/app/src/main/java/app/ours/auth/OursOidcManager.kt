package app.ours.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import app.ours.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Self-hosted Pocket ID OIDC (Authorization Code + PKCE, S256, public client).
 *
 * Flow:
 *  1. [launchAuthorization] opens the authorize URL in a Custom Tab / browser.
 *  2. MainActivity forwards the `ours://callback` redirect into [handleIntent],
 *     which parks it on [pendingIntent].
 *  3. The calling coroutine awaits [pendingIntent], then calls [exchangeCode]
 *     on `Dispatchers.IO` to swap the code for an `id_token`.
 *
 * The native side only obtains the `id_token`; the existing WebView bridge
 * (`window.onOursGoogleAuth`) still POSTs it to `/api/auth/google` unchanged.
 */
object OursOidcManager {

    const val REDIRECT_URI = "ours://callback"
    const val SCOPE = "openid email profile"

    private const val ISSUER = "https://id.mnemonic.my.id"
    private const val AUTHORIZE_PATH = "/authorize"
    private const val TOKEN_PATH = "/api/oidc/token"

    /** How long we will wait for the browser round-trip before giving up. */
    private const val CALLBACK_TIMEOUT_MS = 5 * 60 * 1000L

    /** Pending `ours://callback` intent awaiting consumption by the flow. */
    private val _pendingIntent = MutableStateFlow<Intent?>(null)
    val pendingIntent: StateFlow<Intent?> = _pendingIntent.asStateFlow()

    sealed interface Result {
        data class Authorized(val idToken: String, val email: String, val name: String) : Result
        data class Error(val code: String) : Result
    }

    private var inFlight: InFlight? = null

    private data class InFlight(
        val clientId: String,
        val tokenUrl: String,
        val state: String,
        val codeVerifier: String,
    )

    val isAuthenticating: Boolean get() = inFlight != null

    /**
     * Route an incoming `ours://callback` intent (from either onCreate or
     * onNewIntent) into the flow. Returns true when the URI belongs to this
     * scheme and was captured.
     */
    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val uri = intent.data ?: return false
        if (!uri.toString().startsWith(REDIRECT_URI)) return false
        _pendingIntent.value = intent
        return true
    }

    fun reset() {
        inFlight = null
        _pendingIntent.value = null
    }

    /** Best-effort client_id for the native flow (baked-in fallback). */
    fun defaultAndroidClientId(): String = BuildConfig.OURS_ANDROID_CLIENT_ID.trim()

    /** Best-effort authorize URL when `/api/config` does not advertise one. */
    fun defaultAuthorizeUrl(): String = ISSUER + AUTHORIZE_PATH

    /** Best-effort token URL when `/api/config` does not advertise one. */
    fun defaultTokenUrl(): String = ISSUER + TOKEN_PATH

    /**
     * Begin the authorization round-trip. Must be called from the main thread.
     * @throws OidcException("unavailable") when no browser can be opened.
     */
    @Throws(OidcException::class)
    fun launchAuthorization(
        context: Context,
        clientId: String,
        authorizeUrl: String,
        tokenUrl: String,
    ) {
        val state = randomUrlSafe(16)
        val codeVerifier = randomUrlSafe(48)
        val codeChallenge = s256(codeVerifier)

        val authorize = Uri.parse(authorizeUrl).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()

        inFlight = InFlight(
            clientId = clientId,
            tokenUrl = tokenUrl,
            state = state,
            codeVerifier = codeVerifier,
        )
        _pendingIntent.value = null

        if (!openBrowser(context, authorize)) {
            inFlight = null
            throw OidcException("unavailable")
        }
    }

    /**
     * Await the browser redirect. Suspends until [handleIntent] captures an
     * `ours://callback` or [CALLBACK_TIMEOUT_MS] elapses (returns null).
     */
    suspend fun awaitCallback(): Uri? = withTimeoutOrNull(CALLBACK_TIMEOUT_MS) {
        _pendingIntent.filterNotNull().first().data
    }

    /**
     * Exchange the code for an `id_token`. Performs blocking I/O — call from
     * `Dispatchers.IO`. Returns null when no flow is active or the URI is not a
     * recognized `ours://callback`.
     */
    fun exchangeCode(uri: Uri?): Result? {
        val flow = inFlight ?: return null
        if (uri == null || !uri.toString().startsWith(REDIRECT_URI)) return null

        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            inFlight = null
            val code = if (error == "access_denied") "cancelled" else error
            return Result.Error(code)
        }

        val state = uri.getQueryParameter("state")
        if (state.isNullOrBlank() || !MessageDigest.isEqual(
                state.toByteArray(StandardCharsets.UTF_8),
                flow.state.toByteArray(StandardCharsets.UTF_8),
            )
        ) {
            inFlight = null
            return Result.Error("state_mismatch")
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            inFlight = null
            return Result.Error("missing_code")
        }

        return try {
            val idToken = requestToken(flow.tokenUrl, flow.clientId, code, flow.codeVerifier)
            val claims = decodeJwtPayload(idToken)
            inFlight = null
            Result.Authorized(
                idToken = idToken,
                email = claims.optString("email", "").trim(),
                name = claims.optString("name", "").trim(),
            )
        } catch (err: Exception) {
            inFlight = null
            Result.Error(err.message ?: "token_exchange_failed")
        }
    }

    private fun openBrowser(context: Context, uri: Uri): Boolean {
        // Prefer Chrome Custom Tabs; fall back to any browsable handler so the
        // flow still works on devices without CCT support.
        val customTab = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(customTab) }.isSuccess) return true

        val generic = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return try {
            context.startActivity(generic)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    @Throws(OidcException::class)
    private fun requestToken(
        tokenUrl: String,
        clientId: String,
        code: String,
        codeVerifier: String,
    ): String {
        val body = StringBuilder().apply {
            append("grant_type=").append(enc("authorization_code"))
            append("&code=").append(enc(code))
            append("&redirect_uri=").append(enc(REDIRECT_URI))
            append("&client_id=").append(enc(clientId))
            append("&code_verifier=").append(enc(codeVerifier))
        }.toString()

        val conn = (URL(tokenUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }
        try {
            conn.outputStream.use { out ->
                out.write(body.toByteArray(StandardCharsets.UTF_8))
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (status !in 200..299) throw OidcException("token_endpoint_$status")
            return JSONObject(text).optString("id_token", "").trim().ifBlank {
                throw OidcException("no_id_token")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Pull the base64url-decoded payload out of a JWS compact serialization. */
    fun decodeJwtPayload(jwt: String): JSONObject {
        val parts = jwt.split('.')
        if (parts.size < 2) throw OidcException("malformed_id_token")
        val bytes = Base64.decode(base64UrlSafe(parts[1]), Base64.DEFAULT)
        return JSONObject(String(bytes, StandardCharsets.UTF_8))
    }

    private fun base64UrlSafe(input: String): String {
        val swapped = input.replace('-', '+').replace('_', '/')
        return swapped + "=".repeat((4 - swapped.length % 4) % 4)
    }

    private fun s256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
            .trimEnd('=')
    }

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
            .trimEnd('=')
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    class OidcException(message: String) : Exception(message)
}
