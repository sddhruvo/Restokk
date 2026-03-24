package com.inventory.app.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.repository.SmartDefaultRepository
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.local.entity.SubcategoryEntity
import com.inventory.app.data.local.entity.UnitEntity
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.repository.UnitRepository
import com.inventory.app.domain.model.ProductMatcher
import com.inventory.app.domain.model.ProductMatchResult
import com.inventory.app.domain.model.ResolvedDefaults
import com.inventory.app.domain.model.ShoppingMatch
import com.inventory.app.domain.model.SmartDefaults
import com.inventory.app.domain.model.SuggestedAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ItemFormUiState(
    val name: String = "",
    val description: String = "",
    val barcode: String = "",
    val brand: String = "",
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val selectedLocationId: Long? = null,
    val selectedUnitId: Long? = null,
    val quantity: String = "1",
    val minQuantity: String = "",
    val maxQuantity: String = "",
    val expiryDate: String = "",
    val expiryWarningDays: String = "7",
    val openedDate: String = "",
    val daysAfterOpening: String = "",
    val purchaseDate: String = LocalDate.now().toString(),
    val purchasePrice: String = "",
    val isFavorite: Boolean = false,
    val isPaused: Boolean = false,
    val notes: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val locations: List<StorageLocationEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val nameError: String? = null,
    val quantityError: String? = null,
    val minQuantityError: String? = null,
    val maxQuantityError: String? = null,
    val priceError: String? = null,
    val expiryDateError: String? = null,
    val openedDateError: String? = null,
    val isEditing: Boolean = false,
    val editingId: Long? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null,
    val savedItemId: Long? = null,
    val smartDefaultsApplied: Boolean = false,
    val nameSuggestions: List<String> = emptyList(),
    val autoStrikedItems: List<String> = emptyList(),
    val pendingMatches: List<ShoppingMatch> = emptyList(),
    val currencySymbol: String = "",
    val createdAt: java.time.LocalDateTime? = null,
    val isScanningExpiry: Boolean = false,
    val expiryScanError: String? = null,
    // Smart default correction tracking — records what was auto-filled from server-sourced layers
    val smartDefaultAppliedCategory: String? = null,
    val smartDefaultAppliedSubcategory: String? = null,
    val smartDefaultAppliedLocation: String? = null,
    val smartDefaultAppliedUnit: String? = null,
    val smartDefaultSource: String? = null,  // "static" | "cache" | "remote"
    // SI-1: Expiry date auto-fill tracking
    val expiryDateAutoFilled: Boolean = false,
    val smartDefaultAppliedExpiryDays: Int? = null,
    // SI-2: Quantity auto-fill tracking
    val quantityAutoFilled: Boolean = false,
    // ProductMatcher: duplicate detection
    val duplicateMatches: List<ProductMatcher.MatchCandidate> = emptyList(),
    val duplicateSuggestedAction: SuggestedAction = SuggestedAction.CREATE_NEW
)

@HiltViewModel
class ItemFormViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository,
    private val unitRepository: UnitRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val shoppingListRepository: ShoppingListRepository,
    private val settingsRepository: SettingsRepository,
    private val grokRepository: GrokRepository,
    private val smartDefaultRepository: SmartDefaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemFormUiState())
    val uiState = _uiState.asStateFlow()

    private data class FormSnapshot(
        val name: String = "",
        val description: String = "",
        val barcode: String = "",
        val brand: String = "",
        val selectedCategoryId: Long? = null,
        val selectedSubcategoryId: Long? = null,
        val selectedLocationId: Long? = null,
        val selectedUnitId: Long? = null,
        val quantity: String = "1",
        val minQuantity: String = "",
        val maxQuantity: String = "",
        val expiryDate: String = "",
        val expiryWarningDays: String = "7",
        val openedDate: String = "",
        val daysAfterOpening: String = "",
        val purchaseDate: String = LocalDate.now().toString(),
        val purchasePrice: String = "",
        val isFavorite: Boolean = false,
        val isPaused: Boolean = false,
        val notes: String = ""
    )

    private fun ItemFormUiState.toFormSnapshot() = FormSnapshot(
        name = name,
        description = description,
        barcode = barcode,
        brand = brand,
        selectedCategoryId = selectedCategoryId,
        selectedSubcategoryId = selectedSubcategoryId,
        selectedLocationId = selectedLocationId,
        selectedUnitId = selectedUnitId,
        quantity = quantity,
        minQuantity = minQuantity,
        maxQuantity = maxQuantity,
        expiryDate = expiryDate,
        expiryWarningDays = expiryWarningDays,
        openedDate = openedDate,
        daysAfterOpening = daysAfterOpening,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        isFavorite = isFavorite,
        isPaused = isPaused,
        notes = notes
    )

    private var savedSnapshot = FormSnapshot()

    val isDirty: StateFlow<Boolean> = _uiState
        .map { it.toFormSnapshot() != savedSnapshot }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Track which fields the user has EXPLICITLY interacted with (tapped dropdown, typed value).
    // Distinguished from "auto-filled by smart defaults" — only explicit user interaction counts.
    // This set persists across name changes so user choices are never overwritten.
    private val userTouchedFields = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    // Field name constants defined in companion object below

    private var smartDefaultJob: Job? = null
    private var remoteDefaultJob: Job? = null
    private var suggestionJob: Job? = null
    private var subcategoryJob: Job? = null
    private var duplicateCheckJob: Job? = null
    private var regionCode: String = "US"

    init {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
            regionCode = settingsRepository.getRegionCode()
            _uiState.update { it.copy(currencySymbol = currency) }
        }
        viewModelScope.launch {
            categoryRepository.getAllActive().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            locationRepository.getAllActive().collect { locs ->
                _uiState.update { it.copy(locations = locs) }
            }
        }
        viewModelScope.launch {
            unitRepository.getAllActive().collect { units ->
                _uiState.update { it.copy(units = units) }
            }
        }
    }

    fun loadItem(id: Long) {
        // Mark all as user-touched when editing (don't auto-change anything)
        userTouchedFields.addAll(listOf(
            FIELD_CATEGORY, FIELD_SUBCATEGORY, FIELD_LOCATION,
            FIELD_UNIT, FIELD_EXPIRY, FIELD_QUANTITY, FIELD_PRICE
        ))

        viewModelScope.launch {
            itemRepository.getById(id)?.let { item ->
                _uiState.update {
                    it.copy(
                        name = item.name,
                        description = item.description ?: "",
                        barcode = item.barcode ?: "",
                        brand = item.brand ?: "",
                        selectedCategoryId = item.categoryId,
                        selectedSubcategoryId = item.subcategoryId,
                        selectedLocationId = item.storageLocationId,
                        selectedUnitId = item.unitId,
                        quantity = item.quantity.formatForInput(),
                        minQuantity = item.minQuantity.formatForInput(),
                        maxQuantity = item.maxQuantity?.formatForInput() ?: "",
                        expiryDate = item.expiryDate?.toString() ?: "",
                        expiryWarningDays = item.expiryWarningDays.toString(),
                        openedDate = item.openedDate?.toString() ?: "",
                        daysAfterOpening = item.daysAfterOpening?.toString() ?: "",
                        purchaseDate = item.purchaseDate?.toString() ?: "",
                        purchasePrice = item.purchasePrice?.let { "%.2f".format(it) } ?: "",
                        isFavorite = item.isFavorite,
                        isPaused = item.isPaused,
                        notes = item.notes ?: "",
                        isEditing = true,
                        editingId = item.id,
                        createdAt = item.createdAt
                    )
                }
                savedSnapshot = _uiState.value.toFormSnapshot()
                item.categoryId?.let { loadSubcategories(it) }
            }
        }
    }

    fun prefill(barcode: String?, name: String?, brand: String?, quantity: String? = null, size: String? = null) {
        // Build description from size info if available
        val desc = size?.takeIf { it.isNotBlank() }?.let { "Size: $it" }
        _uiState.update {
            it.copy(
                barcode = barcode ?: it.barcode,
                name = name ?: it.name,
                brand = brand ?: it.brand,
                quantity = quantity ?: it.quantity,
                description = desc ?: it.description
            )
        }
        // Apply smart defaults for prefilled name
        name?.let { applySmartDefaults(it) }
    }

    fun loadSubcategories(categoryId: Long) {
        subcategoryJob?.cancel()
        subcategoryJob = viewModelScope.launch {
            categoryRepository.getSubcategories(categoryId).collect { subs ->
                _uiState.update { it.copy(subcategories = subs) }
            }
        }
    }

    fun updateName(v: String) {
        if (v.length > 100) return
        _uiState.update { it.copy(name = v, nameError = null) }

        // Fetch name suggestions
        suggestionJob?.cancel()
        if (v.length >= 2) {
            suggestionJob = viewModelScope.launch {
                delay(300)
                val dbNames = itemRepository.suggestNames(v, 3)
                val smartNames = SmartDefaults.suggestNames(v, 5)
                val combined = (dbNames + smartNames)
                    .distinct()
                    .filter { !it.equals(v, ignoreCase = true) }
                    .take(5)
                _uiState.update { it.copy(nameSuggestions = combined) }
            }
        } else {
            _uiState.update { it.copy(nameSuggestions = emptyList()) }
        }

        // Debounce smart defaults - wait 500ms after user stops typing
        smartDefaultJob?.cancel()
        remoteDefaultJob?.cancel()
        if (!_uiState.value.isEditing && v.length >= 3) {
            smartDefaultJob = viewModelScope.launch {
                delay(500)
                clearAutoFilledFields()
                applySmartDefaults(v)
            }
        }

        // Debounce duplicate check — 400ms after user stops typing
        duplicateCheckJob?.cancel()
        if (!_uiState.value.isEditing && v.length >= 2) {
            duplicateCheckJob = viewModelScope.launch {
                delay(400)
                checkForDuplicates(v)
            }
        } else {
            _uiState.update { it.copy(duplicateMatches = emptyList(), duplicateSuggestedAction = SuggestedAction.CREATE_NEW) }
        }
    }

    fun selectSuggestion(name: String) {
        _uiState.update { it.copy(name = name, nameSuggestions = emptyList(), nameError = null) }
        // Apply smart defaults immediately for selected suggestion
        if (!_uiState.value.isEditing) {
            smartDefaultJob?.cancel()
            remoteDefaultJob?.cancel()
            clearAutoFilledFields()
            applySmartDefaults(name)
        }
        // Also check for duplicates immediately
        duplicateCheckJob?.cancel()
        duplicateCheckJob = viewModelScope.launch {
            checkForDuplicates(name)
        }
    }

    private suspend fun checkForDuplicates(name: String) {
        if (name.isBlank()) return
        val barcode = _uiState.value.barcode.ifBlank { null }
        val result = try {
            itemRepository.findMatchingItems(name, barcode)
        } catch (_: Exception) { null }

        if (result != null && result.matches.isNotEmpty()) {
            // If editing, filter out the item being edited
            val editingId = _uiState.value.editingId
            val filtered = if (editingId != null) {
                result.matches.filter { it.itemId != editingId }
            } else result.matches

            val action = if (filtered.isEmpty()) SuggestedAction.CREATE_NEW
            else result.suggestedAction

            _uiState.update { it.copy(duplicateMatches = filtered, duplicateSuggestedAction = action) }
        } else {
            _uiState.update { it.copy(duplicateMatches = emptyList(), duplicateSuggestedAction = SuggestedAction.CREATE_NEW) }
        }
    }

    fun updateDescription(v: String) { if (v.length > 500) return; _uiState.update { it.copy(description = v) } }
    fun updateBarcode(v: String) { _uiState.update { it.copy(barcode = v) } }
    fun updateBrand(v: String) { _uiState.update { it.copy(brand = v) } }
    fun updateQuantity(v: String) { userTouchedFields.add(FIELD_QUANTITY); _uiState.update { it.copy(quantity = v, quantityError = null, quantityAutoFilled = false) } }
    fun updateMinQuantity(v: String) { _uiState.update { it.copy(minQuantity = v, minQuantityError = null) } }
    fun updateMaxQuantity(v: String) { _uiState.update { it.copy(maxQuantity = v, maxQuantityError = null) } }
    fun updateExpiryDate(v: String) { userTouchedFields.add(FIELD_EXPIRY); _uiState.update { it.copy(expiryDate = v, expiryDateError = null, expiryDateAutoFilled = false, smartDefaultAppliedExpiryDays = null) } }
    fun updateExpiryWarningDays(v: String) { _uiState.update { it.copy(expiryWarningDays = v) } }
    fun updateOpenedDate(v: String) { _uiState.update { it.copy(openedDate = v, openedDateError = null) } }
    fun updateDaysAfterOpening(v: String) { _uiState.update { it.copy(daysAfterOpening = v) } }
    fun updatePurchaseDate(v: String) { _uiState.update { it.copy(purchaseDate = v) } }
    fun updatePurchasePrice(v: String) { userTouchedFields.add(FIELD_PRICE); _uiState.update { it.copy(purchasePrice = v, priceError = null) } }
    fun updateFavorite(v: Boolean) { _uiState.update { it.copy(isFavorite = v) } }
    fun updatePaused(v: Boolean) { _uiState.update { it.copy(isPaused = v) } }
    fun updateNotes(v: String) { if (v.length > 1000) return; _uiState.update { it.copy(notes = v) } }

    fun scanExpiryDate(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningExpiry = true, expiryScanError = null) }
            try {
                // Bitmap processing on background thread to avoid UI jank
                val base64 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    // Scale to readable resolution (keep color — vision models handle it better)
                    val maxDim = 1600
                    val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap

                    // Compress — keep quality high for text readability
                    val stream = java.io.ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                    val result = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    if (scaled !== bitmap) scaled.recycle()
                    result
                }

                if (base64.length > 2_000_000) {
                    _uiState.update { it.copy(isScanningExpiry = false, expiryScanError = "Image too large — try holding the camera closer to the label") }
                    return@launch
                }

                val result = grokRepository.visionAnalysis(
                    systemPrompt = """You are an expert at reading expiry dates from product packaging photos.

LOOK FOR these keywords near a date: "EXP", "USE BY", "BEST BEFORE", "BB", "BBE", "BBD", "BEST BY", "SELL BY", "EXPIRY", "CONSUME BY", "DRINK BY", "GOOD UNTIL", "BEST END", "DISPLAY UNTIL".

IGNORE manufacture/production dates: "MFG", "MFD", "PROD", "PACKED", "PACK DATE", "LOT", "BATCH", "L:".

DATE FORMATS you may see (convert ALL to YYYY-MM-DD):
- DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY → e.g. 25/03/2026 → 2026-03-25
- MM/DD/YYYY → e.g. 03/25/2026 → 2026-03-25
- YYYY-MM-DD → already correct
- DD MMM YYYY or MMM DD YYYY → e.g. 25 MAR 2026 → 2026-03-25
- MMM YYYY or MM/YYYY → use last day of month, e.g. MAR 2026 → 2026-03-31
- DD/MM/YY or DD.MM.YY → assume 20xx, e.g. 25/03/26 → 2026-03-25
- MMM YY → e.g. MAR 26 → 2026-03-31
- Compact DDMMYYYY or YYYYMMDD → parse accordingly

If a date has no year, assume the next occurrence (current year or next year).
If ambiguous between DD/MM and MM/DD, prefer DD/MM (non-US convention).

Return ONLY the date in YYYY-MM-DD format. Nothing else. No explanation.
If no expiry date found, return exactly: NONE""",
                    userPrompt = "Extract the expiry date from this product label photo.",
                    imageBase64 = base64,
                    maxTokens = 100
                )

                result.fold(
                    onSuccess = { response ->
                        val cleaned = response.trim()
                        val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
                        val match = datePattern.find(cleaned)
                        if (match != null) {
                            val dateStr = match.value
                            try {
                                LocalDate.parse(dateStr)
                                updateExpiryDate(dateStr)
                                _uiState.update { it.copy(isScanningExpiry = false) }
                            } catch (e: Exception) {
                                _uiState.update { it.copy(isScanningExpiry = false, expiryScanError = "Invalid date: $dateStr") }
                            }
                        } else {
                            _uiState.update { it.copy(isScanningExpiry = false, expiryScanError = "No date found in image") }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isScanningExpiry = false, expiryScanError = e.message ?: "Scan failed") }
                    }
                )
            } catch (e: Exception) {
                Log.e("ItemForm", "Expiry scan failed", e)
                _uiState.update { it.copy(isScanningExpiry = false, expiryScanError = e.message ?: "Scan failed") }
            }
        }
    }

    fun selectCategory(id: Long?) {
        userTouchedFields.add(FIELD_CATEGORY)
        _uiState.update { it.copy(selectedCategoryId = id, selectedSubcategoryId = null, subcategories = emptyList()) }
        id?.let { catId ->
            loadSubcategories(catId)
            // Apply category-level location default if user hasn't set location manually
            if (FIELD_LOCATION !in userTouchedFields) {
                applyCategoryDefaults(catId)
            }
        }
    }

    fun selectSubcategory(id: Long?) { userTouchedFields.add(FIELD_SUBCATEGORY); _uiState.update { it.copy(selectedSubcategoryId = id) } }
    fun selectLocation(id: Long?) { userTouchedFields.add(FIELD_LOCATION); _uiState.update { it.copy(selectedLocationId = id) } }
    fun selectUnit(id: Long?) { userTouchedFields.add(FIELD_UNIT); _uiState.update { it.copy(selectedUnitId = id) } }

    /**
     * Apply smart defaults via the unified 5-layer cascade in SmartDefaultRepository.
     * Only fills fields the user hasn't explicitly interacted with.
     */
    private fun applySmartDefaults(itemName: String) {
        viewModelScope.launch {
            // Wait until categories are loaded (they load async in init)
            try {
                kotlinx.coroutines.withTimeout(2000) {
                    _uiState.first { it.categories.isNotEmpty() }
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                // Categories didn't load in 2s — proceed anyway
            }

            val result = smartDefaultRepository.resolve(itemName, regionCode)
            applyResolvedDefaults(result.local)
            applyPurchaseHistoryDefaults(itemName)
            _uiState.update { it.copy(smartDefaultsApplied = true) }

            // Async remote (layers 4+5)
            result.remoteDeferred?.let { deferred ->
                remoteDefaultJob?.cancel()
                remoteDefaultJob = viewModelScope.launch remoteFetch@{
                    delay(800) // longer debounce for network calls
                    if (_uiState.value.name != itemName) return@remoteFetch
                    val remote = deferred.await() ?: return@remoteFetch
                    if (_uiState.value.name != itemName) return@remoteFetch
                    // Only apply if user hasn't touched these fields
                    if (FIELD_CATEGORY !in userTouchedFields &&
                        FIELD_UNIT !in userTouchedFields &&
                        FIELD_LOCATION !in userTouchedFields &&
                        FIELD_EXPIRY !in userTouchedFields
                    ) {
                        applyResolvedDefaults(remote)
                    }
                }
            }
        }
    }

    /**
     * Apply resolved defaults to the form state.
     * Only fills fields the user hasn't explicitly interacted with.
     * Fields were already cleared by clearAutoFilledFields() before this call.
     */
    private fun applyResolvedDefaults(defaults: ResolvedDefaults) {
        if (FIELD_CATEGORY !in userTouchedFields && defaults.categoryId != null) {
            _uiState.update { it.copy(
                selectedCategoryId = defaults.categoryId,
                smartDefaultAppliedCategory = defaults.categoryName,
                smartDefaultSource = defaults.source
            ) }
            loadSubcategories(defaults.categoryId)

            if (FIELD_SUBCATEGORY !in userTouchedFields && defaults.subcategoryId != null) {
                // Wait briefly for subcategories to load after loadSubcategories
                viewModelScope.launch {
                    var subAttempts = 0
                    while (_uiState.value.subcategories.isEmpty() && subAttempts < 20) {
                        delay(50)
                        subAttempts++
                    }
                    if (FIELD_SUBCATEGORY !in userTouchedFields) {
                        _uiState.update { it.copy(
                            selectedSubcategoryId = defaults.subcategoryId,
                            smartDefaultAppliedSubcategory = defaults.subcategoryName
                        ) }
                    }
                }
            }
        }

        if (FIELD_UNIT !in userTouchedFields && defaults.unitId != null) {
            _uiState.update { it.copy(
                selectedUnitId = defaults.unitId,
                smartDefaultAppliedUnit = defaults.unitAbbreviation
            ) }
        }

        if (FIELD_LOCATION !in userTouchedFields && defaults.locationId != null) {
            _uiState.update { it.copy(
                selectedLocationId = defaults.locationId,
                smartDefaultAppliedLocation = defaults.locationName
            ) }
        }

        if (FIELD_EXPIRY !in userTouchedFields && defaults.shelfLifeDays != null) {
            val expiryDate = LocalDate.now().plusDays(defaults.shelfLifeDays.toLong())
            val wasBlank = _uiState.value.expiryDate.isBlank()
            _uiState.update {
                it.copy(
                    expiryDate = if (wasBlank) expiryDate.toString() else it.expiryDate,
                    expiryDateAutoFilled = wasBlank,
                    smartDefaultAppliedExpiryDays = if (wasBlank) defaults.shelfLifeDays else null
                )
            }
        }

        if (FIELD_QUANTITY !in userTouchedFields && defaults.quantity != null && defaults.quantity > 0) {
            val qty = defaults.quantity
            val defaultQty = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.2f".format(qty)
            _uiState.update {
                it.copy(quantity = if (it.quantity == "1" || it.quantity.isBlank()) defaultQty else it.quantity)
            }
        }

        if (FIELD_PRICE !in userTouchedFields && defaults.price != null && defaults.price > 0) {
            _uiState.update {
                it.copy(purchasePrice = if (it.purchasePrice.isBlank()) "%.2f".format(defaults.price) else it.purchasePrice)
            }
        }
    }

    /**
     * Clear fields that were auto-filled by smart defaults (not user-touched).
     * Called before re-applying smart defaults when the item name changes.
     */
    private fun clearAutoFilledFields() {
        _uiState.update {
            it.copy(
                selectedCategoryId = if (FIELD_CATEGORY in userTouchedFields) it.selectedCategoryId else null,
                selectedSubcategoryId = if (FIELD_SUBCATEGORY in userTouchedFields) it.selectedSubcategoryId else null,
                selectedLocationId = if (FIELD_LOCATION in userTouchedFields) it.selectedLocationId else null,
                selectedUnitId = if (FIELD_UNIT in userTouchedFields) it.selectedUnitId else null,
                expiryDate = if (FIELD_EXPIRY in userTouchedFields) it.expiryDate else "",
                quantity = if (FIELD_QUANTITY in userTouchedFields) it.quantity else "1",
                // Clear correction tracking for fresh comparison
                smartDefaultAppliedCategory = null,
                smartDefaultAppliedSubcategory = null,
                smartDefaultAppliedLocation = null,
                smartDefaultAppliedUnit = null,
                smartDefaultSource = null,
                smartDefaultsApplied = false,
                expiryDateAutoFilled = false,
                smartDefaultAppliedExpiryDays = null,
                quantityAutoFilled = false
            )
        }
    }

    private suspend fun applyPurchaseHistoryDefaults(itemName: String) {
        val purchaseDefaults = purchaseHistoryDao.getLatestPurchaseDefaultsByName(itemName)
        if (purchaseDefaults != null) {
            if (FIELD_QUANTITY !in userTouchedFields) {
                val qty = purchaseDefaults.quantity
                if (qty > 0) {
                    _uiState.update {
                        val defaultQty = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.2f".format(qty)
                        val shouldFill = it.quantity == "1" || it.quantity.isBlank()
                        it.copy(
                            quantity = if (shouldFill) defaultQty else it.quantity,
                            quantityAutoFilled = shouldFill
                        )
                    }
                }
            }
            if (FIELD_UNIT !in userTouchedFields && purchaseDefaults.unitId != null) {
                _uiState.update {
                    it.copy(selectedUnitId = if (it.selectedUnitId == null) purchaseDefaults.unitId else it.selectedUnitId)
                }
            }
            if (FIELD_PRICE !in userTouchedFields) {
                val price = purchaseDefaults.totalPrice
                if (price != null && price > 0) {
                    _uiState.update {
                        it.copy(purchasePrice = if (it.purchasePrice.isBlank()) "%.2f".format(price) else it.purchasePrice)
                    }
                }
            }
        }
    }

    /**
     * Apply category-level defaults when user manually selects a category.
     */
    private fun applyCategoryDefaults(categoryId: Long) {
        val state = _uiState.value
        val category = state.categories.find { it.id == categoryId } ?: return
        val catDefaults = SmartDefaults.getCategoryDefaults(category.name, regionCode) ?: return

        // Auto-fill location
        if (FIELD_LOCATION !in userTouchedFields && catDefaults.location != null) {
            val location = state.locations.find {
                it.name.equals(catDefaults.location, ignoreCase = true)
            }
            location?.let { _uiState.update { s -> s.copy(selectedLocationId = it.id) } }
        }

        // Auto-fill unit
        if (FIELD_UNIT !in userTouchedFields && catDefaults.unit != null) {
            val unit = state.units.find {
                it.abbreviation.equals(catDefaults.unit, ignoreCase = true)
            }
            unit?.let { _uiState.update { s -> s.copy(selectedUnitId = it.id) } }
        }
    }

    /**
     * Phase 4: Detect if user overrode server-provided smart defaults and submit anonymous corrections.
     * Only fires for "cache" or "remote" sourced defaults (not personal history or static dictionary).
     * Fire-and-forget — never blocks save.
     */
    private fun submitCorrectionsIfNeeded(state: ItemFormUiState) {
        val source = state.smartDefaultSource ?: return
        // Only submit corrections for server-sourced data (cache = seed/server, remote = Firestore/AI)
        if (source != "cache" && source != "remote") return

        // Check consent (auto-opt-in on first use)
        viewModelScope.launch {
            val consent = settingsRepository.getString(SettingsRepository.KEY_CORRECTION_CONSENT, "")
            if (consent == "no") return@launch
            if (consent.isEmpty()) {
                // Auto opt-in silently
                settingsRepository.set(SettingsRepository.KEY_CORRECTION_CONSENT, "yes")
            }

            val itemName = state.name.trim()

            // Compare each field: what was auto-filled vs what user selected at save time
            state.smartDefaultAppliedCategory?.let { appliedCategory ->
                val currentCategory = state.categories.find { it.id == state.selectedCategoryId }?.name
                if (currentCategory != null && !currentCategory.equals(appliedCategory, ignoreCase = true)) {
                    smartDefaultRepository.submitCorrection(itemName, "category", appliedCategory, currentCategory, regionCode)
                }
            }

            state.smartDefaultAppliedSubcategory?.let { appliedSub ->
                val currentSub = state.subcategories.find { it.id == state.selectedSubcategoryId }?.name
                if (currentSub != null && !currentSub.equals(appliedSub, ignoreCase = true)) {
                    smartDefaultRepository.submitCorrection(itemName, "subcategory", appliedSub, currentSub, regionCode)
                }
            }

            state.smartDefaultAppliedLocation?.let { appliedLocation ->
                val currentLocation = state.locations.find { it.id == state.selectedLocationId }?.name
                if (currentLocation != null && !currentLocation.equals(appliedLocation, ignoreCase = true)) {
                    smartDefaultRepository.submitCorrection(itemName, "location", appliedLocation, currentLocation, regionCode)
                }
            }

            state.smartDefaultAppliedUnit?.let { appliedUnit ->
                val currentUnit = state.units.find { it.id == state.selectedUnitId }?.abbreviation
                if (currentUnit != null && !currentUnit.equals(appliedUnit, ignoreCase = true)) {
                    smartDefaultRepository.submitCorrection(itemName, "unit", appliedUnit, currentUnit, regionCode)
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        // Validate all fields
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            hasError = true
        }
        if (state.quantity.isNotBlank() && state.quantity.toDoubleOrNull() == null) {
            _uiState.update { it.copy(quantityError = "Invalid number") }
            hasError = true
        } else if (state.quantity.toDoubleOrNull()?.let { it <= 0 } == true) {
            _uiState.update { it.copy(quantityError = "Must be greater than zero") }
            hasError = true
        }
        if (state.minQuantity.isNotBlank() && state.minQuantity.toDoubleOrNull() == null) {
            _uiState.update { it.copy(minQuantityError = "Invalid number") }
            hasError = true
        }
        if (state.maxQuantity.isNotBlank() && state.maxQuantity.toDoubleOrNull() == null) {
            _uiState.update { it.copy(maxQuantityError = "Invalid number") }
            hasError = true
        }
        val minQty = state.minQuantity.toDoubleOrNull()
        val maxQty = state.maxQuantity.toDoubleOrNull()
        if (minQty != null && maxQty != null && minQty > maxQty) {
            _uiState.update { it.copy(maxQuantityError = "Must be greater than min quantity") }
            hasError = true
        }
        if (state.purchasePrice.isNotBlank() && state.purchasePrice.toDoubleOrNull() == null) {
            _uiState.update { it.copy(priceError = "Invalid price") }
            hasError = true
        } else if (state.purchasePrice.toDoubleOrNull()?.let { it < 0 } == true) {
            _uiState.update { it.copy(priceError = "Cannot be negative") }
            hasError = true
        }

        // Date validation
        val expiryParsed = state.expiryDate.parseDate()
        val openedParsed = state.openedDate.parseDate()
        if (expiryParsed != null && expiryParsed.isBefore(LocalDate.now())) {
            _uiState.update { it.copy(expiryDateError = "Expiry date is in the past") }
            // Warning only — don't block save (item might already be expired)
        }
        if (openedParsed != null && expiryParsed != null && openedParsed.isAfter(expiryParsed)) {
            _uiState.update { it.copy(openedDateError = "Opened date is after expiry") }
            hasError = true
        }

        if (hasError) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                val qty = state.quantity.toDoubleOrNull() ?: 1.0
                val existingItem = state.editingId?.let { itemRepository.getById(it) }
                val newSmartMin = if (state.isEditing && existingItem != null) {
                    // When editing, update smart min if quantity increased
                    maxOf(existingItem.smartMinQuantity, qty)
                } else {
                    // New item: initial quantity becomes the smart min
                    qty
                }

                val entity = ItemEntity(
                    id = state.editingId ?: 0,
                    name = state.name.trim(),
                    description = state.description.ifBlank { null },
                    barcode = state.barcode.ifBlank { null },
                    brand = state.brand.ifBlank { null },
                    categoryId = state.selectedCategoryId,
                    subcategoryId = state.selectedSubcategoryId,
                    storageLocationId = state.selectedLocationId,
                    unitId = state.selectedUnitId,
                    quantity = qty,
                    minQuantity = state.minQuantity.toDoubleOrNull() ?: 0.0,
                    smartMinQuantity = newSmartMin,
                    maxQuantity = state.maxQuantity.toDoubleOrNull(),
                    expiryDate = state.expiryDate.parseDate(),
                    expiryWarningDays = state.expiryWarningDays.toIntOrNull() ?: 7,
                    openedDate = state.openedDate.parseDate(),
                    daysAfterOpening = state.daysAfterOpening.toIntOrNull(),
                    purchaseDate = state.purchaseDate.parseDate(),
                    purchasePrice = state.purchasePrice.toDoubleOrNull(),
                    isFavorite = state.isFavorite,
                    isPaused = state.isPaused,
                    notes = state.notes.ifBlank { null },
                    createdAt = if (state.isEditing) state.createdAt ?: java.time.LocalDateTime.now() else java.time.LocalDateTime.now()
                )

                val isNewItem = !state.isEditing
                val id = if (state.isEditing) {
                    itemRepository.update(entity)
                    entity.id
                } else {
                    // Check if an item with the same name already exists
                    val duplicate = itemRepository.findByName(state.name.trim())
                    val price = state.purchasePrice.toDoubleOrNull()

                    if (duplicate != null) {
                        // Merge: add quantity to existing item, update metadata
                        val mergedQty = duplicate.quantity + qty
                        val mergedSmartMin = maxOf(duplicate.smartMinQuantity, qty)
                        itemRepository.update(duplicate.copy(
                            quantity = mergedQty,
                            smartMinQuantity = mergedSmartMin,
                            purchaseDate = state.purchaseDate.parseDate() ?: duplicate.purchaseDate,
                            purchasePrice = price ?: duplicate.purchasePrice,
                            // Use form's expiry if present (covers both user-set and smart-default),
                            // otherwise keep the existing item's expiry
                            expiryDate = state.expiryDate.parseDate() ?: duplicate.expiryDate,
                            notes = state.notes.ifBlank { null } ?: duplicate.notes,
                            // M-39: Also merge category, location, unit, barcode, brand from user's form
                            categoryId = if (state.selectedCategoryId != null) state.selectedCategoryId else duplicate.categoryId,
                            subcategoryId = if (state.selectedSubcategoryId != null) state.selectedSubcategoryId else duplicate.subcategoryId,
                            storageLocationId = if (state.selectedLocationId != null) state.selectedLocationId else duplicate.storageLocationId,
                            unitId = if (state.selectedUnitId != null) state.selectedUnitId else duplicate.unitId,
                            barcode = if (!state.barcode.isBlank()) state.barcode else duplicate.barcode,
                            brand = if (!state.brand.isBlank()) state.brand else duplicate.brand
                        ))
                        // Record purchase history for the added quantity
                        purchaseHistoryDao.insert(
                            PurchaseHistoryEntity(
                                itemId = duplicate.id,
                                quantity = qty,
                                unitPrice = price?.let { if (qty > 0) it / qty else null },
                                totalPrice = price,
                                purchaseDate = state.purchaseDate.parseDate() ?: LocalDate.now(),
                                expiryDate = state.expiryDate.parseDate()
                            )
                        )
                        duplicate.id
                    } else {
                        val newId = itemRepository.insert(entity)
                        // Create purchase_history record so spending reports include this item
                        purchaseHistoryDao.insert(
                            PurchaseHistoryEntity(
                                itemId = newId,
                                quantity = qty,
                                unitPrice = price?.let { if (qty > 0) it / qty else null },
                                totalPrice = price,
                                purchaseDate = state.purchaseDate.parseDate() ?: LocalDate.now(),
                                expiryDate = state.expiryDate.parseDate()
                            )
                        )
                        newId
                    }
                }

                // Fire-and-forget correction submission if user overrode server-provided defaults
                if (isNewItem) {
                    submitCorrectionsIfNeeded(state)
                }

                // Auto-strike matching shopping list items (only for new items)
                if (isNewItem) {
                    val matches = shoppingListRepository.findMatchesForItem(state.name.trim())
                    val autoStriked = mutableListOf<String>()
                    val pending = mutableListOf<ShoppingMatch>()

                    for (match in matches) {
                        if (match.score >= 0.8) {
                            shoppingListRepository.markAsPurchasedOnly(match.shoppingItemId)
                            autoStriked.add(match.shoppingItemName)
                        } else if (match.score >= 0.6) {
                            pending.add(match)
                        }
                    }

                    _uiState.update { it.copy(
                        isSaving = false,
                        isSaved = true,
                        savedItemId = id,
                        autoStrikedItems = autoStriked,
                        pendingMatches = pending
                    ) }
                } else {
                    _uiState.update { it.copy(isSaving = false, isSaved = true, savedItemId = id) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "Failed to save: ${e.message}") }
            }
        }
    }

    fun confirmMatch(match: ShoppingMatch) {
        viewModelScope.launch {
            shoppingListRepository.markAsPurchasedOnly(match.shoppingItemId)
            _uiState.update { state ->
                state.copy(
                    autoStrikedItems = state.autoStrikedItems + match.shoppingItemName,
                    pendingMatches = state.pendingMatches.filter { it.shoppingItemId != match.shoppingItemId }
                )
            }
        }
    }

    fun dismissMatch(match: ShoppingMatch) {
        _uiState.update { state ->
            state.copy(
                pendingMatches = state.pendingMatches.filter { it.shoppingItemId != match.shoppingItemId }
            )
        }
    }

    private fun String.parseDate(): LocalDate? {
        if (this.isBlank()) return null
        return try {
            LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }
    }

    private fun Double.formatForInput(): String {
        return if (this == this.toLong().toDouble()) this.toLong().toString()
        else "%.2f".format(this)
    }

    companion object {
        /** One-shot flag: set by Dashboard "Show me" → consumed by ItemFormScreen to show tour overlay */
        val pendingTourMode = java.util.concurrent.atomic.AtomicBoolean(false)

        // Field name constants for userTouchedFields tracking
        private const val FIELD_CATEGORY = "category"
        private const val FIELD_SUBCATEGORY = "subcategory"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_UNIT = "unit"
        private const val FIELD_EXPIRY = "expiry"
        private const val FIELD_QUANTITY = "quantity"
        private const val FIELD_PRICE = "price"
    }
}
