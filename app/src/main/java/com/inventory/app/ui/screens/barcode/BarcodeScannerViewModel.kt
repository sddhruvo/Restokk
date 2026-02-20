package com.inventory.app.ui.screens.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.repository.BarcodeRepository
import com.inventory.app.data.repository.BarcodeResult
import com.inventory.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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

data class BarcodeScannerUiState(
    val manualBarcode: String = "",
    val isLookingUp: Boolean = false,
    val result: ScanResult = ScanResult.None,
    val quickAddDone: Boolean = false,
    val scanningEnabled: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val barcodeRepository: BarcodeRepository,
    private val itemRepository: ItemRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeScannerUiState())
    val uiState = _uiState.asStateFlow()

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
                result.found -> ScanResult.NewProduct(result, barcode)
                else -> ScanResult.NotFound(barcode)
            }

            _uiState.update { it.copy(isLookingUp = false, result = scanResult) }
        }
    }

    fun quickAdd(barcode: String, name: String, brand: String?) {
        viewModelScope.launch {
            try {
                // Check if item with this barcode already exists
                val existing = itemRepository.findByBarcode(barcode)
                if (existing != null) {
                    // Just increment quantity instead of replacing
                    itemRepository.adjustQuantity(existing.id, 1.0)
                    purchaseHistoryDao.insert(
                        PurchaseHistoryEntity(
                            itemId = existing.id,
                            quantity = 1.0,
                            purchaseDate = LocalDate.now(),
                            notes = "From barcode scan"
                        )
                    )
                } else {
                    val newId = itemRepository.insert(
                        ItemEntity(name = name, barcode = barcode, brand = brand, quantity = 1.0)
                    )
                    purchaseHistoryDao.insert(
                        PurchaseHistoryEntity(
                            itemId = newId,
                            quantity = 1.0,
                            purchaseDate = LocalDate.now(),
                            notes = "From barcode scan"
                        )
                    )
                }
                _uiState.update { it.copy(quickAddDone = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add item: ${e.message}") }
            }
        }
    }

    fun onBarcodeDetected(barcode: String) {
        // Only process if scanning is enabled and not already looking up
        if (_uiState.value.scanningEnabled && !_uiState.value.isLookingUp) {
            _uiState.update { it.copy(scanningEnabled = false) }
            lookupBarcode(barcode)
        }
    }

    fun scanAgain() {
        _uiState.update {
            it.copy(result = ScanResult.None, quickAddDone = false, scanningEnabled = true, manualBarcode = "")
        }
    }
}
