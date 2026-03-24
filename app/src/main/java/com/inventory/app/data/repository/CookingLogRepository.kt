package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.CookingLogDao
import com.inventory.app.data.local.dao.MostRecentCookResult
import com.inventory.app.data.local.entity.CookingLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookingLogRepository @Inject constructor(
    private val cookingLogDao: CookingLogDao
) {
    suspend fun insert(log: CookingLogEntity): Long = cookingLogDao.insert(log)

    fun getByRecipeId(recipeId: Long): Flow<List<CookingLogEntity>> =
        cookingLogDao.getByRecipeId(recipeId)

    fun getCookCountForRecipe(recipeId: Long): Flow<Int> =
        cookingLogDao.getCookCountForRecipe(recipeId)

    fun getRecent(limit: Int = 10): Flow<List<CookingLogEntity>> =
        cookingLogDao.getRecent(limit)

    fun getMostRecent(): Flow<CookingLogEntity?> =
        cookingLogDao.getMostRecent()

    fun getMostRecentWithName(): Flow<MostRecentCookResult?> =
        cookingLogDao.getMostRecentWithName()

    suspend fun delete(id: Long) = cookingLogDao.delete(id)

    fun getCookCountMap(): Flow<Map<Long, Int>> =
        cookingLogDao.getCookCountList().map { list -> list.associate { it.recipeId to it.count } }
}
