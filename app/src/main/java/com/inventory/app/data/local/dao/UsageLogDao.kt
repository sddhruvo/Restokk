package com.inventory.app.data.local.dao

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usageLog: UsageLogEntity): Long
}

data class UsageByTypeRow(
    val type: String,
    val count: Int,
    val totalQuantity: Double
)

data class TopUsageItemRow(
    val itemName: String,
    val totalQuantity: Double
)
