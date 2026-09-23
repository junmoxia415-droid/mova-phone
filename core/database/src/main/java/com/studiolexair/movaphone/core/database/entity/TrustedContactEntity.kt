package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Contacto de confianza (requisito 16: contactos de confianza del centro de seguridad). */
@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val canReceiveLocation: Boolean = true,
    val canBeNotified: Boolean = true,
    val createdAt: Long
)
