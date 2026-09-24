package com.studiolexair.movaphone.feature.automation

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.permissions.MovaPermission
import com.studiolexair.movaphone.core.permissions.rememberPermissionHandle
import com.studiolexair.movaphone.data.automation.geofence.GeofencePlace

/**
 * Lugares guardados: base de las automatizaciones «Llego a un lugar» y «Salgo de un lugar».
 *
 * Todo se calcula en el teléfono (sin Google Play Services) y con el permiso de ubicación
 * que el usuario concede aquí mismo, en contexto.
 */
@Composable
fun PlacesRoute(
    navigator: MovaNavigator,
    viewModel: PlacesViewModel,
    modifier: Modifier = Modifier
) {
    val places by viewModel.places.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val locationPermission = rememberPermissionHandle(
        listOf(MovaPermission.FINE_LOCATION, MovaPermission.COARSE_LOCATION)
    )

    var showAddDialog by remember { mutableStateOf(false) }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Lugares",
                subtitle = "Entrar o salir de un sitio, sin servicios de Google",
                actions = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MovaDimens.spaceLg,
                    end = MovaDimens.spaceLg,
                    top = MovaDimens.spaceSm,
                    bottom = MovaDimens.spaceXxl
                ),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                item {
                    MovaInfoBanner(
                        message = "MOVA comprueba tus lugares en el propio teléfono: cada 15 minutos y cada vez " +
                            "que recibe tu posición. Al entrar o salir se disparan tus reglas de automatización.",
                        tone = PillTone.Brand,
                        icon = Icons.Filled.MyLocation
                    )
                }

                if (!locationPermission.granted) {
                    item {
                        MovaInfoBanner(
                            message = "Falta el permiso de ubicación. Sin él no se puede saber cuándo llegas o sales.",
                            tone = PillTone.Warning,
                            icon = Icons.Filled.Place
                        )
                    }
                    item {
                        MovaPrimaryButton(
                            text = "Permitir la ubicación",
                            icon = Icons.Filled.MyLocation,
                            onClick = { locationPermission.request() }
                        )
                    }
                }

                item {
                    MovaPrimaryButton(
                        text = if (busy) "Comprobando…" else "Añadir el lugar donde estoy",
                        icon = Icons.Filled.Place,
                        enabled = !busy,
                        onClick = { showAddDialog = true }
                    )
                }

                message?.let { text ->
                    item { MovaInfoBanner(message = text, tone = PillTone.Neutral, icon = Icons.Filled.Check) }
                }

                if (places.isEmpty()) {
                    item {
                        MovaEmptyState(
                            title = "Aún no hay lugares",
                            description = "Guarda «Casa», «Trabajo» o el sitio que quieras y crea reglas para cuando llegues o salgas.",
                            icon = Icons.Filled.Place
                        )
                    }
                } else {
                    items(places, key = { it.id }) { place ->
                        PlaceRow(
                            place = place,
                            onToggle = { viewModel.toggle(place) },
                            onDelete = { viewModel.delete(place) },
                            onRadius = { radius -> viewModel.updateRadius(place, radius) }
                        )
                    }
                    item {
                        MovaPrimaryButton(
                            text = if (busy) "Comprobando…" else "Comprobar ahora",
                            icon = Icons.Filled.MyLocation,
                            enabled = !busy,
                            onClick = { viewModel.checkNow() }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, radius ->
                viewModel.addPlaceHere(name, radius)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PlaceRow(
    place: GeofencePlace,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onRadius: (Double) -> Unit
) {
    val radii = listOf(100.0, 200.0, 500.0, 1000.0)
    Column {
        MovaListRow(
            title = place.name,
            subtitle = buildString {
                append("Radio ").append(place.radiusMeters.toInt()).append(" m · ")
                append(TextFormatters.coordinates(place.latitude, place.longitude))
                append(if (place.lastInside == true) " · dentro" else " · fuera")
            },
            leading = { Icon(Icons.Filled.Place, contentDescription = null, tint = MovaTheme.extra.violet) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = place.enabled, onCheckedChange = { onToggle() })
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar lugar", tint = MovaTheme.extra.danger)
                    }
                }
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs)) {
            radii.forEach { radius ->
                FilterChip(
                    selected = place.radiusMeters == radius,
                    onClick = { onRadius(radius) },
                    label = { Text(if (radius >= 1000) "${(radius / 1000).toInt()} km" else "${radius.toInt()} m") }
                )
            }
        }
    }
}

@Composable
private fun AddPlaceDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(200.0) }
    val options = listOf(100.0, 200.0, 500.0, 1000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo lugar", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)) {
                Text("Se guardará tu posición actual como centro del lugar.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (Casa, Trabajo…)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Radio de aviso", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = radius == option,
                            onClick = { radius = option },
                            label = { Text(if (option >= 1000) "${(option / 1000).toInt()} km" else "${option.toInt()} m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, radius) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        modifier = Modifier.padding(MovaDimens.spaceSm)
    )
}
