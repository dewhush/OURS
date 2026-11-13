package app.ours.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLocationPolicyTest {

    @Test
    fun `moving speed selects 30s interval`() {
        val sample = LocationSample(-6.2, 106.8, 12.0, 2.5, 1_000L)
        val cfg = AdaptiveLocationPolicy.nextConfig(sample, previous = null)
        assertTrue(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.MOVING_INTERVAL_MS, cfg.intervalMs)
        assertEquals(AdaptiveLocationPolicy.MOVING_MIN_DISTANCE_M, cfg.minDistanceMeters)
    }

    @Test
    fun `stationary speed and small displacement selects 5min interval`() {
        val prev = LocationSample(-6.20000, 106.80000, 10.0, 0.0, 1_000L)
        val cur = LocationSample(-6.20001, 106.80001, 10.0, 0.2, 60_000L)
        val cfg = AdaptiveLocationPolicy.nextConfig(cur, prev)
        assertFalse(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.STATIONARY_INTERVAL_MS, cfg.intervalMs)
        assertEquals(AdaptiveLocationPolicy.STATIONARY_MIN_DISTANCE_M, cfg.minDistanceMeters)
    }

    @Test
    fun `large displacement triggers moving interval even at zero reported speed`() {
        val prev = LocationSample(-6.2000, 106.8000, 10.0, 0.0, 1_000L)
        val cur = LocationSample(-6.2010, 106.8000, 10.0, 0.0, 60_000L) // ~111 m north
        val cfg = AdaptiveLocationPolicy.nextConfig(cur, prev)
        assertTrue(cfg.isMoving)
        assertEquals(AdaptiveLocationPolicy.MOVING_INTERVAL_MS, cfg.intervalMs)
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
