package app.ours.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
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
    canvas = Color(0xFFFAF9F5),
    surface = Color(0xFFFFFFFC),
    surfaceMuted = Color(0xFFF0EFE9),
    glassSurface = Color(0xEBFAF9F5),
    glassSurfaceStrong = Color(0xF5FFFFFC),
    glassGradientStart = Color(0xF5FFFFFC),
    glassGradientEnd = Color(0xEBFAF9F5),
    glassHighlightTopLeft = Color(0x1C000000),
    glassRimBottomRight = Color(0x14000000),
    glassInnerHighlight = Color(0xD9FFFFFF),
    glassActionTint = Color(0xFFEBF1FF),
    glassBorder = Color(0x1C000000),
    border = Color(0xFFE4E2DA),
    ink = Color(0xFF111318),
    inkSecondary = Color(0xFF373B44),
    inkMuted = Color(0xFF5A606D),
    action = Color(0xFF0055FF),
    onAction = Color(0xFFFFFFFF),
    actionSoft = Color(0xFFEBF1FF),
    live = Color(0xFF111318),
    liveSoft = Color(0xFFF0EFE9),
    paused = Color(0xFF373B44),
    pausedSoft = Color(0xFFF0EFE9),
    error = Color(0xFFD70015),
    errorSoft = Color(0xFFFFEBEA),
    danger = Color(0xFFD70015),
    dangerSoft = Color(0xFFFFEBEA),
    success = Color(0xFF1D7A3B),
    successSoft = Color(0xFFE3F6E9),
)

val OursDarkColors = OursColors(
    canvas = Color(0xFF090B10),
    surface = Color(0xFF131722),
    surfaceMuted = Color(0xFF1E2433),
    glassSurface = Color(0xEB131722),
    glassSurfaceStrong = Color(0xF51A2030),
    glassGradientStart = Color(0xF21E2433),
    glassGradientEnd = Color(0xEB131722),
    glassHighlightTopLeft = Color(0x2EFFFFFF),
    glassRimBottomRight = Color(0x1AFFFFFF),
    glassInnerHighlight = Color(0x33FFFFFF),
    glassActionTint = Color(0xFF13203B),
    glassBorder = Color(0x26FFFFFF),
    border = Color(0xFF282F42),
    ink = Color(0xFFFAF9F5),
    inkSecondary = Color(0xFFC5C8D4),
    inkMuted = Color(0xFF8E95A5),
    action = Color(0xFF4D8BFF),
    onAction = Color(0xFF060B16),
    actionSoft = Color(0xFF13203B),
    live = Color(0xFFFAF9F5),
    liveSoft = Color(0xFF1E2433),
    paused = Color(0xFFC5C8D4),
    pausedSoft = Color(0xFF1E2433),
    error = Color(0xFFFF453A),
    errorSoft = Color(0xFF2E1416),
    danger = Color(0xFFFF453A),
    dangerSoft = Color(0xFF2C1214),
    success = Color(0xFF30D158),
    successSoft = Color(0xFF112E1B),
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
 */
object OursRadius {
    val chip: Dp = 10.dp
    val inner: Dp = 12.dp
    val control: Dp = 14.dp
    val card: Dp = 20.dp
    val mediaCard: Dp = inner + OursSpacing.sm
    val sheet: Dp = 26.dp
}

object OursMotion {
    const val ENTER_MS = 240
    const val EXIT_MS = 175
    const val STAGGER_MS = 28
    const val PRESS_SCALE = 0.96f

    val EaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
    val EaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    fun <T> springSnappy(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.80f, stiffness = 520f, visibilityThreshold = visibilityThreshold)

    fun <T> springFluid(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.86f, stiffness = 360f, visibilityThreshold = visibilityThreshold)

    fun <T> springBouncy(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.62f, stiffness = 440f, visibilityThreshold = visibilityThreshold)

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
        letterSpacing = (-0.78).sp,
    )
    val headline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.6).sp,
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
