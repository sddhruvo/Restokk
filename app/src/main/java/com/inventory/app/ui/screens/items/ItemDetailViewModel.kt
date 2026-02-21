package com.inventory.app.ui.screens.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.UsageLogEntity
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.PurchaseRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PriceTrendPoint(
    val date: LocalDate,
    val unitPrice: Double
)

data class ItemDetailUiState(
    val item: ItemWithDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val usageLogs: List<UsageLogEntity> = emptyList(),
    val purchaseHistory: List<PurchaseHistoryEntity> = emptyList(),
    val priceTrendData: List<PriceTrendPoint> = emptyList(),
    val currencySymbol: String = ""
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val usageRepository: UsageRepository,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val itemId: Long = savedStateHandle["itemId"] ?: 0L
    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (itemId == 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Item not found") }
        } else {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
            _uiState.update { it.copy(currencySymbol = currency) }
        }
        viewModelScope.launch {
            itemRepository.getByIdWithDetails(itemId).collect { details ->
                _uiState.update { it.copy(item = details, isLoading = false) }
            }
        }
        viewModelScope.launch {
            usageRepository.getByItemId(itemId).collect { logs ->
                _uiState.update { it.copy(usageLogs = logs) }
            }
        }
        viewModelScope.launch {
            purchaseRepository.getByItemId(itemId).collect { purchases ->
                val trendData = purchases
                    .filter { it.totalPrice != null && it.quantity > 0 }
                    .map { p ->
                        PriceTrendPoint(
                            date = p.purchaseDate,
                            unitPrice = p.unitPrice ?: ((p.totalPrice ?: 0.0) / p.quantity)
                        )
                    }
                    .sortedBy { it.date }
                _uiState.update { it.copy(purchaseHistory = purchases, priceTrendData = trendData) }
            }
        }
        } // else
    }

    fun adjustQuantity(delta: Double) {
        viewModelScope.launch {
            try {
                val current = _uiState.value.item?.item?.quantity ?: 0.0
                if (current + delta < 0) return@launch
                itemRepository.adjustQuantity(itemId, delta)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to adjust quantity: ${e.message}") }
            }
        }
    }

    fun logUsage(quantity: Double, usageType: String, notes: String?) {
        viewModelScope.launch {
            try {
                usageRepository.logUsage(itemId, quantity, usageType, notes)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to log usage: ${e.message}") }
            }
        }
    }

    fun addPurchase(quantity: Double, totalPrice: Double?, storeName: String?, notes: String?) {
        if (quantity <= 0) return
        if (totalPrice != null && totalPrice < 0) return
        viewModelScope.launch {
            try {
                purchaseRepository.addPurchase(
                    itemId = itemId,
                    quantity = quantity,
                    totalPrice = totalPrice,
                    purchaseDate = LocalDate.now(),
                    expiryDate = null,
                    storeName = storeName,
                    notes = notes
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to record purchase: ${e.message}") }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                itemRepository.toggleFavorite(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update favorite: ${e.message}") }
            }
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            try {
                val item = _uiState.value.item?.item ?: return@launch
                if (item.isPaused) {
                    itemRepository.unpauseItem(itemId)
                } else {
                    itemRepository.pauseItem(itemId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update pause status: ${e.message}") }
            }
        }
    }

    fun deleteItem() {
        viewModelScope.launch {
            try {
                itemRepository.softDelete(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete item: ${e.message}") }
            }
        }
    }
}
