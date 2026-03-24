package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_defaults_cache")
data class SmartDefaultCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    val category: String,
    val subcategory: String? = null,
    val unit: String? = null,
    val location: String? = null,
    @ColumnInfo(name = "shelf_life_days")
    val shelfLifeDays: Int? = null,
    val version: Int = 1,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis(),
    val source: String = "seed" // "seed" | "server"
) {
    fun isExpired(): Boolean {
        val ttlMs = 30L * 24 * 60 * 60 * 1000 // 30 days
        return System.currentTimeMillis() - fetchedAt > ttlMs
    }
}
