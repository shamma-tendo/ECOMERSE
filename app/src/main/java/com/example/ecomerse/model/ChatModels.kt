package com.example.ecomerse.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatThread(
    var id: String = "",
    var participantIds: List<String> = emptyList(),
    var title: String = "",
    var lastMessage: String = "",
    var lastTimestamp: Long = 0,
    var unreadCountByUser: Map<String, Int> = emptyMap()
)

@IgnoreExtraProperties
data class ChatMessage(
    var id: String = "",
    var threadId: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var text: String = "",
    var timestamp: Long = 0,
    var deliveredTo: List<String> = emptyList(),
    var readBy: List<String> = emptyList()
)
