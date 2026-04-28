package com.example.ecomerse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.model.*
import kotlinx.coroutines.flow.*

data class ManagerUiState(
    val employees: List<User> = listOf(
        User("dist_staff_1", "Grace Namara", UserRole.DISTRIBUTOR, "dist1", "grace@ecomerse.com", "Distribution Lead"),
        User("dist_staff_2", "Peter Ouma", UserRole.DISTRIBUTOR, "dist2", "peter@ecomerse.com", "Warehouse Coordinator")
    ),
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
        _uiState.update { state ->
            state.copy(employees = state.employees.map {
                if (it.id == employeeId) it.copy(status = EmployeeStatus.INACTIVE) else it
            })
        }
        AppRepository.logActivity("MANAGER", "Terminated employee $employeeId")
    }
}
