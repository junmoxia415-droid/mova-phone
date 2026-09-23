package com.studiolexair.movaphone.core.designsystem.branding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import com.studiolexair.movaphone.core.designsystem.R
import com.studiolexair.movaphone.core.designsystem.theme.BrandSubtitleStyle
import com.studiolexair.movaphone.core.designsystem.theme.BrandWordmarkStyle
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/**
 * Marca gráfica "M" de MOVA. Se dibuja por código (vectorial, escalable,
 * sin depender de imágenes) y el usuario final la ve siempre igual.
 */
@Composable
fun MovaLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    glow: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        drawMovaM(glow = glow)
    }
}

private fun DrawScope.drawMovaM(glow: Boolean) {
    val w = this.size.width
    val h = this.size.height
    val strokeWidth = w * 0.17f

    val leftBrush = Brush.linearGradient(
        colors = listOf(MovaPalette.Cyan, MovaPalette.SkyBlue),
        start = Offset(0f, 0f),
        end = Offset(w, h)
    )
    val rightBrush = Brush.linearGradient(
        colors = listOf(MovaPalette.SkyBlue, MovaPalette.Blue),
        start = Offset(w * 0.4f, 0f),
        end = Offset(w, h)
    )

    val left = Path().apply {
        moveTo(w * 0.10f, h * 0.84f)
        lineTo(w * 0.26f, h * 0.20f)
        lineTo(w * 0.50f, h * 0.62f)
    }
    val right = Path().apply {
        moveTo(w * 0.50f, h * 0.62f)
        lineTo(w * 0.74f, h * 0.20f)
        lineTo(w * 0.90f, h * 0.84f)
    }

    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    if (glow) {
        val glowStroke = Stroke(width = strokeWidth * 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(left, MovaPalette.Cyan.copy(alpha = 0.28f), style = glowStroke)
        drawPath(right, MovaPalette.Blue.copy(alpha = 0.28f), style = glowStroke)
    }
    drawPath(left, leftBrush, style = stroke)
    drawPath(right, rightBrush, style = stroke)
}

/** Logotipo completo: marca + logotipo tipográfico (MOVA / Phone). */
@Composable
fun MovaLogoLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 76.dp,
    showTagline: Boolean = false,
    horizontal: Boolean = false
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val onBrand = if (dark) MovaPalette.TextPrimary else MovaPalette.LightTextPrimary
    val sub = androidx.compose.material3.MaterialTheme.colorScheme.primary

    val content: @Composable () -> Unit = {
        MovaLogoMark(size = markSize)
        val textBlock: @Composable () -> Unit = {
            Text(
                text = BrandConfig.PRODUCT_NAME,
                style = BrandWordmarkStyle,
                color = onBrand
            )
            Text(
                text = BrandConfig.PRODUCT_SUFFIX,
                style = BrandSubtitleStyle,
                color = onBrand.copy(alpha = 0.85f)
            )
        }
        if (horizontal) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) { textBlock() }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { textBlock() }
        }
        if (showTagline) {
            Text(
                text = BrandConfig.TAGLINE,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = sub,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (horizontal) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) { content() }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    }
}

/** Texto legible por lectores de pantalla para la marca. */
@Composable
fun brandContentDescription(): String = stringResource(R.string.brand_logo_description)

@Preview
@Composable
private fun MovaLogoPreview() {
    MovaTheme(darkTheme = true) {
        MovaLogoLockup(showTagline = true)
    }
}
