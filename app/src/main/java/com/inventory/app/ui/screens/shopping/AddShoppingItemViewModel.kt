package com.inventory.app.ui.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.dao.StorageLocationDao
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.local.entity.UnitEntity
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.UnitRepository
import com.inventory.app.domain.model.Priority
import com.inventory.app.domain.model.ShoppingListMatcher
import com.inventory.app.domain.model.ShoppingNameInfo
import com.inventory.app.domain.model.SmartDefaults
import com.inventory.app.ui.components.formatQty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class WasteWarningMatch(
    val itemName: String,
    val quantity: String,
    val locationName: String?,
    val expiryInfo: String?
)

data class AddShoppingItemUiState(
    val itemId: Long? = null,
    val itemName: String = "",
    val customName: String = "",
    val quantity: String = "1",
    val selectedUnitId: Long? = null,
    val units: List<UnitEntity> = emptyList(),
    val priority: Priority = Priority.NORMAL,
    val notes: String = "",
    val isSaved: Boolean = false,
    val nameSuggestions: List<String> = emptyList(),
    val editingId: Long? = null,
    val isEditMode: Boolean = false,
    val showWasteWarning: Boolean = false,
    val wasteWarningMatches: List<WasteWarningMatch> = emptyList()
)

@HiltViewModel
class AddShoppingItemViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val itemRepository: ItemRepository,
    private val unitRepository: UnitRepository,
    private val itemDao: ItemDao,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val storageLocationDao: StorageLocationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddShoppingItemUiState())
    val uiState = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var smartDefaultJob: Job? = null

    // Track which fields user has manually changed
    private var userSetQuantity = false
    private var userSetUnit = false

    init {
        viewModelScope.launch {
            unitRepository.getAllActive().collect { units ->
                _uiState.update { it.copy(units = units) }
            }
        }
    }

    fun loadFromItem(id: Long) {
        userSetQuantity = true
        userSetUnit = true
        viewModelScope.launch {
            itemRepository.getById(id)?.let { item ->
                val neededQty = if (item.minQuantity > item.quantity) {
                    item.minQuantity - item.quantity
                } else {
                    1.0
                }
                _uiState.update {
                    it.copy(
                        itemId = item.id,
                        itemName = item.name,
                        selectedUnitId = item.unitId,
                        quantity = neededQty.let { q ->
                            if (q == q.toLong().toDouble()) q.toLong().toString() else "%.1f".format(q)
                        }
                    )
                }
            }
        }
    }

    fun loadShoppingItem(id: Long) {
        userSetQuantity = true
        userSetUnit = true
        viewModelScope.launch {
            val shoppingItem = shoppingListRepository.getById(id) ?: return@launch
            val itemName = if (shoppingItem.itemId != null) {
                itemRepository.getById(shoppingItem.itemId)?.name ?: ""
            } else {
                shoppingItem.customName ?: ""
            }
            val qty = shoppingItem.quantity
            _uiState.update {
                it.copy(
                    editingId = id,
                    isEditMode = true,
                    itemId = shoppingItem.itemId,
                    itemName = itemName,
                    customName = if (shoppingItem.itemId == null) (shoppingItem.customName ?: "") else "",
                    quantity = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.1f".format(qty),
                    selectedUnitId = shoppingItem.unitId,
                    priority = Priority.fromValue(shoppingItem.priority),
                    notes = shoppingItem.notes ?: ""
                )
            }
        }
    }

    fun updateCustomName(v: String) {
        _uiState.update { it.copy(customName = v, itemId = null) }

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

        // Debounce smart defaults
        smartDefaultJob?.cancel()
        if (v.length >= 3) {
            smartDefaultJob = viewModelScope.launch {
                delay(500)
                applySmartDefaults(v)
            }
        }
    }

    fun selectSuggestion(name: String) {
        _uiState.update { it.copy(customName = name, nameSuggestions = emptyList()) }
        applySmartDefaults(name)
    }

    fun updateQuantity(v: String) { userSetQuantity = true; _uiState.update { it.copy(quantity = v) } }
    fun selectUnit(id: Long?) { userSetUnit = true; _uiState.update { it.copy(selectedUnitId = id) } }
    fun updatePriority(v: Priority) { _uiState.update { it.copy(priority = v) } }
    fun updateNotes(v: String) { _uiState.update { it.copy(notes = v) } }

    private fun applySmartDefaults(name: String) {
        viewModelScope.launch {
            // Try to resolve to an existing inventory item
            val existingItem = itemDao.findByName(name)
            if (existingItem != null) {
                _uiState.update { it.copy(itemId = existingItem.id) }
                // Use item's unit if user hasn't set one
                if (!userSetUnit && existingItem.unitId != null) {
                    _uiState.update { it.copy(selectedUnitId = existingItem.unitId) }
                }
            }

            // Look up purchase history for quantity/unit defaults
            val purchaseDefaults = purchaseHistoryDao.getLatestPurchaseDefaultsByName(name)
            if (purchaseDefaults != null) {
                if (!userSetQuantity && purchaseDefaults.quantity > 0) {
                    val qty = purchaseDefaults.quantity
                    _uiState.update { it.copy(
                        quantity = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.1f".format(qty)
                    )}
                }
                if (!userSetUnit && purchaseDefaults.unitId != null) {
                    _uiState.update { it.copy(selectedUnitId = purchaseDefaults.unitId) }
                }
            } else if (!userSetUnit) {
                // Fallback to hardcoded smart defaults for unit
                val defaults = SmartDefaults.lookup(name)
                if (defaults?.unit != null) {
                    val unit = _uiState.value.units.find {
                        it.abbreviation.equals(defaults.unit, ignoreCase = true)
                    }
                    unit?.let { _uiState.update { s -> s.copy(selectedUnitId = it.id) } }
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        // Skip waste check in edit mode
        if (state.isEditMode) {
            performSave()
            return
        }
        // Run waste-avoidance check for new items
        viewModelScope.launch {
            val nameToCheck = if (state.itemId != null) state.itemName else state.customName
            if (nameToCheck.isBlank()) {
                performSave()
                return@launch
            }
            try {
                val inStockItems = itemRepository.getInStockItems()
                val inStockAsNames = inStockItems.map { ShoppingNameInfo(it.id, it.name) }
                val matches = ShoppingListMatcher.findMatches(nameToCheck, inStockAsNames)
                    .filter { it.score >= 0.8 }

                if (matches.isNotEmpty()) {
                    // Build warning info with location names and expiry
                    val matchedRows = matches.mapNotNull { match ->
                        inStockItems.find { it.id == match.shoppingItemId }
                    }
                    val warnings = matchedRows.map { row ->
                        val locationName = row.storageLocationId?.let { locId ->
                            storageLocationDao.getById(locId)?.name
                        }
                        val expiryInfo = row.expiryDate?.let { epochDay ->
                            val expiryDate = LocalDate.ofEpochDay(epochDay)
                            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
                            when {
                                daysUntil < 0 -> "Expired"
                                daysUntil == 0L -> "Expires today"
                                daysUntil == 1L -> "Expires tomorrow"
                                else -> "Expires in $daysUntil days"
                            }
                        }
                        WasteWarningMatch(
                            itemName = row.name,
                            quantity = row.quantity.formatQty(),
                            locationName = locationName,
                            expiryInfo = expiryInfo
                        )
                    }
                    _uiState.update { it.copy(showWasteWarning = true, wasteWarningMatches = warnings) }
                } else {
                    performSave()
                }
            } catch (_: Exception) {
                // If check fails, just save anyway
                performSave()
            }
        }
    }

    fun dismissWasteWarning() {
        _uiState.update { it.copy(showWasteWarning = false, wasteWarningMatches = emptyList()) }
    }

    fun proceedWithSave() {
        _uiState.update { it.copy(showWasteWarning = false, wasteWarningMatches = emptyList()) }
        performSave()
    }

    private fun performSave() {
        val state = _uiState.value
        viewModelScope.launch {
            // If no itemId resolved yet, try one final lookup by name before saving
            var resolvedItemId = state.itemId
            if (resolvedItemId == null && state.customName.isNotBlank()) {
                resolvedItemId = itemDao.findByName(state.customName)?.id
            }

            shoppingListRepository.addItem(
                ShoppingListItemEntity(
                    id = state.editingId ?: 0,
                    itemId = resolvedItemId,
                    customName = if (resolvedItemId == null) state.customName.ifBlank { null } else null,
                    quantity = state.quantity.toDoubleOrNull() ?: 1.0,
                    unitId = state.selectedUnitId,
                    priority = state.priority.value,
                    notes = state.notes.ifBlank { null }
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
