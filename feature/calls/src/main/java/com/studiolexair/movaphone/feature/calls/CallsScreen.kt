package com.studiolexair.movaphone.feature.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaStatusPill
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.PermissionPrompt
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle
import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.model.CallType
import com.studiolexair.movaphone.domain.calls.usecase.CallFilter

/** Historial: Todas · Perdidas · Entrantes · Salientes · Spam, con acciones por llamada. */
@Composable
fun CallsRoute(
    navigator: MovaNavigator,
    viewModel: CallsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Con permiso se importa el historial real del dispositivo automáticamente.
    val logPermission = rememberPermissionHandle(listOf(MovaPermission.CALL_LOG))
    LaunchedEffect(logPermission.granted) {
        if (logPermission.granted) viewModel.syncFromDevice()
    }

    CallsScreen(
        state = state,
        navigator = navigator,
        onFilterChange = viewModel::setFilter,
        onSync = viewModel::syncFromDevice,
        onDelete = viewModel::delete,
        onBlock = viewModel::block,
        onReportSpam = viewModel::reportSpam,
        onSaveContact = viewModel::saveAsContact,
        onDismissMessage = viewModel::clearMessage,
        permissionCard = if (logPermission.granted) {
            null
        } else {
            {
                PermissionPrompt(
                    permissions = listOf(MovaPermission.CALL_LOG, MovaPermission.READ_PHONE_STATE),
                    title = "Ver el historial de tu teléfono",
                    onRequest = { logPermission.request() }
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun CallsScreen(
    state: CallsUiState,
    navigator: MovaNavigator,
    onFilterChange: (CallFilter) -> Unit,
    onSync: () -> Unit,
    onDelete: (CallRecord) -> Unit,
    onBlock: (CallRecord) -> Unit,
    onReportSpam: (CallRecord) -> Unit,
    onSaveContact: (CallRecord) -> Unit,
    onDismissMessage: () -> Unit,
    permissionCard: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AuroraBackground(modifier = modifier) {
        permissionCard?.invoke()
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Historial de llamadas",
                subtitle = "Revisa tus llamadas recientes",
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Sincronizar historial",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = MovaDimens.spaceSm)
                            .clickableIcon(onSync)
                    )
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MovaDimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                CallFilter.values().forEach { filter ->
                    AssistChip(
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label()) },
                        colors = if (filter == state.filter) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }

            state.statusMessage?.let { message ->
                MovaInfoBanner(
                    message = message,
                    tone = PillTone.Brand,
                    modifier = Modifier.padding(MovaDimens.spaceLg),
                    icon = Icons.Filled.Call
                )
            }

            if (state.filtered.isEmpty()) {
                MovaEmptyState(
                    title = "Sin llamadas en esta vista",
                    description = "Cuando haya llamadas aparecerán aquí. Puedes sincronizar el historial del dispositivo.",
                    icon = Icons.Filled.Call
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MovaDimens.spaceLg,
                        end = MovaDimens.spaceLg,
                        bottom = MovaDimens.spaceXxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(state.filtered, key = { it.id }) { record ->
                        CallRow(
                            record = record,
                            onCall = { navigator.toDialer(record.number) },
                            onMessage = { navigator.toConversation(record.number) },
                            onSaveContact = { onSaveContact(record) },
                            onBlock = { onBlock(record) },
                            onReportSpam = { onReportSpam(record) },
                            onDelete = { onDelete(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRow(
    record: CallRecord,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onSaveContact: () -> Unit,
    onBlock: () -> Unit,
    onReportSpam: () -> Unit,
    onDelete: () -> Unit
) {
    val extra = MovaTheme.extra
    val accent = when {
        record.isSpam -> extra.danger
        record.type == CallType.MISSED -> extra.warning
        record.type == CallType.OUTGOING -> extra.success
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs)) {
        MovaListRow(
            title = record.displayName,
            subtitle = "${record.number} · ${record.type.label()} · ${record.durationLabel()}",
            leading = { MovaAvatar(initials = record.displayName.take(2), accent = accent) },
            trailing = {
                if (record.isSpam) MovaStatusPill(text = "Spam", tone = PillTone.Danger)
                else if (record.type == CallType.MISSED) MovaStatusPill(text = "Perdida", tone = PillTone.Warning)
            },
            onClick = onCall
        )
        Row(
            modifier = Modifier.padding(start = MovaDimens.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallAction(Icons.Filled.Call, "Llamar", onCall)
            CallAction(Icons.Filled.Message, "SMS", onMessage)
            if (!record.isKnownContact) CallAction(Icons.Filled.PersonAdd, "Guardar", onSaveContact)
            CallAction(Icons.Filled.Block, "Bloquear", onBlock)
            CallAction(Icons.Filled.Report, "Spam", onReportSpam)
            CallAction(Icons.Filled.Delete, "Eliminar", onDelete)
        }
    }
}

@Composable
private fun CallAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = MovaTheme.extra.textSecondary,
        modifier = Modifier
            .padding(vertical = MovaDimens.spaceXs)
            .clickableIcon(onClick)
    )
}

private fun Modifier.clickableIcon(onClick: () -> Unit): Modifier =
    this.padding(4.dp).clickable(onClick = onClick)

private fun CallFilter.label(): String = when (this) {
    CallFilter.ALL -> "Todas"
    CallFilter.MISSED -> "Perdidas"
    CallFilter.INCOMING -> "Entrantes"
    CallFilter.OUTGOING -> "Salientes"
    CallFilter.SPAM -> "Spam"
}

private fun CallType.label(): String = when (this) {
    CallType.INCOMING -> "Entrante"
    CallType.OUTGOING -> "Saliente"
    CallType.MISSED -> "Perdida"
    CallType.REJECTED -> "Rechazada"
    CallType.BLOCKED -> "Bloqueada"
}

private fun CallRecord.durationLabel(): String {
    if (durationSeconds <= 0) return "sin respuesta"
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return if (minutes > 0) "$minutes min $seconds s" else "$seconds s"
}
