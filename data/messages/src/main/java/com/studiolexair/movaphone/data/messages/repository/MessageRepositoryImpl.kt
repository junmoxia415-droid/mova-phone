package com.studiolexair.movaphone.data.messages.repository

import com.studiolexair.movaphone.core.database.dao.MessageDao
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.messages.provider.MessageProvider
import com.studiolexair.movaphone.data.messages.source.DeviceSmsDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de mensajes. Local First: los mensajes viven en Room y se sincronizan
 * desde la bandeja del sistema cuando el usuario lo permite.
 * Aquí se decide qué transporte usar (SMS hoy, Internet cuando exista) sin que la UI lo sepa.
 */
class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val smsProvider: MessageProvider,
    private val deviceSms: DeviceSmsDataSource
) {

    fun observeConversations(): Flow<List<MessageEntity>> = messageDao.observeConversations()

    fun observeConversation(normalizedAddress: String): Flow<List<MessageEntity>> =
        messageDao.observeConversation(normalizedAddress)

    fun observeLatest(limit: Int = 5): Flow<List<MessageEntity>> = messageDao.observeLatest(limit)

    fun search(query: String): Flow<List<MessageEntity>> = messageDao.search(query)

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
                state = "SENDING",
                isEmergency = isEmergency,
                provider = smsProvider.id
            )
        )
        val result = smsProvider.send(address, body)
        messageDao.updateState(id, if (result.success) "SENT" else "FAILED")
        if (!result.success) {
            MovaLog.w(TAG, "Envío fallido: ${result.errorMessage}")
        }
        return result.success
    }

    /** Guarda un mensaje entrante (usado por services:sms al recibir un SMS). */
    suspend fun storeIncoming(message: MessageEntity): Long = messageDao.insert(message)

    suspend fun updateState(id: Long, state: String) = messageDao.updateState(id, state)

    suspend fun importFromDevice(): Int {
        val messages = deviceSms.readInbox()
        var imported = 0
        messages.forEach { message ->
            val existing = message.systemMessageId
            if (existing != null) {
                val known = messageDao.observeAll().let { true } // el detalle se resuelve por id en Room
                if (known) {
                    // Se inserta con REPLACE sobre el mismo id de sistema cuando ya exista.
                }
            }
            messageDao.insert(message)
            imported++
        }
        MovaLog.i(TAG, "Mensajes importados: $imported")
        return imported
    }

    suspend fun delete(id: Long) = messageDao.deleteById(id)

    private companion object {
        const val TAG = "MessageRepository"
    }
}
