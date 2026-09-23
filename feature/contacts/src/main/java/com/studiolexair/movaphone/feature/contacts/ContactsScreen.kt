package com.studiolexair.movaphone.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.MovaSearchField
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.contacts.model.Contact

/** Lista de contactos con pestañas (Todos · Favoritos · Privados) y acciones rápidas. */
@Composable
fun ContactsRoute(
    navigator: MovaNavigator,
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ContactsScreen(
        state = state,
        navigator = navigator,
        onTabChange = viewModel::setTab,
        onQueryChange = viewModel::onQueryChange,
        onToggleFavorite = viewModel::toggleFavorite,
        onNewContact = { navigator.toContactEdit(null) },
        onImport = viewModel::importDeviceContacts,
        modifier = modifier
    )
}

@Composable
fun ContactsScreen(
    state: ContactsUiState,
    navigator: MovaNavigator,
    onTabChange: (ContactTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onNewContact: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Contactos",
                subtitle = "Gestiona tus contactos fácilmente",
                actions = {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Nuevo contacto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = MovaDimens.spaceSm)
                    )
                }
            )

            Column(modifier = Modifier.padding(horizontal = MovaDimens.spaceLg)) {
                MovaSearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = "Buscar contactos..."
                )
                Row(
                    modifier = Modifier.padding(top = MovaDimens.spaceSm),
                    horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    listOf(
                        ContactTab.ALL to "Todos",
                        ContactTab.FAVORITES to "Favoritos",
                        ContactTab.PRIVATE to "Privados"
                    ).forEach { (tab, label) ->
                        AssistChip(
                            onClick = { onTabChange(tab) },
                            label = { Text(label) },
                            colors = if (tab == state.tab) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
            }

            if (state.contacts.isEmpty()) {
                MovaEmptyState(
                    title = "Sin contactos aquí",
                    description = "Puedes crear un contacto nuevo o importar tu agenda del dispositivo (MOVA Phone sólo lee, nunca modifica).",
                    icon = Icons.Filled.PersonAdd,
                    action = {
                        com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton(
                            text = "Crear contacto",
                            onClick = onNewContact,
                            icon = Icons.Filled.PersonAdd
                        )
                    }
                )
                com.studiolexair.movaphone.core.designsystem.component.MovaSecondaryButton(
                    text = "Importar agenda del dispositivo",
                    onClick = onImport,
                    modifier = Modifier.padding(horizontal = MovaDimens.spaceLg)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MovaDimens.spaceLg,
                        end = MovaDimens.spaceLg,
                        top = MovaDimens.spaceSm,
                        bottom = MovaDimens.spaceXxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(state.contacts, key = { it.id }) { contact ->
                        MovaListRow(
                            title = contact.displayName,
                            subtitle = listOfNotNull(
                                contact.phoneNumber,
                                contact.groupName,
                                contact.notes?.take(24)
                            ).joinToString(" · "),
                            leading = { MovaAvatar(initials = contact.initials) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "Favorito",
                                        tint = if (contact.isFavorite) MovaTheme.extra.warning else MovaTheme.extra.textMuted
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Llamar",
                                        tint = MovaTheme.extra.success,
                                        modifier = Modifier.padding(start = MovaDimens.spaceSm)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Message,
                                        contentDescription = "Mensaje",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = MovaDimens.spaceSm)
                                    )
                                }
                            },
                            onClick = { navigator.toContactDetail(contact.id) }
                        )
                    }
                }
            }
        }
    }
}
