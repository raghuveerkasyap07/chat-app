package com.demo.chat.data.remote

import android.util.Log
import com.demo.chat.data.model.ConnectionState
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

class WebSocketClientManager {

    companion object {
        private const val TAG = "WebSocketClientManager"
        const val DEFAULT_ECHO_URL = "wss://ws.postman-echo.com/raw"
        const val BACKUP_ECHO_URL = "wss://echo.websocket.org"
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String = DEFAULT_ECHO_URL

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(url: String = currentUrl) {
        currentUrl = url
        if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) {
            disconnect()
        }

        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "Connecting to WebSocket URL: $currentUrl")

        val request = try {
            Request.Builder()
                .url(currentUrl)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL format: $currentUrl", e)
            _connectionState.value = ConnectionState.Error("Invalid URL: ${e.message}")
            return
        }

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    fun sendMessage(text: String): Boolean {
        val socket = webSocket
        if (socket != null && _connectionState.value is ConnectionState.Connected) {
            val sent = socket.send(text)
            Log.d(TAG, "Sending message: $text (success=$sent)")
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
        disconnect()
        connect(currentUrl)
    }

    fun getCurrentUrl(): String = currentUrl

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket Opened successfully!")
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket Received text message: $text")
                scope.launch {
                    _incomingMessages.emit(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val utf8Text = bytes.utf8()
                Log.d(TAG, "WebSocket Received byte message: $utf8Text")
                scope.launch {
                    _incomingMessages.emit(utf8Text)
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
                Log.e(TAG, "WebSocket Failure: ${t.message}", t)
                val errorMessage = t.message ?: "Connection failed"
                _connectionState.value = ConnectionState.Error(errorMessage)
            }
        }
    }
}
