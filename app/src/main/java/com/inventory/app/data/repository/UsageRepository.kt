package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.TopUsageItemRow
import com.inventory.app.data.local.dao.UsageByTypeRow
import com.inventory.app.data.local.dao.UsageLogDao
import com.inventory.app.data.local.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageLogDao: UsageLogDao,
    private val itemRepository: ItemRepository
) {
    fun getByItemId(itemId: Long): Flow<List<UsageLogEntity>> = usageLogDao.getByItemId(itemId)

    fun getRecent(limit: Int = 10): Flow<List<UsageLogEntity>> = usageLogDao.getRecent(limit)

    fun getTotalUsageSince(since: LocalDate): Flow<Double> =
        usageLogDao.getTotalUsageSince(since.toEpochDay())

    fun getUsageByType(since: LocalDate): Flow<List<UsageByTypeRow>> =
        usageLogDao.getUsageByType(since.toEpochDay())

    fun getTopItemsByUsageType(since: LocalDate, usageType: String, limit: Int = 10): Flow<List<TopUsageItemRow>> =
        usageLogDao.getTopItemsByUsageType(since.toEpochDay(), usageType, limit)

    suspend fun logUsage(itemId: Long, quantity: Double, usageType: String, notes: String?) {
        usageLogDao.insert(
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
}
