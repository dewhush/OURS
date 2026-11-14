package app.ours.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class OursColors(
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassGradientStart: Color,
    val glassGradientEnd: Color,
    val glassHighlightTopLeft: Color,
    val glassRimBottomRight: Color,
    val glassInnerHighlight: Color,
    val glassActionTint: Color,
    val glassBorder: Color,
    val border: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val action: Color,
    val onAction: Color,
    val actionSoft: Color,
    val live: Color,
    val liveSoft: Color,
    val paused: Color,
    val pausedSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val successSoft: Color,
)

val OursLightColors = OursColors(
    canvas = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEBEEF3),
    glassSurface = Color(0xE6FFFFFF),
    glassSurfaceStrong = Color(0xF2F7F9FC),
    glassGradientStart = Color(0xF7FFFFFF),
    glassGradientEnd = Color(0xD9EDF2F8),
    glassHighlightTopLeft = Color(0xEEFFFFFF),
    glassRimBottomRight = Color(0x59CBD5E1),
    glassInnerHighlight = Color(0xE6FFFFFF),
    glassActionTint = Color(0x1F005EC4),
    glassBorder = Color(0xCCFFFFFF),
    border = Color(0xFFDCE2EC),
    ink = Color(0xFF0F141C),
    inkSecondary = Color(0xFF3E4856),
    inkMuted = Color(0xFF525D6D),
    action = Color(0xFF005EC4),
    onAction = Color(0xFFFFFFFF),
    actionSoft = Color(0xFFE6F0FC),
    live = Color(0xFF151B24),
    liveSoft = Color(0xFFE6EBF2),
    paused = Color(0xFF4B5563),
    pausedSoft = Color(0xFFEBEEF3),
    error = Color(0xFF1F2937),
    errorSoft = Color(0xFFE5E9F0),
    danger = Color(0xFFB83232),
    dangerSoft = Color(0xFFFCECEC),
    success = Color(0xFF196F43),
    successSoft = Color(0xFFE8F5EE),
)

val OursDarkColors = OursColors(
    canvas = Color(0xFF0B0E14),
    surface = Color(0xFF131822),
    surfaceMuted = Color(0xFF1C2330),
    glassSurface = Color(0xCC161C26),
    glassSurfaceStrong = Color(0xE6181F2B),
    glassGradientStart = Color(0xEB222A38),
    glassGradientEnd = Color(0xCC121721),
    glassHighlightTopLeft = Color(0x66FFFFFF),
    glassRimBottomRight = Color(0x1FFFFFFF),
    glassInnerHighlight = Color(0x33FFFFFF),
    glassActionTint = Color(0x2E5EA8FF),
    glassBorder = Color(0x29FFFFFF),
    border = Color(0xFF262F3F),
    ink = Color(0xFFF5F7FA),
    inkSecondary = Color(0xFFB8C2D0),
    inkMuted = Color(0xFF8E99AA),
    action = Color(0xFF5EA8FF),
    onAction = Color(0xFF08101C),
    actionSoft = Color(0xFF122742),
    live = Color(0xFFF5F7FA),
    liveSoft = Color(0xFF1E2532),
    paused = Color(0xFF9CA3AF),
    pausedSoft = Color(0xFF1C2330),
    error = Color(0xFFE2E8F0),
    errorSoft = Color(0xFF242C3A),
    danger = Color(0xFFE85D5D),
    dangerSoft = Color(0xFF2E1616),
    success = Color(0xFF45C486),
    successSoft = Color(0xFF142E21),
)

object OursSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
}

/**
 * Nested radius rule: outer = inner + padding.
 * Example: a card with 12.dp padding around a 12.dp photo uses 24.dp outer radius.
 */
object OursRadius {
    val chip: Dp = 10.dp
    val inner: Dp = 12.dp
    val control: Dp = 14.dp
    val card: Dp = 20.dp
    val mediaCard: Dp = inner + OursSpacing.sm // 24.dp
    val sheet: Dp = 24.dp
}

object OursMotion {
    const val ENTER_MS = 220
    const val EXIT_MS = 160 // ~27% shorter than entrances
    const val STAGGER_MS = 30
    const val PRESS_SCALE = 0.97f

    // Apple fluid cubic-bezier(0.22, 1, 0.36, 1)
    val EaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
    val EaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    fun <T> enterSpec(durationMs: Int = ENTER_MS) =
        tween<T>(durationMillis = durationMs, easing = EaseOut)

    fun <T> exitSpec(durationMs: Int = EXIT_MS) =
        tween<T>(durationMillis = durationMs, easing = EaseOut)

    fun <T> moveSpec(durationMs: Int = ENTER_MS) =
        tween<T>(durationMillis = durationMs, easing = EaseInOut)
}

object OursTypography {
    val display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.78).sp, // -0.03em Pentagram display tracking
    )
    val headline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.6).sp, // -0.03em
    )
    val title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    )
    val caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val tabular = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    )
    val tabularLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
    )
}

val LocalOursColors = staticCompositionLocalOf { OursLightColors }
