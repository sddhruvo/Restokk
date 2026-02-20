package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.SavedRecipeDao
import com.inventory.app.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedRecipeRepository @Inject constructor(
    private val savedRecipeDao: SavedRecipeDao
) {
    fun getAllActive(): Flow<List<SavedRecipeEntity>> = savedRecipeDao.getAllActive()

    fun getFavorites(): Flow<List<SavedRecipeEntity>> = savedRecipeDao.getFavorites()

    fun search(query: String): Flow<List<SavedRecipeEntity>> = savedRecipeDao.search(query)

    suspend fun getById(id: Long): SavedRecipeEntity? = savedRecipeDao.getById(id)

    suspend fun findByName(name: String): SavedRecipeEntity? = savedRecipeDao.findByName(name)

    suspend fun insert(recipe: SavedRecipeEntity): Long = savedRecipeDao.insert(recipe)

    suspend fun toggleFavorite(id: Long) = savedRecipeDao.toggleFavorite(id)

    suspend fun softDelete(id: Long) = savedRecipeDao.softDelete(id)

    suspend fun restore(id: Long) = savedRecipeDao.restore(id)

    suspend fun updateNotes(id: Long, notes: String?) = savedRecipeDao.updateNotes(id, notes)

    suspend fun updateRating(id: Long, rating: Int) = savedRecipeDao.updateRating(id, rating)

    fun getCount(): Flow<Int> = savedRecipeDao.getCount()

    suspend fun deleteByName(name: String) = savedRecipeDao.deleteByName(name)
}
