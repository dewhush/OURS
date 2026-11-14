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
    canvas = Color(0xFFFAF8F4),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF2EFEA),
    glassSurface = Color(0xE0FFFFFF),
    glassSurfaceStrong = Color(0xF0FAF8F4),
    glassActionTint = Color(0x1ED4623F),
    glassBorder = Color(0xB3FFFFFF),
    border = Color(0xFFE6E0D7),
    ink = Color(0xFF0F0F0E),
    inkSecondary = Color(0xFF4A4844),
    inkMuted = Color(0xFF787570),
    action = Color(0xFFD4623F),
    onAction = Color(0xFFFAF8F4),
    actionSoft = Color(0xFFFBECE6),
    live = Color(0xFF1F1F1D),
    liveSoft = Color(0xFFECEAE4),
    paused = Color(0xFF5A5853),
    pausedSoft = Color(0xFFF0EDE7),
    error = Color(0xFF2C2C2A),
    errorSoft = Color(0xFFE9E6E0),
    danger = Color(0xFFC63B3B),
    dangerSoft = Color(0xFFFCECEC),
    success = Color(0xFF228B54),
    successSoft = Color(0xFFE8F5EE),
)

val OursDarkColors = OursColors(
    canvas = Color(0xFF0F0F0E),
    surface = Color(0xFF181816),
    surfaceMuted = Color(0xFF232320),
    glassSurface = Color(0xCC1C1C19),
    glassSurfaceStrong = Color(0xE61C1C19),
    glassActionTint = Color(0x2EE67B58),
    glassBorder = Color(0x26FFFFFF),
    border = Color(0xFF2E2E2B),
    ink = Color(0xFFFAF8F4),
    inkSecondary = Color(0xFFBFBDB8),
    inkMuted = Color(0xFF8A8882),
    action = Color(0xFFE67B58),
    onAction = Color(0xFF0F0F0E),
    actionSoft = Color(0xFF332019),
    live = Color(0xFFFAF8F4),
    liveSoft = Color(0xFF252522),
    paused = Color(0xFFA8A6A0),
    pausedSoft = Color(0xFF242421),
    error = Color(0xFFE5E3DD),
    errorSoft = Color(0xFF2D2D2A),
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
