package com.example.ecomerse.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ecomerse.model.ChatMessage
import com.example.ecomerse.model.ChatThread
import com.example.ecomerse.model.User
import com.example.ecomerse.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadListScreen(
    viewModel: ChatViewModel,
    onThreadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val threads by viewModel.threads.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser = viewModel.currentUser()
    var showUserPicker by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.id) {
        viewModel.loadThreads()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showUserPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding)) {
            if (threads.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No conversations yet\nTap + to start one",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("Team Chat", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Messaging based on your role permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(threads, key = { it.id }) { thread ->
                        ChatThreadListItem(
                            thread = thread,
                            unreadCount = currentUser?.id?.let { thread.unreadCountByUser[it] ?: 0 } ?: 0,
                            onClick = { onThreadClick(thread.id) }
                        )
                    }
                }
            }
        }
    }

    if (showUserPicker && currentUser != null) {
        UserPickerCallback(
            currentUser = currentUser,
            allUsers = allUsers,
            onUserSelected = { target ->
                viewModel.startDirectChat(target) { threadId ->
                    onThreadClick(threadId)
                }
                showUserPicker = false
            },
            onDismiss = { showUserPicker = false }
        )
    }
}

@Composable
private fun UserPickerCallback(
    currentUser: User,
    allUsers: List<User>,
    onUserSelected: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val allowedUsers = allUsers.filter { target ->
        target.id != currentUser.id && when (currentUser.role) {
            UserRole.CUSTOMER -> target.role == UserRole.DISTRIBUTOR
            UserRole.DISTRIBUTOR -> target.role == UserRole.CUSTOMER || target.role == UserRole.COMPANY_MANAGER || target.role == UserRole.DISTRIBUTOR
            UserRole.COMPANY_MANAGER -> target.role == UserRole.DISTRIBUTOR
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Conversation") },
        text = {
            if (allowedUsers.isEmpty()) {
                Text("No available contacts found for your role.")
            } else {
                LazyColumn {
                    items(allowedUsers) { user ->
                        ListItem(
                            headlineContent = { Text(user.name) },
                            supportingContent = { Text(user.role.name) },
                            modifier = Modifier.clickable { onUserSelected(user) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatThreadListItem(
    thread: ChatThread,
    unreadCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(thread.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge { Text(unreadCount.toString()) }
                    }
                }
            },
            supportingContent = {
                Text(
                    text = if (thread.lastMessage.isBlank()) "No messages yet" else thread.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                Text(
                    text = formatTime(thread.lastTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
fun ChatConversationScreen(
    threadId: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val threads by viewModel.threads.collectAsState()
    val currentUser = viewModel.currentUser()
    val currentThread = threads.find { it.id == threadId }

    LaunchedEffect(threadId) {
        viewModel.loadMessages(threadId)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(currentThread?.title ?: "Conversation", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${messages.size} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val isMine = currentUser?.id == message.senderId
                ChatMessageBubble(
                    message = message,
                    isMine = isMine,
                    allParticipantsCount = currentThread?.participantIds?.size ?: 2
                )
            }
        }

        MessageComposer(
            onSend = { text -> viewModel.sendMessage(text) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    allParticipantsCount: Int
) {
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isMine) {
                        val receipt = when {
                            message.readBy.size >= allParticipantsCount -> "Read"
                            message.deliveredTo.isNotEmpty() -> "Delivered"
                            else -> "Sent"
                        }
                        Text(
                            receipt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageComposer(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message") },
            maxLines = 4
        )

        FilledTonalButton(
            onClick = {
                val cleaned = text.trim()
                if (cleaned.isNotEmpty()) {
                    onSend(cleaned)
                    text = ""
                }
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return if (timestamp <= 0L) {
        ""
    } else {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}



