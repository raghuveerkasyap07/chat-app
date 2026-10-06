package com.demo.chatsdk

import android.content.Context
import android.util.Log

object ChatSdk {
    private const val TAG = "ChatSdk"
    private var isInitialized: Boolean = false
    private var serverUrl: String = "wss://ws.postman-echo.com/raw"

    fun initialize(context: Context, url: String = "wss://ws.postman-echo.com/raw") {
        serverUrl = url
        isInitialized = true
        Log.d(TAG, "ChatSdk initialized with server URL: $serverUrl")
    }

    fun getServerUrl(): String = serverUrl
    
    fun isInitialized(): Boolean = isInitialized
}
