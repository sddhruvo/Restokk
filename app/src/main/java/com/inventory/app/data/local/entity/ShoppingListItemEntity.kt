package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "shopping_list",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unit_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("item_id"), Index("unit_id"), Index("is_purchased")]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_id") val itemId: Long? = null,
    @ColumnInfo(name = "custom_name") val customName: String? = null,
    val quantity: Double = 1.0,
    @ColumnInfo(name = "unit_id") val unitId: Long? = null,
    @ColumnInfo(name = "is_purchased") val isPurchased: Boolean = false,
    @ColumnInfo(name = "purchased_at") val purchasedAt: LocalDateTime? = null,
    val priority: Int = 0,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
