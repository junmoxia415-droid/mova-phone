package com.studiolexair.movaphone.domain.contacts.matcher

import com.google.common.truth.Truth.assertThat
import com.studiolexair.movaphone.core.common.util.TextNormalizer
import com.studiolexair.movaphone.domain.contacts.model.Contact
import org.junit.Test

/**
 * Casos reales de búsqueda de contactos.
 *
 * El más importante: el contacto se guarda como «Nena ❤️» y el usuario dice «llama a nena».
 * El emoji, los acentos y las mayúsculas no pueden impedir encontrar a la persona.
 */
class ContactMatcherTest {

    private fun contact(name: String, number: String, favorite: Boolean = false) = Contact(
        displayName = name,
        phoneNumber = number,
        normalizedNumber = number,
        isFavorite = favorite
    )

    private val nena = contact("Nena ❤️", "+53 5267 8747", favorite = true)
    private val mariaJose = contact("María José", "+34 611 223 344")
    private val juanPerez = contact("Juan Pérez", "+34 699 887 766")
    private val contactos = listOf(nena, mariaJose, juanPerez)

    @Test
    fun `sin emoji ni acentos la busqueda de nena encuentra a Nena`() {
        val match = ContactMatcher.best("llama a nena", contactos)
        assertThat(match).isNotNull()
        assertThat(match!!.contact.displayName).isEqualTo("Nena ❤️")
    }

    @Test
    fun `el emoji del contacto se ignora al comparar`() {
        // El nombre guardado pierde emoji, mayúsculas y acentos al compararlo.
        assertThat(TextNormalizer.normalize("Nena ❤️")).isEqualTo("nena")
        assertThat(TextNormalizer.normalize("María José")).isEqualTo("maria jose")
        // Y la frase dictada también: se quitan los artículos del principio.
        assertThat(TextNormalizer.normalizeQuery("  de A Nena  ")).isEqualTo("nena")
    }

    @Test
    fun `los acentos no impiden encontrar a Maria Jose`() {
        assertThat(ContactMatcher.best("maria jose", contactos)?.contact?.displayName)
            .isEqualTo("María José")
        assertThat(ContactMatcher.best("María José", contactos)?.contact?.displayName)
            .isEqualTo("María José")
    }

    @Test
    fun `buscar por los ultimos digitos del numero funciona`() {
        assertThat(ContactMatcher.best("8747", contactos)?.contact?.displayName)
            .isEqualTo("Nena ❤️")
    }

    @Test
    fun `solo las iniciales tambien encuentran al contacto`() {
        assertThat(ContactMatcher.best("juan pe", contactos)?.contact?.displayName)
            .isEqualTo("Juan Pérez")
    }

    @Test
    fun `un error de dictado leve sigue encontrando el contacto`() {
        assertThat(ContactMatcher.best("nina", contactos)?.contact?.displayName)
            .isEqualTo("Nena ❤️")
    }

    @Test
    fun `una consulta sin relacion no devuelve coincidencias`() {
        assertThat(ContactMatcher.best("zzzz qqqq", contactos)).isNull()
    }

    @Test
    fun `rank deja primero al favorito`() {
        val ranked = ContactMatcher.rank("nena", contactos)
        assertThat(ranked).isNotEmpty()
        assertThat(ranked.first().displayName).isEqualTo("Nena ❤️")
    }
}
