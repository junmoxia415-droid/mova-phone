package com.studiolexair.movaphone.domain.contacts.usecase

import com.studiolexair.movaphone.core.common.result.ErrorCode
import com.studiolexair.movaphone.core.common.result.MovaResult
import com.studiolexair.movaphone.core.common.result.failure
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow

class GetContactsUseCase(private val repository: ContactsRepository) {
    operator fun invoke(): Flow<List<Contact>> = repository.observeContacts()
}

class GetFavoritesUseCase(private val repository: ContactsRepository) {
    operator fun invoke(): Flow<List<Contact>> = repository.observeFavorites()
}

class GetPrivateContactsUseCase(private val repository: ContactsRepository) {
    operator fun invoke(): Flow<List<Contact>> = repository.observePrivateContacts()
}

class SearchContactsUseCase(private val repository: ContactsRepository) {
    operator fun invoke(query: String): Flow<List<Contact>> =
        if (query.isBlank()) repository.observeContacts() else repository.searchContacts(query.trim())
}

class ToggleFavoriteUseCase(private val repository: ContactsRepository) {
    suspend operator fun invoke(id: Long, favorite: Boolean): MovaResult<Unit> = try {
        repository.setFavorite(id, favorite)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible actualizar el contacto.", t.message)
    }
}

class SaveContactUseCase(private val repository: ContactsRepository) {
    suspend operator fun invoke(contact: Contact): MovaResult<Long> {
        if (contact.displayName.isBlank() && contact.phoneNumber.isBlank()) {
            return failure(ErrorCode.VALIDATION, "Añade al menos un nombre o un número de teléfono.")
        }
        return try {
            val prepared = contact.copy(
                normalizedNumber = PhoneNumbers.normalize(contact.phoneNumber),
                displayName = contact.displayName.ifBlank { PhoneNumbers.pretty(contact.phoneNumber) }
            )
            val id = repository.saveContact(prepared)
            MovaResult.Success(id)
        } catch (t: Throwable) {
            failure(ErrorCode.UNKNOWN, "No fue posible guardar el contacto.", t.message)
        }
    }
}

class DeleteContactUseCase(private val repository: ContactsRepository) {
    suspend operator fun invoke(contact: Contact): MovaResult<Unit> = try {
        repository.deleteContact(contact)
        MovaResult.Success(Unit)
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible eliminar el contacto.", t.message)
    }
}

class ImportDeviceContactsUseCase(private val repository: ContactsRepository) {
    suspend operator fun invoke(): MovaResult<Int> = try {
        MovaResult.Success(repository.importFromDevice())
    } catch (t: SecurityException) {
        failure(ErrorCode.PERMISSION_DENIED, "MOVA Phone necesita permiso para leer tus contactos.")
    } catch (t: Throwable) {
        failure(ErrorCode.UNKNOWN, "No fue posible importar los contactos.", t.message)
    }
}
