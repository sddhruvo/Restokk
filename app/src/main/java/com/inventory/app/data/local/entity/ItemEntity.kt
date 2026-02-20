package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategory_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StorageLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["storage_location_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unit_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("subcategory_id"),
        Index("storage_location_id"),
        Index("unit_id"),
        Index("barcode"),
        Index("expiry_date"),
        Index("is_active"),
        Index("is_paused")
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val barcode: String? = null,
    val brand: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "subcategory_id") val subcategoryId: Long? = null,
    @ColumnInfo(name = "storage_location_id") val storageLocationId: Long? = null,
    val quantity: Double = 0.0,
    @ColumnInfo(name = "min_quantity") val minQuantity: Double = 0.0,
    @ColumnInfo(name = "smart_min_quantity", defaultValue = "0.0") val smartMinQuantity: Double = 0.0,
    @ColumnInfo(name = "max_quantity") val maxQuantity: Double? = null,
    @ColumnInfo(name = "unit_id") val unitId: Long? = null,
    @ColumnInfo(name = "expiry_date") val expiryDate: LocalDate? = null,
    @ColumnInfo(name = "expiry_warning_days") val expiryWarningDays: Int = 7,
    @ColumnInfo(name = "opened_date") val openedDate: LocalDate? = null,
    @ColumnInfo(name = "days_after_opening") val daysAfterOpening: Int? = null,
    @ColumnInfo(name = "purchase_date") val purchaseDate: LocalDate? = null,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_paused") val isPaused: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
