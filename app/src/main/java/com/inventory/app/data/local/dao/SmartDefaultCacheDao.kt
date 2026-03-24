package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.SmartDefaultCacheEntity

@Dao
interface SmartDefaultCacheDao {
    @Query("SELECT * FROM smart_defaults_cache WHERE normalized_name = :name")
    suspend fun lookup(name: String): SmartDefaultCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: SmartDefaultCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<SmartDefaultCacheEntity>)

    @Query("SELECT COUNT(*) FROM smart_defaults_cache")
    suspend fun count(): Int
}
