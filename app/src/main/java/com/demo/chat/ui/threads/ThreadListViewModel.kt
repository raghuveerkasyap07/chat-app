package com.demo.chat.ui.threads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.demo.chat.data.model.ChatThread
import com.demo.chat.data.model.ThreadListUiState
import com.demo.chat.data.remote.ChatRepository
import com.demo.chat.data.remote.UserRepository
import com.demo.chat.data.remote.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThreadListViewModel(
    private val chatRepository: ChatRepository? = null,
    private val userRepository: UserRepository? = null
) : ViewModel() {

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _uiState = MutableStateFlow<ThreadListUiState>(ThreadListUiState.Loading)
    val uiState: StateFlow<ThreadListUiState> = _uiState.asStateFlow()

    init {
        loadThreads()
    }

    fun loadThreads() {
        viewModelScope.launch {
            _uiState.value = ThreadListUiState.Loading

            val defaultList = listOf(
                ChatThread(
                    threadId = "thread_echo_bot",
                    peerName = "Echo Bot",
                    lastMessageText = "Welcome to Demo Chat! Tap here to start chatting.",
                    lastMessageTimestamp = System.currentTimeMillis() - 60000,
                    unreadCount = 1
                ),
                ChatThread(
                    threadId = "thread_support",
                    peerName = "Support Assistant",
                    lastMessageText = "How can we help you with your WebSocket integration today?",
                    lastMessageTimestamp = System.currentTimeMillis() - 3600000,
                    unreadCount = 0
                ),
                ChatThread(
                    threadId = "thread_dev_team",
                    peerName = "Dev Team Chat",
                    lastMessageText = "Sprint task integration completed successfully.",
                    lastMessageTimestamp = System.currentTimeMillis() - 86400000,
                    unreadCount = 2
                )
            )

            if (chatRepository != null) {
                val result = chatRepository.getChats()
                val apiChats = result.getOrNull()
                if (!apiChats.isNullOrEmpty()) {
                    val mappedList = apiChats.map { chat ->
                        ChatThread(
                            threadId = chat.id,
                            peerName = chat.recipient.name,
                            peerAvatarUrl = chat.recipient.avatarUrl,
                            lastMessageText = chat.lastMessage ?: "Tap to start conversation",
                            lastMessageTimestamp = chat.updatedAt,
                            unreadCount = chat.unreadCount
                        )
                    }
                    _threads.value = mappedList
                    _uiState.value = ThreadListUiState.Success(mappedList)
                    return@launch
                }
            }

            _threads.value = defaultList
            _uiState.value = ThreadListUiState.Success(defaultList)
        }
    }

    fun startChatWithContact(user: UserDto, onComplete: (chatId: String, partnerName: String) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository?.createOrGetChat(user.id)
            val chatId = result?.getOrNull()?.id ?: "thread_${user.id}"

            val existing = _threads.value.find { it.threadId == chatId }
            if (existing == null) {
                val newThread = ChatThread(
                    threadId = chatId,
                    peerName = user.name,
                    peerAvatarUrl = user.avatarUrl,
                    lastMessageText = "Started new chat",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = 0
                )
                val updated = listOf(newThread) + _threads.value
                _threads.value = updated
                _uiState.value = ThreadListUiState.Success(updated)
            }

            onComplete(chatId, user.name)
        }
    }

    fun updateThreadLastMessage(threadId: String, messageText: String) {
        val current = _threads.value.map { thread ->
            if (thread.threadId == threadId) {
                thread.copy(
                    lastMessageText = messageText,
                    lastMessageTimestamp = System.currentTimeMillis()
                )
            } else {
                thread
            }
        }
        _threads.value = current
        _uiState.value = ThreadListUiState.Success(current)
    }
}

class ThreadListViewModelFactory(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThreadListViewModel::class.java)) {
            return ThreadListViewModel(chatRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
