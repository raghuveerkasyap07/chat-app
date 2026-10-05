package com.demo.chat.ui.threads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.chat.data.model.ChatThread
import com.demo.chat.data.model.ThreadListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThreadListViewModel : ViewModel() {

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _uiState = MutableStateFlow<ThreadListUiState>(ThreadListUiState.Loading)
    val uiState: StateFlow<ThreadListUiState> = _uiState.asStateFlow()

    init {
        loadInitialThreads()
    }

    private fun loadInitialThreads() {
        viewModelScope.launch {
            val initialList = listOf(
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
            _threads.value = initialList
            _uiState.value = ThreadListUiState.Success(initialList)
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
