package com.example.ecomerse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.*
import kotlinx.coroutines.flow.*

data class ManagerUiState(
    val users: List<User> = emptyList(),
    val leaveRequests: List<LeaveRequest> = listOf(
        LeaveRequest("L1", "agent_001", "Alice Smith", "2023-12-01", "2023-12-05", "Vacation"),
        LeaveRequest("L2", "agent_002", "Charlie Brown", "2023-12-10", "2023-12-11", "Medical")
    ),
    val dismissalDraft: String = "",
    val salesReports: List<DistributorSalesReport> = emptyList()
)

data class DistributorSalesReport(
    val distributorId: String,
    val distributorName: String,
    val totalSalesCount: Int,
    val totalRevenue: Double,
    val lastSaleTime: Long?
)

class ManagerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState: StateFlow<ManagerUiState> = _uiState.asStateFlow()

    init {
        observeSalesData()
        loadUsers()
    }

    private fun loadUsers() {
        _uiState.update { it.copy(users = SessionManager.getAllUsers()) }
    }

    private fun observeSalesData() {
        AppRepository.sales.combine(AppRepository.products) { sales, products ->
            sales
                .groupBy { it.distributorId }
                .map { (distributorId, distributorSales) ->
                    val revenue = distributorSales.sumOf { sale ->
                        val product = products.find { it.id == sale.productId }
                        (product?.unitPrice ?: 0.0) * sale.quantity
                    }

                    val distributorName = when (distributorId) {
                        "dist1" -> "North Hub"
                        "dist2" -> "Metro Hub"
                        else -> distributorId
                    }

                    DistributorSalesReport(
                        distributorId = distributorId,
                        distributorName = distributorName,
                        totalSalesCount = distributorSales.sumOf { it.quantity },
                        totalRevenue = revenue,
                        lastSaleTime = distributorSales.maxByOrNull { it.timestamp }?.timestamp
                    )
                }
        }.onEach { reports ->
            _uiState.update { it.copy(salesReports = reports) }
        }.launchIn(viewModelScope)
    }

    fun approveLeave(requestId: String) {
        _uiState.update { state ->
            state.copy(leaveRequests = state.leaveRequests.map {
                if (it.id == requestId) it.copy(status = LeaveStatus.APPROVED) else it
            })
        }
        AppRepository.logActivity("MANAGER", "Approved leave request $requestId")
    }

    fun rejectLeave(requestId: String) {
        _uiState.update { state ->
            state.copy(leaveRequests = state.leaveRequests.map {
                if (it.id == requestId) it.copy(status = LeaveStatus.REJECTED) else it
            })
        }
        AppRepository.logActivity("MANAGER", "Rejected leave request $requestId")
    }

    fun updateDismissalDraft(text: String) {
        _uiState.update { it.copy(dismissalDraft = text) }
    }

    fun terminateEmployee(employeeId: String) {
        SessionManager.updateUserRole(employeeId, UserRole.CUSTOMER) // Just for demo, maybe role change?
        // Actually the user wants to change roles.
        loadUsers()
        AppRepository.logActivity("MANAGER", "Updated user $employeeId status")
    }

    fun changeUserRole(userId: String, newRole: UserRole) {
        SessionManager.updateUserRole(userId, newRole)
        loadUsers()
    }

    fun changeUserDistributorId(userId: String, newHubId: String?) {
        SessionManager.updateUserDistributorId(userId, newHubId)
        loadUsers()
    }
}
