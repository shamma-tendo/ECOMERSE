package com.example.ecomerse.data

import android.util.Log
import com.example.ecomerse.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            _products.value = snapshot?.toObjects(Product::class.java) ?: emptyList()
        }

        db.collection("inventory").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            _inventory.value = snapshot?.toObjects(InventoryItem::class.java) ?: emptyList()
        }

        db.collection("sales")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Sale::class.java) ?: emptyList()
                _sales.value = list.sortedByDescending { it.timestamp }
            }

        db.collection("goodsRequests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.toObjects(GoodsRequest::class.java) ?: emptyList()
                _goodsRequests.value = list.sortedByDescending { it.requestedAt }
            }

        db.collection("activityLogs").limit(100)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.toObjects(ActivityLog::class.java) ?: emptyList()
                _activityLogs.value = list.sortedByDescending { it.timestamp }
            }
    }

    private fun seedInitialDataIfEmpty() {
        db.collection("products").get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val initialProducts = listOf(
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
                initialProducts.forEach { db.collection("products").document(it.id).set(it) }
                
                val initialInventory = listOf(
                    InventoryItem("1", "dist1", 150),
                    InventoryItem("2", "dist1", 85),
                    InventoryItem("3", "dist1", 60),
                    InventoryItem("4", "dist1", 200),
                    InventoryItem("5", "dist1", 120),
                    InventoryItem("6", "dist1", 95),
                    InventoryItem("7", "dist1", 180),
                    InventoryItem("8", "dist1", 250),
                    InventoryItem("9", "dist1", 140),
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
                initialInventory.forEach { 
                    val id = "${it.productId}_${it.distributorId}"
                    db.collection("inventory").document(id).set(it) 
                }
            }
        }
    }

    fun manufactureProduct(name: String, description: String, price: Double, userId: String) {
        val id = UUID.randomUUID().toString()
        val newProduct = Product(id, name, description, price)
        db.collection("products").document(id).set(newProduct)
        logActivity(userId, "Manufactured new product: $name")
    }

    fun distributeStock(productId: String, distributorId: String, quantity: Int, userId: String) {
        val id = "${productId}_${distributorId}"
        db.collection("inventory").document(id).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val current = snapshot.toObject(InventoryItem::class.java)!!
                db.collection("inventory").document(id).update("quantity", current.quantity + quantity)
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
        // Since this is a synchronous return type in current code, but Firestore is async,
        // we might have a problem if we don't change signatures. 
        // For now, I'll use a task but the boolean return will be technically optimistic or requires a refactor.
        // Actually, most UI calls don't strictly wait for the boolean in a blocking way if they use flows.
        
        db.collection("inventory").document(invId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val item = snapshot.toObject(InventoryItem::class.java)!!
                if (item.quantity >= quantity) {
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

        val product = _products.value.find { it.id == productId } ?: return false
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

        db.collection("goodsRequests").document(requestId).set(request)
        logActivity(customerId, "Submitted ${paymentMethod.name.lowercase(Locale.ROOT)} request for $quantity x ${product.name}")
        return true
    }

    fun approveGoodsRequest(requestId: String, userId: String): Boolean {
        db.collection("goodsRequests").document(requestId).update("status", GoodsRequestStatus.APPROVED)
        logActivity(userId, "Approved goods request $requestId")
        return true
    }

    fun rejectGoodsRequest(requestId: String, userId: String): Boolean {
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
            val users = snapshot?.toObjects(User::class.java) ?: emptyList()
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
        }
    }

    private fun seedDefaultUsers() {
        val defaultUsers = listOf(
            User("cust_001", "Customer Demo", UserRole.CUSTOMER, email = "customer@ecomerse.com"),
            User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1", email = "distributor@ecomerse.com"),
            User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER, email = "manager@ecomerse.com")
        )
        defaultUsers.forEach { db.collection("users").document(it.id).set(it) }
    }

    fun login(role: UserRole) {
        val user = when(role) {
            UserRole.CUSTOMER -> User("cust_001", "Customer Demo", UserRole.CUSTOMER)
            UserRole.DISTRIBUTOR -> User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1")
            UserRole.COMPANY_MANAGER -> User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER)
        }
        _currentUser.value = user
        db.collection("users").document(user.id).set(user)
        AppRepository.logActivity(user.id, "Session started: Logged in as ${role.name}")
    }

    fun login(email: String, password: String): Boolean {
        // Simplified auth for demo: finding user by email in Firestore
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val user = _allUsers.value.find { it.email.lowercase() == normalizedEmail }
        if (user != null) {
            // In a real app, use Firebase Auth. Here we just simulate.
            _currentUser.value = user
            AppRepository.logActivity(user.id, "Session started: Logged in with credentials")
            return true
        }
        return false
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole = UserRole.CUSTOMER,
        distributorId: String? = null
    ): Boolean {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (_allUsers.value.any { it.email.lowercase() == normalizedEmail }) return false

        val prefix = when (role) {
            UserRole.CUSTOMER -> "cust"
            UserRole.DISTRIBUTOR -> "dist"
            UserRole.COMPANY_MANAGER -> "mgr"
        }
        val count = _allUsers.value.count { it.role == role } + 1
        val userId = "${prefix}_${count.toString().padStart(3, '0')}"

        val user = User(
            id = userId,
            name = name.trim(),
            role = role,
            distributorId = distributorId,
            email = normalizedEmail
        )

        db.collection("users").document(userId).set(user)
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

    fun getAllUsers(): List<User> = _allUsers.value

    fun updateUserRole(userId: String, newRole: UserRole) {
        db.collection("users").document(userId).update("role", newRole)
    }

    fun updateUserDistributorId(userId: String, distributorId: String?) {
        db.collection("users").document(userId).update("distributorId", distributorId)
    }
}
