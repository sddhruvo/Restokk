package com.inventory.app.data.repository

import com.inventory.app.data.local.dao.CategoryDao
import com.inventory.app.data.local.dao.CategoryWithItemCountRow
import com.inventory.app.data.local.dao.SubcategoryDao
import com.inventory.app.data.local.entity.CategoryEntity
import com.inventory.app.data.local.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao
) {
    fun getAllActive(): Flow<List<CategoryEntity>> = categoryDao.getAllActive()

    fun getAllWithItemCount(): Flow<List<CategoryWithItemCountRow>> = categoryDao.getAllWithItemCount()

    fun getByIdFlow(id: Long): Flow<CategoryEntity?> = categoryDao.getByIdFlow(id)

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    fun getSubcategories(categoryId: Long): Flow<List<SubcategoryEntity>> =
        subcategoryDao.getByCategoryId(categoryId)

    fun getAllSubcategoriesActive(): Flow<List<SubcategoryEntity>> = subcategoryDao.getAllActive()

    suspend fun getSubcategoryById(id: Long): SubcategoryEntity? = subcategoryDao.getById(id)

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.update(category.copy(updatedAt = LocalDateTime.now()))

    suspend fun deleteCategory(id: Long) = categoryDao.softDelete(id)

    suspend fun restoreCategory(id: Long) = categoryDao.restore(id)

    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long =
        subcategoryDao.insert(subcategory)

    suspend fun updateSubcategory(subcategory: SubcategoryEntity) =
        subcategoryDao.update(subcategory.copy(updatedAt = LocalDateTime.now()))

    suspend fun deleteSubcategory(id: Long) = subcategoryDao.softDelete(id)

    suspend fun findCategoryByName(name: String): CategoryEntity? = categoryDao.findByName(name)

    suspend fun findCategoryByNameIgnoreCase(name: String): CategoryEntity? = categoryDao.findByNameIgnoreCase(name)

    suspend fun findSubcategoryByNameAndCategory(name: String, categoryId: Long): SubcategoryEntity? =
        subcategoryDao.findByNameAndCategory(name, categoryId)

    suspend fun search(query: String): List<CategoryEntity> = categoryDao.search(query)

    suspend fun updateSortOrders(idToOrder: List<Pair<Long, Int>>) {
        idToOrder.forEach { (id, order) -> categoryDao.updateSortOrder(id, order) }
    }

    /** One-time backfill: set icon for default categories that have null/default icon. */
    suspend fun backfillIcons() {
        val nameToIcon = mapOf(
            "Dairy & Eggs" to "dairy",
            "Meat & Poultry" to "meat",
            "Seafood" to "meat",
            "Fruits" to "produce",
            "Vegetables" to "produce",
            "Bread & Bakery" to "bakery",
            "Grains & Pasta" to "grains",
            "Canned Goods" to "canned",
            "Condiments & Sauces" to "condiments",
            "Spices & Seasonings" to "spices",
            "Snacks" to "snacks",
            "Beverages" to "beverages",
            "Frozen Foods" to "frozen",
            "Baking Supplies" to "bakery",
            "Oils & Vinegars" to "water",
            "International Foods" to "category",
            "Baby Food" to "category",
            "Pet Food" to "pets",
            "Other" to "category",
        )
        nameToIcon.forEach { (name, icon) -> categoryDao.updateIconByName(name, icon) }
    }
}
