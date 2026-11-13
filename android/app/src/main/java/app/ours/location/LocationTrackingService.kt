package app.ours.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import app.ours.OursApp
import app.ours.data.api.LocationPointDto
import app.ours.notifications.OursNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var currentConfig = SamplingConfig(
        intervalMs = AdaptiveLocationPolicy.MOVING_INTERVAL_MS,
        minDistanceMeters = AdaptiveLocationPolicy.MOVING_MIN_DISTANCE_M,
        isMoving = true,
    )
    private var lastSample: LocationSample? = null
    private var registered = false

    private val listener = LocationListener { location ->
        onLocationReceived(location)
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as OursApp
        val prefs = app.prefs

        val notification = OursNotifier.buildForegroundNotification(this, prefs.sharingPaused)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                OursNotifier.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(OursNotifier.FOREGROUND_NOTIFICATION_ID, notification)
        }

        if (!prefs.isPaired || prefs.sharingPaused) {
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasLocationPermission()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        WatchdogScheduler.schedule(this)
        registerUpdates(currentConfig)

        if (intent?.action == ACTION_WAKE_FIX) {
            triggerImmediateFix()
        } else {
            scope.launch { flushQueue() }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    private fun registerUpdates(config: SamplingConfig) {
        if (registered && config == currentConfig) return
        stopLocationUpdates()
        currentConfig = config

        val providers = locationManager.allProviders
        val useFused = Build.VERSION.SDK_INT >= 31 && providers.contains(LocationManager.FUSED_PROVIDER)
        if (useFused) {
            runCatching {
                locationManager.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    config.intervalMs,
                    config.minDistanceMeters,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } else {
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        config.intervalMs,
                        config.minDistanceMeters,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
            if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        config.intervalMs,
                        config.minDistanceMeters,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        }
        registered = true
    }

    private fun stopLocationUpdates() {
        if (registered) {
            runCatching { locationManager.removeUpdates(listener) }
            registered = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerImmediateFix() {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= 31 && locationManager.allProviders.contains(LocationManager.FUSED_PROVIDER)) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        val best = providers.mapNotNull { p ->
            runCatching { locationManager.getLastKnownLocation(p) }.getOrNull()
        }.maxByOrNull { it.time }

        if (best != null) {
            onLocationReceived(best)
        } else {
            scope.launch { flushQueue() }
        }
    }

    private fun onLocationReceived(location: Location) {
        val sample = LocationSample(
            lat = location.latitude,
            lon = location.longitude,
            accuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0,
            speedMps = if (location.hasSpeed()) location.speed.toDouble().coerceAtLeast(0.0) else 0.0,
            recordedAt = if (location.time > 0) location.time else System.currentTimeMillis(),
        )
        if (!AdaptiveLocationPolicy.isValid(sample)) return

        val nextCfg = AdaptiveLocationPolicy.nextConfig(sample, lastSample)
        lastSample = sample
        if (nextCfg.intervalMs != currentConfig.intervalMs) {
            registerUpdates(nextCfg)
        }

        val (batteryPct, charging) = readBatteryState()
        val bearing = if (location.hasBearing()) {
            location.bearing.toDouble().let { if (it >= 360.0) 0.0 else it.coerceAtLeast(0.0) }
        } else {
            null
        }

        val point = LocationPointDto(
            lat = sample.lat,
            lon = sample.lon,
            accuracyM = sample.accuracyM,
            speedMps = sample.speedMps,
            bearing = bearing,
            batteryPct = batteryPct,
            charging = charging,
            recordedAt = sample.recordedAt,
        )

        val app = application as OursApp
        app.db.enqueueLocation(point)
        scope.launch {
            flushQueue()
            app.uploadManager.flushDueUploads()
        }
    }

    private suspend fun flushQueue() {
        val app = application as OursApp
        val prefs = app.prefs
        if (!prefs.isPaired || prefs.sharingPaused) return

        while (true) {
            val batch = app.db.peekLocations(limit = 100)
            if (batch.isEmpty()) break
            val cursor = prefs.eventCursor
            val result = runCatching {
                app.api.uploadLocations(batch.map { it.point }, afterEventId = cursor)
            }
            val resp = result.getOrNull() ?: break
            app.db.deleteLocations(batch.map { it.id })
            if (resp.paused) {
                prefs.sharingPaused = true
            }
            for (event in resp.events) {
                prefs.advanceCursor(event.id)
                OursNotifier.handleEvent(this, event, prefs.partnerName)
            }
            if (batch.size < 100) break
        }
    }

    private fun readBatteryState(): Pair<Int, Boolean> {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return pct to charging
    }

    companion object {
        const val ACTION_START = "app.ours.action.START_LOCATION"
        const val ACTION_WAKE_FIX = "app.ours.action.WAKE_FIX"

        fun start(context: Context, wakeFix: Boolean = false) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = if (wakeFix) ACTION_WAKE_FIX else ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }
}
