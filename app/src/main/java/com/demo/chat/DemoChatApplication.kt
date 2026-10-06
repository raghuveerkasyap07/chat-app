package com.demo.chat

import android.app.Application
import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.remote.AuthRepository
import com.demo.chat.data.remote.ChatRepository
import com.demo.chat.data.remote.MediaRepository
import com.demo.chat.data.remote.UserRepository
import com.demo.chat.data.remote.WebSocketClientManager

class DemoChatApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var webSocketManager: WebSocketClientManager
        private set

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)
        authRepository = AuthRepository(sessionManager = sessionManager)
        userRepository = UserRepository(sessionManager = sessionManager)
        chatRepository = ChatRepository(sessionManager = sessionManager)
        mediaRepository = MediaRepository(sessionManager = sessionManager)

        webSocketManager = WebSocketClientManager(sessionManager = sessionManager)
    }
}
