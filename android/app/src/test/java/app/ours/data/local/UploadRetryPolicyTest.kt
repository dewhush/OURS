package app.ours.data.local

import app.ours.data.api.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UploadRetryPolicyTest {

    @Test
    fun `exponential backoff grows and caps`() {
        assertEquals(5_000L, UploadRetryPolicy.nextDelayMs(1))
        assertEquals(10_000L, UploadRetryPolicy.nextDelayMs(2))
        assertEquals(20_000L, UploadRetryPolicy.nextDelayMs(3))
        assertEquals(15 * 60_000L, UploadRetryPolicy.nextDelayMs(20))
    }

    @Test
    fun `permanent api errors give up immediately`() {
        assertTrue(UploadRetryPolicy.shouldGiveUp(1, ApiException(410, "expired", "Expired")))
        assertTrue(UploadRetryPolicy.shouldGiveUp(1, ApiException(400, "bad_request", "Bad")))
        assertTrue(UploadRetryPolicy.shouldGiveUp(1, ApiException(413, "too_large", "Large")))
    }

    @Test
    fun `transient errors retry until max attempts`() {
        val io = IOException("timeout")
        assertFalse(UploadRetryPolicy.shouldGiveUp(1, io))
        assertFalse(UploadRetryPolicy.shouldGiveUp(7, io))
        assertTrue(UploadRetryPolicy.shouldGiveUp(UploadRetryPolicy.MAX_ATTEMPTS, io))
    }
}
