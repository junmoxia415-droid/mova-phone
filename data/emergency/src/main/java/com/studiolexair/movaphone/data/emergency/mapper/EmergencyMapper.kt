package com.studiolexair.movaphone.data.emergency.mapper

import com.studiolexair.movaphone.core.database.entity.EmergencyContactEntity
import com.studiolexair.movaphone.domain.emergency.model.EmergencyContact

object EmergencyMapper {

    fun toDomain(entity: EmergencyContactEntity): EmergencyContact = EmergencyContact(
        id = entity.id,
        name = entity.name,
        phoneNumber = entity.phoneNumber,
        normalizedNumber = entity.normalizedNumber,
        priority = entity.priority,
        relationship = entity.relationship,
        allowCall = entity.allowCall,
        allowSms = entity.allowSms,
        shareLocation = entity.shareLocation,
        isMedical = entity.isMedical
    )

    fun toEntity(contact: EmergencyContact, now: Long = System.currentTimeMillis()): EmergencyContactEntity =
        EmergencyContactEntity(
            id = contact.id,
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            normalizedNumber = contact.normalizedNumber,
            priority = contact.priority,
            relationship = contact.relationship,
            allowCall = contact.allowCall,
            allowSms = contact.allowSms,
            shareLocation = contact.shareLocation,
            isMedical = contact.isMedical,
            createdAt = now
        )
}
