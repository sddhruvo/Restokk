package com.inventory.app.ui.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.dao.CategoryDao
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.local.entity.relations.ShoppingListItemWithDetails
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.domain.model.ConsumptionPrediction
import com.inventory.app.domain.model.ConsumptionVelocityCalculator
import com.inventory.app.domain.model.PurchaseDataPoint
import com.inventory.app.ui.components.CelebrationType
import com.inventory.app.domain.model.SmartDefaults
import com.inventory.app.util.CategoryMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class BuyAgainItem(
    val itemId: Long,
    val itemName: String,
    val purchaseCount: Int,
    val unitId: Long?
)

data class StorePriceInfo(
    val storeName: String?,
    val unitPrice: Double,
    val purchaseDate: LocalDate
)

data class QuickAddSuggestion(
    val itemId: Long,              // 0 for SmartDefaults items (no inventory match)
    val name: String,
    val source: String,            // "frequent", "inventory", "common"
    val categoryName: String? = null,
    val purchaseCount: Int = 0,    // times bought (for frequent source)
    val contextLabel: String = "", // "bought 12×" / "in stock · 2 left" / "common item"
    val defaultQuantity: Double = 1.0
)

data class ShoppingListUiState(
    val activeItems: List<ShoppingListItemWithDetails> = emptyList(),
    val purchasedItems: List<ShoppingListItemWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null,
    val itemPrices: Map<Long, Double> = emptyMap(),
    val estimatedTotal: Double = 0.0,
    val currencySymbol: String = "",
    val sortBy: String = "priority",
    val recentlyDeletedItem: ShoppingListItemEntity? = null,
    val shoppingBudget: Double = 0.0,
    val showBatchAddDialog: Boolean = false,
    val predictions: List<ConsumptionPrediction> = emptyList(),
    val isGroupedByCategory: Boolean = false,
    val categories: Map<Long, String> = emptyMap(),
    val buyAgainItems: List<BuyAgainItem> = emptyList(),
    val storePriceInfo: Map<Long, StorePriceInfo> = emptyMap(),
    val quickAddSuggestions: List<QuickAddSuggestion> = emptyList(),
    val inferredCategories: Map<Long, String> = emptyMap(),
    val celebrationType: CelebrationType? = null
)

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val settingsRepository: SettingsRepository,
    private val categoryDao: CategoryDao,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState = _uiState.asStateFlow()

    // Track previous active count for "all done" celebration detection
    private var previousActiveCount = -1 // -1 = not yet loaded
    // Cache the celebration type so re-emissions of combine() don't randomize it
    private var celebrationTypeCache: CelebrationType? = null
    // Mutex to prevent race condition on rapid add (check-then-insert)
    private val addMutex = Mutex()

    init {
        viewModelScope.launch {
            val currency = settingsRepository.getCurrencySymbol()
            val budget = settingsRepository.getString(SettingsRepository.KEY_SHOPPING_BUDGET, "")
                .toDoubleOrNull() ?: 0.0
            _uiState.update { it.copy(currencySymbol = currency, shoppingBudget = budget) }
        }

        // Auto-clear old purchased items
        viewModelScope.launch {
            val days = settingsRepository.getString(SettingsRepository.KEY_AUTO_CLEAR_DAYS, "")
                .toIntOrNull()
            if (days != null && days > 0) {
                shoppingListRepository.clearPurchasedOlderThan(days)
            }
        }

        loadPredictions()
        loadBuyAgainItems()

        // Load category names
        viewModelScope.launch {
            categoryDao.getAllActive().collect { cats ->
                val catMap = cats.associate { it.id to it.name }
                _uiState.update { it.copy(categories = catMap) }
            }
        }

        viewModelScope.launch {
            try {
                combine(
                    shoppingListRepository.getActiveItems(),
                    shoppingListRepository.getPurchasedItems()
                ) { active, purchased ->
                    Pair(active, purchased)
                }.collect { (active, purchased) ->
                    // Calculate estimated costs and store price info for ALL items
                    val allItems = active + purchased
                    val itemIds = allItems.mapNotNull { it.shoppingItem.itemId }
                    val priceMap = mutableMapOf<Long, Double>()
                    val storeInfoMap = mutableMapOf<Long, StorePriceInfo>()

                    if (itemIds.isNotEmpty()) {
                        // Chunk to stay under SQLite's 999 parameter limit
                        val latestPrices = itemIds.chunked(500).flatMap {
                            purchaseHistoryDao.getLatestPricesWithStoreForItems(it)
                        }
                        for (lp in latestPrices) {
                            val unitPrice = lp.unitPrice
                                ?: if (lp.totalPrice != null && lp.quantity > 0) lp.totalPrice / lp.quantity else null
                            if (unitPrice != null) {
                                priceMap[lp.itemId] = unitPrice
                                storeInfoMap[lp.itemId] = StorePriceInfo(
                                    storeName = lp.storeName,
                                    unitPrice = unitPrice,
                                    purchaseDate = LocalDate.ofEpochDay(lp.purchaseDate)
                                )
                            }
                        }
                    }

                    var activeTotal = 0.0
                    val itemCosts = mutableMapOf<Long, Double>()
                    for (item in allItems) {
                        val itemId = item.shoppingItem.itemId
                        val unitPrice = if (itemId != null) {
                            priceMap[itemId]
                                ?: item.item?.purchasePrice
                        } else {
                            // Try to find price for custom-name items via purchase history
                            val customName = item.shoppingItem.customName ?: continue
                            val matchedItem = itemRepository.findByName(customName) ?: continue
                            // Check purchase history for a real unit price first
                            val lp = purchaseHistoryDao.getLatestPricesForItems(listOf(matchedItem.id)).firstOrNull()
                            lp?.unitPrice
                                ?: lp?.let { if (it.totalPrice != null && it.quantity > 0) it.totalPrice / it.quantity else null }
                                ?: matchedItem.purchasePrice
                        }
                        if (unitPrice == null) continue
                        val cost = unitPrice * item.shoppingItem.quantity
                        itemCosts[item.shoppingItem.id] = cost
                        if (!item.shoppingItem.isPurchased) activeTotal += cost
                    }

                    // Infer categories for custom-name items (no inventory match)
                    val inferred = mutableMapOf<Long, String>()
                    for (item in active + purchased) {
                        if (item.item == null && item.shoppingItem.customName != null) {
                            CategoryMatcher.matchCategory(item.shoppingItem.customName)?.let { cat ->
                                inferred[item.shoppingItem.id] = cat
                            }
                        }
                    }

                    // Detect "all done" transition: active was >0, now 0, purchased non-empty
                    // Note: combine may emit transient ([], []) state mid-DB-update,
                    // so only update previousActiveCount when state is meaningful
                    val celebration = if (
                        previousActiveCount > 0 &&
                        active.isEmpty() &&
                        purchased.isNotEmpty()
                    ) {
                        previousActiveCount = 0 // prevent re-trigger
                        val cached = CelebrationType.entries.random()
                        celebrationTypeCache = cached
                        cached
                    } else if (active.isNotEmpty()) {
                        previousActiveCount = active.size
                        celebrationTypeCache = null
                        null // reset celebration when items are added back
                    } else if (celebrationTypeCache != null) {
                        // Re-emission while celebrating — keep the same type
                        celebrationTypeCache
                    } else {
                        null
                    }

                    _uiState.update {
                        it.copy(
                            activeItems = active,
                            purchasedItems = purchased,
                            isLoading = false,
                            itemPrices = itemCosts,
                            estimatedTotal = activeTotal,
                            storePriceInfo = storeInfoMap,
                            inferredCategories = inferred,
                            celebrationType = celebration
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load shopping list: ${e.message}", isLoading = false) }
            }
        }
    }

    fun togglePurchased(id: Long) {
        viewModelScope.launch {
            try {
                shoppingListRepository.togglePurchased(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update item: ${e.message}") }
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                shoppingListRepository.deleteItem(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete item: ${e.message}") }
            }
        }
    }

    fun deleteItemWithUndo(id: Long) {
        viewModelScope.launch {
            try {
                val item = shoppingListRepository.getById(id)
                shoppingListRepository.deleteItem(id)
                _uiState.update { it.copy(recentlyDeletedItem = item) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete item: ${e.message}") }
            }
        }
    }

    fun undoDelete() {
        val item = _uiState.value.recentlyDeletedItem ?: return
        viewModelScope.launch {
            try {
                shoppingListRepository.addItem(item)
                _uiState.update { it.copy(recentlyDeletedItem = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to restore item: ${e.message}") }
            }
        }
    }

    fun clearRecentlyDeleted() {
        _uiState.update { it.copy(recentlyDeletedItem = null) }
    }

    fun updateQuantity(id: Long, delta: Double) {
        viewModelScope.launch {
            try {
                val item = shoppingListRepository.getById(id) ?: return@launch
                val newQty = (item.quantity + delta).coerceAtLeast(0.1)
                shoppingListRepository.updateQuantity(id, newQty)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update quantity: ${e.message}") }
            }
        }
    }

    fun dismissCelebration() {
        celebrationTypeCache = null
        _uiState.update { it.copy(celebrationType = null) }
    }

    fun clearAndFinish() {
        viewModelScope.launch {
            try {
                shoppingListRepository.clearPurchased()
                celebrationTypeCache = null
                _uiState.update { it.copy(celebrationType = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to clear items: ${e.message}") }
            }
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            try {
                shoppingListRepository.clearPurchased()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to clear purchased items: ${e.message}") }
            }
        }
    }

    fun generateFromLowStock() {
        viewModelScope.launch {
            try {
                val added = shoppingListRepository.generateFromLowStock()
                _uiState.update {
                    it.copy(message = if (added > 0) "$added items added from low stock" else "No low stock items to add")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to generate from low stock: ${e.message}") }
            }
        }
    }

    fun showBatchAdd() {
        _uiState.update { it.copy(showBatchAddDialog = true) }
    }

    fun hideBatchAdd() {
        _uiState.update { it.copy(showBatchAddDialog = false) }
    }

    fun batchAddItems(text: String) {
        viewModelScope.launch {
            var added = 0
            val lines = text.lines().filter { it.isNotBlank() }.take(50)
            for (line in lines) {
                val trimmed = line.trim().take(100)
                // Parse optional leading quantity: "2 Milk", "3x Bread", "Eggs"
                val match = Regex("""^(\d+(?:\.\d+)?)\s*[xX]?\s+(.+)$""").find(trimmed)
                val qty: Double
                val name: String
                if (match != null) {
                    qty = match.groupValues[1].toDoubleOrNull() ?: 1.0
                    name = match.groupValues[2].trim()
                } else {
                    qty = 1.0
                    name = trimmed
                }
                if (name.isNotBlank()) {
                    val existingItem = itemRepository.findByName(name)
                    // Check if already on the shopping list — increment quantity instead of duplicating
                    val existingShoppingItem = if (existingItem != null) {
                        shoppingListRepository.findActiveByItemId(existingItem.id)
                    } else {
                        shoppingListRepository.findActiveByCustomName(name)
                    }
                    if (existingShoppingItem != null) {
                        shoppingListRepository.updateQuantity(existingShoppingItem.id, existingShoppingItem.quantity + qty)
                    } else {
                        shoppingListRepository.addItem(
                            if (existingItem != null) {
                                ShoppingListItemEntity(
                                    itemId = existingItem.id,
                                    quantity = qty
                                )
                            } else {
                                ShoppingListItemEntity(
                                    customName = name,
                                    quantity = qty
                                )
                            }
                        )
                    }
                    added++
                }
            }
            _uiState.update {
                it.copy(
                    showBatchAddDialog = false,
                    message = if (added > 0) "$added items added" else "No items to add"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun updateSortBy(sort: String) {
        _uiState.update { it.copy(sortBy = sort) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleCategoryGrouping() {
        _uiState.update { it.copy(isGroupedByCategory = !it.isGroupedByCategory) }
    }

    private fun loadBuyAgainItems() {
        viewModelScope.launch {
            try {
                val frequentItems = purchaseHistoryDao.getFrequentlyPurchasedItems(20)
                val activeItemIds = shoppingListRepository.getActiveItemIds()
                val filtered = frequentItems
                    .filter { it.itemId !in activeItemIds }
                    .map { BuyAgainItem(it.itemId, it.itemName, it.purchaseCount, it.unitId) }
                _uiState.update { it.copy(buyAgainItems = filtered) }
            } catch (_: Exception) {
                // Non-critical
            }
        }
    }

    fun addBuyAgainItemToList(item: BuyAgainItem) {
        viewModelScope.launch {
            try {
                val activeIds = shoppingListRepository.getActiveItemIds()
                if (item.itemId in activeIds) return@launch

                shoppingListRepository.addItem(
                    ShoppingListItemEntity(
                        itemId = item.itemId,
                        quantity = 1.0,
                        unitId = item.unitId,
                        priority = 0
                    )
                )
                _uiState.update { state ->
                    state.copy(
                        buyAgainItems = state.buyAgainItems.filter { it.itemId != item.itemId },
                        message = "${item.itemName} added to shopping list"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add item: ${e.message}") }
            }
        }
    }

    private fun loadPredictions() {
        viewModelScope.launch {
            try {
                val predictions = withContext(Dispatchers.Default) {
                    val rows = purchaseHistoryDao.getPurchaseDataForVelocity()
                    if (rows.isEmpty()) return@withContext emptyList()

                    val purchasesByItem = mutableMapOf<Long, MutableList<PurchaseDataPoint>>()
                    val currentStock = mutableMapOf<Long, Double>()
                    val itemNames = mutableMapOf<Long, String>()
                    val itemUnits = mutableMapOf<Long, Long?>()

                    for (row in rows) {
                        purchasesByItem.getOrPut(row.itemId) { mutableListOf() }
                            .add(PurchaseDataPoint(LocalDate.ofEpochDay(row.purchaseDate), row.quantity))
                        currentStock[row.itemId] = row.currentQty
                        itemNames[row.itemId] = row.itemName
                        itemUnits[row.itemId] = row.unitId
                    }

                    ConsumptionVelocityCalculator.calculatePredictions(
                        purchasesByItem, currentStock, itemNames, itemUnits
                    )
                }

                // Filter out items already on shopping list
                val activeItemIds = shoppingListRepository.getActiveItemIds()
                val filtered = predictions.filter { it.itemId !in activeItemIds }

                _uiState.update { it.copy(predictions = filtered) }
            } catch (_: Exception) {
                // Predictions are non-critical — silently fail
            }
        }
    }

    private var quickAddSearchJob: kotlinx.coroutines.Job? = null

    fun updateQuickAddQuery(query: String) {
        quickAddSearchJob?.cancel()
        if (query.isBlank() || query.length < 2) {
            _uiState.update { it.copy(quickAddSuggestions = emptyList()) }
            return
        }
        // Strip leading quantity to search just the name part
        val nameQuery = Regex("""^\d+(?:\.\d+)?\s*[xX]?\s+""").replace(query, "").trim()
        if (nameQuery.length < 2) {
            _uiState.update { it.copy(quickAddSuggestions = emptyList()) }
            return
        }
        quickAddSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(200) // debounce
            try {
                val activeItemIds = shoppingListRepository.getActiveItemIds()
                val catMap = _uiState.value.categories
                val seenNames = mutableSetOf<String>() // deduplicate by lowercase name
                val results = mutableListOf<QuickAddSuggestion>()

                // --- Tier 1: Purchase history (frequently bought, matching query) ---
                val frequentItems = purchaseHistoryDao.searchFrequentlyPurchased(nameQuery, 5)
                for (item in frequentItems) {
                    if (item.itemId in activeItemIds) continue
                    val key = item.itemName.lowercase()
                    if (key in seenNames) continue
                    seenNames.add(key)

                    val categoryName = CategoryMatcher.matchCategory(item.itemName)
                    results.add(QuickAddSuggestion(
                        itemId = item.itemId,
                        name = item.itemName,
                        source = "frequent",
                        categoryName = categoryName,
                        purchaseCount = item.purchaseCount,
                        contextLabel = "bought ${item.purchaseCount}×",
                        defaultQuantity = 1.0
                    ))
                }

                // --- Tier 2: Inventory items (in stock, matching query) ---
                if (results.size < 5) {
                    val inventoryItems = itemRepository.search(nameQuery)
                    for (item in inventoryItems) {
                        if (results.size >= 5) break
                        if (item.id in activeItemIds) continue
                        val key = item.name.lowercase()
                        if (key in seenNames) continue
                        seenNames.add(key)

                        val categoryName = item.categoryId?.let { catMap[it] }
                            ?: CategoryMatcher.matchCategory(item.name)
                        val stockLabel = if (item.quantity > 0) {
                            "in stock · ${item.quantity.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }}"
                        } else {
                            "in inventory"
                        }
                        results.add(QuickAddSuggestion(
                            itemId = item.id,
                            name = item.name,
                            source = "inventory",
                            categoryName = categoryName,
                            contextLabel = stockLabel,
                            defaultQuantity = 1.0
                        ))
                    }
                }

                // --- Tier 3: SmartDefaults fallback (common grocery items) ---
                if (results.size < 5) {
                    val commonNames = SmartDefaults.suggestNames(nameQuery, 5)
                    for (name in commonNames) {
                        if (results.size >= 5) break
                        val key = name.lowercase()
                        if (key in seenNames) continue
                        seenNames.add(key)

                        val categoryName = CategoryMatcher.matchCategory(name)
                        results.add(QuickAddSuggestion(
                            itemId = 0L,
                            name = name,
                            source = "common",
                            categoryName = categoryName,
                            contextLabel = "common item",
                            defaultQuantity = 1.0
                        ))
                    }
                }

                _uiState.update { it.copy(quickAddSuggestions = results.take(5)) }
            } catch (_: Exception) {
                _uiState.update { it.copy(quickAddSuggestions = emptyList()) }
            }
        }
    }

    fun clearQuickAddSuggestions() {
        quickAddSearchJob?.cancel()
        _uiState.update { it.copy(quickAddSuggestions = emptyList()) }
    }

    fun quickAddItem(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val parts = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
                var added = 0
                var updated = 0
                val names = mutableListOf<String>()

                for (part in parts) {
                    val match = Regex("""^(\d+(?:\.\d+)?)\s*[xX]?\s+(.+)$""").find(part)
                    val qty: Double
                    val name: String
                    if (match != null) {
                        qty = match.groupValues[1].toDoubleOrNull() ?: 1.0
                        name = match.groupValues[2].trim()
                    } else {
                        qty = 1.0
                        name = part
                    }
                    if (name.isBlank()) continue

                    addMutex.withLock {
                        val existingItem = itemRepository.findByName(name)

                        // Check if already on list — increment qty instead of duplicating
                        val onList = if (existingItem != null) {
                            shoppingListRepository.findActiveByItemId(existingItem.id)
                        } else {
                            shoppingListRepository.findActiveByCustomName(name)
                        }

                        if (onList != null) {
                            val newQty = onList.quantity + qty
                            shoppingListRepository.updateQuantity(onList.id, newQty)
                            updated++
                            names.add(name)
                        } else {
                            shoppingListRepository.addItem(
                                if (existingItem != null) {
                                    ShoppingListItemEntity(
                                        itemId = existingItem.id,
                                        quantity = qty
                                    )
                                } else {
                                    ShoppingListItemEntity(
                                        customName = name,
                                        quantity = qty
                                    )
                                }
                            )
                            added++
                            names.add(name)
                        }
                    }
                }

                val total = added + updated
                val message = when {
                    total == 0 -> null
                    total == 1 && added == 1 -> "${names.firstOrNull() ?: "Item"} added"
                    total == 1 && updated == 1 -> "${names.firstOrNull() ?: "Item"} updated"
                    updated == 0 -> "$added items added"
                    added == 0 -> "$updated items updated"
                    else -> "$added added, $updated updated"
                }
                _uiState.update {
                    it.copy(
                        quickAddSuggestions = emptyList(),
                        message = message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add: ${e.message}") }
            }
        }
    }

    fun quickAddSuggestionItem(suggestion: QuickAddSuggestion) {
        viewModelScope.launch {
            try {
                addMutex.withLock {
                    val qty = suggestion.defaultQuantity
                    if (suggestion.itemId != 0L) {
                        // Inventory or frequent item — add by item ID
                        val activeIds = shoppingListRepository.getActiveItemIds()
                        if (suggestion.itemId in activeIds) {
                            _uiState.update { it.copy(message = "${suggestion.name} already on list") }
                            return@withLock
                        }
                        shoppingListRepository.addItem(
                            ShoppingListItemEntity(
                                itemId = suggestion.itemId,
                                quantity = qty
                            )
                        )
                    } else {
                        // SmartDefaults common item — add as custom name
                        // First check if there's an inventory match by name
                        val existingItem = itemRepository.findByName(suggestion.name)
                        if (existingItem != null) {
                            val activeIds = shoppingListRepository.getActiveItemIds()
                            if (existingItem.id in activeIds) {
                                _uiState.update { it.copy(message = "${suggestion.name} already on list") }
                                return@withLock
                            }
                            shoppingListRepository.addItem(
                                ShoppingListItemEntity(
                                    itemId = existingItem.id,
                                    quantity = qty
                                )
                            )
                        } else {
                            shoppingListRepository.addItem(
                                ShoppingListItemEntity(
                                    customName = suggestion.name,
                                    quantity = qty
                                )
                            )
                        }
                    }
                    _uiState.update {
                        it.copy(
                            quickAddSuggestions = emptyList(),
                            message = "${suggestion.name} added"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add: ${e.message}") }
            }
        }
    }

    fun addPredictionToShoppingList(prediction: ConsumptionPrediction) {
        viewModelScope.launch {
            try {
                // Guard against duplicates
                val activeIds = shoppingListRepository.getActiveItemIds()
                if (prediction.itemId in activeIds) return@launch

                shoppingListRepository.addItem(
                    ShoppingListItemEntity(
                        itemId = prediction.itemId,
                        quantity = prediction.suggestedQuantity,
                        unitId = prediction.unitId,
                        priority = if (prediction.daysRemaining <= 1) 2 else 1 // Urgent or High
                    )
                )

                // Optimistically remove from predictions list
                _uiState.update { state ->
                    state.copy(
                        predictions = state.predictions.filter { it.itemId != prediction.itemId },
                        message = "${prediction.itemName} added to shopping list"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add item: ${e.message}") }
            }
        }
    }
}
