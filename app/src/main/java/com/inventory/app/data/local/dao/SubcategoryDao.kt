package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inventory.app.data.local.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubcategoryDao {
    @Query("SELECT * FROM subcategories WHERE category_id = :categoryId AND is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun getByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategories WHERE id = :id")
    suspend fun getById(id: Long): SubcategoryEntity?

    @Query("SELECT * FROM subcategories WHERE is_active = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<SubcategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(subcategory: SubcategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(subcategories: List<SubcategoryEntity>): List<Long>

    @Update
    suspend fun update(subcategory: SubcategoryEntity)

    @Query("UPDATE subcategories SET is_active = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM subcategories WHERE name = :name AND category_id = :categoryId AND is_active = 1 LIMIT 1")
    suspend fun findByNameAndCategory(name: String, categoryId: Long): SubcategoryEntity?
}
