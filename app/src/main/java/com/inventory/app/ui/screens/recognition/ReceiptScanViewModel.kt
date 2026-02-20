package com.inventory.app.ui.screens.recognition

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.BuildConfig
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.PurchaseRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.local.entity.UnitEntity
import com.inventory.app.data.repository.UnitRepository
import com.inventory.app.domain.model.SmartDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.inject.Inject

enum class ReceiptMatchType {
    CREATE_NEW,
    UPDATE_EXISTING,
    SKIP
}

data class ReceiptMatchCandidate(
    val id: Long,
    val name: String,
    val currentQuantity: Double,
    val unitId: Long?,
    val unitAbbreviation: String? = null
)

data class EditableReceiptItem(
    val name: String = "",
    val quantity: String = "1",
    val price: String = "",
    val unit: String = "",
    val categoryName: String = "",
    val matchType: ReceiptMatchType = ReceiptMatchType.CREATE_NEW,
    val matchedInventoryItemId: Long? = null,
    val matchedShoppingId: Long? = null,
    val inventoryCandidates: List<ReceiptMatchCandidate> = emptyList(),
    val expiryDate: LocalDate? = null,
    val isAiEstimatedExpiry: Boolean = false,
    val barcode: String = "",
    val isReviewed: Boolean = false,
    val unitConflict: String? = null // e.g. "Inventory uses kg" when receipt says pcs
)

sealed class ReceiptScanState {
    data object Idle : ReceiptScanState()
    data object Capturing : ReceiptScanState()
    data object ReadingText : ReceiptScanState()
    data object ParsingWithAI : ReceiptScanState()
    data class Review(val items: List<EditableReceiptItem>) : ReceiptScanState()
    data class Saving(val current: Int, val total: Int) : ReceiptScanState()
    data class Success(val count: Int) : ReceiptScanState()
    data class Error(val message: String) : ReceiptScanState()
}

data class ReceiptScanUiState(
    val state: ReceiptScanState = ReceiptScanState.Idle,
    val capturedBitmap: Bitmap? = null,
    val currencySymbol: String = "",
    val units: List<UnitEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val grokRepository: GrokRepository,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val storageLocationRepository: StorageLocationRepository,
    private val unitRepository: UnitRepository,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState = _uiState.asStateFlow()

    private var nameMatchJob: Job? = null

    init {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
            _uiState.update { it.copy(currencySymbol = currency) }
        }
        viewModelScope.launch {
            unitRepository.getAllActive().collect { units ->
                _uiState.update { it.copy(units = units) }
            }
        }
        viewModelScope.launch {
            categoryRepository.getAllActive().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(state = ReceiptScanState.ParsingWithAI, capturedBitmap = bitmap) }
        viewModelScope.launch {
            // Step 1: Compress and base64-encode the image
            val imageBase64 = compressAndEncode(bitmap, grokRepository.getImageConfig())
            if (imageBase64 == null) {
                _uiState.update {
                    it.copy(state = ReceiptScanState.Error("Failed to process the image. Please try again."))
                }
                return@launch
            }

            // Step 2: Get active shopping list for matching (small payload)
            val shoppingItems = try {
                shoppingListRepository.getActiveItems().first().map { detail ->
                    val name = detail.item?.name ?: detail.shoppingItem.customName ?: ""
                    GrokRepository.ShoppingListContext(
                        id = detail.shoppingItem.id,
                        name = name
                    )
                }.filter { it.name.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }

            // Step 3: Get existing inventory names for AI matching
            val inventoryItems = try {
                itemRepository.getAllActiveNamesAndIds().map {
                    GrokRepository.InventoryContext(id = it.id, name = it.name)
                }
            } catch (_: Exception) {
                emptyList()
            }

            // Step 4: Get category names for AI categorization
            val categoryNames = try {
                _uiState.value.categories.map { it.name }
            } catch (_: Exception) {
                emptyList()
            }

            // Step 5: Send image directly to Vision model — AI sees receipt + knows inventory + categories
            grokRepository.parseReceiptImage(imageBase64, shoppingItems, inventoryItems, categoryNames).fold(
                onSuccess = { items ->
                    // Fix Gson default: missing quantity field deserializes as 0.0 instead of 1.0
                    val fixedItems = items.map { if (it.quantity == 0.0) it.copy(quantity = 1.0) else it }
                    // Build review list using AI-returned matches
                    // Pre-load all units for conflict detection
                    val allUnits = try { unitRepository.getAllActive().first() } catch (_: Exception) { emptyList() }
                    val allCategories = _uiState.value.categories

                    val editable = fixedItems.map { item ->
                        val qtyStr = if (item.quantity == item.quantity.toLong().toDouble()) {
                            item.quantity.toLong().toString()
                        } else {
                            String.format("%.3f", item.quantity).trimEnd('0').trimEnd('.')
                        }

                        // AI-returned inventory match — look up details from DB
                        val aiMatchedItem = item.matchedInventoryId?.let { id ->
                            val entity = itemRepository.getById(id)
                            if (entity != null) {
                                val unitAbbr = entity.unitId?.let { uid ->
                                    allUnits.find { it.id == uid }?.abbreviation
                                }
                                ReceiptMatchCandidate(entity.id, entity.name, entity.quantity, entity.unitId, unitAbbr)
                            } else null
                        }

                        // Unit: AI → SmartDefaults fallback
                        val defaults = SmartDefaults.lookup(item.name)
                        val receiptUnit = item.unit ?: ""
                        val resolvedUnit = if (receiptUnit.isNotBlank()) {
                            receiptUnit
                        } else {
                            defaults?.unit ?: ""
                        }

                        // Category: AI suggestion → SmartDefaults fallback
                        val resolvedCategory = run {
                            // 1. Try AI-suggested category (case-insensitive match against DB)
                            val aiCat = item.category
                            if (!aiCat.isNullOrBlank()) {
                                val match = allCategories.find { it.name.equals(aiCat, ignoreCase = true) }
                                if (match != null) return@run match.name
                            }
                            // 2. Fall back to SmartDefaults
                            val defaultCat = defaults?.category
                            if (!defaultCat.isNullOrBlank()) {
                                val match = allCategories.find { it.name.equals(defaultCat, ignoreCase = true) }
                                if (match != null) return@run match.name
                            }
                            "" // uncategorized
                        }

                        // Detect unit conflict with matched inventory item
                        val unitConflict = when {
                            aiMatchedItem?.unitAbbreviation != null && resolvedUnit.isNotBlank() -> {
                                if (!aiMatchedItem.unitAbbreviation.equals(resolvedUnit, ignoreCase = true))
                                    "Inventory uses ${aiMatchedItem.unitAbbreviation}"
                                else null
                            }
                            aiMatchedItem != null && aiMatchedItem.unitAbbreviation == null && resolvedUnit.isNotBlank() ->
                                "Inventory has no unit set"
                            else -> null
                        }

                        // Expiry date: AI estimate → SmartDefaults fallback → null
                        val expiryDays = item.estimatedExpiryDays
                            ?: defaults?.shelfLifeDays
                        val expiryDate = expiryDays?.let { LocalDate.now().plusDays(it.toLong()) }

                        EditableReceiptItem(
                            name = item.name,
                            quantity = qtyStr,
                            price = item.price?.let { String.format("%.2f", it) } ?: "",
                            unit = resolvedUnit,
                            categoryName = resolvedCategory,
                            matchType = if (aiMatchedItem != null) ReceiptMatchType.UPDATE_EXISTING else ReceiptMatchType.CREATE_NEW,
                            matchedInventoryItemId = aiMatchedItem?.id,
                            matchedShoppingId = item.matchedShoppingId,
                            inventoryCandidates = if (aiMatchedItem != null) listOf(aiMatchedItem) else emptyList(),
                            expiryDate = expiryDate,
                            isAiEstimatedExpiry = expiryDate != null,
                            unitConflict = unitConflict
                        )
                    }
                    _uiState.update { it.copy(state = ReceiptScanState.Review(editable)) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(state = ReceiptScanState.Error(
                            error.message ?: "Failed to parse receipt"
                        ))
                    }
                }
            )
        }
    }

    // ── Image compression ─────────────────────────────────────────────

    private fun compressAndEncode(bitmap: Bitmap, config: com.inventory.app.data.repository.ImageConfig): String? {
        return try {
            if (BuildConfig.DEBUG) Log.d("ReceiptScan", "Original image: ${bitmap.width}x${bitmap.height}, maxDim=${config.maxDimension}")

            var scaled = if (bitmap.width > config.maxDimension || bitmap.height > config.maxDimension) {
                val scale = config.maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            var quality = config.startQuality
            var stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            var bytes = stream.toByteArray()
            if (BuildConfig.DEBUG) Log.d("ReceiptScan", "After scale to ${scaled.width}x${scaled.height}, q=$quality: ${bytes.size / 1024}KB")

            while (bytes.size > config.maxBytes && quality > config.minQuality) {
                quality -= 10
                stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                bytes = stream.toByteArray()
                if (BuildConfig.DEBUG) Log.d("ReceiptScan", "Re-compressed q=$quality: ${bytes.size / 1024}KB")
            }

            if (bytes.size > config.maxBytes) {
                val oldScaled = scaled
                scaled = Bitmap.createScaledBitmap(scaled, (scaled.width * config.fallbackScale).toInt(), (scaled.height * config.fallbackScale).toInt(), true)
                if (oldScaled !== bitmap) oldScaled.recycle()
                stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                bytes = stream.toByteArray()
                if (BuildConfig.DEBUG) Log.d("ReceiptScan", "Final resize to ${scaled.width}x${scaled.height}: ${bytes.size / 1024}KB")
            }

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (BuildConfig.DEBUG) Log.d("ReceiptScan", "Base64 encoded: ${base64.length} chars (~${base64.length * 3 / 4 / 1024}KB)")

            if (scaled !== bitmap) scaled.recycle()
            base64
        } catch (e: Exception) {
            Log.e("ReceiptScan", "Image compression failed", e)
            null
        }
    }

    // ── Review screen actions ─────────────────────────────────────────

    fun updateItemName(index: Int, name: String) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(name = name)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }

            // Debounced re-match against inventory
            nameMatchJob?.cancel()
            nameMatchJob = viewModelScope.launch {
                delay(300)
                reMatchItem(index, name)
            }
        }
    }

    private suspend fun reMatchItem(index: Int, name: String) {
        if (name.isBlank()) return

        // Re-read current state (may have changed during delay)
        val currentState = _uiState.value.state
        if (currentState !is ReceiptScanState.Review) return
        if (index !in currentState.items.indices) return

        val currentItem = currentState.items[index]

        val matchedEntity = try {
            itemRepository.findByName(name)
        } catch (e: Exception) {
            Log.w("ReceiptScan", "Failed to find item by name: ${e.message}")
            null
        }
        if (matchedEntity != null) {
            // Found a match — look up unit abbreviation
            val allUnits = try { unitRepository.getAllActive().first() } catch (_: Exception) { emptyList<UnitEntity>() }
            val unitAbbr = matchedEntity.unitId?.let { uid ->
                allUnits.find { it.id == uid }?.abbreviation
            }
            val candidate = ReceiptMatchCandidate(
                id = matchedEntity.id,
                name = matchedEntity.name,
                currentQuantity = matchedEntity.quantity,
                unitId = matchedEntity.unitId,
                unitAbbreviation = unitAbbr
            )

            // Detect unit conflict
            val resolvedUnit = currentItem.unit
            val unitConflict = if (unitAbbr != null && resolvedUnit.isNotBlank()) {
                if (!unitAbbr.equals(resolvedUnit, ignoreCase = true)) "Inventory uses $unitAbbr" else null
            } else null

            val updated = (_uiState.value.state as? ReceiptScanState.Review)?.items?.toMutableList() ?: return
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(
                matchType = ReceiptMatchType.UPDATE_EXISTING,
                matchedInventoryItemId = matchedEntity.id,
                inventoryCandidates = listOf(candidate),
                unitConflict = unitConflict
            )
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        } else if (currentItem.matchType == ReceiptMatchType.UPDATE_EXISTING) {
            // No match found but was previously UPDATE_EXISTING — switch to CREATE_NEW
            val updated = (_uiState.value.state as? ReceiptScanState.Review)?.items?.toMutableList() ?: return
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(
                matchType = ReceiptMatchType.CREATE_NEW,
                matchedInventoryItemId = null,
                inventoryCandidates = emptyList(),
                unitConflict = null
            )
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun updateItemQuantity(index: Int, quantity: String) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(quantity = quantity)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun updateItemPrice(index: Int, price: String) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(price = price)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun updateItemCategory(index: Int, categoryName: String) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(categoryName = categoryName)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun updateItemUnit(index: Int, unit: String) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(unit = unit, unitConflict = null)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun removeItem(index: Int) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated.removeAt(index)
            if (updated.isEmpty()) {
                _uiState.update { it.copy(state = ReceiptScanState.Error("All items removed. Try scanning again.")) }
            } else {
                _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
            }
        }
    }

    fun updateItemExpiryDate(index: Int, date: LocalDate?) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(expiryDate = date, isAiEstimatedExpiry = false)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun updateItemBarcode(index: Int, barcode: String) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(barcode = barcode)
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun markAsReviewed(index: Int) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            if (index in state.items.indices && !state.items[index].isReviewed) {
                val updated = state.items.toMutableList()
                updated[index] = updated[index].copy(isReviewed = true)
                _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
            }
        }
    }

    fun markAllReviewed() {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.map { it.copy(isReviewed = true) }
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    fun updateMatchType(index: Int, matchType: ReceiptMatchType, selectedItemId: Long? = null) {
        val state = _uiState.value.state
        if (state is ReceiptScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(
                matchType = matchType,
                matchedInventoryItemId = if (matchType == ReceiptMatchType.UPDATE_EXISTING) selectedItemId else null
            )
            _uiState.update { it.copy(state = ReceiptScanState.Review(updated)) }
        }
    }

    // ── Confirm and save ──────────────────────────────────────────────

    fun addAllToInventory() {
        val state = _uiState.value.state
        if (state !is ReceiptScanState.Review) return
        val items = state.items.filter { it.name.isNotBlank() && it.matchType != ReceiptMatchType.SKIP }
        if (items.isEmpty()) return

        // Set Saving state synchronously to prevent double-tap
        _uiState.update { it.copy(state = ReceiptScanState.Saving(0, items.size)) }

        viewModelScope.launch {
            val total = items.size

            var addedCount = 0
            for ((index, item) in items.withIndex()) {
                _uiState.update { it.copy(state = ReceiptScanState.Saving(index + 1, total)) }

                val qty = item.quantity.toDoubleOrNull() ?: 1.0
                val price = item.price.toDoubleOrNull()?.takeIf { it >= 0 }

                when (item.matchType) {
                    ReceiptMatchType.UPDATE_EXISTING -> {
                        val existingId = item.matchedInventoryItemId ?: continue

                        // Update expiry date if provided
                        if (item.expiryDate != null) {
                            itemRepository.updateExpiryDate(existingId, item.expiryDate)
                        }

                        // Update barcode if provided
                        if (item.barcode.isNotBlank()) {
                            itemRepository.updateBarcode(existingId, item.barcode)
                        }

                        if (price != null) {
                            // addPurchase handles: quantity adjustment, purchase date, purchase price, purchase history
                            purchaseRepository.addPurchase(
                                itemId = existingId,
                                quantity = qty,
                                totalPrice = price,
                                purchaseDate = LocalDate.now(),
                                expiryDate = item.expiryDate,
                                storeName = null,
                                notes = "From receipt scan"
                            )
                        } else {
                            // No price — just adjust quantity (no purchase history)
                            itemRepository.adjustQuantity(existingId, qty)
                            itemRepository.updatePurchaseDate(existingId, LocalDate.now())
                        }
                    }
                    ReceiptMatchType.CREATE_NEW -> {
                        val defaults = SmartDefaults.lookup(item.name)
                        // Category: use the resolved category from review (AI → SmartDefaults → user override)
                        val categoryId = if (item.categoryName.isNotBlank()) {
                            categoryRepository.findCategoryByNameIgnoreCase(item.categoryName)?.id
                        } else {
                            // Final fallback to SmartDefaults if somehow empty
                            defaults?.category?.let { categoryRepository.findCategoryByName(it)?.id }
                        }
                        val unitId = if (item.unit.isNotBlank()) {
                            unitRepository.findByAbbreviation(item.unit)?.id
                                ?: unitRepository.findByName(item.unit)?.id
                        } else {
                            defaults?.unit?.let { unitRepository.findByAbbreviation(it)?.id }
                        }
                        val locationId = defaults?.location?.let { storageLocationRepository.findByName(it)?.id }

                        val entity = ItemEntity(
                            name = item.name,
                            quantity = if (price != null) 0.0 else qty,  // addPurchase will adjust quantity when price is present
                            categoryId = categoryId,
                            unitId = unitId,
                            storageLocationId = locationId,
                            purchasePrice = price,
                            purchaseDate = LocalDate.now(),
                            expiryDate = item.expiryDate,
                            barcode = item.barcode.ifBlank { null }
                        )
                        val newItemId = itemRepository.insert(entity)

                        if (price != null) {
                            purchaseRepository.addPurchase(
                                itemId = newItemId,
                                quantity = qty,
                                totalPrice = price,
                                purchaseDate = LocalDate.now(),
                                expiryDate = item.expiryDate,
                                storeName = null,
                                notes = "From receipt scan"
                            )
                        }
                    }
                    ReceiptMatchType.SKIP -> { /* filtered out */ }
                }

                // Mark shopping list item as purchased if matched
                if (item.matchedShoppingId != null) {
                    try {
                        shoppingListRepository.markAsPurchasedOnly(item.matchedShoppingId)
                    } catch (e: Exception) {
                        Log.w("ReceiptScan", "Failed to mark shopping item ${item.matchedShoppingId} as purchased", e)
                    }
                }

                addedCount++
            }

            _uiState.update { it.copy(state = ReceiptScanState.Success(addedCount)) }
        }
    }

    fun reset() {
        _uiState.update { ReceiptScanUiState(currencySymbol = it.currencySymbol) }
    }
}
