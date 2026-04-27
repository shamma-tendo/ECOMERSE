package com.example.ecomerse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.model.*
import kotlinx.coroutines.flow.*
import java.util.*

data class ManagerUiState(
    val employees: List<User> = listOf(
        User("agent_001", "Alice Smith", UserRole.SALES_AGENT, "dist1", "alice@ecomerse.com", "Senior Agent"),
        User("sup_bob", "Bob Johnson", UserRole.STOCK_SUPERVISOR, "dist1", "bob@ecomerse.com", "Warehouse Lead"),
        User("agent_002", "Charlie Brown", UserRole.SALES_AGENT, "dist2", "charlie@ecomerse.com", "Junior Agent")
    ),
    val leaveRequests: List<LeaveRequest> = listOf(
        LeaveRequest("L1", "agent_001", "Alice Smith", "2023-12-01", "2023-12-05", "Vacation"),
        LeaveRequest("L2", "agent_002", "Charlie Brown", "2023-12-10", "2023-12-11", "Medical")
    ),
    val dismissalDraft: String = "",
    val salesReports: List<AgentSalesReport> = emptyList()
)

data class AgentSalesReport(
    val agentId: String,
    val agentName: String,
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
            _uiState.value.employees
                .filter { it.role == UserRole.SALES_AGENT }
                .map { agent ->
                    val agentSales = sales.filter { it.agentId == agent.id }
                    val revenue = agentSales.sumOf { sale ->
                        val product = products.find { it.id == sale.productId }
                        (product?.unitPrice ?: 0.0) * sale.quantity
                    }
                    AgentSalesReport(
                        agentId = agent.id,
                        agentName = agent.name,
                        totalSalesCount = agentSales.sumOf { it.quantity },
                        totalRevenue = revenue,
                        lastSaleTime = agentSales.maxByOrNull { it.timestamp }?.timestamp
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
