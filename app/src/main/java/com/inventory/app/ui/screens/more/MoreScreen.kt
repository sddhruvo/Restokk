package com.inventory.app.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.CardBlue
import com.inventory.app.ui.theme.CardGold
import com.inventory.app.ui.theme.CardGreen
import com.inventory.app.ui.theme.CardOrange
import com.inventory.app.ui.theme.CardPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("More") })
        }
    ) { padding ->
        // Only animate on first entry; skip on back-navigation to preserve scroll
        var hasAnimated by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!hasAnimated) {
                kotlinx.coroutines.delay(5 * 50L + 300L)
                hasAnimated = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search — standalone tappable row
            AnimateOnce(index = 0, hasAnimated = hasAnimated) {
                ListItem(
                    headlineContent = { Text("Search") },
                    supportingContent = { Text("Search items, categories, and locations") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.GlobalSearch.route)
                    }
                )
            }

            // AI & Kitchen section
            AnimateOnce(index = 1, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoreSectionHeader(
                        title = "AI & Kitchen",
                        leadingIcon = Icons.Filled.AutoAwesome,
                        color = CardOrange
                    )
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Cook", Icons.Filled.Restaurant, CardOrange, "What Can I Cook?") {
                                    navController.navigate(Screen.Cook.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Kitchen Scan", Icons.Filled.PhotoCamera, CardGreen) {
                                    navController.navigate(Screen.FridgeScan.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Kitchen Map", Icons.Filled.Kitchen, CardGold, "My Kitchen") {
                                    navController.navigate(Screen.KitchenMap.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "My Recipes", Icons.Filled.MenuBook, CardPurple) {
                                    navController.navigate(Screen.SavedRecipes.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Scan Receipt", Icons.Filled.Receipt, CardGreen) {
                                    navController.navigate(Screen.ReceiptScan.route)
                                }
                            }
                        )
                    )
                }
            }

            // Analytics section
            AnimateOnce(index = 2, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoreSectionHeader(title = "Analytics")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Reports", Icons.Filled.Assessment, CardBlue) {
                                    navController.navigate(Screen.Reports.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Purchases", Icons.Filled.ShoppingBag, CardOrange, "Purchase History") {
                                    navController.navigate(Screen.PurchaseHistory.createRoute())
                                }
                            }
                        )
                    )
                }
            }

            // Organize section
            AnimateOnce(index = 3, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoreSectionHeader(title = "Organize")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Categories", Icons.Filled.Category, CardBlue) {
                                    navController.navigate(Screen.CategoryList.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Locations", Icons.Filled.Place, CardGold, "Storage Locations") {
                                    navController.navigate(Screen.LocationList.route)
                                }
                            }
                        )
                    )
                }
            }

            // App section
            AnimateOnce(index = 4, hasAnimated = hasAnimated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoreSectionHeader(title = "App")
                    AdaptiveGrid(
                        columns = 2,
                        spacing = 12.dp,
                        items = listOf<@Composable (Modifier) -> Unit>(
                            { mod ->
                                MoreActionCard(mod, "Settings", Icons.Filled.Settings, MaterialTheme.colorScheme.primary) {
                                    navController.navigate(Screen.Settings.route)
                                }
                            },
                            { mod ->
                                MoreActionCard(mod, "Export / Import", Icons.Filled.ImportExport, MaterialTheme.colorScheme.primary) {
                                    navController.navigate(Screen.ExportImport.route)
                                }
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    leadingIcon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

@Composable
private fun MoreActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, modifier = modifier.height(88.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(28.dp), tint = iconTint)
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

@Composable
private fun AdaptiveGrid(
    columns: Int,
    spacing: Dp,
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
