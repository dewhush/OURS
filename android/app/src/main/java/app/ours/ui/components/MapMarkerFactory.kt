package app.ours.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

object MapMarkerFactory {

    fun partnerAvatarPin(
        context: Context,
        initial: String,
        isLive: Boolean,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (52f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val ringColor = if (isLive) Color.parseColor("#0F0F0E") else Color.parseColor("#787570")
        val fillColor = Color.parseColor("#2C2C2A")

        // Outer translucent halo
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            alpha = 45
        }
        canvas.drawCircle(cx, cy, 24f * density, haloPaint)

        // Crisp white ring
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(4f * density, 0f, 2f * density, Color.argb(60, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, 19f * density, whitePaint)

        // Status ring stroke
        val statusRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawCircle(cx, cy, 18.5f * density, statusRingPaint)

        // Terracotta core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
        }
        canvas.drawCircle(cx, cy, 15.5f * density, corePaint)

        // Initial text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f * density
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial.take(1).uppercase(), cx, textY, textPaint)

        return bmp
    }

    fun placePin(context: Context, label: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11.5f * density
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val clipped = label.take(14)
        val textW = textPaint.measureText(clipped)
        val padX = 10f * density
        val heightPx = (28f * density).toInt()
        val widthPx = (textW + padX * 2).toInt().coerceAtLeast((36f * density).toInt())

        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181615")
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

        return bmp
    }

    fun highlightPin(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (32f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, 13f * density, whitePaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F0F0E") }
        canvas.drawCircle(cx, cy, 10f * density, accentPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, 3.5f * density, dotPaint)

        return bmp
    }

    fun myLocationPin(context: Context, azimuthDeg: Float? = null): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (64f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val green = Color.parseColor("#228B54")
        if (azimuthDeg != null && azimuthDeg.isFinite()) {
            canvas.save()
            canvas.rotate(azimuthDeg, cx, cy)
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

        return bmp
    }
}
