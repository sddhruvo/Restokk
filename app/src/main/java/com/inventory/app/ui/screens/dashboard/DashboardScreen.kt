package com.inventory.app.ui.screens.dashboard

import com.inventory.app.ui.components.ThemedSnackbarHost
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.luminance
import com.inventory.app.domain.model.UrgencyLevel
import com.inventory.app.domain.model.UrgencyResult
import com.inventory.app.domain.model.UrgencyTarget
import com.inventory.app.ui.theme.AppTheme
import com.inventory.app.ui.theme.sectionHeader
import com.inventory.app.ui.theme.VisualStyle
import com.inventory.app.ui.theme.previewColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import com.inventory.app.ui.components.NotificationBadge
import com.inventory.app.ui.components.NotificationDrawer
import com.inventory.app.ui.components.EmpathyCartIcon
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.InkPersonality
import com.inventory.app.ui.components.inkBreathe
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.AppShapes
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.appColors
import kotlinx.coroutines.launch

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
    var showNotificationDrawer by remember { mutableStateOf(false) }
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
        uiState.contextualInsight.isNotBlank() -> uiState.contextualInsight
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
                    NotificationBadge(
                        unreadCount = uiState.unreadNotificationCount,
                        onClick = { showNotificationDrawer = true }
                    )
                    // Theme picker
                    var showThemePicker by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showThemePicker = true }) {
                            ThemedIcon(
                                materialIcon = Icons.Filled.Palette,
                                inkIconRes = 0,
                                contentDescription = "Theme",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showThemePicker,
                            onDismissRequest = { showThemePicker = false }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Color palette row
                                Text(
                                    "Color",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AppTheme.entries.forEach { theme ->
                                        val isSelected = uiState.appTheme == theme
                                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(theme.previewColor)
                                                .border(
                                                    if (isSelected) 2.5.dp else 1.dp,
                                                    borderColor,
                                                    CircleShape
                                                )
                                                .clickable { viewModel.setAppTheme(theme) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                val checkTint = if (theme.previewColor.luminance() > 0.5f)
                                                    Color.Black else Color.White
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = checkTint,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                // Visual style row
                                Text(
                                    "Style",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VisualStyle.entries.forEach { style ->
                                        val isSelected = uiState.visualStyle == style
                                        val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        val border = if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = bg,
                                            modifier = Modifier
                                                .border(
                                                    if (isSelected) 1.5.dp else 0.dp,
                                                    border,
                                                    MaterialTheme.shapes.small
                                                )
                                                .clickable { viewModel.setVisualStyle(style) }
                                        ) {
                                            Text(
                                                style.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = textColor,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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

            // Hero Zone — urgency-driven priority card
            AnimateOnce(index = 1, hasAnimated = hasAnimated) {
                HeroZone(
                    urgencyResult = if (!uiState.isLoading) uiState.urgencyResult
                        else UrgencyResult(UrgencyTarget.NONE, 0, UrgencyLevel.NONE),
                    uiState = uiState,
                    onNavigate = { route -> navController.navigate(route) },
                    onAction = { action ->
                        when (action) {
                            is HeroAction.AddToShoppingList -> showShoppingSheet(action.itemId, action.categoryId)
                            is HeroAction.PauseItem -> {
                                viewModel.pauseItem(action.itemId)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Paused",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.unpauseItem(action.itemId)
                                    }
                                }
                            }
                            is HeroAction.TossItem -> {
                                viewModel.tossItem(action.itemId)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Item removed",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            is HeroAction.MarkStillGood -> {
                                viewModel.markStillGood(action.itemId)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Expiry extended",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            is HeroAction.NavigateTo -> navController.navigate(action.route)
                        }
                    }
                )
            }

            // Secondary Chips — compact stats for non-hero items
            if (!uiState.isLoading) {
                AnimateOnce(index = 2, hasAnimated = hasAnimated) {
                    SecondaryChips(
                        heroTarget = uiState.urgencyResult.target,
                        uiState = uiState,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
            }

            // Cook Card — context-adaptive
            AnimateOnce(index = 3, hasAnimated = hasAnimated) {
                CookCard(
                    recipeCount = uiState.savedRecipeCount,
                    totalItems = uiState.totalItems,
                    expiringItems = uiState.expiringItems,
                    isOffline = false,
                    manualRecipeCount = uiState.manualRecipeCount,
                    lastCookedName = uiState.lastCookedName,
                    lastCookedDaysAgo = uiState.lastCookedDaysAgo,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // Quick Actions — slimmed to 3
            AnimateOnce(index = 4, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)) {
                    Text("Quick Actions", style = MaterialTheme.typography.sectionHeader)
                    AdaptiveGrid(
                        columns = gridColumns,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                val totalShopping = uiState.shoppingActive + uiState.shoppingPurchased
                                QuickActionCard(mod, "Shopping", Icons.Filled.ShoppingCart, MaterialTheme.appColors.accentPurple,
                                    subtitle = if (totalShopping > 0) "${uiState.shoppingPurchased}/$totalShopping done" else null,
                                    inkIconRes = R.drawable.ic_ink_shopping,
                                    iconContent = { EmpathyCartIcon(itemCount = uiState.shoppingActive, modifier = Modifier.size(Dimens.iconSizeLg), tint = MaterialTheme.appColors.accentPurple) }
                                ) { navController.navigate(Screen.ShoppingList.route) }
                            },
                            { mod -> QuickActionCard(mod, "Reports", Icons.Filled.Assessment, MaterialTheme.appColors.accentBlue, inkIconRes = R.drawable.ic_ink_reports) { navController.navigate(Screen.Reports.route) } },
                            { mod -> QuickActionCard(mod, "Kitchen", Icons.Filled.Kitchen, MaterialTheme.appColors.accentGold,
                                subtitle = if (uiState.lastScanItemCount > 0) "${uiState.lastScanItemCount} mapped" else null,
                                inkIconRes = R.drawable.ic_ink_kitchen,
                                breathePersonality = InkPersonality.SETTLE
                            ) { navController.navigate(Screen.KitchenMap.route) } }
                        )
                    )
                }
            }

        }
    }

    // Notification Drawer
    if (showNotificationDrawer) {
        LaunchedEffect(Unit) { viewModel.onNotificationDrawerOpened() }
        NotificationDrawer(
            notifications = uiState.notifications,
            onDismissSheet = { showNotificationDrawer = false },
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onNotificationTap = { notification ->
                viewModel.onNotificationTapped(notification.id, notification.type)
                notification.deepLinkRoute?.let { route ->
                    try { navController.navigate(route) } catch (_: Exception) { }
                }
                showNotificationDrawer = false
            },
            onNotificationCtaClick = { notification ->
                viewModel.onNotificationCtaClicked(notification.id, notification.type)
                notification.ctaRoute?.let { route ->
                    try { navController.navigate(route) } catch (_: Exception) { }
                }
                showNotificationDrawer = false
            },
            onNotificationDismiss = { notification ->
                viewModel.dismissNotification(notification.id, notification.type)
            }
        )
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
