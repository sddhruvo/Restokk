package com.inventory.app.ui.screens.recognition

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.UnitEntity
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.FridgeItem
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.repository.UnitRepository
import com.inventory.app.domain.model.SmartDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

// ── Kitchen area definitions (UI-only, maps to existing StorageLocationEntity names) ──

data class KitchenArea(
    val id: String,
    val name: String,
    val description: String,
    val locationName: String,  // maps to StorageLocationEntity.name
    val aiLabel: String,       // short area name passed to AI prompt as {AREA}
    val icon: String,          // material icon name hint for UI
    val scanHints: String,     // area-specific scanning instructions for AI
    val minExpected: Int        // realistic minimum item count for this area
) {
    companion object {
        val KITCHEN_AREAS = listOf(
            KitchenArea(
                "fridge_shelves", "Fridge (Shelves)", "Main shelves & drawers",
                "Refrigerator", "refrigerator", "kitchen",
                scanHints = "Scan shelf by shelf, top to bottom, left to right, front to back. For EACH shelf: what's in front? What's behind? What's inside bags/containers? Check crisper drawers for produce. Commonly missed: fresh herbs (cilantro, parsley), fruits in drawers, small items behind bottles (garlic, ginger), different cabbage types — list each separately.",
                minExpected = 10
            ),
            KitchenArea(
                "fridge_door", "Fridge Door", "Condiments, drinks, sauces",
                "Refrigerator", "refrigerator door", "door_front",
                scanHints = "Check each door shelf top to bottom. Focus on condiments, sauces, drinks, small bottles, jars, and tubes. Read labels where visible. Commonly missed: small sauce packets, butter/cheese in door compartments, partially hidden bottles behind others.",
                minExpected = 4
            ),
            KitchenArea(
                "freezer", "Freezer", "Frozen items & ice cream",
                "Freezer", "freezer", "ac_unit",
                scanHints = "Items may be in bags, boxes, or wrapped in foil. Look through frost. Identify by packaging shape, color, and any visible text. Stacked items are common. Commonly missed: ice cream tubs at the back, frozen vegetables in bags, items wedged between others.",
                minExpected = 6
            ),
            KitchenArea(
                "pantry", "Pantry / Cabinet", "Dry goods & canned items",
                "Pantry", "pantry", "shelves",
                scanHints = "Scan shelf by shelf, left to right. Look for cans, boxes, bags, jars, packets, and bottles. Read labels where possible. Items may be stacked or behind each other. Commonly missed: items pushed to the back, small spice packets, tea/coffee boxes, cooking oils.",
                minExpected = 8
            ),
            KitchenArea(
                "counter", "Counter / Fruit Bowl", "Fresh items on counter",
                "Counter", "kitchen counter", "countertops",
                scanHints = "Scan left to right across the counter surface. Look for fruit bowls, bread, loose produce, bottles, jars. Counters may have only a few items — that's normal. Commonly missed: individual fruits in a bowl (count each type separately), items near the edges.",
                minExpected = 3
            ),
            KitchenArea(
                "spice_rack", "Spice Rack", "Spices & seasonings",
                "Spice Rack", "spice rack", "local_fire_department",
                scanHints = "Read spice jar labels carefully. Small jars, bottles, sachets, and grinders. Items may be tightly packed. Identify by label text, jar color, or spice color if visible. Commonly missed: small sachets, items in the back row, unlabeled containers.",
                minExpected = 5
            )
        )
    }
}

enum class FridgeMatchType {
    CREATE_NEW,
    UPDATE_EXISTING,
    SKIP
}

data class FridgeMatchCandidate(
    val id: Long,
    val name: String,
    val currentQuantity: Double,
    val unitId: Long?,
    val unitAbbreviation: String? = null
)

data class EditableFridgeItem(
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "",
    val categoryName: String = "",
    val confidence: String = "medium",
    val matchType: FridgeMatchType = FridgeMatchType.CREATE_NEW,
    val matchedInventoryItemId: Long? = null,
    val inventoryCandidates: List<FridgeMatchCandidate> = emptyList(),
    val expiryDate: LocalDate? = null,
    val isAiEstimatedExpiry: Boolean = false,
    val storageLocationId: Long? = null,
    val dupWarning: String? = null  // "Also found in Fridge (Shelves)"
)

sealed class FridgeScanState {
    data object AreaSelection : FridgeScanState()
    data object Idle : FridgeScanState()
    data object Processing : FridgeScanState()
    data class Review(val items: List<EditableFridgeItem>) : FridgeScanState()
    data class Saving(val current: Int, val total: Int) : FridgeScanState()
    data class AreaSuccess(val count: Int, val areaName: String?) : FridgeScanState()
    data class TourSummary(
        val areaResults: Map<String, Int>,  // area name → item count
        val totalItems: Int,
        val categoryBreakdown: Map<String, Int> = emptyMap()  // category → count
    ) : FridgeScanState()
    data class Error(val message: String) : FridgeScanState()
}

data class FridgeScanUiState(
    val state: FridgeScanState = FridgeScanState.AreaSelection,
    val capturedBitmap: Bitmap? = null,
    val units: List<UnitEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    // Tour state
    val currentArea: KitchenArea? = null,
    val completedAreas: Map<String, Int> = emptyMap(),  // areaId → items saved
    val allScannedItemNames: List<Pair<String, String>> = emptyList(),  // (itemName, areaName) for dedup
    val isInTourMode: Boolean = false,
    val allScannedCategories: Map<String, Int> = emptyMap()  // category → count
)

@HiltViewModel
class FridgeScanViewModel @Inject constructor(
    private val grokRepository: GrokRepository,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val storageLocationRepository: StorageLocationRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FridgeScanUiState())
    val uiState = _uiState.asStateFlow()

    private var nameMatchJob: Job? = null

    override fun onCleared() {
        _uiState.value.capturedBitmap?.recycle()
        super.onCleared()
    }

    init {
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

    // ── Area selection & tour flow ────────────────────────────────────

    fun selectArea(areaId: String) {
        val area = KitchenArea.KITCHEN_AREAS.find { it.id == areaId } ?: return
        viewModelScope.launch {
            val locationId = try {
                storageLocationRepository.findByName(area.locationName)?.id
            } catch (_: Exception) { null }
            _uiState.update {
                it.copy(
                    state = FridgeScanState.Idle,
                    currentArea = area.copy(), // store resolved area
                    isInTourMode = true
                )
            }
            // Store locationId for later use — we save it when building items
            resolvedLocationId = locationId
        }
    }

    fun quickScan() {
        resolvedLocationId = null
        _uiState.update {
            it.copy(
                state = FridgeScanState.Idle,
                currentArea = null,
                isInTourMode = false
            )
        }
    }

    fun continueToNextArea() {
        _uiState.update {
            it.copy(
                state = FridgeScanState.AreaSelection,
                capturedBitmap = null
            )
        }
    }

    fun finishTour() {
        val current = _uiState.value
        val areaResults = mutableMapOf<String, Int>()
        for ((areaId, count) in current.completedAreas) {
            val areaName = KitchenArea.KITCHEN_AREAS.find { it.id == areaId }?.name ?: areaId
            areaResults[areaName] = count
        }
        _uiState.update {
            it.copy(state = FridgeScanState.TourSummary(
                areaResults = areaResults,
                totalItems = areaResults.values.sum(),
                categoryBreakdown = it.allScannedCategories
            ))
        }
        // Persist scan summary for dashboard badge
        viewModelScope.launch {
            settingsRepository.setInt("last_scan_item_count", areaResults.values.sum())
            settingsRepository.setInt("last_scan_area_count", areaResults.size)
        }
    }

    @Volatile
    private var resolvedLocationId: Long? = null

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(state = FridgeScanState.Processing, capturedBitmap = bitmap) }
        viewModelScope.launch {
            val imageBase64 = compressAndEncode(bitmap, grokRepository.getImageConfig())
            if (imageBase64 == null) {
                _uiState.update {
                    it.copy(state = FridgeScanState.Error("Failed to process the image. Please try again."))
                }
                return@launch
            }

            val currentUiState = _uiState.value

            // Get category names (for structured output only — NOT to influence what AI finds)
            val categoryNames = currentUiState.categories.map { it.name }

            // Use area-specific AI label, or generic "refrigerator" for quick scan
            val areaLabel = currentUiState.currentArea?.aiLabel ?: "refrigerator"
            val areaHints = currentUiState.currentArea?.scanHints ?: ""
            val minExpected = currentUiState.currentArea?.minExpected ?: 12

            // Pass previously found items for cross-area dedup
            val previousItems = currentUiState.allScannedItemNames.map { it.first }

            grokRepository.parseFridgeImage(
                imageBase64 = imageBase64,
                area = areaLabel,
                categoryNames = categoryNames,
                previouslyFoundItems = previousItems,
                areaHints = areaHints,
                minExpectedItems = minExpected
            ).fold(
                onSuccess = { items ->
                    val fixedItems = items.map { if (it.quantity == 0.0) it.copy(quantity = 1.0) else it }
                    val allUnits = try { unitRepository.getAllActive().first() } catch (_: Exception) { emptyList() }
                    val allCategories = currentUiState.categories

                    val editable = fixedItems.map { item ->
                        buildEditableItem(item, allUnits, allCategories)
                    }
                    _uiState.update { it.copy(state = FridgeScanState.Review(editable)) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(state = FridgeScanState.Error(
                            error.message ?: "Failed to identify items"
                        ))
                    }
                }
            )
        }
    }

    private suspend fun buildEditableItem(
        item: FridgeItem,
        allUnits: List<UnitEntity>,
        allCategories: List<CategoryEntity>
    ): EditableFridgeItem {
        val qtyStr = if (item.quantity == item.quantity.toLong().toDouble()) {
            item.quantity.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", item.quantity)
        }

        val defaults = SmartDefaults.lookup(item.name)

        // Unit: AI → SmartDefaults fallback
        val resolvedUnit = if (!item.unit.isNullOrBlank()) {
            item.unit
        } else {
            defaults?.unit ?: "pcs"
        }

        // Category: AI → SmartDefaults fallback
        val resolvedCategory = run {
            val aiCat = item.category
            if (!aiCat.isNullOrBlank()) {
                val match = allCategories.find { it.name.equals(aiCat, ignoreCase = true) }
                if (match != null) return@run match.name
            }
            val defaultCat = defaults?.category
            if (!defaultCat.isNullOrBlank()) {
                val match = allCategories.find { it.name.equals(defaultCat, ignoreCase = true) }
                if (match != null) return@run match.name
            }
            ""
        }

        // Check for inventory match
        val matchedEntity = itemRepository.findByName(item.name)
        val candidate = matchedEntity?.let {
            val unitAbbr = it.unitId?.let { uid -> allUnits.find { u -> u.id == uid }?.abbreviation }
            FridgeMatchCandidate(it.id, it.name, it.quantity, it.unitId, unitAbbr)
        }

        // Expiry date: AI estimate → SmartDefaults fallback
        val expiryDays = item.estimatedExpiryDays ?: defaults?.shelfLifeDays
        val expiryDate = expiryDays?.let { LocalDate.now().plusDays(it.toLong()) }

        // Cross-area dedup: check if this item was already found in another area
        val previousItems = _uiState.value.allScannedItemNames
        val dupArea = previousItems.find { (prevName, _) ->
            prevName.equals(item.name, ignoreCase = true)
        }?.second

        return EditableFridgeItem(
            name = item.name,
            quantity = qtyStr,
            unit = resolvedUnit,
            categoryName = resolvedCategory,
            confidence = item.confidence,
            matchType = if (candidate != null) FridgeMatchType.UPDATE_EXISTING else FridgeMatchType.CREATE_NEW,
            matchedInventoryItemId = candidate?.id,
            inventoryCandidates = if (candidate != null) listOf(candidate) else emptyList(),
            expiryDate = expiryDate,
            isAiEstimatedExpiry = expiryDate != null,
            storageLocationId = resolvedLocationId,
            dupWarning = dupArea?.let { "Also found in $it" }
        )
    }

    // ── Image compression (same pattern as ReceiptScanViewModel) ──────

    private fun compressAndEncode(bitmap: Bitmap, config: com.inventory.app.data.repository.ImageConfig): String? {
        return try {
            Log.d("FridgeScan", "Original image: ${bitmap.width}x${bitmap.height}, maxDim=${config.maxDimension}")

            var scaled = if (bitmap.width > config.maxDimension || bitmap.height > config.maxDimension) {
                val scale = config.maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
            } else bitmap

            var quality = config.startQuality
            var stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            var bytes = stream.toByteArray()
            Log.d("FridgeScan", "After scale to ${scaled.width}x${scaled.height}, q=$quality: ${bytes.size / 1024}KB")

            while (bytes.size > config.maxBytes && quality > config.minQuality) {
                quality -= 10
                stream.reset() // Reuse stream instead of allocating new one
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                bytes = stream.toByteArray()
                Log.d("FridgeScan", "Re-compressed q=$quality: ${bytes.size / 1024}KB")
            }

            if (bytes.size > config.maxBytes) {
                val oldScaled = scaled
                scaled = Bitmap.createScaledBitmap(scaled, (scaled.width * config.fallbackScale).toInt().coerceAtLeast(1), (scaled.height * config.fallbackScale).toInt().coerceAtLeast(1), true)
                if (oldScaled !== bitmap) oldScaled.recycle()
                stream.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                bytes = stream.toByteArray()
                Log.d("FridgeScan", "Final resize to ${scaled.width}x${scaled.height}: ${bytes.size / 1024}KB")
            }

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d("FridgeScan", "Base64 encoded: ${base64.length} chars (~${base64.length * 3 / 4 / 1024}KB)")

            if (scaled !== bitmap) scaled.recycle()
            base64
        } catch (e: Exception) {
            Log.e("FridgeScan", "Image compression failed", e)
            null
        }
    }

    // ── Review screen actions ─────────────────────────────────────────

    fun updateItemName(index: Int, name: String) {
        val state = _uiState.value.state
        if (state is FridgeScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(name = name)
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }

            nameMatchJob?.cancel()
            nameMatchJob = viewModelScope.launch {
                delay(300)
                reMatchItem(index, name)
            }
        }
    }

    private suspend fun reMatchItem(index: Int, name: String) {
        if (name.isBlank()) return
        val currentState = _uiState.value.state
        if (currentState !is FridgeScanState.Review) return
        if (index !in currentState.items.indices) return

        val currentItem = currentState.items[index]
        val matchedEntity = itemRepository.findByName(name)

        if (matchedEntity != null) {
            val allUnits = try { unitRepository.getAllActive().first() } catch (_: Exception) { emptyList<UnitEntity>() }
            val unitAbbr = matchedEntity.unitId?.let { uid -> allUnits.find { it.id == uid }?.abbreviation }
            val candidate = FridgeMatchCandidate(
                id = matchedEntity.id,
                name = matchedEntity.name,
                currentQuantity = matchedEntity.quantity,
                unitId = matchedEntity.unitId,
                unitAbbreviation = unitAbbr
            )
            val updated = (_uiState.value.state as? FridgeScanState.Review)?.items?.toMutableList() ?: return
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(
                matchType = FridgeMatchType.UPDATE_EXISTING,
                matchedInventoryItemId = matchedEntity.id,
                inventoryCandidates = listOf(candidate)
            )
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
        } else if (currentItem.matchType == FridgeMatchType.UPDATE_EXISTING) {
            val updated = (_uiState.value.state as? FridgeScanState.Review)?.items?.toMutableList() ?: return
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(
                matchType = FridgeMatchType.CREATE_NEW,
                matchedInventoryItemId = null,
                inventoryCandidates = emptyList()
            )
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
        }
    }

    fun updateItemQuantity(index: Int, quantity: String) {
        val state = _uiState.value.state
        if (state is FridgeScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(quantity = quantity)
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
        }
    }

    fun updateItemUnit(index: Int, unit: String) {
        val state = _uiState.value.state
        if (state is FridgeScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(unit = unit)
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
        }
    }

    fun updateItemCategory(index: Int, categoryName: String) {
        val state = _uiState.value.state
        if (state is FridgeScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(categoryName = categoryName)
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
        }
    }

    fun updateMatchType(index: Int, matchType: FridgeMatchType, selectedItemId: Long? = null) {
        val state = _uiState.value.state
        if (state is FridgeScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated[index] = updated[index].copy(
                matchType = matchType,
                matchedInventoryItemId = if (matchType == FridgeMatchType.UPDATE_EXISTING) selectedItemId else null
            )
            _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
        }
    }

    fun removeItem(index: Int) {
        val state = _uiState.value.state
        if (state is FridgeScanState.Review) {
            val updated = state.items.toMutableList()
            if (index !in updated.indices) return
            updated.removeAt(index)
            if (updated.isEmpty()) {
                _uiState.update { it.copy(state = FridgeScanState.Error("All items removed. Try scanning again.")) }
            } else {
                _uiState.update { it.copy(state = FridgeScanState.Review(updated)) }
            }
        }
    }

    // ── Save to inventory ─────────────────────────────────────────────

    fun addAllToInventory() {
        val state = _uiState.value.state
        if (state !is FridgeScanState.Review) return
        val items = state.items.filter { it.name.isNotBlank() && it.matchType != FridgeMatchType.SKIP }
        if (items.isEmpty()) return

        _uiState.update { it.copy(state = FridgeScanState.Saving(0, items.size)) }

        viewModelScope.launch {
            var addedCount = 0
            val currentUiState = _uiState.value
            val areaName = currentUiState.currentArea?.name ?: "Quick Scan"

            for ((index, item) in items.withIndex()) {
                _uiState.update { it.copy(state = FridgeScanState.Saving(index + 1, items.size)) }
                try {
                val qty = item.quantity.toDoubleOrNull() ?: 1.0

                when (item.matchType) {
                    FridgeMatchType.UPDATE_EXISTING -> {
                        val existingId = item.matchedInventoryItemId ?: continue
                        if (item.expiryDate != null) {
                            itemRepository.updateExpiryDate(existingId, item.expiryDate)
                        }
                        // Add scanned quantity to existing inventory
                        itemRepository.adjustQuantity(existingId, qty)
                        purchaseHistoryDao.insert(
                            PurchaseHistoryEntity(
                                itemId = existingId,
                                quantity = qty,
                                purchaseDate = LocalDate.now(),
                                notes = "From kitchen scan"
                            )
                        )
                    }
                    FridgeMatchType.CREATE_NEW -> {
                        val defaults = SmartDefaults.lookup(item.name)
                        val categoryId = if (item.categoryName.isNotBlank()) {
                            categoryRepository.findCategoryByNameIgnoreCase(item.categoryName)?.id
                        } else {
                            defaults?.category?.let { categoryRepository.findCategoryByName(it)?.id }
                        }
                        val unitId = if (item.unit.isNotBlank()) {
                            unitRepository.findByAbbreviation(item.unit)?.id
                                ?: unitRepository.findByName(item.unit)?.id
                        } else {
                            defaults?.unit?.let { unitRepository.findByAbbreviation(it)?.id }
                        }
                        // Area-based location takes priority, then SmartDefaults fallback
                        val locationId = item.storageLocationId
                            ?: defaults?.location?.let { storageLocationRepository.findByName(it)?.id }

                        val entity = ItemEntity(
                            name = item.name,
                            quantity = qty,
                            categoryId = categoryId,
                            unitId = unitId,
                            storageLocationId = locationId,
                            expiryDate = item.expiryDate
                        )
                        val newId = itemRepository.insert(entity)
                        purchaseHistoryDao.insert(
                            PurchaseHistoryEntity(
                                itemId = newId,
                                quantity = qty,
                                purchaseDate = LocalDate.now(),
                                notes = "From kitchen scan"
                            )
                        )
                    }
                    FridgeMatchType.SKIP -> { /* filtered out */ }
                }
                addedCount++
                } catch (e: Exception) {
                    Log.w("FridgeScan", "Failed to save item: ${item.name}", e)
                }
            }

            // Record items in tour tracking
            val newScannedItems = items.map { it.name to areaName }
            val currentAreaId = currentUiState.currentArea?.id

            // Tally categories from saved items
            val categoryTally = items.groupBy { it.categoryName.ifBlank { "Other" } }
                .mapValues { it.value.size }

            _uiState.update {
                val updatedCompleted = if (currentAreaId != null) {
                    it.completedAreas + (currentAreaId to addedCount)
                } else it.completedAreas

                // Merge category tallies
                val mergedCategories = it.allScannedCategories.toMutableMap()
                categoryTally.forEach { (cat, count) ->
                    mergedCategories[cat] = (mergedCategories[cat] ?: 0) + count
                }

                it.copy(
                    state = FridgeScanState.AreaSuccess(addedCount, it.currentArea?.name),
                    allScannedItemNames = it.allScannedItemNames + newScannedItems,
                    completedAreas = updatedCompleted,
                    allScannedCategories = mergedCategories
                )
            }

            // Persist scan summary for dashboard badge (quick scan / non-tour mode)
            if (currentUiState.currentArea == null) {
                val existingCount = settingsRepository.getInt("last_scan_item_count", 0)
                settingsRepository.setInt("last_scan_item_count", existingCount + addedCount)
                val existingAreas = settingsRepository.getInt("last_scan_area_count", 0)
                settingsRepository.setInt("last_scan_area_count", existingAreas + 1)
            }
        }
    }

    /** Full reset — clears everything including tour state (for "Done" exit) */
    fun reset() {
        resolvedLocationId = null
        _uiState.update {
            FridgeScanUiState(
                units = it.units,
                categories = it.categories
            )
        }
    }

    /** Return to area selection in tour mode (from Idle/Review back press) */
    fun returnToAreaSelection() {
        _uiState.update {
            it.copy(
                state = FridgeScanState.AreaSelection,
                capturedBitmap = null
            )
        }
    }
}
