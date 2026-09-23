package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaQuickAction
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/**
 * Centro "Más" del mockup: todas las funciones de MOVA Phone a un toque,
 * además de la barra inferior con Inicio, Contactos e Historial.
 */
private data class HubAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MoreHubRoute(navigator: MovaNavigator, modifier: Modifier = Modifier) {
    val actions = listOf(
        HubAction("Mensajes", Icons.Filled.Message) { navigator.toMessages() },
        HubAction("Ubicación", Icons.Filled.LocationOn) { navigator.toLocation() },
        HubAction("Automatizaciones", Icons.Filled.Bolt) { navigator.toAutomation() },
        HubAction("Seguridad", Icons.Filled.Lock) { navigator.toSecurity() },
        HubAction("Asistente MOVA", Icons.Filled.AutoAwesome) { navigator.toAssistant() },
        HubAction("Modo conducción", Icons.Filled.DirectionsCar) { navigator.toDriving() },
        HubAction("SOS", Icons.Filled.Emergency) { navigator.toSos() },
        HubAction("Favoritos", Icons.Filled.Star) { navigator.toFavorites() },
        HubAction("Historial", Icons.Filled.Call) { navigator.toCalls() },
        HubAction("Contactos", Icons.Filled.Contacts) { navigator.toContacts() },
        HubAction("Ajustes", Icons.Filled.Settings) { navigator.toSettings() }
    )
    val accents = listOf(
        MovaTheme.extra.tileGradientBlue, MovaTheme.extra.tileGradientGreen,
        MovaTheme.extra.tileGradientViolet, MovaTheme.extra.tileGradientAmber
    )

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceLg)
        ) {
            item {
                MovaScreenHeader(title = "Más", subtitle = "Todas las funciones de MOVA Phone")
            }
            items((actions.size + 1) / 2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
                ) {
                    val first = actions[rowIndex * 2]
                    MovaQuickAction(
                        title = first.title,
                        icon = first.icon,
                        accent = accents[rowIndex % accents.size].first(),
                        onClick = first.onClick,
                        modifier = Modifier.weight(1f)
                    )
                    val secondIndex = rowIndex * 2 + 1
                    if (secondIndex < actions.size) {
                        val second = actions[secondIndex]
                        MovaQuickAction(
                            title = second.title,
                            icon = second.icon,
                            accent = accents[(rowIndex + 2) % accents.size].first(),
                            onClick = second.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Column(modifier = Modifier.weight(1f).padding(MovaDimens.spaceSm)) {}
                    }
                }
            }
        }
    }
}
