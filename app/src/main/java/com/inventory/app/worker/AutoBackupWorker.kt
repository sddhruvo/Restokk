package com.inventory.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inventory.app.data.sync.BackupRepository
import com.inventory.app.data.sync.model.BackupEligibility
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "auto_backup"
    }

    override suspend fun doWork(): Result {
        return try {
            val eligibility = backupRepository.canBackup()
            if (eligibility is BackupEligibility.Eligible) {
                backupRepository.performBackup()
            }
            Result.success()
        } catch (_: Exception) {
            Result.success() // Non-critical — don't retry
        }
    }
}
