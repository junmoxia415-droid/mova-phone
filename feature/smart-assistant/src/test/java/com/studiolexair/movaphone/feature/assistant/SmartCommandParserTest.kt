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
        // El destinatario se conserva tal cual se dijo («Luis»), no en minúsculas: es el nombre
        // que luego se busca en la agenda y el que MOVA repite en voz alta.
        assertThat(message.target).isEqualTo("Luis")
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
    fun `conserva tildes y mayusculas del nombre dictado`() {
        val call = SmartCommandParser.parse("Llamar a Mamá") as SmartCommand.Call
        assertThat(call.target).isEqualTo("Mamá")
        val nena = SmartCommandParser.parse("llamar a la nena") as SmartCommand.Call
        assertThat(nena.target).isEqualTo("nena")
        val emoji = SmartCommandParser.parse("llama a Nena ❤️") as SmartCommand.Call
        assertThat(emoji.target).isEqualTo("Nena")
    }

    @Test
    fun `orden vacía no rompe el asistente`() {
        assertThat(SmartCommandParser.parse("   ")).isInstanceOf(SmartCommand.Unknown::class.java)
    }
}
