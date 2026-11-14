package app.ours.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import kotlin.math.roundToInt

object MapMarkerFactory {

    private val pinCache = LruCache<String, Bitmap>(24)

    fun partnerAvatarPin(
        context: Context,
        initial: String,
        isLive: Boolean,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val letter = initial.take(1).uppercase()
        val key = "partner_${letter}_${isLive}_${density.roundToInt()}"
        pinCache.get(key)?.let { if (!it.isRecycled) return it }

        val sizePx = (52f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val ringColor = if (isLive) Color.parseColor("#1D7A3B") else Color.parseColor("#68686E")
        val fillColor = Color.parseColor("#0055FF")

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            alpha = 45
        }
        canvas.drawCircle(cx, cy, 24f * density, haloPaint)

        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(4f * density, 0f, 2f * density, Color.argb(60, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, 19f * density, whitePaint)

        val statusRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawCircle(cx, cy, 18.5f * density, statusRingPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
        }
        canvas.drawCircle(cx, cy, 15.5f * density, corePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f * density
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, cx, textY, textPaint)

        pinCache.put(key, bmp)
        return bmp
    }

    fun placePin(context: Context, label: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val clipped = label.take(14)
        val key = "place_${clipped}_${density.roundToInt()}"
        pinCache.get(key)?.let { if (!it.isRecycled) return it }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11.5f * density
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textW = textPaint.measureText(clipped)
        val padX = 10f * density
        val heightPx = (28f * density).toInt()
        val widthPx = (textW + padX * 2).toInt().coerceAtLeast((36f * density).toInt())

        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1E")
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        val rect = RectF(1.5f * density, 1.5f * density, widthPx - 1.5f * density, heightPx - 1.5f * density)
        canvas.drawRoundRect(rect, 12f * density, 12f * density, bgPaint)
        canvas.drawRoundRect(rect, 12f * density, 12f * density, borderPaint)

        val textY = heightPx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(clipped, padX, textY, textPaint)

        pinCache.put(key, bmp)
        return bmp
    }

    fun highlightPin(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val key = "highlight_${density.roundToInt()}"
        pinCache.get(key)?.let { if (!it.isRecycled) return it }

        val sizePx = (32f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, 13f * density, whitePaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0055FF") }
        canvas.drawCircle(cx, cy, 10f * density, accentPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, 3.5f * density, dotPaint)

        pinCache.put(key, bmp)
        return bmp
    }

    fun myLocationPin(context: Context, azimuthDeg: Float? = null): Bitmap {
        val density = context.resources.displayMetrics.density
        val bucketDeg = azimuthDeg?.takeIf { it.isFinite() }?.let { ((it / 5f).roundToInt() * 5) % 360 }
        val key = "me_${bucketDeg ?: "none"}_${density.roundToInt()}"
        pinCache.get(key)?.let { if (!it.isRecycled) return it }

        val sizePx = (64f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val green = Color.parseColor("#1D7A3B")
        if (bucketDeg != null) {
            canvas.save()
            canvas.rotate(bucketDeg.toFloat(), cx, cy)
            val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = green
                alpha = 65
            }
            val r = 28f * density
            val oval = RectF(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(oval, -120f, 60f, true, conePaint)
            canvas.restore()
        }

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green
            alpha = 45
        }
        canvas.drawCircle(cx, cy, 18f * density, haloPaint)

        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(3f * density, 0f, 1.5f * density, Color.argb(60, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, 11f * density, whitePaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green
        }
        canvas.drawCircle(cx, cy, 7.5f * density, corePaint)

        pinCache.put(key, bmp)
        return bmp
    }
}
