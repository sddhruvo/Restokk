package com.inventory.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inventory.app.data.local.entity.AppNotificationEntity
import com.inventory.app.data.repository.AppNotificationRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.domain.model.ConversionConditionChecker
import com.inventory.app.domain.model.CreditWarningConditionChecker
import com.inventory.app.domain.model.ExpirySummaryConditionChecker
import com.inventory.app.domain.model.FeatureTipConditionChecker
import com.inventory.app.domain.model.NotificationCheckContext
import com.inventory.app.domain.model.NotificationConditionChecker
import com.inventory.app.domain.model.NotificationTemplateProvider
import com.inventory.app.domain.model.NotificationType
import com.inventory.app.domain.model.TrialInfoConditionChecker
import com.inventory.app.domain.model.ValueRecapConditionChecker
import com.inventory.app.domain.model.WinBackConditionChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NotificationGeneratorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: AppNotificationRepository,
    private val itemRepository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "notification_generator"
        private const val MAX_DAILY_NOTIFICATIONS = 2
        private const val MAX_UNREAD = 10

        // Cadence in milliseconds per notification type
        private val CADENCE_MS = mapOf(
            NotificationType.EXPIRY_SUMMARY to 7L * 24 * 60 * 60 * 1000,
            NotificationType.FEATURE_TIP to 3L * 24 * 60 * 60 * 1000,
            NotificationType.TRIAL_INFO to 3L * 24 * 60 * 60 * 1000,
            NotificationType.CREDIT_WARNING to 1L * 24 * 60 * 60 * 1000,
            NotificationType.CONVERSION to 7L * 24 * 60 * 60 * 1000,
            NotificationType.VALUE_RECAP to 14L * 24 * 60 * 60 * 1000,
            NotificationType.WIN_BACK to 14L * 24 * 60 * 60 * 1000
        )

        // Max dismissals before pausing (within 30-day window)
        private val DISMISSAL_PAUSE = mapOf(
            NotificationType.CONVERSION to 3
        )
    }

    private val checkers: List<NotificationConditionChecker> = listOf(
        TrialInfoConditionChecker(),
        CreditWarningConditionChecker(),
        ConversionConditionChecker(),
        ExpirySummaryConditionChecker(),
        FeatureTipConditionChecker(),
        ValueRecapConditionChecker(),
        WinBackConditionChecker()
    ).sortedBy { it.type.priority }

    override suspend fun doWork(): Result {
        try {
            // 1. Cleanup expired notifications
            notificationRepository.deleteExpired()

            // 2. Check unread cap (cheap check first)
            if (notificationRepository.getUnreadCountOnce() >= MAX_UNREAD) return Result.success()

            // 3. Check daily cap
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            var dailyCount = 0
            for (type in NotificationType.entries) {
                dailyCount += notificationRepository.getCountByTypeSince(type.name, oneDayAgo)
            }
            if (dailyCount >= MAX_DAILY_NOTIFICATIONS) return Result.success()

            // 4. Build context
            val context = buildCheckContext()

            // 5. Iterate checkers in priority order
            for (checker in checkers) {
                if (dailyCount >= MAX_DAILY_NOTIFICATIONS) break

                // Check cadence
                val cadence = CADENCE_MS[checker.type] ?: continue
                val since = System.currentTimeMillis() - cadence
                if (notificationRepository.getCountByTypeSince(checker.type.name, since) > 0) continue

                // Check dismissal pause
                val maxDismissals = DISMISSAL_PAUSE[checker.type]
                if (maxDismissals != null) {
                    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                    val dismissCount = notificationRepository.getDismissedCountByTypeSince(
                        checker.type.name, thirtyDaysAgo
                    )
                    if (dismissCount >= maxDismissals) continue
                }

                // Check condition
                if (!checker.shouldGenerate(context)) continue

                // Generate and insert
                val template = NotificationTemplateProvider.generate(checker.type, context)
                val expiresAt = checker.type.expiresAfterDays?.let {
                    System.currentTimeMillis() + it * 24L * 60 * 60 * 1000
                }
                notificationRepository.insert(
                    AppNotificationEntity(
                        type = checker.type.name,
                        title = template.title,
                        body = template.body,
                        deepLinkRoute = template.deepLinkRoute,
                        ctaText = template.ctaText,
                        ctaRoute = template.ctaRoute,
                        priority = checker.type.priority,
                        expiresAt = expiresAt
                    )
                )
                notificationRepository.enforceUnreadCap(MAX_UNREAD)
                dailyCount++
            }
        } catch (_: Exception) {
            // Non-critical — silently fail, will retry next cycle
        }

        return Result.success()
    }

    private suspend fun buildCheckContext(): NotificationCheckContext {
        val totalItems = itemRepository.getTotalItemCount().first()
        val expiringCount = itemRepository.getExpiringSoonCount().first()
        val lowStockCount = itemRepository.getLowStockCount().first()
        val reportsViewed = settingsRepository.getBoolean(SettingsRepository.KEY_REPORTS_EVER_VIEWED)
        val cookUsed = settingsRepository.getBoolean(SettingsRepository.KEY_COOK_FEATURE_USED)

        return NotificationCheckContext(
            totalItems = totalItems,
            expiringIn7DaysCount = expiringCount,
            lowStockCount = lowStockCount,
            reportsEverViewed = reportsViewed,
            cookFeatureUsed = cookUsed
        )
    }
}
