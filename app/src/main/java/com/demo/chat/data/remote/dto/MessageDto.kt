package com.demo.chat.data.remote.dto

data class MessageDataDto(
    val id: String,
    val chatId: String,
    val senderId: String,
    val message: String,
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MessageListResponseDto(
    val success: Boolean,
    val data: List<MessageDataDto> = emptyList()
)

data class SendMessageResponseDto(
    val success: Boolean,
    val data: MessageDataDto? = null
)
