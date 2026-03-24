package com.inventory.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_log WHERE item_id = :itemId ORDER BY usage_date DESC, created_at DESC")
    fun getByItemId(itemId: Long): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_log ORDER BY usage_date DESC, created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<UsageLogEntity>>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM usage_log WHERE usage_date >= :since")
    fun getTotalUsageSince(since: Long): Flow<Double>

    @Query("""
        SELECT usage_type as type, COUNT(*) as count, COALESCE(SUM(quantity), 0) as totalQuantity
        FROM usage_log
        WHERE usage_date >= :since
        GROUP BY usage_type
    """)
    fun getUsageByType(since: Long): Flow<List<UsageByTypeRow>>

    @Query("""
        SELECT i.name as itemName, COALESCE(SUM(ul.quantity), 0) as totalQuantity
        FROM usage_log ul
        JOIN items i ON ul.item_id = i.id
        WHERE ul.usage_date >= :since AND ul.usage_type = :usageType
        GROUP BY ul.item_id
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """)
    fun getTopItemsByUsageType(since: Long, usageType: String, limit: Int = 10): Flow<List<TopUsageItemRow>>

    @Query("""
        SELECT quantity FROM usage_log
        WHERE item_id = :itemId AND usage_type = 'consumed'
        ORDER BY usage_date DESC, created_at DESC LIMIT 10
    """)
    suspend fun getRecentConsumedQuantities(itemId: Long): List<Double>

    @Query("""
        SELECT quantity, usage_date FROM usage_log
        WHERE item_id = :itemId AND usage_type = 'consumed'
          AND usage_date >= :sinceEpochDay
        ORDER BY usage_date ASC
    """)
    suspend fun getConsumedSince(itemId: Long, sinceEpochDay: Long): List<UsageForVelocity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usageLog: UsageLogEntity): Long

    @Query("DELETE FROM usage_log WHERE id = :id")
    suspend fun delete(id: Long)

    // --- Waste Tracking queries ---

    @Query("""
        SELECT ul.item_id, ul.quantity, ul.usage_type, ul.usage_date, ul.notes,
               i.name as item_name, i.purchase_price as item_purchase_price,
               (SELECT ph.unit_price FROM purchase_history ph
                WHERE ph.item_id = ul.item_id
                ORDER BY ph.purchase_date DESC LIMIT 1) as latest_unit_price
        FROM usage_log ul
        JOIN items i ON ul.item_id = i.id
        WHERE ul.usage_type IN ('wasted', 'expired')
          AND ul.usage_date >= :sinceEpochDay
        ORDER BY ul.usage_date DESC
    """)
    fun getWasteLogsSince(sinceEpochDay: Long): Flow<List<WasteLogWithCost>>

    @Query("""
        SELECT COUNT(*) FROM usage_log
        WHERE item_id = :itemId
          AND usage_type IN ('wasted', 'expired')
          AND notes LIKE '%auto-detected%'
          AND usage_date = :dateEpochDay
    """)
    suspend fun hasAutoWasteLogForItemOnDate(itemId: Long, dateEpochDay: Long): Int

    @Query("""
        SELECT usage_date as date, SUM(quantity) as totalQuantity, COUNT(*) as logCount
        FROM usage_log
        WHERE usage_type IN ('wasted', 'expired')
          AND usage_date >= :sinceEpochDay
        GROUP BY usage_date
        ORDER BY usage_date ASC
    """)
    fun getDailyWasteSince(sinceEpochDay: Long): Flow<List<DailyWasteRow>>

    @Query("DELETE FROM usage_log WHERE item_id = :itemId AND usage_type IN ('wasted', 'expired') AND notes LIKE '%auto-detected%'")
    suspend fun deleteAutoWasteLogsForItem(itemId: Long)
}

data class UsageForVelocity(
    val quantity: Double,
    @androidx.room.ColumnInfo(name = "usage_date") val usageDate: Long
)

data class UsageByTypeRow(
    val type: String,
    val count: Int,
    val totalQuantity: Double
)

data class TopUsageItemRow(
    val itemName: String,
    val totalQuantity: Double
)

data class WasteLogWithCost(
    @ColumnInfo(name = "item_id") val itemId: Long,
    val quantity: Double,
    @ColumnInfo(name = "usage_type") val usageType: String,
    @ColumnInfo(name = "usage_date") val usageDate: Long,
    val notes: String?,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "item_purchase_price") val itemPurchasePrice: Double?,
    @ColumnInfo(name = "latest_unit_price") val latestUnitPrice: Double?
)

data class DailyWasteRow(
    val date: Long,
    val totalQuantity: Double,
    val logCount: Int
)
