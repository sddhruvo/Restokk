package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.UnitDao
import com.inventory.app.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnitRepository @Inject constructor(
    private val unitDao: UnitDao
) {
    fun getAllActive(): Flow<List<UnitEntity>> = unitDao.getAllActive()

    suspend fun getById(id: Long): UnitEntity? = unitDao.getById(id)

    suspend fun findByName(name: String): UnitEntity? = unitDao.findByName(name)

    suspend fun findByAbbreviation(abbr: String): UnitEntity? = unitDao.findByAbbreviation(abbr)
}
