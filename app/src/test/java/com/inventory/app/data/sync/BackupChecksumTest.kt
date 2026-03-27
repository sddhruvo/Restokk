package com.inventory.app.data.sync

import com.inventory.app.data.sync.mapper.BackupChecksumUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupChecksumTest {

    @Test
    fun `same data produces same checksum`() {
        val items = listOf(mapOf<String, Any?>("name" to "Milk", "quantity" to 2.0))
        val shopping = listOf(mapOf<String, Any?>("customName" to "Bread"))
        val recipes = emptyList<Map<String, Any?>>()

        val hash1 = BackupChecksumUtil.computeChecksum(items, shopping, recipes)
        val hash2 = BackupChecksumUtil.computeChecksum(items, shopping, recipes)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `different data produces different checksum`() {
        val items1 = listOf(mapOf<String, Any?>("name" to "Milk", "quantity" to 2.0))
        val items2 = listOf(mapOf<String, Any?>("name" to "Milk", "quantity" to 3.0))
        val empty = emptyList<Map<String, Any?>>()

        val hash1 = BackupChecksumUtil.computeChecksum(items1, empty, empty)
        val hash2 = BackupChecksumUtil.computeChecksum(items2, empty, empty)
        assertNotEquals("Different quantity should produce different checksum", hash1, hash2)
    }

    @Test
    fun `key order does not affect checksum - keys are sorted internally`() {
        val map1 = listOf(mapOf<String, Any?>("a" to "1", "b" to "2"))
        val map2 = listOf(linkedMapOf<String, Any?>("b" to "2", "a" to "1"))
        val empty = emptyList<Map<String, Any?>>()

        val hash1 = BackupChecksumUtil.computeChecksum(map1, empty, empty)
        val hash2 = BackupChecksumUtil.computeChecksum(map2, empty, empty)
        assertEquals("Key order should not matter", hash1, hash2)
    }

    @Test
    fun `empty collections produce valid checksum`() {
        val empty = emptyList<Map<String, Any?>>()
        val hash = BackupChecksumUtil.computeChecksum(empty, empty, empty)
        assertTrue(hash.isNotEmpty())
        assertEquals("SHA-256 hex should be 64 chars", 64, hash.length)
    }

    @Test
    fun `null values handled correctly`() {
        val items = listOf(mapOf<String, Any?>("name" to "Milk", "description" to null))
        val empty = emptyList<Map<String, Any?>>()

        // Should not throw
        val hash = BackupChecksumUtil.computeChecksum(items, empty, empty)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun `adding an item changes checksum - detects missing data`() {
        val empty = emptyList<Map<String, Any?>>()
        val oneItem = listOf(mapOf<String, Any?>("name" to "Milk"))
        val twoItems = listOf(
            mapOf<String, Any?>("name" to "Milk"),
            mapOf<String, Any?>("name" to "Eggs")
        )

        val hash1 = BackupChecksumUtil.computeChecksum(oneItem, empty, empty)
        val hash2 = BackupChecksumUtil.computeChecksum(twoItems, empty, empty)
        assertNotEquals("Adding item should change checksum", hash1, hash2)
    }

    @Test
    fun `swapping collections produces different checksum`() {
        val items = listOf(mapOf<String, Any?>("name" to "Milk"))
        val recipes = listOf(mapOf<String, Any?>("name" to "Pancakes"))
        val empty = emptyList<Map<String, Any?>>()

        val hash1 = BackupChecksumUtil.computeChecksum(items, empty, recipes)
        val hash2 = BackupChecksumUtil.computeChecksum(recipes, empty, items)
        assertNotEquals("Swapping items/recipes should change checksum", hash1, hash2)
    }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
    private fun assertTrue(message: String, condition: Boolean) = org.junit.Assert.assertTrue(message, condition)
}
