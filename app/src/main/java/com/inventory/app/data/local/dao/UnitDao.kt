package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units WHERE is_active = 1 ORDER BY unit_type ASC, name ASC")
    fun getAllActive(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: Long): UnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: UnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>): List<Long>

    @Query("SELECT * FROM units WHERE name = :name AND is_active = 1 LIMIT 1")
    suspend fun findByName(name: String): UnitEntity?

    @Query("SELECT * FROM units WHERE abbreviation = :abbr AND is_active = 1 LIMIT 1")
    suspend fun findByAbbreviation(abbr: String): UnitEntity?
}
