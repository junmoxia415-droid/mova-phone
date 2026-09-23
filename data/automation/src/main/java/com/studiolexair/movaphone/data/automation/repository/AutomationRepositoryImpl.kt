package com.studiolexair.movaphone.data.automation.repository

import com.studiolexair.movaphone.core.database.dao.AutomationDao
import com.studiolexair.movaphone.data.automation.mapper.AutomationMapper
import com.studiolexair.movaphone.domain.automation.model.AutomationOutcome
import com.studiolexair.movaphone.domain.automation.model.AutomationRule
import com.studiolexair.movaphone.domain.automation.model.TriggerType
import com.studiolexair.movaphone.domain.automation.repository.AutomationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutomationRepositoryImpl(private val automationDao: AutomationDao) : AutomationRepository {

    override fun observeRules(): Flow<List<AutomationRule>> =
        automationDao.observeRules().map { list -> list.map(AutomationMapper::toDomain) }

    override suspend fun rules(): List<AutomationRule> =
        automationDao.allRules().map(AutomationMapper::toDomain)

    override suspend fun rulesForTrigger(type: TriggerType): List<AutomationRule> =
        automationDao.rulesByTrigger(type.name).map(AutomationMapper::toDomain)

    override suspend fun rule(id: Long): AutomationRule? =
        automationDao.ruleById(id)?.let(AutomationMapper::toDomain)

    override suspend fun save(rule: AutomationRule): Long =
        automationDao.insert(AutomationMapper.toEntity(rule))

    override suspend fun delete(rule: AutomationRule) {
        automationDao.ruleById(rule.id)?.let { automationDao.delete(it) }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) = automationDao.setEnabled(id, enabled)

    override suspend fun markExecuted(id: Long, timestamp: Long) = automationDao.markExecuted(id, timestamp)

    override fun observeHistory(limit: Int): Flow<List<AutomationOutcome>> =
        automationDao.observeExecutions(limit).map { list -> list.map(AutomationMapper::entityToOutcome) }

    override suspend fun logOutcome(outcome: AutomationOutcome, timestamp: Long) {
        automationDao.logExecution(AutomationMapper.outcomeToEntity(outcome, timestamp))
    }

    override suspend fun purgeHistory(before: Long) = automationDao.purgeExecutionsBefore(before)
}
