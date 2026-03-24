package com.inventory.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkPersonality
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.inkBreathe
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.screens.cook.CookViewModel
import com.inventory.app.ui.theme.AppShapes
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CookCard(
    recipeCount: Int,
    totalItems: Int,
    expiringItems: List<ItemWithDetails>,
    isOffline: Boolean,
    manualRecipeCount: Int = 0,
    lastCookedName: String? = null,
    lastCookedDaysAgo: Int? = null,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hour = java.time.LocalTime.now().hour

    // Compute urgent items once — reused for subtitle + navigation
    val urgentItems = expiringItems.filter { item ->
        item.item.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }?.let { it in 0..2 } == true
    }
    val hasUrgentExpiring = urgentItems.isNotEmpty() && hour >= 16

    val subtitle = when {
        totalItems == 0 -> "Add ingredients to discover recipes"
        isOffline -> "Recipes available when you're back online"
        hour >= 22 -> "Plan tomorrow's meals — $recipeCount recipes ready"
        hasUrgentExpiring -> "Use your ${urgentItems.first().item.name} before it expires — $recipeCount recipes"
        lastCookedName != null && lastCookedDaysAgo != null && lastCookedDaysAgo <= 7 -> {
            val when_ = if (lastCookedDaysAgo == 0) "today"
                        else "$lastCookedDaysAgo day${if (lastCookedDaysAgo > 1) "s" else ""} ago"
            "Last cooked: $lastCookedName, $when_"
        }
        manualRecipeCount > 0 ->
            "$manualRecipeCount own recipe${if (manualRecipeCount > 1) "s" else ""}" +
            if (recipeCount > 0) " \u00b7 $recipeCount AI matches" else ""
        recipeCount == 0 && totalItems < 5 -> "Add more items to unlock recipe ideas"
        recipeCount == 0 && totalItems >= 5 -> "No exact matches — try adding a few staples"
        recipeCount > 0 -> "$recipeCount recipes ready with what you have"
        else -> "Discover what you can cook"
    }

    AppCard(
        onClick = {
            if (hasUrgentExpiring) {
                val ids = urgentItems
                    .sortedBy { it.item.expiryDate }
                    .take(3)
                    .map { it.item.id }
                CookViewModel.pendingExpiringItemIds = ids
                onNavigate(Screen.AiCook.createRoute(ids))
            } else {
                onNavigate(Screen.CookHub.route)
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(
                materialIcon = Icons.Filled.Restaurant,
                inkIconRes = R.drawable.ic_ink_cook,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .inkBreathe(InkPersonality.SIMMER),
                tint = MaterialTheme.appColors.accentOrange
            )
            Spacer(modifier = Modifier.width(Dimens.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "What Can I Cook?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(Dimens.spacingSm))
            ThemedIcon(
                materialIcon = Icons.Filled.ArrowForward,
                inkIconRes = R.drawable.ic_ink_chevron_right,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
