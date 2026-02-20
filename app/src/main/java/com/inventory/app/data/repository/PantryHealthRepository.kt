package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.PantryHealthLogDao
import com.inventory.app.data.local.entity.PantryHealthLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryHealthRepository @Inject constructor(
    private val dao: PantryHealthLogDao
) {
    suspend fun recordSnapshot(
        score: Int,
        expiredCount: Int,
        expiringSoonCount: Int,
        lowStockCount: Int,
        outOfStockCount: Int,
        shoppingCompletionPct: Float,
        totalItems: Int = 0,
        engagementScore: Int = 0,
        conditionScore: Int = 0
    ) {
        dao.upsert(
            PantryHealthLogEntity(
                score = score,
                date = LocalDate.now(),
                expiredCount = expiredCount,
                expiringSoonCount = expiringSoonCount,
                lowStockCount = lowStockCount,
                outOfStockCount = outOfStockCount,
                shoppingCompletionPct = shoppingCompletionPct,
                totalItems = totalItems,
                engagementScore = engagementScore,
                conditionScore = conditionScore
            )
        )
    }

    fun getLogsSince(date: LocalDate): Flow<List<PantryHealthLogEntity>> =
        dao.getLogsSince(date.toEpochDay())

    fun getRecentLogs(days: Int): Flow<List<PantryHealthLogEntity>> =
        dao.getRecentLogs(days)

    suspend fun getLatest(): PantryHealthLogEntity? = dao.getLatest()
}
