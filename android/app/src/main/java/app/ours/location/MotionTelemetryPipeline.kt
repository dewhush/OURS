package app.ours.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

data class MotionSnapshot(
    val activity: MotionActivity,
    val altitudeM: Double?,
    val verticalSpeedMps: Double?,
)

/**
 * Continuous sensor-fusion pipeline using platform SensorManager:
 * - TYPE_STEP_DETECTOR -> step cadence (walking/running/stairs)
 * - TYPE_PRESSURE -> barometric altitude & vertical speed (stairs detection)
 * - TYPE_ACCELEROMETER -> impact G-force hook for future Crash Detection
 */
class MotionTelemetryPipeline(
    context: Context,
    private val onImpactCandidate: ((gForce: Double, timestampMs: Long) -> Unit)? = null,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepTimestamps = ArrayDeque<Long>()
    private var lastPressureTimeMs: Long = 0L
    private var lastAltitudeM: Double? = null
    private var latestAltitudeM: Double? = null
    private var latestVerticalSpeedMps: Double = 0.0
    private var lastImpactTimeMs: Long = 0L
    private var lastImpactG: Double = 0.0
    private var lastCrashTriggerMs: Long = 0L
    private var started = false

    fun start() {
        if (started || sensorManager == null) return
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        started = true
    }

    fun stop() {
        if (!started || sensorManager == null) return
        sensorManager.unregisterListener(this)
        started = false
    }

    fun snapshot(speedMps: Double, nowMs: Long = System.currentTimeMillis()): MotionSnapshot {
        val cadence = synchronized(stepTimestamps) {
            while (stepTimestamps.isNotEmpty() && nowMs - stepTimestamps.first() > STEP_WINDOW_MS) {
                stepTimestamps.removeFirst()
            }
            stepTimestamps.size / (STEP_WINDOW_MS / 1000.0)
        }
        val vspeed = if (nowMs - lastPressureTimeMs <= PRESSURE_STALE_MS) latestVerticalSpeedMps else 0.0
        val activity = AdaptiveLocationPolicy.classifyActivity(
            speedMps = speedMps,
            stepCadenceHz = cadence,
            verticalSpeedMps = vspeed,
        )
        // Crash confirmation: high-G impact followed by 5-12s of stillness/low motion
        val impactAgeMs = nowMs - lastImpactTimeMs
        if (lastImpactTimeMs > 0L && impactAgeMs in CRASH_STILLNESS_MIN_MS..CRASH_STILLNESS_MAX_MS &&
            nowMs - lastCrashTriggerMs > CRASH_DEBOUNCE_MS &&
            speedMps < 1.0 && cadence < 0.2
        ) {
            lastCrashTriggerMs = nowMs
            lastImpactTimeMs = 0L
            onImpactCandidate?.invoke(lastImpactG, nowMs)
        }
        return MotionSnapshot(
            activity = activity,
            altitudeM = latestAltitudeM,
            verticalSpeedMps = if (latestAltitudeM != null) vspeed else null,
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowMs = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                synchronized(stepTimestamps) {
                    stepTimestamps.addLast(nowMs)
                    while (stepTimestamps.isNotEmpty() && nowMs - stepTimestamps.first() > STEP_WINDOW_MS) {
                        stepTimestamps.removeFirst()
                    }
                }
            }
            Sensor.TYPE_PRESSURE -> {
                val hpa = event.values.firstOrNull() ?: return
                if (!hpa.isFinite() || hpa <= 0f) return
                val altM = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hpa).toDouble()
                if (!altM.isFinite()) return
                val prevAlt = lastAltitudeM
                val dtSec = (nowMs - lastPressureTimeMs) / 1000.0
                if (prevAlt != null && dtSec in 0.5..15.0) {
                    val vspeed = (altM - prevAlt) / dtSec
                    if (vspeed.isFinite()) {
                        latestVerticalSpeedMps = vspeed
                    }
                }
                lastAltitudeM = altM
                latestAltitudeM = altM
                lastPressureTimeMs = nowMs
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val g = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                if (g.isFinite() && g >= CRASH_IMPACT_THRESHOLD_G) {
                    lastImpactTimeMs = nowMs
                    lastImpactG = g
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val STEP_WINDOW_MS = 10_000L
        private const val PRESSURE_STALE_MS = 15_000L
        private const val CRASH_IMPACT_THRESHOLD_G = 4.0
        private const val CRASH_STILLNESS_MIN_MS = 5_000L
        private const val CRASH_STILLNESS_MAX_MS = 12_000L
        private const val CRASH_DEBOUNCE_MS = 60_000L
    }
}
