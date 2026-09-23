package com.studiolexair.movaphone.data.contacts.repository

import com.studiolexair.movaphone.core.database.dao.ContactDao
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.data.contacts.mapper.ContactMapper
import com.studiolexair.movaphone.data.contacts.source.DeviceContactsDataSource
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repositorio de contactos: Local First.
 * Los contactos propios viven en Room; la agenda del sistema se importa
 * cuando el usuario lo pide y se marca su origen.
 */
class ContactsRepositoryImpl(
    private val contactDao: ContactDao,
    private val deviceContacts: DeviceContactsDataSource
) : ContactsRepository {

    override fun observeContacts(): Flow<List<Contact>> =
        contactDao.observeAll().map { list -> list.map(ContactMapper::toDomain) }

    override fun observeFavorites(): Flow<List<Contact>> =
        contactDao.observeFavorites().map { list -> list.map(ContactMapper::toDomain) }

    override fun observePrivateContacts(): Flow<List<Contact>> =
        contactDao.observePrivate().map { list -> list.map(ContactMapper::toDomain) }

    override fun searchContacts(query: String): Flow<List<Contact>> =
        contactDao.search(query).map { list -> list.map(ContactMapper::toDomain) }

    override suspend fun contactsByNumber(normalizedNumber: String): Contact? =
        contactDao.findByNormalizedNumber(normalizedNumber)?.let(ContactMapper::toDomain)

    override suspend fun getContact(id: Long): Contact? =
        contactDao.findById(id)?.let(ContactMapper::toDomain)

    override suspend fun saveContact(contact: Contact): Long {
        val existing = contactDao.findByNormalizedNumber(contact.normalizedNumber)
        return contactDao.insert(ContactMapper.toEntity(contact, existing))
    }

    override suspend fun updateContact(contact: Contact) {
        val existing = contactDao.findById(contact.id)
        contactDao.update(ContactMapper.toEntity(contact, existing))
    }

    override suspend fun deleteContact(contact: Contact) {
        contactDao.findById(contact.id)?.let { contactDao.delete(it) }
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        contactDao.setFavorite(id, favorite, System.currentTimeMillis())
    }

    override suspend fun setPrivate(id: Long, isPrivate: Boolean) {
        contactDao.setPrivate(id, isPrivate, System.currentTimeMillis())
    }

    /** Importa la agenda del sistema sin duplicar números ya existentes. */
    override suspend fun importFromDevice(): Int {
        val deviceContactsList = deviceContacts.readAll()
        var imported = 0
        deviceContactsList.forEach { entity ->
            val existing = contactDao.findByNormalizedNumber(entity.normalizedNumber)
            if (existing == null) {
                contactDao.insert(entity)
                imported++
            }
        }
        MovaLog.i(TAG, "Contactos importados: $imported de ${deviceContactsList.size}")
        return imported
    }

    override suspend fun suggestions(query: String, limit: Int): List<Contact> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        // Lectura puntual para el marcador: se limita el número de sugerencias.
        return contactDao.search(normalized).first().take(limit).map(ContactMapper::toDomain)
    }

    private companion object {
        const val TAG = "ContactsRepository"
    }
}
