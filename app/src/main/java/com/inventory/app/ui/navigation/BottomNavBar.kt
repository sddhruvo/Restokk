package com.inventory.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(
    navController: NavController,
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
        AlertDialog(
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
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "fabRotation"
    )

    NavigationBar {
        val items = bottomNavItems
        // First two items (Home, Items)
        items.take(2).forEach { item ->
            NavBarItem(item, currentRoute, shoppingBadgeCount, expiringBadgeCount) {
                guardedAction {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }
        }

        // Center "Quick Add" — inline, filled circle with elevation
        NavigationBarItem(
            icon = {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = if (isQuickAddOpen) "Close menu" else "Quick add",
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer { rotationZ = fabRotation },
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            },
            label = { },
            selected = false,
            onClick = { guardedAction { onQuickAddToggle() } },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.surface
            )
        )

        // Last two items (Shopping, More)
        items.drop(2).forEach { item ->
            NavBarItem(item, currentRoute, shoppingBadgeCount, expiringBadgeCount) {
                guardedAction {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.NavBarItem(
    item: BottomNavItem,
    currentRoute: String?,
    shoppingBadgeCount: Int,
    expiringBadgeCount: Int,
    onNavigate: () -> Unit
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
    NavigationBarItem(
        icon = {
            val iconContent: @Composable () -> Unit = {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
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
        },
        label = { Text(item.label) },
        selected = isSelected,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = onNavigate
    )
}
