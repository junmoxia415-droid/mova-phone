package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Patrón usado por el clasificador local de spam (prefijos, fragmentos o números reportados). */
@Entity(tableName = "spam_rules")
data class SpamRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val type: String, // PREFIX | CONTAINS | EXACT | USER_REPORT
    val weight: Int = 1,
    val source: String = "local",
    val createdAt: Long
)
