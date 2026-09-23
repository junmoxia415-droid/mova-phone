package com.studiolexair.movaphone.data.messages.repository

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.dao.MessageDao
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.messages.model.SenderAggregator
import com.studiolexair.movaphone.data.messages.model.SenderSummary
import com.studiolexair.movaphone.data.messages.provider.MessageProvider
import com.studiolexair.movaphone.data.messages.provider.SmsIntentFactory
import com.studiolexair.movaphone.data.messages.source.DeviceSmsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio de mensajes. Local First: los mensajes viven en Room y se sincronizan
 * desde la bandeja del sistema cuando el usuario lo permite.
 *
 * El transporte (SMS hoy, Internet cuando exista) se elige aquí sin que la UI lo sepa,
 * y el estado real de cada mensaje se guarda en [MessageEntity.state] para poder mostrar
 * la palomita correcta: enviando, enviado, entregado, leído o fallido.
 */
class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val smsProvider: MessageProvider,
    private val deviceSms: DeviceSmsDataSource,
    private val intentFactory: SmsIntentFactory? = null
) {

    fun observeConversations(): Flow<List<MessageEntity>> = messageDao.observeConversations()

    fun observeConversation(normalizedAddress: String): Flow<List<MessageEntity>> =
        messageDao.observeConversation(normalizedAddress)

    fun observeLatest(limit: Int = 5): Flow<List<MessageEntity>> = messageDao.observeLatest(limit)

    fun search(query: String): Flow<List<MessageEntity>> = messageDao.search(query)

    fun observeUnreadCount(normalizedAddress: String): Flow<Int> =
        messageDao.observeUnreadCount(normalizedAddress)

    /**
     * Personas que han escrito al usuario (sección "Te han escrito").
     * Se calcula sobre los mensajes entrantes guardados: quién, cuándo, último texto
     * y cuántos quedan sin leer. Sin servidores: todo sale de la base local.
     */
    fun observeSenders(): Flow<List<SenderSummary>> =
        messageDao.observeAll().map(SenderAggregator::summarize)

    /** Persiste un mensaje saliente y lo envía por el transporte disponible. */
    suspend fun sendMessage(
        address: String,
        normalizedAddress: String,
        body: String,
        contactName: String?,
        isEmergency: Boolean = false
    ): Boolean {
        val now = System.currentTimeMillis()
        val id = messageDao.insert(
            MessageEntity(
                address = address,
                normalizedAddress = normalizedAddress,
                contactName = contactName,
                body = body,
                isIncoming = false,
                sentAt = now,
                state = STATE_SENDING,
                isEmergency = isEmergency,
                provider = smsProvider.id
            )
        )
        val result = smsProvider.send(
            destination = address,
            body = body,
            sentIntent = intentFactory?.sentIntent(id, address),
            deliveryIntent = intentFactory?.deliveryIntent(id, address)
        )
        // Si el transporte ni siquiera pudo encolar el mensaje se marca como fallido;
        // si lo encoló, la confirmación real llega por el receptor de SMS.
        if (!result.success) {
            messageDao.updateState(id, STATE_FAILED)
            MovaLog.w(TAG, "Envío fallido: ${result.errorMessage}")
        }
        return result.success
    }

    /** Reintenta un mensaje fallido reutilizando el mismo texto. */
    suspend fun retrySend(id: Long): Boolean {
        val original = messageDao.byId(id) ?: return false
        return sendMessage(
            address = original.address,
            normalizedAddress = original.normalizedAddress,
            body = original.body,
            contactName = original.contactName,
            isEmergency = original.isEmergency
        )
    }

    /** Todos los mensajes entrantes de un número pasan a leído al abrir la conversación. */
    suspend fun markConversationRead(normalizedAddress: String) =
        messageDao.markIncomingRead(normalizedAddress)

    /** Guarda un mensaje entrante (usado por services:sms al recibir un SMS). */
    suspend fun storeIncoming(message: MessageEntity): Long = messageDao.insert(message)

    suspend fun updateState(id: Long, state: String) = messageDao.updateState(id, state)

    suspend fun messageById(id: Long): MessageEntity? = messageDao.byId(id)

    suspend fun importFromDevice(): Int {
        val messages = deviceSms.readInbox()
        if (messages.isEmpty()) return 0
        val known = messageDao.existingSystemIds().toHashSet()
        var imported = 0
        messages.forEach { message ->
            val systemId = message.systemMessageId
            if (systemId != null && known.contains(systemId)) return@forEach
            messageDao.insert(message)
            imported++
        }
        MovaLog.i(TAG, "Mensajes importados: $imported (de ${messages.size} leídos)")
        return imported
    }

    /** Identificador del último mensaje fallido de una conversación (para el botón Reintentar). */
    suspend fun lastFailedId(normalizedAddress: String): Long? =
        messageDao.lastFailedOutgoingId(normalizedAddress)

    suspend fun delete(id: Long) = messageDao.deleteById(id)

    /** Resuelve el nombre del contacto si es posible (lo inyecta el contenedor). */
    fun normalize(address: String): String = PhoneNumbers.normalize(address)

    companion object {
        const val STATE_SENDING = "SENDING"
        const val STATE_SENT = "SENT"
        const val STATE_DELIVERED = "DELIVERED"
        const val STATE_READ = "READ"
        const val STATE_FAILED = "FAILED"
        const val STATE_RECEIVED = "RECEIVED"
        private const val TAG = "MessageRepository"
    }
}
