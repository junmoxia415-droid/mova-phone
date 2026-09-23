package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mensaje SMS (o futuro mensaje online: el origen se distingue con [provider]). */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["normalizedAddress"]), Index(value = ["sentAt"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemMessageId: Long? = null,
    val address: String,
    val normalizedAddress: String,
    val contactName: String? = null,
    val body: String,
    val isIncoming: Boolean,
    val sentAt: Long,
    val state: String = "RECEIVED", // SENDING | SENT | DELIVERED | FAILED | RECEIVED | READ
    val isEmergency: Boolean = false,
    val provider: String = "sms" // sms | internet (arquitectura preparada para chat online)
)
