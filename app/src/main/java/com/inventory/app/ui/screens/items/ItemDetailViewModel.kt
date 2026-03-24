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
    val operationError: String? = null,
    val usageLogs: List<UsageLogEntity> = emptyList(),
    val purchaseHistory: List<PurchaseHistoryEntity> = emptyList(),
    val priceTrendData: List<PriceTrendPoint> = emptyList(),
    val currencySymbol: String = "",
    val typicalServing: Double = 1.0,
    val typicalPurchaseQty: Double = 1.0,
    val daysRemaining: Int? = null,
    val snackbarMessage: String? = null,
    val canUndo: Boolean = false,
    val lowStockThreshold: Float = 0.25f
)

sealed class UndoableAction {
    data class QuickUse(val qty: Double, val usageLogId: Long) : UndoableAction()
    data class QuickRestock(val qty: Double, val purchaseId: Long) : UndoableAction()
}

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
    private val undoStack = mutableListOf<UndoableAction>()

    init {
        if (itemId == 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Item not found") }
        } else {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
            val threshold = (settingsRepository.getString(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, "25").toDoubleOrNull() ?: 25.0) / 100.0
            _uiState.update { it.copy(currencySymbol = currency, lowStockThreshold = threshold.toFloat()) }
        }
        viewModelScope.launch {
            itemRepository.getByIdWithDetails(itemId).collect { details ->
                _uiState.update { it.copy(item = details, isLoading = false) }
                // Recalculate daysRemaining now that item is loaded
                if (details != null) loadSmartDefaults()
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

    private fun loadSmartDefaults() {
        viewModelScope.launch {
            try {
                val serving = usageRepository.getTypicalServing(itemId) ?: 1.0
                val purchaseQty = purchaseRepository.getTypicalPurchaseQuantity(itemId) ?: 1.0
                val daysRemaining = computeDaysRemaining()
                _uiState.update {
                    it.copy(
                        typicalServing = serving,
                        typicalPurchaseQty = purchaseQty,
                        daysRemaining = daysRemaining
                    )
                }
            } catch (_: Exception) {
                // Non-critical — keep defaults
            }
        }
    }

    private suspend fun computeDaysRemaining(): Int? {
        val currentQty = _uiState.value.item?.item?.quantity ?: return null
        if (currentQty <= 0) return 0
        // Primary: usage-log based
        val dailyRate = usageRepository.getDailyConsumptionRate(itemId)
        if (dailyRate != null && dailyRate > 0) {
            return (currentQty / dailyRate).toInt()
        }
        return null
    }

    fun quickUse() {
        viewModelScope.launch {
            try {
                val currentQty = _uiState.value.item?.item?.quantity ?: return@launch
                val serving = _uiState.value.typicalServing
                val effectiveQty = minOf(serving, currentQty)
                if (effectiveQty <= 0) return@launch
                val usageLogId = usageRepository.logUsage(itemId, effectiveQty, "consumed", null)
                undoStack.add(UndoableAction.QuickUse(effectiveQty, usageLogId))
                val unit = _uiState.value.item?.unit?.abbreviation?.let { " $it" } ?: ""
                _uiState.update { it.copy(snackbarMessage = "Used ${formatQtyValue(effectiveQty)}$unit", canUndo = true) }
                loadSmartDefaults()
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to record usage: ${e.message}") }
            }
        }
    }

    fun quickRestock() {
        viewModelScope.launch {
            try {
                val purchaseQty = _uiState.value.typicalPurchaseQty
                if (purchaseQty <= 0) return@launch
                val purchaseId = purchaseRepository.addPurchase(
                    itemId = itemId,
                    quantity = purchaseQty,
                    totalPrice = null,
                    purchaseDate = LocalDate.now(),
                    expiryDate = null,
                    storeName = null,
                    notes = null
                )
                undoStack.add(UndoableAction.QuickRestock(purchaseQty, purchaseId))
                val unit = _uiState.value.item?.unit?.abbreviation?.let { " $it" } ?: ""
                _uiState.update { it.copy(snackbarMessage = "Restocked +${formatQtyValue(purchaseQty)}$unit", canUndo = true) }
                loadSmartDefaults()
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to restock: ${e.message}") }
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.lastIndex)
        viewModelScope.launch {
            try {
                when (action) {
                    is UndoableAction.QuickUse -> {
                        usageRepository.deleteUsageLog(action.usageLogId)
                        itemRepository.adjustQuantity(itemId, action.qty) // add back
                    }
                    is UndoableAction.QuickRestock -> {
                        purchaseRepository.undoPurchase(action.purchaseId, itemId, action.qty)
                    }
                }
                _uiState.update { it.copy(
                    snackbarMessage = "Undone",
                    canUndo = undoStack.isNotEmpty()
                ) }
                loadSmartDefaults()
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to undo: ${e.message}") }
            }
        }
    }

    private fun formatQtyValue(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.1f", value)
    }

    fun adjustQuantity(delta: Double) {
        viewModelScope.launch {
            try {
                val current = _uiState.value.item?.item?.quantity ?: 0.0
                if (current + delta < 0) return@launch
                itemRepository.adjustQuantity(itemId, delta)
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to adjust quantity: ${e.message}") }
            }
        }
    }

    fun logUsage(quantity: Double, usageType: String, notes: String?) {
        viewModelScope.launch {
            try {
                usageRepository.logUsage(itemId, quantity, usageType, notes)
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to log usage: ${e.message}") }
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
                _uiState.update { it.copy(operationError = "Failed to record purchase: ${e.message}") }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                itemRepository.toggleFavorite(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to update favorite: ${e.message}") }
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
                _uiState.update { it.copy(operationError = "Failed to update pause status: ${e.message}") }
            }
        }
    }

    fun clearOperationError() {
        _uiState.update { it.copy(operationError = null) }
    }

    fun deleteItem() {
        viewModelScope.launch {
            try {
                usageRepository.softDeleteWithWasteDetection(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(operationError = "Failed to delete item: ${e.message}") }
            }
        }
    }
}
