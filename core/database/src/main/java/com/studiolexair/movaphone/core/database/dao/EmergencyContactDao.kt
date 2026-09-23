package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studiolexair.movaphone.core.database.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC")
    fun observeAll(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC")
    suspend fun getAll(): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC LIMIT :limit")
    suspend fun getFirst(limit: Int): List<EmergencyContactEntity>

    @Query("SELECT COALESCE(MAX(priority), 0) FROM emergency_contacts")
    suspend fun maxPriority(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: EmergencyContactEntity): Long

    @Update
    suspend fun update(contact: EmergencyContactEntity)

    @Delete
    suspend fun delete(contact: EmergencyContactEntity)

    @Query("UPDATE emergency_contacts SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: Int)

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun count(): Int
}
