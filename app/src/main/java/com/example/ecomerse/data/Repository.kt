package com.example.ecomerse.data

import android.util.Log
import com.example.ecomerse.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*

object AppRepository {
    private val db = FirebaseFirestore.getInstance()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales

    private val _goodsRequests = MutableStateFlow<List<GoodsRequest>>(emptyList())
    val goodsRequests: StateFlow<List<GoodsRequest>> = _goodsRequests

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs

    init {
        setupListeners()
        seedInitialDataIfEmpty()
    }

    private fun setupListeners() {
        db.collection("products").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            try {
                _products.value = snapshot?.toObjects(Product::class.java)?.filterNotNull() ?: emptyList()
            } catch (ex: Exception) {
                Log.e("Repository", "Error parsing products", ex)
            }
        }

        db.collection("inventory").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            try {
                _inventory.value = snapshot?.toObjects(InventoryItem::class.java)?.filterNotNull() ?: emptyList()
            } catch (ex: Exception) {
                Log.e("Repository", "Error parsing inventory", ex)
            }
        }

        db.collection("sales")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                try {
                    val list = snapshot?.toObjects(Sale::class.java)?.filterNotNull() ?: emptyList()
                    _sales.value = list.sortedByDescending { it.timestamp }
                } catch (ex: Exception) {
                    Log.e("Repository", "Error parsing sales", ex)
                }
            }

        db.collection("goodsRequests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                try {
                    val list = snapshot?.toObjects(GoodsRequest::class.java)?.filterNotNull() ?: emptyList()
                    _goodsRequests.value = list.sortedByDescending { it.requestedAt }
                } catch (ex: Exception) {
                    Log.e("Repository", "Error parsing requests", ex)
                }
            }

        db.collection("activityLogs").limit(100)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                try {
                    val list = snapshot?.toObjects(ActivityLog::class.java)?.filterNotNull() ?: emptyList()
                    _activityLogs.value = list.sortedByDescending { it.timestamp }
                } catch (ex: Exception) {
                    Log.e("Repository", "Error parsing logs", ex)
                }
            }
    }

    private fun seedInitialDataIfEmpty() {
        db.collection("products").get().addOnSuccessListener { productSnapshot ->
            val productIds = if (productSnapshot.isEmpty) {
                defaultProducts().also { products ->
                    products.forEach { db.collection("products").document(it.id).set(it) }
                }.map { it.id }
            } else {
                productSnapshot.toObjects(Product::class.java)
                    .map { it.id }
                    .filter { it.isNotBlank() }
            }

            db.collection("inventory").get().addOnSuccessListener { inventorySnapshot ->
                if (inventorySnapshot.isEmpty && productIds.isNotEmpty()) {
                    defaultInventory(productIds).forEach {
                        val id = "${it.productId}_${it.distributorId}"
                        db.collection("inventory").document(id).set(it)
                    }
                }
            }
        }
    }

    private fun defaultProducts(): List<Product> = listOf(
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

    private fun defaultInventory(productIds: List<String>): List<InventoryItem> {
        val seedLevels = listOf(150, 85, 60, 200, 120, 95, 180, 250, 140)
        return productIds.flatMapIndexed { index, productId ->
            val base = seedLevels[index % seedLevels.size]
            listOf(
                InventoryItem(productId, "dist1", base),
                InventoryItem(productId, "dist2", (base * 0.8).toInt().coerceAtLeast(20))
            )
        }
    }

    fun manufactureProduct(name: String, description: String, price: Double, userId: String) {
        val id = UUID.randomUUID().toString()
        val newProduct = Product(id, name, description, price)
        db.collection("products").document(id).set(newProduct)
        logActivity(userId, "Manufactured new product: $name")
    }

    fun distributeStock(productId: String, distributorId: String, quantity: Int, userId: String) {
        val id = "${productId}_$distributorId"
        db.collection("inventory").document(id).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val current = snapshot.toObject(InventoryItem::class.java)
                if (current != null) {
                    db.collection("inventory").document(id).update("quantity", current.quantity + quantity)
                }
            } else {
                db.collection("inventory").document(id).set(InventoryItem(productId, distributorId, quantity))
            }
        }
        logActivity(userId, "Distributed $quantity units of product $productId to $distributorId")
    }

    fun recordSale(
        productId: String,
        distributorId: String,
        handledByUserId: String,
        quantity: Int,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        requestId: String? = null
    ): Boolean {
        val invId = "${productId}_${distributorId}"
        db.collection("inventory").document(invId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val item = snapshot.toObject(InventoryItem::class.java)
                if (item != null && item.quantity >= quantity) {
                    db.collection("inventory").document(invId).update("quantity", item.quantity - quantity)
                    
                    val saleId = UUID.randomUUID().toString()
                    val sale = Sale(
                        id = saleId,
                        productId = productId,
                        distributorId = distributorId,
                        handledByUserId = handledByUserId,
                        quantity = quantity,
                        timestamp = System.currentTimeMillis(),
                        paymentMethod = paymentMethod,
                        requestId = requestId
                    )
                    db.collection("sales").document(saleId).set(sale)
                    logActivity(handledByUserId, "Recorded ${paymentMethod.name.lowercase(Locale.ROOT)} sale: $quantity units of Product $productId")
                }
            }
        }
        return true 
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

        val product = _products.value.find { it.id == productId }
            ?: defaultProducts().find { it.id == productId }
            ?: return false
        val now = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()
        val request = GoodsRequest(
            id = requestId,
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

        _goodsRequests.update { current ->
            (listOf(request) + current)
                .distinctBy { it.id }
                .sortedByDescending { it.requestedAt }
        }
        db.collection("goodsRequests").document(requestId).set(request)
        logActivity(customerId, "Submitted ${paymentMethod.name.lowercase(Locale.ROOT)} request for $quantity x ${product.name}")
        return true
    }

    private fun updateRequestLocally(
        requestId: String,
        transform: (GoodsRequest) -> GoodsRequest
    ) {
        _goodsRequests.update { current ->
            current.map { request -> if (request.id == requestId) transform(request) else request }
                .sortedByDescending { it.requestedAt }
        }
    }

    fun approveGoodsRequest(requestId: String, userId: String): Boolean {
        updateRequestLocally(requestId) { it.copy(status = GoodsRequestStatus.APPROVED) }
        db.collection("goodsRequests").document(requestId).update("status", GoodsRequestStatus.APPROVED)
        logActivity(userId, "Approved goods request $requestId")
        return true
    }

    fun rejectGoodsRequest(requestId: String, userId: String): Boolean {
        updateRequestLocally(requestId) { it.copy(status = GoodsRequestStatus.REJECTED) }
        db.collection("goodsRequests").document(requestId).update("status", GoodsRequestStatus.REJECTED)
        logActivity(userId, "Rejected goods request $requestId")
        return true
    }

    fun fulfillGoodsRequest(requestId: String, handledByUserId: String): Boolean {
        db.collection("goodsRequests").document(requestId).get().addOnSuccessListener { snapshot ->
            val request = snapshot.toObject(GoodsRequest::class.java) ?: return@addOnSuccessListener
            if (request.status == GoodsRequestStatus.REJECTED || request.status == GoodsRequestStatus.SETTLED) return@addOnSuccessListener

            val saleRecorded = recordSale(
                productId = request.productId,
                distributorId = request.distributorId,
                handledByUserId = handledByUserId,
                quantity = request.quantity,
                paymentMethod = request.paymentMethod,
                requestId = request.id
            )

            if (saleRecorded) {
                val now = System.currentTimeMillis()
                val newStatus = if (request.paymentMethod == PaymentMethod.CASH) GoodsRequestStatus.SETTLED else GoodsRequestStatus.FULFILLED
                updateRequestLocally(requestId) {
                    it.copy(
                        status = newStatus,
                        fulfilledAt = now,
                        settledAt = if (request.paymentMethod == PaymentMethod.CASH) now else it.settledAt,
                        handledByUserId = handledByUserId
                    )
                }
                db.collection("goodsRequests").document(requestId).update(
                    "status", newStatus,
                    "fulfilledAt", now,
                    "settledAt", if (request.paymentMethod == PaymentMethod.CASH) now else request.settledAt,
                    "handledByUserId", handledByUserId
                )
                logActivity(handledByUserId, "Fulfilled goods request $requestId")
            }
        }
        return true
    }

    fun settleCreditRequest(requestId: String, userId: String): Boolean {
        updateRequestLocally(requestId) {
            it.copy(
                status = GoodsRequestStatus.SETTLED,
                settledAt = System.currentTimeMillis()
            )
        }
        db.collection("goodsRequests").document(requestId).update(
            "status", GoodsRequestStatus.SETTLED,
            "settledAt", System.currentTimeMillis()
        )
        logActivity(userId, "Settled credit request $requestId")
        return true
    }

    fun logActivity(userId: String, action: String) {
        val id = UUID.randomUUID().toString()
        val log = ActivityLog(
            id = id,
            userId = userId,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        db.collection("activityLogs").document(id).set(log)
    }
}

object SessionManager {
    private val db = FirebaseFirestore.getInstance()
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())

    init {
        setupUserListener()
    }

    private fun setupUserListener() {
        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("SessionManager", "User listener failed", e)
                return@addSnapshotListener
            }
            try {
                val users = snapshot?.toObjects(User::class.java)?.filterNotNull() ?: emptyList()
                _allUsers.value = users
                
                // Sync current user if changed remotely
                _currentUser.value?.let { current ->
                    users.find { it.id == current.id }?.let { updated ->
                        if (updated != current) {
                            _currentUser.value = updated
                        }
                    }
                }

                // Seed users if collection is empty
                if (users.isEmpty()) {
                    seedDefaultUsers()
                }
            } catch (ex: Exception) {
                Log.e("SessionManager", "Error parsing users", ex)
            }
        }
    }

    private fun seedDefaultUsers() {
        val defaultUsers = listOf(
            User("cust_001", "Customer Demo", UserRole.CUSTOMER, email = "customer@ecomerse.com", passwordHash = "pass1234"),
            User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1", email = "distributor@ecomerse.com", passwordHash = "pass1234"),
            User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER, email = "manager@ecomerse.com", passwordHash = "pass1234")
        )
        defaultUsers.forEach { db.collection("users").document(it.id).set(it) }
        _allUsers.value = (_allUsers.value + defaultUsers).distinctBy { it.id }
    }

    private fun upsertLocalUser(user: User) {
        _allUsers.value = (_allUsers.value.filterNot { it.id == user.id } + user)
            .sortedBy { it.email.lowercase(Locale.ROOT) }
    }

    fun login(role: UserRole) {
        val user = when(role) {
            UserRole.CUSTOMER -> User("cust_001", "Customer Demo", UserRole.CUSTOMER, email = "customer@ecomerse.com", passwordHash = "pass1234")
            UserRole.DISTRIBUTOR -> User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1", email = "distributor@ecomerse.com", passwordHash = "pass1234")
            UserRole.COMPANY_MANAGER -> User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER, email = "manager@ecomerse.com", passwordHash = "pass1234")
        }
        upsertLocalUser(user)
        _currentUser.value = user
        db.collection("users").document(user.id).set(user)
        AppRepository.logActivity(user.id, "Session started: Logged in as ${role.name}")
    }

    fun login(email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val user = _allUsers.value.find { it.email.lowercase() == normalizedEmail }
        
        if (user != null) {
            // Existing user in cache: check password
            if (user.passwordHash == password) {
                // Ensure distributor users have a distributorId
                val updatedUser = if (user.role == UserRole.DISTRIBUTOR && user.distributorId == null) {
                    user.copy(distributorId = "dist1")
                } else {
                    user
                }
                upsertLocalUser(updatedUser)
                _currentUser.value = updatedUser
                AppRepository.logActivity(user.id, "Logged in")
                return true
            }
            return false // Wrong password
        }
        
        // User not in cache - try Firestore directly
        var foundUser: User? = null
        var success = false
        val semaphore = java.util.concurrent.Semaphore(0)
        
        db.collection("users").whereEqualTo("email", normalizedEmail).limit(1).get()
            .addOnSuccessListener { snapshot ->
                foundUser = snapshot.toObjects(User::class.java).firstOrNull()
                if (foundUser != null && foundUser!!.passwordHash == password) {
                    upsertLocalUser(foundUser!!)
                    _currentUser.value = foundUser
                    AppRepository.logActivity(foundUser!!.id, "Logged in")
                    success = true
                }
                semaphore.release()
            }
            .addOnFailureListener { semaphore.release() }
        
        try {
            semaphore.tryAcquire(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e("SessionManager", "Login semaphore interrupted", e)
        }
        
        return success
    }

    /**
     * Simplified entry point: Just works for the user.
     * If account exists, logs in. If new, creates it.
     */
    fun fastAccess(name: String, email: String, password: String, role: UserRole = UserRole.CUSTOMER): String? {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val existingUser = _allUsers.value.find { it.email.lowercase() == normalizedEmail }

        if (existingUser != null) {
            if (existingUser.passwordHash == password) {
                _currentUser.value = existingUser
                AppRepository.logActivity(existingUser.id, "Logged in")
                return null // Success login
            }
            return "Incorrect password for this email."
        }

        // New user
        if (name.isBlank()) return "What is your name?"
        if (email.isBlank() || !email.contains("@")) return "Please enter a valid email."
        if (password.length < 4) return "Use at least 4 characters for security."

        val userId = "${role.name.lowercase().take(4)}_${UUID.randomUUID().toString().take(6)}"

        val user = User(
            id = userId,
            name = name.trim(),
            role = role,
            distributorId = if (role == UserRole.DISTRIBUTOR) "dist1" else null, // Default hub for new distributors
            email = normalizedEmail,
            passwordHash = password
        )

        db.collection("users").document(userId).set(user)
        upsertLocalUser(user)
        _currentUser.value = user
        AppRepository.logActivity(userId, "Account created")
        return null // Success signup
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

    fun getAllUsers(): List<User> = _allUsers.value

    fun updateUserRole(userId: String, newRole: UserRole) {
        db.collection("users").document(userId).update("role", newRole)
    }

    fun updateUserDistributorId(userId: String, distributorId: String?) {
        db.collection("users").document(userId).update("distributorId", distributorId)
    }
}
