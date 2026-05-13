package com.codingskillshub.bitpigeon.common.data

import androidx.room.TypeConverter
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import com.google.gson.Gson

class DataTypeConvertor {
    private val gson = Gson()

    @TypeConverter
    fun fromMessageData(data: MessageData): String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun toMessageData(json: String): MessageData {
        return gson.fromJson(json, MessageData::class.java)
    }

    @TypeConverter
    fun fromChatGroupType(value: ChatGroupType): String {
        return value.name // Stores as "PERSONAL" or "DIRECT" or "GROUP"
    }

    @TypeConverter
    fun toChatGroupType(value: String): ChatGroupType {
        return ChatGroupType.valueOf(value) // Converts string back to Enum
    }

    @TypeConverter
    fun fromTransferStatus(value: TransferStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTransferStatus(value: String): TransferStatus {
        return TransferStatus.valueOf(value)
    }
}