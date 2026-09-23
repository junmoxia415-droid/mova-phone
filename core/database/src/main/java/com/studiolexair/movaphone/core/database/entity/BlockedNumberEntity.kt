package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Número bloqueado por el usuario (bloqueo propio de MOVA, además del del sistema). */
@Entity(tableName = "blocked_numbers", indices = [Index(value = ["normalizedNumber"], unique = true)])
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedNumber: String,
    val phoneNumber: String,
    val label: String? = null,
    val reason: String = "manual",
    val blockCalls: Boolean = true,
    val blockMessages: Boolean = true,
    val createdAt: Long
)
