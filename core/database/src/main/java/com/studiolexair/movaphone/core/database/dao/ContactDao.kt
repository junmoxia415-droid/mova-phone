package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studiolexair.movaphone.core.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isPrivate = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun observePrivate(): Flow<List<ContactEntity>>

    @Query(
        """SELECT * FROM contacts
           WHERE displayName LIKE '%' || :query || '%'
              OR normalizedNumber LIKE '%' || :query || '%'
              OR phoneNumber LIKE '%' || :query || '%'
           ORDER BY isFavorite DESC, displayName COLLATE NOCASE ASC"""
    )
    fun search(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, displayName COLLATE NOCASE ASC")
    suspend fun allOnce(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun findByNormalizedNumber(normalizedNumber: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ContactEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contacts: List<ContactEntity>): List<Long>

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("UPDATE contacts SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, updatedAt: Long)

    @Query("UPDATE contacts SET isPrivate = :isPrivate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPrivate(id: Long, isPrivate: Boolean, updatedAt: Long)

    @Query("DELETE FROM contacts")
    suspend fun clear()
}
