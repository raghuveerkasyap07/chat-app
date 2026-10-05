package com.demo.chat

import android.app.Application
import com.demo.chat.data.remote.WebSocketClientManager

class DemoChatApplication : Application() {

    lateinit var webSocketManager: WebSocketClientManager
        private set

    override fun onCreate() {
        super.onCreate()
        webSocketManager = WebSocketClientManager()
    }
}
