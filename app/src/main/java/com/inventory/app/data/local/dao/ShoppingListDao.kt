package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.local.entity.relations.ShoppingListItemWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Transaction
    @Query("SELECT * FROM shopping_list WHERE is_purchased = 0 ORDER BY priority DESC, created_at ASC")
    fun getActiveItems(): Flow<List<ShoppingListItemWithDetails>>

    // LIMIT 50 is intentional — purchased items are historical and shown in a collapsed section.
    // Keeps the query fast and memory low. Cleared periodically via clearPurchasedBefore().
    @Transaction
    @Query("SELECT * FROM shopping_list WHERE is_purchased = 1 ORDER BY purchased_at DESC LIMIT 50")
    fun getPurchasedItems(): Flow<List<ShoppingListItemWithDetails>>

    @Query("SELECT COUNT(*) FROM shopping_list WHERE is_purchased = 0")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM shopping_list WHERE is_purchased = 1")
    fun getPurchasedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingListItemEntity): Long

    @Query("UPDATE shopping_list SET is_purchased = NOT is_purchased, purchased_at = CASE WHEN is_purchased = 0 THEN :now ELSE NULL END, updated_at = :now WHERE id = :id")
    suspend fun togglePurchased(id: Long, now: Long)

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_list WHERE is_purchased = 1")
    suspend fun clearPurchased()

    @Query("SELECT * FROM shopping_list WHERE item_id = :itemId AND is_purchased = 0 LIMIT 1")
    suspend fun findActiveByItemId(itemId: Long): ShoppingListItemEntity?

    @Query("SELECT * FROM shopping_list WHERE custom_name = :name AND is_purchased = 0 LIMIT 1")
    suspend fun findActiveByCustomName(name: String): ShoppingListItemEntity?

    @Query("SELECT * FROM shopping_list WHERE id = :id")
    suspend fun getById(id: Long): ShoppingListItemEntity?

    @Query("SELECT s.id, COALESCE(i.name, s.custom_name) as name FROM shopping_list s LEFT JOIN items i ON s.item_id = i.id WHERE s.is_purchased = 0")
    suspend fun getActiveItemNames(): List<ShoppingListNameProjection>

    @Query("UPDATE shopping_list SET is_purchased = 1, purchased_at = :now, updated_at = :now WHERE id = :id")
    suspend fun markAsPurchasedOnly(id: Long, now: Long)

    @Query("SELECT item_id FROM shopping_list WHERE is_purchased = 0 AND item_id IS NOT NULL")
    suspend fun getActiveItemIds(): List<Long>

    @Query("UPDATE shopping_list SET quantity = :quantity, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double, updatedAt: Long)

    @Query("DELETE FROM shopping_list WHERE is_purchased = 1 AND purchased_at < :before")
    suspend fun clearPurchasedBefore(before: Long)
}

data class ShoppingListNameProjection(
    val id: Long,
    val name: String
)
