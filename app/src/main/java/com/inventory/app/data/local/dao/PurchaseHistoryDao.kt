package com.inventory.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseHistoryDao {
    @Query("SELECT * FROM purchase_history WHERE item_id = :itemId ORDER BY purchase_date DESC")
    fun getByItemId(itemId: Long): Flow<List<PurchaseHistoryEntity>>

    @Query("SELECT * FROM purchase_history ORDER BY purchase_date DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<PurchaseHistoryEntity>>

    @Query("SELECT * FROM purchase_history ORDER BY purchase_date DESC")
    fun getAll(): Flow<List<PurchaseHistoryEntity>>

    @Query("""
        SELECT ph.*, i.name as item_name, s.name as store_name
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id AND i.is_active = 1
        LEFT JOIN stores s ON ph.store_id = s.id
        ORDER BY ph.purchase_date DESC, ph.created_at DESC
    """)
    fun getAllWithDetails(): Flow<List<PurchaseWithItemDetails>>

    @Query("""
        SELECT ph.*, i.name as item_name, s.name as store_name
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        LEFT JOIN stores s ON ph.store_id = s.id
        WHERE ph.item_id = :itemId
        ORDER BY ph.purchase_date DESC, ph.created_at DESC
    """)
    fun getByItemIdWithDetails(itemId: Long): Flow<List<PurchaseWithItemDetails>>

    @Query("SELECT COALESCE(SUM(total_price), 0) FROM purchase_history WHERE purchase_date >= :since")
    fun getTotalSpendingSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(total_price), 0) FROM purchase_history WHERE purchase_date >= :start AND purchase_date < :end")
    fun getTotalSpendingBetween(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(c.name, 'Uncategorized') as label, COALESCE(SUM(ph.total_price), 0) as amount
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        LEFT JOIN categories c ON i.category_id = c.id
        WHERE ph.purchase_date >= :since
        GROUP BY COALESCE(c.name, 'Uncategorized')
        ORDER BY amount DESC
    """)
    fun getSpendingByCategory(since: Long): Flow<List<SpendingByCategoryRow>>

    @Query("""
        SELECT ph.purchase_date as date, COALESCE(SUM(ph.total_price), 0) as amount
        FROM purchase_history ph
        WHERE ph.purchase_date >= :since
        GROUP BY ph.purchase_date
        ORDER BY ph.purchase_date ASC
    """)
    fun getDailySpending(since: Long): Flow<List<DailySpendingRow>>

    @Query("""
        SELECT ph.*, i.name as item_name, s.name as store_name
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        LEFT JOIN stores s ON ph.store_id = s.id
        WHERE ph.purchase_date >= :since
        ORDER BY ph.total_price DESC
        LIMIT :limit
    """)
    fun getTopPurchases(since: Long, limit: Int = 5): Flow<List<PurchaseWithItemDetails>>

    @Query("SELECT COUNT(*) FROM purchase_history WHERE purchase_date >= :since")
    fun getPurchaseCount(since: Long): Flow<Int>

    @Query("""
        SELECT ph.quantity, ph.unit_price, ph.total_price, i.unit_id
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        WHERE LOWER(i.name) = LOWER(:name)
        ORDER BY ph.purchase_date DESC LIMIT 1
    """)
    suspend fun getLatestPurchaseDefaultsByName(name: String): LatestPurchaseDefaults?

    // TODO: Batch itemIds in chunks of 500 to avoid SQLite 999 parameter limit.
    // Prefer getLatestPricesForAllItems() when filtering can be done in Kotlin.
    @Query("""
        SELECT ph.item_id, ph.unit_price, ph.total_price, ph.quantity
        FROM purchase_history ph
        INNER JOIN (
            SELECT item_id, MAX(purchase_date) as max_date
            FROM purchase_history GROUP BY item_id
        ) latest ON ph.item_id = latest.item_id AND ph.purchase_date = latest.max_date
        WHERE ph.item_id IN (:itemIds)
    """)
    suspend fun getLatestPricesForItems(itemIds: List<Long>): List<LatestItemPrice>

    @Query("""
        SELECT ph.item_id, ph.unit_price, ph.total_price, ph.quantity
        FROM purchase_history ph
        INNER JOIN (
            SELECT item_id, MAX(purchase_date) as max_date
            FROM purchase_history GROUP BY item_id
        ) latest ON ph.item_id = latest.item_id AND ph.purchase_date = latest.max_date
    """)
    suspend fun getLatestPricesForAllItems(): List<LatestItemPrice>

    @Query("""
        SELECT ph.item_id, ph.unit_price, ph.total_price, ph.quantity,
               s.name as store_name, ph.purchase_date
        FROM purchase_history ph
        INNER JOIN (
            SELECT item_id, MAX(purchase_date) as max_date
            FROM purchase_history GROUP BY item_id
        ) latest ON ph.item_id = latest.item_id AND ph.purchase_date = latest.max_date
        LEFT JOIN stores s ON ph.store_id = s.id
        WHERE ph.item_id IN (:itemIds)
    """)
    suspend fun getLatestPricesWithStoreForItems(itemIds: List<Long>): List<LatestItemPriceWithStore>

    @Query("""
        SELECT ph.item_id, ph.purchase_date, ph.quantity, i.name as item_name,
               i.quantity as current_qty, i.unit_id
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        WHERE i.is_active = 1
          AND ph.item_id IN (
              SELECT item_id FROM purchase_history GROUP BY item_id HAVING COUNT(*) >= 3
          )
        ORDER BY ph.item_id, ph.purchase_date ASC
    """)
    suspend fun getPurchaseDataForVelocity(): List<VelocityPurchaseRow>

    // Frequently purchased items for "Buy Again" feature (≥2 purchases)
    @Query("""
        SELECT ph.item_id, i.name as item_name, COUNT(*) as purchase_count,
               MAX(ph.purchase_date) as last_purchase_date, i.unit_id
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        WHERE i.is_active = 1
        GROUP BY ph.item_id
        HAVING COUNT(*) >= 2
        ORDER BY COUNT(*) DESC, MAX(ph.purchase_date) DESC
        LIMIT :limit
    """)
    suspend fun getFrequentlyPurchasedItems(limit: Int = 20): List<FrequentPurchaseRow>

    // Search frequently purchased items by name (for typeahead suggestions)
    @Query("""
        SELECT ph.item_id, i.name as item_name, COUNT(*) as purchase_count,
               MAX(ph.purchase_date) as last_purchase_date, i.unit_id
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        WHERE i.is_active = 1
          AND i.name LIKE '%' || :query || '%'
        GROUP BY ph.item_id
        ORDER BY COUNT(*) DESC, MAX(ph.purchase_date) DESC
        LIMIT :limit
    """)
    suspend fun searchFrequentlyPurchased(query: String, limit: Int = 5): List<FrequentPurchaseRow>

    // Spending by store
    @Query("""
        SELECT COALESCE(s.name, 'Unknown') as label,
               COALESCE(SUM(ph.total_price), 0) as amount
        FROM purchase_history ph
        LEFT JOIN stores s ON ph.store_id = s.id
        WHERE ph.purchase_date >= :since
        GROUP BY COALESCE(s.name, 'Unknown')
        ORDER BY amount DESC
    """)
    fun getSpendingByStore(since: Long): Flow<List<SpendingByStoreRow>>

    // Daily spending between two dates (for period comparison)
    @Query("""
        SELECT ph.purchase_date as date, COALESCE(SUM(ph.total_price), 0) as amount
        FROM purchase_history ph
        WHERE ph.purchase_date >= :start AND ph.purchase_date < :end
        GROUP BY ph.purchase_date
        ORDER BY ph.purchase_date ASC
    """)
    fun getDailySpendingBetween(start: Long, end: Long): Flow<List<DailySpendingRow>>

    // All purchases with details since a date (for spending report timeline)
    @Query("""
        SELECT ph.*, i.name as item_name, s.name as store_name
        FROM purchase_history ph
        JOIN items i ON ph.item_id = i.id
        LEFT JOIN stores s ON ph.store_id = s.id
        WHERE ph.purchase_date >= :since
        ORDER BY ph.purchase_date DESC, ph.created_at DESC
    """)
    fun getAllWithDetailsSince(since: Long): Flow<List<PurchaseWithItemDetails>>

    @Query("""
        SELECT id FROM purchase_history
        WHERE item_id = :itemId AND notes = 'From shopping list'
        ORDER BY created_at DESC LIMIT 1
    """)
    suspend fun getLatestShoppingListPurchaseId(itemId: Long): Long?

    @Query("""
        SELECT purchase_date FROM purchase_history
        WHERE item_id = :itemId
        ORDER BY purchase_date DESC LIMIT 1
    """)
    suspend fun getLatestPurchaseDate(itemId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseHistoryEntity): Long

    @Query("DELETE FROM purchase_history WHERE id = :id")
    suspend fun delete(id: Long)
}

data class SpendingByCategoryRow(
    val label: String,
    val amount: Double
)

data class SpendingByStoreRow(
    val label: String,
    val amount: Double
)

data class DailySpendingRow(
    val date: Long,
    val amount: Double
)

data class LatestPurchaseDefaults(
    val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Double?,
    @ColumnInfo(name = "total_price") val totalPrice: Double?,
    @ColumnInfo(name = "unit_id") val unitId: Long?
)

data class LatestItemPrice(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "unit_price") val unitPrice: Double?,
    @ColumnInfo(name = "total_price") val totalPrice: Double?,
    val quantity: Double
)

data class LatestItemPriceWithStore(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "unit_price") val unitPrice: Double?,
    @ColumnInfo(name = "total_price") val totalPrice: Double?,
    val quantity: Double,
    @ColumnInfo(name = "store_name") val storeName: String?,
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long // epoch day
)

data class FrequentPurchaseRow(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "purchase_count") val purchaseCount: Int,
    @ColumnInfo(name = "last_purchase_date") val lastPurchaseDate: Long, // epoch day
    @ColumnInfo(name = "unit_id") val unitId: Long?
)

data class VelocityPurchaseRow(
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long, // epoch day
    val quantity: Double,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "current_qty") val currentQty: Double,
    @ColumnInfo(name = "unit_id") val unitId: Long?
)

data class PurchaseWithItemDetails(
    val id: Long,
    @androidx.room.ColumnInfo(name = "item_id") val itemId: Long,
    @androidx.room.ColumnInfo(name = "store_id") val storeId: Long?,
    val quantity: Double,
    @androidx.room.ColumnInfo(name = "unit_price") val unitPrice: Double?,
    @androidx.room.ColumnInfo(name = "total_price") val totalPrice: Double?,
    @androidx.room.ColumnInfo(name = "purchase_date") val purchaseDate: java.time.LocalDate,
    @androidx.room.ColumnInfo(name = "expiry_date") val expiryDate: java.time.LocalDate?,
    val notes: String?,
    @androidx.room.ColumnInfo(name = "created_at") val createdAt: java.time.LocalDateTime,
    @androidx.room.ColumnInfo(name = "item_name") val itemName: String,
    @androidx.room.ColumnInfo(name = "store_name") val storeName: String?
)
