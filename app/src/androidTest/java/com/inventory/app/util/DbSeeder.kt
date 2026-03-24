package com.inventory.app.util

import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.data.local.entity.SettingsEntity
import java.time.LocalDate

/**
 * Seeds the in-memory test database with known test data.
 * Call from @Before in tests that need pre-populated state.
 */
class DbSeeder(private val db: InventoryDatabase) {

    /**
     * Seeds 5 items covering different alert states:
     * - 2 normal items
     * - 1 expiring soon (2 days)
     * - 1 low stock
     * - 1 expired
     */
    suspend fun seedBasicInventory() {
        val itemDao = db.itemDao()
        itemDao.insert(TestDataFactory.item(name = "Rice", quantity = 3.0, expiryDate = LocalDate.now().plusDays(90)))
        itemDao.insert(TestDataFactory.item(name = "Pasta", quantity = 2.0, expiryDate = LocalDate.now().plusDays(180)))
        itemDao.insert(TestDataFactory.expiringSoonItem(name = "Milk", daysUntilExpiry = 2))
        itemDao.insert(TestDataFactory.lowStockItem(name = "Salt", quantity = 0.5, minQuantity = 2.0))
        itemDao.insert(TestDataFactory.expiredItem(name = "Yogurt", daysAgo = 3))
    }

    /**
     * Seeds 5 active + 2 purchased shopping items.
     */
    suspend fun seedShoppingList() {
        val dao = db.shoppingListDao()
        dao.insert(TestDataFactory.shoppingItem(customName = "Eggs", quantity = 12.0))
        dao.insert(TestDataFactory.shoppingItem(customName = "Bread", quantity = 1.0))
        dao.insert(TestDataFactory.shoppingItem(customName = "Butter", quantity = 1.0))
        dao.insert(TestDataFactory.shoppingItem(customName = "Chicken", quantity = 1.0, priority = 2))
        dao.insert(TestDataFactory.shoppingItem(customName = "Tomatoes", quantity = 6.0))
        dao.insert(TestDataFactory.shoppingItem(customName = "Cheese", isPurchased = true))
        dao.insert(TestDataFactory.shoppingItem(customName = "Onions", isPurchased = true))
    }

    /**
     * Marks onboarding as completed so the app goes straight to Dashboard.
     */
    suspend fun markOnboardingComplete() {
        db.settingsDao().insert(
            SettingsEntity(
                key = "onboarding_completed",
                value = "true",
                valueType = "boolean",
                description = "Test seed"
            )
        )
    }
}
