package com.example.ecomerse.data

import com.example.ecomerse.model.ChatMessage
import com.example.ecomerse.model.ChatThread
import com.example.ecomerse.model.User
import com.example.ecomerse.model.UserRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

class FirestoreChatRepository : ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val userThreads = mutableMapOf<String, MutableStateFlow<List<ChatThread>>>()
    private val threadMessages = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    override fun threadsForUser(userId: String): StateFlow<List<ChatThread>> {
        val flow = userThreads.getOrPut(userId) { MutableStateFlow(emptyList()) }
        
        db.collection("chatThreads")
            .whereArrayContains("participantIds", userId)
            // Temporarily removing orderBy until index is created in console
            // .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val threads = snapshot?.toObjects(ChatThread::class.java) ?: emptyList()
                flow.value = threads.sortedByDescending { it.lastTimestamp }
            }
            
        return flow
    }

    override fun messages(threadId: String): StateFlow<List<ChatMessage>> {
        val flow = threadMessages.getOrPut(threadId) { MutableStateFlow(emptyList()) }
        
        db.collection("chatThreads").document(threadId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                flow.value = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
            }
            
        return flow
    }

    override fun sendMessage(threadId: String, senderId: String, senderName: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        db.collection("chatThreads").document(threadId).get().addOnSuccessListener { snapshot ->
            val thread = snapshot.toObject(ChatThread::class.java) ?: return@addOnSuccessListener
            val recipients = thread.participantIds.filterNot { it == senderId }
            
            val message = ChatMessage(
                id = messageId,
                threadId = threadId,
                senderId = senderId,
                senderName = senderName,
                text = trimmed,
                timestamp = timestamp,
                deliveredTo = recipients,
                readBy = listOf(senderId)
            )

            // Add message
            db.collection("chatThreads").document(threadId).collection("messages")
                .document(messageId).set(message)

            // Update thread last message and increment unread for others
            val updates = mutableMapOf<String, Any>(
                "lastMessage" to trimmed,
                "lastTimestamp" to timestamp
            )
            
            recipients.forEach { recipientId ->
                updates["unreadCountByUser.$recipientId"] = FieldValue.increment(1)
            }
            updates["unreadCountByUser.$senderId"] = 0

            db.collection("chatThreads").document(threadId).update(updates)
        }
    }

    override fun createOrGetDirectThread(userA: String, userB: String): String {
        // This simplified version assumes we find existing thread or create a new one.
        // In a real live environment, you'd query first.
        val threadId = if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
        
        db.collection("chatThreads").document(threadId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val userAObj = SessionManager.getAllUsers().find { it.id == userA }
                val userBObj = SessionManager.getAllUsers().find { it.id == userB }
                
                val thread = ChatThread(
                    id = threadId,
                    participantIds = listOf(userA, userB),
                    title = if (userAObj != null && userBObj != null) "${userAObj.name} & ${userBObj.name}" else "Chat",
                    lastMessage = "",
                    lastTimestamp = System.currentTimeMillis(),
                    unreadCountByUser = mapOf(userA to 0, userB to 0)
                )
                db.collection("chatThreads").document(threadId).set(thread)
            }
        }
        
        return threadId
    }

    override fun markThreadRead(threadId: String, userId: String) {
        db.collection("chatThreads").document(threadId).update("unreadCountByUser.$userId", 0)
        
        // Mark all messages as read by this user (optional refinement)
        db.collection("chatThreads").document(threadId).collection("messages")
            .whereArrayContains("deliveredTo", userId)
            .get().addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val readBy = doc.get("readBy") as? List<String> ?: emptyList()
                    if (userId !in readBy) {
                        doc.reference.update("readBy", FieldValue.arrayUnion(userId))
                    }
                }
            }
    }
}

object ChatRepositoryProvider {
    val repository: ChatRepository = FirestoreChatRepository()
}
