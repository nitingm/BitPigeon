package com.codingskillshub.bitpigeon.common

import androidx.compose.ui.semantics.text
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import javax.inject.Inject


class HashService @Inject constructor() {
    /**
     * Generates a deterministic ID for a 1-on-1 chat group.
     * Sorting peer IDs ensures both users generate the exact same Group ID locally.
     */
    fun generateDirectGroupId(myId: String, peerId: String): String {
        val sortedParties = listOf(myId, peerId).sorted()
        return hashString("DIRECT_${sortedParties[0]}_${sortedParties[1]}")
    }

    fun generatePersonalChatId(myId: String): String {
        return hashString("PERSONAL_${myId}")
    }

    /**
     * Generates a unique, deterministic ID for a message.
     * This ensures the same message has the same ID on all peer devices.
     */
    fun generateMessageId(messageText: String, senderId: String, timeStamp: String): String {
        val rawString = "${senderId}${timeStamp}${messageText}"
        return hashString(rawString)
    }

    fun generateUniqueId(input: String): String {
        return hashString(input)
    }

    private fun hashString(input: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16) // 16-32 chars is usually enough for local P2P collisions
    }
}