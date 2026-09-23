package com.studiolexair.movaphone.data.contacts.mapper

import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.ContactEntity
import com.studiolexair.movaphone.domain.contacts.model.Contact
import com.studiolexair.movaphone.domain.contacts.model.ContactSource

/** Conversión entre capas. Evita exponer entidades de Room al dominio. */
object ContactMapper {

    fun toDomain(entity: ContactEntity): Contact = Contact(
        id = entity.id,
        displayName = entity.displayName,
        phoneNumber = entity.phoneNumber,
        normalizedNumber = entity.normalizedNumber,
        secondaryNumber = entity.secondaryNumber,
        email = entity.email,
        photoUri = entity.photoUri,
        notes = entity.notes,
        groupName = entity.groupName,
        isFavorite = entity.isFavorite,
        isPrivate = entity.isPrivate,
        source = ContactSource.LOCAL
    )

    fun toEntity(contact: Contact, existing: ContactEntity? = null): ContactEntity {
        val now = System.currentTimeMillis()
        return ContactEntity(
            id = existing?.id ?: contact.id,
            deviceContactId = existing?.deviceContactId,
            displayName = contact.displayName,
            phoneNumber = contact.phoneNumber,
            normalizedNumber = contact.normalizedNumber.ifBlank { PhoneNumbers.normalize(contact.phoneNumber) },
            secondaryNumber = contact.secondaryNumber,
            email = contact.email,
            photoUri = contact.photoUri,
            notes = contact.notes,
            groupName = contact.groupName,
            isFavorite = contact.isFavorite,
            isPrivate = contact.isPrivate,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
    }
}
