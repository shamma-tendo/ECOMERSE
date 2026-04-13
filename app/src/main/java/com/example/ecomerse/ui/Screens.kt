package com.example.ecomerse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.UserRole
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ecomerse Login", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Select a role to enter the demo:", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        UserRole.entries.forEach { role ->
            Button(
                onClick = { SessionManager.login(role) },
                modifier = Modifier.fillMaxWidth(0.8f).padding(8.dp)
            ) {
                Text(role.name.replace("_", " "))
            }
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
    var showRestockDialog by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Warehouse Status: ${user?.distributorId}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(distInventory) { item ->
                val product = products.find { it.id == item.productId }
                ListItem(
                    headlineContent = { Text(product?.name ?: "Unknown Product") },
                    supportingContent = { Text("${item.quantity} units in stock") },
                    trailingContent = {
                        TextButton(onClick = { 
                            selectedProductId = item.productId
                            showRestockDialog = true
                        }) {
                            Text("Restock")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showRestockDialog && selectedProductId != null) {
        RestockDialog(
            productId = selectedProductId!!,
            onDismiss = { showRestockDialog = false },
            onConfirm = { qty ->
                AppRepository.updateStock(selectedProductId!!, user?.distributorId ?: "", qty, user?.id ?: "")
                showRestockDialog = false
            }
        )
    }
}

@Composable
fun RestockDialog(productId: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var quantityText by remember { mutableStateOf("50") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock Product $productId") },
        text = {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Quantity to add") },
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
                            Text("$${String.format("%.2f", totalRevenue)}", style = MaterialTheme.typography.headlineLarge)
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
