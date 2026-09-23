package com.studiolexair.movaphone.feature.assistant

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El asistente interpreta órdenes en el dispositivo: estas pruebas fijan el
 * contrato de lo que entiende y, sobre todo, qué se considera acción sensible.
 */
class SmartCommandParserTest {

    @Test
    fun `entiende llamar a un contacto`() {
        val command = SmartCommandParser.parse("Llamar a mamá")
        assertThat(command).isInstanceOf(SmartCommand.Call::class.java)
        assertThat((command as SmartCommand.Call).target).isEqualTo("mamá")
        assertThat(command.isSensitive).isTrue()
    }

    @Test
    fun `entiende llamar a un número dictado`() {
        val command = SmartCommandParser.parse("Llamar 600123456")
        assertThat(command).isInstanceOf(SmartCommand.Call::class.java)
        assertThat((command as SmartCommand.Call).target).isEqualTo("600123456")
    }

    @Test
    fun `entiende enviar un mensaje con texto`() {
        val command = SmartCommandParser.parse("Enviar mensaje a Luis diciendo llego tarde")
        assertThat(command).isInstanceOf(SmartCommand.SendMessage::class.java)
        val message = command as SmartCommand.SendMessage
        assertThat(message.target).isEqualTo("luis")
        assertThat(message.body).isEqualTo("llego tarde")
    }

    @Test
    fun `entiende abrir pantallas`() {
        assertThat(SmartCommandParser.parse("Abrir seguridad"))
            .isEqualTo(SmartCommand.Open(SmartCommand.Destination.SECURITY))
        assertThat(SmartCommandParser.parse("ajustes"))
            .isEqualTo(SmartCommand.Open(SmartCommand.Destination.SETTINGS))
        assertThat(SmartCommandParser.parse("abrir historial"))
            .isEqualTo(SmartCommand.Open(SmartCommand.Destination.CALLS))
    }

    @Test
    fun `la emergencia tiene prioridad y pide confirmación`() {
        val command = SmartCommandParser.parse("necesito ayuda, emergencia")
        assertThat(command).isEqualTo(SmartCommand.StartEmergency)
        assertThat(command.isSensitive).isTrue()
    }

    @Test
    fun `compartir ubicación es una acción sensible`() {
        val command = SmartCommandParser.parse("comparte mi ubicación")
        assertThat(command).isEqualTo(SmartCommand.ShareLocation)
        assertThat(command.isSensitive).isTrue()
    }

    @Test
    fun `una orden desconocida no ejecuta nada`() {
        val command = SmartCommandParser.parse("hazme un café")
        assertThat(command).isInstanceOf(SmartCommand.Unknown::class.java)
        assertThat(command.isSensitive).isFalse()
    }

    @Test
    fun `orden vacía no rompe el asistente`() {
        assertThat(SmartCommandParser.parse("   ")).isInstanceOf(SmartCommand.Unknown::class.java)
    }
}
