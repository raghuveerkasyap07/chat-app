package com.demo.chat.sdk.core

import android.util.Log
import com.demo.chat.sdk.core.model.SdkConnectionState
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class ChatClient internal constructor(
    private var defaultUrl: String
) {

    companion object {
        private const val TAG = "ChatClientSDK"
        private const val NORMAL_CLOSURE_STATUS = 1000
        const val FALLBACK_ECHO_URL = "wss://ws.postman-echo.com/raw"
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String = defaultUrl
    private var hasTriedFallback = false

    private val _connectionState = MutableStateFlow<SdkConnectionState>(SdkConnectionState.Disconnected)
    val connectionState: StateFlow<SdkConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val connectionListeners = CopyOnWriteArrayList<ConnectionListener>()
    private val messageListeners = CopyOnWriteArrayList<MessageListener>()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            _connectionState.collect { state ->
                connectionListeners.forEach { listener ->
                    listener.onConnectionStateChanged(state)
                }
            }
        }
    }

    fun connect(url: String = currentUrl) {
        if (url != currentUrl) {
            hasTriedFallback = false
        }
        currentUrl = url

        if (_connectionState.value is SdkConnectionState.Connected || _connectionState.value is SdkConnectionState.Connecting) {
            disconnect()
        }

        _connectionState.value = SdkConnectionState.Connecting
        Log.d(TAG, "Connecting to WebSocket URL: $currentUrl")

        val request = try {
            Request.Builder()
                .url(currentUrl)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL format: $currentUrl", e)
            _connectionState.value = SdkConnectionState.Error("Invalid URL: ${e.message}")
            return
        }

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    fun sendMessage(text: String): Boolean {
        val socket = webSocket
        if (socket != null && _connectionState.value is SdkConnectionState.Connected) {
            val sent = socket.send(text)
            Log.d(TAG, "Sent text frame (success=$sent): $text")
            return sent
        } else {
            Log.w(TAG, "Cannot send message: WebSocket not connected")
            return false
        }
    }

    fun sendPing(): Boolean {
        val socket = webSocket
        if (socket != null && _connectionState.value is SdkConnectionState.Connected) {
            return socket.send("PING_TEST")
        }
        return false
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        webSocket?.close(NORMAL_CLOSURE_STATUS, "SDK Disconnect")
        webSocket = null
        _connectionState.value = SdkConnectionState.Disconnected
    }

    fun reconnect() {
        hasTriedFallback = false
        disconnect()
        connect(defaultUrl)
    }

    fun getCurrentUrl(): String = currentUrl

    fun addConnectionListener(listener: ConnectionListener) {
        connectionListeners.add(listener)
    }

    fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners.remove(listener)
    }

    fun addMessageListener(listener: MessageListener) {
        messageListeners.add(listener)
    }

    fun removeMessageListener(listener: MessageListener) {
        messageListeners.remove(listener)
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connection opened on $currentUrl!")
                _connectionState.value = SdkConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                scope.launch {
                    _incomingMessages.emit(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val text = bytes.utf8()
                Log.d(TAG, "Message bytes received: $text")
                scope.launch {
                    _incomingMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = SdkConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = SdkConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure on $currentUrl: ${t.message}", t)
                if (!hasTriedFallback && currentUrl != FALLBACK_ECHO_URL) {
                    hasTriedFallback = true
                    Log.i(TAG, "Switching to fallback public echo server: $FALLBACK_ECHO_URL")
                    connect(FALLBACK_ECHO_URL)
                } else {
                    _connectionState.value = SdkConnectionState.Error(t.message ?: "Connection failed")
                }
            }
        }
    }
}
