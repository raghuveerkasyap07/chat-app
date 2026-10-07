package com.demo.chat.data.remote.dto

data class ChatDto(
    val id: String,
    val recipient: UserDto,
    val lastMessage: String? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChatResponseDto(
    val success: Boolean,
    val data: ChatDataDto? = null
)

data class ChatDataDto(
    val chat: ChatDto
)

data class ChatListResponseDto(
    val success: Boolean,
    val data: List<ChatDto> = emptyList()
)
