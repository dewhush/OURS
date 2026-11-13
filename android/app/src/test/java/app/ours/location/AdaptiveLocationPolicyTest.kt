package app.ours.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLocationPolicyTest {

    @Test
    fun `moving speed selects 5s interval`() {
        val sample = LocationSample(-6.2, 106.8, 12.0, 2.5, 1_000L)
        val cfg = AdaptiveLocationPolicy.nextConfig(sample, previous = null)
        assertTrue(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.MOVING_INTERVAL_MS, cfg.intervalMs)
        assertEquals(AdaptiveLocationPolicy.MOVING_MIN_DISTANCE_M, cfg.minDistanceMeters)
    }

    @Test
    fun `stationary speed and small displacement selects 3min interval`() {
        val prev = LocationSample(-6.20000, 106.80000, 10.0, 0.0, 1_000L)
        val cur = LocationSample(-6.20001, 106.80001, 10.0, 0.2, 60_000L)
        val cfg = AdaptiveLocationPolicy.nextConfig(cur, prev)
        assertFalse(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.STATIONARY_INTERVAL_MS, cfg.intervalMs)
        assertEquals(AdaptiveLocationPolicy.STATIONARY_MIN_DISTANCE_M, cfg.minDistanceMeters)
    }

    @Test
    fun `large displacement with good accuracy triggers moving interval`() {
        val prev = LocationSample(-6.2000, 106.8000, 10.0, 0.0, 1_000L)
        val cur = LocationSample(-6.2010, 106.8000, 10.0, 0.0, 60_000L) // ~111 m north
        val cfg = AdaptiveLocationPolicy.nextConfig(cur, prev)
        assertTrue(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.MOVING_INTERVAL_MS, cfg.intervalMs)
    }

    @Test
    fun `noisy indoor GPS drift inside accuracy radius stays stationary`() {
        val prev = LocationSample(-6.2000, 106.8000, 10.0, 0.0, 1_000L)
        val cur = LocationSample(-6.2003, 106.8000, 80.0, 1.0, 15_000L) // ~33 m jitter, 80 m accuracy
        assertTrue(AdaptiveLocationPolicy.isStationaryDrift(cur, prev))
        val cfg = AdaptiveLocationPolicy.nextConfig(cur, prev)
        assertFalse(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.STATIONARY_INTERVAL_MS, cfg.intervalMs)
    }

    @Test
    fun `activity classification handles still walking stairs cycling and driving`() {
        assertEquals(MotionActivity.STILL, AdaptiveLocationPolicy.classifyActivity(0.1, 0.0, 0.0))
        assertEquals(MotionActivity.WALKING, AdaptiveLocationPolicy.classifyActivity(1.3, 1.6, 0.0))
        assertEquals(MotionActivity.STAIRS, AdaptiveLocationPolicy.classifyActivity(0.9, 1.4, 0.35))
        assertEquals(MotionActivity.CYCLING, AdaptiveLocationPolicy.classifyActivity(5.5, 0.0, 0.0))
        assertEquals(MotionActivity.DRIVING, AdaptiveLocationPolicy.classifyActivity(14.5, 0.0, 0.0))
    }

    @Test
    fun `validation rejects out of range coordinates and poor accuracy`() {
        assertFalse(AdaptiveLocationPolicy.isValid(LocationSample(95.0, 10.0, 10.0, 0.0, 1L)))
        assertFalse(AdaptiveLocationPolicy.isValid(LocationSample(0.0, 190.0, 10.0, 0.0, 1L)))
        assertFalse(AdaptiveLocationPolicy.isValid(LocationSample(0.0, 0.0, 900.0, 0.0, 1L)))
        assertFalse(AdaptiveLocationPolicy.isValid(LocationSample(0.0, 0.0, 10.0, -1.0, 1L)))
        assertTrue(AdaptiveLocationPolicy.isValid(LocationSample(-6.2, 106.8, 15.0, 1.2, 1L)))
    }
}
