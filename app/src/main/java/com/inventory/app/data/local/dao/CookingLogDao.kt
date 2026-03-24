package com.inventory.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.CookingLogEntity
import kotlinx.coroutines.flow.Flow

/** Lightweight result type for aggregated cook counts per recipe. */
data class RecipeCookCount(
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val count: Int
)

/** Result type for the most recent cook log with its recipe name. */
data class MostRecentCookResult(
    @ColumnInfo(name = "recipe_name") val recipeName: String,
    @ColumnInfo(name = "cooked_date") val cookedDate: Long,
    @ColumnInfo(name = "recipe_id") val recipeId: Long
)

@Dao
interface CookingLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CookingLogEntity): Long

    @Query("SELECT * FROM cooking_log WHERE recipe_id = :recipeId ORDER BY cooked_date DESC")
    fun getByRecipeId(recipeId: Long): Flow<List<CookingLogEntity>>

    @Query("SELECT COUNT(*) FROM cooking_log WHERE recipe_id = :recipeId")
    fun getCookCountForRecipe(recipeId: Long): Flow<Int>

    @Query("SELECT * FROM cooking_log ORDER BY cooked_date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<CookingLogEntity>>

    @Query("SELECT * FROM cooking_log ORDER BY cooked_date DESC LIMIT 1")
    fun getMostRecent(): Flow<CookingLogEntity?>

    /** Returns the most recent cook log joined with its recipe name. Null if no logs exist. */
    @Query("SELECT l.recipe_id, r.name as recipe_name, l.cooked_date FROM cooking_log l INNER JOIN saved_recipes r ON l.recipe_id = r.id ORDER BY l.cooked_date DESC LIMIT 1")
    fun getMostRecentWithName(): Flow<MostRecentCookResult?>

    @Query("DELETE FROM cooking_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT recipe_id, COUNT(*) as count FROM cooking_log GROUP BY recipe_id")
    fun getCookCountList(): Flow<List<RecipeCookCount>>
}
