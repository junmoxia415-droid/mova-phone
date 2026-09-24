package com.studiolexair.movaphone.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.SystemRoles
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaSwitchRow
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.rememberPermissionChecker
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle

data class SettingsSection(val id: String, val title: String, val icon: ImageVector)

private val sections = listOf(
    SettingsSection("general", "General", Icons.Filled.Tune),
    SettingsSection("calls", "Llamadas", Icons.Filled.Call),
    SettingsSection("contacts", "Contactos", Icons.Filled.Contacts),
    SettingsSection("messages", "Mensajes", Icons.Filled.Message),
    SettingsSection("sos", "SOS", Icons.Filled.Emergency),
    SettingsSection("security", "Seguridad", Icons.Filled.Lock),
    SettingsSection("permisos", "Permisos de la aplicación", Icons.Filled.VerifiedUser),
    SettingsSection("sistema", "MOVA como app del sistema", Icons.Filled.PhoneAndroid),
    SettingsSection("privacy", "Privacidad", Icons.Filled.Storage),
    SettingsSection("automation", "Automatizaciones", Icons.Filled.Bolt),
    SettingsSection("location", "Ubicación", Icons.Filled.LocationOn),
    SettingsSection("notifications", "Notificaciones", Icons.Filled.Notifications),
    SettingsSection("accessibility", "Accesibilidad", Icons.Filled.Accessibility),
    SettingsSection("driving", "Modo conducción", Icons.Filled.DirectionsCar),
    SettingsSection("appearance", "Apariencia", Icons.Filled.Palette),
    SettingsSection("data", "Datos", Icons.Filled.Storage),
    SettingsSection("language", "Idioma", Icons.Filled.Language)
)

@Composable
fun SettingsRoute(
    navigator: MovaNavigator,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MovaDimens.spaceLg,
                end = MovaDimens.spaceLg,
                bottom = MovaDimens.spaceXxl
            ),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
        ) {
            item { MovaScreenHeader(title = "Ajustes", subtitle = settings.userName) }
            items(sections) { section ->
                MovaListRow(
                    title = section.title,
                    leading = { androidx.compose.material3.Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { navigator.toSettingsSection(section.id) }
                )
            }
            item {
                MovaListRow(
                    title = "Acerca de MOVA Phone",
                    subtitle = "Versión, desarrollador y licencias",
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Info, contentDescription = null, tint = MovaTheme.extra.violet) },
                    onClick = { navigator.toAbout() }
                )
            }
            item {
                MovaListRow(
                    title = "Privacidad",
                    subtitle = "Qué datos se guardan y dónde",
                    leading = { androidx.compose.material3.Icon(Icons.Filled.Lock, contentDescription = null, tint = MovaTheme.extra.success) },
                    onClick = { navigator.toPrivacy() }
                )
            }
        }
    }
}

/**
 * Tarjeta para pedir un rol del sistema (teléfono o mensajes).
 * Muestra el estado real y abre el diálogo del sistema correspondiente.
 */
@Composable
private fun SystemRoleCard(
    title: String,
    role: String,
    icon: ImageVector
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refresh by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { refresh++ }

    val summary = androidx.compose.runtime.remember(refresh, role) { SystemRoles.summary(context, role) }
    val isDefault = androidx.compose.runtime.remember(refresh, role) { SystemRoles.holdsRole(context, role) }

    MovaCard {
        MovaListRow(
            title = title,
            subtitle = summary,
            leading = {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDefault) MovaTheme.extra.success else MaterialTheme.colorScheme.primary
                )
            }
        )
        if (!isDefault) {
            MovaSecondaryButton(
                text = "Usar MOVA Phone como $title",
                icon = icon,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MovaDimens.spaceSm),
                onClick = {
                    val intent = SystemRoles.requestIntent(context, role)
                    if (intent != null) launcher.launch(intent)
                    refresh++
                }
            )
        }
    }
}

/** Pantalla de cada sección de ajustes. Cada interruptor se aplica inmediatamente. */
@Composable
fun SettingsSectionRoute(
    section: String,
    navigator: MovaNavigator,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val title = sections.firstOrNull { it.id == section }?.title ?: "Ajustes"

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item { MovaScreenHeader(title = title, subtitle = "MOVA Phone") }

            when (section) {
                "general" -> {
                    item {
                        MovaCard {
                            OutlinedTextField(
                                value = settings.userName,
                                onValueChange = viewModel::setUserName,
                                label = { Text("Tu nombre (para el saludo)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                    item {
                        MovaSwitchRow(
                            title = "Modo conducción automático",
                            subtitle = "Detecta conducción por Bluetooth/velocidad cuando sea posible",
                            checked = settings.drivingAutoDetect,
                            onCheckedChange = viewModel::setDrivingAutoDetect
                        )
                    }
                }
                "calls" -> {
                    item {
                        MovaSwitchRow(
                            title = "Vibración del teclado del marcador",
                            checked = settings.hapticKeypad,
                            onCheckedChange = viewModel::setHaptic
                        )
                    }
                    item {
                        MovaSwitchRow(
                            title = "Confirmar antes de llamar",
                            checked = settings.confirmBeforeCalling,
                            onCheckedChange = viewModel::setConfirmCall
                        )
                    }
                    item {
                        MovaSwitchRow(
                            title = "Bloquear números desconocidos",
                            subtitle = "Sólo se bloquean llamadas si concedes el rol de filtrado",
                            checked = settings.blockUnknownNumbers,
                            onCheckedChange = viewModel::setBlockUnknown
                        )
                    }
                    item {
                        MovaSwitchRow(
                            title = "Detección de spam",
                            checked = settings.spamDetectionEnabled,
                            onCheckedChange = viewModel::setSpamDetection
                        )
                    }
                    item {
                        // Requisito 35: si la API del sistema no lo permite, se dice con claridad.
                        val recording = rememberPermissionChecker()
                        MovaInfoBanner(
                            message = if (recording.isCallRecordingAvailable()) {
                                "Grabación de llamadas: el sistema de este dispositivo permite usar la API oficial. " +
                                    "MOVA Phone no graba nada sin que tú lo inicies y te lo indique legalmente."
                            } else {
                                recording.callRecordingUnavailableMessage()
                            },
                            tone = if (recording.isCallRecordingAvailable()) PillTone.Success else PillTone.Neutral
                        )
                    }
                }
                "contacts" -> {
                    item { MovaSwitchRow(title = "Mostrar contactos del dispositivo", checked = settings.showDeviceContacts, onCheckedChange = { }) }
                    item { MovaListRow(title = "Grupos y privacidad", subtitle = "Gestiona contactos privados desde la sección Contactos") }
                }
                "messages" -> {
                    item { MovaSwitchRow(title = "Respuestas rápidas", checked = settings.quickRepliesEnabled, onCheckedChange = { }) }
                    item { MovaSecondaryButton(text = "Editar plantillas de mensajes", onClick = { navigator.toTemplates() }) }
                }
                "sos" -> {
                    item {
                        MovaSwitchRow(
                            title = "Enviar SMS de emergencia",
                            subtitle = "Requiere permiso de SMS",
                            checked = settings.sosSendSms,
                            onCheckedChange = viewModel::setSosSms
                        )
                    }
                    item {
                        MovaSwitchRow(
                            title = "Compartir ubicación en el SOS",
                            checked = settings.sosShareLocation,
                            onCheckedChange = viewModel::setSosLocation
                        )
                    }
                    item {
                        MovaSwitchRow(
                            title = "Llamar al primer contacto",
                            checked = settings.sosPlaceCall,
                            onCheckedChange = viewModel::setSosCall
                        )
                    }
                    item { MovaSecondaryButton(text = "Editar contactos de emergencia", onClick = { navigator.toEmergencyContacts() }) }
                }
                "security" -> {
                    item { MovaSecondaryButton(text = "Abrir centro de seguridad", onClick = { navigator.toSecurity() }) }
                }
                "sistema" -> {
                    item {
                        MovaInfoBanner(
                            message = "Con el rol del sistema, MOVA Phone sustituye por completo a la app de teléfono y a la de mensajes: " +
                                "las llamadas y los SMS son de MOVA, con su pantalla de llamada y su bandeja.",
                            tone = PillTone.Brand,
                            icon = Icons.Filled.PhoneAndroid
                        )
                    }
                    item {
                        SystemRoleCard(
                            title = "Aplicación de teléfono",
                            role = SystemRoles.DIALER,
                            icon = Icons.Filled.Call
                        )
                    }
                    item {
                        SystemRoleCard(
                            title = "Aplicación de mensajes",
                            role = SystemRoles.SMS,
                            icon = Icons.Filled.Message
                        )
                    }
                    item { MessageTransportCard() }
                    item { WearBridgeCard() }
                }
                "permisos" -> {
                    item {
                        MovaInfoBanner(
                            message = "MOVA Phone pide cada permiso en contexto. Aquí puedes revisarlos y conceder los que falten.",
                            tone = PillTone.Brand,
                            icon = Icons.Filled.VerifiedUser
                        )
                    }
                    item { MovaSecondaryButton(text = "Revisar permisos", onClick = { navigator.toPermissions() }) }
                }
                "privacy" -> {
                    item {
                        MovaSwitchRow(
                            title = "Guardar historial de llamadas",
                            checked = settings.storeCallHistory,
                            onCheckedChange = viewModel::setStoreCalls
                        )
                    }
                    item {
                        MovaSwitchRow(
                            title = "Guardar historial de ubicación",
                            subtitle = "Desactivado por defecto: sólo tú decides",
                            checked = settings.storeLocationHistory,
                            onCheckedChange = viewModel::setStoreLocation
                        )
                    }
                    item {
                        MovaInfoBanner(
                            message = "MOVA Phone funciona en local. No se envían datos personales a servidores externos.",
                            tone = PillTone.Success,
                            icon = Icons.Filled.Lock
                        )
                    }
                }
                "automation" -> {
                    item { MovaSwitchRow(title = "Automatizaciones activadas", checked = settings.automationsEnabled, onCheckedChange = viewModel::setAutomations) }
                    item { MovaSecondaryButton(text = "Gestionar automatizaciones", onClick = { navigator.toAutomation() }) }
                }
                "location" -> {
                    item { MovaSwitchRow(title = "Alta precisión (GPS)", checked = settings.highAccuracyLocation, onCheckedChange = viewModel::setHighAccuracy) }
                    item { MovaSwitchRow(title = "Compartir ubicación en emergencias", checked = settings.shareLocationOnSos, onCheckedChange = { }) }
                    item { MovaSecondaryButton(text = "Abrir ubicación", onClick = { navigator.toLocation() }) }
                }
                "notifications" -> {
                    item { MovaSwitchRow(title = "Notificaciones activadas", checked = settings.notificationsEnabled, onCheckedChange = viewModel::setNotifications) }
                    item { MovaSwitchRow(title = "Emergencias SOS", checked = settings.sosNotifications, onCheckedChange = viewModel::setSosNotifications) }
                    item { MovaSwitchRow(title = "Mensajes", checked = settings.messageNotifications, onCheckedChange = viewModel::setMessageNotifications) }
                    item { MovaSwitchRow(title = "Automatizaciones", checked = settings.automationNotifications, onCheckedChange = viewModel::setAutomationNotifications) }
                    item { MovaSwitchRow(title = "Seguridad", checked = settings.securityNotifications, onCheckedChange = viewModel::setSecurityNotifications) }
                }
                "accessibility" -> {
                    item { MovaSwitchRow(title = "Texto grande", checked = settings.largeText, onCheckedChange = viewModel::setLargeText) }
                    item { MovaSwitchRow(title = "Alto contraste", checked = settings.highContrast, onCheckedChange = viewModel::setHighContrast) }
                    item { MovaSwitchRow(title = "Reducir animaciones", checked = settings.reduceMotion, onCheckedChange = viewModel::setReduceMotion) }
                }
                "driving" -> {
                    item { MovaSwitchRow(title = "Activar modo conducción", checked = settings.drivingModeEnabled, onCheckedChange = viewModel::setDrivingMode) }
                    item { MovaSwitchRow(title = "Comandos de voz", checked = settings.drivingVoiceCommands, onCheckedChange = viewModel::setDrivingVoice) }
                    item { MovaSecondaryButton(text = "Abrir modo conducción", onClick = { navigator.toDriving() }) }
                }
                "appearance" -> {
                    item { MovaSwitchRow(title = "Tema oscuro", checked = settings.darkTheme, onCheckedChange = viewModel::setDarkTheme) }
                    item { MovaSwitchRow(title = "Seguir el tema del sistema", checked = settings.followSystemTheme, onCheckedChange = viewModel::setFollowSystemTheme) }
                }
                "data" -> {
                    item {
                        MovaListRow(
                            title = "Última copia de seguridad",
                            subtitle = if (settings.lastBackupAt > 0) TextFormatters.relativeDay(settings.lastBackupAt) else "Nunca"
                        )
                    }
                    item {
                        MovaInfoBanner(
                            message = "Los datos de MOVA Phone viven en tu dispositivo. La copia en la nube está desactivada por privacidad.",
                            tone = PillTone.Brand,
                            icon = Icons.Filled.Storage
                        )
                    }
                }
                "language" -> {
                    item { MovaListRow(title = "Español", subtitle = "Idioma actual de la interfaz") }
                    item { MovaInfoBanner(message = "Más idiomas llegarán en futuras versiones.", tone = PillTone.Neutral, icon = Icons.Filled.Language) }
                }
                else -> item { MovaInfoBanner(message = "Sección no reconocida.", tone = PillTone.Warning) }
            }
        }
    }
}

/**
 * Aviso honesto: por dónde salen hoy los mensajes.
 *
 * MOVA ya es la app de mensajes del sistema y los envía por la red de telefonía (SMS).
 * La mensajería por Internet (tipo WhatsApp) necesita un servidor propio que todavía no
 * existe: decirlo aquí evita prometer lo que no hay. Por dentro, la app ya tiene el
 * contrato MessageProvider listo para añadir ese transporte el día que haya servidor.
 */
@Composable
private fun MessageTransportCard() {
    MovaCard {
        MovaListRow(
            title = "Mensajes: SMS de tu operador",
            subtitle = "Salen por la red de telefonía, igual que en la app de mensajes del sistema. " +
                "Sin Internet de por medio y sin servidores de terceros.",
            leading = {
                androidx.compose.material3.Icon(
                    Icons.Filled.Message,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        MovaInfoBanner(
            message = "La mensajería por Internet todavía no está activa: necesita un servidor propio " +
                "que no se ha construido. MOVA ya tiene la pieza interna preparada para conectarla " +
                "cuando exista, y el día que la haya lo dirá aquí.",
            tone = PillTone.Neutral,
            icon = Icons.Filled.Info
        )
    }
}

/**
 * Reloj Wear OS: MOVA habla con la muñeca por Bluetooth, sin Google Play Services.
 * Desde aquí se enciende o apaga el puente y se piden los permisos que falten.
 */
@Composable
private fun WearBridgeCard() {
    val context = LocalContext.current
    val state by com.studiolexair.movaphone.services.wear.BridgeState.state.collectAsStateWithLifecycle()
    val permission = rememberPermissionHandle(
        listOf(
            MovaPermission.BLUETOOTH_CONNECT,
            MovaPermission.BLUETOOTH_ADVERTISE
        )
    )

    MovaCard {
        Column(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
            Text(
                text = "Reloj Wear OS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "MOVA lleva las llamadas, los mensajes y el SOS a tu reloj por Bluetooth directo: " +
                    "sin Google y sin nube. Instala la app «MOVA Reloj» en la muñeca y conecta aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MovaTheme.extra.textSecondary
            )
            Text(
                text = when {
                    state.connected -> "Conectado (${state.devices} dispositivo/s)"
                    state.running -> state.detail
                    else -> "Puente apagado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.connected) MovaTheme.extra.success else MovaTheme.extra.textMuted
            )
            if (!permission.granted) {
                MovaInfoBanner(
                    message = "Faltan permisos de Bluetooth para que el reloj pueda conectar.",
                    tone = PillTone.Warning,
                    icon = Icons.Filled.Bluetooth
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                MovaPrimaryButton(
                    text = if (state.running) "Detener el puente" else "Conectar con el reloj",
                    icon = Icons.Filled.Bluetooth,
                    onClick = {
                        if (state.running) {
                            com.studiolexair.movaphone.services.wear.MovaWearBridgeService.stop(context)
                        } else if (!permission.granted) {
                            permission.request()
                        } else {
                            com.studiolexair.movaphone.services.wear.MovaWearBridgeService.start(context)
                        }
                    }
                )
                if (state.running) {
                    MovaSecondaryButton(
                        text = "Permisos",
                        icon = Icons.Filled.Bluetooth,
                        onClick = { permission.request() }
                    )
                }
            }
        }
    }
}
