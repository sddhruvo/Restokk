package com.inventory.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LowStockReportUiState(
    val lowStockItems: List<ItemWithDetails> = emptyList(),
    val outOfStockItems: List<ItemWithDetails> = emptyList(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val lowStockThreshold: Float = 0.25f
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LowStockReportViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LowStockReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                settingsRepository.getStringFlow(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, "25")
                    .map { (it.toDoubleOrNull() ?: 25.0) / 100.0 }
                    .flatMapLatest { ratio ->
                        itemRepository.getLowStockItemsReport(ratio).map { items -> Pair(items, ratio) }
                    }
                    .collect { (items, ratio) ->
                    _uiState.update { it.copy(lowStockItems = items, lowStockCount = items.size, lowStockThreshold = ratio.toFloat()) }
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
