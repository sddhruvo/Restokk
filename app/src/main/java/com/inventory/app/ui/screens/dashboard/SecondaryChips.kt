package com.inventory.app.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.domain.model.UrgencyTarget
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors

private data class ChipData(
    val icon: ImageVector,
    val inkIconRes: Int,
    val tint: Color,
    val text: String,
    val route: String
)

@Composable
fun SecondaryChips(
    heroTarget: UrgencyTarget,
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = buildList {
        if (heroTarget != UrgencyTarget.EXPIRED && uiState.expiredCount > 0) {
            add(ChipData(
                Icons.Filled.Warning, R.drawable.ic_ink_warning,
                MaterialTheme.appColors.statusExpired,
                "${uiState.expiredCount} expired",
                Screen.ExpiringReport.route
            ))
        }
        if (heroTarget != UrgencyTarget.EXPIRING && uiState.expiringSoon > 0) {
            add(ChipData(
                Icons.Filled.Timer, R.drawable.ic_ink_clock,
                MaterialTheme.appColors.accentOrange,
                "${uiState.expiringSoon} expiring",
                Screen.ExpiringReport.route
            ))
        }
        if (heroTarget != UrgencyTarget.LOW_STOCK && uiState.lowStock > 0) {
            add(ChipData(
                Icons.Filled.TrendingDown, R.drawable.ic_ink_trending_down,
                MaterialTheme.appColors.accentGreen,
                "${uiState.lowStock} low stock",
                Screen.LowStockReport.route
            ))
        }
        if (heroTarget != UrgencyTarget.SHOPPING && uiState.shoppingActive > 0) {
            add(ChipData(
                Icons.Filled.ShoppingCart, R.drawable.ic_ink_shopping,
                MaterialTheme.appColors.accentPurple,
                "${uiState.shoppingActive} to buy",
                Screen.ShoppingList.route
            ))
        }
        if (heroTarget != UrgencyTarget.NONE) {
            add(ChipData(
                Icons.Filled.Favorite, R.drawable.ic_ink_heart,
                MaterialTheme.appColors.scoreToColor(uiState.homeScore),
                "Score: ${uiState.homeScore}",
                Screen.PantryHealth.route
            ))
        }
        if (uiState.totalItems > 0) {
            add(ChipData(
                Icons.Filled.Inventory2, R.drawable.ic_ink_box,
                MaterialTheme.appColors.accentBlue,
                "${uiState.totalItems} items",
                Screen.ItemList.createRoute()
            ))
        }
    }

    if (chips.isEmpty()) return

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        chips.forEach { chip ->
            Surface(
                modifier = Modifier.clickable { onNavigate(chip.route) },
                shape = RoundedCornerShape(24.dp),
                color = chip.tint.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
                ) {
                    ThemedIcon(
                        materialIcon = chip.icon,
                        inkIconRes = chip.inkIconRes,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = chip.tint
                    )
                    Text(
                        chip.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
