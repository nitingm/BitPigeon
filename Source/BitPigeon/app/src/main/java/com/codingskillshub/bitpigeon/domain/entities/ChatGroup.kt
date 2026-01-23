package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import androidx.room.Relation

enum class ChatGroupType {
    PERSONAL,
    DIRECT,
    GROUP
}

@Entity(tableName = "chat_group")
data class ChatGroupDb (
    @PrimaryKey val id: String,
    val name: String,
    val type: ChatGroupType = ChatGroupType.DIRECT
)


data class ChatGroup(
    @Embedded val group: ChatGroupDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "chatGroupId"
    )
    val members: List<ChatGroupMember>,
    @Ignore val lastMessage: String,
    @Ignore val timestamp: String
) {
    constructor(group: ChatGroupDb, members: List<ChatGroupMember>) :
            this(group, members, "", "")
}
