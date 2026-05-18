package com.codingskillshub.bitpigeon.domain.interfaces.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codingskillshub.bitpigeon.domain.entities.Attachment
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Query("DELETE FROM attachment WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: String)

    @Query("SELECT * FROM attachment WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): Attachment?

    @Query("SELECT * FROM attachment WHERE messageId = :messageId")
    fun getAllAttachmentsForMessage(messageId: String): Flow<List<Attachment>>

    @Query("SELECT * FROM attachment WHERE messageId = :messageId")
    suspend fun getAttachmentsForMessageSync(messageId: String): List<Attachment>

    @Query("SELECT * FROM attachment WHERE chatGroupId = :chatGroupId")
    fun getAllAttachmentsForChatGroup(chatGroupId: String): Flow<List<Attachment>>

    @Query("SELECT * FROM attachment WHERE chatGroupId = :chatGroupId")
    fun getAllAttachmentsForChatGroupSync(chatGroupId: String): List<Attachment>

    @Query("UPDATE attachment SET transferStatus = :status WHERE id = :id")
    suspend fun updateTransferStatus(id: String, status: TransferStatus)
}