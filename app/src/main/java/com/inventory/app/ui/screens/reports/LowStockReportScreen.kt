package com.inventory.app.ui.screens.reports

import com.inventory.app.ui.components.ThemedSnackbarHost
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.inventory.app.ui.components.ThemedProgressBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.components.computeStockBar
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.sectionHeader
import com.inventory.app.ui.theme.statValue
import com.inventory.app.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockReportScreen(
    navController: NavController,
    viewModel: LowStockReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showShoppingSheet = com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    PageScaffold(
        onBack = { navController.popBackStack() },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        if (uiState.isLoading) {
            LoadingState()
            return@PageScaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.spacingLg),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            item { PageHeader("Low Stock Report") }
            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.appColors.statusExpired.copy(alpha = 0.12f)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                            Text("${uiState.outOfStockCount}", style = MaterialTheme.typography.statValue)
                            Text("Out of Stock", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.appColors.statusLowStock.copy(alpha = 0.12f)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                            Text("${uiState.lowStockCount}", style = MaterialTheme.typography.statValue)
                            Text("Low Stock", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Out of stock
            if (uiState.outOfStockItems.isNotEmpty()) {
                item {
                    Text("Out of Stock", style = MaterialTheme.typography.sectionHeader, color = MaterialTheme.appColors.statusExpired)
                }
                items(uiState.outOfStockItems, key = { "oos_${it.item.id}" }) { item ->
                    val stockState = computeStockBar(item.item.quantity, item.item.minQuantity, item.item.smartMinQuantity, item.item.maxQuantity, uiState.lowStockThreshold)
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = { Text("Target: ${stockState.ceiling.formatQty()}", color = MaterialTheme.appColors.statusExpired) },
                        trailingContent = {
                            IconButton(onClick = {
                                showShoppingSheet(item.item.id, null)
                            }) {
                                ThemedIcon(materialIcon = Icons.Filled.AddShoppingCart, inkIconRes = R.drawable.ic_ink_add_to_cart, contentDescription = "Add to shopping list")
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                        }
                    )
                }
            }

            // Low stock
            if (uiState.lowStockItems.isNotEmpty()) {
                item {
                    Text("Low Stock", style = MaterialTheme.typography.sectionHeader, color = MaterialTheme.appColors.statusLowStock)
                }
                items(uiState.lowStockItems, key = { "low_${it.item.id}" }) { item ->
                    val stockState = computeStockBar(item.item.quantity, item.item.minQuantity, item.item.smartMinQuantity, item.item.maxQuantity, uiState.lowStockThreshold)
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = {
                            Column {
                                val barColor = MaterialTheme.appColors.stockColor(stockState.ratio, stockState.threshold)
                                Text("Qty: ${item.item.quantity.formatQty()} / ${stockState.ceiling.formatQty()}", color = barColor)
                                ThemedProgressBar(
                                    progress = { stockState.ratio },
                                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingXs),
                                    color = barColor
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                showShoppingSheet(item.item.id, null)
                            }) {
                                ThemedIcon(materialIcon = Icons.Filled.AddShoppingCart, inkIconRes = R.drawable.ic_ink_add_to_cart, contentDescription = "Add to shopping list")
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                        }
                    )
                }
            }

            if (uiState.lowStockItems.isEmpty() && uiState.outOfStockItems.isEmpty() && !uiState.isLoading) {
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "All items are well stocked",
                            modifier = Modifier.padding(Dimens.spacingXl),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
