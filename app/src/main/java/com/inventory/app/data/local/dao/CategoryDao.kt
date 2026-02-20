package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.relations.CategoryWithSubcategories
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun getAllActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CategoryEntity?>

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getWithSubcategories(id: Long): Flow<CategoryWithSubcategories?>

    @Query("""
        SELECT c.*, COUNT(i.id) as itemCount
        FROM categories c
        LEFT JOIN items i ON c.id = i.category_id AND i.is_active = 1
        WHERE c.is_active = 1
        GROUP BY c.id
        ORDER BY c.sort_order ASC, c.name ASC
    """)
    fun getAllWithItemCount(): Flow<List<CategoryWithItemCountRow>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET is_active = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE categories SET is_active = 1 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE is_active = 1")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT * FROM categories WHERE name = :name AND is_active = 1 LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) AND is_active = 1 LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): CategoryEntity?

    @Query("UPDATE categories SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT * FROM categories WHERE is_active = 1 AND name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun search(query: String): List<CategoryEntity>

    @Query("UPDATE categories SET icon = :icon WHERE LOWER(name) = LOWER(:name)")
    suspend fun updateIconByName(name: String, icon: String)
}

data class CategoryWithItemCountRow(
    val id: Long,
    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val sort_order: Int,
    val is_active: Boolean,
    val created_at: Long?,
    val updated_at: Long?,
    val itemCount: Int
)
