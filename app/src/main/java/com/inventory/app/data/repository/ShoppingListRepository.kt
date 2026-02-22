package com.inventory.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.dao.ShoppingListDao
import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.local.entity.relations.ShoppingListItemWithDetails
import com.inventory.app.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.inventory.app.domain.model.ShoppingListMatcher
import com.inventory.app.domain.model.ShoppingMatch
import com.inventory.app.domain.model.ShoppingNameInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shoppingListDao: ShoppingListDao,
    private val itemRepository: ItemRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val database: InventoryDatabase
) {
    private val toggleMutex = Mutex()
    private val toggleInProgress = mutableSetOf<Long>()

    private fun now(): Long = LocalDateTime.now()
        .atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun getActiveItems(): Flow<List<ShoppingListItemWithDetails>> = shoppingListDao.getActiveItems()

    fun getPurchasedItems(): Flow<List<ShoppingListItemWithDetails>> = shoppingListDao.getPurchasedItems()

    fun getActiveCount(): Flow<Int> = shoppingListDao.getActiveCount()

    fun getPurchasedCount(): Flow<Int> = shoppingListDao.getPurchasedCount()

    suspend fun addItem(item: ShoppingListItemEntity): Long {
        val id = shoppingListDao.insert(item)
        WidgetUpdater.requestUpdate(context)
        return id
    }

    suspend fun togglePurchased(id: Long) {
        val acquired = toggleMutex.withLock {
            toggleInProgress.add(id) // Returns false if already toggling this item
        }
        if (!acquired) return
        try {
            database.withTransaction {
                // Get the item before toggling to check current state
                val shoppingItem = shoppingListDao.getById(id)

                // Toggle the purchased flag
                shoppingListDao.togglePurchased(id, now())

                if (shoppingItem != null && shoppingItem.itemId != null) {
                    val itemId = shoppingItem.itemId
                    if (!shoppingItem.isPurchased) {
                        // Marking as purchased → create purchase history + increase inventory qty + update purchase date
                        val item = itemRepository.getById(itemId)
                        // Get unit price from latest purchase history (most reliable)
                        // purchase_price in items table is TOTAL price, not per-unit
                        val latestPrices = purchaseHistoryDao.getLatestPricesForItems(listOf(itemId))
                        val latestPrice = latestPrices.firstOrNull()
                        val unitPrice = latestPrice?.unitPrice
                            ?: latestPrice?.let { if (it.totalPrice != null && it.quantity > 0) it.totalPrice / it.quantity else null }
                            ?: item?.purchasePrice?.let { price ->
                                if (item.quantity > 0) price / item.quantity else null
                            }
                        val totalPrice = unitPrice?.let { it * shoppingItem.quantity }

                        purchaseHistoryDao.insert(
                            PurchaseHistoryEntity(
                                itemId = itemId,
                                storeId = null,
                                quantity = shoppingItem.quantity,
                                unitPrice = unitPrice,
                                totalPrice = totalPrice,
                                purchaseDate = LocalDate.now(),
                                expiryDate = null,
                                notes = "From shopping list"
                            )
                        )
                        itemRepository.adjustQuantity(itemId, shoppingItem.quantity)
                        itemRepository.updatePurchaseDate(itemId, LocalDate.now())
                    } else {
                        // Un-marking (undo) → remove purchase history + reverse inventory qty + restore purchase date
                        val purchaseId = purchaseHistoryDao.getLatestShoppingListPurchaseId(itemId)
                        if (purchaseId != null) {
                            purchaseHistoryDao.delete(purchaseId)
                        }
                        itemRepository.adjustQuantity(itemId, -shoppingItem.quantity)

                        // Restore purchase date to the next most recent purchase (or null if none)
                        val previousDate = purchaseHistoryDao.getLatestPurchaseDate(itemId)
                        val restoredDate = previousDate?.let { LocalDate.ofEpochDay(it) }
                        itemRepository.updatePurchaseDate(itemId, restoredDate)
                    }
                }
            }
        } finally {
            toggleMutex.withLock {
                toggleInProgress.remove(id)
            }
            WidgetUpdater.requestUpdate(context)
        }
    }

    suspend fun updateQuantity(id: Long, quantity: Double) {
        shoppingListDao.updateQuantity(id, quantity, now())
    }

    suspend fun getById(id: Long) = shoppingListDao.getById(id)

    suspend fun deleteItem(id: Long) {
        shoppingListDao.delete(id)
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun clearPurchased() {
        shoppingListDao.clearPurchased()
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun clearPurchasedOlderThan(days: Int) {
        val cutoff = LocalDateTime.now().minusDays(days.toLong())
            .atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        shoppingListDao.clearPurchasedBefore(cutoff)
    }

    suspend fun markAsPurchasedOnly(id: Long) {
        shoppingListDao.markAsPurchasedOnly(id, now())
    }

    suspend fun getActiveItemIds(): Set<Long> = shoppingListDao.getActiveItemIds().toSet()

    suspend fun findActiveByItemId(itemId: Long) = shoppingListDao.findActiveByItemId(itemId)

    suspend fun findActiveByCustomName(name: String) = shoppingListDao.findActiveByCustomName(name)

    suspend fun findMatchesForItem(itemName: String): List<ShoppingMatch> {
        val activeItems = shoppingListDao.getActiveItemNames().map {
            ShoppingNameInfo(id = it.id, name = it.name)
        }
        return ShoppingListMatcher.findMatches(itemName, activeItems)
    }

    suspend fun generateFromLowStock(): Int {
        val lowStockItems = itemRepository.getLowStockItemsList()
        var added = 0
        for (item in lowStockItems) {
            // Skip if already on the list
            val existing = shoppingListDao.findActiveByItemId(item.id)
            if (existing != null) continue

            val effectiveMin = if (item.minQuantity > 0) item.minQuantity else item.smartMinQuantity
            val neededQty = if (effectiveMin > item.quantity) {
                effectiveMin - item.quantity
            } else {
                1.0
            }

            shoppingListDao.insert(
                ShoppingListItemEntity(
                    itemId = item.id,
                    quantity = neededQty,
                    unitId = item.unitId,
                    priority = 1 // High priority for auto-generated
                )
            )
            added++
        }
        return added
    }
}
