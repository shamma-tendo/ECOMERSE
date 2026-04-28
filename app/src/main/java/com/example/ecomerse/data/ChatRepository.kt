package com.example.ecomerse.data

import com.example.ecomerse.model.ChatMessage
import com.example.ecomerse.model.ChatThread
import com.example.ecomerse.model.User
import com.example.ecomerse.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

interface ChatRepository {
    fun threadsForUser(userId: String): StateFlow<List<ChatThread>>
    fun messages(threadId: String): StateFlow<List<ChatMessage>>
    fun sendMessage(threadId: String, senderId: String, senderName: String, text: String)
    fun createOrGetDirectThread(userA: String, userB: String): String
    fun markThreadRead(threadId: String, userId: String)
}

class InMemoryChatRepository : ChatRepository {
    private val usersById = mutableMapOf(
        "cust_001" to User("cust_001", "Customer Demo", UserRole.CUSTOMER),
        "dist_admin_1" to User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1"),
        "mgr_carol" to User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER)
    )

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    private val threadMessages = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    private val userThreadFlows = mutableMapOf<String, MutableStateFlow<List<ChatThread>>>()

    init {
        seedStarterThreads()
    }

    override fun threadsForUser(userId: String): StateFlow<List<ChatThread>> {
        val flow = userThreadFlows.getOrPut(userId) { MutableStateFlow(emptyList()) }
        flow.value = currentThreadsForUser(userId)
        return flow
    }

    override fun messages(threadId: String): StateFlow<List<ChatMessage>> {
        return threadMessages.getOrPut(threadId) { MutableStateFlow(emptyList()) }
    }

    override fun sendMessage(threadId: String, senderId: String, senderName: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val thread = _threads.value.find { it.id == threadId } ?: return
        if (senderId !in thread.participantIds) return

        if (!thread.participantIds.all { participantId ->
                participantId == senderId || canUsersChat(senderId, participantId)
            }) {
            throw IllegalAccessException("Sender is not allowed to message all participants in this thread")
        }

        val recipients = thread.participantIds.filterNot { it == senderId }
        val timestamp = System.currentTimeMillis()
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            senderId = senderId,
            senderName = senderName,
            text = trimmed,
            timestamp = timestamp,
            deliveredTo = recipients,
            readBy = listOf(senderId)
        )

        val messageFlow = threadMessages.getOrPut(threadId) { MutableStateFlow(emptyList()) }
        messageFlow.value = messageFlow.value + message

        _threads.update { list ->
            list.map { existingThread ->
                if (existingThread.id != threadId) return@map existingThread

                val unreadCountByUser = existingThread.participantIds.associateWith { participantId ->
                    when (participantId) {
                        senderId -> 0
                        else -> (existingThread.unreadCountByUser[participantId] ?: 0) + 1
                    }
                }

                existingThread.copy(
                    lastMessage = trimmed,
                    lastTimestamp = timestamp,
                    unreadCountByUser = unreadCountByUser
                )
            }
        }

        refreshAllUserThreadFlows()
    }

    override fun createOrGetDirectThread(userA: String, userB: String): String {
        val firstUser = usersById[userA] ?: throw IllegalArgumentException("Unknown user: $userA")
        val secondUser = usersById[userB] ?: throw IllegalArgumentException("Unknown user: $userB")

        if (!canUsersChat(firstUser.id, secondUser.id)) {
            throw IllegalAccessException("Users are not allowed to chat directly")
        }

        val existing = _threads.value.firstOrNull { thread ->
            thread.participantIds.size == 2 &&
                thread.participantIds.contains(firstUser.id) &&
                thread.participantIds.contains(secondUser.id)
        }

        if (existing != null) return existing.id

        val now = System.currentTimeMillis()
        val threadId = UUID.randomUUID().toString()
        val thread = ChatThread(
            id = threadId,
            participantIds = listOf(firstUser.id, secondUser.id),
            title = buildThreadTitle(firstUser, secondUser),
            lastMessage = "",
            lastTimestamp = now,
            unreadCountByUser = mapOf(firstUser.id to 0, secondUser.id to 0)
        )

        _threads.update { it + thread }
        threadMessages.getOrPut(threadId) { MutableStateFlow(emptyList()) }
        refreshAllUserThreadFlows()

        return threadId
    }

    override fun markThreadRead(threadId: String, userId: String) {
        val thread = _threads.value.find { it.id == threadId } ?: return
        if (userId !in thread.participantIds) return

        _threads.update { list ->
            list.map { existingThread ->
                if (existingThread.id != threadId) return@map existingThread

                existingThread.copy(
                    unreadCountByUser = existingThread.unreadCountByUser + (userId to 0)
                )
            }
        }

        val messageFlow = threadMessages[threadId] ?: MutableStateFlow(emptyList())
        threadMessages[threadId] = messageFlow
        messageFlow.value = messageFlow.value.map { message ->
            if (userId in message.readBy) message else message.copy(readBy = message.readBy + userId)
        }

        refreshAllUserThreadFlows()
    }

    private fun canUsersChat(userA: String, userB: String): Boolean {
        val first = usersById[userA] ?: return false
        val second = usersById[userB] ?: return false

        if (first.role == UserRole.COMPANY_MANAGER || second.role == UserRole.COMPANY_MANAGER) return true

        val sameDistributor = !first.distributorId.isNullOrBlank() && first.distributorId == second.distributorId
        return when {
            // Customer can chat with distributor (e.g., request follow-up)
            first.role == UserRole.CUSTOMER && second.role == UserRole.DISTRIBUTOR -> true
            first.role == UserRole.DISTRIBUTOR && second.role == UserRole.CUSTOMER -> true
            // Allow distributor peers in same network to coordinate
            first.role == UserRole.DISTRIBUTOR && second.role == UserRole.DISTRIBUTOR -> true
            // Keep same-distributor guard for non-manager internal chats
            sameDistributor -> true
            else -> false
        }
    }

    private fun buildThreadTitle(userA: User, userB: User): String {
        return if (userA.role == UserRole.COMPANY_MANAGER || userB.role == UserRole.COMPANY_MANAGER) {
            listOf(userA.name, userB.name).joinToString(" / ")
        } else {
            "${userA.name} & ${userB.name}"
        }
    }

    private fun currentThreadsForUser(userId: String): List<ChatThread> {
        return _threads.value
            .filter { userId in it.participantIds }
            .sortedByDescending { it.lastTimestamp }
    }

    private fun refreshAllUserThreadFlows() {
        userThreadFlows.forEach { (userId, flow) ->
            flow.value = currentThreadsForUser(userId)
        }
    }

    private fun seedStarterThreads() {
        val seedPairs = listOf(
            "cust_001" to "dist_admin_1",
            "mgr_carol" to "dist_admin_1",
            "mgr_carol" to "cust_001"
        )

        seedPairs.forEach { (userA, userB) ->
            val threadId = createOrGetDirectThread(userA, userB)
            sendMessage(
                threadId = threadId,
                senderId = userA,
                senderName = usersById[userA]?.name ?: userA,
                text = "Welcome to the in-app team chat"
            )
            markThreadRead(threadId, userA)
            markThreadRead(threadId, userB)
        }
    }
}

object ChatRepositoryProvider {
    val repository: ChatRepository = InMemoryChatRepository()
}

