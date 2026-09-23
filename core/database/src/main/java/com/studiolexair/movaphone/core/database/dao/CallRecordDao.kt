package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studiolexair.movaphone.core.database.entity.CallRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordDao {

    @Query("SELECT * FROM call_records ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE type = :type ORDER BY startedAt DESC")
    fun observeByType(type: String): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE isSpam = 1 ORDER BY startedAt DESC")
    fun observeSpam(): Flow<List<CallRecordEntity>>

    @Query("SELECT COUNT(*) FROM call_records WHERE type = 'MISSED' AND startedAt >= :since")
    fun observeMissedCount(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CallRecordEntity): Long

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clear()

    @Query("SELECT * FROM call_records WHERE systemCallId = :systemCallId LIMIT 1")
    suspend fun findBySystemId(systemCallId: Long): CallRecordEntity?

    @Query("UPDATE call_records SET isSpam = :isSpam WHERE normalizedNumber = :normalizedNumber")
    suspend fun markSpamByNumber(normalizedNumber: String, isSpam: Boolean)
}
