package com.studiolexair.movaphone.feature.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.domain.emergency.model.EmergencyContact
import com.studiolexair.movaphone.domain.emergency.model.EmergencySession
import com.studiolexair.movaphone.domain.emergency.repository.EmergencyRepository
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator
import com.studiolexair.movaphone.domain.emergency.usecase.DeleteEmergencyContactUseCase
import com.studiolexair.movaphone.domain.emergency.usecase.ReorderEmergencyContactsUseCase
import com.studiolexair.movaphone.domain.emergency.usecase.SaveEmergencyContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EmergencyUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val session: EmergencySession? = null,
    val statusMessage: String? = null
) {
    val isActive: Boolean get() = session?.active == true
}

/**
 * SOS avanzado (requisito 12): activación por pulsación mantenida, cuenta regresiva
 * visible, estado real de cada paso y cancelación en cualquier momento.
 */
class EmergencyViewModel(
    private val repository: EmergencyRepository,
    private val orchestrator: SosOrchestrator,
    private val saveContact: SaveEmergencyContactUseCase,
    private val deleteContact: DeleteEmergencyContactUseCase,
    private val reorder: ReorderEmergencyContactsUseCase
) : ViewModel() {

    private val messageState = MutableStateFlow<String?>(null)

    val contacts: StateFlow<List<EmergencyContact>> = repository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val session: StateFlow<EmergencySession?> = orchestrator.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val statusMessage: StateFlow<String?> = messageState.asStateFlow()

    /** Inicia el protocolo real tras la confirmación por pulsación mantenida. */
    fun triggerSos() {
        viewModelScope.launch {
            try {
                orchestrator.start(trigger = "app")
            } catch (t: Throwable) {
                messageState.value = "No fue posible iniciar el protocolo de emergencia."
            }
        }
    }

    fun cancelSos() {
        viewModelScope.launch {
            orchestrator.cancel()
            messageState.value = "Emergencia cancelada"
        }
    }

    fun retryFailedSteps() {
        viewModelScope.launch { orchestrator.retryFailedSteps() }
    }

    fun saveContact(
        name: String,
        phone: String,
        relationship: String?,
        allowCall: Boolean,
        allowSms: Boolean,
        shareLocation: Boolean,
        existingId: Long = 0L,
        priority: Int = 0
    ) {
        viewModelScope.launch {
            val result = saveContact(
                EmergencyContact(
                    id = existingId,
                    name = name,
                    phoneNumber = phone,
                    priority = priority,
                    relationship = relationship,
                    allowCall = allowCall,
                    allowSms = allowSms,
                    shareLocation = shareLocation
                )
            )
            messageState.value = when (result) {
                is MovaResult.Success -> "Contacto de emergencia guardado"
                is MovaResult.Failure -> result.error.userMessage
            }
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            deleteContact(contact)
            messageState.value = "Contacto eliminado"
        }
    }

    /** Reordena prioridades: el primero recibe la llamada del protocolo SOS. */
    fun moveUp(contact: EmergencyContact) {
        val current = contacts.value
        val index = current.indexOfFirst { it.id == contact.id }
        if (index <= 0) return
        val reordered = current.toMutableList().apply {
            removeAt(index)
            add(index - 1, contact)
        }
        viewModelScope.launch { reorder(reordered) }
    }

    fun clearMessage() {
        messageState.value = null
    }

    companion object {
        fun factory(
            repository: EmergencyRepository,
            orchestrator: SosOrchestrator,
            saveContact: SaveEmergencyContactUseCase,
            deleteContact: DeleteEmergencyContactUseCase,
            reorder: ReorderEmergencyContactsUseCase
        ) = viewModelFactory {
            initializer { EmergencyViewModel(repository, orchestrator, saveContact, deleteContact, reorder) }
        }
    }
}
