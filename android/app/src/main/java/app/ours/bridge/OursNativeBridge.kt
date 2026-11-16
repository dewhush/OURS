package app.ours.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import app.ours.BuildConfig
import app.ours.data.api.TokensDto
import app.ours.data.local.PrefsStore
import app.ours.location.LocationTrackingService
import org.json.JSONObject

class OursNativeBridge(
    private val context: Context,
    private val prefs: PrefsStore,
    private val onReloadFromServer: (String) -> Unit,
    private val onReloadUpdatingUi: () -> Unit,
    private val onHapticTick: () -> Unit,
    private val onSignInWithGoogle: () -> Unit = {},
    private val isTrustedOrigin: () -> Boolean = { false },
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var safeTopCssPx: Int = 0
    @Volatile
    private var safeBottomCssPx: Int = 0

    fun updateSafeInsets(topCssPx: Int, bottomCssPx: Int) {
        if (topCssPx >= 0) safeTopCssPx = topCssPx
        if (bottomCssPx >= 0) safeBottomCssPx = bottomCssPx
    }

    @JavascriptInterface
    fun syncAuth(json: String) {
        if (!isTrustedOrigin()) return
        runCatching {
            val obj = JSONObject(json)
            val serverUrl = obj.optString("serverUrl", "").trim().trimEnd('/')
            val currentHost = runCatching { Uri.parse(prefs.serverUrl).host?.lowercase() }.getOrNull()
            if (serverUrl.isNotBlank() && isTrustedServerUrl(
                    serverUrl,
                    allowCustomHttps = false,
                    currentConfiguredHost = currentHost,
                )
            ) {
                prefs.serverUrl = serverUrl
            }

            if (obj.has("accessToken")) {
                val access = obj.optString("accessToken", "").trim()
                val refresh = obj.optString("refreshToken", "").trim()
                if (access.isBlank()) {
                    prefs.clearSession()
                } else if (refresh.isNotBlank()) {
                    val now = System.currentTimeMillis()
                    val accessExp = obj.optLong("accessExpiresAt", now + 3600_000L)
                    val refreshExp = obj.optLong("refreshExpiresAt", now + 30L * 86400_000L)
                    prefs.saveTokens(
                        TokensDto(
                            accessToken = access,
                            accessExpiresAt = accessExp,
                            refreshToken = refresh,
                            refreshExpiresAt = refreshExp,
                        ),
                    )
                }
            }

            val userId = obj.optLong("userId", -1L)
            if (userId > 0L) {
                prefs.userId = userId
            }
            if (obj.has("myName")) {
                prefs.userName = obj.optString("myName").takeIf { it.isNotBlank() }
            }
            if (obj.has("myEmail")) {
                prefs.userEmail = obj.optString("myEmail").takeIf { it.isNotBlank() }
            }
            if (obj.has("sharingPaused")) {
                prefs.sharingPaused = obj.optBoolean("sharingPaused", false)
            }
            if (obj.has("crashDetectionEnabled")) {
                prefs.crashDetectionEnabled = obj.optBoolean("crashDetectionEnabled", true)
            }
            if (obj.has("language")) {
                prefs.language = obj.optString("language", "en")
            }
            if (obj.has("themeMode")) {
                prefs.themeMode = obj.optString("themeMode", "system")
            }

            val emergencyObj = obj.optJSONObject("emergencyContact")
            if (emergencyObj != null) {
                prefs.emergencyContactName = emergencyObj.optString("name", "")
                prefs.emergencyContactPhone = emergencyObj.optString("phone", "")
            } else if (obj.has("emergencyContact")) {
                prefs.emergencyContactPhone = obj.optString("emergencyContact", "")
            }

            if (prefs.isPaired && !prefs.sharingPaused) {
                LocationTrackingService.start(context)
            } else if (!prefs.isPaired) {
                LocationTrackingService.stop(context)
            }
        }
    }

    @SuppressLint("HardwareIds")
    @JavascriptInterface
    fun getNativeState(): String {
        val trusted = isTrustedOrigin()
        if (!trusted) {
            return "{}"
        }
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "android-${prefs.userId}"

        var lat: Double? = null
        var lon: Double? = null
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            runCatching {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                }
            }
        }

        return JSONObject().apply {
            put("serverUrl", prefs.serverUrl)
            put("accessToken", prefs.accessToken ?: "")
            put("refreshToken", prefs.refreshToken ?: "")
            put("userId", prefs.userId)
            put("deviceId", androidId)
            put("myName", prefs.userName ?: "")
            put("myEmail", prefs.userEmail ?: "")
            put("sharingPaused", prefs.sharingPaused)
            put("crashDetectionEnabled", prefs.crashDetectionEnabled)
            put("emergencyContactName", prefs.emergencyContactName)
            put("emergencyContactPhone", prefs.emergencyContactPhone)
            put("language", prefs.language)
            put("themeMode", prefs.themeMode)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("batteryPct", batteryPct)
            put("charging", charging)
            put("safeTopPx", safeTopCssPx)
            put("safeBottomPx", safeBottomCssPx)
            if (lat != null && lon != null) {
                put("lat", lat)
                put("lon", lon)
            }
        }.toString()
    }

    @JavascriptInterface
    fun setServerUrl(url: String) {
        if (!isTrustedOrigin()) return
        val cleaned = url.trim().trimEnd('/')
        val currentHost = runCatching { Uri.parse(prefs.serverUrl).host?.lowercase() }.getOrNull()
        if (cleaned.isNotBlank() && isTrustedServerUrl(
                cleaned,
                allowCustomHttps = true,
                currentConfiguredHost = currentHost,
            )
        ) {
            prefs.serverUrl = cleaned
            mainHandler.post { onReloadFromServer(prefs.serverUrl) }
        }
    }

    @JavascriptInterface
    fun reloadUpdatingUi() {
        if (!isTrustedOrigin()) return
        mainHandler.post { onReloadUpdatingUi() }
    }

    @JavascriptInterface
    fun hapticTick() {
        if (!isTrustedOrigin()) return
        mainHandler.post { onHapticTick() }
    }

    @JavascriptInterface
    fun signInWithGoogle() {
        if (!isTrustedOrigin()) return
        mainHandler.post { onSignInWithGoogle() }
    }

    companion object {
        private fun isBlockedHost(host: String): Boolean {
            if (host.contains("trycloudflare.com")) return true
            if (host == "0.0.0.0" || host.startsWith("169.254.") || host.startsWith("fe80:")) return true
            return false
        }

        fun isTrustedServerUrl(
            rawUrl: String,
            allowCustomHttps: Boolean = false,
            currentConfiguredHost: String? = null,
            isDebugBuild: Boolean = BuildConfig.DEBUG,
        ): Boolean {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank() || trimmed.length > 256) return false
            val parsed = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return false
            if (!parsed.rawUserInfo.isNullOrBlank()) return false
            val scheme = parsed.scheme?.lowercase() ?: return false
            val host = parsed.host?.lowercase() ?: return false
            if (host.isBlank() || isBlockedHost(host)) return false

            val defaultHost = runCatching { java.net.URI(BuildConfig.DEFAULT_SERVER_URL).host?.lowercase() }.getOrNull()
            return when (scheme) {
                "https" -> {
                    host == "mnemonic.my.id" ||
                        host.endsWith(".mnemonic.my.id") ||
                        (defaultHost != null && host == defaultHost) ||
                        (currentConfiguredHost != null && host == currentConfiguredHost.lowercase()) ||
                        (allowCustomHttps && host.contains('.'))
                }
                "http" -> {
                    val isLocalLoopback = host == "127.0.0.1" || host == "localhost" || host == "10.0.2.2"
                    val isLanOrTailscale = host.startsWith("192.168.") || host.startsWith("100.")
                    (isDebugBuild && isLocalLoopback) || isLanOrTailscale
                }
                else -> false
            }
        }
    }
}
