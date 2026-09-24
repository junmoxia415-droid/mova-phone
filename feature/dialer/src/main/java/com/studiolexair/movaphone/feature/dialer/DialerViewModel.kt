package com.studiolexair.movaphone.feature.dialer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.domain.calls.model.SpamVerdict
import com.studiolexair.movaphone.domain.calls.usecase.PlaceCallUseCase
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import com.studiolexair.movaphone.domain.contacts.usecase.SaveContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DialerUiState(
    val input: String = "",
    val formatted: String = "",
    val suggestions: List<Contact> = emptyList(),
    val spamVerdict: SpamVerdict? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val canCall: Boolean = false,
    /** Ultimo numero al que se ha llamado desde MOVA (para poder repetir de un toque). */
    val lastDialed: String? = null,
    /** Nombre del contacto de esa ultima llamada, si se conoce. */
    val lastDialedName: String? = null
)

/**
 * Marcador moderno (requisito 9):
 * búsqueda mientras se escribe, sugerencias de contactos, llamada rápida,
 * guardar contacto, copiar, bloquear y reportar spam.
 */
class DialerViewModel(
    private val contactsRepository: ContactsRepository,
    private val callsRepository: com.studiolexair.movaphone.domain.calls.repository.CallsRepository,
    private val placeCall: PlaceCallUseCase,
    private val saveContact: SaveContactUseCase,
    private val classifyNumber: suspend (String) -> SpamVerdict,
    private val blockNumber: suspend (String) -> Unit
) : ViewModel() {

    private val state = MutableStateFlow(DialerUiState())
    val uiState: StateFlow<DialerUiState> = state.asStateFlow()

    fun onKeyPressed(key: String) {
        val current = state.value.input
        val updated = when (key) {
            "DEL" -> current.dropLast(1)
            "+" -> if (current.startsWith("+")) current else "+$current"
            else -> current + key
        }
        state.value = state.value.copy(
            input = updated,
            formatted = PhoneNumbers.pretty(updated),
            canCall = PhoneNumbers.canBeDialed(updated),
            statusMessage = null,
            errorMessage = null
        )
        searchSuggestions(updated)
    }

    fun setInput(value: String) {
        state.value = state.value.copy(
            input = value,
            formatted = PhoneNumbers.pretty(value),
            canCall = PhoneNumbers.canBeDialed(value)
        )
        searchSuggestions(value)
    }

    private fun searchSuggestions(value: String) {
        if (value.length < 2) {
            state.value = state.value.copy(suggestions = emptyList(), spamVerdict = null)
            return
        }
        viewModelScope.launch {
            val suggestions = try {
                contactsRepository.suggestions(value)
            } catch (t: Throwable) {
                emptyList()
            }
            state.value = state.value.copy(suggestions = suggestions)

            if (PhoneNumbers.canBeDialed(value)) {
                val verdict = try {
                    classifyNumber(value)
                } catch (t: Throwable) {
                    null
                }
                state.value = state.value.copy(spamVerdict = verdict)
            }
        }
    }

    fun call(number: String = state.value.input) {
        val clean = number.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val result = placeCall(clean)
            if (result is com.studiolexair.movaphone.core.common.result.MovaResult.Failure) {
                state.value = state.value.copy(errorMessage = result.error.userMessage)
            } else {
                // El usuario pidio expresamente que, tras llamar, quede a la vista el numero
                // al que acaba de llamar, con un boton para volver a llamar.
                val name = runCatching {
                    contactsRepository.contactsByNumber(PhoneNumbers.normalize(clean))?.displayName
                }.getOrNull()
                state.value = state.value.copy(
                    input = "",
                    formatted = "",
                    canCall = false,
                    suggestions = emptyList(),
                    lastDialed = clean,
                    lastDialedName = name
                )
                // El número recién marcado entra YA en el historial de MOVA, sin esperar a que
                // el sistema vuelque su registro: es lo que pidió el usuario al probar el 1.1.
                runCatching {
                    callsRepository.registerCall(
                        com.studiolexair.movaphone.domain.calls.model.CallRecord(
                            number = clean,
                            normalizedNumber = PhoneNumbers.normalize(clean),
                            contactName = name,
                            type = com.studiolexair.movaphone.domain.calls.model.CallType.OUTGOING,
                            startedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    /** Guarda el número tecleado como contacto nuevo. */
    fun saveAsContact(name: String, number: String = state.value.input) {
        viewModelScope.launch {
            val result = saveContact(
                Contact(displayName = name, phoneNumber = number, normalizedNumber = PhoneNumbers.normalize(number))
            )
            state.value = when (result) {
                is com.studiolexair.movaphone.core.common.result.MovaResult.Success ->
                    state.value.copy(statusMessage = "Contacto guardado", errorMessage = null)
                is com.studiolexair.movaphone.core.common.result.MovaResult.Failure ->
                    state.value.copy(errorMessage = result.error.userMessage)
            }
        }
    }

    fun blockCurrentNumber() {
        val number = state.value.input
        if (number.isBlank()) return
        viewModelScope.launch {
            try {
                blockNumber(number)
                state.value = state.value.copy(statusMessage = "Número bloqueado")
            } catch (t: Throwable) {
                state.value = state.value.copy(errorMessage = "No fue posible bloquear el número.")
            }
        }
    }

    fun reportSpam() {
        val number = state.value.input
        if (number.isBlank()) return
        viewModelScope.launch {
            try {
                blockNumber(number)
                state.value = state.value.copy(statusMessage = "Número reportado como spam")
            } catch (t: Throwable) {
                state.value = state.value.copy(errorMessage = "No fue posible reportar el número.")
            }
        }
    }

    fun clearMessage() {
        state.value = state.value.copy(statusMessage = null, errorMessage = null)
    }

    companion object {
        fun factory(
            contactsRepository: ContactsRepository,
            callsRepository: com.studiolexair.movaphone.domain.calls.repository.CallsRepository,
            placeCall: PlaceCallUseCase,
            saveContact: SaveContactUseCase,
            classifyNumber: suspend (String) -> SpamVerdict,
            blockNumber: suspend (String) -> Unit
        ) = viewModelFactory {
            initializer {
                DialerViewModel(
                    contactsRepository,
                    callsRepository,
                    placeCall,
                    saveContact,
                    classifyNumber,
                    blockNumber
                )
            }
        }
    }
}
