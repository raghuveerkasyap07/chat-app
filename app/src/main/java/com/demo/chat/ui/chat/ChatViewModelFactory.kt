package com.demo.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.remote.ChatRepository
import com.demo.chat.data.remote.MediaRepository
import com.demo.chat.data.remote.WebSocketClientManager

class ChatViewModelFactory(
    private val webSocketManager: WebSocketClientManager,
    private val chatRepository: ChatRepository? = null,
    private val mediaRepository: MediaRepository? = null,
    private val sessionManager: SessionManager? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(webSocketManager, chatRepository, mediaRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
