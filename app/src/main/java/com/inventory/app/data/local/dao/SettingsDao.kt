package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    suspend fun get(key: String): SettingsEntity?

    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    fun getFlow(key: String): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings")
    fun getAll(): Flow<List<SettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: SettingsEntity): Long

    @Query("UPDATE settings SET value = :value, updated_at = :now WHERE key = :key")
    suspend fun updateValue(key: String, value: String, now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<SettingsEntity>)
}
