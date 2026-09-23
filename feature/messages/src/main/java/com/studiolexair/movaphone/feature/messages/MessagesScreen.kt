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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/** Bandeja de conversaciones SMS con importación desde el sistema. */
@Composable
fun MessagesRoute(
    navigator: MovaNavigator,
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.ensureDefaultTemplates() }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Mensajes",
                subtitle = "Comunícate de forma rápida y segura",
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Importar mensajes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = MovaDimens.spaceSm)
                            .clickable { viewModel.importDeviceMessages() }
                    )
                }
            )

            Row(
                modifier = Modifier.padding(horizontal = MovaDimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                AssistChip(onClick = { viewModel.importDeviceMessages() }, label = { Text("Importar del sistema") })
                AssistChip(onClick = { navigator.toTemplates() }, label = { Text("Plantillas") })
            }

            message?.let {
                MovaInfoBanner(
                    message = it,
                    tone = PillTone.Brand,
                    modifier = Modifier.padding(MovaDimens.spaceLg)
                )
            }

            if (conversations.isEmpty()) {
                MovaEmptyState(
                    title = "Sin conversaciones",
                    description = "Importa tus mensajes del sistema o escribe a un contacto desde su ficha.",
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
                    items(conversations, key = { it.id }) { message ->
                        MovaListRow(
                            title = message.contactName ?: message.address,
                            subtitle = "${message.body.take(60)} · ${TextFormatters.clock(message.sentAt)}",
                            leading = { MovaAvatar(initials = (message.contactName ?: message.address).take(2)) },
                            onClick = { navigator.toConversation(message.address) }
                        )
                    }
                }
            }
        }
    }
}

/** Conversación individual: envío real por SMS y compartir ubicación en un toque. */
@Composable
fun ConversationRoute(
    address: String,
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.conversation(address).collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(title = "Conversación", subtitle = address)

            message?.let { MovaInfoBanner(message = it, tone = PillTone.Warning, modifier = Modifier.padding(MovaDimens.spaceLg)) }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                items(messages, key = { it.id }) { entity ->
                    MovaListRow(
                        title = if (entity.isIncoming) "Recibido" else "Enviado",
                        subtitle = entity.body.replace("\n", " ") + "\n" +
                            TextFormatters.relativeDay(entity.sentAt) + " · " + entity.state,
                        leading = { MovaAvatar(initials = if (entity.isIncoming) "RE" else "EN") }
                    )
                }
            }

            Column(modifier = Modifier.padding(MovaDimens.spaceLg)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Escribe un mensaje") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 4
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MovaDimens.spaceSm),
                    horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MovaPrimaryButton(
                        text = "Enviar",
                        icon = Icons.Filled.Message,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.send(address, text)
                            text = ""
                        }
                    )
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Compartir ubicación",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(MovaDimens.spaceSm)
                            .clickable { viewModel.shareLocation(address) }
                    )
                }
            }
        }
    }
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
