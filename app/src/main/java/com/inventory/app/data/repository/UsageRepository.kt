package com.inventory.app.data.repository

import androidx.room.withTransaction
import com.inventory.app.data.local.dao.DailyWasteRow
import com.inventory.app.data.local.dao.TopUsageItemRow
import com.inventory.app.data.local.dao.UsageByTypeRow
import com.inventory.app.data.local.dao.UsageLogDao
import com.inventory.app.data.local.dao.WasteLogWithCost
import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.data.local.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageLogDao: UsageLogDao,
    private val itemRepository: ItemRepository,
    private val database: InventoryDatabase
) {
    fun getByItemId(itemId: Long): Flow<List<UsageLogEntity>> = usageLogDao.getByItemId(itemId)

    fun getRecent(limit: Int = 10): Flow<List<UsageLogEntity>> = usageLogDao.getRecent(limit)

    fun getTotalUsageSince(since: LocalDate): Flow<Double> =
        usageLogDao.getTotalUsageSince(since.toEpochDay())

    fun getUsageByType(since: LocalDate): Flow<List<UsageByTypeRow>> =
        usageLogDao.getUsageByType(since.toEpochDay())

    fun getTopItemsByUsageType(since: LocalDate, usageType: String, limit: Int = 10): Flow<List<TopUsageItemRow>> =
        usageLogDao.getTopItemsByUsageType(since.toEpochDay(), usageType, limit)

    suspend fun getTypicalServing(itemId: Long): Double? {
        val quantities = usageLogDao.getRecentConsumedQuantities(itemId)
        if (quantities.isEmpty()) return null
        val sorted = quantities.sorted()
        return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    }

    suspend fun getDailyConsumptionRate(itemId: Long): Double? {
        val sinceEpochDay = LocalDate.now().minusDays(90).toEpochDay()
        val logs = usageLogDao.getConsumedSince(itemId, sinceEpochDay)
        if (logs.size < 2) return null
        val firstDay = logs.first().usageDate
        val lastDay = logs.last().usageDate
        val daySpan = lastDay - firstDay
        if (daySpan <= 0) return null
        val totalConsumed = logs.sumOf { it.quantity }
        return totalConsumed / daySpan.toDouble()
    }

    suspend fun logUsage(itemId: Long, quantity: Double, usageType: String, notes: String?): Long {
        require(quantity > 0) { "Usage quantity must be positive" }
        var id = 0L
        database.withTransaction {
            id = usageLogDao.insert(
                UsageLogEntity(
                    itemId = itemId,
                    quantity = quantity,
                    usageType = usageType,
                    usageDate = LocalDate.now(),
                    notes = notes
                )
            )
            // Decrease item quantity
            itemRepository.adjustQuantity(itemId, -quantity)
        }
        return id
    }

    /** Deletes the usage log entry only. Caller must handle quantity restoration if needed. */
    suspend fun deleteUsageLog(id: Long) {
        usageLogDao.delete(id)
    }

    // --- Waste Tracking ---

    fun getWasteLogsSince(since: LocalDate): Flow<List<WasteLogWithCost>> =
        usageLogDao.getWasteLogsSince(since.toEpochDay())

    fun getDailyWasteSince(since: LocalDate): Flow<List<DailyWasteRow>> =
        usageLogDao.getDailyWasteSince(since.toEpochDay())

    /**
     * Auto-log waste with duplicate guard. Tagged "auto-detected" in notes
     * so restore can clean up only auto-generated entries.
     * Does NOT adjust quantity — caller handles that separately.
     */
    private suspend fun autoLogWaste(
        itemId: Long,
        quantity: Double,
        usageType: String,
        notes: String
    ): Long? {
        val today = LocalDate.now().toEpochDay()
        if (usageLogDao.hasAutoWasteLogForItemOnDate(itemId, today) > 0) return null

        return usageLogDao.insert(
            UsageLogEntity(
                itemId = itemId,
                quantity = quantity,
                usageType = usageType,
                usageDate = LocalDate.now(),
                notes = notes
            )
        )
    }

    /**
     * Soft-deletes an item AND auto-logs waste if the item is expired with remaining quantity.
     * Returns true if waste was auto-logged.
     */
    suspend fun softDeleteWithWasteDetection(itemId: Long): Boolean {
        return database.withTransaction {
            val item = itemRepository.getById(itemId) ?: return@withTransaction false

            val isExpired = item.expiryDate != null &&
                item.expiryDate.isBefore(LocalDate.now())
            val hasQuantity = item.quantity > 0

            // Soft delete first
            itemRepository.softDelete(itemId)

            if (isExpired && hasQuantity) {
                autoLogWaste(
                    itemId = itemId,
                    quantity = item.quantity,
                    usageType = "expired",
                    notes = "auto-detected: deleted while expired"
                )
                true
            } else {
                false
            }
        }
    }

    /**
     * Restores a soft-deleted item AND removes any auto-logged waste entries.
     * Manual waste logs (without "auto-detected" tag) are preserved.
     */
    suspend fun restoreWithWasteCleanup(itemId: Long) {
        itemRepository.restore(itemId)
        usageLogDao.deleteAutoWasteLogsForItem(itemId)
    }
}
