package com.inventory.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LowStockReportUiState(
    val lowStockItems: List<ItemWithDetails> = emptyList(),
    val outOfStockItems: List<ItemWithDetails> = emptyList(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LowStockReportViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LowStockReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                itemRepository.getLowStockItemsReport().collect { items ->
                    _uiState.update { it.copy(lowStockItems = items, lowStockCount = items.size) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load low stock items: ${e.message}", isLoading = false) }
            }
        }
        viewModelScope.launch {
            try {
                itemRepository.getOutOfStockItemsReport().collect { items ->
                    _uiState.update { it.copy(outOfStockItems = items, outOfStockCount = items.size, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load out of stock items: ${e.message}", isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
