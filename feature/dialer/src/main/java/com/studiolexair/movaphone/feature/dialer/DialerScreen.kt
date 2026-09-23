package com.studiolexair.movaphone.feature.dialer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.PermissionPrompt
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle

/**
 * Marcador: teclado grande, háptica configurable, sugerencias en vivo y acciones
 * sobre el número antes de llamar (guardar, copiar, bloquear, reportar).
 */
@Composable
fun DialerRoute(
    navigator: MovaNavigator,
    viewModel: DialerViewModel,
    hapticEnabled: Boolean = true,
    prefill: String = "",
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permiso de llamadas: se pide al pulsar "llamar" y la llamada se reintenta sola.
    val callPermission = rememberPermissionHandle(listOf(MovaPermission.CALL_PHONE))
    var pendingNumber by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(callPermission.granted) {
        if (callPermission.granted) {
            val pending = pendingNumber
            pendingNumber = null
            if (pending != null) viewModel.call(pending)
        }
    }

    // Si se llega con un número (contacto, historial, asistente) se escribe solo.
    LaunchedEffect(prefill) {
        if (prefill.isNotBlank()) viewModel.setInput(prefill)
    }

    val startCall: (String) -> Unit = { number ->
        if (callPermission.granted) {
            viewModel.call(number)
        } else {
            pendingNumber = number
            callPermission.request()
        }
    }

    DialerScreen(
        state = state,
        navigator = navigator,
        hapticEnabled = hapticEnabled,
        onKeyPressed = viewModel::onKeyPressed,
        onCall = { startCall(state.input) },
        onCallNumber = { number -> startCall(number) },
        onCopy = { number -> copyToClipboard(context, number) },
        onBlock = viewModel::blockCurrentNumber,
        onReportSpam = viewModel::reportSpam,
        onSaveContact = { name -> viewModel.saveAsContact(name) },
        onDismissMessage = viewModel::clearMessage,
        permissionCard = if (callPermission.granted) {
            null
        } else {
            {
                PermissionPrompt(
                    permissions = listOf(MovaPermission.CALL_PHONE),
                    title = "Permiso para llamar",
                    onRequest = { callPermission.request() }
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun DialerScreen(
    state: DialerUiState,
    navigator: MovaNavigator,
    hapticEnabled: Boolean,
    onKeyPressed: (String) -> Unit,
    onCall: () -> Unit,
    onCallNumber: (String) -> Unit,
    onCopy: (String) -> Unit,
    onBlock: () -> Unit,
    onReportSpam: () -> Unit,
    onSaveContact: (String) -> Unit,
    onDismissMessage: () -> Unit,
    permissionCard: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val extra = MovaTheme.extra
    val haptics = LocalHapticFeedback.current
    AuroraBackground(modifier = modifier) {
        permissionCard?.invoke()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MovaDimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.formatted.ifBlank { "Marca un número" },
                style = MaterialTheme.typography.headlineMedium,
                color = if (state.formatted.isBlank()) extra.textMuted else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MovaDimens.spaceXl)
            )

            state.spamVerdict?.takeIf { it.isSpam }?.let { verdict ->
                MovaInfoBanner(
                    message = "Posible spam detectado (${verdict.reason}). Puedes bloquearlo antes de responder.",
                    tone = PillTone.Danger,
                    icon = Icons.Filled.Report
                )
            }

            state.statusMessage?.let { MovaInfoBanner(message = it, tone = PillTone.Success, icon = Icons.Filled.Call) }
            state.errorMessage?.let { MovaInfoBanner(message = it, tone = PillTone.Warning) }

            Keypad(
                onKeyPressed = { key ->
                    if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onKeyPressed(key)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MovaDimens.spaceLg),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialerAction(icon = Icons.Filled.PersonAdd, label = "Guardar", enabled = state.input.isNotBlank()) {
                    onSaveContact(state.formatted)
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(MovaPalette.Success, Color(0xFF15803D))), shape = CircleShape)
                        .clickable(role = Role.Button, enabled = state.canCall, onClick = onCall),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Llamar",
                        tint = Color.White,
                        modifier = Modifier.size(MovaDimens.iconLg)
                    )
                }
                DialerAction(icon = Icons.Filled.Block, label = "Bloquear", enabled = state.input.isNotBlank()) {
                    onBlock()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MovaDimens.spaceMd),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DialerAction(icon = Icons.Filled.Backspace, label = "Borrar", enabled = state.input.isNotBlank()) {
                    onKeyPressed("DEL")
                }
                DialerAction(icon = Icons.Filled.Report, label = "Copiar", enabled = state.input.isNotBlank()) {
                    onCopy(state.formatted)
                }
                DialerAction(icon = Icons.Filled.Report, label = "Reportar", enabled = state.input.isNotBlank()) {
                    onReportSpam()
                }
            }

            if (state.suggestions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                    items(state.suggestions, key = { it.id }) { contact ->
                        MovaListRow(
                            title = contact.displayName,
                            subtitle = contact.phoneNumber,
                            leading = { MovaAvatar(initials = contact.initials) },
                            trailing = { Icon(Icons.Filled.Call, contentDescription = null, tint = extra.success) },
                            onClick = { onCallNumber(contact.phoneNumber) }
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun DialerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(MovaDimens.minTouchTarget)
                .clickable(role = Role.Button, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.onBackground else MovaTheme.extra.textMuted
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MovaTheme.extra.textSecondary)
    }
}

@Composable
private fun Keypad(onKeyPressed: (String) -> Unit) {
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )
    Column(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)) {
                row.forEach { (digit, letters) ->
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape)
                            .border(1.dp, MovaTheme.extra.border.copy(alpha = 0.6f), CircleShape)
                            .clickable(role = Role.Button, onClick = { onKeyPressed(if (digit == "0") "0" else digit) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (letters.isNotEmpty()) {
                                Text(
                                    text = letters,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MovaTheme.extra.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Copia el número al portapapeles del sistema. */
private fun copyToClipboard(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Número MOVA Phone", value))
}
