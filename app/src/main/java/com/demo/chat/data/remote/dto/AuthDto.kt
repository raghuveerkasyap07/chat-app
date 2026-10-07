package com.demo.chat.data.remote.dto

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null
)

data class AuthDataDto(
    val token: String,
    val user: UserDto
)

data class AuthResponseDto(
    val success: Boolean,
    val message: String? = null,
    val data: AuthDataDto? = null
)

data class UsersListResponseDto(
    val success: Boolean,
    val data: List<UserDto> = emptyList()
)
