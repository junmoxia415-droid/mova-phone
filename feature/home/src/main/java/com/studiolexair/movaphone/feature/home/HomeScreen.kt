package com.studiolexair.movaphone.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaFeatureTile
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.common.util.SystemRoles
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaQuickAction
import com.studiolexair.movaphone.core.designsystem.component.MovaSearchField
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaSectionHeader
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.studiolexair.movaphone.core.designsystem.theme.MovaPalette
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import java.util.Calendar

/**
 * Pantalla de inicio: la central de operaciones del usuario.
 * Cada tarjeta navega a una función real; el saludo cambia según la hora.
 */
@Composable
fun HomeRoute(
    navigator: MovaNavigator,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Si MOVA todavía no es la app de teléfono, Android pone SU pantalla durante las llamadas.
    // Se avisa aquí, nada más abrir la aplicación, con el botón para arreglarlo en un toque.
    var roleRefresh by remember { mutableStateOf(0) }
    val holdsDialerRole = remember(roleRefresh) {
        SystemRoles.holdsRole(context, SystemRoles.DIALER)
    }
    val roleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { roleRefresh++ }

    HomeScreen(
        state = state,
        navigator = navigator,
        onQueryChange = viewModel::onQueryChange,
        onImportContacts = viewModel::importDeviceContacts,
        onSyncCalls = viewModel::syncCallLog,
        onDismissError = viewModel::dismissError,
        dialerRoleCard = if (holdsDialerRole || !state.isTelephonyAvailable) {
            null
        } else {
            {
                MovaCard {
                    Text(
                        text = "Que la pantalla de llamada sea la de MOVA",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ahora mismo Android pone la suya. Si le das a MOVA el papel de " +
                            "aplicación de teléfono, verás siempre la pantalla de MOVA: quién " +
                            "llama, contestar, colgar y tus últimas llamadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MovaTheme.extra.textSecondary,
                        modifier = Modifier.padding(top = MovaDimens.spaceXs)
                    )
                    MovaPrimaryButton(
                        text = "Activar MOVA como mi teléfono",
                        icon = Icons.Filled.PhoneAndroid,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm),
                        onClick = {
                            val intent = SystemRoles.requestIntent(context, SystemRoles.DIALER)
                            if (intent != null) roleLauncher.launch(intent) else roleRefresh++
                        }
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    navigator: MovaNavigator,
    onQueryChange: (String) -> Unit,
    onImportContacts: () -> Unit,
    onSyncCalls: () -> Unit,
    onDismissError: () -> Unit,
    /** Aviso del rol de teléfono: se pinta arriba del todo cuando hace falta. */
    dialerRoleCard: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val extra = MovaTheme.extra
    AuroraBackground(modifier = modifier) {
        dialerRoleCard?.invoke()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MovaDimens.spaceLg,
                end = MovaDimens.spaceLg,
                top = MovaDimens.spaceXl,
                bottom = MovaDimens.spaceXxl
            ),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item {
                Column {
                    Text(
                        text = "Hola, ${state.userName}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${greetingForNow()} · ${state.contactsCount} contactos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extra.textSecondary
                    )
                }
            }

            // Donde aparece la palabra MOVA, se abre el asistente: la marca es la puerta al chat.
            item {
                MovaListRow(
                    title = "MOVA",
                    subtitle = "Tu asistente: pídele lo que necesites con tu voz",
                    accent = MovaPalette.Violet,
                    leading = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MovaPalette.Violet
                        )
                    },
                    onClick = { navigator.toAssistant() }
                )
            }

            item {
                MovaSearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = "Buscar contactos, números o funciones..."
                )
            }

            if (!state.isTelephonyAvailable) {
                item {
                    MovaInfoBanner(
                        message = "Este dispositivo no admite llamadas telefónicas. Las funciones de comunicación seguirán disponibles cuando sea posible.",
                        tone = PillTone.Warning,
                        icon = Icons.Filled.Wifi
                    )
                }
            }

            state.errorMessage?.let { error ->
                item {
                    MovaInfoBanner(
                        message = error,
                        tone = PillTone.Warning,
                        icon = Icons.Filled.Wifi
                    )
                }
            }

            if (state.searchResults.isNotEmpty()) {
                item { MovaSectionHeader(text = "Resultados") }
                items(state.searchResults, key = { "search-${it.id}" }) { contact ->
                    MovaListRow(
                        title = contact.displayName,
                        subtitle = contact.phoneNumber,
                        leading = { MovaAvatar(initials = contact.initials) },
                        trailing = { Icon(Icons.Filled.Call, contentDescription = "Llamar", tint = MaterialTheme.colorScheme.primary) },
                        onClick = { navigator.toContactDetail(contact.id) }
                    )
                }
            }

            item { MovaSectionHeader(text = "Accesos directos") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)) {
                    MovaFeatureTile(
                        title = "Llamar",
                        subtitle = "Marcador",
                        icon = Icons.Filled.Call,
                        gradient = extra.tileGradientGreen,
                        onClick = { navigator.toDialer() },
                        modifier = Modifier.weight(1f)
                    )
                    MovaFeatureTile(
                        title = "Contactos",
                        subtitle = "${state.contactsCount} personas",
                        icon = Icons.Filled.Contacts,
                        gradient = extra.tileGradientBlue,
                        onClick = { navigator.toContacts() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)) {
                    MovaFeatureTile(
                        title = "Historial",
                        subtitle = if (state.missedCount > 0) "${state.missedCount} perdidas" else "Llamadas recientes",
                        icon = Icons.Filled.AccessTime,
                        gradient = extra.tileGradientViolet,
                        onClick = {
                            onSyncCalls()
                            navigator.toCalls()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MovaFeatureTile(
                        title = "Favoritos",
                        subtitle = "${state.favorites.size} contactos",
                        icon = Icons.Filled.Star,
                        gradient = extra.tileGradientAmber,
                        onClick = { navigator.toFavorites() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SosBanner(
                    ready = state.emergencyContactsCount > 0,
                    contactsCount = state.emergencyContactsCount,
                    onOpen = { navigator.toSos() },
                    onConfigure = { navigator.toEmergencyContacts() }
                )
            }

            item {
                MovaSectionHeader(
                    text = "Funciones rápidas",
                    trailing = {
                        Text(
                            text = "Personalizar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { navigator.toSettingsSection("appearance") }
                        )
                    }
                )
            }

            // Los atajos los elige el usuario en Ajustes → Apariencia: aquí se pintan tal cual.
            val shortcuts = state.shortcuts.ifEmpty {
                com.studiolexair.movaphone.core.navigation.HomeShortcuts.resolve(
                    com.studiolexair.movaphone.core.navigation.HomeShortcuts.serialize(
                        com.studiolexair.movaphone.core.navigation.HomeShortcuts.defaultIds
                    )
                )
            }
            items((shortcuts.size + 1) / 2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    shortcuts.drop(rowIndex * 2).take(2).forEach { shortcut ->
                        MovaQuickAction(
                            title = shortcut.label,
                            icon = iconForShortcut(shortcut.id),
                            accent = accentForShortcut(shortcut.id),
                            onClick = { navigator.navigateRoute(shortcut.route) }
                        )
                    }
                    // Si la fila queda con un solo atajo, se deja el hueco para que no se estire.
                    if (shortcuts.drop(rowIndex * 2).size == 1) {
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                MovaSectionHeader(
                    text = "Llamadas recientes",
                    trailing = {
                        Text(
                            text = "Ver todo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = MovaDimens.spaceXs)
                        )
                    }
                )
            }

            if (state.recentCalls.isEmpty()) {
                item {
                    MovaListRow(
                        title = "Aún no hay llamadas",
                        subtitle = if (state.isTelephonyAvailable) "Cuando llames o recibas llamadas aparecerán aquí." else "Este dispositivo no admite telefonía.",
                        leading = { Icon(Icons.Filled.AccessTime, contentDescription = null, tint = extra.textMuted) },
                        onClick = onSyncCalls
                    )
                }
            } else {
                items(state.recentCalls, key = { "call-${it.id}-${it.startedAt}" }) { call ->
                    MovaListRow(
                        title = call.displayName,
                        subtitle = "${call.number} · ${call.timestampLabel()}",
                        leading = {
                            MovaAvatar(
                                initials = call.displayName.take(2),
                                accent = if (call.isSpam) extra.danger else MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            if (call.isSpam) {
                                Text("Spam", color = extra.danger, style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(Icons.Filled.Call, contentDescription = null, tint = extra.success)
                            }
                        },
                        onClick = { navigator.toDialer(call.number) }
                    )
                }
            }

            item {
                MovaListRow(
                    title = "Importar contactos del dispositivo",
                    subtitle = "MOVA Phone sólo lee tu agenda; nunca la modifica",
                    leading = { Icon(Icons.Filled.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = onImportContacts
                )
            }
        }
    }
}

@Composable
private fun SosBanner(
    ready: Boolean,
    contactsCount: Int,
    onOpen: () -> Unit,
    onConfigure: () -> Unit
) {
    val extra = MovaTheme.extra
    Box(modifier = Modifier.fillMaxWidth()) {
        MovaListRow(
            title = if (ready) "Emergencia SOS lista" else "Configura tu emergencia",
            subtitle = if (ready) {
                "$contactsCount contactos de emergencia · mantén 3 segundos para activar"
            } else {
                "Añade al menos un contacto para que el protocolo SOS pueda avisar"
            },
            leading = {
                Icon(
                    imageVector = Icons.Filled.Emergency,
                    contentDescription = null,
                    tint = extra.danger
                )
            },
            accent = extra.danger,
            onClick = if (ready) onOpen else onConfigure
        )
    }
}

/** Saludo según la hora del dispositivo. */
private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

private fun com.studiolexair.movaphone.domain.calls.model.CallRecord.timestampLabel(): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = startedAt }
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    val isToday = System.currentTimeMillis() - startedAt < 86_400_000L
    return if (isToday) "Hoy $hour:$minute" else "$hour:$minute"
}


/** Icono de cada acceso directo (los identificadores están en [HomeShortcuts]). */
private fun iconForShortcut(id: String): ImageVector = when (id) {
    "llamar" -> Icons.Filled.Phone
    "mensajes" -> Icons.Filled.Message
    "contactos" -> Icons.Filled.Contacts
    "favoritos" -> Icons.Filled.Star
    "historial" -> Icons.Filled.History
    "seguridad" -> Icons.Filled.Shield
    "ubicacion" -> Icons.Filled.LocationOn
    "automatizar" -> Icons.Filled.Bolt
    "sos" -> Icons.Filled.Emergency
    "conduccion" -> Icons.Filled.DirectionsCar
    else -> Icons.Filled.Star
}

private fun accentForShortcut(id: String): Color = when (id) {
    "llamar" -> MovaPalette.Success
    "mensajes" -> MovaPalette.Cyan
    "contactos" -> MovaPalette.Blue
    "favoritos" -> MovaPalette.Warning
    "historial" -> MovaPalette.Indigo
    "seguridad" -> MovaPalette.Blue
    "ubicacion" -> MovaPalette.SkyBlue
    "automatizar" -> MovaPalette.Violet
    "sos" -> MovaPalette.Danger
    "conduccion" -> MovaPalette.Warning
    else -> MovaPalette.Violet
}
