package com.inventory.app.data.sync

import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.sync.mapper.toFirestoreMap
import com.inventory.app.data.sync.mapper.toItemEntity
import com.inventory.app.data.sync.mapper.toSavedRecipeEntity
import com.inventory.app.data.sync.model.SyncConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class FirestoreMappersTest {

    // ─── ItemEntity roundtrip ────────────────────────────────────

    @Test
    fun `ItemEntity roundtrip preserves all fields`() {
        val original = ItemEntity(
            id = 42,
            name = "Organic Milk",
            description = "2% fat",
            barcode = "1234567890",
            brand = "Amul",
            categoryId = 3,
            subcategoryId = 7,
            storageLocationId = 2,
            quantity = 2.5,
            minQuantity = 1.0,
            smartMinQuantity = 1.5,
            maxQuantity = 5.0,
            unitId = 4,
            expiryDate = LocalDate.of(2026, 4, 15),
            expiryWarningDays = 3,
            openedDate = LocalDate.of(2026, 3, 20),
            daysAfterOpening = 7,
            purchaseDate = LocalDate.of(2026, 3, 18),
            purchasePrice = 65.0,
            isFavorite = true,
            isPaused = false,
            isActive = true,
            notes = "Buy from store A",
            createdAt = LocalDateTime.of(2026, 3, 1, 10, 30),
            updatedAt = LocalDateTime.of(2026, 3, 25, 14, 0)
        )

        val map = original.toFirestoreMap()
        val restored = map.toItemEntity(idOverride = 42)

        assertEquals(original.name, restored.name)
        assertEquals(original.description, restored.description)
        assertEquals(original.barcode, restored.barcode)
        assertEquals(original.brand, restored.brand)
        assertEquals(original.categoryId, restored.categoryId)
        assertEquals(original.subcategoryId, restored.subcategoryId)
        assertEquals(original.storageLocationId, restored.storageLocationId)
        assertEquals(original.quantity, restored.quantity, 0.001)
        assertEquals(original.minQuantity, restored.minQuantity, 0.001)
        assertEquals(original.smartMinQuantity, restored.smartMinQuantity, 0.001)
        assertEquals(original.maxQuantity, restored.maxQuantity)
        assertEquals(original.unitId, restored.unitId)
        assertEquals(original.expiryDate, restored.expiryDate)
        assertEquals(original.expiryWarningDays, restored.expiryWarningDays)
        assertEquals(original.openedDate, restored.openedDate)
        assertEquals(original.daysAfterOpening, restored.daysAfterOpening)
        assertEquals(original.purchaseDate, restored.purchaseDate)
        assertEquals(original.purchasePrice, restored.purchasePrice)
        assertEquals(original.isFavorite, restored.isFavorite)
        assertEquals(original.isPaused, restored.isPaused)
        assertEquals(original.isActive, restored.isActive)
        assertEquals(original.notes, restored.notes)
        // DateTime roundtrip: compare millis (LocalDateTime has no timezone, roundtrip via UTC millis)
        assertEquals(
            original.createdAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
            restored.createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        assertEquals(
            original.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
            restored.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
        )
    }

    @Test
    fun `ItemEntity roundtrip with null optional fields`() {
        val original = ItemEntity(
            id = 1,
            name = "Simple Item",
            // All optional fields left as defaults (null)
        )

        val map = original.toFirestoreMap()
        val restored = map.toItemEntity(idOverride = 1)

        assertEquals(original.name, restored.name)
        assertNull(restored.description)
        assertNull(restored.barcode)
        assertNull(restored.brand)
        assertNull(restored.categoryId)
        assertNull(restored.expiryDate)
        assertNull(restored.purchasePrice)
    }

    @Test
    fun `ItemEntity stores originalId in Firestore map`() {
        val item = ItemEntity(id = 99, name = "Test")
        val map = item.toFirestoreMap()
        assertEquals(99L, map[SyncConstants.FIELD_ORIGINAL_ID])
    }

    // ─── FK remapping ────────────────────────────────────────────

    @Test
    fun `toItemEntity remaps foreign keys correctly`() {
        val map = mapOf<String, Any?>(
            "name" to "Test Item",
            "categoryId" to 10L,
            "subcategoryId" to 20L,
            "storageLocationId" to 30L,
            "unitId" to 40L,
            "quantity" to 1.0,
            "minQuantity" to 0.0,
            "smartMinQuantity" to 0.0,
            "expiryWarningDays" to 7L,
            "isFavorite" to false,
            "isPaused" to false,
            "isActive" to true
        )

        val categoryRemap = mapOf(10L to 100L)
        val subcategoryRemap = mapOf(20L to 200L)
        val locationRemap = mapOf(30L to 300L)
        val unitRemap = mapOf(40L to 400L)

        val entity = map.toItemEntity(
            idOverride = 0,
            categoryIdRemap = categoryRemap,
            subcategoryIdRemap = subcategoryRemap,
            storageLocationIdRemap = locationRemap,
            unitIdRemap = unitRemap
        )

        assertEquals(100L, entity.categoryId)
        assertEquals(200L, entity.subcategoryId)
        assertEquals(300L, entity.storageLocationId)
        assertEquals(400L, entity.unitId)
    }

    @Test
    fun `toItemEntity keeps original FK when no remap exists`() {
        val map = mapOf<String, Any?>(
            "name" to "Test Item",
            "categoryId" to 10L,
            "unitId" to 40L,
            "quantity" to 1.0,
            "minQuantity" to 0.0,
            "smartMinQuantity" to 0.0,
            "expiryWarningDays" to 7L,
            "isFavorite" to false,
            "isPaused" to false,
            "isActive" to true
        )

        val entity = map.toItemEntity(
            idOverride = 0,
            categoryIdRemap = emptyMap(), // No remap
            unitIdRemap = emptyMap()
        )

        assertEquals(10L, entity.categoryId)
        assertEquals(40L, entity.unitId)
    }

    // ─── SavedRecipeEntity roundtrip ─────────────────────────────

    @Test
    fun `SavedRecipeEntity roundtrip preserves all fields`() {
        val original = SavedRecipeEntity(
            id = 5,
            name = "Butter Chicken",
            description = "Classic Indian dish",
            cuisineOrigin = "Indian",
            timeMinutes = 45,
            difficulty = "medium",
            servings = 4,
            ingredientsJson = """[{"name":"chicken","qty":"500g"}]""",
            stepsJson = """["Marinate","Cook","Serve"]""",
            tips = "Use fresh cream",
            personalNotes = "Family favorite",
            isFavorite = true,
            rating = 5,
            sourceSettingsJson = """{"mood":"comfort"}""",
            isActive = true,
            createdAt = 1711400000000L,
            updatedAt = 1711500000000L,
            source = "ai",
            mealType = "dinner",
            tags = """["comfort","weeknight"]""",
            isDraft = false,
            coverPhotoUri = null
        )

        val map = original.toFirestoreMap()
        val restored = map.toSavedRecipeEntity(idOverride = 5)

        assertEquals(original.name, restored.name)
        assertEquals(original.description, restored.description)
        assertEquals(original.cuisineOrigin, restored.cuisineOrigin)
        assertEquals(original.timeMinutes, restored.timeMinutes)
        assertEquals(original.difficulty, restored.difficulty)
        assertEquals(original.servings, restored.servings)
        assertEquals(original.ingredientsJson, restored.ingredientsJson)
        assertEquals(original.stepsJson, restored.stepsJson)
        assertEquals(original.tips, restored.tips)
        assertEquals(original.personalNotes, restored.personalNotes)
        assertEquals(original.isFavorite, restored.isFavorite)
        assertEquals(original.rating, restored.rating)
        assertEquals(original.sourceSettingsJson, restored.sourceSettingsJson)
        assertEquals(original.isActive, restored.isActive)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt, restored.updatedAt)
        assertEquals(original.source, restored.source)
        assertEquals(original.mealType, restored.mealType)
        assertEquals(original.tags, restored.tags)
        assertEquals(original.isDraft, restored.isDraft)
        assertNull(restored.coverPhotoUri)
    }

    // ─── Edge cases ──────────────────────────────────────────────

    @Test
    fun `toItemEntity with idOverride 0 sets id to 0 for Room auto-generate`() {
        val map = mapOf<String, Any?>(
            SyncConstants.FIELD_ORIGINAL_ID to 99L,
            "name" to "Test",
            "quantity" to 1.0,
            "minQuantity" to 0.0,
            "smartMinQuantity" to 0.0,
            "expiryWarningDays" to 7L,
            "isFavorite" to false,
            "isPaused" to false,
            "isActive" to true
        )
        val entity = map.toItemEntity(idOverride = 0)
        assertEquals(0L, entity.id)
    }

    @Test
    fun `LocalDate survives epoch day roundtrip`() {
        val date = LocalDate.of(2026, 12, 31)
        val item = ItemEntity(id = 1, name = "Test", expiryDate = date)
        val map = item.toFirestoreMap()
        val restored = map.toItemEntity(idOverride = 1)
        assertEquals(date, restored.expiryDate)
    }
}
