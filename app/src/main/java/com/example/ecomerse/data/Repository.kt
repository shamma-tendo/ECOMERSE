package com.example.ecomerse.data

import com.example.ecomerse.model.*
import com.google.firebase.auth.FirebaseAuth
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

    private val _employees = MutableStateFlow<List<User>>(emptyList())
    val employees: StateFlow<List<User>> = _employees

    private val _leaveRequests = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRequest>> = _leaveRequests

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers

    init {
        setupListeners()
    }

    private fun setupListeners() {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(User::class.java)?.let { _allUsers.value = it }
        }
        db.collection("products").addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(Product::class.java)?.let { _products.value = it }
        }
        db.collection("inventory").addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(InventoryItem::class.java)?.let { _inventory.value = it }
        }
        db.collection("sales").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(Sale::class.java)?.let { _sales.value = it }
        }
        db.collection("goodsRequests").orderBy("requestedAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(GoodsRequest::class.java)?.let { _goodsRequests.value = it }
        }
        db.collection("activityLogs").orderBy("timestamp", Query.Direction.DESCENDING).limit(100).addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(ActivityLog::class.java)?.let { _activityLogs.value = it }
        }
        db.collection("users").whereIn("role", listOf(UserRole.DISTRIBUTOR.name, UserRole.COMPANY_MANAGER.name)).addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(User::class.java)?.let { _employees.value = it }
        }
        db.collection("leaveRequests").addSnapshotListener { snapshot, _ ->
            snapshot?.toObjects(LeaveRequest::class.java)?.let { _leaveRequests.value = it }
        }
    }

    fun seedDataToFirebase() {
        db.collection("products").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                performSeed()
            }
        }
    }

    private fun performSeed() {
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
            InventoryItem("1", "dist1", 150), InventoryItem("2", "dist1", 85), InventoryItem("3", "dist1", 60),
            InventoryItem("4", "dist1", 200), InventoryItem("5", "dist1", 120), InventoryItem("6", "dist1", 95),
            InventoryItem("7", "dist1", 180), InventoryItem("8", "dist1", 250), InventoryItem("9", "dist1", 140),
            InventoryItem("1", "dist2", 120), InventoryItem("2", "dist2", 90), InventoryItem("3", "dist2", 75),
            InventoryItem("4", "dist2", 220), InventoryItem("5", "dist2", 110), InventoryItem("6", "dist2", 105),
            InventoryItem("7", "dist2", 200), InventoryItem("8", "dist2", 280), InventoryItem("9", "dist2", 160)
        )
        initialInventory.forEach { db.collection("inventory").document("${it.distributorId}_${it.productId}").set(it) }

        val initialEmployees = listOf(
            User("dist_staff_1", "Grace Namara", UserRole.DISTRIBUTOR, "dist1", "grace@ecomerse.com", "Distribution Lead"),
            User("dist_staff_2", "Peter Ouma", UserRole.DISTRIBUTOR, "dist2", "peter@ecomerse.com", "Warehouse Coordinator")
        )
        initialEmployees.forEach { db.collection("users").document(it.id).set(it) }

        val initialLeaves = listOf(
            LeaveRequest("L1", "agent_001", "Alice Smith", "2023-12-01", "2023-12-05", "Vacation"),
            LeaveRequest("L2", "agent_002", "Charlie Brown", "2023-12-10", "2023-12-11", "Medical")
        )
        initialLeaves.forEach { db.collection("leaveRequests").document(it.id).set(it) }
        
        SessionManager.seedDefaultAccountsToFirebase()
    }

    fun updateLeaveStatus(requestId: String, status: LeaveStatus) {
        db.collection("leaveRequests").document(requestId).update("status", status)
    }

    fun updateEmployeeStatus(employeeId: String, status: EmployeeStatus) {
        db.collection("users").document(employeeId).update("status", status)
    }

    fun manufactureProduct(name: String, description: String, price: Double, userId: String) {
        val id = (products.value.size + 1).toString()
        val newProduct = Product(id, name, description, price)
        db.collection("products").document(id).set(newProduct)
        logActivity(userId, "Manufactured new product: $name")
    }

    fun distributeStock(productId: String, distributorId: String, quantity: Int, userId: String) {
        val docId = "${distributorId}_$productId"
        db.collection("inventory").document(docId).get().addOnSuccessListener { doc ->
            val currentQty = doc.toObject(InventoryItem::class.java)?.quantity ?: 0
            db.collection("inventory").document(docId).set(InventoryItem(productId, distributorId, currentQty + quantity))
            logActivity(userId, "Distributed $quantity units of product $productId to $distributorId")
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
        val docId = "${distributorId}_$productId"
        db.collection("inventory").document(docId).get().addOnSuccessListener { doc ->
            val item = doc.toObject(InventoryItem::class.java)
            if (item != null && item.quantity >= quantity) {
                db.collection("inventory").document(docId).update("quantity", item.quantity - quantity)
                
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
                db.collection("sales").document(sale.id).set(sale)
                logActivity(handledByUserId, "Recorded ${paymentMethod.name.lowercase(Locale.ROOT)} sale: $quantity units of Product $productId")
            }
        }
        return true // Firestore is async, but we return true for API compatibility
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
        val product = products.value.find { it.id == productId } ?: return false
        
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
            requestedAt = System.currentTimeMillis(),
            dueAt = if (paymentMethod == PaymentMethod.CREDIT) System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 else null,
            note = note.trim()
        )
        db.collection("goodsRequests").document(request.id).set(request)
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
        db.collection("goodsRequests").document(requestId).get().addOnSuccessListener { doc ->
            val request = doc.toObject(GoodsRequest::class.java) ?: return@addOnSuccessListener
            if (request.status == GoodsRequestStatus.REJECTED || request.status == GoodsRequestStatus.SETTLED) return@addOnSuccessListener

            recordSale(
                productId = request.productId,
                distributorId = request.distributorId,
                handledByUserId = handledByUserId,
                quantity = request.quantity,
                paymentMethod = request.paymentMethod,
                requestId = request.id
            )

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
        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            userId = userId,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        db.collection("activityLogs").document(log.id).set(log)
    }
}

object SessionManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                db.collection("users").document(firebaseUser.uid).get().addOnSuccessListener { doc ->
                    val user = doc.toObject(User::class.java)
                    _currentUser.value = user
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    fun login(role: UserRole) {
        val email = when(role) {
            UserRole.CUSTOMER -> "customer@ecomerse.com"
            UserRole.DISTRIBUTOR -> "distributor@ecomerse.com"
            UserRole.COMPANY_MANAGER -> "manager@ecomerse.com"
        }
        login(email, "pass1234") { _, _ -> }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Email and password cannot be empty")
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Login failed")
                }
            }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole = UserRole.CUSTOMER,
        distributorId: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            onResult(false, "Please provide all details. Password must be at least 6 characters.")
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    val user = User(
                        id = uid,
                        name = name.trim(),
                        role = role,
                        distributorId = distributorId,
                        email = email.trim().lowercase(Locale.ROOT)
                    )
                    db.collection("users").document(uid).set(user)
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Registration failed")
                }
            }
    }

    fun demoCredentials(): List<Pair<String, String>> = listOf(
        "customer@ecomerse.com" to "pass1234",
        "distributor@ecomerse.com" to "pass1234",
        "manager@ecomerse.com" to "pass1234"
    )

    fun logout() {
        auth.signOut()
    }

    fun seedDefaultAccountsToFirebase() {
        // Firebase Auth users must be created via createUserWithEmailAndPassword.
        // This seeding is for the Firestore profile matching.
        // For a real app, you'd manually create these 3 users in Firebase Console first.
        seedAccount("customer@ecomerse.com", "pass1234", User("cust_001", "Customer Demo", UserRole.CUSTOMER))
        seedAccount("distributor@ecomerse.com", "pass1234", User("dist_admin_1", "Global Logistics", UserRole.DISTRIBUTOR, "dist1"))
        seedAccount("manager@ecomerse.com", "pass1234", User("mgr_carol", "Carol White", UserRole.COMPANY_MANAGER))
    }

    private fun seedAccount(email: String, password: String, user: User) {
        // Just ensuring Firestore entries exist for these demo IDs if they happen to login
        db.collection("users").document(user.id).set(user)
    }

    private fun buildUserId(role: UserRole): String {
        val prefix = when (role) {
            UserRole.CUSTOMER -> "cust"
            UserRole.DISTRIBUTOR -> "dist"
            UserRole.COMPANY_MANAGER -> "mgr"
        }
        return "${prefix}_${System.currentTimeMillis().toString().takeLast(6)}"
    }
}
