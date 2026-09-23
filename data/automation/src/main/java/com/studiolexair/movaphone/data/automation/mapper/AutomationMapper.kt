package com.studiolexair.movaphone.data.automation.mapper

import com.studiolexair.movaphone.core.database.entity.AutomationExecutionEntity
import com.studiolexair.movaphone.core.database.entity.AutomationRuleEntity
import com.studiolexair.movaphone.domain.automation.model.Action
import com.studiolexair.movaphone.domain.automation.model.ActionType
import com.studiolexair.movaphone.domain.automation.model.AutomationOutcome
import com.studiolexair.movaphone.domain.automation.model.AutomationRule
import com.studiolexair.movaphone.domain.automation.model.Condition
import com.studiolexair.movaphone.domain.automation.model.ConditionType
import com.studiolexair.movaphone.domain.automation.model.Trigger
import com.studiolexair.movaphone.domain.automation.model.TriggerType

object AutomationMapper {

    fun toDomain(entity: AutomationRuleEntity): AutomationRule = AutomationRule(
        id = entity.id,
        name = entity.name,
        enabled = entity.enabled,
        trigger = Trigger(
            type = runCatching { TriggerType.valueOf(entity.triggerType) }.getOrDefault(TriggerType.MANUAL),
            value = entity.triggerValue
        ),
        condition = entity.conditionType?.let {
            Condition(
                type = runCatching { ConditionType.valueOf(it) }.getOrDefault(ConditionType.ALWAYS),
                value = entity.conditionValue
            )
        },
        action = Action(
            type = runCatching { ActionType.valueOf(entity.actionType) }.getOrDefault(ActionType.NOTIFY),
            value = entity.actionValue
        ),
        lastRunAt = entity.lastRunAt,
        runCount = entity.runCount
    )

    fun toEntity(rule: AutomationRule, now: Long = System.currentTimeMillis()): AutomationRuleEntity =
        AutomationRuleEntity(
            id = rule.id,
            name = rule.name,
            enabled = rule.enabled,
            triggerType = rule.trigger.type.name,
            triggerValue = rule.trigger.value,
            conditionType = rule.condition?.type?.name,
            conditionValue = rule.condition?.value,
            actionType = rule.action.type.name,
            actionValue = rule.action.value,
            createdAt = now,
            lastRunAt = rule.lastRunAt,
            runCount = rule.runCount
        )

    fun outcomeToEntity(outcome: AutomationOutcome, timestamp: Long): AutomationExecutionEntity =
        AutomationExecutionEntity(
            ruleId = outcome.ruleId,
            ruleName = outcome.ruleName,
            triggerType = "",
            actionSummary = outcome.detail ?: "",
            success = outcome.success,
            detail = outcome.detail,
            executedAt = timestamp
        )

    fun entityToOutcome(entity: AutomationExecutionEntity): AutomationOutcome = AutomationOutcome(
        ruleId = entity.ruleId,
        ruleName = entity.ruleName,
        executed = true,
        success = entity.success,
        detail = entity.detail
    )
}
