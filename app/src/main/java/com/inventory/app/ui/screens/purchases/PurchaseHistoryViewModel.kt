package com.inventory.app.ui.screens.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.PurchaseWithItemDetails
import com.inventory.app.data.repository.PurchaseRepository
import com.inventory.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PurchaseHistoryUiState(
    val purchases: List<PurchaseWithItemDetails> = emptyList(),
    val filteredPurchases: List<PurchaseWithItemDetails> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: PurchaseFilter = PurchaseFilter.ALL,
    val totalSpent: Double = 0.0,
    val purchaseCount: Int = 0,
    val currencySymbol: String = "",
    val itemName: String? = null
)

enum class PurchaseFilter(val label: String) {
    ALL("All"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("3 Months")
}

@HiltViewModel
class PurchaseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val itemId: Long? = savedStateHandle.get<String>("itemId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(PurchaseHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
            _uiState.update { it.copy(currencySymbol = currency) }
        }
        viewModelScope.launch {
            val source = if (itemId != null) {
                purchaseRepository.getByItemIdWithDetails(itemId)
            } else {
                purchaseRepository.getAllWithDetails()
            }
            source.collect { purchases ->
                val name = if (itemId != null) purchases.firstOrNull()?.itemName else null
                // Single state update: set purchases and apply filters together to avoid double recomposition
                _uiState.update { current ->
                    val filtered = computeFiltered(purchases, current.selectedFilter, current.searchQuery)
                    current.copy(
                        purchases = purchases,
                        itemName = name,
                        isLoading = false,
                        filteredPurchases = filtered,
                        totalSpent = filtered.sumOf { it.totalPrice ?: 0.0 },
                        purchaseCount = filtered.size
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setFilter(filter: PurchaseFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilters()
    }

    private fun computeFiltered(
        purchases: List<PurchaseWithItemDetails>,
        filter: PurchaseFilter,
        searchQuery: String
    ): List<PurchaseWithItemDetails> {
        val now = LocalDate.now()

        val dateFiltered = when (filter) {
            PurchaseFilter.ALL -> purchases
            PurchaseFilter.THIS_WEEK -> purchases.filter {
                it.purchaseDate.isAfter(now.minusWeeks(1)) || it.purchaseDate.isEqual(now.minusWeeks(1))
            }
            PurchaseFilter.THIS_MONTH -> purchases.filter {
                it.purchaseDate.isAfter(now.minusMonths(1)) || it.purchaseDate.isEqual(now.minusMonths(1))
            }
            PurchaseFilter.LAST_3_MONTHS -> purchases.filter {
                it.purchaseDate.isAfter(now.minusMonths(3)) || it.purchaseDate.isEqual(now.minusMonths(3))
            }
        }

        return if (searchQuery.isBlank()) {
            dateFiltered
        } else {
            dateFiltered.filter { purchase ->
                purchase.itemName.contains(searchQuery, ignoreCase = true) ||
                    purchase.storeName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val filtered = computeFiltered(state.purchases, state.selectedFilter, state.searchQuery)
            state.copy(
                filteredPurchases = filtered,
                totalSpent = filtered.sumOf { it.totalPrice ?: 0.0 },
                purchaseCount = filtered.size
            )
        }
    }
}
