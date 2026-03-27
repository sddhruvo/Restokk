package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.AppNotificationDao
import com.inventory.app.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationRepository @Inject constructor(
    private val dao: AppNotificationDao
) {
    fun getUnreadCountFlow(): Flow<Int> = dao.getUnreadCountFlow()

    fun getAllActive(): Flow<List<AppNotificationEntity>> = dao.getAllActive()

    suspend fun markRead(id: Long) = dao.markRead(id)

    suspend fun markAllRead() = dao.markAllRead()

    suspend fun markDismissed(id: Long) = dao.markDismissed(id)

    suspend fun deleteExpired() = dao.deleteExpired()

    suspend fun insert(notification: AppNotificationEntity): Long = dao.insert(notification)

    suspend fun getUnreadCountOnce(): Int = dao.getUnreadCountOnce()

    suspend fun getCountByTypeSince(type: String, since: Long): Int =
        dao.getCountByTypeSince(type, since)

    suspend fun getDismissedCountByTypeSince(type: String, since: Long): Int =
        dao.getDismissedCountByTypeSince(type, since)

    suspend fun enforceUnreadCap(max: Int = 10) {
        while (dao.getUnreadCountOnce() > max) {
            dao.autoReadOldest()
        }
    }
}
