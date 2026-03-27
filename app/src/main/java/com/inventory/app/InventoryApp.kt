package com.inventory.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.inventory.app.data.local.dao.ItemDao
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.dao.SettingsDao
import com.inventory.app.data.local.entity.PurchaseHistoryEntity
import com.inventory.app.data.local.entity.SettingsEntity
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.domain.model.UnitSystem
import androidx.work.Constraints
import androidx.work.NetworkType
import com.inventory.app.widget.WidgetUpdateWorker
import com.inventory.app.worker.AutoBackupWorker
import com.inventory.app.worker.NotificationGeneratorWorker
import com.inventory.app.worker.SmartNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class InventoryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsDao: SettingsDao

    @Inject
    lateinit var itemDao: ItemDao

    @Inject
    lateinit var purchaseHistoryDao: PurchaseHistoryDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleExpiryCheck()
        scheduleWidgetRefresh()
        scheduleAutoBackup()
        scheduleNotificationGenerator()
        backfillPurchaseHistory()
        initializeUnitSystem()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(
                    "cooking_timers",
                    "Cooking Timers",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Live countdown timers while cooking" }
            )
        }
    }

    private fun initializeUnitSystem() {
        // Initialize with default region immediately (synchronous, no I/O)
        UnitSystem.initialize(this, "US")
        // Then re-initialize with the user's actual region from settings (async)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val region = settingsRepository.getRegionCode()
                if (region != "US") UnitSystem.initialize(this@InventoryApp, region)
            } catch (_: Exception) {
                // Default US stays active — safe fallback
            }
        }
    }

    private fun scheduleExpiryCheck() {
        val workRequest = PeriodicWorkRequestBuilder<SmartNotificationWorker>(
            12, TimeUnit.HOURS
        ).build()

        // REPLACE swaps old ExpiryNotificationWorker for SmartNotificationWorker
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SmartNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleAutoBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            24, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleNotificationGenerator() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationGeneratorWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(1, TimeUnit.HOURS).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NotificationGeneratorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleWidgetRefresh() {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            30, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun backfillPurchaseHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alreadyDone = settingsDao.get("purchase_history_backfilled")?.value == "true"
                if (alreadyDone) return@launch

                val items = itemDao.getItemsMissingPurchaseHistory()
                for (item in items) {
                    val price = item.purchasePrice ?: continue
                    val qty = item.quantity.coerceAtLeast(1.0)
                    purchaseHistoryDao.insert(
                        PurchaseHistoryEntity(
                            itemId = item.id,
                            quantity = qty,
                            unitPrice = if (qty > 0) price / qty else null,
                            totalPrice = price,
                            purchaseDate = item.purchaseDate ?: item.createdAt.toLocalDate(),
                            expiryDate = item.expiryDate
                        )
                    )
                }

                settingsDao.insert(
                    SettingsEntity(
                        key = "purchase_history_backfilled",
                        value = "true",
                        valueType = "boolean",
                        description = "One-time backfill of purchase history for existing items"
                    )
                )
            } catch (_: Exception) {
                // Silently fail — will retry on next app launch
            }
        }
    }
}
