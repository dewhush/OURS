package app.ours.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.ours.BuildConfig
import app.ours.auth.FirebaseGoogleAuthManager
import app.ours.auth.FirebaseResult
import app.ours.auth.OursOidcManager
import app.ours.bridge.OursNativeBridge
import app.ours.data.local.PrefsStore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val OFFLINE_FALLBACK_URL = "file:///android_asset/web/index.html"
private const val TAG = "OursWebShell"
private const val PRODUCTION_SERVER_URL = "https://ours.mnemonic.my.id"

/** Resolved OIDC configuration used by the native Pocket ID sign-in flow. */
private data class OidcConfig(
    val androidClientId: String,
    val webClientId: String,
    val issuer: String,
    val authorizeUrl: String,
    val tokenUrl: String,
)

private fun Context.findActivity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull()

private fun isAllowedWebOrigin(rawUrl: String?, serverUrl: String): Boolean {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return false
    if (url.startsWith("file:///android_asset/")) return true
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false
    val host = uri.host?.lowercase() ?: return false
    val configuredHost = runCatching { Uri.parse(serverUrl).host?.lowercase() }.getOrNull()
    return (configuredHost != null && host == configuredHost) ||
        host == "127.0.0.1" ||
        host == "10.0.2.2" ||
        host == "mnemonic.my.id" ||
        host.endsWith(".mnemonic.my.id")
}

private fun resolveReachableServerUrl(preferred: String): String? {
    val normalizedPreferred = preferred.trim().trimEnd('/')
    val candidates = linkedSetOf(
        normalizedPreferred,
        PRODUCTION_SERVER_URL,
    ).filter {
        (it.startsWith("http://") || it.startsWith("https://")) &&
            !it.contains("127.0.0.1") &&
            !it.contains(":8080") &&
            !it.contains("trycloudflare.com")
    }

    for (base in candidates) {
        val ok = runCatching {
            val conn = (URL("$base/healthz").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                // First-load reachability probe must tolerate slow networks (mobile
                // data, cold DNS, NAT'd emulators). A 1s timeout falsely marked a
                // reachable server as offline and dropped fresh installs to the
                // offline shell. 8s matches a typical mobile network cold start.
                connectTimeout = 8000
                readTimeout = 8000
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
        if (ok) return base
    }
    return null
}

private fun fetchOidcConfig(serverUrl: String): OidcConfig {
    // Local BuildConfig wins so the native flow still works when the server is
    // unreachable or has not been upgraded to expose the oidc block yet.
    val bakedAndroidId = OursOidcManager.defaultAndroidClientId()
    val bakedWebId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    val base = serverUrl.trim().trimEnd('/')
    if (!base.startsWith("http://") && !base.startsWith("https://")) {
        return OidcConfig(bakedAndroidId, bakedWebId, "", "", "")
    }
    val remote = runCatching {
        val conn = (URL("$base/api/config").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 1500
            readTimeout = 1500
        }
        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            ""
        }
        conn.disconnect()
        if (body.isBlank()) null else JSONObject(body)
    }.getOrNull() ?: return OidcConfig(bakedAndroidId, bakedWebId, "", "", "")

    // Preferred shape: /api/config -> oidc { client_ids: {android, web}, issuer, endpoints{...} }
    val oidc = remote.optJSONObject("oidc")
    if (oidc != null) {
        val ids = oidc.optJSONObject("client_ids")
        val endpoints = oidc.optJSONObject("endpoints")
        val issuer = oidc.optString("issuer", "").trim()
        val derivedAuth = issuer.trimEnd('/').takeIf { it.isNotBlank() }?.plus("/authorize").orEmpty()
        val derivedToken = issuer.trimEnd('/').takeIf { it.isNotBlank() }?.plus("/api/oidc/token").orEmpty()
        return OidcConfig(
            androidClientId = ids?.optString("android", "")?.trim().orEmpty().ifBlank { bakedAndroidId },
            webClientId = ids?.optString("web", "")?.trim().orEmpty().ifBlank { bakedWebId },
            issuer = issuer,
            authorizeUrl = endpoints?.optString("authorization", "")?.trim().orEmpty().ifBlank { derivedAuth },
            tokenUrl = endpoints?.optString("token", "")?.trim().orEmpty().ifBlank { derivedToken },
        )
    }

    // Legacy shape: only google_client_id is published.
    val legacyWeb = remote.optString("google_client_id", "").trim()
    return OidcConfig(bakedAndroidId, legacyWeb.ifBlank { bakedWebId }, "", "", "")
}

/**
 * Resolves the Google OAuth **web** client ID used by Firebase Google Sign-In.
 * This is the client registered in the Firebase console (SHA-1 tied) — NOT the
 * Pocket ID client. Preference: server /api/config oidc.client_ids.web, then the
 * baked-in BuildConfig.GOOGLE_WEB_CLIENT_ID.
 */
private fun fetchFirebaseWebClientId(serverUrl: String): String {
    val base = serverUrl.trim().trimEnd('/')
    if (base.startsWith("http://") || base.startsWith("https://")) {
        val remote = runCatching {
            val conn = (URL("$base/api/config").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1500
                readTimeout = 1500
            }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
            conn.disconnect()
            if (body.isBlank()) null else JSONObject(body)
        }.getOrNull()
        val fromServer = remote
            ?.optJSONObject("oidc")
            ?.optJSONObject("client_ids")
            ?.optString("web", "")
            ?.trim().orEmpty()
        if (fromServer.isNotBlank()) return fromServer
    }
    return BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OursWebShellScreen(
    prefs: PrefsStore,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val useDarkCanvas = when (prefs.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val canvasComposeColor = if (useDarkCanvas) Color(0xFF090B10) else Color(0xFFFAF9F5)
    val canvasAndroidColor = if (useDarkCanvas) 0xFF090B10.toInt() else 0xFFFAF9F5.toInt()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }
    // Holds the raw Google ID token from a legacy GoogleSignInClient result,
    // waiting for a LaunchedEffect to exchange it for Firebase and dispatch.
    var pendingLegacyGoogleIdToken by remember { mutableStateOf<String?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        if (callback == null) return@rememberLauncherForActivityResult

        val uris: Array<Uri>? = when {
            result.resultCode != Activity.RESULT_OK -> null
            result.data?.clipData != null -> {
                val clip = result.data!!.clipData!!
                Array(clip.itemCount) { idx -> clip.getItemAt(idx).uri }
            }
            result.data?.data != null -> arrayOf(result.data!!.data!!)
            pendingCameraUri != null -> arrayOf(pendingCameraUri!!)
            else -> null
        }
        pendingCameraUri = null
        callback.onReceiveValue(uris)
    }

    val geoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        pendingGeoCallback?.invoke(pendingGeoOrigin ?: "", granted, false)
        pendingGeoOrigin = null
        pendingGeoCallback = null
    }

    // Legacy Google Sign-In launcher: uses GoogleSignInClient.signInIntent,
    // which renders the native account-picker via a GMS code path that does NOT
    // go through the identitycredentials process (the one that crashes with
    // DeadObjectException on this device's BadAuthentication state). This is a
    // second-chance fallback tried AFTER the Credential Manager hang-detector
    // fires, before jumping to the web PKCE path.

    fun isTrustedActivePage(wv: WebView?): Boolean =
        isAllowedWebOrigin(wv?.url, prefs.serverUrl)

    fun dispatchGoogleAuthPayload(payload: JSONObject) {
        val wv = webViewRef
        if (wv == null) {
            Log.w(TAG, "googleAuthDispatch: webViewRef is null, payload dropped")
            return
        }
        val trusted = isTrustedActivePage(wv)
        Log.i(
            TAG,
            "googleAuthDispatch: trusted=$trusted wvUrl=${wv.url} " +
                "keys=${payload.keys().asSequence().toList()}",
        )
        if (!trusted) {
            Log.w(TAG, "googleAuthDispatch: BLOCKED by isTrustedActivePage guard — payload dropped")
            return
        }
        val quoted = JSONObject.quote(payload.toString())
        wv.evaluateJavascript(
            "window.onOursGoogleAuth && window.onOursGoogleAuth(JSON.parse($quoted))",
            null,
        )
        Log.i(TAG, "googleAuthDispatch: evaluateJavascript dispatched to web layer")
    }

    /**
     * Legacy Google Sign-In via GoogleSignInClient.signInIntent.
     *
     * This uses a DIFFERENT GMS code path from Credential Manager's
     * GetGoogleIdOption / identitycredentials. It renders the classic native
     * account-picker bottom sheet and is NOT deprecated. Used as a second-chance
     * fallback when the Credential Manager path crashes (DeadObjectException on
     * BadAuthentication) on this device.
     *
     * Returns the intent to launch via an ActivityResultLauncher, or null if
     * the requested scope can't be built (e.g. webClientId blank).
     */
    fun legacyGoogleSignInIntent(activity: Activity, webClientId: String): Intent? {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val gsc = GoogleSignIn.getClient(activity, gso)
        return gsc.signInIntent
    }

    fun dispatchGoogleAuthFallback(reason: String = "") {
        val wv = webViewRef
        if (wv == null) {
            Log.w(TAG, "googleAuthFallback: webViewRef is null, reason=$reason dropped")
            return
        }
        val trusted = isTrustedActivePage(wv)
        Log.i(TAG, "googleAuthFallback: trusted=$trusted reason=$reason wvUrl=${wv.url}")
        if (!trusted) {
            Log.w(TAG, "googleAuthFallback: BLOCKED by isTrustedActivePage guard — dropped")
            return
        }
        val quoted = JSONObject.quote(reason)
        wv.evaluateJavascript(
            "window.onOursGoogleAuthFallback && window.onOursGoogleAuthFallback($quoted)",
            null,
        )
    }

    fun persistOidcIdentity(email: String, displayName: String) {
        if (email.isNotBlank()) prefs.userEmail = email
        if (displayName.isNotBlank()) prefs.userName = displayName
    }

    fun newCameraCaptureUri(): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "web_cap_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildLiveUrl(baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/?shell=android&t=${System.currentTimeMillis()}"

    // Legacy Google Sign-In launcher: uses GoogleSignInClient.signInIntent,
    // which renders the native account-picker via a GMS code path that does NOT
    // go through the identitycredentials process (the one that crashes with
    // DeadObjectException on this device's BadAuthentication state). This is a
    // second-chance fallback tried AFTER the Credential Manager hang-detector
    // fires, before jumping to the web PKCE path.
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "googleSignInLauncher: result ${result.resultCode}, user cancelled legacy sign-in")
            dispatchGoogleAuthFallback("cancelled")
            return@rememberLauncherForActivityResult
        }
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.result
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Log.w(TAG, "googleSignInLauncher: no idToken in GoogleSignInAccount, falling through")
                dispatchGoogleAuthFallback("no_credential")
                return@rememberLauncherForActivityResult
            }
            Log.i(TAG, "googleSignInLauncher: got Google ID token, queuing for Firebase exchange")
            pendingLegacyGoogleIdToken = idToken
        } catch (e: Exception) {
            Log.w(TAG, "googleSignInLauncher: exception extracting account: ${e.message}, falling through")
            dispatchGoogleAuthFallback("exception:${e.message?.take(80) ?: "unknown"}")
        }
    }

    // Process a Google ID token obtained via the legacy GoogleSignInClient path.
    // Exchanges it for a Firebase ID token (same exchange as the Credential Manager
    // path) and dispatches the result to the WebView. Triggers whenever
    // pendingLegacyGoogleIdToken changes from null to a non-blank value.
    LaunchedEffect(pendingLegacyGoogleIdToken) {
        if (pendingLegacyGoogleIdToken.isNullOrBlank()) return@LaunchedEffect
        val token = pendingLegacyGoogleIdToken
        pendingLegacyGoogleIdToken = null
        coroutineScope.launch {
            val fb = FirebaseGoogleAuthManager.firebaseSignIn(token!!)
            when (fb) {
                is FirebaseResult.Authorized -> {
                    val displayName = fb.displayName.ifBlank {
                        fb.email.substringBefore('@').ifBlank { "Member" }
                    }
                    persistOidcIdentity(fb.email, displayName)
                    dispatchGoogleAuthPayload(
                        JSONObject().apply {
                            put("email", fb.email)
                            put("name", displayName)
                            put("id_token", fb.idToken)
                            put("provider", "firebase-google")
                        },
                    )
                }
                is FirebaseResult.Error -> {
                    Log.w(TAG, "googleSignInLauncher: Firebase exchange failed: ${fb.reason}, falling through to web PKCE")
                    dispatchGoogleAuthFallback(fb.reason)
                }
            }
        }
    }

    val bridge = remember(prefs) {
        OursNativeBridge(
            context = context.applicationContext,
            prefs = prefs,
            onReloadFromServer = { url ->
                webViewRef?.apply {
                    clearCache(false)
                    loadUrl(buildLiveUrl(url))
                }
            },
            onReloadUpdatingUi = {
                webViewRef?.apply {
                    clearCache(true)
                    loadUrl(buildLiveUrl(prefs.serverUrl))
                }
            },
            onHapticTick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            },
            onSignInWithGoogle = {
                coroutineScope.launch {
                    val activity = context.findActivity()
                    if (activity == null) {
                        dispatchGoogleAuthFallback("unavailable")
                        return@launch
                    }

                    // Preferred path: Firebase Auth Google Sign-In (native Google
                    // account picker). Only available when google-services.json is
                    // present so FirebaseApp is initialised.
                    if (FirebaseGoogleAuthManager.isAvailable(activity)) {
                        val webClientId = withContext(Dispatchers.IO) {
                            fetchFirebaseWebClientId(prefs.serverUrl)
                        }
                        if (webClientId.isBlank()) {
                            dispatchGoogleAuthFallback("unavailable")
                            return@launch
                        }
                        // Race native sign-in against a hang-detector. GMS's
                        // identitycredentials process can crash silently
                        // (DeadObjectException on BadAuthentication) leaving the
                        // account picker blank and the coroutine never resuming.
                        // Heuristic: once MainActivity regains window focus after a
                        // short grace period, treat it as a GMS crash and try the
                        // legacy GoogleSignInClient recovery path before web PKCE.
                        var fb: FirebaseResult? = null
                        val signInJob = launch {
                            fb = FirebaseGoogleAuthManager.signIn(activity, webClientId)
                        }
                        val watcherJob = launch {
                            // 3s grace for the picker to appear on slow devices.
                            delay(3_000)
                            // Then wait up to 18s more, checking focus every 500ms.
                            for (tick in 1..36) {
                                if (fb != null) return@launch
                                if (activity.hasWindowFocus()) {
                                    // We're back on top but native never delivered a
                                    // credential — GMS identitycredentials crashed.
                                    Log.w(TAG, "googleAuthHang: main activity refocused with no result, assuming GMS crash")
                                    return@launch
                                }
                                delay(500)
                            }
                        }
                        // Wait for whichever resolves first.
                        while (fb == null && signInJob.isActive && watcherJob.isActive) {
                            delay(100)
                        }
                        if (fb == null) {
                            signInJob.cancel()
                            watcherJob.cancel()
                            Log.w(TAG, "googleAuthHang: Credential Manager hung — trying legacy GoogleSignInClient before web PKCE")
                            val legacyIntent = legacyGoogleSignInIntent(activity, webClientId)
                            if (legacyIntent != null) {
                                Log.i(TAG, "googleAuthHang: launching legacy GoogleSignInClient.signInIntent as second-chance fallback")
                                googleSignInLauncher.launch(legacyIntent)
                                return@launch
                            }
                            Log.w(TAG, "googleAuthHang: legacy GoogleSignInClient unavailable (no webClientId?), falling back to web PKCE")
                            dispatchGoogleAuthFallback("native_hang")
                            return@launch
                        }
                        watcherJob.cancel()
                        when (val result = fb) {
                            is FirebaseResult.Authorized -> {
                                val displayName = result.displayName.ifBlank {
                                    result.email.substringBefore('@').ifBlank { "Member" }
                                }
                                persistOidcIdentity(result.email, displayName)
                                dispatchGoogleAuthPayload(
                                    JSONObject().apply {
                                        put("email", result.email)
                                        put("name", displayName)
                                        put("id_token", result.idToken)
                                        put("provider", "firebase-google")
                                    },
                                )
                            }
                            is FirebaseResult.Error -> {
                                // "cancelled" is a normal user dismiss; surface others too.
                                // But if the error looks like a GMS/auth failure (BadAuthentication,
                                // DeadObjectException, UserRecoverableAuthException, etc.), try the
                                // legacy GoogleSignInClient path BEFORE going to web PKCE — it uses
                                // a different GMS code path that may bypass the broken identitycredentials
                                // process.
                                if (result.reason.startsWith("exception:") ||
                                    result.reason == "no_credential" ||
                                    result.reason == "bad_auth" ||
                                    result.reason == "native_hang"
                                ) {
                                    val hangReason = result.reason
                                    Log.w(
                                        TAG,
                                        "googleAuthError: auth failure '$hangReason' — trying legacy GoogleSignInClient before web PKCE",
                                    )
                                    val legacyIntent = legacyGoogleSignInIntent(activity, webClientId)
                                    if (legacyIntent != null) {
                                        Log.i(
                                            TAG,
                                            "googleAuthError: launching legacy GoogleSignInClient.signInIntent as recovery fallback",
                                        )
                                        googleSignInLauncher.launch(legacyIntent)
                                        return@launch
                                    }
                                    Log.w(
                                        TAG,
                                        "googleAuthError: legacy GoogleSignInClient unavailable, falling back to web PKCE",
                                    )
                                }
                                dispatchGoogleAuthFallback(result.reason)
                            }
                        }
                        return@launch
                    }

                    // Fallback path: self-hosted Pocket ID OIDC (no google-services.json).
                    val config = withContext(Dispatchers.IO) {
                        fetchOidcConfig(prefs.serverUrl)
                    }
                    if (config.androidClientId.isBlank()) {
                        dispatchGoogleAuthFallback("unavailable")
                        return@launch
                    }
                    try {
                        OursOidcManager.launchAuthorization(
                            context = activity,
                            clientId = config.androidClientId,
                            authorizeUrl = config.authorizeUrl.ifBlank { OursOidcManager.defaultAuthorizeUrl() },
                            tokenUrl = config.tokenUrl.ifBlank { OursOidcManager.defaultTokenUrl() },
                        )
                    } catch (_: OursOidcManager.OidcException) {
                        dispatchGoogleAuthFallback("unavailable")
                        return@launch
                    }

                    val callback = OursOidcManager.awaitCallback()
                    if (callback == null) {
                        // Browser round-trip timed out or was dismissed.
                        OursOidcManager.reset()
                        dispatchGoogleAuthFallback("cancelled")
                        return@launch
                    }

                    val outcome = withContext(Dispatchers.IO) {
                        OursOidcManager.exchangeCode(callback)
                    }
                    when (outcome) {
                        is OursOidcManager.Result.Authorized -> {
                            val displayName = outcome.name.ifBlank {
                                outcome.email.substringBefore('@').ifBlank { "Member" }
                            }
                            persistOidcIdentity(outcome.email, displayName)
                            dispatchGoogleAuthPayload(
                                JSONObject().apply {
                                    put("email", outcome.email)
                                    put("name", displayName)
                                    put("id_token", outcome.idToken)
                                    put("provider", "pocket-id")
                                },
                            )
                        }
                        is OursOidcManager.Result.Error ->
                            dispatchGoogleAuthFallback(outcome.code)
                        null ->
                            dispatchGoogleAuthFallback("unavailable")
                    }
                }
            },
            isTrustedOrigin = {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    isAllowedWebOrigin(webViewRef?.url, prefs.serverUrl)
                } else {
                    var allowed = false
                    val latch = CountDownLatch(1)
                    Handler(Looper.getMainLooper()).post {
                        allowed = isAllowedWebOrigin(webViewRef?.url, prefs.serverUrl)
                        latch.countDown()
                    }
                    latch.await(500, TimeUnit.MILLISECONDS)
                    allowed
                }
            },
        )
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasComposeColor),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(canvasAndroidColor)

                    // Remote debugging for debug builds only — lets ADB/CDP drive the
                    // WebView to test the OAuth flow deterministically. Stripped from
                    // release builds (checks the app's own DEBUGGABLE flag, which the
                    // release manifest sets to false).
                    if ((ctx.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        allowContentAccess = false
                        allowFileAccessFromFileURLs = false
                        allowUniversalAccessFromFileURLs = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }

                    addJavascriptInterface(bridge, "OursNative")

                    fun applySafeInsets(wv: WebView, insetsCompat: WindowInsetsCompat?) {
                        val bars = insetsCompat?.getInsets(
                            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
                        ) ?: return
                        val density = wv.resources.displayMetrics.density.coerceAtLeast(1f)
                        val topCss = kotlin.math.ceil(bars.top / density.toDouble()).toInt()
                        val bottomCss = kotlin.math.ceil(bars.bottom / density.toDouble()).toInt()
                        bridge.updateSafeInsets(topCss, bottomCss)
                        wv.evaluateJavascript(
                            "document.documentElement.style.setProperty('--safe-top','${topCss}px');" +
                                "document.documentElement.style.setProperty('--safe-bottom','${bottomCss}px');",
                            null,
                        )
                    }

                    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                        applySafeInsets(this, insets)
                        insets
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            if (request?.isForMainFrame != true) return false
                            val targetUri = request.url ?: return false
                            if (isAllowedWebOrigin(targetUri.toString(), prefs.serverUrl)) {
                                return false
                            }
                            // Google OAuth sign-in must stay INSIDE the WebView: the web
                            // layer's Authorization-Code+PKCE flow redirects the top-level
                            // window to accounts.google.com and back to our own origin with
                            // ?code=...&state=.... If we hand Google's host to an external
                            // browser, the callback lands in that browser instead of this
                            // WebView and handleGoogleCodeCallback() never sees the code.
                            // Only Google's sign-in/accounts hosts are permitted — never an
                            // arbitrary https URL.
                            val oauthHost = targetUri.host?.lowercase().orEmpty()
                            if (oauthHost == "accounts.google.com" ||
                                oauthHost.endsWith(".accounts.google.com") ||
                                oauthHost == "accounts.google.co.id"
                            ) {
                                return false // load in-WebView
                            }
                            val scheme = targetUri.scheme?.lowercase()
                            if (scheme == "https" || scheme == "http" || scheme == "mailto" || scheme == "tel") {
                                runCatching {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, targetUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                            return true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            canGoBack = view?.canGoBack() == true
                            view?.let { applySafeInsets(it, ViewCompat.getRootWindowInsets(it)) }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                val current = view?.url.orEmpty()
                                if (!current.startsWith("file:///android_asset/")) {
                                    view?.loadUrl(OFFLINE_FALLBACK_URL)
                                }
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?,
                        ) {
                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 500) {
                                val current = view?.url.orEmpty()
                                if (!current.startsWith("file:///android_asset/")) {
                                    view?.loadUrl(OFFLINE_FALLBACK_URL)
                                }
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?,
                        ) {
                            val hasFine = ContextCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                callback?.invoke(origin, true, false)
                            } else {
                                pendingGeoOrigin = origin
                                pendingGeoCallback = callback
                                geoPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?,
                        ): Boolean {
                            pendingFileCallback?.onReceiveValue(null)
                            pendingFileCallback = filePathCallback

                            val cameraUri = runCatching { newCameraCaptureUri() }.getOrNull()
                            pendingCameraUri = cameraUri
                            val captureIntent = if (cameraUri != null) {
                                Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                    putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            } else {
                                null
                            }

                            val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = fileChooserParams?.acceptTypes
                                    ?.firstOrNull { it.isNotBlank() }
                                    ?: "image/*"
                            }

                            val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                                putExtra(Intent.EXTRA_INTENT, contentIntent)
                                if (captureIntent != null) {
                                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
                                }
                            }

                            return runCatching {
                                fileChooserLauncher.launch(chooser)
                                true
                            }.getOrElse {
                                pendingFileCallback?.onReceiveValue(null)
                                pendingFileCallback = null
                                pendingCameraUri = null
                                false
                            }
                        }
                    }

                    webViewRef = this
                }
            },
        )
    }

    LaunchedEffect(webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        val configured = prefs.serverUrl.trim().trimEnd('/')
        if ((configured.startsWith("https://") || configured.startsWith("http://")) &&
            !configured.contains("trycloudflare.com") &&
            (BuildConfig.DEBUG || (!configured.contains("127.0.0.1") && !configured.contains(":8080")))
        ) {
            wv.loadUrl(buildLiveUrl(configured))
            return@LaunchedEffect
        }
        val reachable = withContext(Dispatchers.IO) {
            resolveReachableServerUrl(configured)
        }
        if (reachable != null) {
            prefs.serverUrl = reachable
            wv.loadUrl(buildLiveUrl(reachable))
        } else {
            wv.loadUrl(OFFLINE_FALLBACK_URL)
        }
    }
}
