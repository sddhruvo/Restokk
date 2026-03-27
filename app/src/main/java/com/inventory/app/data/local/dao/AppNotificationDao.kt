package com.inventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventory.app.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {

    @Query("""
        SELECT COUNT(*) FROM app_notifications
        WHERE read_at IS NULL AND dismissed_at IS NULL
        AND (expires_at IS NULL OR expires_at > :now)
    """)
    fun getUnreadCountFlow(now: Long = System.currentTimeMillis()): Flow<Int>

    @Query("""
        SELECT * FROM app_notifications
        WHERE dismissed_at IS NULL
        AND (expires_at IS NULL OR expires_at > :now)
        ORDER BY priority ASC, created_at DESC
    """)
    fun getAllActive(now: Long = System.currentTimeMillis()): Flow<List<AppNotificationEntity>>

    @Query("UPDATE app_notifications SET read_at = :timestamp WHERE id = :id")
    suspend fun markRead(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_notifications SET read_at = :timestamp WHERE read_at IS NULL")
    suspend fun markAllRead(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_notifications SET dismissed_at = :timestamp WHERE id = :id")
    suspend fun markDismissed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_notifications WHERE expires_at IS NOT NULL AND expires_at <= :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotificationEntity): Long

    @Query("""
        SELECT COUNT(*) FROM app_notifications
        WHERE read_at IS NULL AND dismissed_at IS NULL
        AND (expires_at IS NULL OR expires_at > :now)
    """)
    suspend fun getUnreadCountOnce(now: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM app_notifications WHERE type = :type AND created_at >= :since")
    suspend fun getCountByTypeSince(type: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM app_notifications WHERE type = :type AND dismissed_at IS NOT NULL AND dismissed_at >= :since")
    suspend fun getDismissedCountByTypeSince(type: String, since: Long): Int

    @Query("""
        UPDATE app_notifications SET read_at = :timestamp
        WHERE id = (
            SELECT id FROM app_notifications
            WHERE read_at IS NULL AND dismissed_at IS NULL
            ORDER BY created_at ASC LIMIT 1
        )
    """)
    suspend fun autoReadOldest(timestamp: Long = System.currentTimeMillis())
}
