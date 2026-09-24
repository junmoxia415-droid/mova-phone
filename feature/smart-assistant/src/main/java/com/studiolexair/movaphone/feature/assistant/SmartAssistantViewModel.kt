package com.studiolexair.movaphone.feature.assistant

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.security.settings.MovaSettingsStore
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.domain.automation.repository.DrivingModeController
import com.studiolexair.movaphone.domain.calls.repository.CallsRepository
import com.studiolexair.movaphone.domain.calls.usecase.PlaceCallUseCase
import com.studiolexair.movaphone.domain.contacts.matcher.ContactMatcher
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator
import com.studiolexair.movaphone.feature.assistant.ai.AssistantPreferences
import com.studiolexair.movaphone.feature.assistant.ai.BrainFactory
import com.studiolexair.movaphone.feature.assistant.ai.LocalModel
import com.studiolexair.movaphone.feature.assistant.ai.LocalModelManager
import com.studiolexair.movaphone.feature.assistant.voice.MovaVoiceOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un mensaje del chat con MOVA. */
data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    /** Botones que acompañan al mensaje («Sí, llama» / «Cancelar» / elegir contacto). */
    val options: List<ChatOption> = emptyList()
)

/** Botón de una respuesta de MOVA. */
data class ChatOption(
    val label: String,
    val value: String,
    val kind: Kind
) {
    enum class Kind {
        /** Se interpreta como si el usuario lo hubiera escrito. */
        COMMAND,

        /** Elegir un contacto entre varios parecidos. */
        PICK_CONTACT,

        /** Confirmar una acción sensible que ya está preparada. */
        CONFIRM,

        /** Descartar la acción preparada. */
        CANCEL,

        /** Abrir la ayuda con todas las órdenes. */
        HELP,

        /** Abrir los ajustes del asistente (modelo, voz…). */
        SETTINGS
    }
}

/** Acción sensible esperando confirmación del usuario. */
data class PendingAction(val description: String, val command: SmartCommand)

data class SmartAssistantState(
    val messages: List<ChatMessage> = emptyList(),
    val listening: Boolean = false,
    val partial: String = "",
    /** Ya está preparada una acción y sólo falta el «sí». */
    val pending: PendingAction? = null,
    /** Orden a la espera de que el usuario elija entre varios contactos. */
    val choiceTemplate: SmartCommand? = null,
    /** Forma de escuchar: en el teléfono (sin Internet) o el reconocedor del sistema. */
    val voiceMode: String = "",
    /** Aviso sobre la voz (sin idioma instalado, sin permiso…). */
    val voiceNotice: String? = null,
    /** Quién interpretó la última orden. */
    val interpretedBy: String? = null,
    /** El modelo local está razonando. */
    val thinking: Boolean = false
)

/** Estado del modelo local, para la pantalla de ajustes del asistente. */
data class AssistantModelState(
    val models: List<LocalModel> = emptyList(),
    val downloadedId: String? = null,
    val downloadingId: String? = null,
    val progress: Float = 0f,
    val useModel: Boolean = false,
    val speakReplies: Boolean = true,
    val message: String? = null,
    /** Capacidad real del teléfono para cada modelo (memoria y espacio). */
    val availability: Map<String, String> = emptyMap(),
    /**
     * Primera vez que el usuario abre el chat: se le ofrecen los tres tamaños del modelo
     * (ligero, recomendado y avanzado) sin obligarle a nada.
     */
    val offerFirstRun: Boolean = false
)

/**
 * El cerebro conversacional de MOVA.
 *
 * Flujo de cada frase del usuario:
 *  1. **Se corrige lo mal oído** ([SpeechCorrector]) con el vocabulario de MOVA y los nombres
 *     reales de la agenda: «yamar a nena» → «llamar a nena».
 *  2. **Se interpreta** ([SmartCommandParser]); si hay modelo local activo, se le pide que
 *     traduzca la frase y el resultado se **valida siempre** con las reglas.
 *  3. **Se razona antes de actuar**: si el nombre es ambiguo (dos «Juan»), MOVA pregunta cuál
 *     en lugar de llamar al primero. Si la acción es sensible, pide confirmación.
 *  4. **Se ejecuta** y se responde en lenguaje natural, con la opción de decirlo en voz alta.
 */
class SmartAssistantViewModel(
    private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val callsRepository: CallsRepository,
    private val placeCall: PlaceCallUseCase,
    private val messageRepository: MessageRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl,
    private val sosOrchestrator: SosOrchestrator,
    private val drivingMode: DrivingModeController,
    private val settingsStore: MovaSettingsStore,
    val modelManager: LocalModelManager,
    private val preferences: AssistantPreferences,
    private val voiceOutput: MovaVoiceOutput
) : ViewModel() {

    private val stateFlow = MutableStateFlow(SmartAssistantState())
    val state: StateFlow<SmartAssistantState> = stateFlow.asStateFlow()

    private val modelStateFlow = MutableStateFlow(AssistantModelState())
    val modelState: StateFlow<AssistantModelState> = modelStateFlow.asStateFlow()

    private var nextId = 1L
    private var userName: String = ""

    /** La pantalla conecta estos avisos con la navegación real. */
    var onOpenHelp: () -> Unit = {}
    var onOpenSettings: () -> Unit = {}

    init {
        refreshModels()
        viewModelScope.launch {
            // Se saluda por el nombre del perfil y se explica qué sabe hacer.
            userName = settingsStore.settings.first().userName.trim().takeIf { it != "Usuario" }.orEmpty()
            greet()
        }
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                userName = settings.userName.trim().takeIf { it != "Usuario" }.orEmpty()
            }
        }
    }

    // ---------------- Conversación ----------------

    private fun say(text: String, options: List<ChatOption> = emptyList(), speak: Boolean = true) {
        val message = ChatMessage(id = nextId++, fromUser = false, text = text, options = options)
        stateFlow.update { it.copy(messages = it.messages + message) }
        if (speak && modelStateFlow.value.speakReplies) voiceOutput.speak(text)
    }

    private fun userSaid(text: String) {
        stateFlow.update { it.copy(messages = it.messages + ChatMessage(nextId++, fromUser = true, text = text)) }
    }

    private fun greet() {
        val hello = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 6..11 -> "Buenos días"
            in 12..20 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        val who = if (userName.isNotBlank()) ", $userName" else ""
        say(
            "$hello$who. Soy MOVA. Pídeme lo que necesites hablando o escribiendo: " +
                "«llama a mamá», «manda un mensaje a Luis», «léeme los mensajes»…",
            options = listOf(
                ChatOption("¿Qué sabes hacer?", "", ChatOption.Kind.HELP),
                ChatOption("Ajustes del asistente", "", ChatOption.Kind.SETTINGS)
            )
        )
    }

    /** El usuario escribe o dicta una orden. */
    fun submit(rawInput: String, onOpen: (SmartCommand.Destination) -> Unit) {
        val input = rawInput.trim()
        if (input.isBlank()) return
        stateFlow.update { it.copy(voiceNotice = null) }
        userSaid(input)
        viewModelScope.launch { understandAndAct(input, onOpen) }
    }

    private suspend fun understandAndAct(input: String, onOpen: (SmartCommand.Destination) -> Unit) {
        val contacts = runCatching { contactsRepository.observeContacts().first() }.getOrDefault(emptyList())

        // 1) Reparar lo que se oyó mal con el vocabulario de MOVA y los nombres reales.
        val repair = SpeechCorrector.repair(input, contacts.map { it.displayName })
        val corrected = repair.text

        // 2) Traducir la frase a una orden (modelo local si está activo, reglas siempre).
        stateFlow.update { it.copy(thinking = true) }
        val brain = BrainFactory.create(context, modelManager, preferences.useLocalModel)
        val brainResult = runCatching { brain.understand(corrected, contacts.map { it.displayName }) }.getOrNull()
        stateFlow.update { it.copy(thinking = false) }

        var command = SmartCommandParser.parse(brainResult?.normalizedCommand ?: corrected)
        if (command is SmartCommand.Unknown) {
            command = SmartCommandParser.parse(corrected)
        }
        if (repair.changed && command !is SmartCommand.Unknown) {
            say("Entendí «${repair.corrections.joinToString(", ") { "${it.first} → ${it.second}" }}».", speak = false)
        }

        when (command) {
            is SmartCommand.Unknown -> {
                say(
                    "No te he entendido, lo siento. Prueba a decirlo de otra forma: " +
                        "«llama a mamá», «manda un mensaje a Luis diciendo llego tarde», «léeme los mensajes».",
                    options = listOf(ChatOption("Ver todo lo que sé hacer", "", ChatOption.Kind.HELP))
                )
            }

            is SmartCommand.Open -> {
                onOpen(command.destination)
                say("Abriendo ${command.destination.label}.", speak = false)
            }

            is SmartCommand.Call -> resolveAndPropose(input, command.target, contacts) { target ->
                SmartCommand.Call(target)
            }

            is SmartCommand.SendMessage -> resolveAndPropose(input, command.target, contacts) { target ->
                command.copy(target = target)
            }

            SmartCommand.ShareLocation -> confirm(SmartCommand.ShareLocation, "Compartir tu ubicación con quien te lo pida")
            SmartCommand.StartEmergency -> confirm(SmartCommand.StartEmergency, "Lanzar el protocolo de emergencia (SOS)")
            SmartCommand.ReadMessages -> readMessages()
            SmartCommand.ReadMissedCalls -> readMissedCalls()
            is SmartCommand.Driving -> {
                drivingMode.setDrivingMode(command.enabled)
                say(if (command.enabled) "Modo conducción activado." else "Modo conducción desactivado.")
            }
            SmartCommand.Silence -> silence()
            is SmartCommand.RenameUser -> renameUser(command.name)
        }
    }

    /**
     * Busca el contacto evitando equivocarse.
     *
     * Si hay **varios** contactos con el mismo nombre o parecidos (dos «Juan»), MOVA no elige
     * por su cuenta: pregunta cuál y ofrece cada uno con su número para no confundirse.
     */
    private suspend fun resolveAndPropose(
        original: String,
        target: String,
        contacts: List<Contact>,
        build: (String) -> SmartCommand
    ) {
        // Número dictado: se usa tal cual.
        if (target.count { it.isDigit() } >= 3) {
            val number = PhoneNumbers.normalize(target)
            val name = contacts.firstOrNull { it.normalizedNumber == number }?.displayName
            confirm(build(number), if (name != null) "Llamar a $name ($number)" else "Llamar al número $number")
            return
        }

        // Se puntúan todos los contactos y se mira si los dos mejores empatan: eso es
        // exactamente la situación en la que MOVA debe preguntar en vez de adivinar.
        val scored = contacts
            .mapNotNull { contact -> ContactMatcher.score(target, contact)?.let { it.score to contact } }
            .sortedByDescending { it.first }
        val matches = scored.map { it.second }.distinctBy { it.normalizedNumber }
        val topScore = scored.firstOrNull()?.first
        val tied = if (topScore == null) emptyList() else scored.filter { it.first == topScore }.map { it.second }
            .distinctBy { it.normalizedNumber }

        when {
            matches.isEmpty() -> {
                say(
                    "No encuentro a nadie que se llame «$target» en tu agenda. " +
                        "Puedes decirme el número directamente o dar de alta el contacto.",
                    options = listOf(
                        ChatOption("Abrir contactos", "abre contactos", ChatOption.Kind.COMMAND),
                        ChatOption("Abrir el marcador", "abre el marcador", ChatOption.Kind.COMMAND)
                    )
                )
            }

            // Una sola opción clara: se propone y se pide confirmación.
            tied.size <= 1 -> {
                val chosen = tied.firstOrNull() ?: matches.first()
                confirm(build(chosen.phoneNumber), describeAction(build(chosen.phoneNumber), chosen))
            }

            // Empate: hay que preguntar. «¿A cuál de los dos Juan?»
            else -> {
                val template = build(target)
                stateFlow.update { it.copy(choiceTemplate = template) }
                say(
                    "Con «$target» me salen ${tied.size} contactos. ¿A cuál quieres?",
                    options = tied.take(4).map { contact ->
                        ChatOption(
                            label = "${contact.displayName} · ${contact.phoneNumber}",
                            value = contact.phoneNumber,
                            kind = ChatOption.Kind.PICK_CONTACT
                        )
                    } + ChatOption("Cancelar", "", ChatOption.Kind.CANCEL)
                )
            }
        }
    }

    private fun describeAction(command: SmartCommand, contact: Contact): String = when (command) {
        is SmartCommand.Call -> "Llamar a ${contact.displayName}"
        is SmartCommand.SendMessage -> "Enviar un mensaje a ${contact.displayName}"
        else -> "Continuar"
    }

    /** Deja la acción lista y pide permiso. Nada sensible se ejecuta sin este paso. */
    private fun confirm(command: SmartCommand, description: String) {
        stateFlow.update { it.copy(pending = PendingAction(description, command)) }
        say(
            "$description. ¿Lo hago?",
            options = listOf(
                ChatOption("Sí, adelante", "", ChatOption.Kind.CONFIRM),
                ChatOption("Cancelar", "", ChatOption.Kind.CANCEL)
            )
        )
    }

    /** Toque en un botón de una respuesta de MOVA. */
    fun onOption(option: ChatOption, onOpen: (SmartCommand.Destination) -> Unit) {
        when (option.kind) {
            ChatOption.Kind.CONFIRM -> confirmPending()
            ChatOption.Kind.CANCEL -> {
                stateFlow.update { it.copy(pending = null, choiceTemplate = null) }
                say("Vale, no hago nada.", speak = false)
            }
            ChatOption.Kind.PICK_CONTACT -> {
                // La orden que estaba esperando se completa con el número elegido.
                val template = stateFlow.value.choiceTemplate
                stateFlow.update { it.copy(choiceTemplate = null) }
                when (template) {
                    is SmartCommand.Call -> confirm(
                        SmartCommand.Call(option.value),
                        "Llamar a ${option.label.substringBefore(" ·")}"
                    )
                    is SmartCommand.SendMessage -> confirm(
                        template.copy(target = option.value),
                        "Enviar el mensaje a ${option.label.substringBefore(" ·")}"
                    )
                    else -> say("Vale, ${option.label.substringBefore(" ·")}.")
                }
            }
            ChatOption.Kind.HELP -> {
                say("Aquí tienes todo lo que sé hacer.", speak = false)
                onOpenHelp()
            }
            ChatOption.Kind.SETTINGS -> {
                say("Abriendo los ajustes del asistente.", speak = false)
                onOpenSettings()
            }
            ChatOption.Kind.COMMAND -> submit(option.value, onOpen)
        }
    }

    /** Confirma y ejecuta la acción pendiente. */
    fun confirmPending() {
        val pending = stateFlow.value.pending ?: return
        stateFlow.update { it.copy(pending = null, choiceTemplate = null) }
        viewModelScope.launch { execute(pending.command) }
    }

    /** El usuario quiere que MOVA le llame de otra forma: se guarda en su perfil. */
    private fun renameUser(name: String) {
        val clean = name.trim().trim(',', '.', '!', '?').take(40)
        if (clean.isBlank()) {
            say("Dime el nombre y lo cambio: por ejemplo, «cambia mi nombre a Ana».")
            return
        }
        val pretty = clean.split(' ').filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        viewModelScope.launch { settingsStore.setUserName(pretty) }
        userName = pretty
        say("Hecho, $pretty. A partir de ahora te llamo así, y lo tienes en tu perfil para cambiarlo cuando quieras.",
            options = listOf(ChatOption("Ver mi perfil", "abre mi perfil", ChatOption.Kind.COMMAND)))
    }

    private suspend fun execute(command: SmartCommand) {
        when (command) {
            is SmartCommand.Call -> {
                val name = contactNameFor(command.target)
                val result = placeCall(command.target)
                when (result) {
                    is com.studiolexair.movaphone.core.common.result.MovaResult.Success ->
                        say(if (name != null) "Llamando a $name." else "Llamando al ${command.target}.")
                    is com.studiolexair.movaphone.core.common.result.MovaResult.Failure ->
                        say(result.error.userMessage)
                }
            }

            is SmartCommand.RenameUser -> renameUser(command.name)

            is SmartCommand.SendMessage -> {
                val name = contactNameFor(command.target) ?: command.target
                val sent = messageRepository.sendMessage(
                    address = command.target,
                    normalizedAddress = PhoneNumbers.normalize(command.target),
                    body = command.body,
                    contactName = name
                )
                say(
                    if (sent) "Mensaje enviado a $name." else
                        "No he podido enviar el mensaje a $name. Revisa la cobertura y el permiso de SMS."
                )
            }

            SmartCommand.ShareLocation -> {
                val fix = locationRepository.lastKnown()
                val text = fix?.let { locationRepository.shareText(it) }
                if (text != null) say("Aquí tienes tu ubicación: $text", speak = false)
                else when (val fresh = locationRepository.currentLocation()) {
                    is com.studiolexair.movaphone.data.location.model.LocationResult.Available ->
                        say("Aquí tienes tu ubicación: ${locationRepository.shareText(fresh.fix)}", speak = false)
                    is com.studiolexair.movaphone.data.location.model.LocationResult.Unavailable ->
                        say(fresh.reason)
                }
            }

            SmartCommand.StartEmergency -> {
                sosOrchestrator.start(trigger = "assistant")
                say("Protocolo de emergencia iniciado. Avisa a tus contactos de confianza.")
            }

            else -> Unit
        }
    }

    private suspend fun contactNameFor(number: String): String? =
        runCatching { contactsRepository.contactsByNumber(PhoneNumbers.normalize(number))?.displayName }.getOrNull()

    // ---------------- Leer en voz alta ----------------

    private suspend fun readMessages() {
        val unread = runCatching {
            messageRepository.observeLatest(50).first().filter { it.isIncoming && it.state == "RECEIVED" }
        }.getOrDefault(emptyList())

        if (unread.isEmpty()) {
            say("No tienes mensajes sin leer. Todo al día.")
            return
        }
        val summary = unread.take(3).joinToString(". ") { message ->
            val who = message.contactName?.takeIf { it.isNotBlank() } ?: message.address
            "$who dice: ${message.body.take(120)}"
        }
        say(
            if (unread.size <= 3) "Tienes ${unread.size} mensaje${if (unread.size > 1) "s" else ""}: $summary"
            else "Tienes ${unread.size} mensajes sin leer. Los tres últimos: $summary",
            options = listOf(ChatOption("Abrir los mensajes", "abre mensajes", ChatOption.Kind.COMMAND))
        )
    }

    private suspend fun readMissedCalls() {
        val missed = runCatching { callsRepository.observeRecentCalls(50).first() }.getOrDefault(emptyList())
            .filter { it.type == com.studiolexair.movaphone.domain.calls.model.CallType.MISSED }

        if (missed.isEmpty()) {
            say("No tienes llamadas perdidas. Nada pendiente.")
            return
        }
        val summary = missed.take(3).joinToString(". ") { call ->
            val who = call.contactName?.takeIf { it.isNotBlank() } ?: call.number
            "$who, ${haceCuanto(call.startedAt)}"
        }
        say(
            if (missed.size <= 3) "Tienes ${missed.size} llamada${if (missed.size > 1) "s" else ""} perdida${if (missed.size > 1) "s" else ""}: $summary"
            else "Tienes ${missed.size} llamadas perdidas. Las tres últimas: $summary",
            options = listOf(ChatOption("Ver el historial", "abre el historial", ChatOption.Kind.COMMAND))
        )
    }

    private fun haceCuanto(timestamp: Long): String {
        val minutes = ((System.currentTimeMillis() - timestamp) / 60000).coerceAtLeast(0)
        return when {
            minutes < 60 -> "hace $minutes minuto${if (minutes != 1L) "s" else ""}"
            minutes < 1440 -> "hace ${minutes / 60} hora${if (minutes / 60 != 1L) "s" else ""}"
            else -> "hace ${minutes / 1440} día${if (minutes / 1440 != 1L) "s" else ""}"
        }
    }

    // ---------------- Silencio ----------------

    private fun silence() {
        val audio = context.getSystemService(AudioManager::class.java)
        val changed = runCatching {
            audio?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }.isSuccess
        say(
            if (changed) "He puesto el teléfono en vibración. Si quieres silencio total, " +
                "Android exige darle acceso a No molestar: te lo abro desde Ajustes → Permisos."
            else "No he podido cambiar el sonido del teléfono. Puedes hacerlo con los botones de volumen."
        )
    }

    // ---------------- Voz ----------------

    fun onListeningStarted() = stateFlow.update { it.copy(listening = true, partial = "", voiceNotice = null) }
    fun onPartial(text: String) = stateFlow.update { it.copy(partial = text) }
    fun onListeningStopped() = stateFlow.update { it.copy(listening = false) }
    fun onVoiceError(message: String) = stateFlow.update {
        it.copy(listening = false, partial = "", voiceNotice = message)
    }
    fun onVoiceMode(mode: String) = stateFlow.update {
        it.copy(voiceMode = mode)
    }
    fun clearVoiceNotice() = stateFlow.update { it.copy(voiceNotice = null) }

    // ---------------- Modelo local (pantalla de ajustes) ----------------

    private fun refreshModels() {
        val downloaded = modelManager.downloadedModel()?.id
        modelStateFlow.update { current ->
            current.copy(
                models = modelManager.models(),
                downloadedId = downloaded,
                offerFirstRun = !preferences.offeredModelDownload && downloaded == null,
                useModel = preferences.useLocalModel,
                speakReplies = preferences.speakReplies,
                availability = modelManager.models().associate { it.id to modelManager.canRun(it).second }
            )
        }
    }

    /** El usuario dijo «ahora no»: no se vuelve a ofrecer el modelo al abrir el chat. */
    fun dismissModelOffer() {
        preferences.offeredModelDownload = true
        modelStateFlow.update { it.copy(offerFirstRun = false) }
    }

    fun downloadModel(model: LocalModel) {
        viewModelScope.launch {
            modelStateFlow.update { it.copy(downloadingId = model.id, progress = 0f, message = null) }
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                modelManager.download(model)
            }
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
            if (ok) {
                preferences.offeredModelDownload = true
                modelStateFlow.update { it.copy(offerFirstRun = false) }
                say("Ya tengo el modelo en el teléfono: a partir de ahora te entiendo mejor, y sin Internet.")
            }
        }
    }

    fun deleteModel(model: LocalModel) {
        modelManager.delete(model)
        if (modelManager.downloadedModel() == null) preferences.useLocalModel = false
        refreshModels()
        modelStateFlow.update { it.copy(message = modelManager.statusMessage.value) }
    }

    fun setUseModel(enabled: Boolean) {
        if (enabled && modelManager.downloadedModel() == null) {
            modelStateFlow.update {
                it.copy(message = "Primero descarga un modelo: se guarda en tu teléfono y funciona sin conexión.")
            }
            return
        }
        preferences.useLocalModel = enabled
        refreshModels()
        modelStateFlow.update {
            it.copy(
                message = if (enabled) {
                    "Modelo local activado: MOVA razona en el teléfono, sin enviar nada a Internet."
                } else {
                    "Intérprete de reglas activado (sigue funcionando sin Internet)."
                }
            )
        }
    }

    fun setSpeakReplies(enabled: Boolean) {
        preferences.speakReplies = enabled
        if (!enabled) voiceOutput.stop()
        modelStateFlow.update {
            it.copy(
                speakReplies = enabled,
                message = if (enabled) "MOVA te contestará en voz alta." else "MOVA sólo responderá por escrito."
            )
        }
    }

    fun clearModelMessage() = modelStateFlow.update { it.copy(message = null) }

    override fun onCleared() {
        voiceOutput.release()
        super.onCleared()
    }

    companion object {
        fun factory(
            context: Context,
            contactsRepository: ContactsRepository,
            callsRepository: CallsRepository,
            placeCall: PlaceCallUseCase,
            messageRepository: MessageRepositoryImpl,
            locationRepository: LocationRepositoryImpl,
            sosOrchestrator: SosOrchestrator,
            drivingMode: DrivingModeController,
            settingsStore: MovaSettingsStore,
            modelManager: LocalModelManager,
            preferences: AssistantPreferences,
            voiceOutput: MovaVoiceOutput
        ) = viewModelFactory {
            initializer {
                SmartAssistantViewModel(
                    context = context.applicationContext,
                    contactsRepository = contactsRepository,
                    callsRepository = callsRepository,
                    placeCall = placeCall,
                    messageRepository = messageRepository,
                    locationRepository = locationRepository,
                    sosOrchestrator = sosOrchestrator,
                    drivingMode = drivingMode,
                    settingsStore = settingsStore,
                    modelManager = modelManager,
                    preferences = preferences,
                    voiceOutput = voiceOutput
                )
            }
        }
    }
}
