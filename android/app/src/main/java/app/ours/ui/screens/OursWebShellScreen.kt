package app.ours.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.ours.bridge.OursNativeBridge
import app.ours.data.local.PrefsStore
import java.io.File

private const val OFFLINE_FALLBACK_URL = "file:///android_asset/web/index.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OursWebShellScreen(
    prefs: PrefsStore,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current

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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }

                    addJavascriptInterface(bridge, "OursNative")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            canGoBack = view?.canGoBack() == true
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

                    clearCache(false)
                    loadUrl(buildLiveUrl(prefs.serverUrl))
                    webViewRef = this
                }
            },
        )
    }
}
