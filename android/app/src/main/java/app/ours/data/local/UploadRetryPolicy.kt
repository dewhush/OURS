package app.ours.data.local

import app.ours.data.api.ApiException
import kotlin.math.min

object UploadRetryPolicy {
    const val MAX_ATTEMPTS = 8
    private const val BASE_DELAY_MS = 5_000L
    private const val MAX_DELAY_MS = 15 * 60_000L

    fun nextDelayMs(attemptsAfterFailure: Int): Long {
        val shift = (attemptsAfterFailure - 1).coerceIn(0, 10)
        return min(BASE_DELAY_MS shl shift, MAX_DELAY_MS)
    }

    fun isPermanentFailure(error: Throwable): Boolean {
        if (error is ApiException) {
            return error.statusCode in setOf(400, 403, 404, 410, 413)
        }
        return false
    }

    fun shouldGiveUp(attemptsAfterFailure: Int, error: Throwable): Boolean =
        isPermanentFailure(error) || attemptsAfterFailure >= MAX_ATTEMPTS
}
