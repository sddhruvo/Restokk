package com.inventory.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(navController: NavController, shoppingBadgeCount: Int = 0, expiringBadgeCount: Int = 0) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            val navRoute = when (item.screen) {
                is Screen.ItemList -> Screen.ItemList.baseRoute
                else -> item.screen.route
            }
            val routePrefix = item.screen.route.substringBefore('/')
                .substringBefore('?')
            val isSelected = when (item.screen) {
                is Screen.ItemList -> currentRoute?.startsWith("items") == true
                else -> currentRoute?.startsWith(routePrefix) == true
            }
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
                onClick = {
                    navController.navigate(navRoute) {
                        // Pop up to start destination using ID (reliable, Google-recommended)
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                            inclusive = false
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
