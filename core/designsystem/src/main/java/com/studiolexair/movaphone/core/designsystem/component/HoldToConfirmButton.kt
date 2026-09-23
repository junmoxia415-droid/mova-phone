package com.studiolexair.movaphone.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/**
 * Botón de confirmación por pulsación mantenida.
 * Requisito del proyecto: la emergencia se activa manteniendo 3 segundos,
 * con cuenta regresiva visible y posibilidad de cancelar soltando el dedo.
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 3_000L,
    size: Dp = 200.dp,
    enabled: Boolean = true,
    dangerColors: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val remainingSeconds = (((1f - progress.value) * (holdMillis / 1000f)).toInt() + 1)

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) {
            progress.animateTo(0f, tween(220))
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        val result = progress.animateTo(1f, tween(holdMillis.toInt().coerceAtLeast(1), easing = LinearEasing))
        if (result.endReason == AnimationEndReason.Finished) {
            onConfirmed()
            pressed = false
        }
    }

    val baseColors = if (dangerColors) {
        listOf(MovaPalette.Danger, MovaPalette.DangerDeep)
    } else {
        listOf(MovaPalette.SkyBlue, MovaPalette.Blue)
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = label }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        pressed = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // halo exterior
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(baseColors.first().copy(alpha = 0.28f), Color.Transparent)
                ),
                radius = this.size.minDimension / 2f
            )
            drawCircle(color = Color.White.copy(alpha = 0.08f), radius = this.size.minDimension / 2f - stroke / 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress.value > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(baseColors),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (pressed && enabled) "$remainingSeconds" else label,
                fontSize = if (pressed && enabled) 54.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (pressed && enabled) "suelta para cancelar" else "mantén 3 segundos",
                style = MaterialTheme.typography.bodySmall,
                color = MovaTheme.extra.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
