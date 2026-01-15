package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_member")
data class ChatGroupMember(
    @PrimaryKey val id: String,
    val chatGroupId: String,
    val userId: String
)
