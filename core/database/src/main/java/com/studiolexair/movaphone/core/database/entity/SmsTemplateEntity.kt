package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Plantilla de mensaje rápido o de emergencia (requisito 14). */
@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val category: String = "quick", // quick | emergency
    val isDefault: Boolean = false,
    val createdAt: Long
)
