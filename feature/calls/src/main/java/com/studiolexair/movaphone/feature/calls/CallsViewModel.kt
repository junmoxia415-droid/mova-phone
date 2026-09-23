package com.studiolexair.movaphone.feature.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.domain.calls.model.CallRecord
import com.studiolexair.movaphone.domain.calls.repository.CallsRepository
import com.studiolexair.movaphone.domain.calls.usecase.CallFilter
import com.studiolexair.movaphone.domain.calls.usecase.FilterCallLogUseCase
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallsUiState(
    val records: List<CallRecord> = emptyList(),
    val filtered: List<CallRecord> = emptyList(),
    val filter: CallFilter = CallFilter.ALL,
    val statusMessage: String? = null
)

/** Historial de llamadas con pestañas y acciones (requisito 11). */
class CallsViewModel(
    private val callsRepository: CallsRepository,
    private val contactsRepository: ContactsRepository,
    private val filterUseCase: FilterCallLogUseCase,
    private val onMarkSpam: suspend (String) -> Unit,
    private val onBlockNumber: suspend (String) -> Unit
) : ViewModel() {

    private val filterState = MutableStateFlow(CallFilter.ALL)
    private val messageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CallsUiState> = combine(
        callsRepository.observeCallLog(),
        filterState,
        messageState
    ) { records, filter, message ->
        CallsUiState(
            records = records,
            filtered = filterUseCase(records, filter),
            filter = filter,
            statusMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CallsUiState())

    fun setFilter(filter: CallFilter) {
        filterState.value = filter
    }

    fun syncFromDevice() {
        viewModelScope.launch {
            try {
                val imported = callsRepository.enrichFromDevice()
                messageState.value = if (imported > 0) "Historial actualizado ($imported llamadas)" else "El historial ya estaba actualizado"
            } catch (security: SecurityException) {
                messageState.value = "Necesitas conceder el permiso de historial de llamadas."
            } catch (t: Throwable) {
                messageState.value = "No fue posible actualizar el historial."
            }
        }
    }

    fun delete(record: CallRecord) {
        viewModelScope.launch {
            callsRepository.deleteCall(record.id)
            messageState.value = "Registro eliminado"
        }
    }

    fun block(record: CallRecord) {
        viewModelScope.launch {
            try {
                onBlockNumber(record.number)
                messageState.value = "${record.number} bloqueado"
            } catch (t: Throwable) {
                messageState.value = "No fue posible bloquear el número."
            }
        }
    }

    fun reportSpam(record: CallRecord) {
        viewModelScope.launch {
            onMarkSpam(record.number)
            messageState.value = "Número reportado como spam"
        }
    }

    /** Guarda un número desconocido como contacto nuevo. */
    fun saveAsContact(record: CallRecord) {
        viewModelScope.launch {
            try {
                contactsRepository.saveContact(
                    Contact(
                        displayName = record.number,
                        phoneNumber = record.number,
                        normalizedNumber = record.normalizedNumber
                    )
                )
                messageState.value = "Contacto guardado"
            } catch (t: Throwable) {
                messageState.value = "No fue posible guardar el contacto."
            }
        }
    }

    fun clearMessage() {
        messageState.value = null
    }

    companion object {
        fun factory(
            callsRepository: CallsRepository,
            contactsRepository: ContactsRepository,
            filterUseCase: FilterCallLogUseCase,
            onMarkSpam: suspend (String) -> Unit,
            onBlockNumber: suspend (String) -> Unit
        ) = viewModelFactory {
            initializer {
                CallsViewModel(callsRepository, contactsRepository, filterUseCase, onMarkSpam, onBlockNumber)
            }
        }
    }
}
