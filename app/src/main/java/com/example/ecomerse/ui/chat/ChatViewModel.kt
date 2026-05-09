package com.example.ecomerse.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ecomerse.data.ChatRepository
import com.example.ecomerse.data.ChatRepositoryProvider
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.ChatMessage
import com.example.ecomerse.model.ChatThread
import com.example.ecomerse.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager = SessionManager
) : ViewModel() {

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentThreadId = MutableStateFlow<String?>(null)
    val currentThreadId: StateFlow<String?> = _currentThreadId.asStateFlow()

    private var threadsJob: Job? = null
    private var messagesJob: Job? = null

    init {
        loadThreads()
    }

    fun loadThreads() {
        val userId = sessionManager.currentUser.value?.id ?: run {
            _threads.value = emptyList()
            return
        }

        if (threadsJob?.isActive == true) return

        threadsJob = viewModelScope.launch {
            chatRepository.threadsForUser(userId).collectLatest { userThreads ->
                _threads.value = userThreads
            }
        }
    }

    fun loadMessages(threadId: String) {
        _currentThreadId.value = threadId

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.messages(threadId).collectLatest { threadMessages ->
                _messages.value = threadMessages.sortedBy { it.timestamp }
            }
        }

        markRead(threadId)
    }

    fun sendMessage(text: String) {
        val currentUser = sessionManager.currentUser.value ?: return
        val threadId = _currentThreadId.value ?: return

        chatRepository.sendMessage(
            threadId = threadId,
            senderId = currentUser.id,
            senderName = currentUser.name,
            text = text
        )
        markRead(threadId)
    }

    fun markRead(threadId: String) {
        val userId = sessionManager.currentUser.value?.id ?: return
        chatRepository.markThreadRead(threadId, userId)
    }

    fun currentUser(): User? = sessionManager.currentUser.value

    companion object {
        fun factory(
            chatRepository: ChatRepository = ChatRepositoryProvider.repository,
            sessionManager: SessionManager = SessionManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(chatRepository, sessionManager) as T
                }
            }
        }
    }
}

