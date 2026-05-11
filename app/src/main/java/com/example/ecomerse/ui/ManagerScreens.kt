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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var selectedTab by rememberSaveable { mutableStateOf(ManagerBottomTab.HOME) }
    val uiState by viewModel.uiState.collectAsState()
    var chatThreadId by rememberSaveable { mutableStateOf<String?>(null) }

    // Navigation logic: Back button returns to Home or clears active chat thread
    if (chatThreadId != null) {
        BackHandler { chatThreadId = null }
    } else if (selectedTab != ManagerBottomTab.HOME) {
        BackHandler { selectedTab = ManagerBottomTab.HOME }
    }

    Scaffold(
        bottomBar = {
            if (selectedTab != ManagerBottomTab.CHAT || chatThreadId == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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
                            ManagerBottomTab.values().forEach { tab ->
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                ManagerBottomTab.HOME -> ManagerHomeScreen(uiState, onNavigate = { selectedTab = it })
                ManagerBottomTab.STAFF -> EmployeeManagementScreen(uiState, viewModel)
                ManagerBottomTab.APPROVALS -> ApprovalsScreen(uiState, viewModel)
                ManagerBottomTab.REPORTS -> DistributorSalesPerformanceScreen(uiState)
                ManagerBottomTab.CHAT -> RoleChatHost(
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
private fun ManagerHomeScreen(state: ManagerUiState, onNavigate: (ManagerBottomTab) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Company Overview", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardCard(
                title = "Total Users", 
                value = state.users.size.toString(), 
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ManagerBottomTab.STAFF) }
            )
            DashboardCard(
                title = "Pending Leaves", 
                value = state.leaveRequests.count { it.status == LeaveStatus.PENDING }.toString(), 
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ManagerBottomTab.APPROVALS) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val totalSales = state.salesReports.sumOf { it.totalSalesCount }
        DashboardCard(
            title = "Overall Sales Report", 
            value = "$totalSales Units", 
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(ManagerBottomTab.REPORTS) }
        )
    }
}

enum class ManagerBottomTab(val title: String) {
    HOME("Home"),
    STAFF("Staff"),
    APPROVALS("Approvals"),
    REPORTS("Reports"),
    CHAT("Chat");

    val icon: androidx.compose.ui.graphics.vector.ImageVector
        get() = when (this) {
            HOME -> Icons.Default.Home
            STAFF -> Icons.Default.People
            APPROVALS -> Icons.Default.Task
            REPORTS -> Icons.Default.Assessment
            CHAT -> Icons.AutoMirrored.Filled.Chat
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
                                Text("UGX ${revenueStr}", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Customer Orders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricChip(label = "Placed", value = report.totalRequestCount.toString(), modifier = Modifier.weight(1f))
                            MetricChip(label = "Pending", value = report.pendingRequestCount.toString(), modifier = Modifier.weight(1f))
                            MetricChip(label = "Approved", value = report.approvedRequestCount.toString(), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricChip(label = "Fulfilled", value = report.fulfilledRequestCount.toString(), modifier = Modifier.weight(1f))
                            MetricChip(label = "Settled", value = report.settledRequestCount.toString(), modifier = Modifier.weight(1f))
                            MetricChip(label = "Units", value = report.totalRequestedUnits.toString(), modifier = Modifier.weight(1f))
                        }

                        if (report.lastSaleTime != null || report.lastRequestTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val saleText = report.lastSaleTime?.let {
                                "Last Sale: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))}"
                            }
                            val requestText = report.lastRequestTime?.let {
                                "Last Order: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))}"
                            }
                            Text(
                                text = listOfNotNull(saleText, requestText).joinToString(" • "),
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
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Column(modifier = Modifier.padding(16.dp)) {
        Text("User Management", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.users) { user ->
                UserRoleCard(
                    user = user, 
                    onRoleChange = { newRole: UserRole ->
                        viewModel.changeUserRole(user.id, newRole)
                    },
                    onDistributorIdChange = { newHubId: String? ->
                        viewModel.changeUserDistributorId(user.id, newHubId)
                    }
                )
            }
        }
    }
}

@Composable
fun UserRoleCard(user: User, onRoleChange: (UserRole) -> Unit, onDistributorIdChange: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var hubExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Box {
                        AssistChip(
                            onClick = { expanded = true },
                            label = { Text(user.role.name) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            UserRole.entries.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.name) },
                                    onClick = {
                                        onRoleChange(role)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (user.role == UserRole.DISTRIBUTOR) {
                        Box {
                            TextButton(onClick = { hubExpanded = true }) {
                                val currentHub = user.distributorId
                                Text(
                                    text = if (currentHub.isNullOrBlank()) "Assign Hub" 
                                           else displayDistributorName(currentHub),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            
                            DropdownMenu(expanded = hubExpanded, onDismissRequest = { hubExpanded = false }) {
                                listOf("dist1" to "North Hub", "dist2" to "Metro Hub").forEach { pair ->
                                    val id = pair.first
                                    val name = pair.second
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onDistributorIdChange(id)
                                            hubExpanded = false
                                        }
                                    )
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

private fun displayDistributorName(distributorId: String): String = when (distributorId) {
    "dist1" -> "North Hub"
    "dist2" -> "Metro Hub"
    else -> distributorId
}
