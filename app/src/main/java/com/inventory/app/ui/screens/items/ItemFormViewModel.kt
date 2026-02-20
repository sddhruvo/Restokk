package com.inventory.app.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.StorageLocationEntity
import com.inventory.app.data.local.entity.SubcategoryEntity
import com.inventory.app.data.local.entity.UnitEntity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Base64
import android.util.Log
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.repository.UnitRepository
import com.inventory.app.domain.model.ShoppingMatch
import com.inventory.app.domain.model.SmartDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    val expiryScanError: String? = null
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
    private val grokRepository: GrokRepository
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

    // Track which fields user has manually changed (don't override them with smart defaults)
    private var userSetCategory = false
    private var userSetSubcategory = false
    private var userSetLocation = false
    private var userSetUnit = false
    private var userSetExpiry = false
    private var userSetQuantity = false
    private var userSetPrice = false

    private var smartDefaultJob: Job? = null
    private var suggestionJob: Job? = null
    private var subcategoryJob: Job? = null

    init {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
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
        // Mark all as user-set when editing (don't auto-change anything)
        userSetCategory = true
        userSetSubcategory = true
        userSetLocation = true
        userSetUnit = true
        userSetExpiry = true
        userSetQuantity = true
        userSetPrice = true

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

    fun prefill(barcode: String?, name: String?, brand: String?, quantity: String? = null) {
        _uiState.update {
            it.copy(
                barcode = barcode ?: it.barcode,
                name = name ?: it.name,
                brand = brand ?: it.brand,
                quantity = quantity ?: it.quantity
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
        if (!_uiState.value.isEditing && v.length >= 3) {
            smartDefaultJob = viewModelScope.launch {
                delay(500)
                applySmartDefaults(v)
            }
        }
    }

    fun selectSuggestion(name: String) {
        _uiState.update { it.copy(name = name, nameSuggestions = emptyList(), nameError = null) }
        // Apply smart defaults immediately for selected suggestion
        if (!_uiState.value.isEditing) {
            applySmartDefaults(name)
        }
    }

    fun updateDescription(v: String) { _uiState.update { it.copy(description = v) } }
    fun updateBarcode(v: String) { _uiState.update { it.copy(barcode = v) } }
    fun updateBrand(v: String) { _uiState.update { it.copy(brand = v) } }
    fun updateQuantity(v: String) { userSetQuantity = true; _uiState.update { it.copy(quantity = v, quantityError = null) } }
    fun updateMinQuantity(v: String) { _uiState.update { it.copy(minQuantity = v, minQuantityError = null) } }
    fun updateMaxQuantity(v: String) { _uiState.update { it.copy(maxQuantity = v, maxQuantityError = null) } }
    fun updateExpiryDate(v: String) { userSetExpiry = true; _uiState.update { it.copy(expiryDate = v, expiryDateError = null) } }
    fun updateExpiryWarningDays(v: String) { _uiState.update { it.copy(expiryWarningDays = v) } }
    fun updateOpenedDate(v: String) { _uiState.update { it.copy(openedDate = v, openedDateError = null) } }
    fun updateDaysAfterOpening(v: String) { _uiState.update { it.copy(daysAfterOpening = v) } }
    fun updatePurchaseDate(v: String) { _uiState.update { it.copy(purchaseDate = v) } }
    fun updatePurchasePrice(v: String) { userSetPrice = true; _uiState.update { it.copy(purchasePrice = v, priceError = null) } }
    fun updateFavorite(v: Boolean) { _uiState.update { it.copy(isFavorite = v) } }
    fun updatePaused(v: Boolean) { _uiState.update { it.copy(isPaused = v) } }
    fun updateNotes(v: String) { _uiState.update { it.copy(notes = v) } }

    fun scanExpiryDate(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningExpiry = true, expiryScanError = null) }
            try {
                // Step 1: Scale to readable resolution (higher than before for small label text)
                val maxDim = 1200
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    ).also { if (it !== bitmap) bitmap.recycle() }
                } else bitmap

                // Step 2: Document-style preprocessing — grayscale + high contrast
                // Makes text pop against background, like a flatbed scanner
                val enhanced = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(enhanced)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val contrast = 1.8f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                // Grayscale + contrast boost in a single ColorMatrix
                paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                    0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, translate,
                    0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, translate,
                    0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )))
                canvas.drawBitmap(scaled, 0f, 0f, paint)
                if (scaled !== bitmap) scaled.recycle()

                // Step 3: Compress with higher quality for text readability
                val stream = java.io.ByteArrayOutputStream()
                enhanced.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                enhanced.recycle()

                if (base64.length > 2_000_000) {
                    _uiState.update { it.copy(isScanningExpiry = false, expiryScanError = "Image too large — try holding the camera closer to the label") }
                    return@launch
                }

                val result = grokRepository.visionAnalysis(
                    systemPrompt = """You extract expiry dates from product labels. The image has been preprocessed for clarity.

Rules:
1. Look for keywords: "EXP", "USE BY", "BEST BEFORE", "BB", "BEST BY", "SELL BY", "EXPIRY", or date printed near these words.
2. Do NOT return a manufacture date ("MFG", "MFD", "PROD", "PACKED") — only the expiry/use-by date.
3. If you see a format like DD/MM/YYYY or DD-MM-YYYY, convert it to YYYY-MM-DD. If you see MM/YYYY, use the last day of that month.
4. Return ONLY the date in YYYY-MM-DD format. Nothing else.
5. If no expiry date is found, return NONE.""",
                    userPrompt = "Extract the expiry date from this product label. The image is grayscale-enhanced for readability.",
                    imageBase64 = base64,
                    maxTokens = 50
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
        userSetCategory = true
        _uiState.update { it.copy(selectedCategoryId = id, selectedSubcategoryId = null, subcategories = emptyList()) }
        id?.let { catId ->
            loadSubcategories(catId)
            // Apply category-level location default if user hasn't set location manually
            if (!userSetLocation) {
                applyCategoryDefaults(catId)
            }
        }
    }

    fun selectSubcategory(id: Long?) { userSetSubcategory = true; _uiState.update { it.copy(selectedSubcategoryId = id) } }
    fun selectLocation(id: Long?) { userSetLocation = true; _uiState.update { it.copy(selectedLocationId = id) } }
    fun selectUnit(id: Long?) { userSetUnit = true; _uiState.update { it.copy(selectedUnitId = id) } }

    /**
     * Apply smart defaults based on item name.
     * Looks up the name in SmartDefaults and auto-fills category, subcategory,
     * unit, location, and expiry date — only for fields the user hasn't manually changed.
     */
    private fun applySmartDefaults(itemName: String) {
        val defaults = SmartDefaults.lookup(itemName) ?: return

        viewModelScope.launch {
            // Wait until categories/units/locations are loaded (they load async in init)
            // Retry up to 10 times with 100ms delay — covers slow DB reads
            var attempts = 0
            while (_uiState.value.categories.isEmpty() && attempts < 10) {
                delay(100)
                attempts++
            }

            // Re-read current state before each field update to avoid overwriting
            // fields the user may have changed during the async lookups.

            // Find matching category from loaded list
            if (!userSetCategory) {
                val current = _uiState.value
                val category = current.categories.find {
                    it.name.equals(defaults.category, ignoreCase = true)
                }
                if (category != null) {
                    _uiState.update { it.copy(
                        selectedCategoryId = if (it.selectedCategoryId == null) category.id else it.selectedCategoryId
                    ) }
                    loadSubcategories(category.id)

                    // Wait for subcategories to load, then match subcategory
                    if (!userSetSubcategory && defaults.subcategory != null) {
                        var subAttempts = 0
                        while (_uiState.value.subcategories.isEmpty() && subAttempts < 20) {
                            delay(50)
                            subAttempts++
                        }
                        val sub = _uiState.value.subcategories.find {
                            it.name.equals(defaults.subcategory, ignoreCase = true)
                        }
                        sub?.let { matched ->
                            _uiState.update { s ->
                                s.copy(selectedSubcategoryId = if (s.selectedSubcategoryId == null) matched.id else s.selectedSubcategoryId)
                            }
                        }
                    }
                }
            }

            // Find matching unit
            if (!userSetUnit && defaults.unit != null) {
                val current = _uiState.value
                val unit = current.units.find {
                    it.abbreviation.equals(defaults.unit, ignoreCase = true)
                }
                unit?.let { matched ->
                    _uiState.update { s ->
                        s.copy(selectedUnitId = if (s.selectedUnitId == null) matched.id else s.selectedUnitId)
                    }
                }
            }

            // Find matching location
            if (!userSetLocation && defaults.location != null) {
                val current = _uiState.value
                val location = current.locations.find {
                    it.name.equals(defaults.location, ignoreCase = true)
                }
                location?.let { matched ->
                    _uiState.update { s ->
                        s.copy(selectedLocationId = if (s.selectedLocationId == null) matched.id else s.selectedLocationId)
                    }
                }
            }

            // Set expiry date based on shelf life
            if (!userSetExpiry && defaults.shelfLifeDays != null) {
                val expiryDate = LocalDate.now().plusDays(defaults.shelfLifeDays.toLong())
                _uiState.update {
                    it.copy(expiryDate = if (it.expiryDate.isBlank()) expiryDate.toString() else it.expiryDate)
                }
            }

            // Also check purchase history for price/quantity/unit defaults
            val purchaseDefaults = purchaseHistoryDao.getLatestPurchaseDefaultsByName(itemName)
            if (purchaseDefaults != null) {
                if (!userSetQuantity) {
                    val qty = purchaseDefaults.quantity
                    if (qty > 0) {
                        _uiState.update {
                            val defaultQty = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.2f".format(qty)
                            it.copy(quantity = if (it.quantity == "1" || it.quantity.isBlank()) defaultQty else it.quantity)
                        }
                    }
                }
                if (!userSetUnit && purchaseDefaults.unitId != null) {
                    _uiState.update {
                        it.copy(selectedUnitId = if (it.selectedUnitId == null) purchaseDefaults.unitId else it.selectedUnitId)
                    }
                }
                if (!userSetPrice) {
                    val price = purchaseDefaults.totalPrice
                    if (price != null && price > 0) {
                        _uiState.update {
                            it.copy(purchasePrice = if (it.purchasePrice.isBlank()) "%.2f".format(price) else it.purchasePrice)
                        }
                    }
                }
            }

            _uiState.update { it.copy(smartDefaultsApplied = true) }
        }
    }

    /**
     * Apply category-level defaults when user manually selects a category.
     */
    private fun applyCategoryDefaults(categoryId: Long) {
        val state = _uiState.value
        val category = state.categories.find { it.id == categoryId } ?: return
        val catDefaults = SmartDefaults.getCategoryDefaults(category.name) ?: return

        // Auto-fill location
        if (!userSetLocation && catDefaults.location != null) {
            val location = state.locations.find {
                it.name.equals(catDefaults.location, ignoreCase = true)
            }
            location?.let { _uiState.update { s -> s.copy(selectedLocationId = it.id) } }
        }

        // Auto-fill unit
        if (!userSetUnit && catDefaults.unit != null) {
            val unit = state.units.find {
                it.abbreviation.equals(catDefaults.unit, ignoreCase = true)
            }
            unit?.let { _uiState.update { s -> s.copy(selectedUnitId = it.id) } }
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
        } else if (state.quantity.toDoubleOrNull()?.let { it < 0 } == true) {
            _uiState.update { it.copy(quantityError = "Cannot be negative") }
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
                        val mergedSmartMin = maxOf(duplicate.smartMinQuantity, mergedQty)
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
}
