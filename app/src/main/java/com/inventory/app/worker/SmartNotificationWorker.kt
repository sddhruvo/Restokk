package com.inventory.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inventory.app.MainActivity
import com.inventory.app.R
import com.inventory.app.data.local.dao.PurchaseHistoryDao
import com.inventory.app.data.local.dao.ShoppingListDao
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.domain.model.PurchaseDataPoint
import com.inventory.app.domain.model.PurchaseRhythmCalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class SmartNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val shoppingListDao: ShoppingListDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "smart_notification_check"
        const val EXTRA_NAV_ROUTE = "extra_nav_route"
        const val CHANNEL_EXPIRY = "expiry_notifications"
        const val CHANNEL_RESTOCK = "restock_predictions"
        const val CHANNEL_SHOPPING = "shopping_reminders"
        const val MAX_NOTIFICATIONS_PER_WEEK = 3
        private const val NOTIFICATION_ID_EXPIRY = 100
        private const val NOTIFICATION_ID_RESTOCK = 200
        private const val NOTIFICATION_ID_SHOPPING = 300
    }

    override suspend fun doWork(): Result {
        createNotificationChannels()

        // Master toggle
        val enabled = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIFICATIONS_ENABLED, true)
        if (!enabled) return Result.success()

        // Weekly throttle
        val sentThisWeek = settingsRepository.getNotificationCountThisWeek()
        if (sentThisWeek >= MAX_NOTIFICATIONS_PER_WEEK) return Result.success()

        // Try each notification type in priority order (only send one per run)
        val expiryEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_EXPIRY_ENABLED, true)
        if (expiryEnabled && tryExpiryNotification()) {
            settingsRepository.recordNotificationSent()
            return Result.success()
        }

        val restockEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_RESTOCK_ENABLED, true)
        if (restockEnabled && tryRestockNotification()) {
            settingsRepository.recordNotificationSent()
            return Result.success()
        }

        val shoppingEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_NOTIF_SHOPPING_ENABLED, true)
        if (shoppingEnabled && tryShoppingReminder()) {
            settingsRepository.recordNotificationSent()
            return Result.success()
        }

        return Result.success()
    }

    private suspend fun tryExpiryNotification(): Boolean {
        val warningDays = settingsRepository.getExpiryWarningDays()
        val expiringItems = itemRepository.getExpiringSoon(warningDays, limit = 10).first()
        if (expiringItems.isEmpty()) return false

        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val tomorrowEpoch = today.plusDays(1).toEpochDay()

        // Categorize by urgency
        val expiringToday = expiringItems.filter { it.item.expiryDate?.toEpochDay() == todayEpoch }
        val expiringTomorrow = expiringItems.filter { it.item.expiryDate?.toEpochDay() == tomorrowEpoch }
        val expiringThisWeek = expiringItems.filter {
            val epoch = it.item.expiryDate?.toEpochDay() ?: return@filter false
            epoch > tomorrowEpoch && epoch <= today.plusDays(7).toEpochDay()
        }

        val (title, text) = when {
            expiringToday.isNotEmpty() -> {
                val firstName = expiringToday.first().item.name
                val others = expiringToday.size - 1
                val suffix = if (others > 0) " + $others other${if (others > 1) "s" else ""}" else ""
                "Expires Today!" to "$firstName expires today!$suffix"
            }
            expiringTomorrow.isNotEmpty() -> {
                val firstName = expiringTomorrow.first().item.name
                val others = expiringTomorrow.size - 1
                val suffix = if (others > 0) " + $others other${if (others > 1) "s" else ""}" else ""
                "Expiring Tomorrow" to "$firstName expires tomorrow$suffix"
            }
            expiringThisWeek.isNotEmpty() -> {
                val count = expiringThisWeek.size
                "Expiring This Week" to "$count item${if (count > 1) "s" else ""} expiring this week"
            }
            else -> return false
        }

        sendNotification(
            id = NOTIFICATION_ID_EXPIRY,
            channelId = CHANNEL_EXPIRY,
            title = title,
            text = text,
            targetRoute = "dashboard"
        )
        return true
    }

    private suspend fun tryRestockNotification(): Boolean {
        val velocityData = purchaseHistoryDao.getPurchaseDataForVelocity()
        if (velocityData.isEmpty()) return false

        // Build purchase map
        val purchasesByItem = mutableMapOf<Long, MutableList<PurchaseDataPoint>>()
        val itemNames = mutableMapOf<Long, String>()
        for (row in velocityData) {
            val date = LocalDate.ofEpochDay(row.purchaseDate)
            purchasesByItem.getOrPut(row.itemId) { mutableListOf() }
                .add(PurchaseDataPoint(date, row.quantity))
            itemNames[row.itemId] = row.itemName
        }

        val today = LocalDate.now()
        val predictions = PurchaseRhythmCalculator.calculatePredictions(purchasesByItem, itemNames, today)

        // Find predictions due today or overdue, skip items already on shopping list
        val activeShoppingItemIds = shoppingListDao.getActiveItemIds().toSet()
        val duePredictions = predictions.filter { prediction ->
            !prediction.predictedDate.isAfter(today) && prediction.itemId !in activeShoppingItemIds
        }

        if (duePredictions.isEmpty()) return false

        val best = duePredictions.first() // highest confidence (list is sorted)

        val intent = Intent(applicationContext, AddToShoppingListReceiver::class.java).apply {
            putExtra(AddToShoppingListReceiver.EXTRA_ITEM_ID, best.itemId)
            putExtra(AddToShoppingListReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_RESTOCK)
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            best.itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        sendNotification(
            id = NOTIFICATION_ID_RESTOCK,
            channelId = CHANNEL_RESTOCK,
            title = "Time to Restock?",
            text = "You usually buy ${best.itemName} around this time",
            targetRoute = "shopping",
            actionText = "Add to Shopping List",
            actionPendingIntent = addPendingIntent
        )
        return true
    }

    private suspend fun tryShoppingReminder(): Boolean {
        val activeCount = shoppingListDao.getActiveCount().first()
        if (activeCount <= 0) return false

        sendNotification(
            id = NOTIFICATION_ID_SHOPPING,
            channelId = CHANNEL_SHOPPING,
            title = "Shopping Reminder",
            text = "You have $activeCount item${if (activeCount > 1) "s" else ""} on your shopping list",
            targetRoute = "shopping"
        )
        return true
    }

    private fun sendNotification(
        id: Int,
        channelId: String,
        title: String,
        text: String,
        targetRoute: String,
        actionText: String? = null,
        actionPendingIntent: PendingIntent? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        // Deep link intent → opens MainActivity with target route
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(EXTRA_NAV_ROUTE, targetRoute)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(
                when (channelId) {
                    CHANNEL_EXPIRY -> NotificationCompat.PRIORITY_HIGH
                    CHANNEL_RESTOCK -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }
            )

        if (actionText != null && actionPendingIntent != null) {
            builder.addAction(0, actionText, actionPendingIntent)
        }

        NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
    }

    private fun createNotificationChannels() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_EXPIRY, "Expiry Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when items are about to expire"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RESTOCK, "Smart Restock", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Suggestions to restock items based on your buying patterns"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SHOPPING, "Shopping Reminders", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Reminders about items on your shopping list"
            }
        )
    }

}
