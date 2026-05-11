package com.example.ecomerse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.data.ChatRepositoryProvider
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.UserRole
import com.example.ecomerse.ui.chat.ChatConversationScreen
import com.example.ecomerse.ui.chat.ChatThreadListScreen
import com.example.ecomerse.ui.chat.ChatViewModel
import com.example.ecomerse.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LandingScreen(onNavigateToLogin: () -> Unit, onNavigateToSignUp: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Graphic
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 3f
                    
                    val nodeColor = if (isDark) CyanPrimary else primaryColor
                    val accentColor = OrangeAccent
                    
                    // Connection lines
                    for (i in 0..4) {
                        val angle = Math.toRadians((i * 72.0) - 90.0)
                        val end = Offset(
                            center.x + (radius * cos(angle)).toFloat(),
                            center.y + (radius * sin(angle)).toFloat()
                        )
                        drawLine(nodeColor.copy(alpha = 0.5f), center, end, strokeWidth = 2f)
                        drawCircle(nodeColor, radius = 6f, center = end)
                    }

                    // Central Diamond
                    val diamondSize = 20f
                    val path = Path().apply {
                        moveTo(center.x, center.y - diamondSize)
                        lineTo(center.x + diamondSize, center.y)
                        lineTo(center.x, center.y + diamondSize)
                        lineTo(center.x - diamondSize, center.y)
                        close()
                    }
                    drawPath(path, accentColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ECOMERSE",
                color = if (isDark) CyanPrimary else primaryColor,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "DISTRIBUTED STOCK INTELLIGENCE",
                color = (if (isDark) CyanPrimary else primaryColor).copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("LOG IN", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavigateToSignUp,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                border = BorderStroke(1.dp, primaryColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIGN UP", fontWeight = FontWeight.Bold)
            }
        }

        // Footer "from GROUP S"
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "from",
                color = onBackgroundColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Text(
                text = "GROUP S",
                color = onBackgroundColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun SignUpScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Account",
            color = primaryColor,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name", color = primaryColor.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = primaryColor.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = primaryColor.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select your role:", style = MaterialTheme.typography.labelMedium, color = primaryColor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UserRole.entries.forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    label = { Text(role.name.replace("_", " ").lowercase().capitalize(Locale.ROOT)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val error = SessionManager.fastAccess(
                    name = name,
                    email = email,
                    password = password,
                    role = selectedRole
                )
                errorMessage = error
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("GET STARTED", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Already have an account? Back", color = primaryColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun LoginScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome Back",
            color = primaryColor,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = primaryColor.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = primaryColor.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { /* Forgot password logic */ }) {
                Text("Forgot Password?", color = primaryColor.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val success = SessionManager.login(email, password)
                errorMessage = if (success) null else "Invalid email or password"
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("LOG IN", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Demo Credentials (Email / Password)",
            style = MaterialTheme.typography.labelMedium,
            color = primaryColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        SessionManager.demoCredentials().forEach { (demoEmail, demoPassword) ->
            val roleName = demoEmail.split("@")[0].capitalize(Locale.ROOT)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                color = primaryColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(roleName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = primaryColor)
                    Text("$demoEmail / $demoPassword", style = MaterialTheme.typography.bodySmall, color = primaryColor.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = primaryColor.copy(alpha = 0.2f))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Quick Access Demo Roles", style = MaterialTheme.typography.labelSmall, color = primaryColor.copy(alpha = 0.5f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UserRole.entries.forEach { role ->
                IconButton(onClick = { SessionManager.login(role) }) {
                    val iconText = role.name.take(1)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = primaryColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            iconText, 
                            modifier = Modifier.padding(4.dp), 
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Back to Landing", color = primaryColor.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun CustomerDashboardScreen() {
    val user by SessionManager.currentUser.collectAsState()
    val products by AppRepository.products.collectAsState()
    val inventory by AppRepository.inventory.collectAsState()
    val requests by AppRepository.goodsRequests.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(CustomerBottomTab.HOME) }
    var chatThreadId by rememberSaveable { mutableStateOf<String?>(null) }
    
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(
            chatRepository = ChatRepositoryProvider.repository,
            sessionManager = SessionManager
        )
    )

    Scaffold(
        bottomBar = {
            if (selectedTab != CustomerBottomTab.CHAT || chatThreadId == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        tonalElevation = 2.dp,
                        shadowElevation = 8.dp
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(84.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            CustomerBottomTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        Crossfade(targetState = selectedTab, label = "customer-tab") { currentTab ->
            when (currentTab) {
                CustomerBottomTab.HOME -> CustomerHomeTabContent(
                    modifier = contentModifier,
                    user = user,
                    products = products,
                    inventory = inventory,
                    requests = requests
                )

                CustomerBottomTab.REQUESTS -> CustomerRequestsTabContent(
                    modifier = contentModifier,
                    user = user,
                    requests = requests,
                    products = products
                )

                CustomerBottomTab.CHAT -> RoleChatHost(
                    modifier = contentModifier.fillMaxSize(),
                    chatViewModel = chatViewModel,
                    activeThreadId = chatThreadId,
                    onThreadChange = { chatThreadId = it }
                )
            }
        }
    }
}

@Composable
fun DistributorInventoryScreen() {
    val user by SessionManager.currentUser.collectAsState()
    val inventory by AppRepository.inventory.collectAsState()
    val products by AppRepository.products.collectAsState()
    val sales by AppRepository.sales.collectAsState()
    val requests by AppRepository.goodsRequests.collectAsState()

    // Hub switcher state - allow distributor to switch between hubs
    var selectedHub by rememberSaveable { mutableStateOf("dist1") }

    // Use selected hub instead of fixed user distributor ID
    val distId = selectedHub

    val distInventory = remember(inventory, distId) {
        inventory.filter { it.distributorId == distId && it.productId.isNotBlank() }
    }
    
    val distributorSales = remember(sales, distId) {
        sales.filter { it.distributorId == distId }
    }
    
    val distributorRequests = remember(requests, distId) {
        requests.filter { it.distributorId == distId }
    }
    
    val totalUnits = remember(distInventory) { distInventory.sumOf { it.quantity } }
    val lowStockCount = remember(distInventory) { distInventory.count { it.quantity < 10 } }
    
    val weeklyUnits = remember(distributorSales) {
        distributorSales
            .filter { 
                val ts = it.timestamp
                ts > 0 && (System.currentTimeMillis() - ts <= 7L * 24 * 60 * 60 * 1000) 
            }
            .sumOf { it.quantity }
    }
    
    val showRestockDialog = remember { mutableStateOf(false) }
    val selectedProductId = remember { mutableStateOf<String?>(null) }
    val showCatalog = remember { mutableStateOf(false) }
    
    var selectedTab by rememberSaveable { mutableStateOf(DistributorBottomTab.INVENTORY) }
    var chatThreadId by rememberSaveable { mutableStateOf<String?>(null) }

    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(
            chatRepository = ChatRepositoryProvider.repository,
            sessionManager = SessionManager
        )
    )

    Scaffold(
        bottomBar = {
            if (selectedTab != DistributorBottomTab.CHAT || chatThreadId == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        tonalElevation = 2.dp,
                        shadowElevation = 8.dp
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(84.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            DistributorBottomTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        Crossfade(targetState = selectedTab, label = "distributor-tab") { currentTab ->
            when (currentTab) {
                DistributorBottomTab.INVENTORY -> DistributorInventoryTabContent(
                    modifier = contentModifier,
                    distributorId = distId,
                    selectedHub = distId,
                    onHubChange = { selectedHub = it },
                    inventoryCount = distInventory.size,
                    totalUnits = totalUnits,
                    weeklyUnits = weeklyUnits,
                    lowStockCount = lowStockCount,
                    distInventory = distInventory,
                    products = products,
                    onAddProductClick = { showCatalog.value = true },
                    onRestockClick = { productId ->
                        selectedProductId.value = productId
                        showRestockDialog.value = true
                    }
                )

                DistributorBottomTab.SHIPMENTS -> DistributorRequestsTabContent(
                    modifier = contentModifier,
                    distributorId = distId,
                    requestsCount = distributorRequests.size,
                    requests = distributorRequests,
                    products = products
                )

                DistributorBottomTab.CATALOG -> DistributorAnalyticsTabContent(
                    modifier = contentModifier,
                    distributorId = distId,
                    inventory = inventory,
                    sales = sales,
                    products = products
                )

                DistributorBottomTab.CHAT -> RoleChatHost(
                    modifier = contentModifier.fillMaxSize(),
                    chatViewModel = chatViewModel,
                    activeThreadId = chatThreadId,
                    onThreadChange = { chatThreadId = it }
                )

                DistributorBottomTab.SETTINGS -> DistributorPlaceholderTab(
                    modifier = contentModifier,
                    title = "Settings",
                    subtitle = "Notifications, profile, and preferences can be managed here."
                )
            }
        }
    }

    if (showCatalog.value) {
        AlertDialog(
            onDismissRequest = { showCatalog.value = false },
            title = { Text("Add from Catalog") },
            text = {
                LazyColumn {
                    items(products) { p ->
                        if (distInventory.none { it.productId == p.id }) {
                            ListItem(
                                headlineContent = { Text(p.name) },
                                modifier = Modifier.clickable {
                                    selectedProductId.value = p.id
                                    showCatalog.value = false
                                    showRestockDialog.value = true
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCatalog.value = false }) { Text("Close") } }
        )
    }

    val currentSelectedId = selectedProductId.value
    if (showRestockDialog.value && currentSelectedId != null) {
        RestockDialog(
            productId = currentSelectedId,
            onDismiss = { showRestockDialog.value = false },
            onConfirm = { qty ->
                AppRepository.updateStock(currentSelectedId, distId, qty, user?.id ?: "")
                showRestockDialog.value = false
            }
        )
    }
}

enum class DistributorBottomTab(val title: String) {
    INVENTORY("Home"),
    SHIPMENTS("Customer Requests"),
    CATALOG("Orders"),
    CHAT("Chat"),
    SETTINGS("Settings");

    val icon: ImageVector
        get() = when (this) {
            INVENTORY -> Icons.Default.Home
            SHIPMENTS -> Icons.Default.LocalShipping
            CATALOG -> Icons.Default.Inventory2
            CHAT -> Icons.AutoMirrored.Filled.Chat
            SETTINGS -> Icons.Default.Settings
        }
}

enum class CustomerBottomTab(val title: String) {
    HOME("Home"),
    REQUESTS("Requests"),
    CHAT("Chat");

    val icon: ImageVector
        get() = when (this) {
            HOME -> Icons.Default.Home
            REQUESTS -> Icons.Default.LocalShipping
            CHAT -> Icons.AutoMirrored.Filled.Chat
        }
}

@Composable
private fun CustomerHomeTabContent(
    modifier: Modifier = Modifier,
    user: com.example.ecomerse.model.User?,
    products: List<com.example.ecomerse.model.Product>,
    inventory: List<com.example.ecomerse.model.InventoryItem>,
    requests: List<com.example.ecomerse.model.GoodsRequest>
) {
    val customerRequests = remember(requests, user) { 
        try {
            requests.filter { it.customerId == user?.id } 
        } catch (e: Exception) {
            emptyList()
        }
    }
    val openCreditBalance = remember(customerRequests) {
        try {
            customerRequests
                .filter { it.paymentMethod == com.example.ecomerse.model.PaymentMethod.CREDIT && (it.status == com.example.ecomerse.model.GoodsRequestStatus.APPROVED || it.status == com.example.ecomerse.model.GoodsRequestStatus.FULFILLED) }
                .sumOf { it.totalAmount }
        } catch (e: Exception) {
            0.0
        }
    }
    val completedRequests = remember(customerRequests) { 
        try {
            customerRequests.count { it.status == com.example.ecomerse.model.GoodsRequestStatus.SETTLED } 
        } catch (e: Exception) {
            0
        }
    }
    
    val requestCountAnimated by animateIntAsState(targetValue = customerRequests.size, label = "customer-request-count")
    val balanceAnimated by animateFloatAsState(targetValue = openCreditBalance.toFloat(), label = "customer-credit-balance")

    val now = System.currentTimeMillis()

    fun isSameDay(ts: Long, now: Long): Boolean {
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calTs = Calendar.getInstance().apply { timeInMillis = ts }
        return calNow.get(Calendar.YEAR) == calTs.get(Calendar.YEAR) && calNow.get(Calendar.DAY_OF_YEAR) == calTs.get(Calendar.DAY_OF_YEAR)
    }

    fun isWithinLastDays(ts: Long, now: Long, days: Int): Boolean {
        return now - ts <= days * 24L * 60L * 60L * 1000L
    }

    fun isSameMonth(ts: Long, now: Long): Boolean {
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calTs = Calendar.getInstance().apply { timeInMillis = ts }
        return calNow.get(Calendar.YEAR) == calTs.get(Calendar.YEAR) && calNow.get(Calendar.MONTH) == calTs.get(Calendar.MONTH)
    }

    fun sumForPeriod(method: com.example.ecomerse.model.PaymentMethod, period: String): Double {
        val relevant = customerRequests.filter { (it.paymentMethod == method) && (it.status == com.example.ecomerse.model.GoodsRequestStatus.APPROVED || it.status == com.example.ecomerse.model.GoodsRequestStatus.FULFILLED || it.status == com.example.ecomerse.model.GoodsRequestStatus.SETTLED) }
        return relevant.filter { req ->
            when (period) {
                "day" -> isSameDay(req.requestedAt, now)
                "week" -> isWithinLastDays(req.requestedAt, now, 7)
                "month" -> isSameMonth(req.requestedAt, now)
                else -> false
            }
        }.sumOf { it.totalAmount }
    }

    val cashDay = sumForPeriod(com.example.ecomerse.model.PaymentMethod.CASH, "day")
    val cashWeek = sumForPeriod(com.example.ecomerse.model.PaymentMethod.CASH, "week")
    val cashMonth = sumForPeriod(com.example.ecomerse.model.PaymentMethod.CASH, "month")

    val creditDay = sumForPeriod(com.example.ecomerse.model.PaymentMethod.CREDIT, "day")
    val creditWeek = sumForPeriod(com.example.ecomerse.model.PaymentMethod.CREDIT, "week")
    val creditMonth = sumForPeriod(com.example.ecomerse.model.PaymentMethod.CREDIT, "month")

    // Fallback products in case Firestore hasn't loaded yet
    val fallbackProducts = listOf(
        com.example.ecomerse.model.Product("1", "Rice", "High-quality long-grain rice, 25kg per bag", 84375.0, "bag (25kg)"),
        com.example.ecomerse.model.Product("2", "Cooking Oil", "Pure vegetable cooking oil, 5L per jerrican", 32800.0, "jerrican (5L)"),
        com.example.ecomerse.model.Product("3", "Sugar", "Granulated sugar, 50kg per bag", 142500.0, "bag (50kg)"),
        com.example.ecomerse.model.Product("4", "Laundry Detergent", "Powerful cleaning powder, 1kg per pack", 13125.0, "pack (1kg)"),
        com.example.ecomerse.model.Product("5", "Bar Soap", "Premium bath soap, box of 72 bars", 53400.0, "box (72 bars)"),
        com.example.ecomerse.model.Product("6", "Toothpaste", "Fluoride toothpaste, box of 12 tubes", 63700.0, "box (12 tubes)"),
        com.example.ecomerse.model.Product("7", "Toilet Paper", "Soft tissue rolls, pack of 10", 22500.0, "pack (10 rolls)"),
        com.example.ecomerse.model.Product("8", "Bottled Water", "Purified drinking water, crate of 24 bottles", 16875.0, "crate (24 bottles)"),
        com.example.ecomerse.model.Product("9", "Diapers", "Infant/toddler diapers, pack of 50", 69375.0, "pack (50)")
    )

    // Use actual products if available, otherwise use fallback
    val effectiveProducts = if (products.isNotEmpty()) products else fallbackProducts

    val distinctDistributors = inventory.map { it.distributorId }.distinct()
    // Fallback distributors
    val fallbackDistributors = listOf("dist1", "dist2")
    val effectiveDistributors = if (distinctDistributors.isNotEmpty()) distinctDistributors else fallbackDistributors

    var selectedProductId by rememberSaveable { mutableStateOf(effectiveProducts.firstOrNull()?.id.orEmpty()) }
    var selectedDistributorId by rememberSaveable { mutableStateOf(effectiveDistributors.firstOrNull().orEmpty()) }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var note by rememberSaveable { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(com.example.ecomerse.model.PaymentMethod.CASH) }
    var submitMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(effectiveProducts, effectiveDistributors) {
        if (selectedProductId.isBlank()) {
            selectedProductId = effectiveProducts.firstOrNull()?.id.orEmpty()
        }
        if (selectedDistributorId.isBlank()) {
            selectedDistributorId = effectiveDistributors.firstOrNull().orEmpty()
        }
    }

    val selectedProduct = effectiveProducts.find { it.id == selectedProductId }
    val eligibleDistributors = if (selectedProductId.isNotBlank()) {
        inventory.filter { it.productId == selectedProductId }.map { it.distributorId }.distinct()
            .takeIf { it.isNotEmpty() } ?: effectiveDistributors
    } else {
        effectiveDistributors
    }

    LaunchedEffect(selectedProductId, eligibleDistributors) {
        if (selectedDistributorId !in eligibleDistributors && eligibleDistributors.isNotEmpty()) {
            selectedDistributorId = eligibleDistributors.first()
        }
    }

    val availableStock = inventory.find { it.productId == selectedProductId && it.distributorId == selectedDistributorId }?.quantity ?: 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Quick Stock Access",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Welcome back, ${user?.name ?: "Customer"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CustomerMetricCard(
                    modifier = Modifier.weight(1f),
                    value = requestCountAnimated.toString(),
                    title = "Requests",
                    subtitle = "Submitted"
                )
                CustomerMetricCard(
                    modifier = Modifier.weight(1f),
                    value = completedRequests.toString(),
                    title = "Settled",
                    subtitle = "Closed"
                )
                CustomerMetricCard(
                    modifier = Modifier.weight(1f),
                    value = String.format(Locale.getDefault(), "%.0f", balanceAnimated),
                    title = "Credit",
                    subtitle = "Outstanding"
                )
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Purchase totals", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Cash (UGX)", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SmallMetric(modifier = Modifier.weight(1f), label = "Day", value = String.format(Locale.getDefault(), "%.0f", cashDay))
                        SmallMetric(modifier = Modifier.weight(1f), label = "Week", value = String.format(Locale.getDefault(), "%.0f", cashWeek))
                        SmallMetric(modifier = Modifier.weight(1f), label = "Month", value = String.format(Locale.getDefault(), "%.0f", cashMonth))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Credit (UGX)", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SmallMetric(modifier = Modifier.weight(1f), label = "Day", value = String.format(Locale.getDefault(), "%.0f", creditDay))
                        SmallMetric(modifier = Modifier.weight(1f), label = "Week", value = String.format(Locale.getDefault(), "%.0f", creditWeek))
                        SmallMetric(modifier = Modifier.weight(1f), label = "Month", value = String.format(Locale.getDefault(), "%.0f", creditMonth))
                    }
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Goods Request", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    SelectionDropdown(
                        title = "Product",
                        options = effectiveProducts.map { it.id to "${it.name} (${it.unit})" },
                        selectedId = selectedProductId,
                        onSelect = { selectedProductId = it }
                    )

                    SelectionDropdown(
                        title = "Distributor",
                        options = eligibleDistributors.map { it to when (it) {
                            "dist1" -> "North Hub"
                            "dist2" -> "Metro Hub"
                            else -> it
                        } },
                        selectedId = selectedDistributorId,
                        onSelect = { selectedDistributorId = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it.filter { char -> char.isDigit() }.take(4) },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text("Stock", style = MaterialTheme.typography.labelSmall)
                                Text(availableStock.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    PaymentModeSelector(
                        paymentMethod = paymentMethod,
                        onPaymentMethodChange = { paymentMethod = it }
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    if (selectedProduct != null) {
                        val qty = quantityText.toIntOrNull() ?: 0
                        val requestTotal = qty * selectedProduct.unitPrice
                        Text(
                            text = "Estimated total: UGX ${String.format(Locale.getDefault(), "%.0f", requestTotal)} (${qty} x ${selectedProduct.unit})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (submitMessage != null) {
                        AssistChip(onClick = { submitMessage = null }, label = { Text(submitMessage ?: "") })
                    }

                    Button(
                        onClick = {
                            val qty = quantityText.toIntOrNull() ?: 0
                            val success = AppRepository.submitGoodsRequest(
                                customerId = user?.id.orEmpty(),
                                customerName = user?.name.orEmpty(),
                                distributorId = selectedDistributorId,
                                productId = selectedProductId,
                                quantity = qty,
                                paymentMethod = paymentMethod,
                                note = note
                            )
                            submitMessage = if (success) {
                                note = ""
                                quantityText = "1"
                                "Request submitted successfully"
                            } else {
                                "Please choose a product, distributor, and quantity"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedProductId.isNotBlank() && selectedDistributorId.isNotBlank()
                    ) {
                        Text("Submit Request")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRequestsTabContent(
    modifier: Modifier = Modifier,
    user: com.example.ecomerse.model.User?,
    requests: List<com.example.ecomerse.model.GoodsRequest>,
    products: List<com.example.ecomerse.model.Product>
) {
    val customerRequests = remember(requests, user) { 
        requests.filter { it.customerId == user?.id } 
    }
    val pendingOnlyRequests = remember(customerRequests) {
        customerRequests.filter { it.status == com.example.ecomerse.model.GoodsRequestStatus.PENDING }
    }
    val approvedRequests = remember(customerRequests) {
        customerRequests.filter { it.status == com.example.ecomerse.model.GoodsRequestStatus.APPROVED }
    }
    val totalSpend = remember(customerRequests) { 
        customerRequests.sumOf { it.totalAmount } 
    }
    val settledSpend = remember(customerRequests) { 
        customerRequests.filter { it.status == com.example.ecomerse.model.GoodsRequestStatus.SETTLED }.sumOf { it.totalAmount } 
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Request Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Pending review: ${pendingOnlyRequests.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Approved: ${approvedRequests.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total spend: UGX ${String.format(Locale.getDefault(), "%.0f", totalSpend)} • Settled: UGX ${String.format(Locale.getDefault(), "%.0f", settledSpend)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (customerRequests.isEmpty()) {
                        Text("Your request history will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

         if (pendingOnlyRequests.isNotEmpty()) {
             item {
                 Text("Pending Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
             }
             items(pendingOnlyRequests.sortedByDescending { it.requestedAt }) { request ->
                 val productName = products.find { it.id == request.productId }?.name ?: request.productId
                 val productUnit = products.find { it.id == request.productId }?.unit ?: "unit"
                 val statusColor = when (request.status) {
                     com.example.ecomerse.model.GoodsRequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                     com.example.ecomerse.model.GoodsRequestStatus.APPROVED -> MaterialTheme.colorScheme.primary
                     com.example.ecomerse.model.GoodsRequestStatus.FULFILLED -> Color(0xFF6A1B9A)
                     com.example.ecomerse.model.GoodsRequestStatus.SETTLED -> Color(0xFF2E7D32)
                     com.example.ecomerse.model.GoodsRequestStatus.REJECTED -> MaterialTheme.colorScheme.error
                 }

                 ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                     Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(productName, style = MaterialTheme.typography.titleMedium)
                                 Text(
                                     text = "${request.quantity} x ${productUnit} • ${displayDistributorName(request.distributorId)}",
                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     style = MaterialTheme.typography.bodySmall
                                 )
                             }
                             Column(horizontalAlignment = Alignment.End) {
                                 Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                     Text(
                                         text = request.status.name.replace("_", " "),
                                         color = statusColor,
                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                         style = MaterialTheme.typography.labelSmall
                                     )
                                 }
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                                     Text(
                                         text = request.paymentMethod.name,
                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                         style = MaterialTheme.typography.labelSmall
                                     )
                                 }
                             }
                         }

                         Text(
                             text = "Amount: UGX ${String.format(Locale.getDefault(), "%.0f", request.totalAmount)}",
                             style = MaterialTheme.typography.bodyMedium
                         )

                         request.note.takeIf { it.isNotBlank() }?.let {
                             Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                         }

                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                             Text(
                                 text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(request.requestedAt)),
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         }
                     }
                 }
             }
         }

         if (approvedRequests.isNotEmpty()) {
             item {
                 Text("Approved by Distributor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
             }
             items(approvedRequests.sortedByDescending { it.requestedAt }) { request ->
                 val productName = products.find { it.id == request.productId }?.name ?: request.productId
                 val productUnit = products.find { it.id == request.productId }?.unit ?: "unit"
                 val statusColor = when (request.status) {
                     com.example.ecomerse.model.GoodsRequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                     com.example.ecomerse.model.GoodsRequestStatus.APPROVED -> MaterialTheme.colorScheme.primary
                     com.example.ecomerse.model.GoodsRequestStatus.FULFILLED -> Color(0xFF6A1B9A)
                     com.example.ecomerse.model.GoodsRequestStatus.SETTLED -> Color(0xFF2E7D32)
                     com.example.ecomerse.model.GoodsRequestStatus.REJECTED -> MaterialTheme.colorScheme.error
                 }

                 ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                     Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(productName, style = MaterialTheme.typography.titleMedium)
                                 Text(
                                     text = "${request.quantity} x ${productUnit} • ${displayDistributorName(request.distributorId)}",
                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     style = MaterialTheme.typography.bodySmall
                                 )
                             }
                             Column(horizontalAlignment = Alignment.End) {
                                 Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                     Text(
                                         text = request.status.name.replace("_", " "),
                                         color = statusColor,
                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                         style = MaterialTheme.typography.labelSmall
                                     )
                                 }
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                                     Text(
                                         text = request.paymentMethod.name,
                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                         style = MaterialTheme.typography.labelSmall
                                     )
                                 }
                             }
                         }

                         Text(
                             text = "Amount: UGX ${String.format(Locale.getDefault(), "%.0f", request.totalAmount)}",
                             style = MaterialTheme.typography.bodyMedium
                         )

                         request.note.takeIf { it.isNotBlank() }?.let {
                             Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                         }

                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                             Text(
                                 text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(request.requestedAt)),
                                 style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )

                             if (request.paymentMethod == com.example.ecomerse.model.PaymentMethod.CREDIT && request.status == com.example.ecomerse.model.GoodsRequestStatus.FULFILLED) {
                                 TextButton(onClick = { AppRepository.settleCreditRequest(request.id, user?.id.orEmpty()) }) {
                                     Text("Settle now")
                                 }
                             }
                         }
                     }
                 }
             }
         }
     }
 }

@Composable
private fun DistributorInventoryTabContent(
    modifier: Modifier = Modifier,
    distributorId: String?,
    selectedHub: String = "dist1",
    onHubChange: (String) -> Unit = {},
    inventoryCount: Int,
    totalUnits: Int,
    weeklyUnits: Int,
    lowStockCount: Int,
    distInventory: List<com.example.ecomerse.model.InventoryItem>,
    products: List<com.example.ecomerse.model.Product>,
    onAddProductClick: () -> Unit,
    onRestockClick: (String) -> Unit
) {
    val animatedUnits by animateIntAsState(totalUnits, label = "total-units")
    val animatedWeeklyUnits by animateIntAsState(weeklyUnits, label = "weekly-units")
    val animatedLowStock by animateIntAsState(lowStockCount, label = "low-stock")
    val maxQuantity = (distInventory.maxOfOrNull { it.quantity }?.coerceAtLeast(1) ?: 1).toFloat()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Inventory Hub", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(when (selectedHub) {
                        "dist1" -> "North Hub"
                        "dist2" -> "Metro Hub"
                        else -> "Distributor: ${distributorId ?: "N/A"}"
                    }, style = MaterialTheme.typography.headlineSmall)
                    Text("Live stock visibility and quick actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Hub Switcher
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onHubChange("dist1") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedHub == "dist1") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("North Hub")
                        }
                        Button(
                            onClick = { onHubChange("dist2") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedHub == "dist2") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("Metro Hub")
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = inventoryCount.toString(),
                    title = "Products",
                    subtitle = "Tracked"
                )
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = animatedUnits.toString(),
                    title = "Units",
                    subtitle = "On hand"
                )
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = animatedLowStock.toString(),
                    title = "Alerts",
                    subtitle = "Low stock"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddProductClick, modifier = Modifier.weight(1f)) {
                    Text("Add Product")
                }
                OutlinedButton(
                    onClick = {
                        distInventory.firstOrNull()?.let { onRestockClick(it.productId) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = distInventory.isNotEmpty()
                ) {
                    Text("Quick Restock")
                }
            }
        }

        item {
            Text(
                text = "${animatedWeeklyUnits} units moved in the last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (distInventory.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No products are tracked yet. Add items from the catalog to start monitoring stock.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(distInventory, key = { it.productId }) { item ->
                val product = products.find { it.id == item.productId }
                val stockProgress by animateFloatAsState(
                    targetValue = (item.quantity / maxQuantity).coerceIn(0f, 1f),
                    label = "stock-progress-${item.productId}"
                )
                val stockColor by animateColorAsState(
                    targetValue = if (item.quantity < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    label = "stock-color-${item.productId}"
                )

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = stockColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                                    Text((product?.name ?: "?").take(1).uppercase(), color = stockColor, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                                Text(product?.name ?: "Unknown Product", style = MaterialTheme.typography.titleMedium)
                                                Text("${product?.unit ?: "units"} • Stock: ${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onRestockClick(item.productId) }) { Text("Restock") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { stockProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = stockColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        AnimatedVisibility(visible = item.quantity < 10) {
                            Text(
                                text = "Low stock - replenish soon",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributorRequestsTabContent(
    modifier: Modifier = Modifier,
    distributorId: String?,
    requestsCount: Int,
    requests: List<com.example.ecomerse.model.GoodsRequest>,
    products: List<com.example.ecomerse.model.Product>
) {
    val currentUser by SessionManager.currentUser.collectAsState()
    val currentUserId = currentUser?.id ?: ""

    val pendingRequests = requests.count { it.status == com.example.ecomerse.model.GoodsRequestStatus.PENDING }
    val cashRequests = requests.count { it.paymentMethod == com.example.ecomerse.model.PaymentMethod.CASH }
    val creditRequests = requests.count { it.paymentMethod == com.example.ecomerse.model.PaymentMethod.CREDIT }
    val totalUnits = requests.sumOf { it.quantity }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Customer Requests", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cash and credit request queue", style = MaterialTheme.typography.headlineSmall)
                    Text("Showing ${requests.size} requests for ${distributorId ?: "this hub"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = requestsCount.toString(),
                    title = "Requests",
                    subtitle = "Total"
                )
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = pendingRequests.toString(),
                    title = "Pending",
                    subtitle = "Needs action"
                )
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = totalUnits.toString(),
                    title = "Units",
                    subtitle = "Requested"
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CustomerMetricCard(
                    modifier = Modifier.weight(1f),
                    value = cashRequests.toString(),
                    title = "Cash",
                    subtitle = "Requests"
                )
                CustomerMetricCard(
                    modifier = Modifier.weight(1f),
                    value = creditRequests.toString(),
                    title = "Credit",
                    subtitle = "Requests"
                )
            }
        }

        if (requests.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No customer requests yet. New cash or credit requests will appear here in real time.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(requests.reversed(), key = { it.id }) { request ->
                val productName = products.find { it.id == request.productId }?.name ?: "Product"
                val productUnit = products.find { it.id == request.productId }?.unit ?: "unit"
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(productName, style = MaterialTheme.typography.titleMedium)
                                Text("${request.quantity}x ${productUnit} | ${request.customerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = SimpleDateFormat("MMM dd\nHH:mm", Locale.getDefault()).format(Date(request.requestedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text("Amount: UGX ${String.format(Locale.getDefault(), "%.0f", request.totalAmount)} • ${request.paymentMethod.name}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (request.status == com.example.ecomerse.model.GoodsRequestStatus.PENDING) {
                                TextButton(onClick = { AppRepository.rejectGoodsRequest(request.id, currentUserId) }) { Text("Reject") }
                                Button(onClick = { AppRepository.approveGoodsRequest(request.id, currentUserId) }) { Text("Approve") }
                            }
                            if (request.status != com.example.ecomerse.model.GoodsRequestStatus.REJECTED && request.status != com.example.ecomerse.model.GoodsRequestStatus.SETTLED) {
                                OutlinedButton(onClick = { AppRepository.fulfillGoodsRequest(request.id, currentUserId) }) {
                                    Text(if (request.paymentMethod == com.example.ecomerse.model.PaymentMethod.CREDIT) "Fulfill credit" else "Mark paid")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributorAnalyticsTabContent(
    modifier: Modifier = Modifier,
    distributorId: String?,
    inventory: List<com.example.ecomerse.model.InventoryItem>,
    sales: List<com.example.ecomerse.model.Sale>,
    products: List<com.example.ecomerse.model.Product>
) {
    val distInventory = remember(inventory, distributorId) { 
        try {
            if (distributorId != null) {
                inventory.filter { it.distributorId == distributorId }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    val distSales = remember(sales, distributorId) { 
        try {
            if (distributorId != null) {
                sales.filter { it.distributorId == distributorId }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    val unitsSold = remember(distSales) { 
        try { distSales.sumOf { it.quantity } } catch (e: Exception) { 0 }
    }
    val lowStock = remember(distInventory) { 
        try { distInventory.count { it.quantity < 10 } } catch (e: Exception) { 0 }
    }

    val topProducts = remember(distSales) {
        try {
            distSales
                .groupBy { it.productId }
                .mapValues { (_, records) -> records.sumOf { it.quantity } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val maxTopUnits = remember(topProducts) {
        (topProducts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1).toFloat()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Orders & Performance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Distributor Snapshot", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = distInventory.size.toString(),
                    title = "Products",
                    subtitle = "In inventory"
                )
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = lowStock.toString(),
                    title = "Low Stock",
                    subtitle = "Need action"
                )
                DistributorMetricCard(
                    modifier = Modifier.weight(1f),
                    value = unitsSold.toString(),
                    title = "Units",
                    subtitle = "Sold"
                )
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Top Moving Products", style = MaterialTheme.typography.titleMedium)
                    if (topProducts.isEmpty()) {
                        Text("No orders recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        topProducts.forEach { (productId, qty) ->
                            val progress by animateFloatAsState(
                                targetValue = (qty / maxTopUnits).coerceIn(0f, 1f),
                                label = "top-product-$productId"
                            )
                            val productName = products.find { it.id == productId }?.name ?: productId

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(productName, style = MaterialTheme.typography.bodyMedium)
                                    Text("$qty units", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributorMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    title: String,
    subtitle: String
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CustomerMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    title: String,
    subtitle: String
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmallMetric(modifier: Modifier = Modifier, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp, modifier = Modifier.padding(top = 6.dp)) {
            Text("UGX $value", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PaymentModeSelector(
    paymentMethod: com.example.ecomerse.model.PaymentMethod,
    onPaymentMethodChange: (com.example.ecomerse.model.PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Payment method", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = paymentMethod == com.example.ecomerse.model.PaymentMethod.CASH,
                onClick = { onPaymentMethodChange(com.example.ecomerse.model.PaymentMethod.CASH) },
                label = { Text("Cash") }
            )
            FilterChip(
                selected = paymentMethod == com.example.ecomerse.model.PaymentMethod.CREDIT,
                onClick = { onPaymentMethodChange(com.example.ecomerse.model.PaymentMethod.CREDIT) },
                label = { Text("Credit") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdown(
    title: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hasOptions = options.isNotEmpty()
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: "Choose one"

    LaunchedEffect(options) {
        if (!hasOptions) expanded = false
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { isExpanded ->
            if (hasOptions) expanded = isExpanded
        }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(title) },
            enabled = hasOptions,
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded && hasOptions,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun displayDistributorName(distributorId: String): String = when (distributorId) {
    "dist1" -> "North Hub"
    "dist2" -> "Metro Hub"
    else -> distributorId
}

@Composable
private fun DistributorPlaceholderTab(modifier: Modifier = Modifier, title: String, subtitle: String) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RestockDialog(productId: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var quantityText by remember { mutableStateOf("50") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Stock - $productId") },
        text = {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(quantityText.toIntOrNull() ?: 0) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DashboardScreen() {
    val sales by AppRepository.sales.collectAsState()
    val logs by AppRepository.activityLogs.collectAsState()
    val products by AppRepository.products.collectAsState()
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(
            chatRepository = ChatRepositoryProvider.repository,
            sessionManager = SessionManager
        )
    )
    var chatThreadId by rememberSaveable { mutableStateOf<String?>(null) }

    val totalRevenue = sales.sumOf { sale -> 
        val product = products.find { it.id == sale.productId }
        (product?.unitPrice ?: 0.0) * sale.quantity
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Performance") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Audit Logs") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Chat") })
        }

        when (selectedTab) {
            0 -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                         Column(modifier = Modifier.padding(24.dp)) {
                             Text("Real-time Revenue", style = MaterialTheme.typography.labelLarge)
                             Text(
                                 text = "UGX ${String.format(Locale.getDefault(), "%.0f", totalRevenue)}",
                                 style = MaterialTheme.typography.headlineLarge
                             )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Total Sales: ${sales.size}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Live Sales Feed", style = MaterialTheme.typography.titleLarge)
                    LazyColumn {
                        items(sales.reversed()) { sale ->
                            val productName = products.find { it.id == sale.productId }?.name ?: "Product"
                            ListItem(
                                headlineContent = { Text("$productName (x${sale.quantity})") },
                                supportingContent = { Text("Handled by: ${sale.handledByUserId} | Dist: ${sale.distributorId}") },
                                trailingContent = { Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sale.timestamp))) }
                            )
                        }
                    }
                }
            }
            1 -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs.reversed()) { log ->
                        ListItem(
                            headlineContent = { Text(log.action) },
                            supportingContent = { Text("UserID: ${log.userId}") },
                            trailingContent = { Text(SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(log.timestamp))) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            2 -> RoleChatHost(
                modifier = Modifier.fillMaxSize(),
                chatViewModel = chatViewModel,
                activeThreadId = chatThreadId,
                onThreadChange = { chatThreadId = it }
            )
        }
    }
}

@Composable
fun RoleChatHost(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel,
    activeThreadId: String?,
    onThreadChange: (String?) -> Unit
) {
    if (activeThreadId == null) {
        ChatThreadListScreen(
            viewModel = chatViewModel,
            onThreadClick = { onThreadChange(it) },
            modifier = modifier
        )
    } else {
        ChatConversationScreen(
            threadId = activeThreadId,
            viewModel = chatViewModel,
            onBack = { onThreadChange(null) },
            modifier = modifier
        )
    }
}
