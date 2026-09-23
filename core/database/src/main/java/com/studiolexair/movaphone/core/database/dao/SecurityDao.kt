package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.database.entity.SecurityEventEntity
import com.studiolexair.movaphone.core.database.entity.SpamRuleEntity
import com.studiolexair.movaphone.core.database.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {

    // --- números bloqueados ---
    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun observeBlocked(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun isBlocked(normalizedNumber: String): BlockedNumberEntity?

    @Query("SELECT * FROM blocked_numbers")
    suspend fun allBlocked(): List<BlockedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(entity: BlockedNumberEntity): Long

    @Delete
    suspend fun unblock(entity: BlockedNumberEntity)

    // --- reglas de spam ---
    @Query("SELECT * FROM spam_rules")
    suspend fun allSpamRules(): List<SpamRuleEntity>

    @Query("SELECT * FROM spam_rules")
    fun observeSpamRules(): Flow<List<SpamRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSpamRule(rule: SpamRuleEntity): Long

    @Query("DELETE FROM spam_rules WHERE id = :id")
    suspend fun deleteSpamRule(id: Long)

    // --- eventos de seguridad ---
    @Query("SELECT * FROM security_events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeEvents(limit: Int): Flow<List<SecurityEventEntity>>

    @Insert
    suspend fun logEvent(event: SecurityEventEntity): Long

    @Query("DELETE FROM security_events WHERE occurredAt < :before")
    suspend fun purgeEventsBefore(before: Long)

    @Query("DELETE FROM security_events")
    suspend fun clearEvents()

    // --- contactos de confianza ---
    @Query("SELECT * FROM trusted_contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeTrusted(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts")
    suspend fun allTrusted(): List<TrustedContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrusted(contact: TrustedContactEntity): Long

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun removeTrusted(id: Long)
}
