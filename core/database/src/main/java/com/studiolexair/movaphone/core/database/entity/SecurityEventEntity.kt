package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Evento de seguridad (bloqueos, accesos, cambios de PIN, intentos fallidos). */
@Entity(tableName = "security_events", indices = [Index(value = ["occurredAt"])])
data class SecurityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // BLOCKED_CALL | SOS_TRIGGERED | PIN_CHANGED | UNLOCK_FAILED | SPAM_DETECTED...
    val description: String,
    val severity: String = "info", // info | warning | critical
    val metadata: String? = null,
    val occurredAt: Long
)
