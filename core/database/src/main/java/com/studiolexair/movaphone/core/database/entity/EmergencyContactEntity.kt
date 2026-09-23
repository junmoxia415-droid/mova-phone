package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contacto de emergencia con prioridad. El orden de prioridad define a quién se
 * contacta primero durante un SOS (requisito 13 del proyecto).
 */
@Entity(tableName = "emergency_contacts", indices = [Index(value = ["priority"], unique = true)])
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val priority: Int,
    val relationship: String? = null,
    val allowCall: Boolean = true,
    val allowSms: Boolean = true,
    val shareLocation: Boolean = true,
    val isMedical: Boolean = false,
    val createdAt: Long
)
