package com.studiolexair.movaphone.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSwitchRow
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.contacts.model.Contact

/**
 * Alta y edición de contactos.
 * Valida en el dominio (SaveContactUseCase) y avisa cuando falta información.
 */
@Composable
fun ContactEditRoute(
    contactId: Long?,
    navigator: MovaNavigator,
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = contactId?.takeIf { it > 0 }?.let { id -> state.contacts.firstOrNull { it.id == id } }

    ContactEditScreen(
        existing = existing,
        navigator = navigator,
        onSave = { name, phone, notes, group, favorite, private ->
            viewModel.save(name, phone, notes, group, favorite, private, existing?.id ?: 0L)
            navigator.back()
        },
        modifier = modifier
    )
}

@Composable
fun ContactEditScreen(
    existing: Contact?,
    navigator: MovaNavigator,
    onSave: (String, String, String?, String?, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(existing?.displayName.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phoneNumber.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var group by remember { mutableStateOf(existing?.groupName.orEmpty()) }
    var favorite by remember { mutableStateOf(existing?.isFavorite ?: false) }
    var private by remember { mutableStateOf(existing?.isPrivate ?: false) }

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = if (existing == null) "Nuevo contacto" else "Editar contacto",
                subtitle = if (existing == null) "Añade los datos básicos" else existing.displayName
            )
            Column(
                modifier = Modifier.padding(MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Número de teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Grupo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                MovaSwitchRow(
                    title = "Favorito",
                    subtitle = "Aparecerá en el acceso rápido",
                    checked = favorite,
                    onCheckedChange = { favorite = it }
                )
                MovaSwitchRow(
                    title = "Contacto privado",
                    subtitle = "Requiere desbloqueo para verlo en el modo privado",
                    checked = private,
                    onCheckedChange = { private = it }
                )
                MovaPrimaryButton(
                    text = if (existing == null) "Guardar contacto" else "Guardar cambios",
                    onClick = {
                        if (name.isNotBlank() || phone.isNotBlank()) {
                            onSave(name, phone, notes, group, favorite, private)
                        }
                    }
                )
            }
        }
    }
}
