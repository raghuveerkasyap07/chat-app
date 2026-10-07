package com.demo.chat.sdk

import android.content.Context
import com.demo.chat.sdk.core.ChatClient

class ChatEngine private constructor(
    val context: Context,
    val client: ChatClient
) {
    companion object {
        const val DEFAULT_SERVER_URL = "ws://192.168.0.10:5000"
        const val FALLBACK_ECHO_URL = "wss://ws.postman-echo.com/raw"

        @Volatile
        private var INSTANCE: ChatEngine? = null

        fun initialize(
            context: Context,
            serverUrl: String = DEFAULT_SERVER_URL
        ): ChatEngine {
            return INSTANCE ?: synchronized(this) {
                val client = ChatClient(serverUrl)
                val engine = ChatEngine(context.applicationContext, client)
                INSTANCE = engine
                engine
            }
        }

        fun getInstance(): ChatEngine {
            return INSTANCE ?: throw IllegalStateException(
                "ChatEngine is not initialized! Please call ChatEngine.initialize(context) in your Application class or Activity."
            )
        }

        fun isInitialized(): Boolean = INSTANCE != null
    }
}
