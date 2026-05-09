package com.example.ecomerse.model

import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserRole {
    CUSTOMER,
    DISTRIBUTOR,
    COMPANY_MANAGER
}

@IgnoreExtraProperties
data class User(
    var id: String = "",
    var name: String = "",
    var role: UserRole = UserRole.CUSTOMER,
    var distributorId: String? = null,
    var email: String = "",
    var position: String = "Staff",
    var status: EmployeeStatus = EmployeeStatus.ACTIVE
)

enum class EmployeeStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

@IgnoreExtraProperties
data class LeaveRequest(
    var id: String = "",
    var employeeId: String = "",
    var employeeName: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var reason: String = "",
    var status: LeaveStatus = LeaveStatus.PENDING
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

@IgnoreExtraProperties
data class Product(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var unitPrice: Double = 0.0,
    var unit: String = "unit"
)

@IgnoreExtraProperties
data class InventoryItem(
    var productId: String = "",
    var distributorId: String = "",
    var quantity: Int = 0
)

@IgnoreExtraProperties
data class Sale(
    var id: String = "",
    var productId: String = "",
    var distributorId: String = "",
    var handledByUserId: String = "",
    var quantity: Int = 0,
    var timestamp: Long = 0,
    var paymentMethod: PaymentMethod = PaymentMethod.CASH,
    var requestId: String? = null
)

@IgnoreExtraProperties
data class GoodsRequest(
    var id: String = "",
    var customerId: String = "",
    var customerName: String = "",
    var distributorId: String = "",
    var productId: String = "",
    var quantity: Int = 0,
    var unitPrice: Double = 0.0,
    var totalAmount: Double = 0.0,
    var paymentMethod: PaymentMethod = PaymentMethod.CASH,
    var status: GoodsRequestStatus = GoodsRequestStatus.PENDING,
    var requestedAt: Long = 0,
    var dueAt: Long? = null,
    var fulfilledAt: Long? = null,
    var settledAt: Long? = null,
    var handledByUserId: String? = null,
    var note: String = ""
)

@IgnoreExtraProperties
data class ActivityLog(
    var id: String = "",
    var userId: String = "",
    var action: String = "",
    var timestamp: Long = 0
)
