package com.example.ecomerse.model

data class ChatThread(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val title: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val unreadCountByUser: Map<String, Int> = emptyMap()
)

data class ChatMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val deliveredTo: List<String> = emptyList(),
    val readBy: List<String> = emptyList()
)

