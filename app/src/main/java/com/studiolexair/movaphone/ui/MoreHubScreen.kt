package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSectionHeader
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.navigation.MovaNavigator

/**
 * Centro «Más», rehecho según lo que pidió el usuario: el 1.1 tenía 14 cuadros sueltos en
 * una cuadrícula y costaba encontrar las cosas. Ahora son **cuatro grupos con nombre** y
 * filas normales, de modo que se lee de un vistazo y todo está a un toque.
 *
 * Lo que ya vive en la barra inferior (Contactos, Historial, Mensajes) no se repite aquí.
 */
private data class HubEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MoreHubRoute(navigator: MovaNavigator, modifier: Modifier = Modifier) {
    // Cada grupo responde a "¿para qué entro aquí?"
    val groups: List<Pair<String, List<HubEntry>>> = listOf(
        "Llamar y escribir" to listOf(
            HubEntry("Marcar y llamar", "Teclado, contactos y llamada", Icons.Filled.Phone) {
                navigator.toDialer()
            },
            HubEntry("Favoritos", "Tus personas de siempre", Icons.Filled.Star) {
                navigator.toFavorites()
            },
            HubEntry("Mensajes", "Conversaciones y SMS", Icons.Filled.Message) {
                navigator.toMessages()
            }
        ),
        "Seguridad y emergencia" to listOf(
            HubEntry("Centro de seguridad", "Bloqueo, spam y privacidad", Icons.Filled.Lock) {
                navigator.toSecurity()
            },
            HubEntry("SOS", "Ayuda inmediata a tus contactos", Icons.Filled.Emergency) {
                navigator.toSos()
            },
            HubEntry("Ubicación", "Dónde estás y compartir con quien quieras", Icons.Filled.LocationOn) {
                navigator.toLocation()
            },
            HubEntry("Permisos", "Qué puede hacer MOVA y por qué", Icons.Filled.VerifiedUser) {
                navigator.toPermissions()
            }
        ),
        "Asistente MOVA" to listOf(
            HubEntry("Hablar con MOVA", "Pídele cosas con tu voz o por escrito", Icons.Filled.AutoAwesome) {
                navigator.toAssistant()
            },
            HubEntry("Modo conducción", "Manos libres en el coche", Icons.Filled.DirectionsCar) {
                navigator.toDriving()
            },
            HubEntry("Automatizaciones", "Que MOVA haga cosas por ti", Icons.Filled.Bolt) {
                navigator.toAutomation()
            }
        ),
        "Aplicación" to listOf(
            HubEntry("Ajustes", "Todo lo configurable, ordenado por temas", Icons.Filled.Settings) {
                navigator.toSettings()
            },
            HubEntry("Acerca de MOVA Phone", "Versión, licencias y privacidad", Icons.Filled.Info) {
                navigator.toAbout()
            }
        )
    )

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
        ) {
            item {
                MovaScreenHeader(
                    title = "Más",
                    subtitle = "Todo lo que MOVA puede hacer por ti"
                )
            }
            item {
                MovaInfoBanner(
                    message = "Aquí sólo lo que no está en la barra de abajo. La barra te lleva " +
                        "siempre a Inicio, Contactos, Historial y Mensajes.",
                    tone = PillTone.Brand
                )
            }
            groups.forEach { (title, entries) ->
                item {
                    MovaSectionHeader(
                        text = title,
                        modifier = Modifier.padding(top = MovaDimens.spaceSm)
                    )
                }
                entries.forEach { entry ->
                    item {
                        MovaListRow(
                            title = entry.title,
                            subtitle = entry.subtitle,
                            leading = {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = entry.onClick
                        )
                    }
                }
            }
        }
    }
}
