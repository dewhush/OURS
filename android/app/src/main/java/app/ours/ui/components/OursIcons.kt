package app.ours.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object OursIcons {
    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private val stroke = SolidColor(Color.Black)

    val Map: ImageVector by lazy {
        icon("Map") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 6f)
                lineTo(9f, 3f)
                lineTo(15f, 6f)
                lineTo(21f, 3f)
                verticalLineTo(18f)
                lineTo(15f, 21f)
                lineTo(9f, 18f)
                lineTo(3f, 21f)
                close()
                moveTo(9f, 3f)
                verticalLineTo(18f)
                moveTo(15f, 6f)
                verticalLineTo(21f)
            }
        }
    }

    val Memories: ImageVector by lazy {
        icon("Memories") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(19f, 21f)
                lineTo(12f, 16f)
                lineTo(5f, 21f)
                verticalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 7f, 3f)
                horizontalLineTo(17f)
                arcTo(2f, 2f, 0f, false, true, 19f, 5f)
                close()
            }
        }
    }

    val Chat: ImageVector by lazy {
        icon("Chat") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21f, 11.5f)
                arcTo(8.38f, 8.38f, 0f, false, true, 20.1f, 15.3f)
                arcTo(8.5f, 8.5f, 0f, false, true, 12.5f, 20f)
                arcTo(8.38f, 8.38f, 0f, false, true, 8.7f, 19.1f)
                lineTo(3f, 21f)
                lineTo(4.9f, 15.3f)
                arcTo(8.38f, 8.38f, 0f, false, true, 4f, 11.5f)
                arcTo(8.5f, 8.5f, 0f, false, true, 8.7f, 3.9f)
                arcTo(8.38f, 8.38f, 0f, false, true, 12.5f, 3f)
                horizontalLineTo(13f)
                arcTo(8.48f, 8.48f, 0f, false, true, 21f, 11f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 15f)
                arcTo(3f, 3f, 0f, true, false, 12f, 9f)
                arcTo(3f, 3f, 0f, true, false, 12f, 15f)
                close()
                moveTo(19.4f, 15f)
                arcTo(1.65f, 1.65f, 0f, false, false, 19.73f, 16.82f)
                lineTo(19.79f, 16.88f)
                arcTo(2f, 2f, 0f, true, true, 16.96f, 19.71f)
                lineTo(16.9f, 19.65f)
                arcTo(1.65f, 1.65f, 0f, false, false, 15.08f, 19.32f)
                arcTo(1.65f, 1.65f, 0f, false, false, 14f, 20.84f)
                verticalLineTo(21f)
                arcTo(2f, 2f, 0f, true, true, 10f, 21f)
                verticalLineTo(20.91f)
                arcTo(1.65f, 1.65f, 0f, false, false, 8.92f, 19.4f)
                arcTo(1.65f, 1.65f, 0f, false, false, 7.1f, 19.73f)
                lineTo(7.04f, 19.79f)
                arcTo(2f, 2f, 0f, true, true, 4.21f, 16.96f)
                lineTo(4.27f, 16.9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 4.6f, 15.08f)
                arcTo(1.65f, 1.65f, 0f, false, false, 3.08f, 14f)
                horizontalLineTo(3f)
                arcTo(2f, 2f, 0f, true, true, 3f, 10f)
                horizontalLineTo(3.09f)
                arcTo(1.65f, 1.65f, 0f, false, false, 4.6f, 8.92f)
                arcTo(1.65f, 1.65f, 0f, false, false, 4.27f, 7.1f)
                lineTo(4.21f, 7.04f)
                arcTo(2f, 2f, 0f, true, true, 7.04f, 4.21f)
                lineTo(7.1f, 4.27f)
                arcTo(1.65f, 1.65f, 0f, false, false, 8.92f, 4.6f)
                horizontalLineTo(9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 10f, 3.09f)
                verticalLineTo(3f)
                arcTo(2f, 2f, 0f, true, true, 14f, 3f)
                verticalLineTo(3.09f)
                arcTo(1.65f, 1.65f, 0f, false, false, 15.08f, 4.6f)
                arcTo(1.65f, 1.65f, 0f, false, false, 16.9f, 4.27f)
                lineTo(16.96f, 4.21f)
                arcTo(2f, 2f, 0f, true, true, 19.79f, 7.04f)
                lineTo(19.73f, 7.1f)
                arcTo(1.65f, 1.65f, 0f, false, false, 19.4f, 8.92f)
                verticalLineTo(9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 20.91f, 10f)
                horizontalLineTo(21f)
                arcTo(2f, 2f, 0f, true, true, 21f, 14f)
                horizontalLineTo(20.91f)
                arcTo(1.65f, 1.65f, 0f, false, false, 19.4f, 15f)
                close()
            }
        }
    }

    val Camera: ImageVector by lazy {
        icon("Camera") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(23f, 19f)
                arcTo(2f, 2f, 0f, false, true, 21f, 21f)
                horizontalLineTo(3f)
                arcTo(2f, 2f, 0f, false, true, 1f, 19f)
                verticalLineTo(8f)
                arcTo(2f, 2f, 0f, false, true, 3f, 6f)
                horizontalLineTo(7f)
                lineTo(9f, 3f)
                horizontalLineTo(15f)
                lineTo(17f, 6f)
                horizontalLineTo(21f)
                arcTo(2f, 2f, 0f, false, true, 23f, 8f)
                close()
                moveTo(12f, 17f)
                arcTo(4f, 4f, 0f, true, false, 12f, 9f)
                arcTo(4f, 4f, 0f, true, false, 12f, 17f)
                close()
            }
        }
    }

    val Send: ImageVector by lazy {
        icon("Send") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(22f, 2f)
                lineTo(11f, 13f)
                moveTo(22f, 2f)
                lineTo(15f, 22f)
                lineTo(11f, 13f)
                lineTo(2f, 9f)
                close()
            }
        }
    }

    val Heart: ImageVector by lazy {
        icon("Heart") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20.84f, 4.61f)
                arcTo(5.5f, 5.5f, 0f, false, false, 13.06f, 4.61f)
                lineTo(12f, 5.67f)
                lineTo(10.94f, 4.61f)
                arcTo(5.5f, 5.5f, 0f, false, false, 3.16f, 12.39f)
                lineTo(4.22f, 13.45f)
                lineTo(12f, 21.23f)
                lineTo(19.78f, 13.45f)
                lineTo(20.84f, 12.39f)
                arcTo(5.5f, 5.5f, 0f, false, false, 20.84f, 4.61f)
                close()
            }
        }
    }

    val Crosshair: ImageVector by lazy {
        icon("Crosshair") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 20f)
                arcTo(8f, 8f, 0f, true, false, 12f, 4f)
                arcTo(8f, 8f, 0f, true, false, 12f, 20f)
                close()
                moveTo(12f, 2f)
                verticalLineTo(6f)
                moveTo(12f, 18f)
                verticalLineTo(22f)
                moveTo(2f, 12f)
                horizontalLineTo(6f)
                moveTo(18f, 12f)
                horizontalLineTo(22f)
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }
    }

    val Trash: ImageVector by lazy {
        icon("Trash") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                moveTo(19f, 6f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, false, true, 17f, 22f)
                horizontalLineTo(7f)
                arcTo(2f, 2f, 0f, false, true, 5f, 20f)
                verticalLineTo(6f)
                moveTo(8f, 6f)
                verticalLineTo(4f)
                arcTo(2f, 2f, 0f, false, true, 10f, 2f)
                horizontalLineTo(14f)
                arcTo(2f, 2f, 0f, false, true, 16f, 4f)
                verticalLineTo(6f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }
}
