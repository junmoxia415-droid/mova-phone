package com.studiolexair.movaphone.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.database.entity.SmsTemplateEntity
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.SmsTemplateRepositoryImpl
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Mensajes (requisito 21): conversaciones reales de SMS, envío, plantillas
 * y compartir ubicación. Preparado para mensajería online futura sin cambios de UI.
 */
class MessagesViewModel(
    private val messageRepository: MessageRepositoryImpl,
    private val templateRepository: SmsTemplateRepositoryImpl,
    private val locationRepository: LocationRepositoryImpl
) : ViewModel() {

    private val messageState = MutableStateFlow<String?>(null)

    val conversations: StateFlow<List<MessageEntity>> = messageRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<SmsTemplateEntity>> = templateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val statusMessage: StateFlow<String?> = messageState.asStateFlow()

    fun conversation(address: String): StateFlow<List<MessageEntity>> =
        messageRepository.observeConversation(PhoneNumbers.normalize(address))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun send(address: String, body: String, contactName: String? = null) {
        if (address.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            val ok = messageRepository.sendMessage(
                address = address,
                normalizedAddress = PhoneNumbers.normalize(address),
                body = body,
                contactName = contactName
            )
            messageState.value = if (ok) null else "No fue posible enviar el mensaje. Comprueba cobertura y permisos."
        }
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

    companion object {
        fun factory(
            messageRepository: MessageRepositoryImpl,
            templateRepository: SmsTemplateRepositoryImpl,
            locationRepository: LocationRepositoryImpl
        ) = viewModelFactory {
            initializer { MessagesViewModel(messageRepository, templateRepository, locationRepository) }
        }
    }
}
