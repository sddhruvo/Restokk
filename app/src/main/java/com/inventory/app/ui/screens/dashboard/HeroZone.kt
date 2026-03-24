package com.inventory.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.inventory.app.R
import com.inventory.app.domain.model.UrgencyLevel
import com.inventory.app.domain.model.UrgencyResult
import com.inventory.app.domain.model.UrgencyTarget
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.EmpathyCartIcon
import com.inventory.app.ui.components.EmpathyHeartIcon
import com.inventory.app.ui.components.EmpathyTrendingIcon
import com.inventory.app.ui.components.EmpathyWarningIcon
import com.inventory.app.ui.components.ShimmerStatCard
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.ThemedProgressBar
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.components.computeStockBar
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.screens.cook.CookViewModel
import com.inventory.app.ui.theme.AppShapes
import com.inventory.app.ui.theme.sectionHeader
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.appColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface HeroAction {
    data class AddToShoppingList(val itemId: Long, val categoryId: Long?) : HeroAction
    data class PauseItem(val itemId: Long) : HeroAction
    data class TossItem(val itemId: Long) : HeroAction
    data class MarkStillGood(val itemId: Long) : HeroAction
    data class NavigateTo(val route: String) : HeroAction
}

@Composable
fun HeroZone(
    urgencyResult: UrgencyResult,
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    onAction: (HeroAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        ShimmerStatCard(modifier = modifier.fillMaxWidth().height(160.dp))
        return
    }

    val reduceMotion = com.inventory.app.ui.theme.LocalReduceMotion.current

    AnimatedContent(
        targetState = urgencyResult.target,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            } else {
                (fadeIn(spring(dampingRatio = 1f, stiffness = 200f)) +
                    slideInVertically(spring(dampingRatio = 1f, stiffness = 200f)) { it / 8 }) togetherWith
                    (fadeOut(tween(100)) + slideOutVertically(tween(100)) { -it / 8 })
            }
        },
        label = "heroZone",
        modifier = modifier
    ) { target ->
        when (target) {
            UrgencyTarget.EXPIRED -> HeroExpired(uiState, onNavigate, onAction)
            UrgencyTarget.EXPIRING -> HeroExpiring(uiState, onNavigate, onAction)
            UrgencyTarget.LOW_STOCK -> HeroLowStock(uiState, onNavigate, onAction)
            UrgencyTarget.SHOPPING -> HeroShopping(uiState, onNavigate)
            UrgencyTarget.TOTAL_ITEMS -> HeroTotalItems(uiState, onNavigate)
            UrgencyTarget.NONE -> HeroHealthy(uiState, onNavigate)
        }
    }
}

// ─── EXPIRED — Triage Mode ─────────────────────────────────────────────

@Composable
private fun HeroExpired(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    onAction: (HeroAction) -> Unit
) {
    val expiredColor = MaterialTheme.appColors.statusExpired
    val reduceMotion = com.inventory.app.ui.theme.LocalReduceMotion.current

    val glowAlpha = if (!reduceMotion) {
        val transition = rememberInfiniteTransition(label = "expiredGlow")
        val alpha by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "expiredGlowAlpha"
        )
        alpha
    } else 0.15f

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = expiredColor.copy(alpha = glowAlpha),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            },
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmpathyWarningIcon(
                    expiringCount = uiState.expiredItems.size,
                    modifier = Modifier.size(Dimens.iconSizeMd),
                    tint = expiredColor
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                Text(
                    "${uiState.expiredItems.size} item${if (uiState.expiredItems.size != 1) "s" else ""} past their date",
                    style = MaterialTheme.typography.sectionHeader,
                    color = expiredColor
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            uiState.expiredItems.take(5).forEach { item ->
                val daysAgo = item.item.expiryDate?.let {
                    ChronoUnit.DAYS.between(it, LocalDate.now())
                } ?: 0

                ListItem(
                    headlineContent = { Text(item.item.name) },
                    supportingContent = {
                        Text(
                            "Expired $daysAgo day${if (daysAgo != 1L) "s" else ""} ago",
                            style = MaterialTheme.typography.bodySmall,
                            color = expiredColor
                        )
                    },
                    trailingContent = {
                        Row {
                            IconButton(
                                onClick = { onAction(HeroAction.TossItem(item.item.id)) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.Delete,
                                    inkIconRes = R.drawable.ic_ink_delete,
                                    contentDescription = "Toss ${item.item.name}",
                                    modifier = Modifier.size(20.dp),
                                    tint = expiredColor
                                )
                            }
                            IconButton(
                                onClick = { onAction(HeroAction.MarkStillGood(item.item.id)) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.CheckCircle,
                                    inkIconRes = R.drawable.ic_ink_check,
                                    contentDescription = "Mark ${item.item.name} still good",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.appColors.accentGreen
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        onNavigate(Screen.ItemDetail.createRoute(item.item.id))
                    }
                )
            }

            if (uiState.expiredItems.size > 5) {
                TextButton(onClick = { onNavigate(Screen.ExpiringReport.route) }) {
                    Text("View All (${uiState.expiredItems.size})")
                }
            }
        }
    }
}

// ─── EXPIRING — Use It Up ──────────────────────────────────────────────

@Composable
private fun HeroExpiring(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    onAction: (HeroAction) -> Unit
) {
    val expiringColor = MaterialTheme.appColors.accentOrange

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmpathyWarningIcon(
                    expiringCount = uiState.expiringSoon,
                    modifier = Modifier.size(Dimens.iconSizeMd),
                    tint = expiringColor
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                Text(
                    "${uiState.expiringSoon} item${if (uiState.expiringSoon != 1) "s" else ""} to use soon",
                    style = MaterialTheme.typography.sectionHeader,
                    color = expiringColor
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            uiState.expiringItems.take(5).forEach { item ->
                val daysUntil = item.item.expiryDate?.let {
                    ChronoUnit.DAYS.between(LocalDate.now(), it)
                }
                val color = when {
                    daysUntil == null -> MaterialTheme.colorScheme.onSurface
                    daysUntil < 0 -> MaterialTheme.appColors.statusExpired
                    daysUntil <= 3 -> MaterialTheme.appColors.statusExpiring
                    else -> MaterialTheme.colorScheme.onSurface
                }

                ListItem(
                    headlineContent = { Text(item.item.name) },
                    supportingContent = {
                        Text(
                            when {
                                daysUntil == null -> ""
                                daysUntil < 0 -> "Expired ${-daysUntil} days ago"
                                daysUntil == 0L -> "Expires today"
                                else -> "Expires in $daysUntil days"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    },
                    trailingContent = {
                        Row {
                            IconButton(
                                onClick = { onAction(HeroAction.AddToShoppingList(item.item.id, null)) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.AddShoppingCart,
                                    inkIconRes = R.drawable.ic_ink_add_to_cart,
                                    contentDescription = "Add to shopping list",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onAction(HeroAction.PauseItem(item.item.id)) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.PauseCircleOutline,
                                    inkIconRes = R.drawable.ic_ink_pause,
                                    contentDescription = "Pause alerts",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        onNavigate(Screen.ItemDetail.createRoute(item.item.id))
                    }
                )
            }

            if (uiState.expiringItems.size > 5) {
                TextButton(onClick = { onNavigate(Screen.ExpiringReport.route) }) {
                    Text("View All (${uiState.expiringItems.size})")
                }
            }

            // Time-aware cook CTA
            val hour = java.time.LocalTime.now().hour
            if (hour >= 16 && uiState.expiringItems.isNotEmpty()) {
                val firstItem = uiState.expiringItems.first().item.name
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                AppCard(
                    onClick = {
                        val ids = uiState.expiringItems
                            .sortedBy { it.item.expiryDate }
                            .take(3)
                            .map { it.item.id }
                        CookViewModel.pendingExpiringItemIds = ids
                        onNavigate(Screen.AiCook.createRoute(ids))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Timer,
                            inkIconRes = R.drawable.ic_ink_clock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.appColors.accentOrange
                        )
                        Text(
                            "Cook with $firstItem tonight?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        ThemedIcon(
                            materialIcon = Icons.Filled.ArrowForward,
                            inkIconRes = R.drawable.ic_ink_chevron_right,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.appColors.accentOrange
                        )
                    }
                }
            }
        }
    }
}

// ─── LOW_STOCK — Time to Shop ──────────────────────────────────────────

@Composable
private fun HeroLowStock(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    onAction: (HeroAction) -> Unit
) {
    val lowStockColor = MaterialTheme.appColors.accentGreen

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmpathyTrendingIcon(
                    lowStockCount = uiState.lowStock,
                    modifier = Modifier.size(Dimens.iconSizeMd),
                    tint = lowStockColor
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                Text(
                    "${uiState.lowStock} item${if (uiState.lowStock != 1) "s" else ""} running low",
                    style = MaterialTheme.typography.sectionHeader,
                    color = lowStockColor
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            uiState.lowStockItems.take(5).forEach { item ->
                val stockState = computeStockBar(item.item.quantity, item.item.minQuantity, item.item.smartMinQuantity, item.item.maxQuantity, uiState.lowStockThreshold)
                val ratio = stockState.ratio
                val barColor = MaterialTheme.appColors.stockColor(ratio, stockState.threshold)

                ListItem(
                    headlineContent = { Text(item.item.name) },
                    supportingContent = {
                        Column {
                            Text(
                                "Qty: ${item.item.quantity.formatQty()} / ${stockState.ceiling.formatQty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = barColor
                            )
                            ThemedProgressBar(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(6.dp),
                                color = barColor,
                                trackColor = barColor.copy(alpha = 0.2f)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { onAction(HeroAction.AddToShoppingList(item.item.id, null)) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.AddShoppingCart,
                                inkIconRes = R.drawable.ic_ink_add_to_cart,
                                contentDescription = "Add to shopping list",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        onNavigate(Screen.ItemDetail.createRoute(item.item.id))
                    }
                )
            }

            if (uiState.lowStockItems.size > 5) {
                TextButton(onClick = { onNavigate(Screen.LowStockReport.route) }) {
                    Text("View All (${uiState.lowStockItems.size})")
                }
            }

            // Batch add-to-shopping button
            if (uiState.lowStockItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                ThemedButton(
                    onClick = {
                        uiState.lowStockItems.take(5).forEach { item ->
                            onAction(HeroAction.AddToShoppingList(item.item.id, null))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.AddShoppingCart,
                        inkIconRes = R.drawable.ic_ink_add_to_cart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacingXs))
                    Text("Add all to shopping list")
                }
            }
        }
    }
}

// ─── SHOPPING — Ready to Shop ──────────────────────────────────────────

@Composable
private fun HeroShopping(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit
) {
    val shoppingColor = MaterialTheme.appColors.accentPurple
    val totalShopping = uiState.shoppingActive + uiState.shoppingPurchased
    val progressText = if (totalShopping > 0) "${uiState.shoppingPurchased}/$totalShopping done" else ""

    AppCard(
        onClick = { onNavigate(Screen.ShoppingList.route) },
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmpathyCartIcon(
                    itemCount = uiState.shoppingActive,
                    modifier = Modifier.size(Dimens.iconSizeMd),
                    tint = shoppingColor
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${uiState.shoppingActive} item${if (uiState.shoppingActive != 1) "s" else ""} on your shopping list",
                        style = MaterialTheme.typography.sectionHeader,
                        color = shoppingColor
                    )
                    if (progressText.isNotEmpty()) {
                        Text(
                            progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ThemedIcon(
                    materialIcon = Icons.Filled.ArrowForward,
                    inkIconRes = R.drawable.ic_ink_chevron_right,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = shoppingColor
                )
            }

            if (totalShopping > 0) {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                ThemedProgressBar(
                    progress = { uiState.shoppingPurchased.toFloat() / totalShopping },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = shoppingColor,
                    trackColor = shoppingColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

// ─── TOTAL_ITEMS — New User Nudge ──────────────────────────────────────

@Composable
private fun HeroTotalItems(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit
) {
    val nudgeColor = MaterialTheme.appColors.accentBlue
    val remaining = 20 - uiState.totalItems

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Inventory2,
                    inkIconRes = R.drawable.ic_ink_box,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSizeMd),
                    tint = nudgeColor
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                Text(
                    "${uiState.totalItems} item${if (uiState.totalItems != 1) "s" else ""} tracked — keep adding!",
                    style = MaterialTheme.typography.sectionHeader,
                    color = nudgeColor
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            if (remaining > 0) {
                Text(
                    "Add $remaining more to reach 20",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                ThemedProgressBar(
                    progress = { uiState.totalItems / 20f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = nudgeColor,
                    trackColor = nudgeColor.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                ThemedButton(onClick = { onNavigate(Screen.ItemForm.createRoute()) }) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.Add,
                        inkIconRes = R.drawable.ic_ink_add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
                ThemedButton(onClick = { onNavigate(Screen.KitchenMap.route) }) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.PhotoCamera,
                        inkIconRes = R.drawable.ic_ink_camera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Kitchen")
                }
            }
        }
    }
}

// ─── NONE — All Good (Healthy Kitchen) ─────────────────────────────────

@Composable
private fun HeroHealthy(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit
) {
    // Zero items — show get-started card instead of health score
    if (uiState.totalItems == 0) {
        HeroEmpty(uiState, onNavigate)
        return
    }

    val scoreColor = MaterialTheme.appColors.scoreToColor(uiState.homeScore)

    AppCard(
        onClick = { onNavigate(Screen.PantryHealth.route) },
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmpathyHeartIcon(
                    healthScore = uiState.homeScore,
                    modifier = Modifier.size(Dimens.iconSizeLg),
                    tint = scoreColor
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${uiState.homeScore}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacingXs))
                        Text(
                            uiState.homeScoreLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = scoreColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    ThemedProgressBar(
                        progress = { uiState.homeScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = scoreColor,
                        trackColor = scoreColor.copy(alpha = 0.2f)
                    )
                }
            }

            if (uiState.contextualInsight.isNotBlank()) {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Text(
                    uiState.contextualInsight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.homeScore >= 70) {
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                Text(
                    "Your kitchen is in great shape!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(
                "View details",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── EMPTY — Get Started (zero items) ──────────────────────────────────

@Composable
private fun HeroEmpty(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit
) {
    val heroSubtitle = when (uiState.userPreference) {
        "WASTE" -> "Start tracking expiry dates to reduce waste"
        "COOK" -> "Add ingredients to discover what you can cook"
        else -> "Scan your kitchen to see what you have"
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemedIcon(
                    materialIcon = Icons.Filled.PhotoCamera,
                    inkIconRes = R.drawable.ic_ink_camera,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.appColors.accentOrange
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm))
                Text(
                    "Map Your Kitchen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            Text(
                heroSubtitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(
                "Take photos of your fridge, pantry, and shelves. AI identifies every item and builds your inventory in minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                ThemedButton(onClick = { onNavigate(Screen.KitchenMap.route) }) {
                    Text("Start Kitchen Tour")
                    Spacer(modifier = Modifier.width(4.dp))
                    ThemedIcon(
                        materialIcon = Icons.Filled.ArrowForward,
                        inkIconRes = R.drawable.ic_ink_chevron_right,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                ThemedButton(onClick = { onNavigate(Screen.ItemForm.createRoute()) }) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.Add,
                        inkIconRes = R.drawable.ic_ink_add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }
        }
    }
}
