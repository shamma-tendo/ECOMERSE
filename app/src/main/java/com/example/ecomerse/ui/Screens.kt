package com.example.ecomerse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.UserRole
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Sign up logic */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("SIGN UP", fontWeight = FontWeight.Bold)
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

        Button(
            onClick = { 
                // For demo purposes, logging in as Company Manager
                SessionManager.login(UserRole.COMPANY_MANAGER)
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
fun SalesAgentScreen() {
    val products by AppRepository.products.collectAsState()
    val user by SessionManager.currentUser.collectAsState()
    val inventory by AppRepository.inventory.collectAsState()
    
    val distId = user?.distributorId ?: ""
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Agent: ${user?.name}", style = MaterialTheme.typography.titleMedium)
        Text("Distributor: $distId", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Available Inventory", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(products) { product ->
                val stock = inventory.find { it.productId == product.id && it.distributorId == distId }?.quantity ?: 0
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium)
                            Text("Stock: $stock", color = if (stock < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { AppRepository.recordSale(product.id, distId, user?.id ?: "", 1) },
                            enabled = stock > 0
                        ) {
                            Text("Record Sale")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DistributorInventoryScreen() {
    val user by SessionManager.currentUser.collectAsState()
    val inventory by AppRepository.inventory.collectAsState()
    val products by AppRepository.products.collectAsState()
    
    val distInventory = inventory.filter { it.distributorId == user?.distributorId }
    val showRestockDialog = remember { mutableStateOf(false) }
    val selectedProductId = remember { mutableStateOf<String?>(null) }
    val showCatalog = remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Inventory: ${user?.distributorId}", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showCatalog.value = true }) { Text("Add Product") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(distInventory) { item ->
                val product = products.find { it.id == item.productId }
                ListItem(
                    headlineContent = { Text(product?.name ?: "Unknown") },
                    supportingContent = { Text("Stock: ${item.quantity}") },
                    trailingContent = {
                        TextButton(onClick = { 
                            selectedProductId.value = item.productId
                            showRestockDialog.value = true
                        }) { Text("Restock") }
                    }
                )
                HorizontalDivider()
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

    if (showRestockDialog.value && selectedProductId.value != null) {
        RestockDialog(
            productId = selectedProductId.value!!,
            onDismiss = { showRestockDialog.value = false },
            onConfirm = { qty ->
                AppRepository.updateStock(selectedProductId.value!!, user?.distributorId ?: "", qty, user?.id ?: "")
                showRestockDialog.value = false
            }
        )
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
    
    val totalRevenue = sales.sumOf { sale -> 
        val product = products.find { it.id == sale.productId }
        (product?.unitPrice ?: 0.0) * sale.quantity
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Performance") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Audit Logs") })
        }

        when (selectedTab) {
            0 -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Real-time Revenue", style = MaterialTheme.typography.labelLarge)
                            val revenueFormatted = String.format(Locale.getDefault(), "%.2f", totalRevenue)
                            Text(
                                text = "{'$'}$revenueFormatted",
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
                                supportingContent = { Text("Agent: ${sale.agentId} | Dist: ${sale.distributorId}") },
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
        }
    }
}
