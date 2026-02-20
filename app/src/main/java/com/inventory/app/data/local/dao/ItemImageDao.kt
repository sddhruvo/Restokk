package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.inventory.app.data.local.entity.ItemImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemImageDao {
    @Query("SELECT * FROM item_images WHERE item_id = :itemId ORDER BY sort_order ASC")
    fun getByItemId(itemId: Long): Flow<List<ItemImageEntity>>

    @Query("SELECT * FROM item_images WHERE item_id = :itemId AND is_primary = 1 LIMIT 1")
    suspend fun getPrimaryImage(itemId: Long): ItemImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ItemImageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ItemImageEntity>): List<Long>

    @Query("DELETE FROM item_images WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE item_images SET is_primary = 0 WHERE item_id = :itemId")
    suspend fun clearPrimary(itemId: Long)

    @Query("UPDATE item_images SET is_primary = 1 WHERE id = :imageId")
    suspend fun setPrimary(imageId: Long)

    @Transaction
    suspend fun setPrimaryAtomic(itemId: Long, imageId: Long) {
        clearPrimary(itemId)
        setPrimary(imageId)
    }

    @Query("SELECT * FROM item_images WHERE id = :id")
    suspend fun getById(id: Long): ItemImageEntity?
}
