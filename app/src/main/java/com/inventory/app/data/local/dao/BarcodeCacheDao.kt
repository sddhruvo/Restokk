package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.BarcodeCacheEntity

@Dao
interface BarcodeCacheDao {
    @Query("SELECT * FROM barcode_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): BarcodeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: BarcodeCacheEntity): Long

    @Query("DELETE FROM barcode_cache WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM barcode_cache WHERE created_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
