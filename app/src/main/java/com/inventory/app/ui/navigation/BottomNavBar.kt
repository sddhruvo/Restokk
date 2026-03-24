package com.inventory.app.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.inventory.app.ui.components.ThemedAlertDialog
import com.inventory.app.R
import com.inventory.app.ui.components.ThemedIcon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.inventory.app.ui.components.InkBorderCard
import com.inventory.app.ui.components.buildWobbleCirclePath
import com.inventory.app.ui.components.inkBreathe
import com.inventory.app.ui.theme.AppShapeTokens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.visuals

/** Height of the floating bottom nav bar — screens read this to reserve bottom space. */
val LocalBottomNavHeight = compositionLocalOf { 0.dp }

/** How far the nav bar has slid off-screen (0f = fully visible, 1f = fully hidden). */
val LocalBottomNavSlide = compositionLocalOf { 0f }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    shoppingBadgeCount: Int = 0,
    expiringBadgeCount: Int = 0,
    isQuickAddOpen: Boolean = false,
    onQuickAddToggle: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val navigationGuard = LocalNavigationGuard.current

    // Guard dialog state
    var showGuardDialog by remember { mutableStateOf(false) }
    var guardMessage by remember { mutableStateOf("") }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Discard Changes dialog
    if (showGuardDialog) {
        ThemedAlertDialog(
            onDismissRequest = {
                showGuardDialog = false
                pendingNavAction = null
            },
            title = { Text("Discard Changes?") },
            text = { Text(guardMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showGuardDialog = false
                    pendingNavAction?.invoke()
                    pendingNavAction = null
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGuardDialog = false
                    pendingNavAction = null
                }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Helper: attempt a navigation action, checking the guard first
    fun guardedAction(action: () -> Unit) {
        val blocked = navigationGuard.shouldBlock()
        if (blocked != null) {
            guardMessage = blocked
            pendingNavAction = action
            showGuardDialog = true
        } else {
            action()
        }
    }

    // + rotates to x when open
    val fabRotation by animateFloatAsState(
        targetValue = if (isQuickAddOpen) 45f else 0f,
        animationSpec = PaperInkMotion.BouncySpring,
        label = "fabRotation"
    )

    val isPaperInk = !MaterialTheme.visuals.useElevation
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // 65% on compact, 50% on wider screens
    val pillWidthFraction = if (screenWidth > 600.dp) 0.50f else 0.65f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The pill container
        val pillShape = RoundedCornerShape(AppShapeTokens.CornerPill)
        val items = bottomNavItems

        if (isPaperInk) {
            // Paper & Ink mode: InkBorderCard wrapper
            InkBorderCard(
                modifier = Modifier.fillMaxWidth(pillWidthFraction),
                containerColor = MaterialTheme.colorScheme.surface,
                cornerRadius = AppShapeTokens.CornerPill
            ) {
                PillContent(
                    items = items,
                    currentRoute = currentRoute,
                    shoppingBadgeCount = shoppingBadgeCount,
                    expiringBadgeCount = expiringBadgeCount,
                    fabRotation = fabRotation,
                    isQuickAddOpen = isQuickAddOpen,
                    isPaperInk = true,
                    onNavItem = { item ->
                        guardedAction {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onQuickAdd = { guardedAction { onQuickAddToggle() } }
                )
            }
        } else {
            // Modern mode: Surface with elevation
            Surface(
                modifier = Modifier.fillMaxWidth(pillWidthFraction),
                shape = pillShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 6.dp,
                tonalElevation = 2.dp
            ) {
                PillContent(
                    items = items,
                    currentRoute = currentRoute,
                    shoppingBadgeCount = shoppingBadgeCount,
                    expiringBadgeCount = expiringBadgeCount,
                    fabRotation = fabRotation,
                    isQuickAddOpen = isQuickAddOpen,
                    isPaperInk = false,
                    onNavItem = { item ->
                        guardedAction {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onQuickAdd = { guardedAction { onQuickAddToggle() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillContent(
    items: List<BottomNavItem>,
    currentRoute: String?,
    shoppingBadgeCount: Int,
    expiringBadgeCount: Int,
    fabRotation: Float,
    isQuickAddOpen: Boolean,
    isPaperInk: Boolean,
    onNavItem: (BottomNavItem) -> Unit,
    onQuickAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First two items (Home, Cook)
        items.take(2).forEach { item ->
            PillNavItem(
                item = item,
                currentRoute = currentRoute,
                shoppingBadgeCount = shoppingBadgeCount,
                expiringBadgeCount = expiringBadgeCount,
                onClick = { onNavItem(item) }
            )
        }

        // Center FAB — Ink Well (Paper & Ink) or standard (Modern)
        Box(
            modifier = Modifier
                .testTag(com.inventory.app.ui.TestTags.BottomNav.FAB_QUICK_ADD)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onQuickAdd() },
            contentAlignment = Alignment.Center
        ) {
            if (isPaperInk) {
                InkWellFab(fabRotation, isQuickAddOpen)
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Add,
                            inkIconRes = R.drawable.ic_ink_add,
                            contentDescription = if (isQuickAddOpen) "Close menu" else "Quick add",
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = fabRotation },
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Last two items (Shopping, More)
        items.drop(2).forEach { item ->
            PillNavItem(
                item = item,
                currentRoute = currentRoute,
                shoppingBadgeCount = shoppingBadgeCount,
                expiringBadgeCount = expiringBadgeCount,
                onClick = { onNavItem(item) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillNavItem(
    item: BottomNavItem,
    currentRoute: String?,
    shoppingBadgeCount: Int,
    expiringBadgeCount: Int,
    onClick: () -> Unit
) {
    val routePrefix = item.screen.route.substringBefore('/')
        .substringBefore('?')
    val isSelected = currentRoute?.startsWith(routePrefix) == true
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navIconScale"
    )
    val isShoppingTab = item.screen is Screen.ShoppingList
    val isDashboardTab = item.screen is Screen.Dashboard

    // Ink dot indicator width (animated)
    val dotWidth by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "dotWidth"
    )

    val tint = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    val testTag = when (item.screen) {
        is Screen.Dashboard -> com.inventory.app.ui.TestTags.BottomNav.TAB_HOME
        is Screen.CookHub -> com.inventory.app.ui.TestTags.BottomNav.TAB_COOK
        is Screen.ShoppingList -> com.inventory.app.ui.TestTags.BottomNav.TAB_SHOPPING
        is Screen.More -> com.inventory.app.ui.TestTags.BottomNav.TAB_MORE
        else -> "bottomNav.tab.unknown"
    }

    Column(
        modifier = Modifier
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconContent: @Composable () -> Unit = {
            ThemedIcon(
                materialIcon = item.icon,
                inkIconRes = item.inkIcon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(24.dp)
                    .inkBreathe(item.breathePersonality)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = tint
            )
        }

        if (isShoppingTab && shoppingBadgeCount > 0) {
            BadgedBox(badge = {
                Badge { Text("$shoppingBadgeCount") }
            }) {
                iconContent()
            }
        } else if (isDashboardTab && expiringBadgeCount > 0) {
            BadgedBox(badge = {
                Badge { Text("$expiringBadgeCount") }
            }) {
                iconContent()
            }
        } else {
            iconContent()
        }

        // Ink dot indicator below selected icon
        if (dotWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(dotWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Ink Well FAB — an organic wobble-circle filled with primary color,
 * with a soft bleed edge and gentle breathing animation.
 * Replaces the standard circular Surface FAB in Paper & Ink mode.
 */
@Composable
private fun InkWellFab(
    fabRotation: Float,
    isQuickAddOpen: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val wellSeed = remember { (Math.random() * 1000).toFloat() }
    val density = LocalDensity.current

    // Ink ripple breathing — slower than icon breathing
    val infiniteTransition = rememberInfiniteTransition(label = "inkWell")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wellBreathe"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = breatheScale
                scaleY = breatheScale
            }
            .drawBehind {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val wobbleAmp = with(density) { 1.5f.dp.toPx() }
                val bleedWidth = with(density) { 5.dp.toPx() }
                val radius = (size.minDimension / 2f) - wobbleAmp

                val wellPath = buildWobbleCirclePath(
                    centerX = cx,
                    centerY = cy,
                    radius = radius,
                    wobbleAmplitude = wobbleAmp,
                    wobbleSeed = wellSeed
                )

                // Layer 1: Bleed — ink seeping into paper
                drawPath(
                    path = wellPath,
                    color = primaryColor.copy(alpha = 0.25f),
                    style = Stroke(
                        width = bleedWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Layer 2: Fill — the ink pool
                drawPath(
                    path = wellPath,
                    color = primaryColor
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ThemedIcon(
            materialIcon = Icons.Filled.Add,
            inkIconRes = R.drawable.ic_ink_add,
            contentDescription = if (isQuickAddOpen) "Close menu" else "Quick add",
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = fabRotation },
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
