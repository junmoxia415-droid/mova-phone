package com.studiolexair.movaphone.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import com.studiolexair.movaphone.domain.calls.usecase.PlaceCallUseCase
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del asistente: lo que se oyó, lo que se entendió y lo que falta confirmar. */
data class SmartAssistantState(
    val transcript: String = "",
    val command: SmartCommand? = null,
    val awaitingConfirmation: Boolean = false,
    val result: String? = null,
    val history: List<String> = emptyList()
)

/**
 * Asistente inteligente (requisito 24): entiende órdenes locales y **pide confirmación**
 * para las acciones sensibles (llamar, enviar SMS, compartir ubicación o lanzar el SOS).
 * Ninguna orden se ejecuta sin que el usuario lo haya aceptado.
 */
class SmartAssistantViewModel(
    private val contactsRepository: ContactsRepository,
    private val placeCall: PlaceCallUseCase,
    private val messageRepository: MessageRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl,
    private val sosOrchestrator: SosOrchestrator
) : ViewModel() {

    private val stateFlow = MutableStateFlow(SmartAssistantState())
    val state: StateFlow<SmartAssistantState> = stateFlow.asStateFlow()

    /** Se llama con el texto reconocido por el sistema (voz) o escrito por el usuario. */
    fun interpret(input: String, onOpen: (SmartCommand.Destination) -> Unit) {
        val command = SmartCommandParser.parse(input)
        if (command is SmartCommand.Open) {
            onOpen(command.destination)
            stateFlow.update { it.copy(transcript = input, command = command, awaitingConfirmation = false, result = "Abriendo ${command.destination.label}") }
            return
        }
        stateFlow.update {
            it.copy(
                transcript = input,
                command = command,
                awaitingConfirmation = command.isSensitive,
                result = when (command) {
                    is SmartCommand.Unknown -> "No entendí la orden. Prueba con «llamar a Ana» o «abrir ubicación»."
                    else -> null
                }
            )
        }
    }

    /** Ejecuta la orden ya confirmada por el usuario. */
    fun confirm() {
        val command = stateFlow.value.command ?: return
        stateFlow.update { it.copy(awaitingConfirmation = false) }
        viewModelScope.launch { execute(command) }
    }

    fun cancel() = stateFlow.update {
        it.copy(awaitingConfirmation = false, result = "Orden cancelada. No se hizo nada.", command = null)
    }

    private suspend fun execute(command: SmartCommand) {
        val outcome = when (command) {
            is SmartCommand.Call -> call(command.target)
            is SmartCommand.SendMessage -> sendMessage(command.target, command.body)
            SmartCommand.ShareLocation -> shareLocation()
            SmartCommand.StartEmergency -> {
                sosOrchestrator.start(trigger = "assistant")
                "Protocolo de emergencia iniciado."
            }
            is SmartCommand.Open -> "Abriendo ${command.destination.label}"
            is SmartCommand.Unknown -> "No entendí la orden."
        }
        stateFlow.update { it.copy(result = outcome, history = (listOf(outcome) + it.history).take(8)) }
    }

    private suspend fun call(target: String): String {
        val number = resolveNumber(target) ?: return "No encontré a «$target» en tus contactos."
        return when (val result = placeCall(number)) {
            is com.studiolexair.movaphone.core.common.result.MovaResult.Success -> "Llamando a $target…"
            is com.studiolexair.movaphone.core.common.result.MovaResult.Failure -> result.error.userMessage
        }
    }

    private suspend fun sendMessage(target: String, body: String): String {
        val number = resolveNumber(target) ?: return "No encontré a «$target» en tus contactos."
        val sent = messageRepository.sendMessage(
            address = number,
            normalizedAddress = PhoneNumbers.normalize(number),
            body = body,
            contactName = target
        )
        return if (sent) "Mensaje enviado a $target." else "No fue posible enviar el mensaje. Revisa el permiso de SMS."
    }

    private suspend fun shareLocation(): String {
        val fix = locationRepository.lastKnown()
        if (fix == null) {
            val fresh = locationRepository.currentLocation()
            if (fresh is com.studiolexair.movaphone.data.location.model.LocationResult.Available) {
                return "Ubicación lista: " + locationRepository.shareText(fresh.fix)
            }
            return "No pude obtener tu ubicación. Comprueba el GPS y los permisos."
        }
        return "Ubicación lista: " + locationRepository.shareText(fix)
    }

    /** Resuelve un nombre contra la agenda local; si es un número, se usa tal cual. */
    private suspend fun resolveNumber(target: String): String? {
        if (target.any { it.isDigit() } && target.count { it.isDigit() } >= 3) {
            return PhoneNumbers.normalize(target)
        }
        val contacts = contactsRepository.suggestions(target, limit = 1)
        return contacts.firstOrNull()?.phoneNumber
    }

    companion object {
        fun factory(
            contactsRepository: ContactsRepository,
            placeCall: PlaceCallUseCase,
            messageRepository: MessageRepositoryImpl,
            locationRepository: LocationRepositoryImpl,
            sosOrchestrator: SosOrchestrator
        ) = viewModelFactory {
            initializer {
                SmartAssistantViewModel(contactsRepository, placeCall, messageRepository, locationRepository, sosOrchestrator)
            }
        }
    }
}
