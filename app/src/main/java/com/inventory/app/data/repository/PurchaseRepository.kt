package com.inventory.app.data.repository

import androidx.room.withTransaction
import com.inventory.app.data.local.dao.DailySpendingRow
import com.inventory.app.data.local.dao.FrequentPurchaseRow
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.dao.PurchaseWithItemDetails
import com.inventory.app.data.local.dao.SpendingByCategoryRow
import com.inventory.app.data.local.dao.SpendingByStoreRow
import com.inventory.app.data.local.dao.StoreDao
import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.StoreEntity
import com.inventory.app.domain.model.SmartDefaults
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor(
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val storeDao: StoreDao,
    private val itemRepository: ItemRepository,
    private val database: InventoryDatabase
) {
    fun getByItemId(itemId: Long): Flow<List<PurchaseHistoryEntity>> =
        purchaseHistoryDao.getByItemId(itemId)

    fun getRecent(limit: Int = 10): Flow<List<PurchaseHistoryEntity>> =
        purchaseHistoryDao.getRecent(limit)

    fun getAllPurchases(): Flow<List<PurchaseHistoryEntity>> =
        purchaseHistoryDao.getAll()

    fun getTotalSpendingSince(since: LocalDate): Flow<Double> =
        purchaseHistoryDao.getTotalSpendingSince(since.toEpochDay())

    fun getTotalSpendingBetween(start: LocalDate, end: LocalDate): Flow<Double> =
        purchaseHistoryDao.getTotalSpendingBetween(start.toEpochDay(), end.toEpochDay())

    fun getSpendingByCategory(since: LocalDate): Flow<List<SpendingByCategoryRow>> =
        purchaseHistoryDao.getSpendingByCategory(since.toEpochDay())

    fun getDailySpending(since: LocalDate): Flow<List<DailySpendingRow>> =
        purchaseHistoryDao.getDailySpending(since.toEpochDay())

    fun getTopPurchases(since: LocalDate, limit: Int = 5): Flow<List<PurchaseWithItemDetails>> =
        purchaseHistoryDao.getTopPurchases(since.toEpochDay(), limit)

    fun getPurchaseCount(since: LocalDate): Flow<Int> =
        purchaseHistoryDao.getPurchaseCount(since.toEpochDay())

    fun getAllWithDetails(): Flow<List<PurchaseWithItemDetails>> =
        purchaseHistoryDao.getAllWithDetails()

    fun getByItemIdWithDetails(itemId: Long): Flow<List<PurchaseWithItemDetails>> =
        purchaseHistoryDao.getByItemIdWithDetails(itemId)

    fun getSpendingByStore(since: LocalDate): Flow<List<SpendingByStoreRow>> =
        purchaseHistoryDao.getSpendingByStore(since.toEpochDay())

    fun getDailySpendingBetween(start: LocalDate, end: LocalDate): Flow<List<DailySpendingRow>> =
        purchaseHistoryDao.getDailySpendingBetween(start.toEpochDay(), end.toEpochDay())

    fun getAllWithDetailsSince(since: LocalDate): Flow<List<PurchaseWithItemDetails>> =
        purchaseHistoryDao.getAllWithDetailsSince(since.toEpochDay())

    suspend fun getFrequentlyPurchasedItems(limit: Int = 10): List<FrequentPurchaseRow> =
        purchaseHistoryDao.getFrequentlyPurchasedItems(limit)

    fun getAllStores(): Flow<List<StoreEntity>> = storeDao.getAllActive()

    suspend fun getStoreById(id: Long): StoreEntity? = storeDao.getById(id)

    suspend fun addPurchase(
        itemId: Long,
        quantity: Double,
        totalPrice: Double?,
        purchaseDate: LocalDate,
        expiryDate: LocalDate?,
        storeName: String?,
        notes: String?
    ) {
        if (quantity <= 0) return  // Guard against zero/negative quantity
        database.withTransaction {
        // Find or create store
        val storeId = storeName?.let { name ->
            storeDao.findByName(name)?.id ?: storeDao.insert(StoreEntity(name = name))
        }

        val unitPrice = if (quantity > 0 && totalPrice != null) totalPrice / quantity else null

        // Get the item to recalculate expiry date
        val item = itemRepository.getById(itemId)

        // Calculate new expiry date based on smart defaults shelf life
        val newExpiryDate = expiryDate ?: item?.let {
            val defaults = SmartDefaults.lookup(it.name)
            defaults?.shelfLifeDays?.let { days ->
                purchaseDate.plusDays(days.toLong())
            }
        }

        purchaseHistoryDao.insert(
            PurchaseHistoryEntity(
                itemId = itemId,
                storeId = storeId,
                quantity = quantity,
                unitPrice = unitPrice,
                totalPrice = totalPrice,
                purchaseDate = purchaseDate,
                expiryDate = newExpiryDate,
                notes = notes
            )
        )

        // Increase item quantity
        itemRepository.adjustQuantity(itemId, quantity)

        // Re-fetch item AFTER quantity adjustment to avoid stale data overwrite
        val updatedItem = itemRepository.getById(itemId)
        updatedItem?.let {
            itemRepository.update(
                it.copy(
                    purchaseDate = purchaseDate,
                    purchasePrice = totalPrice ?: it.purchasePrice,
                    expiryDate = newExpiryDate ?: it.expiryDate,
                    updatedAt = LocalDateTime.now()
                )
            )
        }
        }
    }
}
