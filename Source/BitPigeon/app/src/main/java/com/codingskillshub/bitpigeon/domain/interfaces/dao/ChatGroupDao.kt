package com.codingskillshub.bitpigeon.domain.interfaces.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupDb
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupMember
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChatGroup(chatGroup: ChatGroupDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ChatGroupMember>)

    @Query("DELETE FROM chat_group WHERE id = :chatGroupId")
    suspend fun deleteChatGroupById(chatGroupId: String)

    @Transaction
    @Query("SELECT * FROM chat_group WHERE id = :chatGroupId")
    fun getChatGroupById(chatGroupId: String): Flow<ChatGroup?>

    @Transaction
    @Query("SELECT * FROM chat_group")
    fun getAllChatGroups(): Flow<List<ChatGroup>>
}