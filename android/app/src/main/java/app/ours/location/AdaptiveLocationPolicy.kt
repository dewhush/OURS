package app.ours.location

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MotionActivity(val wireName: String) {
    STILL("still"),
    WALKING("walking"),
    RUNNING("running"),
    CYCLING("cycling"),
    DRIVING("driving"),
    STAIRS("stairs"),
}

data class SamplingConfig(
    val intervalMs: Long,
    val minDistanceMeters: Float,
    val isMoving: Boolean,
)

data class LocationSample(
    val lat: Double,
    val lon: Double,
    val accuracyM: Double,
    val speedMps: Double,
    val recordedAt: Long,
)

object AdaptiveLocationPolicy {
    const val MOVING_INTERVAL_MS = 5_000L
    const val STATIONARY_INTERVAL_MS = 180_000L
    const val MOVING_MIN_DISTANCE_M = 5f
    const val STATIONARY_MIN_DISTANCE_M = 20f

    const val MOVING_SPEED_THRESHOLD_MPS = 0.9
    const val SIGNIFICANT_DISPLACEMENT_M = 25.0
    const val MAX_ACCEPTABLE_ACCURACY_M = 500.0
    const val DRIFT_ACCURACY_THRESHOLD_M = 25.0
    const val DRIFT_MAX_SPEED_MPS = 1.2

    fun nextConfig(
        current: LocationSample,
        previous: LocationSample?,
    ): SamplingConfig {
        if (isStationaryDrift(current, previous)) {
            return SamplingConfig(
                intervalMs = STATIONARY_INTERVAL_MS,
                minDistanceMeters = STATIONARY_MIN_DISTANCE_M,
                isMoving = false,
            )
        }
        val movedBySpeed = current.speedMps >= MOVING_SPEED_THRESHOLD_MPS
        val movedByDistance = previous != null &&
            distanceMeters(previous.lat, previous.lon, current.lat, current.lon) >= SIGNIFICANT_DISPLACEMENT_M
        val isMoving = movedBySpeed || movedByDistance
        return if (isMoving) {
            SamplingConfig(
                intervalMs = MOVING_INTERVAL_MS,
                minDistanceMeters = MOVING_MIN_DISTANCE_M,
                isMoving = true,
            )
        } else {
            SamplingConfig(
                intervalMs = STATIONARY_INTERVAL_MS,
                minDistanceMeters = STATIONARY_MIN_DISTANCE_M,
                isMoving = false,
            )
        }
    }

    /**
     * Rejects noisy indoor/stationary GPS drift when accuracy is poor and displacement
     * falls well inside the accuracy radius at low reported speed.
     */
    fun isStationaryDrift(
        current: LocationSample,
        previous: LocationSample?,
    ): Boolean {
        if (previous == null) return false
        if (current.accuracyM <= DRIFT_ACCURACY_THRESHOLD_M) return false
        if (current.speedMps >= DRIFT_MAX_SPEED_MPS) return false
        val disp = distanceMeters(previous.lat, previous.lon, current.lat, current.lon)
        return disp < current.accuracyM * 0.8
    }

    fun classifyActivity(
        speedMps: Double,
        stepCadenceHz: Double = 0.0,
        verticalSpeedMps: Double = 0.0,
    ): MotionActivity {
        if (speedMps >= 8.5) return MotionActivity.DRIVING
        if (abs(verticalSpeedMps) >= 0.25 && speedMps < 4.5 && (stepCadenceHz >= 0.4 || speedMps >= 0.3)) {
            return MotionActivity.STAIRS
        }
        if (speedMps >= 4.5 && stepCadenceHz < 1.2) return MotionActivity.CYCLING
        if (speedMps >= 2.6 || stepCadenceHz >= 2.4) return MotionActivity.RUNNING
        if (speedMps >= 0.6 || stepCadenceHz >= 0.5) return MotionActivity.WALKING
        return MotionActivity.STILL
    }

    fun isValid(sample: LocationSample): Boolean {
        if (sample.lat !in -90.0..90.0) return false
        if (sample.lon !in -180.0..180.0) return false
        if (sample.accuracyM < 0.0 || sample.accuracyM > MAX_ACCEPTABLE_ACCURACY_M) return false
        if (sample.speedMps < 0.0) return false
        return true
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
