package com.inventory.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.data.local.entity.AppNotificationEntity

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NotificationDrawer(
    notifications: List<AppNotificationEntity>,
    onDismissSheet: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNotificationTap: (AppNotificationEntity) -> Unit,
    onNotificationCtaClick: (AppNotificationEntity) -> Unit,
    onNotificationDismiss: (AppNotificationEntity) -> Unit
) {
    val hasUnread = notifications.any { it.readAt == null }

    ThemedBottomSheet(onDismissRequest = onDismissSheet) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium
            )
            if (hasUnread) {
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read")
                }
            }
        }

        if (notifications.isEmpty()) {
            EmptyStateIllustration(
                icon = Icons.Filled.NotificationsNone,
                headline = "You're all caught up",
                body = "No new notifications",
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = notifications,
                    key = { _, notif -> notif.id }
                ) { index, notification ->
                    StaggeredAnimatedItem(index = index.coerceAtMost(4)) {
                        NotificationCard(
                            notification = notification,
                            onTap = { onNotificationTap(notification) },
                            onCtaClick = if (notification.ctaRoute != null) {
                                { onNotificationCtaClick(notification) }
                            } else null,
                            onDismiss = { onNotificationDismiss(notification) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .animateItemPlacement()
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
