package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Registro de llamada propio (historial enriquecido: spam, bloqueo, notas). */
@Entity(
    tableName = "call_records",
    indices = [Index(value = ["normalizedNumber"]), Index(value = ["startedAt"])]
)
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemCallId: Long? = null,
    val number: String,
    val normalizedNumber: String,
    val contactName: String? = null,
    val type: String, // INCOMING | OUTGOING | MISSED | REJECTED | BLOCKED
    val startedAt: Long,
    val durationSeconds: Long = 0,
    val isSpam: Boolean = false,
    val blockedByMova: Boolean = false,
    val simLabel: String? = null
)
