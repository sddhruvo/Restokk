package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "pantry_health_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class PantryHealthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val date: LocalDate,
    @ColumnInfo(name = "expired_count") val expiredCount: Int = 0,
    @ColumnInfo(name = "expiring_soon_count") val expiringSoonCount: Int = 0,
    @ColumnInfo(name = "low_stock_count") val lowStockCount: Int = 0,
    @ColumnInfo(name = "out_of_stock_count") val outOfStockCount: Int = 0,
    @ColumnInfo(name = "shopping_completion_pct") val shoppingCompletionPct: Float = 0f,
    @ColumnInfo(name = "total_items") val totalItems: Int = 0,
    @ColumnInfo(name = "engagement_score") val engagementScore: Int = 0,
    @ColumnInfo(name = "condition_score") val conditionScore: Int = 0
)
