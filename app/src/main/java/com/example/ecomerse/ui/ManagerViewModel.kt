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
    val lastSaleTime: Long?,
    val totalRequestCount: Int = 0,
    val pendingRequestCount: Int = 0,
    val approvedRequestCount: Int = 0,
    val fulfilledRequestCount: Int = 0,
    val settledRequestCount: Int = 0,
    val totalRequestedUnits: Int = 0,
    val lastRequestTime: Long? = null
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
        combine(AppRepository.sales, AppRepository.goodsRequests, AppRepository.products) { sales, requests, products ->
            val distributorIds = (sales.map { it.distributorId } + requests.map { it.distributorId })
                .filter { it.isNotBlank() }
                .distinct()

            distributorIds.map { distributorId ->
                val distributorSales = sales.filter { it.distributorId == distributorId }
                val distributorRequests = requests.filter { it.distributorId == distributorId }

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
                    lastSaleTime = distributorSales.maxByOrNull { it.timestamp }?.timestamp,
                    totalRequestCount = distributorRequests.size,
                    pendingRequestCount = distributorRequests.count { it.status == GoodsRequestStatus.PENDING },
                    approvedRequestCount = distributorRequests.count { it.status == GoodsRequestStatus.APPROVED },
                    fulfilledRequestCount = distributorRequests.count { it.status == GoodsRequestStatus.FULFILLED },
                    settledRequestCount = distributorRequests.count { it.status == GoodsRequestStatus.SETTLED },
                    totalRequestedUnits = distributorRequests.sumOf { it.quantity },
                    lastRequestTime = distributorRequests.maxByOrNull { it.requestedAt }?.requestedAt
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
