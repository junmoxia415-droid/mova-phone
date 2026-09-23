package com.studiolexair.movaphone.data.messages.model

import com.studiolexair.movaphone.core.database.entity.MessageEntity

/**
 * Agrupa los mensajes entrantes por persona que escribió.
 *
 * Es lógica pura (sin Android ni Room) para poder probarla: alimenta la sección
 * **"Te han escrito"** de la pantalla de Mensajes, donde el usuario ve quién le ha
 * escrito, el último texto, cuándo y cuántos mensajes quedan sin leer.
 */
object SenderAggregator {

    fun summarize(messages: List<MessageEntity>): List<SenderSummary> =
        messages.filter { it.isIncoming }
            .groupBy { it.normalizedAddress }
            .map { (normalized, incoming) ->
                val last = incoming.maxByOrNull { it.sentAt }
                val name = incoming.firstNotNullOfOrNull { it.contactName?.takeIf { candidate -> candidate.isNotBlank() } }
                SenderSummary(
                    address = last?.address ?: normalized,
                    normalizedAddress = normalized,
                    displayName = name ?: last?.address ?: normalized,
                    lastBody = last?.body.orEmpty(),
                    lastAt = last?.sentAt ?: 0L,
                    totalMessages = messages.count { it.normalizedAddress == normalized },
                    unreadCount = incoming.count { it.state == STATE_RECEIVED }
                )
            }
            .sortedByDescending { it.lastAt }

    private const val STATE_RECEIVED = "RECEIVED"
}
