package com.example.ecomerse.data

import com.example.ecomerse.model.ChatMessage
import com.example.ecomerse.model.ChatThread
import com.example.ecomerse.model.User
import com.example.ecomerse.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.UUID

interface ChatRepository {
    fun threadsForUser(userId: String): Flow<List<ChatThread>>
    fun messages(threadId: String): Flow<List<ChatMessage>>
    fun sendMessage(threadId: String, senderId: String, senderName: String, text: String)
    fun createOrGetDirectThread(userA: User, userB: User): String
    fun markThreadRead(threadId: String, userId: String)
}

class FirestoreChatRepository : ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    override fun threadsForUser(userId: String): Flow<List<ChatThread>> = callbackFlow {
        val subscription = db.collection("chatThreads")
            .whereArrayContains("participantIds", userId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val threads = snapshot?.toObjects(ChatThread::class.java) ?: emptyList()
                trySend(threads)
            }
        awaitClose { subscription.remove() }
    }

    override fun messages(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = db.collection("chatThreads")
            .document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val msgs = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(msgs)
            }
        awaitClose { subscription.remove() }
    }

    override fun sendMessage(threadId: String, senderId: String, senderName: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        db.runTransaction { transaction ->
            val threadRef = db.collection("chatThreads").document(threadId)
            val thread = transaction.get(threadRef).toObject(ChatThread::class.java) ?: return@runTransaction
            
            if (senderId !in thread.participantIds) return@runTransaction

            val message = ChatMessage(
                id = messageId,
                threadId = threadId,
                senderId = senderId,
                senderName = senderName,
                text = trimmed,
                timestamp = timestamp,
                readBy = listOf(senderId)
            )

            val messageRef = threadRef.collection("messages").document(messageId)
            transaction.set(messageRef, message)

            val unreadCounts = thread.unreadCountByUser.toMutableMap()
            thread.participantIds.forEach { pid ->
                if (pid != senderId) {
                    unreadCounts[pid] = (unreadCounts[pid] ?: 0) + 1
                }
            }

            transaction.update(threadRef, mapOf(
                "lastMessage" to trimmed,
                "lastTimestamp" to timestamp,
                "unreadCountByUser" to unreadCounts
            ))
        }
    }

    override fun createOrGetDirectThread(userA: User, userB: User): String {
        if (!canUsersChat(userA, userB)) {
            throw IllegalAccessException("Communication between these roles is not permitted.")
        }

        // Check for existing direct thread (participantIds is a list, we sort to find consistently)
        val sortedIds = listOf(userA.id, userB.id).sorted()
        
        // This is a simplified check; in a production app you'd query specifically for these two.
        // For now, we'll try to find it or create a new one.
        val threadId = "${sortedIds[0]}_${sortedIds[1]}"
        
        val threadRef = db.collection("chatThreads").document(threadId)
        threadRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val thread = ChatThread(
                    id = threadId,
                    participantIds = sortedIds,
                    title = "${userA.name} & ${userB.name}",
                    lastTimestamp = System.currentTimeMillis(),
                    unreadCountByUser = sortedIds.associateWith { 0 }
                )
                threadRef.set(thread)
            }
        }
        
        return threadId
    }

    override fun markThreadRead(threadId: String, userId: String) {
        val threadRef = db.collection("chatThreads").document(threadId)
        db.runTransaction { transaction ->
            val thread = transaction.get(threadRef).toObject(ChatThread::class.java) ?: return@runTransaction
            val unreadCounts = thread.unreadCountByUser.toMutableMap()
            unreadCounts[userId] = 0
            transaction.update(threadRef, "unreadCountByUser", unreadCounts)
        }
    }

    private fun canUsersChat(userA: User, userB: User): Boolean {
        return when (userA.role) {
            UserRole.CUSTOMER -> userB.role == UserRole.DISTRIBUTOR
            UserRole.DISTRIBUTOR -> userB.role == UserRole.CUSTOMER || userB.role == UserRole.COMPANY_MANAGER || userB.role == UserRole.DISTRIBUTOR
            UserRole.COMPANY_MANAGER -> userB.role == UserRole.DISTRIBUTOR
        }
    }
}

object ChatRepositoryProvider {
    val repository: ChatRepository = FirestoreChatRepository()
}

