package com.studiolexair.movaphone.data.messages.model

import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import org.junit.Test

/**
 * Pruebas de la sección "Te han escrito": agrupación por persona, último mensaje,
 * recuento de no leídos y orden por fecha.
 */
class SenderAggregatorTest {

    private fun incoming(
        address: String,
        normalized: String,
        body: String,
        at: Long,
        name: String? = null,
        state: String = "RECEIVED"
    ) = MessageEntity(
        address = address,
        normalizedAddress = normalized,
        contactName = name,
        body = body,
        isIncoming = true,
        sentAt = at,
        state = state
    )

    private fun outgoing(address: String, normalized: String, body: String, at: Long) = MessageEntity(
        address = address,
        normalizedAddress = normalized,
        body = body,
        isIncoming = false,
        sentAt = at,
        state = "SENT"
    )

    @Test
    fun `agrupa por persona y usa el ultimo mensaje recibido`() {
        val resumen = SenderAggregator.summarize(
            listOf(
                incoming("600111222", "+34600111222", "Hola", 1_000, name = "Ana"),
                incoming("600111222", "+34600111222", "¿Quedamos?", 5_000, name = "Ana"),
                outgoing("600111222", "+34600111222", "Vale", 6_000)
            )
        )

        assertThat(resumen).hasSize(1)
        assertThat(resumen[0].displayName).isEqualTo("Ana")
        assertThat(resumen[0].lastBody).isEqualTo("¿Quedamos?")
        assertThat(resumen[0].lastAt).isEqualTo(5_000)
        // Cuenta sólo los mensajes de esa persona, entrantes y salientes.
        assertThat(resumen[0].totalMessages).isEqualTo(3)
    }

    @Test
    fun `cuenta como no leidos solo los mensajes recibidos`() {
        val resumen = SenderAggregator.summarize(
            listOf(
                incoming("600111222", "+34600111222", "Uno", 1_000),
                incoming("600111222", "+34600111222", "Dos", 2_000),
                incoming("600111222", "+34600111222", "Leído", 3_000, state = "READ"),
                outgoing("600111222", "+34600111222", "Respuesta", 4_000)
            )
        )

        assertThat(resumen[0].unreadCount).isEqualTo(2)
    }

    @Test
    fun `ordena por fecha descendente y usa el numero si no hay nombre`() {
        val resumen = SenderAggregator.summarize(
            listOf(
                incoming("600000000", "+34600000000", "Antiguo", 1_000),
                incoming("611111111", "+34611111111", "Nuevo", 9_000),
                incoming("622222222", "+34622222222", "Medio", 5_000)
            )
        )

        assertThat(resumen.map { it.normalizedAddress })
            .containsExactly("+34611111111", "+34622222222", "+34600000000")
            .inOrder()
        assertThat(resumen[0].displayName).isEqualTo("611111111")
    }

    @Test
    fun `sin mensajes entrantes no hay personas`() {
        val resumen = SenderAggregator.summarize(
            listOf(outgoing("600111222", "+34600111222", "Yo escribo", 1_000))
        )

        assertThat(resumen).isEmpty()
    }
}
