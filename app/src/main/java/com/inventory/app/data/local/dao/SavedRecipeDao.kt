package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecipeDao {

    @Query("SELECT * FROM saved_recipes WHERE is_active = 1 ORDER BY created_at DESC")
    fun getAllActive(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes WHERE is_active = 1 AND is_favorite = 1 ORDER BY created_at DESC")
    fun getFavorites(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes WHERE is_active = 1 AND (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(cuisine_origin) LIKE '%' || LOWER(:query) || '%') ORDER BY created_at DESC")
    fun search(query: String): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes WHERE id = :id")
    suspend fun getById(id: Long): SavedRecipeEntity?

    @Query("SELECT * FROM saved_recipes WHERE name = :name AND is_active = 1 LIMIT 1")
    suspend fun findByName(name: String): SavedRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: SavedRecipeEntity): Long

    @Query("UPDATE saved_recipes SET is_favorite = NOT is_favorite, updated_at = :now WHERE id = :id")
    suspend fun toggleFavorite(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE saved_recipes SET is_active = 0, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE saved_recipes SET personal_notes = :notes, updated_at = :now WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE saved_recipes SET rating = :rating, updated_at = :now WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Int, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM saved_recipes WHERE is_active = 1")
    fun getCount(): Flow<Int>

    @Query("UPDATE saved_recipes SET is_active = 1, updated_at = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE saved_recipes SET is_active = 0, updated_at = :now WHERE name = :name AND is_active = 1")
    suspend fun deleteByName(name: String, now: Long = System.currentTimeMillis())
}
