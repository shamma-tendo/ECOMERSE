package com.example.ecomerse

import com.example.ecomerse.data.InMemoryChatRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSystemTest {

    @Test
    fun testChatFlow() {
        val repository = InMemoryChatRepository()
        val customerId = "cust_001"
        val distributorId = "dist_admin_1"

        // 1. Verify seeded threads exist
        val customerThreads = repository.threadsForUser(customerId).value
        assertTrue("Customer should have at least one thread", customerThreads.isNotEmpty())
        
        // Find the thread between customer and distributor
        val thread = customerThreads.find { it.participantIds.contains(distributorId) }
        assertTrue("Thread between customer and distributor should exist", thread != null)
        val threadId = thread!!.id

        // 2. Send a message from distributor to customer
        val initialUnread = thread.unreadCountByUser[customerId] ?: 0
        val messageText = "Hello from distributor!"
        repository.sendMessage(threadId, distributorId, "Distributor", messageText)

        // 3. Verify unread count increased for customer
        val updatedThreads = repository.threadsForUser(customerId).value
        val updatedThread = updatedThreads.find { it.id == threadId }!!
        assertEquals("Unread count should increase", initialUnread + 1, updatedThread.unreadCountByUser[customerId])
        assertEquals("Last message should match", messageText, updatedThread.lastMessage)

        // 4. Mark as read and verify unread count is 0
        repository.markThreadRead(threadId, customerId)
        val finalThreads = repository.threadsForUser(customerId).value
        val finalThread = finalThreads.find { it.id == threadId }!!
        assertEquals("Unread count should be 0 after reading", 0, finalThread.unreadCountByUser[customerId])
    }
}
