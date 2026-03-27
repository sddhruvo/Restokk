package com.inventory.app.data.sync.model

/**
 * Represents the sync metadata stored in Firestore at users/{uid}/metadata/sync_state.
 * Tracks backup state, schema compatibility, and integrity checksums.
 */
data class SyncState(
    val lastBackupTimestamp: Long = 0L,
    val deviceId: String = "",
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val itemCount: Int = 0,
    val shoppingCount: Int = 0,
    val recipeCount: Int = 0,
    val checksum: String = "",
    val appVersion: String = ""
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 10

        fun fromFirestoreMap(map: Map<String, Any?>): SyncState = SyncState(
            lastBackupTimestamp = (map["lastBackupTimestamp"] as? Long) ?: 0L,
            deviceId = (map["deviceId"] as? String) ?: "",
            schemaVersion = (map["schemaVersion"] as? Long)?.toInt() ?: CURRENT_SCHEMA_VERSION,
            itemCount = (map["itemCount"] as? Long)?.toInt() ?: 0,
            shoppingCount = (map["shoppingCount"] as? Long)?.toInt() ?: 0,
            recipeCount = (map["recipeCount"] as? Long)?.toInt() ?: 0,
            checksum = (map["checksum"] as? String) ?: "",
            appVersion = (map["appVersion"] as? String) ?: ""
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "lastBackupTimestamp" to lastBackupTimestamp,
        "deviceId" to deviceId,
        "schemaVersion" to schemaVersion,
        "itemCount" to itemCount,
        "shoppingCount" to shoppingCount,
        "recipeCount" to recipeCount,
        "checksum" to checksum,
        "appVersion" to appVersion
    )
}
