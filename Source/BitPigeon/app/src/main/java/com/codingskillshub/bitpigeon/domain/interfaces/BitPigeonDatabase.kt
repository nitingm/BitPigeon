package com.codingskillshub.bitpigeon.domain.interfaces

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codingskillshub.bitpigeon.common.data.DataTypeConvertor
import com.codingskillshub.bitpigeon.domain.entities.Attachment
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupDb
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupMember

import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.AttachmentDao

import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatGroupDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao

@Database(
    entities = [ChatMessage::class, ChatGroupDb::class, ChatGroupMember::class, User::class, Attachment::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DataTypeConvertor::class)
abstract class BitPigeonDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun chatGroupDao(): ChatGroupDao
    abstract fun userDao(): UserDao
    abstract fun attachmentDao(): AttachmentDao
}