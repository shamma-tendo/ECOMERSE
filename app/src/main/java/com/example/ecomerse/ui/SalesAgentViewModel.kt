package com.example.ecomerse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecomerse.data.AppRepository
import com.example.ecomerse.data.SessionManager
import com.example.ecomerse.model.InventoryItem
import com.example.ecomerse.model.Product
import kotlinx.coroutines.flow.*

data class SalesAgentUiState(
    val agentName: String = "",
    val distributorId: String = "",
    val productsWithStock: List<ProductStockInfo> = emptyList(),
    val isLoading: Boolean = false
)

data class ProductStockInfo(
    val product: Product,
    val stockQuantity: Int
)

class SalesAgentViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SalesAgentUiState())
    val uiState: StateFlow<SalesAgentUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        combine(
            SessionManager.currentUser,
            AppRepository.products,
            AppRepository.inventory
        ) { user, products, inventory ->
            if (user != null) {
                val distId = user.distributorId ?: ""
                val stockInfoList = products.map { product ->
                    val stock = inventory.find { 
                        it.productId == product.id && it.distributorId == distId 
                    }?.quantity ?: 0
                    ProductStockInfo(product, stock)
                }
                
                _uiState.update { currentState ->
                    currentState.copy(
                        agentName = user.name,
                        distributorId = distId,
                        productsWithStock = stockInfoList
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun recordSale(productId: String) {
        val currentUser = SessionManager.currentUser.value
        if (currentUser != null && currentUser.distributorId != null) {
            AppRepository.recordSale(
                productId = productId,
                distributorId = currentUser.distributorId,
                agentId = currentUser.id,
                quantity = 1
            )
        }
    }
}
