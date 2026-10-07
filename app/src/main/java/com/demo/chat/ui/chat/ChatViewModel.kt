package com.demo.chat.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.ConnectionState
import com.demo.chat.data.model.MessageStatus
import com.demo.chat.data.model.MessageType
import com.demo.chat.data.remote.ChatRepository
import com.demo.chat.data.remote.MediaRepository
import com.demo.chat.data.remote.WebSocketClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ChatViewModel(
    private val webSocketManager: WebSocketClientManager,
    private val chatRepository: ChatRepository? = null,
    private val mediaRepository: MediaRepository? = null,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private var currentChatId: String = "chat_default"
    private var currentPartnerName: String = "Echo Bot"

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState

    init {
        observeConnectionState()
        observeIncomingMessages()
        connect()
    }

    fun initChat(chatId: String, partnerName: String) {
        currentChatId = chatId
        currentPartnerName = partnerName

        webSocketManager.joinChatRoom(chatId)

        // Load chat history if chatRepository is available
        if (chatRepository != null) {
            viewModelScope.launch {
                val result = chatRepository.getChatMessages(chatId)
                val messageDtos = result.getOrNull()
                if (!messageDtos.isNullOrEmpty()) {
                    val currentUserId = sessionManager?.getUserId() ?: ""
                    val historyMessages = messageDtos.map { dto ->
                        val isSent = dto.senderId == currentUserId
                        val isImage = !dto.mediaUrl.isNullOrBlank()
                        val type = when {
                            isImage && isSent -> MessageType.SENT_IMAGE
                            isImage && !isSent -> MessageType.RECEIVED_IMAGE
                            isSent -> MessageType.SENT_TEXT
                            else -> MessageType.RECEIVED_TEXT
                        }
                        ChatMessage(
                            id = dto.id,
                            text = if (isImage && dto.message.isBlank()) "[Photo]" else dto.message,
                            imageUrl = dto.mediaUrl,
                            timestamp = dto.timestamp,
                            type = type,
                            status = MessageStatus.SENT,
                            sender = if (isSent) "You" else partnerName
                        )
                    }
                    _messages.value = historyMessages
                    return@launch
                }

                // If no history on server and it's Echo Bot, provide starter message
                if (_messages.value.isEmpty()) {
                    loadInitialEchoMessages()
                }
            }
        } else if (_messages.value.isEmpty()) {
            loadInitialEchoMessages()
        }
    }

    private fun loadInitialEchoMessages() {
        val welcomeMsg = ChatMessage(
            text = "Welcome to WhatsApp Clone! Type a message or attach a photo to test real-time 2-way chat.",
            type = MessageType.RECEIVED_TEXT,
            sender = currentPartnerName
        )
        _messages.value = listOf(welcomeMsg)
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
            type = MessageType.SENT_TEXT,
            status = MessageStatus.SENDING
        )
        addMessage(sentMessage)

        // 1. Broadcast over live WebSocket
        val isWsSent = webSocketManager.sendMessage(trimmed, currentChatId)

        // 2. Also send via REST API if available
        if (chatRepository != null) {
            viewModelScope.launch {
                chatRepository.sendMessageRest(currentChatId, trimmed)
            }
        }

        val updatedStatus = if (isWsSent) MessageStatus.SENT else MessageStatus.SENT
        updateMessageStatus(sentMessage.id, updatedStatus)
    }

    fun sendPhoto(uri: Uri, context: Context) {
        val messageId = UUID.randomUUID().toString()

        // 1. Optimistic photo bubble in chat timeline
        val optimisticMessage = ChatMessage(
            id = messageId,
            text = "[Photo]",
            imageUrl = uri.toString(),
            type = MessageType.SENT_IMAGE,
            status = MessageStatus.SENDING
        )
        addMessage(optimisticMessage)

        viewModelScope.launch(Dispatchers.IO) {
            var uploadedMediaUrl: String? = null

            // 2. Copy URI content to temporary cache file for multipart upload
            val tempFile = copyUriToTempFile(context, uri)

            // 3. Upload photo attachment via MediaRepository
            if (tempFile != null && mediaRepository != null) {
                try {
                    val uploadResult = mediaRepository.uploadPhotoAttachment(currentChatId, tempFile)
                    if (uploadResult.isSuccess) {
                        uploadedMediaUrl = uploadResult.getOrNull()
                        Log.i(TAG, "Photo uploaded successfully: $uploadedMediaUrl")
                    } else {
                        Log.w(TAG, "Multipart upload failed: ${uploadResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during media upload", e)
                } finally {
                    tempFile.delete()
                }
            }

            // 4. Broadcast image URL or URI over WebSocket
            val broadcastUrl = uploadedMediaUrl ?: uri.toString()
            webSocketManager.sendImage(broadcastUrl, currentChatId)

            // 5. Update optimistic bubble to SENT status
            withContext(Dispatchers.Main) {
                updateMessageImageAndStatus(
                    id = messageId,
                    newImageUrl = broadcastUrl,
                    newStatus = MessageStatus.SENT
                )
            }
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_photo_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file", e)
            null
        }
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
                        addSystemMessage("Connected to WebSocket Server (${webSocketManager.getCurrentUrl()})")
                        if (currentChatId.isNotBlank()) {
                            webSocketManager.joinChatRoom(currentChatId)
                        }
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
            webSocketManager.incomingMessages.collect { incomingMessage ->
                // Avoid displaying duplicate optimistic sent messages
                val current = _messages.value
                val isDuplicate = current.any {
                    it.text == incomingMessage.text &&
                            it.imageUrl == incomingMessage.imageUrl &&
                            it.type == incomingMessage.type &&
                            Math.abs(it.timestamp - incomingMessage.timestamp) < 3000
                }
                if (!isDuplicate) {
                    addMessage(incomingMessage)
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

    private fun updateMessageImageAndStatus(id: String, newImageUrl: String, newStatus: MessageStatus) {
        val currentList = _messages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                imageUrl = newImageUrl,
                status = newStatus
            )
            _messages.value = currentList
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
