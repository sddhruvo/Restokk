package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "barcode_cache",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class BarcodeCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    @ColumnInfo(name = "product_name") val productName: String? = null,
    val brand: String? = null,
    @ColumnInfo(name = "quantity_info") val quantityInfo: String? = null,
    val categories: String? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    val ingredients: String? = null,
    @ColumnInfo(name = "nutrition_grade") val nutritionGrade: String? = null,
    @ColumnInfo(name = "raw_data") val rawData: String? = null,
    val found: Boolean = false,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
