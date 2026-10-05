package com.demo.chat.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType,
    val status: MessageStatus = MessageStatus.SENT,
    val sender: String = when (type) {
        MessageType.SENT -> "You"
        MessageType.RECEIVED -> "Echo Bot"
        MessageType.SYSTEM -> "System"
    }
)
