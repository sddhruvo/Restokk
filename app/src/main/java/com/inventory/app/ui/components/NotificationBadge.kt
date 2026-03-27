package com.inventory.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals

@Composable
fun NotificationBadge(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInk = MaterialTheme.visuals.isInk
    val breatheModifier = if (unreadCount > 0 && isInk) {
        Modifier.inkBreathe()
    } else {
        Modifier
    }

    BadgedBox(
        badge = {
            if (unreadCount > 0) {
                Badge {
                    Text(if (unreadCount > 9) "9+" else unreadCount.toString())
                }
            }
        },
        modifier = modifier.then(breatheModifier)
    ) {
        IconButton(onClick = onClick) {
            ThemedIcon(
                materialIcon = Icons.Filled.Notifications,
                inkIconRes = 0,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
