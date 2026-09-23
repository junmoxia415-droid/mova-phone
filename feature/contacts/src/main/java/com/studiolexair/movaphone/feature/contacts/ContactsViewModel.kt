package com.studiolexair.movaphone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import com.studiolexair.movaphone.domain.contacts.usecase.DeleteContactUseCase
import com.studiolexair.movaphone.domain.contacts.usecase.SaveContactUseCase
import com.studiolexair.movaphone.domain.contacts.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ContactTab { ALL, FAVORITES, PRIVATE }

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val tab: ContactTab = ContactTab.ALL,
    val query: String = "",
    val selected: Contact? = null,
    val message: String? = null
)

/** Contactos: lista, búsqueda, favoritos, privados, alta/edición y borrado (requisito 10). */
class ContactsViewModel(
    private val repository: ContactsRepository,
    private val saveContact: SaveContactUseCase,
    private val deleteContact: DeleteContactUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val tabState = MutableStateFlow(ContactTab.ALL)
    private val queryState = MutableStateFlow("")
    private val selectedState = MutableStateFlow<Contact?>(null)
    private val messageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ContactsUiState> = combine(
        repository.observeContacts(),
        tabState,
        queryState
    ) { contacts, tab, query ->
        val filteredByTab = when (tab) {
            ContactTab.ALL -> contacts
            ContactTab.FAVORITES -> contacts.filter { it.isFavorite }
            ContactTab.PRIVATE -> contacts.filter { it.isPrivate }
        }
        val searched = if (query.isBlank()) filteredByTab else filteredByTab.filter {
            it.displayName.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
        }
        ContactsUiState(contacts = searched, tab = tab, query = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    val selectedContact: StateFlow<Contact?> = selectedState

    val statusMessage: StateFlow<String?> = messageState

    fun setTab(tab: ContactTab) {
        tabState.value = tab
    }

    fun onQueryChange(value: String) {
        queryState.value = value
    }

    fun select(contact: Contact?) {
        selectedState.value = contact
    }

    fun save(
        name: String,
        phone: String,
        notes: String?,
        group: String?,
        isFavorite: Boolean,
        isPrivate: Boolean,
        existingId: Long = 0
    ) {
        viewModelScope.launch {
            val result = saveContact(
                Contact(
                    id = existingId,
                    displayName = name,
                    phoneNumber = phone,
                    normalizedNumber = "",
                    notes = notes?.ifBlank { null },
                    groupName = group?.ifBlank { null },
                    isFavorite = isFavorite,
                    isPrivate = isPrivate
                )
            )
            messageState.value = when (result) {
                is MovaResult.Success -> "Contacto guardado"
                is MovaResult.Failure -> result.error.userMessage
            }
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch {
            when (val result = deleteContact(contact)) {
                is MovaResult.Success -> messageState.value = "Contacto eliminado"
                is MovaResult.Failure -> messageState.value = result.error.userMessage
            }
            selectedState.value = null
        }
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch { toggleFavorite(contact.id, !contact.isFavorite) }
    }

    fun togglePrivate(contact: Contact) {
        viewModelScope.launch {
            repository.setPrivate(contact.id, !contact.isPrivate)
            messageState.value = if (!contact.isPrivate) "Contacto marcado como privado" else "Contacto ya no es privado"
        }
    }

    fun importDeviceContacts() {
        viewModelScope.launch {
            try {
                val imported = repository.importFromDevice()
                messageState.value = if (imported > 0) "$imported contactos importados" else "No hay contactos nuevos"
            } catch (security: SecurityException) {
                messageState.value = "MOVA Phone necesita permiso para leer tus contactos."
            } catch (t: Throwable) {
                messageState.value = "No fue posible importar los contactos."
            }
        }
    }

    fun clearMessage() {
        messageState.value = null
    }

    companion object {
        fun factory(
            repository: ContactsRepository,
            saveContact: SaveContactUseCase,
            deleteContact: DeleteContactUseCase,
            toggleFavorite: ToggleFavoriteUseCase
        ) = viewModelFactory {
            initializer { ContactsViewModel(repository, saveContact, deleteContact, toggleFavorite) }
        }
    }
}
