package com.inventory.app.ui.screens.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.repository.BarcodeRepository
import com.inventory.app.data.repository.BarcodeResult
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.SmartDefaultRepository
import com.inventory.app.domain.model.DefaultHints
import com.inventory.app.domain.model.SuggestedAction
import com.inventory.app.domain.model.QuantitySource
import com.inventory.app.domain.model.ResolvedDefaults
import com.inventory.app.domain.model.SmartQuantityResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanResult {
    data object None : ScanResult()
    data class ExistingItem(val item: ItemEntity) : ScanResult()
    data class NewProduct(val barcodeResult: BarcodeResult, val barcode: String) : ScanResult()
    data class NotFound(val barcode: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

data class PendingQuickAdd(
    val itemId: Long?,
    val barcode: String,
    val name: String,
    val brand: String?,
    val quantity: Double,
    val source: QuantitySource,
    val shoppingListId: Long?,
    val unitAbbreviation: String?,
    val isNewProduct: Boolean,
    val resolvedDefaults: ResolvedDefaults? = null
)

data class BarcodeScannerUiState(
    val manualBarcode: String = "",
    val isLookingUp: Boolean = false,
    val result: ScanResult = ScanResult.None,
    val quickAddDone: Boolean = false,
    val scanningEnabled: Boolean = true,
    val error: String? = null,
    val lowStockThreshold: Float = 0.25f,
    val pendingAdd: PendingQuickAdd? = null
)

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val barcodeRepository: BarcodeRepository,
    private val itemRepository: ItemRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val settingsRepository: SettingsRepository,
    private val smartDefaultRepository: SmartDefaultRepository,
    private val smartQuantityResolver: SmartQuantityResolver,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeScannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val threshold = (settingsRepository.getString(SettingsRepository.KEY_LOW_STOCK_THRESHOLD, "25").toDoubleOrNull() ?: 25.0) / 100.0
            _uiState.update { it.copy(lowStockThreshold = threshold.toFloat()) }
        }
    }

    fun updateManualBarcode(value: String) {
        _uiState.update { it.copy(manualBarcode = value) }
    }

    fun lookupBarcode(barcode: String) {
        if (barcode.isBlank()) return
        _uiState.update { it.copy(isLookingUp = true, result = ScanResult.None) }

        viewModelScope.launch {
            // Check if item with this barcode already exists
            val existingItem = itemRepository.findByBarcode(barcode)
            if (existingItem != null) {
                _uiState.update {
                    it.copy(isLookingUp = false, result = ScanResult.ExistingItem(existingItem))
                }
                return@launch
            }

            // Look up in API
            val result = barcodeRepository.lookup(barcode)
            val scanResult = when {
                result.errorMessage != null -> ScanResult.Error(result.errorMessage)
                result.found -> {
                    // API found a product — check if name matches an existing inventory item
                    val productName = result.productName ?: ""
                    if (productName.isNotBlank()) {
                        val matchResult = try {
                            itemRepository.findMatchingItems(productName, barcode)
                        } catch (_: Exception) { null }
                        val bestMatch = matchResult?.bestMatch
                        if (bestMatch != null && bestMatch.confidence == com.inventory.app.domain.model.ProductMatcher.MatchConfidence.DEFINITE) {
                            // DEFINITE match only — safe to auto-link barcode to existing item
                            val matchedEntity = itemRepository.getById(bestMatch.itemId)
                            if (matchedEntity != null) {
                                itemRepository.updateBarcode(matchedEntity.id, barcode)
                                ScanResult.ExistingItem(matchedEntity.copy(barcode = barcode))
                            } else {
                                ScanResult.NewProduct(result, barcode)
                            }
                        } else {
                            ScanResult.NewProduct(result, barcode)
                        }
                    } else {
                        ScanResult.NewProduct(result, barcode)
                    }
                }
                else -> ScanResult.NotFound(barcode)
            }

            _uiState.update { it.copy(isLookingUp = false, result = scanResult) }
        }
    }

    fun prepareQuickAdd(barcode: String, name: String, brand: String?) {
        viewModelScope.launch {
            try {
                val existing = itemRepository.findByBarcode(barcode)
                if (existing != null) {
                    // Existing item — resolve smart quantity
                    val smartQty = smartQuantityResolver.resolve(existing.id, name, existing.unitId)
                    _uiState.update {
                        it.copy(
                            pendingAdd = PendingQuickAdd(
                                itemId = existing.id,
                                barcode = barcode,
                                name = name,
                                brand = brand,
                                quantity = smartQty.value,
                                source = smartQty.source,
                                shoppingListId = smartQty.shoppingListId,
                                unitAbbreviation = smartQty.unitAbbreviation,
                                isNewProduct = false
                            )
                        )
                    }
                } else {
                    // New product — resolve smart defaults + quantity
                    val regionCode = settingsRepository.getRegionCode()
                    val result = smartDefaultRepository.resolve(
                        itemName = name,
                        regionCode = regionCode,
                        hints = DefaultHints(brand = brand),
                        includeRemote = false
                    )
                    val d = result.local
                    val unitAbbr = d.unitAbbreviation
                    _uiState.update {
                        it.copy(
                            pendingAdd = PendingQuickAdd(
                                itemId = null,
                                barcode = barcode,
                                name = name,
                                brand = brand,
                                quantity = d.quantity ?: 1.0,
                                source = QuantitySource.DEFAULT,
                                shoppingListId = null,
                                unitAbbreviation = unitAbbr,
                                isNewProduct = true,
                                resolvedDefaults = d
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to prepare: ${e.message}") }
            }
        }
    }

    fun updatePendingQuantity(quantity: Double) {
        _uiState.update { state ->
            state.pendingAdd?.let { pending ->
                state.copy(pendingAdd = pending.copy(quantity = quantity))
            } ?: state
        }
    }

    fun confirmQuickAdd(quantity: Double) {
        val pending = _uiState.value.pendingAdd ?: return
        viewModelScope.launch {
            try {
                if (!pending.isNewProduct && pending.itemId != null) {
                    // Existing item — adjust quantity
                    itemRepository.adjustQuantity(pending.itemId, quantity)
                    purchaseHistoryDao.insert(
                        PurchaseHistoryEntity(
                            itemId = pending.itemId,
                            quantity = quantity,
                            purchaseDate = LocalDate.now(),
                            notes = "From barcode scan"
                        )
                    )
                } else {
                    // New product — create item
                    val d = pending.resolvedDefaults
                    val newId = itemRepository.insert(
                        ItemEntity(
                            name = pending.name,
                            barcode = pending.barcode,
                            brand = pending.brand,
                            quantity = quantity,
                            categoryId = d?.categoryId,
                            unitId = d?.unitId,
                            storageLocationId = d?.locationId,
                            expiryDate = d?.shelfLifeDays?.let { LocalDate.now().plusDays(it.toLong()) }
                        )
                    )
                    purchaseHistoryDao.insert(
                        PurchaseHistoryEntity(
                            itemId = newId,
                            quantity = quantity,
                            purchaseDate = LocalDate.now(),
                            notes = "From barcode scan"
                        )
                    )
                }

                // Auto-mark shopping list item as purchased if matched
                pending.shoppingListId?.let { shoppingListRepository.markAsPurchasedOnly(it) }

                _uiState.update { it.copy(pendingAdd = null, quickAddDone = true) }
                // Auto-reset after 2s so user can scan next item
                delay(2000)
                _uiState.update {
                    it.copy(result = ScanResult.None, quickAddDone = false, scanningEnabled = true, manualBarcode = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add item: ${e.message}") }
            }
        }
    }

    fun dismissPendingAdd() {
        _uiState.update { it.copy(pendingAdd = null) }
    }

    fun onBarcodeDetected(barcode: String) {
        // Atomic check-and-update to prevent duplicate lookups from rapid camera frames
        var shouldLookup = false
        _uiState.update {
            if (it.scanningEnabled && !it.isLookingUp) {
                shouldLookup = true
                it.copy(scanningEnabled = false)
            } else it
        }
        if (!shouldLookup) return

        // Rapid scan: auto-confirm current pending before processing new barcode
        val pending = _uiState.value.pendingAdd
        if (pending != null) {
            viewModelScope.launch {
                confirmQuickAdd(pending.quantity)
                lookupBarcode(barcode)
            }
        } else {
            lookupBarcode(barcode)
        }
    }

    fun scanAgain() {
        _uiState.update {
            it.copy(result = ScanResult.None, quickAddDone = false, scanningEnabled = true, manualBarcode = "", pendingAdd = null)
        }
    }

    /** Called on ON_RESUME — if the displayed barcode was added via ItemForm, auto-reset. */
    fun checkCurrentBarcodeAdded() {
        val result = _uiState.value.result
        val barcode = when (result) {
            is ScanResult.NewProduct -> result.barcode
            is ScanResult.NotFound -> result.barcode
            else -> return
        }
        viewModelScope.launch {
            val exists = itemRepository.findByBarcode(barcode)
            if (exists != null) {
                _uiState.update {
                    it.copy(result = ScanResult.None, scanningEnabled = true, manualBarcode = "")
                }
            }
        }
    }
}
