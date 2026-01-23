package com.codingskillshub.bitpigeon.domain.interfaces.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_message WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("SELECT * FROM chat_message WHERE chatGroupId = :chatGroupId ORDER BY timestamp ASC")
    fun getAllMessagesForChatGroup(chatGroupId: String): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_message WHERE chatGroupId = :chatGroupId")
    suspend fun deleteAllMessagesForChatGroupId(chatGroupId: String)

    @Query("SELECT * FROM chat_message ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMessage(): Flow<ChatMessage?>

    @Query("SELECT * FROM chat_message WHERE chatGroupId = :chatGroupId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMessageForChatGroup(chatGroupId: String): Flow<ChatMessage?>

    @Query("""
        SELECT * FROM chat_message
        WHERE id IN (
            SELECT id FROM chat_message
            GROUP BY chatGroupId
            HAVING MAX(timestamp)
        )
        ORDER BY timestamp DESC
    """)
    fun getLatestMessageForAllChatGroups(): Flow<List<ChatMessage>>
}