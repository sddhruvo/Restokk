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
import com.inventory.app.data.repository.SmartDefaultRepository
import com.inventory.app.domain.model.DefaultHints
import com.inventory.app.domain.model.ProductMatcher
import com.inventory.app.domain.model.SuggestedAction
import com.inventory.app.util.ItemNameNormalizer
import com.inventory.app.util.NonFoodCategories
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
    val unitConflict: String? = null, // e.g. "Inventory uses kg" when receipt says pcs
    val locationName: String = "",
    // Resolved IDs from first SmartDefaults resolve — used at save time to avoid double-resolve mismatch
    val resolvedCategoryId: Long? = null,
    val resolvedUnitId: Long? = null,
    val resolvedLocationId: Long? = null,
    val mergedCount: Int = 1, // How many AI receipt lines were merged into this item
    val shoppingQuantity: Double? = null, // Quantity from shopping list for mismatch warning
    val isNonFood: Boolean = false
)

sealed class ReceiptScanState {
    data object Idle : ReceiptScanState()
    data object Capturing : ReceiptScanState()
    data object ReadingText : ReceiptScanState()
    data object ParsingWithAI : ReceiptScanState()
    data class Review(val items: List<EditableReceiptItem>) : ReceiptScanState()
    data class Saving(val current: Int, val total: Int) : ReceiptScanState()
    data class Success(val count: Int, val failedItems: List<String> = emptyList()) : ReceiptScanState()
    data class Error(val message: String) : ReceiptScanState()
}

enum class ProcessingPhase {
    IDLE, COMPRESSING, SENDING_TO_AI, WAITING_FOR_RESPONSE, BUILDING_REVIEW, DONE
}

data class ReceiptScanUiState(
    val state: ReceiptScanState = ReceiptScanState.Idle,
    val capturedBitmap: Bitmap? = null,
    val currencySymbol: String = "",
    val units: List<UnitEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val locations: List<com.inventory.app.data.local.entity.StorageLocationEntity> = emptyList(),
    val storeName: String? = null,
    val purchaseDate: LocalDate? = null,
    val receiptTotal: Double? = null,
    val processingPhase: ProcessingPhase = ProcessingPhase.IDLE,
    val previousPageItems: List<EditableReceiptItem> = emptyList(),
    val pageCount: Int = 1
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
    private val shoppingListRepository: ShoppingListRepository,
    private val smartDefaultRepository: SmartDefaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState = _uiState.asStateFlow()

    private var nameMatchJob: Job? = null
    private var processingJob: Job? = null

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
        viewModelScope.launch {
            storageLocationRepository.getAllActive().collect { locations ->
                _uiState.update { it.copy(locations = locations) }
            }
        }
    }

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.value.capturedBitmap?.recycle()
        _uiState.update { it.copy(state = ReceiptScanState.ParsingWithAI, capturedBitmap = bitmap, processingPhase = ProcessingPhase.COMPRESSING) }
        processingJob = viewModelScope.launch {
            // Step 1: Compress and base64-encode the image
            val imageBase64 = compressAndEncode(bitmap, grokRepository.getImageConfig())
            if (imageBase64 == null) {
                bitmap.recycle()
                _uiState.update {
                    it.copy(
                        state = ReceiptScanState.Error("Failed to process the image. Please try again."),
                        processingPhase = ProcessingPhase.IDLE,
                        capturedBitmap = null
                    )
                }
                return@launch
            }

            // Step 2: Get active shopping list for matching (small payload)
            val shoppingDetails = try {
                shoppingListRepository.getActiveItems().first()
            } catch (_: Exception) {
                emptyList()
            }
            val shoppingItems = shoppingDetails.map { detail ->
                val name = detail.item?.name ?: detail.shoppingItem.customName ?: ""
                GrokRepository.ShoppingListContext(
                    id = detail.shoppingItem.id,
                    name = name
                )
            }.filter { it.name.isNotBlank() }
            // Build qty map for reconciliation warning (shopping list qty vs receipt qty)
            val shoppingQtyMap: Map<Long, Double> = shoppingDetails.associate {
                it.shoppingItem.id to it.shoppingItem.quantity
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
            _uiState.update { it.copy(processingPhase = ProcessingPhase.SENDING_TO_AI) }
            grokRepository.parseReceiptImage(imageBase64, shoppingItems, inventoryItems, categoryNames).fold(
                onSuccess = { result ->
                    _uiState.update { it.copy(processingPhase = ProcessingPhase.WAITING_FOR_RESPONSE) }
                    // Parse and build review list from AI response
                    _uiState.update { it.copy(processingPhase = ProcessingPhase.BUILDING_REVIEW) }
                    // Parse receipt-level metadata
                    val receiptDate = result.purchaseDate?.let {
                        try { LocalDate.parse(it) } catch (_: Exception) { null }
                    }

                    // Fix Gson default: missing quantity field deserializes as 0.0 instead of 1.0
                    val fixedItems = result.items.map { if (it.quantity == 0.0) it.copy(quantity = 1.0) else it }

                    // Filter out discount/refund lines the AI didn't merge into parent items
                    val filteredItems = fixedItems.filter { item ->
                        if (item.name.isBlank()) return@filter false
                        val lowerName = item.name.lowercase()
                        val isDiscount = listOf("saving", "discount", "off", "deal", "coupon", "refund", "promotion", "reward")
                            .any { keyword -> lowerName.contains(keyword) }
                        val hasNegativeOrZeroPrice = (item.price ?: 0.0) <= 0.0
                        !(isDiscount && hasNegativeOrZeroPrice)
                    }

                    // Deduplicate: merge items with same normalized name + unit
                    val deduped = filteredItems
                        .groupBy { ItemNameNormalizer.normalize(it.name) + "|" + ItemNameNormalizer.normalizeUnit(it.unit) }
                        .flatMap { (_, group) ->
                            if (group.size <= 1) group.map { it to 1 }
                            else {
                                val merged = group.first().copy(
                                    name = group.maxByOrNull { it.name.length }?.name ?: group.first().name,
                                    quantity = group.sumOf { it.quantity },
                                    price = if (group.any { it.price != null }) group.sumOf { it.price ?: 0.0 }.takeIf { it > 0 } else null,
                                    matchedShoppingId = group.firstNotNullOfOrNull { it.matchedShoppingId },
                                    matchedInventoryId = group.firstNotNullOfOrNull { it.matchedInventoryId }
                                )
                                listOf(merged to group.size)
                            }
                        }

                    // Build review list using AI-returned matches
                    // Pre-load all units for conflict detection
                    val allUnits = try { unitRepository.getAllActive().first() } catch (_: Exception) { emptyList() }
                    val allCategories = _uiState.value.categories
                    val regionCode = settingsRepository.getRegionCode()

                    val editable = deduped.map { (item, mergedCount) ->
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

                        // If AI didn't match, fall back to ProductMatcher local matching
                        val localMatchResult = if (aiMatchedItem == null && item.name.isNotBlank()) {
                            try { itemRepository.findMatchingItems(item.name) } catch (_: Exception) { null }
                        } else null
                        val localCandidates = localMatchResult?.matches?.map { match ->
                            val entity = itemRepository.getById(match.itemId)
                            val unitAbbr = entity?.unitId?.let { uid ->
                                allUnits.find { it.id == uid }?.abbreviation
                            }
                            ReceiptMatchCandidate(
                                id = match.itemId,
                                name = match.itemName,
                                currentQuantity = entity?.quantity ?: 0.0,
                                unitId = entity?.unitId,
                                unitAbbreviation = unitAbbr
                            )
                        } ?: emptyList()
                        val localBestAction = localMatchResult?.suggestedAction

                        // Resolve via 5-layer cascade with AI suggestions as hints
                        val resolved = smartDefaultRepository.resolve(
                            itemName = item.name,
                            regionCode = regionCode,
                            hints = DefaultHints(
                                categoryName = item.category,
                                unitAbbreviation = item.unit,
                                shelfLifeDays = item.estimatedExpiryDays
                            )
                        ).local
                        val resolvedUnit = resolved.unitAbbreviation ?: ""
                        val resolvedCategory = resolved.categoryName ?: ""
                        val resolvedLocation = resolved.locationName ?: ""

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

                        // Expiry date: already resolved via cascade (AI hint → static dict → cache)
                        // Guard: must be 1–1095 days (3 years); outside range → no expiry
                        val expiryDate = resolved.shelfLifeDays?.takeIf { it in 1..1095 }?.let { LocalDate.now().plusDays(it.toLong()) }

                        // Determine match type and candidates from AI match or local ProductMatcher
                        val effectiveMatchType = when {
                            aiMatchedItem != null -> ReceiptMatchType.UPDATE_EXISTING
                            localBestAction == SuggestedAction.UPDATE_EXISTING -> ReceiptMatchType.UPDATE_EXISTING
                            else -> ReceiptMatchType.CREATE_NEW
                        }
                        val effectiveCandidates = when {
                            aiMatchedItem != null -> listOf(aiMatchedItem)
                            localCandidates.isNotEmpty() -> localCandidates
                            else -> emptyList()
                        }
                        val effectiveMatchId = when {
                            aiMatchedItem != null -> aiMatchedItem.id
                            localBestAction == SuggestedAction.UPDATE_EXISTING -> localCandidates.firstOrNull()?.id
                            else -> null
                        }

                        EditableReceiptItem(
                            name = item.name,
                            quantity = qtyStr,
                            price = item.price?.let { String.format("%.2f", it) } ?: "",
                            unit = resolvedUnit,
                            categoryName = resolvedCategory,
                            matchType = effectiveMatchType,
                            matchedInventoryItemId = effectiveMatchId,
                            matchedShoppingId = item.matchedShoppingId,
                            inventoryCandidates = effectiveCandidates,
                            expiryDate = expiryDate,
                            isAiEstimatedExpiry = expiryDate != null,
                            unitConflict = unitConflict,
                            locationName = resolvedLocation,
                            resolvedCategoryId = resolved.categoryId,
                            resolvedUnitId = resolved.unitId,
                            resolvedLocationId = resolved.locationId,
                            mergedCount = mergedCount,
                            shoppingQuantity = item.matchedShoppingId?.let { shoppingQtyMap[it] },
                            isNonFood = item.isNonFood || NonFoodCategories.isNonFood(resolvedCategory)
                        )
                    }

                    // Ensure each shopping list item is matched at most once (first match wins)
                    val usedShoppingIds = mutableSetOf<Long>()
                    val dedupedEditable = editable.map { item ->
                        val shoppingId = item.matchedShoppingId
                        if (shoppingId != null && !usedShoppingIds.add(shoppingId)) {
                            item.copy(matchedShoppingId = null, shoppingQuantity = null)
                        } else item
                    }

                    // Merge with items from previous pages (cross-page dedup)
                    val previousItems = _uiState.value.previousPageItems
                    val mergedItems = if (previousItems.isNotEmpty()) {
                        mergeWithPreviousPages(previousItems, dedupedEditable)
                    } else {
                        dedupedEditable
                    }

                    val isSubsequentPage = previousItems.isNotEmpty()
                    _uiState.update { it.copy(
                        state = ReceiptScanState.Review(mergedItems),
                        storeName = if (isSubsequentPage) it.storeName ?: result.storeName else result.storeName,
                        purchaseDate = if (isSubsequentPage) it.purchaseDate else (receiptDate ?: LocalDate.now()),
                        receiptTotal = result.receiptTotal ?: it.receiptTotal,
                        processingPhase = ProcessingPhase.DONE,
                        previousPageItems = emptyList()
                    ) }
                },
                onFailure = { error ->
                    _uiState.value.capturedBitmap?.recycle()
                    _uiState.update {
                        it.copy(
                            state = ReceiptScanState.Error(
                                error.message ?: "Failed to parse receipt"
                            ),
                            processingPhase = ProcessingPhase.IDLE,
                            capturedBitmap = null
                        )
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
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
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

    // ── Atomic review item helper ────────────────────────────────────
    // All review-item mutations go through this to avoid race conditions
    // between concurrent coroutines (e.g. category resolve vs unit resolve).
    // MutableStateFlow.update uses CAS internally, so the transform always
    // reads the latest state and retries on conflict.

    private inline fun updateReviewItem(
        index: Int,
        transform: (EditableReceiptItem) -> EditableReceiptItem
    ) {
        _uiState.update { current ->
            val review = current.state as? ReceiptScanState.Review ?: return@update current
            if (index !in review.items.indices) return@update current
            val updated = review.items.toMutableList()
            updated[index] = transform(updated[index])
            current.copy(state = ReceiptScanState.Review(updated))
        }
    }

    // ── Review screen actions ─────────────────────────────────────────

    fun updateItemName(index: Int, name: String) {
        updateReviewItem(index) { it.copy(name = name) }

        // Debounced re-match against inventory
        nameMatchJob?.cancel()
        nameMatchJob = viewModelScope.launch {
            delay(300)
            reMatchItem(index, name)
        }
    }

    private suspend fun reMatchItem(index: Int, name: String) {
        if (name.isBlank()) return

        val matchResult = try {
            itemRepository.findMatchingItems(name)
        } catch (e: Exception) {
            Log.w("ReceiptScan", "Failed to match item: ${e.message}")
            null
        }

        if (matchResult != null && matchResult.matches.isNotEmpty()) {
            val allUnits = try { unitRepository.getAllActive().first() } catch (_: Exception) { emptyList<UnitEntity>() }

            // Build candidates from all matches
            val candidates = matchResult.matches.map { match ->
                val entity = try { itemRepository.getById(match.itemId) } catch (_: Exception) { null }
                val unitAbbr = entity?.unitId?.let { uid ->
                    allUnits.find { it.id == uid }?.abbreviation
                }
                ReceiptMatchCandidate(
                    id = match.itemId,
                    name = match.itemName,
                    currentQuantity = entity?.quantity ?: 0.0,
                    unitId = entity?.unitId,
                    unitAbbreviation = unitAbbr
                )
            }

            val bestCandidate = candidates.firstOrNull()
            val matchType = when (matchResult.suggestedAction) {
                SuggestedAction.UPDATE_EXISTING -> ReceiptMatchType.UPDATE_EXISTING
                else -> ReceiptMatchType.CREATE_NEW
            }

            updateReviewItem(index) { item ->
                val unitConflict = if (matchType == ReceiptMatchType.UPDATE_EXISTING) {
                    val unitAbbr = bestCandidate?.unitAbbreviation
                    if (unitAbbr != null && item.unit.isNotBlank()) {
                        if (!unitAbbr.equals(item.unit, ignoreCase = true)) "Inventory uses $unitAbbr" else null
                    } else null
                } else null

                item.copy(
                    matchType = matchType,
                    matchedInventoryItemId = if (matchType == ReceiptMatchType.UPDATE_EXISTING) bestCandidate?.id else null,
                    inventoryCandidates = candidates,
                    unitConflict = unitConflict
                )
            }
        } else {
            // No match found — switch to CREATE_NEW only if currently UPDATE_EXISTING
            updateReviewItem(index) { item ->
                if (item.matchType == ReceiptMatchType.UPDATE_EXISTING) {
                    item.copy(
                        matchType = ReceiptMatchType.CREATE_NEW,
                        matchedInventoryItemId = null,
                        inventoryCandidates = emptyList(),
                        unitConflict = null
                    )
                } else item
            }
        }
    }

    fun updateItemQuantity(index: Int, quantity: String) {
        updateReviewItem(index) { it.copy(quantity = quantity) }
    }

    fun updateItemPrice(index: Int, price: String) {
        updateReviewItem(index) { it.copy(price = price) }
    }

    fun updateItemCategory(index: Int, categoryName: String) {
        updateReviewItem(index) { it.copy(categoryName = categoryName) }

        // Resolve the new category name to its DB ID
        viewModelScope.launch {
            val catEntity = categoryRepository.findCategoryByNameIgnoreCase(categoryName)
            updateReviewItem(index) { it.copy(resolvedCategoryId = catEntity?.id) }
        }
    }

    fun updateItemUnit(index: Int, unit: String) {
        updateReviewItem(index) { it.copy(unit = unit, unitConflict = null) }

        // Resolve the new unit abbreviation to its DB ID
        viewModelScope.launch {
            val unitEntity = unitRepository.findByAbbreviation(unit) ?: unitRepository.findByName(unit)
            updateReviewItem(index) { it.copy(resolvedUnitId = unitEntity?.id) }
        }
    }

    fun updateItemLocation(index: Int, locationName: String) {
        updateReviewItem(index) { it.copy(locationName = locationName) }

        // Resolve the new location name to its DB ID
        viewModelScope.launch {
            val locEntity = storageLocationRepository.findByName(locationName)
            updateReviewItem(index) { it.copy(resolvedLocationId = locEntity?.id) }
        }
    }

    fun removeItem(index: Int) {
        _uiState.update { current ->
            val review = current.state as? ReceiptScanState.Review ?: return@update current
            if (index !in review.items.indices) return@update current
            val updated = review.items.toMutableList()
            updated.removeAt(index)
            if (updated.isEmpty()) {
                current.copy(state = ReceiptScanState.Error("All items removed. Try scanning again."))
            } else {
                current.copy(state = ReceiptScanState.Review(updated))
            }
        }
    }

    fun updateItemExpiryDate(index: Int, date: LocalDate?) {
        updateReviewItem(index) { it.copy(expiryDate = date, isAiEstimatedExpiry = false) }
    }

    fun updateItemBarcode(index: Int, barcode: String) {
        updateReviewItem(index) { it.copy(barcode = barcode) }
    }

    fun addBlankItem(): Int {
        var newIndex = -1
        _uiState.update { current ->
            val review = current.state as? ReceiptScanState.Review ?: return@update current
            val blank = EditableReceiptItem(name = "", quantity = "1", matchType = ReceiptMatchType.CREATE_NEW)
            val updated = review.items + blank
            newIndex = updated.size - 1
            current.copy(state = ReceiptScanState.Review(updated))
        }
        return newIndex
    }

    fun markAsReviewed(index: Int) {
        updateReviewItem(index) { item ->
            if (!item.isReviewed) item.copy(isReviewed = true) else item
        }
    }

    fun markAllReviewed() {
        _uiState.update { current ->
            val review = current.state as? ReceiptScanState.Review ?: return@update current
            val updated = review.items.map { it.copy(isReviewed = true) }
            current.copy(state = ReceiptScanState.Review(updated))
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

        // Receipt-level metadata
        val storeName = _uiState.value.storeName
        val purchaseDate = _uiState.value.purchaseDate ?: LocalDate.now()

        // Set Saving state synchronously to prevent double-tap
        _uiState.update { it.copy(state = ReceiptScanState.Saving(0, items.size)) }

        viewModelScope.launch {
            val total = items.size

            var addedCount = 0
            val failedNames = mutableListOf<String>()
            for ((index, item) in items.withIndex()) {
                _uiState.update { it.copy(state = ReceiptScanState.Saving(index + 1, total)) }
                try {
                val qty = item.quantity.toDoubleOrNull()
                if (qty == null || qty <= 0) {
                    failedNames.add("${item.name} (invalid quantity)")
                    continue
                }
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
                                purchaseDate = purchaseDate,
                                expiryDate = item.expiryDate,
                                storeName = storeName,
                                notes = "From receipt scan"
                            )
                        } else {
                            // No price — just adjust quantity (no purchase history)
                            itemRepository.adjustQuantity(existingId, qty)
                            itemRepository.updatePurchaseDate(existingId, purchaseDate)
                        }
                    }
                    ReceiptMatchType.CREATE_NEW -> {
                        // Use IDs stored during first resolve — no second resolve needed
                        val categoryId = item.resolvedCategoryId
                        val unitId = item.resolvedUnitId
                        val locationId = item.resolvedLocationId

                        val entity = ItemEntity(
                            name = item.name,
                            quantity = if (price != null) 0.0 else qty,  // addPurchase will adjust quantity when price is present
                            categoryId = categoryId,
                            unitId = unitId,
                            storageLocationId = locationId,
                            purchasePrice = price,
                            purchaseDate = purchaseDate,
                            expiryDate = item.expiryDate,
                            barcode = item.barcode.ifBlank { null }
                        )
                        val newItemId = itemRepository.insert(entity)

                        if (price != null) {
                            purchaseRepository.addPurchase(
                                itemId = newItemId,
                                quantity = qty,
                                totalPrice = price,
                                purchaseDate = purchaseDate,
                                expiryDate = item.expiryDate,
                                storeName = storeName,
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
                } catch (e: Exception) {
                    Log.w("ReceiptScan", "Failed to save item: ${item.name}", e)
                    failedNames.add(item.name)
                }
            }

            _uiState.update { it.copy(state = ReceiptScanState.Success(addedCount, failedNames)) }
        }
    }

    fun updateStoreName(name: String) {
        _uiState.update { it.copy(storeName = name.ifBlank { null }) }
    }

    fun updatePurchaseDate(date: LocalDate) {
        _uiState.update { it.copy(purchaseDate = date) }
    }

    fun reset() {
        _uiState.value.capturedBitmap?.recycle()
        _uiState.update { ReceiptScanUiState(currencySymbol = it.currencySymbol) }
    }

    fun scanAnotherPage() {
        val state = _uiState.value.state as? ReceiptScanState.Review ?: return
        _uiState.update {
            it.copy(
                previousPageItems = it.previousPageItems + state.items,
                state = ReceiptScanState.Idle,
                capturedBitmap = null,
                processingPhase = ProcessingPhase.IDLE,
                pageCount = it.pageCount + 1
            )
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _uiState.value.capturedBitmap?.recycle()
        val previousItems = _uiState.value.previousPageItems
        _uiState.update {
            if (previousItems.isNotEmpty()) {
                it.copy(
                    state = ReceiptScanState.Review(previousItems),
                    capturedBitmap = null,
                    processingPhase = ProcessingPhase.IDLE,
                    previousPageItems = emptyList(),
                    pageCount = it.pageCount - 1
                )
            } else {
                it.copy(
                    state = ReceiptScanState.Idle,
                    capturedBitmap = null,
                    processingPhase = ProcessingPhase.IDLE
                )
            }
        }
    }

    private fun mergeWithPreviousPages(
        previous: List<EditableReceiptItem>,
        current: List<EditableReceiptItem>
    ): List<EditableReceiptItem> {
        val combined = previous + current
        val grouped = combined.groupBy {
            ItemNameNormalizer.normalize(it.name) + "|" + ItemNameNormalizer.normalizeUnit(it.unit)
        }
        return grouped.flatMap { (_, group) ->
            if (group.size <= 1) group
            else {
                val base = group.first()
                val totalQty = group.sumOf { it.quantity.toDoubleOrNull() ?: 1.0 }
                val qtyStr = if (totalQty == totalQty.toLong().toDouble()) {
                    totalQty.toLong().toString()
                } else {
                    String.format("%.3f", totalQty).trimEnd('0').trimEnd('.')
                }
                val totalPrice = group.mapNotNull { it.price.toDoubleOrNull() }
                    .takeIf { it.isNotEmpty() }?.sum()
                listOf(base.copy(
                    name = group.maxByOrNull { it.name.length }?.name ?: base.name,
                    quantity = qtyStr,
                    price = totalPrice?.let { String.format("%.2f", it) } ?: "",
                    matchedShoppingId = group.firstNotNullOfOrNull { it.matchedShoppingId },
                    matchedInventoryItemId = group.firstNotNullOfOrNull { it.matchedInventoryItemId },
                    inventoryCandidates = group.flatMap { it.inventoryCandidates }.distinctBy { it.id },
                    mergedCount = group.sumOf { it.mergedCount }
                ))
            }
        }
    }

    override fun onCleared() {
        processingJob?.cancel()
        _uiState.value.capturedBitmap?.recycle()
        super.onCleared()
    }
}
