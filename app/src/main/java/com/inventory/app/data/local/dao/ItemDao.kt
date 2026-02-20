package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE is_active = 1 ORDER BY updated_at DESC")
    fun getAllActive(): Flow<List<ItemEntity>>

    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 ORDER BY updated_at DESC")
    fun getAllActiveWithDetails(): Flow<List<ItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id AND is_active = 1")
    fun getByIdWithDetails(id: Long): Flow<ItemWithDetails?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE barcode = :barcode AND is_active = 1 LIMIT 1")
    suspend fun findByBarcode(barcode: String): ItemEntity?

    // Dashboard queries (exclude paused items from all alert/score counts)
    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0")
    fun getTotalItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0 AND expiry_date IS NOT NULL AND expiry_date <= :futureDate AND expiry_date >= :today")
    fun getExpiringSoonCount(today: Long, futureDate: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0 AND expiry_date IS NOT NULL AND expiry_date < :today")
    fun getExpiredCount(today: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0
        AND (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END) > 0
        AND quantity < (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END)
    """)
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0 AND quantity <= 0")
    fun getOutOfStockCount(): Flow<Int>

    // Completeness queries for Home Score engagement (exclude paused)
    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0 AND category_id IS NOT NULL")
    fun getItemsWithCategoryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0 AND storage_location_id IS NOT NULL")
    fun getItemsWithLocationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND is_paused = 0 AND expiry_date IS NOT NULL")
    fun getItemsWithExpiryCount(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(
            COALESCE(
                i.quantity * (SELECT ph.unit_price FROM purchase_history ph WHERE ph.item_id = i.id ORDER BY ph.purchase_date DESC LIMIT 1),
                i.purchase_price,
                0
            )
        ), 0) FROM items i WHERE i.is_active = 1
    """)
    fun getTotalValue(): Flow<Double>

    // Search and filter
    @Query("""
        SELECT * FROM items WHERE is_active = 1
        AND (:search IS NULL OR name LIKE '%' || :search || '%' OR brand LIKE '%' || :search || '%' OR barcode LIKE '%' || :search || '%')
        AND (:categoryId IS NULL OR category_id = :categoryId)
        AND (:locationId IS NULL OR storage_location_id = :locationId)
        ORDER BY
            CASE WHEN :sortBy = 'name' THEN name END ASC,
            CASE WHEN :sortBy = 'expiry' AND expiry_date IS NULL THEN 1 ELSE 0 END,
            CASE WHEN :sortBy = 'expiry' THEN expiry_date END ASC,
            CASE WHEN :sortBy = 'quantity' THEN quantity END ASC,
            CASE WHEN :sortBy = 'updated' THEN updated_at END DESC,
            CASE WHEN :sortBy = 'created' THEN created_at ELSE updated_at END DESC
    """)
    fun getFiltered(
        search: String?,
        categoryId: Long?,
        locationId: Long?,
        sortBy: String
    ): Flow<List<ItemEntity>>

    // Items by category for reports
    @Query("""
        SELECT c.id as id, c.name as label, COUNT(i.id) as count
        FROM categories c
        LEFT JOIN items i ON c.id = i.category_id AND i.is_active = 1
        WHERE c.is_active = 1
        GROUP BY c.id
        HAVING count > 0
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getItemCountByCategory(limit: Int = 10): Flow<List<ChartDataRow>>

    // Items by location for reports
    @Query("""
        SELECT sl.id as id, sl.name as label, COUNT(i.id) as count
        FROM storage_locations sl
        LEFT JOIN items i ON sl.id = i.storage_location_id AND i.is_active = 1
        WHERE sl.is_active = 1
        GROUP BY sl.id
        HAVING count > 0
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getItemCountByLocation(limit: Int = 10): Flow<List<ChartDataRow>>

    // Value by category for inventory report
    // purchase_price = total price paid (not per-unit), so don't multiply by quantity
    // unit_price from purchase_history IS per-unit, so multiply by quantity
    @Query("""
        SELECT c.name as label,
            COALESCE(SUM(
                COALESCE(
                    i.quantity * (SELECT ph.unit_price FROM purchase_history ph WHERE ph.item_id = i.id ORDER BY ph.purchase_date DESC LIMIT 1),
                    i.purchase_price,
                    0
                )
            ), 0) as totalValue
        FROM categories c
        JOIN items i ON c.id = i.category_id AND i.is_active = 1
        WHERE c.is_active = 1
        GROUP BY c.id
        HAVING totalValue > 0
        ORDER BY totalValue DESC
        LIMIT :limit
    """)
    fun getValueByCategory(limit: Int = 10): Flow<List<CategoryValueRow>>

    // Top most valuable items for inventory report
    @Query("""
        SELECT i.id, i.name,
            COALESCE(
                i.quantity * (SELECT ph.unit_price FROM purchase_history ph WHERE ph.item_id = i.id ORDER BY ph.purchase_date DESC LIMIT 1),
                i.purchase_price,
                0
            ) as totalValue
        FROM items i
        WHERE i.is_active = 1
            AND (i.purchase_price IS NOT NULL
                OR EXISTS (SELECT 1 FROM purchase_history ph WHERE ph.item_id = i.id))
        ORDER BY totalValue DESC
        LIMIT :limit
    """)
    fun getTopValueItems(limit: Int = 5): Flow<List<TopValueItemRow>>

    // Expiring items (exclude paused)
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND is_paused = 0 AND expiry_date IS NOT NULL AND expiry_date <= :futureDate ORDER BY expiry_date ASC LIMIT :limit")
    fun getExpiringSoon(futureDate: Long, limit: Int = 5): Flow<List<ItemWithDetails>>

    // Low stock items (exclude paused)
    @Transaction
    @Query("""
        SELECT * FROM items WHERE is_active = 1 AND is_paused = 0
        AND (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END) > 0
        AND quantity < (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END)
        ORDER BY (quantity / (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END)) ASC
        LIMIT :limit
    """)
    fun getLowStockItems(limit: Int = 5): Flow<List<ItemWithDetails>>

    // Items at location
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND storage_location_id = :locationId ORDER BY name ASC")
    fun getByLocation(locationId: Long): Flow<List<ItemWithDetails>>

    // Items by category
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND category_id = :categoryId ORDER BY name ASC")
    fun getByCategory(categoryId: Long): Flow<List<ItemWithDetails>>

    // Search
    @Query("""
        SELECT * FROM items WHERE is_active = 1
        AND (name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')
        LIMIT 10
    """)
    suspend fun search(query: String): List<ItemEntity>

    // Name suggestions for autocomplete
    @Query("SELECT DISTINCT name FROM items WHERE is_active = 1 AND name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun suggestNames(query: String, limit: Int = 5): List<String>

    // Find item by name (case-insensitive), also matches plural/singular variants
    @Query("""
        SELECT * FROM items WHERE is_active = 1
        AND (LOWER(name) = LOWER(:name)
          OR LOWER(name) || 's' = LOWER(:name)
          OR LOWER(name) || 'es' = LOWER(:name)
          OR LOWER(:name) || 's' = LOWER(name)
          OR LOWER(:name) || 'es' = LOWER(name))
        ORDER BY CASE WHEN LOWER(name) = LOWER(:name) THEN 0 ELSE 1 END
        LIMIT 1
    """)
    suspend fun findByName(name: String): ItemEntity?

    // Search items by keyword for receipt matching (case-insensitive contains)
    @Query("""
        SELECT id, name, quantity, unit_id FROM items
        WHERE is_active = 1
        AND LOWER(name) LIKE '%' || LOWER(:keyword) || '%'
        ORDER BY CASE WHEN LOWER(name) = LOWER(:keyword) THEN 0 ELSE 1 END, name
        LIMIT 5
    """)
    suspend fun searchByKeyword(keyword: String): List<InventoryMatchCandidate>

    // Items with a purchase price but no purchase_history record (for one-time backfill)
    @Query("""
        SELECT * FROM items
        WHERE is_active = 1
        AND purchase_price IS NOT NULL
        AND id NOT IN (SELECT DISTINCT item_id FROM purchase_history)
    """)
    suspend fun getItemsMissingPurchaseHistory(): List<ItemEntity>

    // Low stock items for shopping list generation (exclude paused)
    @Query("""
        SELECT * FROM items WHERE is_active = 1 AND is_paused = 0
        AND (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END) > 0
        AND quantity < (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END)
    """)
    suspend fun getLowStockItemsList(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET is_active = 0, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("UPDATE items SET is_active = 1, updated_at = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long)

    @Query("""
        UPDATE items SET
            quantity = MAX(0, quantity + :delta),
            smart_min_quantity = CASE
                WHEN :delta > 0 THEN MAX(smart_min_quantity, MAX(0, quantity + :delta))
                ELSE smart_min_quantity
            END,
            updated_at = :now
        WHERE id = :id
    """)
    suspend fun adjustQuantity(id: Long, delta: Double, now: Long)

    @Query("UPDATE items SET purchase_date = :date, updated_at = :now WHERE id = :id")
    suspend fun updatePurchaseDate(id: Long, date: Long?, now: Long)

    @Query("UPDATE items SET expiry_date = :date, updated_at = :now WHERE id = :id AND expiry_date IS NULL")
    suspend fun updateExpiryDateIfNull(id: Long, date: Long?, now: Long)

    @Query("UPDATE items SET barcode = :barcode, updated_at = :now WHERE id = :id AND (barcode IS NULL OR barcode = '')")
    suspend fun updateBarcodeIfEmpty(id: Long, barcode: String, now: Long)

    @Query("SELECT purchase_date FROM items WHERE id = :id")
    suspend fun getPurchaseDate(id: Long): Long?

    @Query("UPDATE items SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END, updated_at = :now WHERE id = :id")
    suspend fun toggleFavorite(id: Long, now: Long)

    @Query("UPDATE items SET is_paused = 1, updated_at = :now WHERE id = :id")
    suspend fun pauseItem(id: Long, now: Long)

    @Query("UPDATE items SET is_paused = 0, updated_at = :now WHERE id = :id")
    suspend fun unpauseItem(id: Long, now: Long)

    // Favorite items filter
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND is_favorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<ItemWithDetails>>

    // Expired items (exclude paused)
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND is_paused = 0 AND expiry_date IS NOT NULL AND expiry_date < :today ORDER BY expiry_date ASC")
    fun getExpiredItems(today: Long): Flow<List<ItemWithDetails>>

    // All expiring (for reports, exclude paused)
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND is_paused = 0 AND expiry_date IS NOT NULL AND expiry_date <= :futureDate AND expiry_date >= :today ORDER BY expiry_date ASC")
    fun getExpiringItemsReport(today: Long, futureDate: Long): Flow<List<ItemWithDetails>>

    // All low stock (for reports, exclude paused) — excludes out-of-stock items (quantity <= 0) to avoid overlap
    @Transaction
    @Query("""
        SELECT * FROM items WHERE is_active = 1 AND is_paused = 0
        AND quantity > 0
        AND (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END) > 0
        AND quantity < (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END)
        ORDER BY (quantity / (CASE WHEN min_quantity > 0 THEN min_quantity ELSE smart_min_quantity END)) ASC
    """)
    fun getLowStockItemsReport(): Flow<List<ItemWithDetails>>

    // All out of stock (for reports, exclude paused)
    @Transaction
    @Query("SELECT * FROM items WHERE is_active = 1 AND is_paused = 0 AND quantity <= 0 ORDER BY name ASC")
    fun getOutOfStockItemsReport(): Flow<List<ItemWithDetails>>

    // Count items added recently
    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1 AND created_at >= :since")
    fun getRecentItemCount(since: Long): Flow<Int>

    // All active item names + IDs (lightweight, for AI matching context)
    @Query("SELECT id, name FROM items WHERE is_active = 1 ORDER BY name")
    suspend fun getAllActiveNamesAndIds(): List<InventoryNameId>

    // In-stock items for waste-avoidance check
    @Query("""
        SELECT id, name, quantity, storage_location_id, expiry_date
        FROM items WHERE is_active = 1 AND quantity > 0
    """)
    suspend fun getInStockItems(): List<WasteCheckItemRow>
}

data class ChartDataRow(
    val id: Long = 0,
    val label: String,
    val count: Int
)

data class WasteCheckItemRow(
    val id: Long,
    val name: String,
    val quantity: Double,
    @androidx.room.ColumnInfo(name = "storage_location_id") val storageLocationId: Long?,
    @androidx.room.ColumnInfo(name = "expiry_date") val expiryDate: Long?
)

data class InventoryMatchCandidate(
    val id: Long,
    val name: String,
    val quantity: Double,
    @androidx.room.ColumnInfo(name = "unit_id") val unitId: Long?
)

data class InventoryNameId(
    val id: Long,
    val name: String
)

data class CategoryValueRow(
    val label: String,
    val totalValue: Double
)

data class TopValueItemRow(
    val id: Long,
    val name: String,
    val totalValue: Double
)
