package com.studiolexair.movaphone.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.repository.CallsRepository
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla principal. */
data class HomeUiState(
    val userName: String = "Usuario",
    val recentCalls: List<CallRecord> = emptyList(),
    val favorites: List<Contact> = emptyList(),
    val contactsCount: Int = 0,
    val missedCount: Int = 0,
    val emergencyContactsCount: Int = 0,
    val query: String = "",
    val searchResults: List<Contact> = emptyList(),
    val isTelephonyAvailable: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Central de operaciones personal (requisito 7).
 * Reúne en un único estado: saludo, accesos rápidos, llamadas recientes,
 * favoritos, estado de emergencia y búsqueda universal (contactos + números + funciones).
 */
class HomeViewModel(
    private val callsRepository: CallsRepository,
    private val contactsRepository: ContactsRepository,
    private val emergencyRepository: EmergencyRepository,
    telephonyAvailable: Boolean
) : ViewModel() {

    private val queryState = MutableStateFlow("")
    private val errorState = MutableStateFlow<String?>(null)

    private data class CoreData(
        val recent: List<CallRecord>,
        val favorites: List<Contact>,
        val contacts: List<Contact>,
        val missed: Int
    )

    private data class ExtraData(val emergencyCount: Int, val query: String, val error: String?)

    private val coreFlow = combine(
        callsRepository.observeRecentCalls(6),
        contactsRepository.observeFavorites(),
        contactsRepository.observeContacts(),
        callsRepository.observeMissedCount()
    ) { recent, favorites, contacts, missed -> CoreData(recent, favorites, contacts, missed) }

    private val extraFlow = combine(emergencyRepository.observeContacts(), queryState, errorState) { emergency, query, error ->
        ExtraData(emergency.size, query, error)
    }

    val uiState: StateFlow<HomeUiState> = combine(coreFlow, extraFlow) { core, extra ->
        HomeUiState(
            recentCalls = core.recent,
            favorites = core.favorites,
            contactsCount = core.contacts.size,
            missedCount = core.missed,
            emergencyContactsCount = extra.emergencyCount,
            query = extra.query,
            searchResults = if (extra.query.isBlank()) emptyList() else core.contacts.filter {
                it.displayName.contains(extra.query, ignoreCase = true) || it.phoneNumber.contains(extra.query)
            }.take(6),
            isTelephonyAvailable = telephonyAvailable,
            errorMessage = extra.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        queryState.value = value
    }

    fun clearQuery() {
        queryState.value = ""
    }

    /** Importa la agenda del dispositivo (acción explícita del usuario). */
    fun importDeviceContacts() {
        viewModelScope.launch {
            try {
                val imported = contactsRepository.importFromDevice()
                errorState.value = if (imported > 0) null else "No hay contactos nuevos por importar."
            } catch (security: SecurityException) {
                errorState.value = "MOVA Phone necesita permiso para leer tus contactos."
            } catch (t: Throwable) {
                errorState.value = "No fue posible importar los contactos. Inténtalo nuevamente."
            }
        }
    }

    /** Sincroniza el historial del sistema con el historial propio. */
    fun syncCallLog() {
        viewModelScope.launch {
            try {
                callsRepository.enrichFromDevice()
            } catch (security: SecurityException) {
                errorState.value = "MOVA Phone necesita permiso para leer el historial de llamadas."
            } catch (t: Throwable) {
                errorState.value = "No fue posible actualizar el historial."
            }
        }
    }

    fun dismissError() {
        errorState.value = null
    }

    companion object {
        fun factory(
            callsRepository: CallsRepository,
            contactsRepository: ContactsRepository,
            emergencyRepository: EmergencyRepository,
            telephonyAvailable: Boolean
        ) = viewModelFactory {
            initializer { HomeViewModel(callsRepository, contactsRepository, emergencyRepository, telephonyAvailable) }
        }
    }
}
