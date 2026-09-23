package com.studiolexair.movaphone.feature.favorites

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaAvatar
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.remember

/**
 * Favoritos: acceso rápido a las personas más importantes (llamada y SMS en un toque).
 */
@Composable
fun FavoritesRoute(
    navigator: MovaNavigator,
    contactsRepository: ContactsRepository,
    modifier: Modifier = Modifier
) {
    val favoritesFlow = remember(contactsRepository) {
        contactsRepository.observeFavorites()
    }
    val favorites by favoritesFlow.collectAsState(initial = emptyList())
    FavoritesScreen(favorites = favorites, navigator = navigator, modifier = modifier)
}

@Composable
fun FavoritesScreen(
    favorites: List<Contact>,
    navigator: MovaNavigator,
    modifier: Modifier = Modifier
) {
    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Favoritos",
                subtitle = "Acceso rápido a tus personas importantes"
            )

            if (favorites.isEmpty()) {
                MovaEmptyState(
                    title = "Todavía no hay favoritos",
                    description = "Marca contactos como favoritos y aparecerán aquí, listos para llamar o escribir.",
                    icon = Icons.Filled.Star
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MovaDimens.spaceLg,
                        end = MovaDimens.spaceLg,
                        bottom = MovaDimens.spaceXxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(favorites, key = { it.id }) { contact ->
                        MovaListRow(
                            title = contact.displayName,
                            subtitle = contact.phoneNumber,
                            leading = { MovaAvatar(initials = contact.initials, accent = MovaTheme.extra.warning) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Llamar",
                                        tint = MovaTheme.extra.success,
                                        modifier = Modifier.padding(end = MovaDimens.spaceSm)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Message,
                                        contentDescription = "Mensaje",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = { navigator.toDialer(contact.phoneNumber) }
                        )
                    }
                }
            }
        }
    }
}
