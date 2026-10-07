package com.demo.chat.data.remote

import android.util.Log
import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.ConnectionState
import com.demo.chat.data.model.MessageStatus
import com.demo.chat.data.model.MessageType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebSocketClientManager(
    private val sessionManager: SessionManager? = null,
    private val gson: Gson = Gson()
) {

    companion object {
        private const val TAG = "WebSocketClientManager"
        const val DEFAULT_ECHO_URL = "wss://ws.postman-echo.com/raw"
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String = sessionManager?.getWsUrl() ?: DEFAULT_ECHO_URL
    private var hasTriedFallback = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(url: String = currentUrl) {
        if (url != currentUrl) {
            hasTriedFallback = false
        }
        currentUrl = url

        if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) {
            disconnect()
        }

        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "Connecting to WebSocket URL: $currentUrl")

        val token = sessionManager?.getAuthToken()
        val requestBuilder = Request.Builder()
            .url(if (!token.isNullOrBlank()) "$currentUrl?token=$token" else currentUrl)

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = try {
            requestBuilder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL format: $currentUrl", e)
            _connectionState.value = ConnectionState.Error("Invalid URL: ${e.message}")
            return
        }

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    fun joinChatRoom(chatId: String) {
        val payload = mapOf("action" to "join_chat", "chatId" to chatId)
        webSocket?.send(gson.toJson(payload))
    }

    fun sendMessage(text: String, chatId: String? = null): Boolean {
        val socket = webSocket
        if (socket != null && _connectionState.value is ConnectionState.Connected) {
            val payload = if (chatId != null) {
                gson.toJson(mapOf("chatId" to chatId, "message" to text))
            } else {
                text
            }
            val sent = socket.send(payload)
            Log.d(TAG, "Sending message: $payload (success=$sent)")
            return sent
        } else {
            Log.w(TAG, "Cannot send message: WebSocket not connected")
            return false
        }
    }

    fun sendPing(): Boolean {
        val socket = webSocket
        if (socket != null && _connectionState.value is ConnectionState.Connected) {
            val pingSent = socket.send("PING_TEST")
            Log.d(TAG, "Ping sent: $pingSent")
            return pingSent
        }
        return false
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        webSocket?.close(NORMAL_CLOSURE_STATUS, "User requested disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun reconnect() {
        hasTriedFallback = false
        disconnect()
        connect(sessionManager?.getWsUrl() ?: DEFAULT_ECHO_URL)
    }

    fun getCurrentUrl(): String = currentUrl

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket Opened successfully on $currentUrl!")
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket Received text message: $text")
                scope.launch {
                    val msg = parseIncomingText(text)
                    _incomingMessages.emit(msg)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val utf8Text = bytes.utf8()
                Log.d(TAG, "WebSocket Received byte message: $utf8Text")
                scope.launch {
                    val msg = parseIncomingText(utf8Text)
                    _incomingMessages.emit(msg)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket Closed: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure on $currentUrl: ${t.message}", t)
                if (!hasTriedFallback && currentUrl != DEFAULT_ECHO_URL) {
                    hasTriedFallback = true
                    Log.i(TAG, "Switching to fallback public echo server: $DEFAULT_ECHO_URL")
                    connect(DEFAULT_ECHO_URL)
                } else {
                    val errorMessage = t.message ?: "Connection failed"
                    _connectionState.value = ConnectionState.Error(errorMessage)
                }
            }
        }
    }

    private fun parseIncomingText(rawText: String): ChatMessage {
        if (rawText == "PING_TEST") {
            return ChatMessage(
                text = "Pong response received from Server! 🏓",
                type = MessageType.SYSTEM
            )
        }

        return try {
            val jsonObject = gson.fromJson(rawText, Map::class.java) as? Map<*, *>
            val senderId = jsonObject?.get("senderId")?.toString() ?: ""
            val senderName = jsonObject?.get("senderName")?.toString()
                ?: jsonObject?.get("sender")?.toString()
                ?: "Partner"
            val text = jsonObject?.get("message")?.toString()
                ?: jsonObject?.get("text")?.toString()
                ?: rawText

            val currentUserId = sessionManager?.getUserId() ?: ""
            val messageType = if (senderId.isNotBlank() && senderId == currentUserId) {
                MessageType.SENT
            } else {
                MessageType.RECEIVED
            }

            ChatMessage(
                text = text,
                type = messageType,
                status = MessageStatus.SENT,
                sender = if (messageType == MessageType.SENT) "You" else senderName
            )
        } catch (e: Exception) {
            ChatMessage(
                text = rawText,
                type = MessageType.RECEIVED,
                status = MessageStatus.SENT,
                sender = "Partner"
            )
        }
    }
}
