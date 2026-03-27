package com.inventory.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class NotificationType(
    val priority: Int,
    val materialIcon: ImageVector,
    val expiresAfterDays: Int?
) {
    TRIAL_INFO(0, Icons.Filled.Info, 7),
    CREDIT_WARNING(0, Icons.Filled.Warning, 14),
    CONVERSION(1, Icons.Filled.Star, 30),
    FEATURE_TIP(2, Icons.AutoMirrored.Filled.TrendingUp, 14),
    EXPIRY_SUMMARY(1, Icons.Filled.Schedule, 7),
    VALUE_RECAP(2, Icons.Filled.Insights, 30),
    WIN_BACK(1, Icons.Filled.Replay, 14);

    companion object {
        fun fromString(value: String): NotificationType? =
            entries.firstOrNull { it.name == value }
    }
}
