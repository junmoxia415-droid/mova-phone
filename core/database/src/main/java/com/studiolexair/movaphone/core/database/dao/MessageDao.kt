package com.studiolexair.movaphone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.database.entity.SmsTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY sentAt DESC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE normalizedAddress = :normalizedAddress ORDER BY sentAt ASC")
    fun observeConversation(normalizedAddress: String): Flow<List<MessageEntity>>

    @Query(
        """SELECT * FROM messages m
           WHERE m.sentAt = (SELECT MAX(sentAt) FROM messages WHERE normalizedAddress = m.normalizedAddress)
           ORDER BY m.sentAt DESC"""
    )
    fun observeConversations(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE body LIKE '%' || :query || '%' OR normalizedAddress LIKE '%' || :query || '%' ORDER BY sentAt DESC")
    fun search(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY sentAt DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET state = :state WHERE id = :id")
    suspend fun updateState(id: Long, state: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sms_templates ORDER BY isDefault DESC, title ASC")
    fun observeTemplates(): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates WHERE category = :category ORDER BY isDefault DESC, title ASC")
    suspend fun templatesByCategory(category: String): List<SmsTemplateEntity>

    @Query("SELECT * FROM sms_templates ORDER BY isDefault DESC LIMIT 1")
    suspend fun defaultTemplate(): SmsTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SmsTemplateEntity): Long

    @Query("UPDATE sms_templates SET title = :title, body = :body WHERE id = :id")
    suspend fun updateTemplate(id: Long, title: String, body: String)

    @Query("DELETE FROM sms_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)
}
