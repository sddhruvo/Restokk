package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.PantryHealthLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryHealthLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: PantryHealthLogEntity)

    @Query("SELECT * FROM pantry_health_log WHERE date >= :sinceDate ORDER BY date ASC")
    fun getLogsSince(sinceDate: Long): Flow<List<PantryHealthLogEntity>>

    @Query("SELECT * FROM pantry_health_log ORDER BY date DESC LIMIT :days")
    fun getRecentLogs(days: Int): Flow<List<PantryHealthLogEntity>>

    @Query("SELECT * FROM pantry_health_log ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): PantryHealthLogEntity?
}
