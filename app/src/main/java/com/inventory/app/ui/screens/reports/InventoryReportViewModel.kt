package com.inventory.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.CategoryValueRow
import com.inventory.app.data.local.dao.ChartDataRow
import com.inventory.app.data.local.dao.TopValueItemRow
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryReportUiState(
    val allItems: List<ItemWithDetails> = emptyList(),
    val totalItems: Int = 0,
    val totalValue: Double = 0.0,
    val averageItemValue: Double = 0.0,
    val itemsByCategory: List<ChartDataRow> = emptyList(),
    val itemsByLocation: List<ChartDataRow> = emptyList(),
    val valueByCategory: List<CategoryValueRow> = emptyList(),
    val topValueItems: List<TopValueItemRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currencySymbol: String = ""
)

@HiltViewModel
class InventoryReportViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getStringFlow(
                SettingsRepository.KEY_CURRENCY_SYMBOL,
                FormatUtils.getDefaultCurrencySymbol()
            ).collect { currency ->
                _uiState.update { it.copy(currencySymbol = currency) }
            }
        }
        viewModelScope.launch {
            try {
                itemRepository.getAllActiveWithDetails().collect { items ->
                    _uiState.update {
                        val avg = if (items.isNotEmpty() && it.totalValue > 0) it.totalValue / items.size else 0.0
                        it.copy(
                            allItems = items,
                            totalItems = items.size,
                            averageItemValue = avg,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load inventory: ${e.message}", isLoading = false) }
            }
        }
        viewModelScope.launch {
            try {
                itemRepository.getTotalValue().collect { value ->
                    _uiState.update {
                        val avgValue = if (it.totalItems > 0) value / it.totalItems else 0.0
                        it.copy(totalValue = value, averageItemValue = avgValue)
                    }
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                itemRepository.getItemCountByCategory(20).collect { data ->
                    _uiState.update { it.copy(itemsByCategory = data) }
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                itemRepository.getItemCountByLocation(20).collect { data ->
                    _uiState.update { it.copy(itemsByLocation = data) }
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                itemRepository.getValueByCategory(10).collect { data ->
                    _uiState.update { it.copy(valueByCategory = data) }
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                itemRepository.getTopValueItems(5).collect { data ->
                    _uiState.update { it.copy(topValueItems = data) }
                }
            } catch (_: Exception) { }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
