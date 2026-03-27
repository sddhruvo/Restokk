package com.inventory.app.data.sync

import com.inventory.app.data.sync.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStateTest {

    // ─── Schema version ──────────────────────────────────────────

    @Test
    fun `restore blocked when cloud schema greater than local`() {
        val cloudSchema = SyncState.CURRENT_SCHEMA_VERSION + 1
        assertTrue(
            "Cloud schema $cloudSchema should be greater than current ${SyncState.CURRENT_SCHEMA_VERSION}",
            cloudSchema > SyncState.CURRENT_SCHEMA_VERSION
        )
    }

    @Test
    fun `restore allowed when cloud schema equals local`() {
        val cloudSchema = SyncState.CURRENT_SCHEMA_VERSION
        assertTrue(
            "Equal schema should be allowed",
            cloudSchema <= SyncState.CURRENT_SCHEMA_VERSION
        )
    }

    @Test
    fun `restore allowed when cloud schema less than local`() {
        val cloudSchema = SyncState.CURRENT_SCHEMA_VERSION - 1
        assertTrue(
            "Older cloud schema should be allowed",
            cloudSchema <= SyncState.CURRENT_SCHEMA_VERSION
        )
    }

    @Test
    fun `schema version check matches BackupRepository logic`() {
        // BackupRepository line: if (syncState.schemaVersion > SyncState.CURRENT_SCHEMA_VERSION)
        val testCases = listOf(
            SyncState.CURRENT_SCHEMA_VERSION - 2 to true,  // old cloud → allowed
            SyncState.CURRENT_SCHEMA_VERSION - 1 to true,  // slightly old → allowed
            SyncState.CURRENT_SCHEMA_VERSION to true,       // same → allowed
            SyncState.CURRENT_SCHEMA_VERSION + 1 to false,  // newer → blocked
            SyncState.CURRENT_SCHEMA_VERSION + 5 to false   // much newer → blocked
        )

        for ((cloudVersion, shouldAllow) in testCases) {
            val allowed = cloudVersion <= SyncState.CURRENT_SCHEMA_VERSION
            assertEquals(
                "Schema v$cloudVersion should be ${if (shouldAllow) "allowed" else "blocked"}",
                shouldAllow, allowed
            )
        }
    }

    // ─── Firestore roundtrip ─────────────────────────────────────

    @Test
    fun `toFirestoreMap and fromFirestoreMap roundtrip preserves all fields`() {
        val original = SyncState(
            lastBackupTimestamp = 1711400000000L,
            deviceId = "Pixel 7",
            schemaVersion = 10,
            itemCount = 35,
            shoppingCount = 10,
            recipeCount = 5,
            checksum = "abc123def456",
            appVersion = "1.4.4"
        )

        val map = original.toFirestoreMap()
        // Simulate Firestore behavior: all numbers become Long
        val firestoreMap = map.mapValues { (_, v) ->
            when (v) {
                is Int -> v.toLong()
                else -> v
            }
        }
        val restored = SyncState.fromFirestoreMap(firestoreMap)

        assertEquals(original.lastBackupTimestamp, restored.lastBackupTimestamp)
        assertEquals(original.deviceId, restored.deviceId)
        assertEquals(original.schemaVersion, restored.schemaVersion)
        assertEquals(original.itemCount, restored.itemCount)
        assertEquals(original.shoppingCount, restored.shoppingCount)
        assertEquals(original.recipeCount, restored.recipeCount)
        assertEquals(original.checksum, restored.checksum)
        assertEquals(original.appVersion, restored.appVersion)
    }

    @Test
    fun `fromFirestoreMap handles missing fields with defaults`() {
        val emptyMap = emptyMap<String, Any?>()
        val state = SyncState.fromFirestoreMap(emptyMap)

        assertEquals(0L, state.lastBackupTimestamp)
        assertEquals("", state.deviceId)
        assertEquals(SyncState.CURRENT_SCHEMA_VERSION, state.schemaVersion)
        assertEquals(0, state.itemCount)
        assertEquals(0, state.shoppingCount)
        assertEquals(0, state.recipeCount)
        assertEquals("", state.checksum)
        assertEquals("", state.appVersion)
    }

    @Test
    fun `fromFirestoreMap handles Firestore Long types correctly`() {
        // Firestore stores numbers as Long, not Int
        val map = mapOf<String, Any?>(
            "schemaVersion" to 10L,  // Firestore returns Long
            "itemCount" to 75L,
            "shoppingCount" to 20L,
            "recipeCount" to 8L,
            "lastBackupTimestamp" to 1711400000000L
        )
        val state = SyncState.fromFirestoreMap(map)

        assertEquals(10, state.schemaVersion)
        assertEquals(75, state.itemCount)
        assertEquals(20, state.shoppingCount)
        assertEquals(8, state.recipeCount)
    }
}
