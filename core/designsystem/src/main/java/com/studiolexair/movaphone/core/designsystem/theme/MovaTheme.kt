package com.studiolexair.movaphone.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

data class MovaExtraColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val dangerDeep: Color,
    val violet: Color,
    val border: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val tileGradientBlue: List<Color>,
    val tileGradientGreen: List<Color>,
    val tileGradientViolet: List<Color>,
    val tileGradientAmber: List<Color>,
    val backgroundGradient: List<Color>,
    val brandGradient: List<Color>
)

val LocalMovaExtraColors = staticCompositionLocalOf {
    MovaExtraColors(
        success = MovaPalette.Success,
        warning = MovaPalette.Warning,
        danger = MovaPalette.Danger,
        dangerDeep = MovaPalette.DangerDeep,
        violet = MovaPalette.Violet,
        border = MovaPalette.Border,
        textSecondary = MovaPalette.TextSecondary,
        textMuted = MovaPalette.TextMuted,
        tileGradientBlue = listOf(Color(0xFF2F6BFF), Color(0xFF1D4ED8)),
        tileGradientGreen = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
        tileGradientViolet = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
        tileGradientAmber = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
        backgroundGradient = listOf(MovaPalette.Ink, MovaPalette.Night),
        brandGradient = listOf(MovaPalette.Cyan, MovaPalette.Blue)
    )
}

/** Acceso directo: `MovaTheme.extra.success` */
object MovaTheme {
    val extra: MovaExtraColors
        @Composable get() = LocalMovaExtraColors.current
}

private val DarkScheme = darkColorScheme(
    primary = MovaPalette.SkyBlue,
    onPrimary = Color(0xFF04121F),
    primaryContainer = MovaPalette.Blue,
    onPrimaryContainer = MovaPalette.TextPrimary,
    secondary = MovaPalette.Cyan,
    onSecondary = Color(0xFF04121F),
    tertiary = MovaPalette.Violet,
    background = MovaPalette.Ink,
    onBackground = MovaPalette.TextPrimary,
    surface = MovaPalette.SurfaceLow,
    onSurface = MovaPalette.TextPrimary,
    surfaceVariant = MovaPalette.Surface,
    onSurfaceVariant = MovaPalette.TextSecondary,
    outline = MovaPalette.Border,
    error = MovaPalette.Danger,
    onError = Color.White
)

private val LightScheme = lightColorScheme(
    primary = MovaPalette.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF0A1F45),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    tertiary = Color(0xFF6D28D9),
    background = MovaPalette.LightBackground,
    onBackground = MovaPalette.LightTextPrimary,
    surface = MovaPalette.LightSurface,
    onSurface = MovaPalette.LightTextPrimary,
    surfaceVariant = Color(0xFFE8EEF9),
    onSurfaceVariant = MovaPalette.LightTextSecondary,
    outline = MovaPalette.LightBorder,
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun MovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extra = LocalMovaExtraColors.current.let { base ->
        if (darkTheme) base else base.copy(
            border = MovaPalette.LightBorder,
            textSecondary = MovaPalette.LightTextSecondary,
            textMuted = Color(0xFF7A879E),
            backgroundGradient = listOf(Color(0xFFF7FAFF), Color(0xFFEAF1FB))
        )
    }

    CompositionLocalProvider(LocalMovaExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = MovaTypography,
            shapes = MovaShapes,
            content = content
        )
    }
}

/** Estilo de texto para etiquetas de sección en mayúsculas. */
val SectionLabelStyle: TextStyle
    @Composable get() = androidx.compose.material3.MaterialTheme.typography.labelMedium
