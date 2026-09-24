package com.studiolexair.movaphone.feature.assistant

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El catálogo de órdenes: todo lo que MOVA presume de saber hacer tiene que entenderse de
 * verdad. Si alguien añade un ejemplo al catálogo y el intérprete no lo resuelve, esta prueba
 * lo avisa antes de llegar al teléfono.
 */
class CommandLibraryTest {

    @Test
    fun `todos los ejemplos rapidos se entienden`() {
        CommandLibrary.quickExamples.forEach { example ->
            val command = SmartCommandParser.parse(example)
            assertThat(command).isNotInstanceOf(SmartCommand.Unknown::class.java)
        }
    }

    @Test
    fun `el catalogo es completo y esta explicado`() {
        assertThat(CommandLibrary.capabilities.size).isAtLeast(10)
        CommandLibrary.capabilities.forEach { capability ->
            assertThat(capability.title).isNotEmpty()
            assertThat(capability.what).isNotEmpty()
            assertThat(capability.examples).isNotEmpty()
            // Cada ejemplo del catálogo tiene que ser una orden que MOVA entiende.
            capability.examples.forEach { example ->
                assertThat(SmartCommandParser.parse(example))
                    .isNotInstanceOf(SmartCommand.Unknown::class.java)
            }
        }
    }

    @Test
    fun `el vocabulario cubre las palabras clave`() {
        assertThat(CommandLibrary.vocabulary).containsAtLeast("llamar", "mensaje", "ubicacion", "emergencia")
    }
}
