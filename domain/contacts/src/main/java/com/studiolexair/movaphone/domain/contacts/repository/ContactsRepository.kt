package com.studiolexair.movaphone.domain.contacts.repository

import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.model.ContactMatch
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de contactos. La implementación decide si el origen es local (Room)
 * o la agenda del sistema (ContactsContract), sin que el resto de la app lo sepa.
 */
interface ContactsRepository {
    fun observeContacts(): Flow<List<Contact>>
    fun observeFavorites(): Flow<List<Contact>>
    fun observePrivateContacts(): Flow<List<Contact>>
    fun searchContacts(query: String): Flow<List<Contact>>
    suspend fun contactsByNumber(normalizedNumber: String): Contact?
    suspend fun getContact(id: Long): Contact?

    suspend fun saveContact(contact: Contact): Long
    suspend fun updateContact(contact: Contact)
    suspend fun deleteContact(contact: Contact)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setPrivate(id: Long, isPrivate: Boolean)

    /** Importa la agenda del sistema a la base local (requiere permiso de lectura). */
    suspend fun importFromDevice(): Int
    /** Sugerencias para el marcador a partir de un número o de un nombre dictado. */
    suspend fun suggestions(query: String, limit: Int = 6): List<Contact>

    /**
     * Mejor coincidencia para una orden hablada o escrita (“llama a nena”, “marca 8747”).
     * Devuelve también el motivo, para poder decir al usuario por qué eligió ese contacto.
     */
    suspend fun bestMatch(query: String): ContactMatch?
}
