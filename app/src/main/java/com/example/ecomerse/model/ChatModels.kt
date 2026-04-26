package com.example.ecomerse.model

data class ChatThread(
    val id: String,
    val participantIds: List<String>,
    val title: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCountByUser: Map<String, Int>
)

data class ChatMessage(
    val id: String,
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val deliveredTo: List<String>,
    val readBy: List<String>
)

