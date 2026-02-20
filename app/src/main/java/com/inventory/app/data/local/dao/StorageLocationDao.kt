package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inventory.app.data.local.entity.StorageLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageLocationDao {
    @Query("SELECT * FROM storage_locations WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun getAllActive(): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations ORDER BY sort_order ASC, name ASC")
    fun getAll(): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations WHERE id = :id")
    suspend fun getById(id: Long): StorageLocationEntity?

    @Query("SELECT * FROM storage_locations WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<StorageLocationEntity?>

    @Query("""
        SELECT sl.*, COUNT(i.id) as itemCount
        FROM storage_locations sl
        LEFT JOIN items i ON sl.id = i.storage_location_id AND i.is_active = 1
        WHERE sl.is_active = 1
        GROUP BY sl.id
        ORDER BY sl.sort_order ASC, sl.name ASC
    """)
    fun getAllWithItemCount(): Flow<List<LocationWithItemCountRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: StorageLocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<StorageLocationEntity>): List<Long>

    @Update
    suspend fun update(location: StorageLocationEntity)

    @Query("UPDATE storage_locations SET is_active = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM storage_locations WHERE name = :name AND is_active = 1 LIMIT 1")
    suspend fun findByName(name: String): StorageLocationEntity?

    @Query("UPDATE storage_locations SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT * FROM storage_locations WHERE is_active = 1 AND name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun search(query: String): List<StorageLocationEntity>
}

data class LocationWithItemCountRow(
    val id: Long,
    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val temperature_zone: String?,
    val sort_order: Int,
    val is_active: Boolean,
    val created_at: Long?,
    val updated_at: Long?,
    val itemCount: Int
)
