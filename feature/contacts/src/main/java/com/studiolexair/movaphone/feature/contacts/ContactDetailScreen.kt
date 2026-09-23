package com.studiolexair.movaphone.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaCard
import com.studiolexair.movaphone.core.designsystem.component.MovaDangerButton
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.contacts.model.Contact

/** Detalle del contacto con las acciones reales del producto. */
@Composable
fun ContactDetailRoute(
    contactId: Long,
    navigator: MovaNavigator,
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val selected by viewModel.selectedContact.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contact = selected ?: state.contacts.firstOrNull { it.id == contactId }

    if (contact == null) {
        AuroraBackground(modifier = modifier) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                MovaScreenHeader(title = "Contacto", subtitle = "No se encontró el contacto")
                MovaSecondaryButton(
                    text = "Volver a contactos",
                    onClick = { navigator.toContacts() },
                    modifier = Modifier.padding(MovaDimens.spaceLg)
                )
            }
        }
        return
    }

    ContactDetailScreen(
        contact = contact,
        navigator = navigator,
        onToggleFavorite = { viewModel.toggleFavorite(contact) },
        onTogglePrivate = { viewModel.togglePrivate(contact) },
        onDelete = { viewModel.delete(contact) },
        modifier = modifier
    )
}

@Composable
fun ContactDetailScreen(
    contact: Contact,
    navigator: MovaNavigator,
    onToggleFavorite: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(title = "Contacto", subtitle = contact.displayName)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MovaDimens.spaceLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                MovaAvatar(initials = contact.initials, modifier = Modifier.padding(bottom = MovaDimens.spaceSm))
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MovaTheme.extra.textSecondary
                )
                Text(
                    text = if (contact.isFavorite) "⭐ Favorito" else "Sin marcar como favorito",
                    style = MaterialTheme.typography.labelMedium,
                    color = MovaTheme.extra.textSecondary
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = MovaDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                MovaPrimaryButton(text = "Llamar", icon = Icons.Filled.Call, onClick = { navigator.toDialer(contact.phoneNumber) })
                MovaSecondaryButton(text = "Enviar mensaje", icon = Icons.Filled.Message, onClick = { navigator.toConversation(contact.phoneNumber) })
                MovaSecondaryButton(text = "Editar contacto", icon = Icons.Filled.Edit, onClick = { navigator.toContactEdit(contact.id) })
                MovaSecondaryButton(
                    text = if (contact.isFavorite) "Quitar de favoritos" else "Marcar como favorito",
                    icon = Icons.Filled.Star,
                    onClick = onToggleFavorite
                )
                MovaSecondaryButton(
                    text = if (contact.isPrivate) "Quitar de privados" else "Marcar como privado",
                    icon = Icons.Filled.Lock,
                    onClick = onTogglePrivate
                )
                MovaSecondaryButton(
                    text = "Bloquear número",
                    icon = Icons.Filled.Block,
                    accent = MovaTheme.extra.warning,
                    onClick = { navigator.toBlockedNumbers() }
                )
                MovaDangerButton(text = "Eliminar contacto", icon = Icons.Filled.Delete, onClick = onDelete)
            }

            if (!contact.notes.isNullOrBlank() || !contact.groupName.isNullOrBlank()) {
                MovaCard(modifier = Modifier.padding(MovaDimens.spaceLg)) {
                    Text(
                        text = "Información",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    contact.groupName?.let {
                        MovaListRow(title = "Grupo", subtitle = it)
                    }
                    contact.notes?.let {
                        MovaListRow(title = "Notas", subtitle = it)
                    }
                }
            }
        }
    }
}
