package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.studiolexair.movaphone.core.database.entity.LocationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM location_records ORDER BY recordedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LocationRecordEntity>>

    @Query("SELECT * FROM location_records WHERE source = :source ORDER BY recordedAt DESC LIMIT :limit")
    fun observeBySource(source: String, limit: Int): Flow<List<LocationRecordEntity>>

    @Query("SELECT * FROM location_records ORDER BY recordedAt DESC LIMIT 1")
    suspend fun lastKnown(): LocationRecordEntity?

    @Insert
    suspend fun insert(record: LocationRecordEntity): Long

    @Query("DELETE FROM location_records WHERE recordedAt < :before")
    suspend fun purgeBefore(before: Long)

    @Query("DELETE FROM location_records")
    suspend fun clear()
}
