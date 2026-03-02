package com.inventory.app.ui.screens.dashboard

import com.inventory.app.ui.components.ThemedSnackbarHost
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import com.inventory.app.ui.components.ThemedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.inventory.app.ui.components.ThemedProgressBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.EmpathyCartIcon
import com.inventory.app.ui.components.EmpathyHeartIcon
import com.inventory.app.ui.components.EmpathyTrendingIcon
import com.inventory.app.ui.components.EmpathyWarningIcon
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.InkPersonality
import com.inventory.app.ui.components.inkBreathe
import com.inventory.app.ui.components.rememberAiSignInGate
import com.inventory.app.ui.components.EmptyState
import com.inventory.app.ui.components.ShimmerStatCard
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.theme.AppShapes
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.appColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val uiState by viewModel.uiState.collectAsState()
    val showShoppingSheet = com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val aiGate = rememberAiSignInGate()

    val gridColumns = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> 4
        WindowWidthSizeClass.Medium -> 3
        else -> 2
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Compact greeting text (inline, no DashboardGreeting composable)
    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val prefTagline = when (uiState.userPreference) {
        "WASTE" -> "Let's keep things fresh"
        "COOK" -> "Ready to cook something great?"
        else -> "Here's your kitchen at a glance"
    }
    val subtitle = when {
        uiState.totalItems == 0 -> "Start by adding your first item"
        uiState.expiringSoon > 0 -> "$prefTagline \u2014 ${uiState.expiringSoon} item${if (uiState.expiringSoon != 1) "s" else ""} expiring soon"
        else -> "$prefTagline \u2014 tracking ${uiState.totalItems} item${if (uiState.totalItems != 1) "s" else ""}"
    }

    ThemedScaffold(
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
    ) { padding ->
        // Only animate on first entry; skip on back-navigation to preserve scroll
        var hasAnimated by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!hasAnimated) {
                kotlinx.coroutines.delay(6 * 50L + 300L)
                hasAnimated = true
            }
        }

        val scrollState = rememberScrollState()
        val reduceMotion = com.inventory.app.ui.theme.LocalReduceMotion.current

        // Collapse progress: 0f (top) → 1f (fully collapsed after ~100dp scroll)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val collapseThresholdPx = with(density) { 100.dp.toPx() }
        val rawProgress = (scrollState.value / collapseThresholdPx).coerceIn(0f, 1f)
        val collapseProgress by animateFloatAsState(
            targetValue = rawProgress,
            animationSpec = if (reduceMotion) spring(stiffness = 10000f)
            else PaperInkMotion.GentleSpring,
            label = "collapseProgress"
        )

        // Derived values from collapse progress
        val greetingScale = 1f - (collapseProgress * 0.3f) // 1.0 → 0.7
        val subtitleAlpha = (1f - (collapseProgress * 2f)).coerceIn(0f, 1f) // fades out at 50%

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)
        ) {
            // Compact header: greeting + action icons
            AnimateOnce(index = 0, hasAnimated = hasAnimated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer {
                                scaleX = greetingScale
                                scaleY = greetingScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            }
                        )
                        if (subtitleAlpha > 0.01f) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Refresh,
                            inkIconRes = R.drawable.ic_ink_refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.GlobalSearch.route) }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.Search,
                                inkIconRes = R.drawable.ic_ink_search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Kitchen Story Card — persistent onboarding missions
            // rememberSaveable keeps card in composition during crumple exit animation
            var showStoryCard by rememberSaveable { mutableStateOf(false) }
            if (uiState.kitchenStory.isVisible && !showStoryCard) {
                showStoryCard = true
            }
            // Safety: if ViewModel already dismissed but flag is stale (e.g. re-navigation during crumple)
            if (!uiState.kitchenStory.isVisible && showStoryCard) {
                showStoryCard = false
            }
            if (showStoryCard) {
                KitchenStoryCard(
                    state = uiState.kitchenStory,
                    onMissionTap = { mission ->
                        val target = mission.navTarget
                        if (target != null) {
                            navController.navigate(target)
                        }
                    },
                    onDismiss = { /* dismiss persisted in onExitComplete */ },
                    onAllComplete = { /* dismiss persisted in onExitComplete */ },
                    onDismissSmartDefaults = { viewModel.dismissSmartDefaultsEducation() },
                    onShowMeSmartDefaults = {
                        scope.launch {
                            val itemId = viewModel.startSmartDefaultsTour()
                            if (itemId != null) {
                                navController.navigate(Screen.ItemForm.createRoute(itemId = itemId))
                            }
                        }
                    },
                    onExitComplete = {
                        showStoryCard = false
                        viewModel.dismissKitchenStory()
                    }
                )
            }

            // Hero card / insight chip — contextual insight based on state + user preference
            // Determine active insight type for stat card highlight
            val activeInsightType: InsightType? = if (!uiState.isLoading && uiState.totalItems > 0) {
                data class InsightEntry(val type: InsightType, val available: Boolean)
                val entries = when (uiState.userPreference) {
                    "WASTE" -> listOf(
                        InsightEntry(InsightType.EXPIRING, uiState.expiringSoon > 0),
                        InsightEntry(InsightType.SHOPPING, uiState.shoppingActive > 0),
                        InsightEntry(InsightType.RECIPES, uiState.savedRecipeCount > 0)
                    )
                    "COOK" -> listOf(
                        InsightEntry(InsightType.RECIPES, uiState.savedRecipeCount > 0),
                        InsightEntry(InsightType.EXPIRING, uiState.expiringSoon > 0),
                        InsightEntry(InsightType.SHOPPING, uiState.shoppingActive > 0)
                    )
                    else -> listOf(
                        InsightEntry(InsightType.INVENTORY, true),
                        InsightEntry(InsightType.EXPIRING, uiState.expiringSoon > 0),
                        InsightEntry(InsightType.SHOPPING, uiState.shoppingActive > 0),
                        InsightEntry(InsightType.RECIPES, uiState.savedRecipeCount > 0)
                    )
                }
                entries.firstOrNull { it.available }?.type
            } else null

            if (!uiState.isLoading) {
                if (uiState.totalItems == 0) {
                    val heroSubtitle = when (uiState.userPreference) {
                        "WASTE" -> "Start tracking expiry dates to reduce waste"
                        "COOK" -> "Add ingredients to discover what you can cook"
                        else -> "Scan your kitchen to see what you have"
                    }
                    KitchenScanHeroCard(subtitle = heroSubtitle) { navController.navigate(Screen.KitchenMap.route) }
                } else {
                    // Compact insight chip — same priority logic, compact visual
                    when (activeInsightType) {
                        InsightType.EXPIRING -> CompactInsightChip(
                            icon = Icons.Filled.Timer, inkIconRes = R.drawable.ic_ink_clock,
                            iconTint = MaterialTheme.appColors.accentOrange,
                            text = "${uiState.expiringSoon} item${if (uiState.expiringSoon != 1) "s" else ""} expiring soon",
                            onClick = { navController.navigate(Screen.ExpiringReport.route) }
                        )
                        InsightType.SHOPPING -> CompactInsightChip(
                            icon = Icons.Filled.ShoppingCart, inkIconRes = R.drawable.ic_ink_shopping,
                            iconTint = MaterialTheme.appColors.accentPurple,
                            text = "${uiState.shoppingActive} item${if (uiState.shoppingActive != 1) "s" else ""} to buy",
                            onClick = { navController.navigate(Screen.ShoppingList.route) }
                        )
                        InsightType.RECIPES -> CompactInsightChip(
                            icon = Icons.Filled.MenuBook, inkIconRes = R.drawable.ic_ink_book,
                            iconTint = MaterialTheme.appColors.accentGreen,
                            text = "${uiState.savedRecipeCount} saved recipe${if (uiState.savedRecipeCount != 1) "s" else ""} \u2014 time to cook?",
                            onClick = { navController.navigate(Screen.Cook.route) }
                        )
                        InsightType.INVENTORY -> CompactInsightChip(
                            icon = Icons.Filled.Inventory2, inkIconRes = R.drawable.ic_ink_box,
                            iconTint = MaterialTheme.appColors.accentBlue,
                            text = "${uiState.totalItems} item${if (uiState.totalItems != 1) "s" else ""} in your kitchen",
                            onClick = { navController.navigate(Screen.ItemList.createRoute()) }
                        )
                        null -> { /* no insight available */ }
                    }
                }
            }

            // Stats cards
            AnimateOnce(index = 1, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)) {
                    if (uiState.isLoading) {
                        AdaptiveGrid(
                            columns = gridColumns,
                            spacing = 12.dp,
                            items = listOf<@Composable (Modifier) -> Unit>(
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) },
                                { mod -> ShimmerStatCard(modifier = mod) }
                            )
                        )
                    } else {
                        AdaptiveGrid(
                            columns = gridColumns,
                            spacing = 12.dp,
                            items = listOf<@Composable (Modifier) -> Unit>(
                                { mod -> HealthScoreCard(mod, uiState.homeScore, uiState.homeScoreLabel) { navController.navigate(Screen.PantryHealth.route) } },
                                { mod -> StatCard(mod, "Total Items", "${uiState.totalItems}", Icons.Filled.Inventory2, MaterialTheme.appColors.accentBlue, inkIconRes = R.drawable.ic_ink_box, isHighlighted = activeInsightType == InsightType.INVENTORY) { navController.navigate(Screen.ItemList.createRoute()) } },
                                { mod -> StatCard(mod, "Expiring Soon", "${uiState.expiringSoon}", Icons.Filled.Warning, MaterialTheme.appColors.accentOrange, inkIconRes = R.drawable.ic_ink_warning, iconContent = { EmpathyWarningIcon(expiringCount = uiState.expiringSoon, modifier = Modifier.size(Dimens.iconSizeMd), tint = MaterialTheme.appColors.accentOrange) }, isHighlighted = activeInsightType == InsightType.EXPIRING) { navController.navigate(Screen.ExpiringReport.route) } },
                                { mod -> StatCard(mod, "Low Stock", "${uiState.lowStock}", Icons.Filled.TrendingDown, MaterialTheme.appColors.accentGreen, inkIconRes = R.drawable.ic_ink_trending_down, iconContent = { EmpathyTrendingIcon(lowStockCount = uiState.lowStock, modifier = Modifier.size(Dimens.iconSizeMd), tint = MaterialTheme.appColors.accentGreen) }) { navController.navigate(Screen.LowStockReport.route) } },
                                { mod -> StatCard(mod, "Total Value", "${uiState.currencySymbol}${String.format("%.2f", uiState.totalValue)}", Icons.Filled.AttachMoney, MaterialTheme.appColors.accentGreen) { navController.navigate(Screen.InventoryReport.route) } }
                            )
                        )
                    }
                }
            }

            // Quick actions
            AnimateOnce(index = 2, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)) {
                    Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    AdaptiveGrid(
                    columns = gridColumns,
                    spacing = 12.dp,
                    items = listOf<@Composable (Modifier) -> Unit>(
                        { mod -> QuickActionCard(mod, "Cook", Icons.Filled.Restaurant, MaterialTheme.appColors.accentOrange,
                            subtitle = if (uiState.savedRecipeCount > 0) "${uiState.savedRecipeCount} saved" else null,
                            inkIconRes = R.drawable.ic_ink_cook,
                            breathePersonality = InkPersonality.SIMMER
                        ) { navController.navigate(Screen.Cook.route) } },
                        { mod -> QuickActionCard(mod, "Kitchen", Icons.Filled.Kitchen, MaterialTheme.appColors.accentGold,
                            subtitle = if (uiState.lastScanItemCount > 0) "${uiState.lastScanItemCount} mapped" else null,
                            inkIconRes = R.drawable.ic_ink_kitchen,
                            breathePersonality = InkPersonality.SETTLE
                        ) { navController.navigate(Screen.KitchenMap.route) } },
                        { mod -> QuickActionCard(mod, "Reports", Icons.Filled.Assessment, MaterialTheme.appColors.accentBlue, inkIconRes = R.drawable.ic_ink_reports) { navController.navigate(Screen.Reports.route) } },
                        { mod -> QuickActionCard(mod, "Scan", Icons.Filled.QrCodeScanner, MaterialTheme.appColors.accentGold, inkIconRes = R.drawable.ic_ink_barcode) { navController.navigate(Screen.BarcodeScan.route) } },
                        { mod -> QuickActionCard(mod, "Receipt", Icons.Filled.Receipt, MaterialTheme.appColors.accentGreen, inkIconRes = R.drawable.ic_ink_receipt) { aiGate.requireSignIn("parse receipts") { navController.navigate(Screen.ReceiptScan.route) } } },
                        { mod ->
                            val totalShopping = uiState.shoppingActive + uiState.shoppingPurchased
                            QuickActionCard(mod, "Shopping", Icons.Filled.ShoppingCart, MaterialTheme.appColors.accentPurple,
                                subtitle = if (totalShopping > 0) "${uiState.shoppingPurchased}/$totalShopping done" else null,
                                inkIconRes = R.drawable.ic_ink_shopping,
                                iconContent = { EmpathyCartIcon(itemCount = uiState.shoppingActive, modifier = Modifier.size(Dimens.iconSizeLg), tint = MaterialTheme.appColors.accentPurple) }
                            ) { navController.navigate(Screen.ShoppingList.route) }
                        }
                    )
                )
                }
            }

            // Expiring soon list
            if (uiState.expiringItems.isNotEmpty()) {
            AnimateOnce(index = 3, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Expiring Soon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.expiringItems.size > 5) {
                                TextButton(onClick = { navController.navigate(Screen.ExpiringReport.route) }) {
                                    Text("View All (${uiState.expiringItems.size})")
                                }
                            }
                            IconButton(onClick = { navController.navigate(Screen.ItemForm.createRoute()) }) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.Add,
                                        inkIconRes = R.drawable.ic_ink_add,
                                        contentDescription = "Add item",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    run {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
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
                                            color = color
                                        )
                                    },
                                    trailingContent = {
                                        Row {
                                            IconButton(
                                                onClick = { showShoppingSheet(item.item.id, null) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                ThemedIcon(
                                                    materialIcon = Icons.Filled.AddShoppingCart,
                                                    inkIconRes = R.drawable.ic_ink_add_to_cart,
                                                    contentDescription = "Add ${item.item.name} to shopping list",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.pauseItem(item.item.id)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "${item.item.name} paused",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.unpauseItem(item.item.id)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                ThemedIcon(
                                                    materialIcon = Icons.Filled.PauseCircleOutline,
                                                    inkIconRes = R.drawable.ic_ink_pause,
                                                    contentDescription = "Pause alerts for ${item.item.name}",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }

            // Low stock list
            if (uiState.lowStockItems.isNotEmpty()) {
            AnimateOnce(index = 4, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Low Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (uiState.lowStockItems.size > 5) {
                            TextButton(onClick = { navController.navigate(Screen.LowStockReport.route) }) {
                                Text("View All (${uiState.lowStockItems.size})")
                            }
                        }
                    }

                    run {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            uiState.lowStockItems.take(5).forEach { item ->
                                val effectiveMin = if (item.item.minQuantity > 0) item.item.minQuantity else item.item.smartMinQuantity
                                val ratio = if (effectiveMin > 0) {
                                    (item.item.quantity / effectiveMin).toFloat().coerceIn(0f, 1f)
                                } else 1f
                                val barColor = if (ratio < 0.3f) MaterialTheme.appColors.statusExpired else MaterialTheme.appColors.statusLowStock
                                ListItem(
                                    headlineContent = { Text(item.item.name) },
                                    supportingContent = {
                                        Column {
                                            Text(
                                                "Qty: ${item.item.quantity.formatQty()} / ${effectiveMin.formatQty()}",
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
                                        Row {
                                            IconButton(
                                                onClick = { showShoppingSheet(item.item.id, null) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                ThemedIcon(
                                                    materialIcon = Icons.Filled.AddShoppingCart,
                                                    inkIconRes = R.drawable.ic_ink_add_to_cart,
                                                    contentDescription = "Add ${item.item.name} to shopping list",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.pauseItem(item.item.id)
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "${item.item.name} paused",
                                                            actionLabel = "Undo",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.unpauseItem(item.item.id)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                ThemedIcon(
                                                    materialIcon = Icons.Filled.PauseCircleOutline,
                                                    inkIconRes = R.drawable.ic_ink_pause,
                                                    contentDescription = "Pause alerts for ${item.item.name}",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }

            // Items by category summary
            if (uiState.itemsByCategory.isNotEmpty()) {
                AnimateOnce(index = 5, hasAnimated = hasAnimated) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text("Items by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val maxCategoryCount = uiState.itemsByCategory.maxOfOrNull { it.count } ?: 1
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.itemsByCategory.forEach { data ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate(Screen.ItemList.createRoute(categoryId = data.id))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(data.label, style = MaterialTheme.typography.bodyMedium)
                                            Text("${data.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        val fraction = if (maxCategoryCount > 0) data.count.toFloat() / maxCategoryCount else 0f
                                        ThemedProgressBar(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Items by location summary
            if (uiState.itemsByLocation.isNotEmpty()) {
                AnimateOnce(index = 6, hasAnimated = hasAnimated) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text("Items by Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val maxLocationCount = uiState.itemsByLocation.maxOfOrNull { it.count } ?: 1
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.spacingLg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.itemsByLocation.forEach { data ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate(Screen.LocationDetail.createRoute(data.id))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(data.label, style = MaterialTheme.typography.bodyMedium)
                                            Text("${data.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                        val fraction = if (maxLocationCount > 0) data.count.toFloat() / maxLocationCount else 0f
                                        ThemedProgressBar(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Adaptive Grid ──────────────────────────────────────────────────────

@Composable
private fun AdaptiveGrid(
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    items: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { child ->
                    child(Modifier.weight(1f))
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── Insight Type (for stat card highlight) ─────────────────────────────

private enum class InsightType { EXPIRING, SHOPPING, RECIPES, INVENTORY }

// ─── Stat Card ──────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    inkIconRes: Int = 0,
    iconContent: (@Composable () -> Unit)? = null,
    isHighlighted: Boolean = false,
    onClick: () -> Unit = {}
) {
    val reduceMotion = com.inventory.app.ui.theme.LocalReduceMotion.current

    // Pulse glow behind card when highlighted
    val glowAlpha = if (isHighlighted && !reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "statPulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        alpha
    } else if (isHighlighted) {
        0.15f // static subtle glow for reduce-motion
    } else {
        0f
    }

    val highlightMod = if (glowAlpha > 0f) {
        modifier.drawBehind {
            drawRoundRect(
                color = iconTint.copy(alpha = glowAlpha),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
            )
        }
    } else modifier

    AppCard(onClick = onClick, modifier = highlightMod, shape = AppShapes.large) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
            if (iconContent != null) {
                iconContent()
            } else {
                ThemedIcon(materialIcon = icon, inkIconRes = inkIconRes, contentDescription = title, modifier = Modifier.size(Dimens.iconSizeMd).inkBreathe(), tint = iconTint)
            }
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Pantry Health Score Card ───────────────────────────────────────────

@Composable
private fun HealthScoreCard(
    modifier: Modifier = Modifier,
    score: Int,
    label: String,
    onClick: () -> Unit = {}
) {
    val scoreColor = MaterialTheme.appColors.scoreToColor(score)
    AppCard(onClick = onClick, modifier = modifier, shape = AppShapes.large) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
            EmpathyHeartIcon(healthScore = score, modifier = Modifier.size(Dimens.iconSizeMd), tint = scoreColor)
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = scoreColor)
            ThemedProgressBar(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("Home $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tap for details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// ─── Kitchen Scan Hero Card (empty inventory) ──────────────────────────

@Composable
private fun KitchenScanHeroCard(
    subtitle: String = "Scan your kitchen to see what you have",
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemedIcon(
                    materialIcon = Icons.Filled.PhotoCamera,
                    inkIconRes = R.drawable.ic_ink_camera,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.appColors.accentOrange
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    "Map Your Kitchen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(
                "Take photos of your fridge, pantry, and shelves. AI identifies every item and builds your inventory in minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spacingMd))
            ThemedButton(onClick = onClick) {
                Text("Start Kitchen Tour")
                Spacer(modifier = Modifier.size(4.dp))
                ThemedIcon(
                    materialIcon = Icons.Filled.ArrowForward,
                    inkIconRes = R.drawable.ic_ink_chevron_right,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Compact Insight Chip (replaces full-height InsightCard) ─────────────

@Composable
private fun CompactInsightChip(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    inkIconRes: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = iconTint.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            ThemedIcon(
                materialIcon = icon,
                inkIconRes = inkIconRes,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ThemedIcon(
                materialIcon = Icons.Filled.ArrowForward,
                inkIconRes = R.drawable.ic_ink_chevron_right,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = iconTint
            )
        }
    }
}

// ─── Quick Action Card (with scale-on-tap) ──────────────────────────────

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    inkIconRes: Int = 0,
    iconContent: (@Composable () -> Unit)? = null,
    breathePersonality: InkPersonality = InkPersonality.BREATHE,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = PaperInkMotion.BouncySpring,
        label = "quickActionScale"
    )
    AppCard(
        onClick = onClick,
        modifier = modifier
            .height(88.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = AppShapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spacingMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconContent != null) {
                iconContent()
            } else {
                ThemedIcon(materialIcon = icon, inkIconRes = inkIconRes, contentDescription = title, modifier = Modifier.size(Dimens.iconSizeLg).inkBreathe(breathePersonality), tint = iconTint)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── AnimateOnce (skip animation on back-navigation) ────────────────────

@Composable
private fun AnimateOnce(
    index: Int,
    hasAnimated: Boolean,
    content: @Composable () -> Unit
) {
    if (hasAnimated) {
        content()
    } else {
        StaggeredAnimatedItem(index = index, slideOffsetDivisor = 6) {
            content()
        }
    }
}
