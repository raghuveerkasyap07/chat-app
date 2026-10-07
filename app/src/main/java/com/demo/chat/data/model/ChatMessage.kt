package com.demo.chat.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType,
    val status: MessageStatus = MessageStatus.SENT,
    val sender: String = when (type) {
        MessageType.SENT, MessageType.SENT_TEXT, MessageType.SENT_IMAGE -> "You"
        MessageType.RECEIVED, MessageType.RECEIVED_TEXT, MessageType.RECEIVED_IMAGE -> "Echo Bot"
        MessageType.SYSTEM -> "System"
    }
) {
    val isImage: Boolean
        get() = !imageUrl.isNullOrBlank() ||
                type == MessageType.SENT_IMAGE ||
                type == MessageType.RECEIVED_IMAGE
}
