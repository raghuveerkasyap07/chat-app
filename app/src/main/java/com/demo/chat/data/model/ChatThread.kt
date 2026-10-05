package com.demo.chat.data.model

data class ChatThread(
    val threadId: String,
    val peerName: String,
    val peerAvatarUrl: String? = null,
    val lastMessageText: String,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)
