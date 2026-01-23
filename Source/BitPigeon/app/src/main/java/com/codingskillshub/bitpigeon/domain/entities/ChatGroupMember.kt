package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_group_member",
    indices = [Index("chatGroupId")],
    foreignKeys = [
        ForeignKey(
            entity = ChatGroupDb::class,
            parentColumns = ["id"],
            childColumns = ["chatGroupId"],
            onDelete = ForeignKey.CASCADE // If group is deleted, members are removed
        )
    ]
)
data class ChatGroupMember(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val chatGroupId: String,
    val userId: String
)
