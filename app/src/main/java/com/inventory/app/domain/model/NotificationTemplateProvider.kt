package com.inventory.app.domain.model

import com.inventory.app.ui.navigation.Screen

data class NotificationTemplate(
    val title: String,
    val body: String,
    val deepLinkRoute: String? = null,
    val ctaText: String? = null,
    val ctaRoute: String? = null
)

object NotificationTemplateProvider {

    fun generate(type: NotificationType, context: NotificationCheckContext): NotificationTemplate {
        return when (type) {
            NotificationType.FEATURE_TIP -> generateFeatureTip(context)
            NotificationType.EXPIRY_SUMMARY -> generateExpirySummary(context)
            NotificationType.TRIAL_INFO -> NotificationTemplate(
                title = "Welcome to Restokk Pro",
                body = "Your free trial is active. Explore all premium features!"
            )
            NotificationType.CREDIT_WARNING -> NotificationTemplate(
                title = "AI Credits Running Low",
                body = "You have ${context.creditsRemaining ?: 0} AI scans remaining this month."
            )
            NotificationType.CONVERSION -> NotificationTemplate(
                title = "Unlock Full Power",
                body = "Upgrade to Pro for unlimited AI scans and premium features.",
                ctaText = "See Plans",
                ctaRoute = Screen.Settings.route
            )
            NotificationType.VALUE_RECAP -> NotificationTemplate(
                title = "Your Kitchen Stats",
                body = "You're tracking ${context.totalItems} items. See your full pantry health report.",
                deepLinkRoute = Screen.PantryHealth.route
            )
            NotificationType.WIN_BACK -> NotificationTemplate(
                title = "Your Kitchen Misses You",
                body = "Check on your pantry — some items may need attention.",
                deepLinkRoute = Screen.Dashboard.route
            )
        }
    }

    private fun generateFeatureTip(context: NotificationCheckContext): NotificationTemplate {
        val checker = FeatureTipConditionChecker()
        return when (checker.getVariant(context)) {
            FeatureTipConditionChecker.FeatureTipVariant.REPORTS -> NotificationTemplate(
                title = "Your kitchen data is ready",
                body = "You have ${context.totalItems} items tracked. See spending trends, usage patterns, and more.",
                deepLinkRoute = Screen.Reports.route,
                ctaText = "View Reports",
                ctaRoute = Screen.Reports.route
            )
            FeatureTipConditionChecker.FeatureTipVariant.COOK -> NotificationTemplate(
                title = "Don't let food go to waste",
                body = "${context.expiringIn7DaysCount} items expire soon. Get recipe ideas to use them up!",
                deepLinkRoute = Screen.CookHub.route,
                ctaText = "Cook Now",
                ctaRoute = Screen.CookHub.route
            )
        }
    }

    private fun generateExpirySummary(context: NotificationCheckContext): NotificationTemplate {
        return NotificationTemplate(
            title = "Weekly Expiry Summary",
            body = "${context.expiringIn7DaysCount} items expire in the next 7 days. Review and plan meals around them.",
            deepLinkRoute = Screen.ExpiringReport.route,
            ctaText = "Review",
            ctaRoute = Screen.ExpiringReport.route
        )
    }
}
