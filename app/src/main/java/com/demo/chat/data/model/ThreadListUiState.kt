package com.demo.chat.data.model

sealed interface ThreadListUiState {
    data object Loading : ThreadListUiState
    data class Success(val threads: List<ChatThread>) : ThreadListUiState
    data class Empty(val message: String = "No conversations yet") : ThreadListUiState
}
