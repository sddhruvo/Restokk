package com.inventory.app.data.sync

import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.sync.mapper.BackupPrioritizer
import com.inventory.app.data.sync.model.SyncConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BackupPrioritizerTest {

    // ─── Helper factories ────────────────────────────────────────

    private fun makeItem(
        id: Long,
        name: String = "Item $id",
        expiryDate: LocalDate? = null,
        updatedAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        quantity: Double = 5.0,
        minQuantity: Double = 0.0
    ) = ItemEntity(
        id = id,
        name = name,
        expiryDate = expiryDate,
        updatedAt = updatedAt,
        quantity = quantity,
        minQuantity = minQuantity
    )

    private fun makeRecipe(
        id: Long,
        name: String = "Recipe $id",
        isFavorite: Boolean = false,
        updatedAt: Long = 1_000_000L
    ) = SavedRecipeEntity(
        id = id,
        name = name,
        isFavorite = isFavorite,
        updatedAt = updatedAt
    )

    // ─── Item tests ──────────────────────────────────────────────

    @Test
    fun `items at exactly 75 - all returned unchanged`() {
        val items = (1L..75L).map { makeItem(it) }
        val result = BackupPrioritizer.prioritizeItems(items, SyncConstants.FREE_TIER_ITEM_LIMIT)
        assertEquals(75, result.size)
        assertEquals(items.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun `items below limit - all returned`() {
        val items = (1L..30L).map { makeItem(it) }
        val result = BackupPrioritizer.prioritizeItems(items, SyncConstants.FREE_TIER_ITEM_LIMIT)
        assertEquals(30, result.size)
    }

    @Test
    fun `items above 75 - truncated to exactly 75`() {
        val items = (1L..100L).map { makeItem(it) }
        val result = BackupPrioritizer.prioritizeItems(items, SyncConstants.FREE_TIER_ITEM_LIMIT)
        assertEquals(75, result.size)
    }

    @Test
    fun `items above 75 - expiry items prioritized over non-expiry`() {
        val noExpiry = (1L..50L).map { makeItem(it) }
        val withExpiry = (51L..100L).map {
            makeItem(it, expiryDate = LocalDate.now().plusDays(it))
        }
        val all = noExpiry + withExpiry // 100 items, 50 with expiry
        val result = BackupPrioritizer.prioritizeItems(all, SyncConstants.FREE_TIER_ITEM_LIMIT)

        // All 50 expiry items should be included
        val expiryIds = withExpiry.map { it.id }.toSet()
        val resultIds = result.map { it.id }.toSet()
        assertTrue("All expiry items should be in the result", resultIds.containsAll(expiryIds))
    }

    @Test
    fun `items above 75 - soonest expiry comes first`() {
        val items = (1L..80L).map {
            makeItem(it, expiryDate = LocalDate.now().plusDays(80 - it)) // item 1 expires latest, 80 soonest
        }
        val result = BackupPrioritizer.prioritizeItems(items, SyncConstants.FREE_TIER_ITEM_LIMIT)

        // The 5 items dropped should be the ones expiring latest (ids 1-5)
        val droppedIds = items.map { it.id }.toSet() - result.map { it.id }.toSet()
        // Items 1-5 have the latest expiry dates (80-1=79, 80-2=78, ... days out)
        assertTrue("Items with latest expiry should be dropped", droppedIds.all { it <= 5 })
    }

    @Test
    fun `items above 75 - recently updated preferred among equal priority`() {
        // All items have no expiry, no min quantity — only updatedAt differs
        val items = (1L..100L).map {
            makeItem(it, updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(it))
        }
        val result = BackupPrioritizer.prioritizeItems(items, SyncConstants.FREE_TIER_ITEM_LIMIT)

        // The 75 most recently updated (ids 26-100) should be kept
        val resultIds = result.map { it.id }.toSet()
        assertTrue("Most recently updated items kept", resultIds.containsAll((26L..100L).toSet()))
    }

    @Test
    fun `items above 75 - low stock items preferred over well-stocked`() {
        // All have no expiry, same updatedAt — only stock ratio differs
        val baseTime = LocalDateTime.of(2026, 1, 1, 0, 0)
        val lowStock = (1L..40L).map {
            makeItem(it, updatedAt = baseTime, quantity = 1.0, minQuantity = 10.0) // ratio 0.1
        }
        val wellStocked = (41L..100L).map {
            makeItem(it, updatedAt = baseTime, quantity = 100.0, minQuantity = 1.0) // ratio 100
        }
        val all = lowStock + wellStocked
        val result = BackupPrioritizer.prioritizeItems(all, SyncConstants.FREE_TIER_ITEM_LIMIT)

        // All 40 low-stock items should be included
        val lowStockIds = lowStock.map { it.id }.toSet()
        assertTrue("All low-stock items should be kept", result.map { it.id }.toSet().containsAll(lowStockIds))
    }

    @Test
    fun `empty list returns empty`() {
        val result = BackupPrioritizer.prioritizeItems(emptyList(), SyncConstants.FREE_TIER_ITEM_LIMIT)
        assertTrue(result.isEmpty())
    }

    // ─── Recipe tests ────────────────────────────────────────────

    @Test
    fun `recipes at exactly 10 - all returned`() {
        val recipes = (1L..10L).map { makeRecipe(it) }
        val result = BackupPrioritizer.prioritizeRecipes(recipes, SyncConstants.FREE_TIER_RECIPE_LIMIT)
        assertEquals(10, result.size)
    }

    @Test
    fun `recipes above 10 - truncated to 10`() {
        val recipes = (1L..20L).map { makeRecipe(it) }
        val result = BackupPrioritizer.prioritizeRecipes(recipes, SyncConstants.FREE_TIER_RECIPE_LIMIT)
        assertEquals(10, result.size)
    }

    @Test
    fun `recipes above 10 - favorites prioritized`() {
        val nonFav = (1L..15L).map { makeRecipe(it, isFavorite = false) }
        val fav = (16L..20L).map { makeRecipe(it, isFavorite = true) }
        val all = nonFav + fav
        val result = BackupPrioritizer.prioritizeRecipes(all, SyncConstants.FREE_TIER_RECIPE_LIMIT)

        val favIds = fav.map { it.id }.toSet()
        assertTrue("All favorites should be in result", result.map { it.id }.toSet().containsAll(favIds))
    }

    @Test
    fun `recipes above 10 - recently updated preferred among non-favorites`() {
        val recipes = (1L..20L).map {
            makeRecipe(it, isFavorite = false, updatedAt = it * 1000L)
        }
        val result = BackupPrioritizer.prioritizeRecipes(recipes, SyncConstants.FREE_TIER_RECIPE_LIMIT)

        // The 10 most recently updated (ids 11-20) should be kept
        val resultIds = result.map { it.id }.toSet()
        assertTrue("Most recently updated kept", resultIds.containsAll((11L..20L).toSet()))
    }
}
