package com.studiolexair.movaphone.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Regla de automatización persistida: Trigger → Condition → Action.
 * Se almacenan como texto para poder añadir nuevos tipos sin migrar el esquema
 * (ver domain/automation: TriggerType, ActionType).
 */
@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val triggerType: String,
    val triggerValue: String? = null,
    val conditionType: String? = null,
    val conditionValue: String? = null,
    val actionType: String,
    val actionValue: String? = null,
    val createdAt: Long,
    val lastRunAt: Long? = null,
    val runCount: Int = 0
)
