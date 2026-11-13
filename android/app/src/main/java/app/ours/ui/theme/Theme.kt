package app.ours.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object OursTheme {
    val colors: OursColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOursColors.current
}

@Composable
fun OursTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) OursDarkColors else OursLightColors
    val m3Scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.action,
            onPrimary = colors.onAction,
            background = colors.canvas,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.inkSecondary,
            outline = colors.border,
            error = colors.error,
        )
    } else {
        lightColorScheme(
            primary = colors.action,
            onPrimary = colors.onAction,
            background = colors.canvas,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.inkSecondary,
            outline = colors.border,
            error = colors.error,
        )
    }

    CompositionLocalProvider(LocalOursColors provides colors) {
        MaterialTheme(
            colorScheme = m3Scheme,
            content = content,
        )
    }
}
