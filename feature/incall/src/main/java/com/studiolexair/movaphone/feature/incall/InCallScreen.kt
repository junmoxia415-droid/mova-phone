package com.studiolexair.movaphone.feature.incall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoMark
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.services.calls.CallSessionHolder
import com.studiolexair.movaphone.services.calls.CallSnapshot
import android.telecom.Call
import android.telecom.CallAudioState
import android.os.SystemClock
import kotlinx.coroutines.delay

/**
 * Interfaz de llamada de MOVA Phone, con la identidad de la app:
 * avatar, nombre del contacto, estado real de la llamada, cronómetro y botones grandes
 * (contestar, colgar, altavoz, silenciar, teclado DTMF, espera y cambio de llamada).
 */
@Composable
fun InCallScreen(
    state: CallSessionHolder.UiState,
    onAnswer: (String) -> Unit,
    onHangUp: (String) -> Unit,
    onHangUpAll: () -> Unit,
    onHoldToggle: (String, Boolean) -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
    onDtmf: (String, Char) -> Unit,
    onDtmfStop: () -> Unit,
    onSwapCalls: () -> Unit,
    onDismiss: () -> Unit
) {
    val call = state.primary
    var showKeypad by remember { mutableStateOf(false) }

    LaunchedEffect(state.isEmpty) { if (state.isEmpty) onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaTheme.extra.backgroundGradient.last())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MovaDimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(MovaDimens.spaceXl))
                MovaAvatar(initials = (call?.contactName ?: call?.number ?: "M").take(2))
                Text(
                    text = call?.contactName ?: call?.number ?: "Llamada",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = MovaDimens.spaceLg)
                )
                if (call?.contactName != null) {
                    Text(
                        text = call.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceXxs)
                    )
                }
                Text(
                    text = callStatusText(call),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MovaTheme.extra.textSecondary,
                    modifier = Modifier.padding(top = MovaDimens.spaceSm)
                )
                state.secondaryCall?.let { secondary ->
                    Text(
                        text = "Segunda llamada: ${secondary.contactName ?: secondary.number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.warning,
                        modifier = Modifier
                            .padding(top = MovaDimens.spaceXs)
                            .clickable { onSwapCalls() }
                    )
                }
            }

            if (showKeypad) {
                DtmfKeypad(onKey = { char -> call?.let { onDtmf(it.id, char) } }, onClose = { onDtmfStop(); showKeypad = false })
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MovaLogoMark(size = MovaDimens.iconLg)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallAction(
                    icon = if (state.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label = if (state.muted) "Activar micro" else "Silenciar",
                    active = state.muted,
                    onClick = { onMuteToggle(!state.muted) }
                )
                CallAction(
                    icon = Icons.Filled.VolumeUp,
                    label = "Altavoz",
                    active = state.audioRoute == CallAudioState.ROUTE_SPEAKER,
                    onClick = { onSpeakerToggle(state.audioRoute != CallAudioState.ROUTE_SPEAKER) }
                )
                CallAction(
                    icon = Icons.Filled.Dialpad,
                    label = "Teclado",
                    active = showKeypad,
                    onClick = { showKeypad = !showKeypad }
                )
                CallAction(
                    icon = if (call?.held == true) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    label = if (call?.held == true) "Reanudar" else "Espera",
                    active = call?.held == true,
                    onClick = { call?.let { onHoldToggle(it.id, !it.held) } }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MovaDimens.spaceXl),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (call != null) {
                    if (call.isRinging) {
                        BigCallButton(
                            icon = Icons.Filled.Call,
                            color = MovaTheme.extra.success,
                            label = "Contestar",
                            onClick = { onAnswer(call.id) }
                        )
                    } else {
                        BigCallButton(
                            icon = Icons.Filled.SwapCalls,
                            color = MovaTheme.extra.violet,
                            label = "Cambiar",
                            onClick = onSwapCalls
                        )
                    }
                    BigCallButton(
                        icon = Icons.Filled.CallEnd,
                        color = MovaTheme.extra.danger,
                        label = if (state.calls.size > 1) "Colgar todo" else "Colgar",
                        onClick = { if (state.calls.size > 1) onHangUpAll() else onHangUp(call.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun callStatusText(call: CallSnapshot?): String {
    if (call == null) return "Sin llamadas"
    if (call.isActive) {
        val seconds = ((SystemClock.elapsedRealtime() - call.startedAt) / 1000).coerceAtLeast(0)
        return "En llamada · %02d:%02d".format(seconds / 60, seconds % 60)
    }
    return call.label
}

@Composable
private fun CallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(MovaDimens.minTouchTarget + 12.dp)
                .clip(CircleShape)
                .background(if (active) MovaTheme.extra.brandGradient.first() else MovaTheme.extra.border.copy(alpha = 0.35f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MovaTheme.extra.textSecondary,
            modifier = Modifier.padding(top = MovaDimens.spaceXs)
        )
    }
}

@Composable
private fun BigCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(MovaDimens.iconLg))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MovaTheme.extra.textSecondary,
            modifier = Modifier.padding(top = MovaDimens.spaceXs)
        )
    }
}

/** Teclado DTMF para menús de voz (los tonos se envían por la llamada real). */
@Composable
private fun DtmfKeypad(onKey: (Char) -> Unit, onClose: () -> Unit) {
    val rows = listOf(
        listOf('1' to "", '2' to "ABC", '3' to "DEF"),
        listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
        listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
        listOf('*' to "", '0' to "+", '#' to "")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MovaDimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    Column(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MovaTheme.extra.border.copy(alpha = 0.35f))
                            .clickable { onKey(digit) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = digit.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        if (letters.isNotEmpty()) {
                            Text(text = letters, style = MaterialTheme.typography.labelSmall, color = MovaTheme.extra.textMuted)
                        }
                    }
                }
            }
        }
        Text(
            text = "Cerrar teclado",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = MovaDimens.spaceSm)
                .clickable(onClick = onClose)
        )
    }
}
