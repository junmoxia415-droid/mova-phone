package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studiolexair.movaphone.core.database.entity.AutomationExecutionEntity
import com.studiolexair.movaphone.core.database.entity.AutomationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    @Query("SELECT * FROM automation_rules ORDER BY createdAt DESC")
    fun observeRules(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules ORDER BY createdAt DESC")
    suspend fun allRules(): List<AutomationRuleEntity>

    @Query("SELECT * FROM automation_rules WHERE enabled = 1")
    suspend fun enabledRules(): List<AutomationRuleEntity>

    @Query("SELECT * FROM automation_rules WHERE triggerType = :triggerType AND enabled = 1")
    suspend fun rulesByTrigger(triggerType: String): List<AutomationRuleEntity>

    @Query("SELECT * FROM automation_rules WHERE id = :id LIMIT 1")
    suspend fun ruleById(id: Long): AutomationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutomationRuleEntity): Long

    @Update
    suspend fun update(rule: AutomationRuleEntity)

    @Delete
    suspend fun delete(rule: AutomationRuleEntity)

    @Query("UPDATE automation_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE automation_rules SET lastRunAt = :timestamp, runCount = runCount + 1 WHERE id = :id")
    suspend fun markExecuted(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM automation_rules WHERE enabled = 1")
    fun observeEnabledCount(): Flow<Int>

    @Query("SELECT * FROM automation_executions ORDER BY executedAt DESC LIMIT :limit")
    fun observeExecutions(limit: Int): Flow<List<AutomationExecutionEntity>>

    @Insert
    suspend fun logExecution(execution: AutomationExecutionEntity): Long

    @Query("DELETE FROM automation_executions WHERE executedAt < :before")
    suspend fun purgeExecutionsBefore(before: Long)
}
