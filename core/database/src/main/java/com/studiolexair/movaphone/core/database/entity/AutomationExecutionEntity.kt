package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Historial de ejecuciones del motor de automatización (auditable). */
@Entity(tableName = "automation_executions", indices = [Index(value = ["executedAt"])])
data class AutomationExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val ruleName: String,
    val triggerType: String,
    val actionSummary: String,
    val success: Boolean,
    val detail: String? = null,
    val executedAt: Long
)
