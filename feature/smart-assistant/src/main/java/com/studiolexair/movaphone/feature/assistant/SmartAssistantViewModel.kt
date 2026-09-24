package com.studiolexair.movaphone.feature.assistant

import android.content.Context
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
import com.studiolexair.movaphone.feature.assistant.ai.AssistantPreferences
import com.studiolexair.movaphone.feature.assistant.ai.BrainFactory
import com.studiolexair.movaphone.feature.assistant.ai.LocalModel
import com.studiolexair.movaphone.feature.assistant.ai.LocalModelManager
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
    val history: List<String> = emptyList(),
    /** Quién interpretó la orden: el intérprete de reglas o el modelo local del teléfono. */
    val interpretedBy: String? = null,
    val listening: Boolean = false,
    val partial: String = "",
    val voiceNotice: String? = null
)

/** Estado del modelo local de lenguaje (requisito 9: razonamiento sin nube). */
data class AssistantModelState(
    val models: List<LocalModel> = emptyList(),
    val downloadedId: String? = null,
    val downloadingId: String? = null,
    val progress: Float = 0f,
    val useModel: Boolean = false,
    val message: String? = null
)

/**
 * Asistente inteligente (requisito 24): entiende órdenes **en el propio teléfono**.
 *
 * La voz se transcribe sin conexión (reconocedor del dispositivo) y la interpretación puede
 * hacerla el intérprete de reglas local o, si el usuario lo activa, un modelo de lenguaje
 * pequeño descargado en el móvil. Nada sale del dispositivo y ninguna acción sensible se
 * ejecuta sin confirmación explícita.
 */
class SmartAssistantViewModel(
    private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val placeCall: PlaceCallUseCase,
    private val messageRepository: MessageRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl,
    private val sosOrchestrator: SosOrchestrator,
    val modelManager: LocalModelManager,
    private val preferences: AssistantPreferences
) : ViewModel() {

    private val stateFlow = MutableStateFlow(SmartAssistantState())
    val state: StateFlow<SmartAssistantState> = stateFlow.asStateFlow()

    private val modelStateFlow = MutableStateFlow(
        AssistantModelState(
            models = modelManager.models(),
            downloadedId = modelManager.downloadedModel()?.id,
            useModel = preferences.useLocalModel
        )
    )
    val modelState: StateFlow<AssistantModelState> = modelStateFlow.asStateFlow()

    // ---------------- Voz (sin conexión) ----------------

    fun onVoiceStarted() = stateFlow.update {
        it.copy(listening = true, partial = "", voiceNotice = null, result = null)
    }

    fun onVoicePartial(text: String) = stateFlow.update { it.copy(partial = text) }

    fun onVoiceStopped() = stateFlow.update { it.copy(listening = false) }

    /** Error de voz: se explica al usuario sin tecnicismos. */
    fun onVoiceError(message: String) = stateFlow.update {
        it.copy(listening = false, partial = "", voiceNotice = message)
    }

    fun onVoiceResult(text: String) = stateFlow.update { it.copy(listening = false, partial = "") }

    // ---------------- Interpretación ----------------

    /** Se llama con el texto reconocido por la voz o escrito por el usuario. */
    fun interpret(input: String, onOpen: (SmartCommand.Destination) -> Unit) {
        val cleanInput = input.trim()
        if (cleanInput.isBlank()) return
        viewModelScope.launch {
            // 1) El «cerebro» elegido (reglas o modelo local) reescribe la frase como orden.
            val brain = BrainFactory.create(context, modelManager, preferences.useLocalModel)
            val brainResult = runCatching { brain.understand(cleanInput) }.getOrNull()

            // 2) El intérprete de MOVA valida siempre el resultado antes de ejecutar nada.
            val fromModel = brainResult?.normalizedCommand?.takeIf { it.isNotBlank() }
            var command = SmartCommandParser.parse(fromModel ?: cleanInput)
            var usedFallback = false
            if (command is SmartCommand.Unknown && fromModel != null && fromModel != cleanInput) {
                command = SmartCommandParser.parse(cleanInput)
                usedFallback = true
            }

            if (command is SmartCommand.Open) {
                onOpen(command.destination)
                stateFlow.update {
                    it.copy(
                        transcript = cleanInput,
                        command = command,
                        awaitingConfirmation = false,
                        result = "Abriendo ${command.destination.label}",
                        interpretedBy = brainResult?.source
                    )
                }
                return@launch
            }

            stateFlow.update {
                it.copy(
                    transcript = cleanInput,
                    command = command,
                    awaitingConfirmation = command.isSensitive,
                    interpretedBy = brainResult?.source,
                    result = when (command) {
                        is SmartCommand.Unknown ->
                            "No entendí la orden. Prueba con «llamar a Ana» o «abrir ubicación»."
                        else -> null
                    },
                    voiceNotice = if (usedFallback) {
                        "El modelo no devolvió una orden clara; se usó el intérprete de MOVA."
                    } else {
                        it.voiceNotice
                    }
                )
            }
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

    /**
     * Resuelve un nombre contra la agenda local. El buscador tolera emoji, tildes y
     * mayúsculas: «nena» encuentra «Nena ❤️». Si es un número, se usa tal cual.
     */
    private suspend fun resolveNumber(target: String): String? {
        if (target.count { it.isDigit() } >= 3) return PhoneNumbers.normalize(target)
        return contactsRepository.bestMatch(target)?.contact?.phoneNumber
            ?: contactsRepository.suggestions(target, limit = 1).firstOrNull()?.phoneNumber
    }

    // ---------------- Modelo local ----------------

    fun downloadModel(model: LocalModel) {
        viewModelScope.launch {
            modelStateFlow.update { it.copy(downloadingId = model.id, progress = 0f, message = null) }
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                modelManager.download(model)
            }
            // Se refleja el progreso real mientras se descarga.
            while (job.isActive) {
                modelStateFlow.update { it.copy(progress = modelManager.downloadProgress.value) }
                kotlinx.coroutines.delay(300)
            }
            job.join()
            val ok = modelManager.downloadedModel()?.id == model.id
            if (ok && !preferences.useLocalModel) preferences.useLocalModel = true
            modelStateFlow.update {
                it.copy(
                    downloadingId = null,
                    progress = if (ok) 1f else 0f,
                    downloadedId = modelManager.downloadedModel()?.id,
                    useModel = preferences.useLocalModel,
                    message = modelManager.statusMessage.value
                )
            }
        }
    }

    fun deleteModel(model: LocalModel) {
        modelManager.delete(model)
        if (modelManager.downloadedModel() == null) preferences.useLocalModel = false
        modelStateFlow.update {
            it.copy(
                downloadedId = modelManager.downloadedModel()?.id,
                useModel = preferences.useLocalModel,
                progress = 0f,
                message = modelManager.statusMessage.value
            )
        }
    }

    fun setUseModel(enabled: Boolean) {
        if (enabled && modelManager.downloadedModel() == null) {
            modelStateFlow.update { it.copy(message = "Primero descarga un modelo: se guarda en tu teléfono y funciona sin conexión.") }
            return
        }
        preferences.useLocalModel = enabled
        modelStateFlow.update {
            it.copy(
                useModel = enabled,
                message = if (enabled) {
                    "Modelo local activado: MOVA razona en el teléfono, sin enviar nada a Internet."
                } else {
                    "Intérprete de reglas activado (sin modelo)."
                }
            )
        }
    }

    fun clearModelMessage() = modelStateFlow.update { it.copy(message = null) }

    companion object {
        fun factory(
            context: Context,
            contactsRepository: ContactsRepository,
            placeCall: PlaceCallUseCase,
            messageRepository: MessageRepositoryImpl,
            locationRepository: LocationRepositoryImpl,
            sosOrchestrator: SosOrchestrator,
            modelManager: LocalModelManager,
            preferences: AssistantPreferences
        ) = viewModelFactory {
            initializer {
                SmartAssistantViewModel(
                    context = context.applicationContext,
                    contactsRepository = contactsRepository,
                    placeCall = placeCall,
                    messageRepository = messageRepository,
                    locationRepository = locationRepository,
                    sosOrchestrator = sosOrchestrator,
                    modelManager = modelManager,
                    preferences = preferences
                )
            }
        }
    }
}
