package com.studiolexair.movaphone.feature.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaDangerButton
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSwitchRow
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/** Centro de seguridad: protección de la app, privacidad y control de accesos. */
@Composable
fun SecurityRoute(
    navigator: MovaNavigator,
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()
    val pinIsSet by viewModel.pinIsSet.collectAsStateWithLifecycle()
    var pinInput by remember { mutableStateOf("") }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MovaDimens.spaceLg,
                end = MovaDimens.spaceLg,
                bottom = MovaDimens.spaceXxl
            ),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item { MovaScreenHeader(title = "Seguridad", subtitle = "Protege tu información y privacidad") }

            message?.let { item { MovaInfoBanner(message = it, tone = PillTone.Brand, icon = Icons.Filled.Lock) } }

            item {
                MovaCard {
                    MovaSwitchRow(
                        title = "Bloqueo de la aplicación",
                        subtitle = "Pide autenticación al abrir MOVA Phone",
                        checked = settings.appLockEnabled,
                        onCheckedChange = viewModel::setAppLock
                    )
                    MovaSwitchRow(
                        title = "Biometría",
                        subtitle = biometricSubtitle(viewModel),
                        checked = settings.biometricEnabled,
                        onCheckedChange = viewModel::setBiometric
                    )
                    MovaSwitchRow(
                        title = "Modo privado",
                        subtitle = "Oculta contactos, historial y mensajes marcados como privados",
                        checked = settings.privateModeEnabled,
                        onCheckedChange = viewModel::setPrivateMode
                    )
                }
            }

            item {
                MovaCard {
                    Text(
                        text = if (pinIsSet) "PIN configurado" else "PIN de la aplicación",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "El PIN se guarda cifrado con el Android Keystore. MOVA Phone nunca almacena el PIN en texto plano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(vertical = MovaDimens.spaceSm)
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { value -> pinInput = value.filter { it.isDigit() }.take(8) },
                        label = { Text("PIN numérico (mínimo 4 dígitos)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                    MovaPrimaryButton(
                        text = if (pinIsSet) "Cambiar PIN" else "Guardar PIN",
                        icon = Icons.Filled.Pin,
                        enabled = pinInput.length >= 4,
                        onClick = {
                            viewModel.setPin(pinInput)
                            pinInput = ""
                        },
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                    if (pinIsSet) {
                        MovaDangerButton(
                            text = "Eliminar PIN",
                            icon = Icons.Filled.Pin,
                            onClick = viewModel::clearPin
                        )
                    }
                }
            }

            item {
                MovaListRow(
                    title = "Números bloqueados",
                    subtitle = "Gestiona el bloqueo de llamadas y SMS",
                    leading = { Icon(Icons.Filled.Block, contentDescription = null, tint = MovaTheme.extra.danger) },
                    onClick = { navigator.toBlockedNumbers() }
                )
            }
            item {
                MovaListRow(
                    title = "Contactos de confianza",
                    subtitle = "Personas autorizadas en caso de emergencia",
                    leading = { Icon(Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { navigator.toTrustedContacts() }
                )
            }
            item {
                MovaListRow(
                    title = "Registro de eventos",
                    subtitle = "Auditoría local de bloqueos, accesos y emergencias",
                    leading = { Icon(Icons.Filled.History, contentDescription = null, tint = MovaTheme.extra.violet) },
                    onClick = { navigator.toSecurityEvents() }
                )
            }
        }
    }
}

private fun biometricSubtitle(viewModel: SecurityViewModel): String = when (viewModel.biometricAvailability()) {
    com.studiolexair.movaphone.core.security.biometric.BiometricAvailability.AVAILABLE -> "Huella o rostro registrado en el dispositivo"
    com.studiolexair.movaphone.core.security.biometric.BiometricAvailability.NO_HARDWARE -> "Este dispositivo no tiene lector biométrico"
    com.studiolexair.movaphone.core.security.biometric.BiometricAvailability.NONE_ENROLLED -> "No hay datos biométricos registrados en el sistema"
    else -> "Biometría no disponible en este momento"
}

/** Números bloqueados por MOVA Phone (además del bloqueo del sistema). */
@Composable
fun BlockedNumbersRoute(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val blocked by viewModel.blockedNumbers.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(title = "Números bloqueados", subtitle = "Llamadas y SMS de estos números serán rechazados")
            Column(modifier = Modifier.padding(MovaDimens.spaceLg)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Número a bloquear") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Etiqueta (opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MovaDimens.spaceSm),
                    singleLine = true
                )
                MovaPrimaryButton(
                    text = "Bloquear número",
                    icon = Icons.Filled.Block,
                    enabled = input.length >= 4,
                    modifier = Modifier.padding(top = MovaDimens.spaceSm),
                    onClick = {
                        viewModel.blockNumber(input, label.ifBlank { null })
                        input = ""
                        label = ""
                    }
                )
            }

            if (blocked.isEmpty()) {
                MovaEmptyState(
                    title = "Sin números bloqueados",
                    description = "Puedes bloquear desde el marcador, el historial o aquí mismo.",
                    icon = Icons.Filled.Block
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(MovaDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(blocked, key = { it.id }) { entity ->
                        MovaListRow(
                            title = entity.phoneNumber,
                            subtitle = (entity.label ?: "sin etiqueta") + " · " + TextFormatters.relativeDay(entity.createdAt),
                            trailing = {
                                Text(
                                    text = "Desbloquear",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(MovaDimens.spaceSm)
                                )
                            },
                            onClick = { viewModel.unblock(entity) }
                        )
                    }
                }
            }
        }
    }
}

/** Contactos de confianza. */
@Composable
fun TrustedContactsRoute(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val trusted by viewModel.trustedContacts.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(title = "Contactos de confianza", subtitle = "Reciben información en caso de emergencia")
            Column(modifier = Modifier.padding(MovaDimens.spaceLg)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MovaDimens.spaceSm),
                    singleLine = true
                )
                MovaPrimaryButton(
                    text = "Añadir contacto de confianza",
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.padding(top = MovaDimens.spaceSm),
                    onClick = {
                        viewModel.addTrusted(name, phone)
                        name = ""
                        phone = ""
                    }
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                items(trusted, key = { it.id }) { entity ->
                    MovaListRow(
                        title = entity.name,
                        subtitle = entity.phoneNumber,
                        trailing = { Text("Quitar", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.danger) },
                        onClick = { viewModel.removeTrusted(entity) }
                    )
                }
            }
        }
    }
}

/** Registro de eventos de seguridad (auditoría local). */
@Composable
fun SecurityEventsRoute(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Eventos de seguridad",
                subtitle = "Todo lo relevante queda registrado en tu dispositivo",
                actions = {
                    Text(
                        text = "Vaciar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MovaTheme.extra.danger,
                        modifier = Modifier.padding(end = MovaDimens.spaceLg).clickable { viewModel.clearEvents() }
                    )
                }
            )
            if (events.isEmpty()) {
                MovaEmptyState(
                    title = "Sin eventos",
                    description = "Aquí verás bloqueos, accesos, cambios de PIN y emergencias.",
                    icon = Icons.Filled.History
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(MovaDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(events, key = { it.id }) { event ->
                        MovaListRow(
                            title = event.type.replace('_', ' '),
                            subtitle = "${event.description} · ${TextFormatters.relativeDay(event.occurredAt)}",
                            accent = when (event.severity) {
                                "critical" -> MovaTheme.extra.danger
                                "warning" -> MovaTheme.extra.warning
                                else -> null
                            }
                        )
                    }
                }
            }
        }
    }
}
