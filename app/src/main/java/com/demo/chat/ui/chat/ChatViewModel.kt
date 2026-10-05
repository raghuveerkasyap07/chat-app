package com.demo.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.ConnectionState
import com.demo.chat.data.model.MessageStatus
import com.demo.chat.data.model.MessageType
import com.demo.chat.data.remote.WebSocketClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val webSocketManager: WebSocketClientManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState

    init {
        observeConnectionState()
        observeIncomingMessages()
        // Auto connect on start
        connect()
    }

    fun connect(url: String = webSocketManager.getCurrentUrl()) {
        webSocketManager.connect(url)
    }

    fun disconnect() {
        webSocketManager.disconnect()
    }

    fun reconnect() {
        webSocketManager.reconnect()
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val sentMessage = ChatMessage(
            text = trimmed,
            type = MessageType.SENT,
            status = MessageStatus.SENDING
        )

        addMessage(sentMessage)

        val isSent = webSocketManager.sendMessage(trimmed)
        val updatedStatus = if (isSent) MessageStatus.SENT else MessageStatus.FAILED
        updateMessageStatus(sentMessage.id, updatedStatus)
    }

    fun sendPing() {
        val pingSent = webSocketManager.sendPing()
        if (pingSent) {
            addSystemMessage("Ping sent to server")
        } else {
            addSystemMessage("Failed to send Ping (Not connected)")
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        addSystemMessage("Chat history cleared")
    }

    fun getServerUrl(): String = webSocketManager.getCurrentUrl()

    private fun observeConnectionState() {
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        addSystemMessage("Connected to WebSocket Echo Server (${webSocketManager.getCurrentUrl()})")
                    }
                    is ConnectionState.Connecting -> {
                        addSystemMessage("Connecting to WebSocket server...")
                    }
                    is ConnectionState.Disconnected -> {
                        addSystemMessage("Disconnected from server")
                    }
                    is ConnectionState.Error -> {
                        addSystemMessage("Connection Error: ${state.message}")
                    }
                }
            }
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            webSocketManager.incomingMessages.collect { text ->
                if (text == "PING_TEST") {
                    addSystemMessage("Pong response received from Echo Server! 🏓")
                } else {
                    val receivedMessage = ChatMessage(
                        text = text,
                        type = MessageType.RECEIVED,
                        sender = "Echo Bot"
                    )
                    addMessage(receivedMessage)
                }
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList
    }

    private fun addSystemMessage(text: String) {
        val sysMsg = ChatMessage(
            text = text,
            type = MessageType.SYSTEM,
            sender = "System"
        )
        addMessage(sysMsg)
    }

    private fun updateMessageStatus(id: String, newStatus: MessageStatus) {
        val currentList = _messages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(status = newStatus)
            _messages.value = currentList
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
