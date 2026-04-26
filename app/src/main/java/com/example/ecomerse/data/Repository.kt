package com.example.ecomerse.data

import com.example.ecomerse.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*

object AppRepository {
    private val _products = MutableStateFlow<List<Product>>(
        listOf(
            Product("1", "Product A", "Standard Stock Item", 10.0),
            Product("2", "Product B", "Premium Quality Item", 25.0),
            Product("3", "Product C", "Bulk Supply Unit", 45.0),
            Product("4", "Product D", "Essential Tool", 15.0),
            Product("5", "Product E", "Accessories Kit", 8.0)
        )
    )
    val products: StateFlow<List<Product>> = _products

    private val _inventory = MutableStateFlow<List<InventoryItem>>(
        listOf(
            InventoryItem("1", "dist1", 100),
            InventoryItem("2", "dist1", 50),
            InventoryItem("3", "dist1", 20),
            InventoryItem("1", "dist2", 80),
            InventoryItem("4", "dist2", 150),
            InventoryItem("5", "dist2", 200)
        )
    )
    val inventory: StateFlow<List<InventoryItem>> = _inventory

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales

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

    fun recordSale(productId: String, distributorId: String, agentId: String, quantity: Int) {
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
                    agentId = agentId,
                    quantity = quantity,
                    timestamp = System.currentTimeMillis()
                )
                _sales.value = _sales.value + sale
                logActivity(agentId, "Recorded sale: $quantity units of Product $productId")
                
                // Rewards logic placeholder
                checkRewards(distributorId)
            }
        }
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
            UserRole.SALES_AGENT -> User("agent_001", "Alice Smith", UserRole.SALES_AGENT, "dist1")
            UserRole.STOCK_SUPERVISOR -> User("sup_bob", "Bob Johnson", UserRole.STOCK_SUPERVISOR, "dist1")
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
        "agent@ecomerse.com" to "pass1234",
        "supervisor@ecomerse.com" to "pass1234",
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
            email = "agent@ecomerse.com",
            password = "pass1234",
            user = User("agent_001", "Alice Smith", UserRole.SALES_AGENT, "dist1")
        )
        addAccount(
            email = "supervisor@ecomerse.com",
            password = "pass1234",
            user = User("sup_bob", "Bob Johnson", UserRole.STOCK_SUPERVISOR, "dist1")
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
            UserRole.SALES_AGENT -> "agent"
            UserRole.STOCK_SUPERVISOR -> "sup"
            UserRole.COMPANY_MANAGER -> "mgr"
        }
        val count = authAccounts.values.count { it.user.role == role } + 1
        return "${prefix}_${count.toString().padStart(3, '0')}"
    }
}
