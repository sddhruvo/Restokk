package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.LocationWithItemCountRow
import com.inventory.app.data.local.dao.StorageLocationDao
import com.inventory.app.data.local.entity.StorageLocationEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageLocationRepository @Inject constructor(
    private val locationDao: StorageLocationDao
) {
    fun getAllActive(): Flow<List<StorageLocationEntity>> = locationDao.getAllActive()

    fun getAllWithItemCount(): Flow<List<LocationWithItemCountRow>> = locationDao.getAllWithItemCount()

    fun getByIdFlow(id: Long): Flow<StorageLocationEntity?> = locationDao.getByIdFlow(id)

    suspend fun getById(id: Long): StorageLocationEntity? = locationDao.getById(id)

    suspend fun insert(location: StorageLocationEntity): Long = locationDao.insert(location)

    suspend fun update(location: StorageLocationEntity) =
        locationDao.update(location.copy(updatedAt = LocalDateTime.now()))

    suspend fun delete(id: Long) = locationDao.softDelete(id)

    suspend fun restore(id: Long) = locationDao.restore(id)

    suspend fun findByName(name: String): StorageLocationEntity? = locationDao.findByName(name)

    suspend fun search(query: String): List<StorageLocationEntity> = locationDao.search(query)

    suspend fun updateSortOrders(idToOrder: List<Pair<Long, Int>>) {
        idToOrder.forEach { (id, order) -> locationDao.updateSortOrder(id, order) }
    }
}
