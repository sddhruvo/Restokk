package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "purchase_history",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("item_id"), Index("store_id"), Index("purchase_date")]
)
data class PurchaseHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "store_id") val storeId: Long? = null,
    val quantity: Double = 0.0,
    @ColumnInfo(name = "unit_price") val unitPrice: Double? = null,
    @ColumnInfo(name = "total_price") val totalPrice: Double? = null,
    @ColumnInfo(name = "purchase_date") val purchaseDate: LocalDate = LocalDate.now(),
    @ColumnInfo(name = "expiry_date") val expiryDate: LocalDate? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now()
)
