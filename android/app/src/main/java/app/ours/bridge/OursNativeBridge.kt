package app.ours.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
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
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun syncAuth(json: String) {
        runCatching {
            val obj = JSONObject(json)
            val serverUrl = obj.optString("serverUrl", "").trim().trimEnd('/')
            if (serverUrl.isNotBlank()) {
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
            if (lat != null && lon != null) {
                put("lat", lat)
                put("lon", lon)
            }
        }.toString()
    }

    @JavascriptInterface
    fun setServerUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        if (cleaned.isNotBlank()) {
            prefs.serverUrl = cleaned
            mainHandler.post { onReloadFromServer(prefs.serverUrl) }
        }
    }

    @JavascriptInterface
    fun reloadUpdatingUi() {
        mainHandler.post { onReloadUpdatingUi() }
    }

    @JavascriptInterface
    fun hapticTick() {
        mainHandler.post { onHapticTick() }
    }
}
