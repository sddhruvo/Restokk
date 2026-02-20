package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.ChartDataRow
import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.dao.InventoryMatchCandidate
import com.inventory.app.data.local.dao.WasteCheckItemRow
import com.inventory.app.data.local.dao.ItemImageDao
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.ItemImageEntity
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val itemImageDao: ItemImageDao
) {
    private fun now(): Long = LocalDateTime.now()
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun dateToEpoch(date: LocalDate): Long = date.toEpochDay()

    fun getAllActiveWithDetails(): Flow<List<ItemWithDetails>> = itemDao.getAllActiveWithDetails()

    fun getByIdWithDetails(id: Long): Flow<ItemWithDetails?> = itemDao.getByIdWithDetails(id)

    suspend fun getById(id: Long): ItemEntity? = itemDao.getById(id)

    suspend fun findByBarcode(barcode: String): ItemEntity? = itemDao.findByBarcode(barcode)

    fun getFiltered(
        search: String?,
        categoryId: Long?,
        locationId: Long?,
        sortBy: String
    ): Flow<List<ItemEntity>> = itemDao.getFiltered(search, categoryId, locationId, sortBy)

    // Dashboard queries
    fun getTotalItemCount(): Flow<Int> = itemDao.getTotalItemCount()

    fun getExpiringSoonCount(warningDays: Int = 7): Flow<Int> {
        val today = dateToEpoch(LocalDate.now())
        val future = dateToEpoch(LocalDate.now().plusDays(warningDays.toLong()))
        return itemDao.getExpiringSoonCount(today, future)
    }

    fun getExpiredCount(): Flow<Int> = itemDao.getExpiredCount(dateToEpoch(LocalDate.now()))

    fun getLowStockCount(): Flow<Int> = itemDao.getLowStockCount()

    fun getOutOfStockCount(): Flow<Int> = itemDao.getOutOfStockCount()

    fun getItemsWithCategoryCount(): Flow<Int> = itemDao.getItemsWithCategoryCount()
    fun getItemsWithLocationCount(): Flow<Int> = itemDao.getItemsWithLocationCount()
    fun getItemsWithExpiryCount(): Flow<Int> = itemDao.getItemsWithExpiryCount()

    fun getTotalValue(): Flow<Double> = itemDao.getTotalValue()

    fun getExpiringSoon(warningDays: Int = 7, limit: Int = 5): Flow<List<ItemWithDetails>> =
        itemDao.getExpiringSoon(dateToEpoch(LocalDate.now().plusDays(warningDays.toLong())), limit)

    fun getLowStockItems(limit: Int = 5): Flow<List<ItemWithDetails>> =
        itemDao.getLowStockItems(limit)

    fun getItemCountByCategory(limit: Int = 10): Flow<List<ChartDataRow>> =
        itemDao.getItemCountByCategory(limit)

    fun getItemCountByLocation(limit: Int = 10): Flow<List<ChartDataRow>> =
        itemDao.getItemCountByLocation(limit)

    fun getByLocation(locationId: Long): Flow<List<ItemWithDetails>> =
        itemDao.getByLocation(locationId)

    fun getExpiredItems(): Flow<List<ItemWithDetails>> =
        itemDao.getExpiredItems(dateToEpoch(LocalDate.now()))

    fun getExpiringItemsReport(warningDays: Int): Flow<List<ItemWithDetails>> {
        val today = dateToEpoch(LocalDate.now())
        val future = dateToEpoch(LocalDate.now().plusDays(warningDays.toLong()))
        return itemDao.getExpiringItemsReport(today, future)
    }

    fun getLowStockItemsReport(): Flow<List<ItemWithDetails>> = itemDao.getLowStockItemsReport()
    fun getOutOfStockItemsReport(): Flow<List<ItemWithDetails>> = itemDao.getOutOfStockItemsReport()

    suspend fun getLowStockItemsList(): List<ItemEntity> = itemDao.getLowStockItemsList()

    fun getRecentItemCount(daysSince: Int = 7): Flow<Int> {
        val since = LocalDateTime.now().minusDays(daysSince.toLong())
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return itemDao.getRecentItemCount(since)
    }

    suspend fun getInStockItems(): List<WasteCheckItemRow> = itemDao.getInStockItems()

    suspend fun findByName(name: String): ItemEntity? = itemDao.findByName(name)

    suspend fun searchByKeyword(keyword: String): List<InventoryMatchCandidate> = itemDao.searchByKeyword(keyword)

    suspend fun getAllActiveNamesAndIds(): List<com.inventory.app.data.local.dao.InventoryNameId> = itemDao.getAllActiveNamesAndIds()

    suspend fun search(query: String): List<ItemEntity> = itemDao.search(query)

    suspend fun suggestNames(query: String, limit: Int = 5): List<String> = itemDao.suggestNames(query, limit)

    suspend fun insert(item: ItemEntity): Long = itemDao.insert(item)

    suspend fun update(item: ItemEntity) =
        itemDao.update(item.copy(updatedAt = LocalDateTime.now()))

    suspend fun softDelete(id: Long) = itemDao.softDelete(id, now())

    suspend fun restore(id: Long) = itemDao.restore(id, now())

    suspend fun adjustQuantity(id: Long, delta: Double) = itemDao.adjustQuantity(id, delta, now())

    suspend fun updatePurchaseDate(id: Long, date: LocalDate?) {
        val epochDay = date?.toEpochDay()
        itemDao.updatePurchaseDate(id, epochDay, now())
    }

    suspend fun updateExpiryDate(id: Long, date: LocalDate) {
        itemDao.updateExpiryDateIfNull(id, date.toEpochDay(), now())
    }

    suspend fun updateBarcode(id: Long, barcode: String) {
        itemDao.updateBarcodeIfEmpty(id, barcode, now())
    }

    suspend fun getPurchaseDate(id: Long): LocalDate? {
        val epochDay = itemDao.getPurchaseDate(id) ?: return null
        return LocalDate.ofEpochDay(epochDay)
    }

    suspend fun toggleFavorite(id: Long) = itemDao.toggleFavorite(id, now())

    // Image operations
    fun getImages(itemId: Long): Flow<List<ItemImageEntity>> = itemImageDao.getByItemId(itemId)

    suspend fun addImage(image: ItemImageEntity): Long = itemImageDao.insert(image)

    suspend fun deleteImage(id: Long) {
        val image = itemImageDao.getById(id) ?: return
        // Delete physical file from disk
        try {
            val file = File(image.filename)
            if (file.exists()) file.delete()
        } catch (_: Exception) { }
        itemImageDao.delete(id)
        // If deleted image was primary, promote the next remaining image
        if (image.isPrimary) {
            val remaining = itemImageDao.getByItemId(image.itemId).first()
            remaining.firstOrNull()?.let { next ->
                itemImageDao.setPrimaryAtomic(image.itemId, next.id)
            }
        }
    }

    suspend fun setPrimaryImage(itemId: Long, imageId: Long) {
        itemImageDao.setPrimaryAtomic(itemId, imageId)
    }
}
