package app.ours.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    fun relativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffSec = ((nowMs - timestampMs) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 15 -> "Just now"
            diffSec < 60 -> "${diffSec}s ago"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            diffSec < 86_400 -> "${diffSec / 3600}h ago"
            else -> "${diffSec / 86_400}d ago"
        }
    }

    fun countdown24h(expiresAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val remMs = (expiresAtMs - nowMs).coerceAtLeast(0L)
        val totalMinutes = remMs / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return String.format(Locale.US, "%02dh %02dm left", hours, minutes)
    }

    fun speedKmh(speedMps: Double?): String {
        if (speedMps == null || speedMps < 0.3) return "0 km/h"
        val kmh = (speedMps * 3.6).roundToInt()
        return "$kmh km/h"
    }

    fun dateShort(timestampMs: Long): String {
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        return fmt.format(Date(timestampMs))
    }
}
