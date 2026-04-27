package com.example.ecomerse.model

enum class UserRole {
    CUSTOMER,
    DISTRIBUTOR,
    SALES_AGENT,
    STOCK_SUPERVISOR,
    COMPANY_MANAGER
}

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val distributorId: String? = null,
    val email: String = "",
    val position: String = "Staff",
    val status: EmployeeStatus = EmployeeStatus.ACTIVE
)

enum class EmployeeStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

data class LeaveRequest(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val startDate: String,
    val endDate: String,
    val reason: String,
    val status: LeaveStatus = LeaveStatus.PENDING
)

enum class LeaveStatus {
    PENDING, APPROVED, REJECTED
}

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val unitPrice: Double
)

data class InventoryItem(
    val productId: String,
    val distributorId: String,
    val quantity: Int
)

data class Sale(
    val id: String,
    val productId: String,
    val distributorId: String,
    val agentId: String,
    val quantity: Int,
    val timestamp: Long
)

data class ActivityLog(
    val id: String,
    val userId: String,
    val action: String,
    val timestamp: Long
)
