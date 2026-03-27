package com.inventory.app.data.sync.model

/**
 * UI state for the backup operation.
 */
sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object InProgress : BackupStatus
    data class Success(val metadata: BackupMetadata) : BackupStatus
    data class Failed(val error: String) : BackupStatus
}
