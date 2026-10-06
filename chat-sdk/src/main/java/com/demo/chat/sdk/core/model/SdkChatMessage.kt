package com.demo.chat.sdk.core.model

import java.util.UUID

data class SdkChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: SdkMessageType,
    val status: SdkMessageStatus = SdkMessageStatus.SENT,
    val sender: String = when (type) {
        SdkMessageType.SENT -> "You"
        SdkMessageType.RECEIVED -> "Echo Bot"
        SdkMessageType.SYSTEM -> "System"
    }
)
