package com.studiolexair.movaphone.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.database.entity.SmsTemplateEntity
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.data.messages.model.SenderSummary
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.SmsTemplateRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Mensajes: conversaciones SMS reales, envío con confirmación, sección de personas que
 * han escrito al usuario ("Te han escrito"), plantillas y compartir ubicación.
 *
 * [lastFailedId] permite reintentar en un toque un mensaje que no salió: es la pieza que
 * resuelve el error "no fue posible enviar el mensaje" que veía el usuario.
 */
class MessagesViewModel(
    private val messageRepository: MessageRepositoryImpl,
    private val templateRepository: SmsTemplateRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl,
    private val contactNameResolver: ((String) -> String?)? = null
) : ViewModel() {

    private val messageState = MutableStateFlow<String?>(null)
    private val failedId = MutableStateFlow<Long?>(null)

    val conversations: StateFlow<List<MessageEntity>> = messageRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Personas que han escrito al usuario, con su último mensaje y los no leídos. */
    val senders: StateFlow<List<SenderSummary>> = messageRepository.observeSenders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<SmsTemplateEntity>> = templateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statusMessage: StateFlow<String?> = messageState.asStateFlow()

    val lastFailedId: StateFlow<Long?> = failedId.asStateFlow()

    val unreadTotal: StateFlow<Int> = messageRepository.observeSenders()
        .map { list -> list.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun conversation(address: String): StateFlow<List<MessageEntity>> =
        messageRepository.observeConversation(PhoneNumbers.normalize(address))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Envía y deja preparado el reintento si el sistema rechaza el envío. */
    fun send(address: String, body: String, contactName: String? = null) {
        if (address.isBlank() || body.isBlank()) return
        val name = contactName ?: contactNameResolver?.invoke(address)
        viewModelScope.launch {
            val ok = messageRepository.sendMessage(
                address = address,
                normalizedAddress = PhoneNumbers.normalize(address),
                body = body,
                contactName = name
            )
            messageState.value = if (ok) null else
                "No fue posible enviar el mensaje. Comprueba la cobertura y los permisos, y pulsa Reintentar."
            failedId.value = if (ok) null else lastFailedOutgoing(address)
        }
    }

    /** Reintenta un mensaje concreto que quedó sin enviar. */
    fun retry(messageId: Long) {
        viewModelScope.launch {
            val ok = messageRepository.retrySend(messageId)
            messageState.value = if (ok) "Mensaje reenviado." else
                "Sigue sin salir. Revisa la cobertura: el mensaje queda guardado y puedes reintentarlo."
            failedId.value = if (ok) null else messageId
        }
    }

    /** Abrir la conversación marca como leídos los mensajes recibidos. */
    fun markRead(address: String) {
        viewModelScope.launch { messageRepository.markConversationRead(PhoneNumbers.normalize(address)) }
    }

    fun shareLocation(address: String) {
        viewModelScope.launch {
            when (val result = locationRepository.currentLocation()) {
                is com.studiolexair.movaphone.data.location.model.LocationResult.Available -> {
                    val text = locationRepository.shareText(result.fix)
                    send(address, text)
                }
                is com.studiolexair.movaphone.data.location.model.LocationResult.Unavailable ->
                    messageState.value = result.reason
            }
        }
    }

    fun importDeviceMessages() {
        viewModelScope.launch {
            try {
                val imported = messageRepository.importFromDevice()
                messageState.value = if (imported > 0) "$imported mensajes importados" else "No hay mensajes nuevos"
            } catch (security: SecurityException) {
                messageState.value = "MOVA Phone necesita permiso para leer tus SMS."
            } catch (t: Throwable) {
                messageState.value = "No fue posible importar los mensajes."
            }
        }
    }

    fun ensureDefaultTemplates() {
        viewModelScope.launch { templateRepository.ensureDefaults() }
    }

    fun delete(messageId: Long) {
        viewModelScope.launch { messageRepository.delete(messageId) }
    }

    fun clearMessage() {
        messageState.value = null
    }

    private suspend fun lastFailedOutgoing(address: String): Long? =
        messageRepository.lastFailedId(PhoneNumbers.normalize(address))

    companion object {
        fun factory(
            messageRepository: MessageRepositoryImpl,
            templateRepository: SmsTemplateRepositoryImpl,
            locationRepository: LocationRepositoryImpl,
            contactNameResolver: ((String) -> String?)? = null
        ) = viewModelFactory {
            initializer {
                MessagesViewModel(messageRepository, templateRepository, locationRepository, contactNameResolver)
            }
        }
    }
}
