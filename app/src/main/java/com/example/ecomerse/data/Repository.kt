package com.example.ecomerse.data

import com.example.ecomerse.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*

object AppRepository {
    private val _products = MutableStateFlow<List<Product>>(
        listOf(
            Product("1", "Rice", "High-quality long-grain rice, 25kg per bag", 84375.0, "bag (25kg)"),
            Product("2", "Cooking Oil", "Pure vegetable cooking oil, 5L per jerrican", 32800.0, "jerrican (5L)"),
            Product("3", "Sugar", "Granulated sugar, 50kg per bag", 142500.0, "bag (50kg)"),
            Product("4", "Laundry Detergent", "Powerful cleaning powder, 1kg per pack", 13125.0, "pack (1kg)"),
            Product("5", "Bar Soap", "Premium bath soap, box of 72 bars", 53400.0, "box (72 bars)"),
            Product("6", "Toothpaste", "Fluoride toothpaste, box of 12 tubes", 63700.0, "box (12 tubes)"),
            Product("7", "Toilet Paper", "Soft tissue rolls, pack of 10", 22500.0, "pack (10 rolls)"),
            Product("8", "Bottled Water", "Purified drinking water, crate of 24 bottles", 16875.0, "crate (24 bottles)"),
            Product("9", "Diapers", "Infant/toddler diapers, pack of 50", 69375.0, "pack (50)")
        )
    )
    val products: StateFlow<List<Product>> = _products

    private val _inventory = MutableStateFlow<List<InventoryItem>>(
        listOf(
            // North Hub (dist1) inventory
            InventoryItem("1", "dist1", 150),
            InventoryItem("2", "dist1", 85),
            InventoryItem("3", "dist1", 60),
            InventoryItem("4", "dist1", 200),
            InventoryItem("5", "dist1", 120),
            InventoryItem("6", "dist1", 95),
            InventoryItem("7", "dist1", 180),
            InventoryItem("8", "dist1", 250),
            InventoryItem("9", "dist1", 140),
            // Metro Hub (dist2) inventory
            InventoryItem("1", "dist2", 120),
            InventoryItem("2", "dist2", 90),
            InventoryItem("3", "dist2", 75),
            InventoryItem("4", "dist2", 220),
            InventoryItem("5", "dist2", 110),
            InventoryItem("6", "dist2", 105),
            InventoryItem("7", "dist2", 200),
            InventoryItem("8", "dist2", 280),
            InventoryItem("9", "dist2", 160)
        )
    )
    val inventory: StateFlow<List<InventoryItem>> = _inventory

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales

    private val _goodsRequests = MutableStateFlow<List<GoodsRequest>>(emptyList())
    val goodsRequests: StateFlow<List<GoodsRequest>> = _goodsRequests

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs

    fun manufactureProduct(name: String, description: String, price: Double, userId: String) {
        val newProduct = Product(
            id = (products.value.size + 1).toString(),
            name = name,
            description = description,
            unitPrice = price
        )
        _products.update { it + newProduct }
        logActivity(userId, "Manufactured new product: $name")
    }

    fun distributeStock(productId: String, distributorId: String, quantity: Int, userId: String) {
        val currentInventory = _inventory.value.toMutableList()
        val index = currentInventory.indexOfFirst { it.productId == productId && it.distributorId == distributorId }
        
        if (index != -1) {
            val item = currentInventory[index]
            currentInventory[index] = item.copy(quantity = item.quantity + quantity)
            _inventory.value = currentInventory
            logActivity(userId, "Distributed $quantity units of product $productId to $distributorId")
        } else {
            currentInventory.add(InventoryItem(productId, distributorId, quantity))
            _inventory.value = currentInventory
            logActivity(userId, "Distributed initial $quantity units of product $productId to $distributorId")
        }
    }

    fun recordSale(
        productId: String,
        distributorId: String,
        handledByUserId: String,
        quantity: Int,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        requestId: String? = null
    ): Boolean {
        val currentInventory = _inventory.value.toMutableList()
        val index = currentInventory.indexOfFirst { it.productId == productId && it.distributorId == distributorId }
        
        if (index != -1) {
            val item = currentInventory[index]
            if (item.quantity >= quantity) {
                currentInventory[index] = item.copy(quantity = item.quantity - quantity)
                _inventory.value = currentInventory
                
                val sale = Sale(
                    id = UUID.randomUUID().toString(),
                    productId = productId,
                    distributorId = distributorId,
                    handledByUserId = handledByUserId,
                    quantity = quantity,
                    timestamp = System.currentTimeMillis(),
                    paymentMethod = paymentMethod,
                    requestId = requestId
                )
                _sales.value = _sales.value + sale
                logActivity(handledByUserId, "Recorded ${paymentMethod.name.lowercase(Locale.ROOT)} sale: $quantity units of Product $productId")

                // Rewards logic placeholder
                checkRewards(distributorId)
                return true
            }
        }

        return false
    }

    private fun checkRewards(distributorId: String) {
        val distSales = _sales.value.filter { it.distributorId == distributorId }
        if (distSales.size % 10 == 0) {
            logActivity("SYSTEM", "Distributor $distributorId reached a sales milestone! Reward unlocked.")
        }
    }

    fun updateStock(productId: String, distributorId: String, addedQuantity: Int, userId: String) {
        distributeStock(productId, distributorId, addedQuantity, userId)
    }

    fun submitGoodsRequest(
        customerId: String,
        customerName: String,
        distributorId: String,
        productId: String,
        quantity: Int,
        paymentMethod: PaymentMethod,
        note: String = ""
    ): Boolean {
        if (quantity <= 0) return false

        val product = _products.value.find { it.id == productId } ?: return false
        val now = System.currentTimeMillis()
        val request = GoodsRequest(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            customerName = customerName,
            distributorId = distributorId,
            productId = productId,
            quantity = quantity,
            unitPrice = product.unitPrice,
            totalAmount = product.unitPrice * quantity,
            paymentMethod = paymentMethod,
            requestedAt = now,
            dueAt = if (paymentMethod == PaymentMethod.CREDIT) now + 30L * 24 * 60 * 60 * 1000 else null,
            note = note.trim()
        )

        _goodsRequests.value = _goodsRequests.value + request
        logActivity(customerId, "Submitted ${paymentMethod.name.lowercase(Locale.ROOT)} request for $quantity x ${product.name}")
        return true
    }

    fun approveGoodsRequest(requestId: String, userId: String): Boolean {
        return updateGoodsRequest(requestId) {
            if (it.status == GoodsRequestStatus.PENDING) {
                it.copy(status = GoodsRequestStatus.APPROVED)
            } else {
                it
            }
        }.also { updated ->
            if (updated) logActivity(userId, "Approved goods request $requestId")
        }
    }

    fun rejectGoodsRequest(requestId: String, userId: String): Boolean {
        return updateGoodsRequest(requestId) {
            if (it.status == GoodsRequestStatus.SETTLED) {
                it
            } else {
                it.copy(status = GoodsRequestStatus.REJECTED)
            }
        }.also { updated ->
            if (updated) logActivity(userId, "Rejected goods request $requestId")
        }
    }

    fun fulfillGoodsRequest(requestId: String, handledByUserId: String): Boolean {
        val request = _goodsRequests.value.find { it.id == requestId } ?: return false
        if (request.status == GoodsRequestStatus.REJECTED || request.status == GoodsRequestStatus.SETTLED) return false

        val saleRecorded = recordSale(
            productId = request.productId,
            distributorId = request.distributorId,
            handledByUserId = handledByUserId,
            quantity = request.quantity,
            paymentMethod = request.paymentMethod,
            requestId = request.id
        )

        if (!saleRecorded) return false

        val now = System.currentTimeMillis()
        val newStatus = if (request.paymentMethod == PaymentMethod.CASH) GoodsRequestStatus.SETTLED else GoodsRequestStatus.FULFILLED
        val updatedRequest = request.copy(
            status = newStatus,
            fulfilledAt = now,
            settledAt = if (request.paymentMethod == PaymentMethod.CASH) now else request.settledAt,
            handledByUserId = handledByUserId
        )

        _goodsRequests.value = _goodsRequests.value.map { if (it.id == requestId) updatedRequest else it }
        logActivity(handledByUserId, "Fulfilled goods request $requestId")
        return true
    }

    fun settleCreditRequest(requestId: String, userId: String): Boolean {
        val request = _goodsRequests.value.find { it.id == requestId } ?: return false
        if (request.paymentMethod != PaymentMethod.CREDIT || request.status != GoodsRequestStatus.FULFILLED) return false

        _goodsRequests.update { list ->
            list.map {
                if (it.id == requestId) {
                    it.copy(status = GoodsRequestStatus.SETTLED, settledAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
        }

        logActivity(userId, "Settled credit request $requestId")
        return true
    }

    private fun updateGoodsRequest(requestId: String, transform: (GoodsRequest) -> GoodsRequest): Boolean {
        val existing = _goodsRequests.value.find { it.id == requestId } ?: return false
        val updated = transform(existing)
        _goodsRequests.value = _goodsRequests.value.map { if (it.id == requestId) updated else it }
        return true
    }

    fun logActivity(userId: String, action: String) {
        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            userId = userId,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        _activityLogs.value = _activityLogs.value + log
    }
}

object SessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private data class AuthAccount(
        val email: String,
        val password: String,
        val user: User
    )

    private val authAccounts = mutableMapOf<String, AuthAccount>()

    init {
        seedDefaultAccounts()
    }

    fun login(role: UserRole) {
        val user = when(role) {
            UserRole.CUSTOMER -> User("cust_001", "Customer Demo", UserRole.CUSTOMER)
            UserRole.DISTRIBUTOR -> User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1")
            UserRole.COMPANY_MANAGER -> User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER)
        }
        _currentUser.value = user
        AppRepository.logActivity(user.id, "Session started: Logged in as ${role.name}")
    }

    fun login(email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val account = authAccounts[normalizedEmail] ?: return false
        if (account.password != password) return false

        _currentUser.value = account.user
        AppRepository.logActivity(account.user.id, "Session started: Logged in with credentials")
        return true
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole = UserRole.CUSTOMER,
        distributorId: String? = null
    ): Boolean {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (name.isBlank() || normalizedEmail.isBlank() || password.length < 4) return false
        if (authAccounts.containsKey(normalizedEmail)) return false

        val userId = buildUserId(role)
        val user = User(
            id = userId,
            name = name.trim(),
            role = role,
            distributorId = distributorId
        )

        addAccount(normalizedEmail, password, user)
        AppRepository.logActivity(userId, "Account created")
        return true
    }

    fun demoCredentials(): List<Pair<String, String>> = listOf(
        "customer@ecomerse.com" to "pass1234",
        "distributor@ecomerse.com" to "pass1234",
        "manager@ecomerse.com" to "pass1234"
    )

    fun logout() {
        _currentUser.value?.let { 
            AppRepository.logActivity(it.id, "Session ended: Logged out")
        }
        _currentUser.value = null
    }

    private fun seedDefaultAccounts() {
        addAccount(
            email = "customer@ecomerse.com",
            password = "pass1234",
            user = User("cust_001", "Customer Demo", UserRole.CUSTOMER)
        )
        addAccount(
            email = "distributor@ecomerse.com",
            password = "pass1234",
            user = User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1")
        )
        addAccount(
            email = "manager@ecomerse.com",
            password = "pass1234",
            user = User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER)
        )
    }

    private fun addAccount(email: String, password: String, user: User) {
        authAccounts[email.trim().lowercase(Locale.ROOT)] = AuthAccount(
            email = email.trim().lowercase(Locale.ROOT),
            password = password,
            user = user
        )
    }

    private fun buildUserId(role: UserRole): String {
        val prefix = when (role) {
            UserRole.CUSTOMER -> "cust"
            UserRole.DISTRIBUTOR -> "dist"
            UserRole.COMPANY_MANAGER -> "mgr"
        }
        val count = authAccounts.values.count { it.user.role == role } + 1
        return "${prefix}_${count.toString().padStart(3, '0')}"
    }
}
