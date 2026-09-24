package com.studiolexair.movaphone.feature.messages

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.PermissionPrompt
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle

/**
 * Conversación individual con el diseño aprobado:
 * - cada burbuja lleva su **palomita** de estado a la derecha (enviando · enviado · entregado · leído · fallo);
 * - al **tocar** un mensaje se abre una ficha con el estado real y su explicación;
 * - si falta el permiso de SMS se pide aquí y, en cuanto se concede, el mensaje escrito se envía solo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationRoute(
    address: String,
    viewModel: MessagesViewModel,
    navigator: MovaNavigator?,
    contactName: String? = null,
    prefill: String = "",
    /** Ajustes → Mensajes: se pueden apagar las respuestas rápidas. */
    quickRepliesEnabled: Boolean = true,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.conversation(address).collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val lastFailure by viewModel.lastFailedId.collectAsStateWithLifecycle()

    var draft by remember(address, prefill) { mutableStateOf(prefill) }
    var pendingSend by remember(address) { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<MessageEntity?>(null) }
    var toDelete by remember { mutableStateOf<MessageEntity?>(null) }
    // Opción B (decidida por el usuario): RCS/MMS del teléfono, sin servidor propio.
    var showOtherApps by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val smsPermission = rememberPermissionHandle(
        listOf(MovaPermission.SEND_SMS, MovaPermission.READ_SMS)
    )

    LaunchedEffect(address) { viewModel.markRead(address) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    // En cuanto llega el permiso, se envía lo que el usuario había escrito: nada se pierde.
    LaunchedEffect(smsPermission.granted, pendingSend) {
        val text = pendingSend
        if (smsPermission.granted && !text.isNullOrBlank()) {
            viewModel.send(address, text, contactName)
            pendingSend = null
        }
    }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = contactName?.takeIf { it.isNotBlank() } ?: address,
                subtitle = address,
                actions = {
                    if (onClose != null) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = MovaTheme.extra.textSecondary,
                            modifier = Modifier
                                .padding(end = MovaDimens.spaceSm)
                                .clickable { onClose() }
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Llamar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = MovaDimens.spaceSm)
                            .clickable { navigator?.toDialer(address) }
                    )
                }
            )

            if (!smsPermission.granted) {
                Box(modifier = Modifier.padding(horizontal = MovaDimens.spaceLg)) {
                    PermissionPrompt(
                        permissions = smsPermission.missing,
                        title = "Escribe y responde a tus mensajes",
                        onRequest = { smsPermission.request() }
                    )
                }
            }

            status?.let {
                MovaInfoBanner(
                    message = it,
                    tone = PillTone.Warning,
                    modifier = Modifier.padding(horizontal = MovaDimens.spaceLg, vertical = MovaDimens.spaceSm),
                    icon = Icons.Filled.Refresh
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MovaDimens.spaceLg,
                    end = MovaDimens.spaceLg,
                    top = MovaDimens.spaceSm,
                    bottom = MovaDimens.spaceLg
                ),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                items(messages, key = { it.id }) { entity ->
                    MessageBubble(
                        message = entity,
                        onClick = { detail = entity },
                        onLongClick = { toDelete = entity }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MovaTheme.extra.border.copy(alpha = 0.25f))
                    .padding(MovaDimens.spaceLg)
            ) {
                lastFailure?.let { failedId ->
                    MovaSecondaryButton(
                        text = "Reintentar el último mensaje",
                        icon = Icons.Filled.Refresh,
                        onClick = { viewModel.retry(failedId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (quickRepliesEnabled) {
                    // Respuestas de un toque: se apagan desde Ajustes → Mensajes.
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = MovaDimens.spaceXs)
                    ) {
                        items(QUICK_REPLIES) { quick ->
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable {
                                    if (smsPermission.granted) {
                                        viewModel.send(address, quick, contactName)
                                    } else {
                                        pendingSend = quick
                                        smsPermission.request()
                                    }
                                }
                            ) {
                                Text(
                                    text = quick,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        horizontal = MovaDimens.spaceMd,
                                        vertical = MovaDimens.spaceXs
                                    )
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
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
                        text = if (smsPermission.granted) "Enviar" else "Permitir y enviar",
                        icon = Icons.Filled.Send,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                if (smsPermission.granted) {
                                    viewModel.send(address, text, contactName)
                                } else {
                                    pendingSend = text
                                    smsPermission.request()
                                }
                                draft = ""
                            }
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
                MovaSecondaryButton(
                    text = "Enviar por otra app (RCS o WhatsApp)",
                    icon = Icons.Filled.Share,
                    onClick = { showOtherApps = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MovaDimens.spaceSm)
                )
            }
        }
    }

    if (showOtherApps) {
        ModalBottomSheet(
            onDismissRequest = { showOtherApps = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                Text(
                    text = "Enviar este chat con otra aplicación",
                    style = MaterialTheme.typography.titleMedium
                )
                MovaInfoBanner(
                    message = "MOVA no tiene servidor propio: escribimos con el SMS/MMS de tu " +
                        "operadora. Si tu compañía ofrece RCS (mensajes mejorados), se envía desde " +
                        "la app de mensajería del teléfono; y si prefiere WhatsApp u otra app, se " +
                        "abre con el texto ya escrito.",
                    tone = PillTone.Brand
                )
                MovaListRow(
                    title = "Mensajería del teléfono (RCS/MMS)",
                    subtitle = "Usa el RCS del operador si está disponible",
                    leading = {
                        Icon(
                            Icons.Filled.Message,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(address)}"))
                            .putExtra("sms_body", draft)
                        runCatching { context.startActivity(intent) }
                        showOtherApps = false
                    }
                )
                MovaListRow(
                    title = "Otra aplicación (WhatsApp, Telegram…)",
                    subtitle = "Se abre con el texto escrito",
                    leading = {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, draft)
                        }
                        runCatching { context.startActivity(Intent.createChooser(intent, "Enviar con")) }
                        showOtherApps = false
                    }
                )
            }
        }
    }

    detail?.let { entity ->
        MessageDetailSheet(
            message = entity,
            address = address,
            contactName = contactName,
            onDismiss = { detail = null },
            onCall = { navigator?.toDialer(address) },
            onDelete = {
                viewModel.delete(entity.id)
                detail = null
            },
            onResend = {
                viewModel.retry(entity.id)
                detail = null
            }
        )
    }

    toDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar el mensaje?") },
            text = { Text("Se eliminará de MOVA Phone. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entity.id)
                    toDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * Burbuja de mensaje: saliente a la derecha con el degradado de marca, entrante a la
 * izquierda sobre la superficie. Debajo de las burbujas salientes va la **palomita**
 * del estado real; nada de textos "enviado" sueltos.
 */
@Composable
fun MessageBubble(
    message: MessageEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val stateUi = MessageStates.of(
        message = message,
        accent = MaterialTheme.colorScheme.primary,
        successColor = MovaTheme.extra.success,
        dangerColor = MovaTheme.extra.danger,
        muted = MovaTheme.extra.textMuted
    )
    val outgoing = !message.isIncoming

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = MovaDimens.radiusMd,
                            topEnd = MovaDimens.radiusMd,
                            bottomStart = if (outgoing) MovaDimens.radiusMd else MovaDimens.spaceXs,
                            bottomEnd = if (outgoing) MovaDimens.spaceXs else MovaDimens.radiusMd
                        )
                    )
                    .then(
                        if (outgoing) {
                            Modifier.background(
                                Brush.linearGradient(MovaTheme.extra.brandGradient)
                            )
                        } else {
                            Modifier.background(MovaTheme.extra.border.copy(alpha = 0.6f))
                        }
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = MovaDimens.spaceMd, vertical = MovaDimens.spaceSm)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier.padding(top = MovaDimens.spaceXxs, start = MovaDimens.spaceXs, end = MovaDimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceXxs)
            ) {
                Text(
                    text = TextFormatters.clock(message.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MovaTheme.extra.textMuted
                )
                Icon(
                    imageVector = stateUi.icon,
                    contentDescription = stateUi.label,
                    tint = stateUi.tint,
                    modifier = Modifier
                        .padding(start = MovaDimens.spaceXxs)
                        .clickable(onClick = onClick)
                )
                if (message.isEmergency) {
                    Text(
                        text = "SOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MovaTheme.extra.danger
                    )
                }
            }
        }
    }
}

/** Ficha que aparece al tocar un mensaje: estado real, explicación, hora y acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailSheet(
    message: MessageEntity,
    address: String,
    contactName: String?,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit,
    onResend: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val stateUi = MessageStates.of(
        message = message,
        accent = MaterialTheme.colorScheme.primary,
        successColor = MovaTheme.extra.success,
        dangerColor = MovaTheme.extra.danger,
        muted = MovaTheme.extra.textMuted
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MovaDimens.spaceLg)
                .padding(bottom = MovaDimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MovaAvatar(initials = (contactName ?: address).take(2))
                Column(modifier = Modifier.padding(start = MovaDimens.spaceMd)) {
                    Text(
                        text = contactName?.takeIf { it.isNotBlank() } ?: address,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (message.isIncoming) "Mensaje recibido" else "Mensaje enviado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary
                    )
                }
            }

            MovaInfoBanner(
                message = stateUi.label + ": " + stateUi.explanation,
                tone = when (stateUi.key) {
                    MessageStates.FAILED -> PillTone.Danger
                    MessageStates.DELIVERED, MessageStates.READ -> PillTone.Success
                    else -> PillTone.Brand
                },
                icon = stateUi.icon
            )

            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = TextFormatters.relativeDay(message.sentAt) + " · vía " + message.provider.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MovaTheme.extra.textMuted
            )

            MovaSecondaryButton(
                text = "Copiar el texto",
                icon = Icons.Filled.ContentCopy,
                onClick = { clipboard.setText(AnnotatedString(message.body)) },
                modifier = Modifier.fillMaxWidth()
            )
            MovaSecondaryButton(
                text = "Llamar a " + (contactName ?: address),
                icon = Icons.Filled.Call,
                onClick = onCall,
                modifier = Modifier.fillMaxWidth()
            )
            if (!message.isIncoming && stateUi.key == MessageStates.FAILED) {
                MovaPrimaryButton(
                    text = "Reintentar el envío",
                    icon = Icons.Filled.Refresh,
                    onClick = onResend,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            MovaSecondaryButton(
                text = "Borrar el mensaje",
                icon = Icons.Filled.Delete,
                accent = MovaTheme.extra.danger,
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MovaTheme.extra.textMuted
                )
                Text(
                    text = "Guardado sólo en tu teléfono",
                    style = MaterialTheme.typography.labelSmall,
                    color = MovaTheme.extra.textMuted,
                    modifier = Modifier.padding(start = MovaDimens.spaceSm)
                )
            }
        }
    }
}

/** Respuestas rápidas de un toque (se pueden apagar en Ajustes → Mensajes). */
private val QUICK_REPLIES = listOf("Ya voy", "Te llamo luego", "¿Estás bien?", "Gracias")
