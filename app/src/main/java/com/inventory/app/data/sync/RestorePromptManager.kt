package com.inventory.app.data.sync

import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.sync.model.BackupMetadata
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines whether to show a restore prompt after Google Sign-In.
 * Checks if the user has existing cloud backup data.
 */
@Singleton
class RestorePromptManager @Inject constructor(
    private val backupRepository: BackupRepository,
    private val itemDao: ItemDao
) {
    /**
     * Check if the signed-in user has an existing cloud backup.
     * Returns the backup metadata if found, null otherwise.
     */
    suspend fun checkForExistingBackup(): BackupMetadata? {
        return backupRepository.checkExistingBackup()
    }

    /**
     * Whether the local database already has user data (non-empty inventory).
     * Used to decide between merge vs. replace restore mode.
     */
    suspend fun hasLocalData(): Boolean {
        return itemDao.getAllActiveSnapshot().isNotEmpty()
    }
}
