package com.demo.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.demo.chat.data.remote.WebSocketClientManager

class ChatViewModelFactory(
    private val webSocketManager: WebSocketClientManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(webSocketManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
