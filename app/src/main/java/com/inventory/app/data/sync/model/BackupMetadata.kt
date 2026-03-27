package com.inventory.app.data.sync.model

/**
 * Summary of a backup's contents — used for UI display and restore prompts.
 */
data class BackupMetadata(
    val lastBackupAt: Long,
    val itemCount: Int,
    val shoppingCount: Int,
    val recipeCount: Int,
    val schemaVersion: Int
)
