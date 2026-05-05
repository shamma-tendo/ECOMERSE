package com.example.ecomerse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.ecomerse.data.ChatRepositoryProvider
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.*
import com.example.ecomerse.ui.chat.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CompanyManagerDashboard(
    viewModel: ManagerViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(
            chatRepository = ChatRepositoryProvider.repository,
            sessionManager = SessionManager
        )
    )
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    var chatThreadId by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (chatThreadId != null) {
            chatThreadId = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            if (System.currentTimeMillis() - backPressedTime < 2000) {
                SessionManager.logout()
            } else {
                Toast.makeText(context, "Tap again to exit", Toast.LENGTH_SHORT).show()
                backPressedTime = System.currentTimeMillis()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, text = { Text("Home") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.People, null) }, text = { Text("Staff") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Task, null) }, text = { Text("Approvals") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Assessment, null) }, text = { Text("Reports") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) }, text = { Text("Chat") })
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ManagerHomeScreen(uiState, onNavigate = { selectedTab = it })
                1 -> EmployeeManagementScreen(uiState, viewModel)
                2 -> ApprovalsScreen(uiState, viewModel)
                3 -> DistributorSalesPerformanceScreen(uiState)
                4 -> RoleChatHost(
                    modifier = Modifier.fillMaxSize(),
                    chatViewModel = chatViewModel,
                    activeThreadId = chatThreadId,
                    onThreadChange = { chatThreadId = it }
                )
            }
        }
    }
}

@Composable
fun ManagerHomeScreen(state: ManagerUiState, onNavigate: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Company Overview", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardCard(
                title = "Employees", 
                value = state.employees.size.toString(), 
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(1) }
            )
            DashboardCard(
                title = "Pending Leaves", 
                value = state.leaveRequests.count { it.status == LeaveStatus.PENDING }.toString(), 
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(2) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val totalSales = state.salesReports.sumOf { it.totalSalesCount }
        DashboardCard(
            title = "Overall Sales Report", 
            value = "$totalSales Units", 
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(3) }
        )
    }
}

@Composable
fun DistributorSalesPerformanceScreen(state: ManagerUiState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Distributor Performance", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.salesReports) { report ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(report.distributorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Distributor: ${report.distributorId}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Sold", style = MaterialTheme.typography.labelSmall)
                                Text("${report.totalSalesCount}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Revenue", style = MaterialTheme.typography.labelSmall)
                                val revenueStr = String.format(Locale.getDefault(), "%.0f", report.totalRevenue)
                                Text("UGX $revenueStr", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50))
                            }
                        }
                        
                        if (report.lastSaleTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(report.lastSaleTime))
                            Text(
                                text = "Last Sale: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmployeeManagementScreen(state: ManagerUiState, viewModel: ManagerViewModel) {
    val showDismissalDialog = remember { mutableStateOf<User?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Staff Management", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(state.employees) { employee ->
                ListItem(
                    headlineContent = { Text(employee.name) },
                    supportingContent = { Text("${employee.position} | ${employee.status}") },
                    trailingContent = {
                        if (employee.status == EmployeeStatus.ACTIVE) {
                            IconButton(onClick = { showDismissalDialog.value = employee }) {
                                Icon(Icons.Default.PersonOff, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDismissalDialog.value != null) {
        DismissalDialog(
            employee = showDismissalDialog.value!!,
            draft = state.dismissalDraft,
            onDraftChange = { viewModel.updateDismissalDraft(it) },
            onDismiss = { showDismissalDialog.value = null },
            onConfirm = {
                viewModel.terminateEmployee(showDismissalDialog.value!!.id)
                showDismissalDialog.value = null
            }
        )
    }
}

@Composable
fun ApprovalsScreen(state: ManagerUiState, viewModel: ManagerViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Pending Approvals", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(state.leaveRequests.filter { it.status == LeaveStatus.PENDING }) { request ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(request.employeeName, fontWeight = FontWeight.Bold)
                        Text("${request.startDate} to ${request.endDate}")
                        Text("Reason: ${request.reason}")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.rejectLeave(request.id) }) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = { viewModel.approveLeave(request.id) }) {
                                Text("Approve")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DismissalDialog(
    employee: User,
    draft: String,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Dismissal Letter for ${employee.name}") },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Dismissal Reason/Letter Content") }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Confirm Dismissal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
