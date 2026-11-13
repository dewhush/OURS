package app.ours.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    const val MOVING_INTERVAL_MS = 30_000L
    const val STATIONARY_INTERVAL_MS = 300_000L
    const val MOVING_MIN_DISTANCE_M = 10f
    const val STATIONARY_MIN_DISTANCE_M = 25f

    const val MOVING_SPEED_THRESHOLD_MPS = 1.0
    const val SIGNIFICANT_DISPLACEMENT_M = 35.0
    const val MAX_ACCEPTABLE_ACCURACY_M = 500.0

    fun nextConfig(
        current: LocationSample,
        previous: LocationSample?,
    ): SamplingConfig {
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
