package app.ours.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Low-pass smoothed compass heading provider using TYPE_ROTATION_VECTOR,
 * falling back to TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD.
 */
class CompassSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val _azimuthDeg = MutableStateFlow(0f)
    val azimuthDeg: StateFlow<Float> = _azimuthDeg.asStateFlow()

    private var hasRotationVector = false
    private val accelValues = FloatArray(3)
    private val magValues = FloatArray(3)
    private var hasAccel = false
    private var hasMag = false
    private val rotMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var smoothedSin = 0.0
    private var smoothedCos = 1.0
    private var initialized = false

    fun start() {
        val sm = sensorManager ?: return
        val rot = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rot != null) {
            hasRotationVector = true
            sm.registerListener(this, rot, SensorManager.SENSOR_DELAY_UI)
        } else {
            hasRotationVector = false
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rawDeg: Double? = when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                SensorManager.getOrientation(rotMatrix, orientation)
                Math.toDegrees(orientation[0].toDouble())
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelValues, 0, minOf(3, event.values.size))
                hasAccel = true
                if (hasMag && SensorManager.getRotationMatrix(rotMatrix, null, accelValues, magValues)) {
                    SensorManager.getOrientation(rotMatrix, orientation)
                    Math.toDegrees(orientation[0].toDouble())
                } else null
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magValues, 0, minOf(3, event.values.size))
                hasMag = true
                if (hasAccel && SensorManager.getRotationMatrix(rotMatrix, null, accelValues, magValues)) {
                    SensorManager.getOrientation(rotMatrix, orientation)
                    Math.toDegrees(orientation[0].toDouble())
                } else null
            }
            else -> null
        }

        if (rawDeg != null && rawDeg.isFinite()) {
            val rad = Math.toRadians(rawDeg)
            val alpha = 0.2
            if (!initialized) {
                smoothedSin = sin(rad)
                smoothedCos = cos(rad)
                initialized = true
            } else {
                smoothedSin = smoothedSin * (1.0 - alpha) + sin(rad) * alpha
                smoothedCos = smoothedCos * (1.0 - alpha) + cos(rad) * alpha
            }
            val deg = ((Math.toDegrees(atan2(smoothedSin, smoothedCos)) + 360.0) % 360.0).toFloat()
            _azimuthDeg.value = deg
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
