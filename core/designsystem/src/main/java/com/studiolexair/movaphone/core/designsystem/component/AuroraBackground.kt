package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/**
 * Fondo de la marca: degradado profundo con destellos sutiles.
 * Se usa detrás de todas las pantallas para que el producto se sienta único.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val extra = MovaTheme.extra
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(extra.backgroundGradient))
            .background(
                Brush.radialGradient(
                    colors = listOf(MovaPalette.Blue.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(MovaPalette.Cyan.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(1100f, 260f),
                    radius = 720f
                )
            ),
        content = content
    )
}
