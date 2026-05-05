package com.example.ecomerse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.model.*
import kotlinx.coroutines.flow.*

data class ManagerUiState(
    val employees: List<User> = emptyList(),
    val leaveRequests: List<LeaveRequest> = emptyList(),
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
        observeData()
    }

    private fun observeData() {
        // Observe Sales, Products, and Users for reports
        combine(AppRepository.sales, AppRepository.products, AppRepository.allUsers) { sales, products, users ->
            sales
                .groupBy { it.distributorId }
                .map { (distributorId, distributorSales) ->
                    val revenue = distributorSales.sumOf { sale ->
                        val product = products.find { it.id == sale.productId }
                        (product?.unitPrice ?: 0.0) * sale.quantity
                    }

                    val distributor = users.find { it.role == UserRole.DISTRIBUTOR && it.distributorId == distributorId }
                    val distributorName = distributor?.name ?: distributorId

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

        // Observe Employees
        AppRepository.employees.onEach { employees ->
            _uiState.update { it.copy(employees = employees) }
        }.launchIn(viewModelScope)

        // Observe Leave Requests
        AppRepository.leaveRequests.onEach { leaveRequests ->
            _uiState.update { it.copy(leaveRequests = leaveRequests) }
        }.launchIn(viewModelScope)
    }

    fun approveLeave(requestId: String) {
        AppRepository.updateLeaveStatus(requestId, LeaveStatus.APPROVED)
        AppRepository.logActivity("MANAGER", "Approved leave request $requestId")
    }

    fun rejectLeave(requestId: String) {
        AppRepository.updateLeaveStatus(requestId, LeaveStatus.REJECTED)
        AppRepository.logActivity("MANAGER", "Rejected leave request $requestId")
    }

    fun updateDismissalDraft(text: String) {
        _uiState.update { it.copy(dismissalDraft = text) }
    }

    fun terminateEmployee(employeeId: String) {
        AppRepository.updateEmployeeStatus(employeeId, EmployeeStatus.INACTIVE)
        AppRepository.logActivity("MANAGER", "Terminated employee $employeeId")
    }
}
