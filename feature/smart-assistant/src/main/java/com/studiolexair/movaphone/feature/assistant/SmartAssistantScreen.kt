package com.studiolexair.movaphone.feature.assistant

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoMark
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle
import com.studiolexair.movaphone.feature.assistant.ai.LocalModel
import com.studiolexair.movaphone.feature.assistant.voice.OfflineVoiceInput

/**
 * El chat de MOVA.
 *
 * Es una conversación normal: tú escribes o hablas, MOVA contesta en lenguaje natural, te
 * propone botones cuando hay que decidir algo y **nunca ejecuta una acción sensible sin tu sí**.
 *
 * Todo lo técnico (descargar el modelo, la voz…) vive en el engranaje de arriba:
 * [AssistantSettingsScreen]. Y todo lo que MOVA sabe hacer, en el botón de ayuda.
 */
@Composable
fun SmartAssistantRoute(
    navigator: MovaNavigator,
    viewModel: SmartAssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val micPermission = rememberPermissionHandle(listOf(MovaPermission.RECORD_AUDIO))
    val voice = remember(context) { OfflineVoiceInput(context) }

    DisposableEffect(voice) {
        onDispose { voice.stop() }
    }

    // La pantalla conecta el asistente con la navegación real.
    LaunchedEffect(Unit) {
        viewModel.onOpenHelp = { navigator.toAssistantHelp() }
        viewModel.onOpenSettings = { navigator.toAssistantSettings() }
    }

    fun listen() {
        viewModel.onListeningStarted()
        voice.onModeChanged = { mode ->
            viewModel.onVoiceMode(
                if (mode == OfflineVoiceInput.Mode.ON_DEVICE) "en el teléfono, sin Internet"
                else "con el reconocedor del sistema"
            )
        }
        voice.start(
            languageTag = "es-ES",
            onPartial = { viewModel.onPartial(it) },
            onResult = { spoken ->
                viewModel.onListeningStopped()
                viewModel.submit(spoken, navigator::openFromAssistant)
            },
            onError = { viewModel.onVoiceError(it) }
        )
    }

    // El chat baja solo al último mensaje.
    LaunchedEffect(state.messages.size, state.partial) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size)
    }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(
                onBack = { navigator.back() },
                onHelp = { navigator.toAssistantHelp() },
                onSettings = { navigator.toAssistantSettings() }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MovaDimens.spaceLg,
                    end = MovaDimens.spaceLg,
                    top = MovaDimens.spaceSm,
                    bottom = MovaDimens.spaceSm
                ),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                // Primera vez: se ofrecen los tres tamaños del modelo, sin obligar a nada.
                if (modelState.offerFirstRun) {
                    item {
                        ModelTierOffer(
                            models = modelState.models,
                            downloadingId = modelState.downloadingId,
                            progress = modelState.progress,
                            onDownload = { viewModel.downloadModel(it) },
                            onDismiss = { viewModel.dismissModelOffer() },
                            onOpenSettings = { navigator.toAssistantSettings() }
                        )
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message = message, onOption = { viewModel.onOption(it, navigator::openFromAssistant) })
                }

                if (state.thinking) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MovaLogoMark(size = MovaDimens.iconSm)
                            Text(
                                text = "Pensando…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MovaTheme.extra.textMuted,
                                modifier = Modifier.padding(start = MovaDimens.spaceSm)
                            )
                        }
                    }
                }

                state.partial.takeIf { it.isNotBlank() }?.let { partial ->
                    item {
                        ChatBubble(
                            message = ChatMessage(id = -1, fromUser = true, text = partial),
                            onOption = {},
                            muted = true
                        )
                    }
                }

                state.voiceNotice?.let { notice ->
                    item {
                        MovaInfoBanner(
                            message = notice,
                            tone = PillTone.Warning,
                            icon = Icons.Filled.MicOff
                        )
                    }
                }

                if (modelState.downloadingId != null) {
                    item {
                        Column {
                            Text(
                                text = "Descargando el modelo en tu teléfono…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MovaTheme.extra.textSecondary
                            )
                            LinearProgressIndicator(
                                progress = { modelState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = MovaDimens.spaceXs)
                            )
                        }
                    }
                }
            }

            // Sugerencias rápidas: siempre hay algo que tocar sin pensar.
            LazyRow(
                contentPadding = PaddingValues(horizontal = MovaDimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CommandLibrary.quickExamples) { example ->
                    SuggestionChip(
                        text = example,
                        onClick = { viewModel.submit(example, navigator::openFromAssistant) }
                    )
                }
                item {
                    SuggestionChip(text = "¿Qué sabes hacer?", onClick = { navigator.toAssistantHelp() })
                }
            }

            ChatInputRow(
                value = textInput,
                onValueChange = { textInput = it },
                listening = state.listening,
                micGranted = micPermission.granted,
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.submit(textInput, navigator::openFromAssistant)
                        textInput = ""
                    }
                },
                onMic = {
                    when {
                        !micPermission.granted -> micPermission.request()
                        state.listening -> {
                            voice.stop()
                            viewModel.onListeningStopped()
                        }
                        else -> listen()
                    }
                }
            )

            if (state.voiceMode.isNotBlank() && state.listening) {
                Text(
                    text = "Te escucho ${state.voiceMode}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MovaTheme.extra.textMuted,
                    modifier = Modifier.padding(horizontal = MovaDimens.spaceLg, vertical = MovaDimens.spaceXs)
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(onBack: () -> Unit, onHelp: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MovaDimens.spaceMd, vertical = MovaDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MovaLogoMark(size = MovaDimens.iconMd)
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = MovaDimens.spaceSm)
        ) {
            Text(
                text = "MOVA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tu asistente, aquí dentro y sin Internet",
                style = MaterialTheme.typography.labelSmall,
                color = MovaTheme.extra.textMuted
            )
        }
        IconButton(onClick = onHelp) {
            Icon(Icons.Filled.HelpOutline, contentDescription = "Qué sabe hacer MOVA")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Ajustes del asistente")
        }
    }
}

/** Una burbuja del chat: las tuyas a la derecha, las de MOVA a la izquierda. */
@Composable
private fun ChatBubble(message: ChatMessage, onOption: (ChatOption) -> Unit, muted: Boolean = false) {
    val isUser = message.fromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            MovaLogoMark(
                size = MovaDimens.iconSm,
                modifier = Modifier
                    .padding(end = MovaDimens.spaceSm)
                    .align(Alignment.Top)
            )
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = when {
                    isUser -> MaterialTheme.colorScheme.primary
                    muted -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                },
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            if (message.options.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs),
                    modifier = Modifier.padding(top = MovaDimens.spaceXs)
                ) {
                    message.options.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOption(option) }
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MovaTheme.extra.textSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** Barra de escritura: campo de texto, micrófono y enviar. */
@Composable
private fun ChatInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    listening: Boolean,
    micGranted: Boolean,
    onSend: () -> Unit,
    onMic: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MovaDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (listening) Brush.linearGradient(listOf(MovaTheme.extra.violet, MovaTheme.extra.violet))
                    else Brush.linearGradient(listOf(MovaTheme.extra.violet.copy(alpha = 0.35f), MovaTheme.extra.violet.copy(alpha = 0.2f)))
                )
                .clickable(onClick = onMic),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (listening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (listening) "Parar de escuchar" else "Hablar",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Escribe lo que necesitas…") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = RoundedCornerShape(20.dp)
        )

        IconButton(onClick = onSend) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Enviar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    if (!micGranted) {
        Text(
            text = "Para hablar, concede el permiso de micrófono (te lo pide al tocar el botón). " +
                "Escribiendo funciona todo igual.",
            style = MaterialTheme.typography.labelSmall,
            color = MovaTheme.extra.textMuted,
            modifier = Modifier.padding(horizontal = MovaDimens.spaceLg)
        )
    }
}

/**
 * Traduce el destino entendido por el asistente a navegación real.
 *
 * Está aquí (y no en el módulo de navegación) para que el asistente no dependa de las
 * pantallas concretas: sólo dice «quiero abrir contactos» y esta función decide cómo.
 */
fun MovaNavigator.openFromAssistant(destination: SmartCommand.Destination) {
    when (destination) {
        SmartCommand.Destination.HOME -> toHome()
        SmartCommand.Destination.DIALER -> toDialer()
        SmartCommand.Destination.CALLS -> toCalls()
        SmartCommand.Destination.CONTACTS -> toContacts()
        SmartCommand.Destination.MESSAGES -> toMessages()
        SmartCommand.Destination.SECURITY -> toSecurity()
        SmartCommand.Destination.LOCATION -> toLocation()
        SmartCommand.Destination.AUTOMATION -> toAutomation()
        SmartCommand.Destination.SETTINGS -> toSettings()
        SmartCommand.Destination.SOS -> toSos()
        SmartCommand.Destination.DRIVING -> toDriving()
        SmartCommand.Destination.PROFILE -> toProfile()
        SmartCommand.Destination.FAVORITES -> toFavorites()
        SmartCommand.Destination.TEMPLATES -> toTemplates()
        SmartCommand.Destination.BLOCKED -> toBlockedNumbers()
    }
}

/**
 * Oferta de la primera vez: tres tamaños de modelo local, explicados en palabras normales.
 *
 * Se dice claramente que MOVA **funciona sin descargar nada**, para que nadie sienta que le
 * obligan. Todo ocurre en el teléfono, sin cuentas ni servidores.
 */
@Composable
private fun ModelTierOffer(
    models: List<LocalModel>,
    downloadingId: String?,
    progress: Float,
    onDownload: (LocalModel) -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    MovaCard {
        Text(
            text = "¿Quieres que MOVA piense todavía mejor?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "MOVA ya te entiende sin descargar nada. Si quieres, puedes añadir un cerebro " +
                "extra que vive sólo en tu teléfono (sin Internet y sin cuentas). Elige el tamaño " +
                "que mejor le vaya a tu teléfono:",
            style = MaterialTheme.typography.bodySmall,
            color = MovaTheme.extra.textSecondary,
            modifier = Modifier.padding(top = MovaDimens.spaceXs)
        )

        models.forEach { model ->
            val downloading = downloadingId == model.id
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MovaDimens.spaceSm)
            ) {
                Column(modifier = Modifier.padding(MovaDimens.spaceMd)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary
                    )
                    if (downloading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MovaDimens.spaceSm)
                        )
                        Text(
                            text = "Descargando… ${(progress * 100).toInt()} %",
                            style = MaterialTheme.typography.labelSmall,
                            color = MovaTheme.extra.textMuted
                        )
                    } else {
                        MovaPrimaryButton(
                            text = "Descargar",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MovaDimens.spaceSm),
                            onClick = { onDownload(model) }
                        )
                    }
                }
            }
        }

        MovaSecondaryButton(
            text = "Verlo con calma en los ajustes",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MovaDimens.spaceSm),
            onClick = { onDismiss(); onOpenSettings() }
        )
        MovaSecondaryButton(
            text = "Ahora no, sigo así",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MovaDimens.spaceXs),
            onClick = onDismiss
        )
    }
}
