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
import kotlinx.coroutines.Dispatchers
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
                connectTimeout = 1000
                readTimeout = 1000
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
                        when (val fb = FirebaseGoogleAuthManager.signIn(activity, webClientId)) {
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
                                // "cancelled" is a normal user dismiss; surface others too.
                                dispatchGoogleAuthFallback(fb.reason)
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
