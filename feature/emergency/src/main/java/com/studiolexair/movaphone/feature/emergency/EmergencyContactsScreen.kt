package com.studiolexair.movaphone.feature.emergency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSwitchRow
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme

/**
 * Contactos de emergencia con prioridad reordenable (requisito 13).
 * El primero de la lista es a quien se llama durante el SOS.
 */
@Composable
fun EmergencyContactsRoute(
    viewModel: EmergencyViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var allowCall by remember { mutableStateOf(true) }
    var allowSms by remember { mutableStateOf(true) }
    var shareLocation by remember { mutableStateOf(true) }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Contactos de emergencia",
                subtitle = "Orden de prioridad · 1 = primero"
            )

            if (contacts.isEmpty()) {
                MovaEmptyState(
                    title = "Sin contactos de emergencia",
                    description = "Añade al menos una persona de confianza. Sin contactos, el protocolo SOS no puede avisar a nadie.",
                    icon = Icons.Filled.Emergency
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = MovaDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        MovaListRow(
                            title = "${contact.priority}° · ${contact.name}",
                            subtitle = listOfNotNull(
                                contact.phoneNumber,
                                contact.relationship,
                                if (contact.allowCall) "llamada" else null,
                                if (contact.allowSms) "SMS" else null,
                                if (contact.shareLocation) "ubicación" else null
                            ).joinToString(" · "),
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = "Subir prioridad",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = MovaDimens.spaceSm)
                                            .androidxClickable { viewModel.moveUp(contact) }
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MovaTheme.extra.danger,
                                        modifier = Modifier.androidxClickable { viewModel.deleteContact(contact) }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MovaTheme.extra.success,
                    modifier = Modifier.padding(horizontal = MovaDimens.spaceLg, vertical = MovaDimens.spaceSm)
                )
            }

            Column(
                modifier = Modifier.padding(MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                Text(
                    text = "Añadir contacto de emergencia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relación (mamá, pareja, médico...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MovaSwitchRow(
                    title = "Permitir llamada",
                    checked = allowCall,
                    onCheckedChange = { allowCall = it }
                )
                MovaSwitchRow(
                    title = "Permitir SMS con mi ubicación",
                    checked = allowSms,
                    onCheckedChange = { allowSms = it }
                )
                MovaSwitchRow(
                    title = "Compartir ubicación",
                    checked = shareLocation,
                    onCheckedChange = { shareLocation = it }
                )
                MovaPrimaryButton(
                    text = "Guardar contacto",
                    icon = Icons.Filled.Add,
                    enabled = name.isNotBlank() && phone.length >= 4,
                    onClick = {
                        viewModel.saveContact(name, phone, relationship, allowCall, allowSms, shareLocation)
                        name = ""
                        phone = ""
                        relationship = ""
                    }
                )
            }
        }
    }
}

private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier =
    this.padding(4.dp).clickable(onClick = onClick)
