package com.inventory.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_notifications",
    indices = [
        Index("type"),
        Index("read_at"),
        Index("dismissed_at"),
        Index("expires_at"),
        Index(value = ["priority", "created_at"])
    ]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "deep_link_route") val deepLinkRoute: String? = null,
    @ColumnInfo(name = "cta_text") val ctaText: String? = null,
    @ColumnInfo(name = "cta_route") val ctaRoute: String? = null,
    val priority: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "read_at") val readAt: Long? = null,
    @ColumnInfo(name = "dismissed_at") val dismissedAt: Long? = null,
    @ColumnInfo(name = "expires_at") val expiresAt: Long? = null
)
