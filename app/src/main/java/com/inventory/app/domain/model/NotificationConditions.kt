package com.inventory.app.domain.model

data class NotificationCheckContext(
    val totalItems: Int = 0,
    val expiringIn7DaysCount: Int = 0,
    val lowStockCount: Int = 0,
    val reportsEverViewed: Boolean = false,
    val cookFeatureUsed: Boolean = false,
    // Phase 2 fields — default to null until subscription system exists
    val isTrialActive: Boolean? = null,
    val trialDaysRemaining: Int? = null,
    val creditsRemaining: Int? = null,
    val isSubscribed: Boolean? = null,
    val daysSinceLastOpen: Int? = null
)

interface NotificationConditionChecker {
    val type: NotificationType
    suspend fun shouldGenerate(context: NotificationCheckContext): Boolean
}

class FeatureTipConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.FEATURE_TIP

    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean {
        val reportsTip = context.totalItems >= 10 && !context.reportsEverViewed
        val cookTip = context.expiringIn7DaysCount >= 5 && !context.cookFeatureUsed
        return reportsTip || cookTip
    }

    fun getVariant(context: NotificationCheckContext): FeatureTipVariant {
        val reportsTip = context.totalItems >= 10 && !context.reportsEverViewed
        return if (reportsTip) FeatureTipVariant.REPORTS else FeatureTipVariant.COOK
    }

    enum class FeatureTipVariant { REPORTS, COOK }
}

class ExpirySummaryConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.EXPIRY_SUMMARY

    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean =
        context.expiringIn7DaysCount >= 3
}

// Phase 2 stubs — all return false until subscription/credit state is wired
class TrialInfoConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.TRIAL_INFO
    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean = false
}

class CreditWarningConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.CREDIT_WARNING
    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean = false
}

class ConversionConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.CONVERSION
    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean = false
}

class ValueRecapConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.VALUE_RECAP
    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean = false
}

class WinBackConditionChecker : NotificationConditionChecker {
    override val type = NotificationType.WIN_BACK
    override suspend fun shouldGenerate(context: NotificationCheckContext): Boolean = false
}
