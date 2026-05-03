package com.example.ecomerse.model

enum class UserRole {
    CUSTOMER,
    DISTRIBUTOR,
    COMPANY_MANAGER
}

data class User(
    val id: String = "",
    val name: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val distributorId: String? = null,
    val email: String = "",
    val position: String = "Staff",
    val status: EmployeeStatus = EmployeeStatus.ACTIVE
)

enum class EmployeeStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

data class LeaveRequest(
    val id: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val reason: String = "",
    val status: LeaveStatus = LeaveStatus.PENDING
)

enum class LeaveStatus {
    PENDING, APPROVED, REJECTED
}

enum class PaymentMethod {
    CASH,
    CREDIT
}

enum class GoodsRequestStatus {
    PENDING,
    APPROVED,
    FULFILLED,
    SETTLED,
    REJECTED
}

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val unitPrice: Double = 0.0,
    val unit: String = "unit"
)

data class InventoryItem(
    val productId: String = "",
    val distributorId: String = "",
    val quantity: Int = 0
)

data class Sale(
    val id: String = "",
    val productId: String = "",
    val distributorId: String = "",
    val handledByUserId: String = "",
    val quantity: Int = 0,
    val timestamp: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val requestId: String? = null
)

data class GoodsRequest(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val distributorId: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: GoodsRequestStatus = GoodsRequestStatus.PENDING,
    val requestedAt: Long = 0L,
    val dueAt: Long? = null,
    val fulfilledAt: Long? = null,
    val settledAt: Long? = null,
    val handledByUserId: String? = null,
    val note: String = ""
)

data class ActivityLog(
    val id: String = "",
    val userId: String = "",
    val action: String = "",
    val timestamp: Long = 0L
)
