package com.studiolexair.movaphone.feature.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSearchField
import com.studiolexair.movaphone.core.designsystem.component.MovaSectionHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaStatusPill
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.PermissionPrompt
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle
import com.studiolexair.movaphone.data.messages.model.SenderSummary

/** Secciones de la bandeja de mensajes. */
private enum class MessagesTab(val title: String) {
    CONVERSATIONS("Conversaciones"),
    SENDERS("Te han escrito")
}

/**
 * Bandeja de mensajes en dos secciones:
 * 1. **Conversaciones**: los chats que ya existen en el teléfono.
 * 2. **Te han escrito**: todas las personas que te han enviado un mensaje, con cuántos
 *    quedan sin leer; desde aquí se entra directo a la conversación.
 */
@Composable
fun MessagesRoute(
    navigator: MovaNavigator,
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val senders by viewModel.senders.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()
    val unreadTotal by viewModel.unreadTotal.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(MessagesTab.CONVERSATIONS) }
    var query by remember { mutableStateOf("") }

    // Pedir el permiso aquí hace que los mensajes del teléfono aparezcan solos.
    val smsPermission = rememberPermissionHandle(
        listOf(MovaPermission.READ_SMS, MovaPermission.RECEIVE_SMS, MovaPermission.SEND_SMS)
    )
    LaunchedEffect(smsPermission.granted) {
        viewModel.ensureDefaultTemplates()
        if (smsPermission.granted) viewModel.importDeviceMessages()
    }

    val filteredConversations = conversations.filter { matches(it.contactName ?: it.address, it.body, query) }
    val filteredSenders = senders.filter { matches(it.displayName, it.lastBody, query) }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Mensajes",
                subtitle = if (unreadTotal > 0) "$unreadTotal mensajes sin leer" else "Comunícate de forma rápida y segura",
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Importar mensajes del sistema",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = MovaDimens.spaceSm)
                            .clickable { viewModel.importDeviceMessages() }
                    )
                }
            )

            if (!smsPermission.granted) {
                Column(modifier = Modifier.padding(horizontal = MovaDimens.spaceLg)) {
                    PermissionPrompt(
                        permissions = smsPermission.missing,
                        title = "Ver y responder tus mensajes",
                        onRequest = { smsPermission.request() }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MovaDimens.spaceLg, vertical = MovaDimens.spaceSm),
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                MessagesTab.values().forEach { candidate ->
                    FilterChip(
                        selected = tab == candidate,
                        onClick = { tab = candidate },
                        label = {
                            Text(
                                if (candidate == MessagesTab.SENDERS && unreadTotal > 0) {
                                    candidate.title + " ($unreadTotal)"
                                } else {
                                    candidate.title
                                }
                            )
                        }
                    )
                }
            }

            MovaSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar en los mensajes",
                modifier = Modifier.padding(horizontal = MovaDimens.spaceLg)
            )

            message?.let {
                MovaInfoBanner(
                    message = it,
                    tone = PillTone.Brand,
                    modifier = Modifier.padding(MovaDimens.spaceLg)
                )
            }

            when (tab) {
                MessagesTab.CONVERSATIONS ->
                    if (filteredConversations.isEmpty()) {
                        MovaEmptyState(
                            title = "Sin conversaciones",
                            description = "Concede el permiso de SMS o escribe a un contacto desde su ficha para empezar.",
                            icon = Icons.Filled.Message,
                            action = {
                                MovaPrimaryButton(
                                    text = "Importar mensajes",
                                    icon = Icons.Filled.Sync,
                                    onClick = viewModel::importDeviceMessages
                                )
                            }
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
                            items(filteredConversations, key = { it.id }) { entity ->
                                val name = entity.contactName ?: entity.address
                                MovaListRow(
                                    title = name,
                                    subtitle = entity.body.take(70).replace("\n", " ") +
                                        " · " + TextFormatters.relativeDay(entity.sentAt),
                                    leading = { MovaAvatar(initials = name.take(2)) },
                                    trailing = {
                                        if (!entity.isIncoming && entity.state == MessageStates.FAILED) {
                                            MovaStatusPill(text = "No enviado", tone = PillTone.Danger)
                                        }
                                    },
                                    accent = if (!entity.isIncoming && entity.state == MessageStates.FAILED) {
                                        MovaTheme.extra.danger
                                    } else {
                                        null
                                    },
                                    onClick = { navigator.toConversation(entity.address) },
                                    onLongClick = { viewModel.delete(entity.id) }
                                )
                            }
                        }
                    }

                MessagesTab.SENDERS ->
                    if (filteredSenders.isEmpty()) {
                        MovaEmptyState(
                            title = "Todavía nadie te ha escrito",
                            description = "Cuando alguien te envíe un SMS aparecerá aquí, con su último mensaje y los que no has leído.",
                            icon = Icons.Filled.Person
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
                            item {
                                MovaSectionHeader(
                                    text = "${filteredSenders.size} personas que te han escrito",
                                    trailing = {
                                        Icon(
                                            imageVector = Icons.Filled.MarkChatRead,
                                            contentDescription = null,
                                            tint = MovaTheme.extra.textMuted
                                        )
                                    }
                                )
                            }
                            items(filteredSenders, key = { it.normalizedAddress }) { sender ->
                                SenderRow(sender = sender, onClick = { navigator.toConversation(sender.address) })
                            }
                        }
                    }
            }
        }
    }
}

/** Fila de una persona que ha escrito: nombre, último mensaje, hora y no leídos. */
@Composable
private fun SenderRow(sender: SenderSummary, onClick: () -> Unit) {
    MovaListRow(
        title = sender.displayName,
        subtitle = sender.lastBody.take(70).replace("\n", " ") + " · " +
            TextFormatters.relativeDay(sender.lastAt) + " · " + sender.totalMessages + " mensajes",
        leading = { MovaAvatar(initials = sender.displayName.take(2)) },
        trailing = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (sender.unreadCount > 0) {
                    MovaStatusPill(text = "${sender.unreadCount} sin leer", tone = PillTone.Brand)
                } else {
                    MovaStatusPill(text = "Al día", tone = PillTone.Neutral)
                }
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = "Abrir la conversación",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = MovaDimens.spaceSm)
                )
            }
        },
        onClick = onClick
    )
}

/** Plantillas de mensajes rápidos y de emergencia. */
@Composable
fun TemplatesRoute(
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Plantillas",
                subtitle = "Mensajes rápidos y plantilla de emergencia"
            )
            LazyColumn(
                contentPadding = PaddingValues(MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                items(templates, key = { it.id }) { template ->
                    MovaListRow(
                        title = template.title,
                        subtitle = template.body.take(90),
                        accent = if (template.category == "emergency") MovaTheme.extra.danger else null
                    )
                }
            }
        }
    }
}

private fun matches(name: String?, body: String, query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim().lowercase()
    return name?.lowercase()?.contains(needle) == true || body.lowercase().contains(needle)
}

/** Icono de la barra inferior para Mensajes (usado por el hub de Inicio). */
val messagesIcon: ImageVector = Icons.Filled.Message
