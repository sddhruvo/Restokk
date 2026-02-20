package com.inventory.app.ui.screens.reports

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.StockYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockReportScreen(
    navController: NavController,
    viewModel: LowStockReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Low Stock Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState()
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${uiState.outOfStockCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Out of Stock", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${uiState.lowStockCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Low Stock", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Out of stock
            if (uiState.outOfStockItems.isNotEmpty()) {
                item {
                    Text("Out of Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpiryRed)
                }
                items(uiState.outOfStockItems, key = { "oos_${it.item.id}" }) { item ->
                    val effectiveMin = if (item.item.minQuantity > 0) item.item.minQuantity else item.item.smartMinQuantity
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = { Text("Target: ${effectiveMin.formatQty()}", color = ExpiryRed) },
                        trailingContent = {
                            IconButton(onClick = {
                                navController.navigate(Screen.AddShoppingItem.createRoute(item.item.id))
                            }) {
                                Icon(Icons.Filled.AddShoppingCart, contentDescription = "Add to shopping list")
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
                    Text("Low Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StockYellow)
                }
                items(uiState.lowStockItems, key = { "low_${it.item.id}" }) { item ->
                    val effectiveMin = if (item.item.minQuantity > 0) item.item.minQuantity else item.item.smartMinQuantity
                    val ratio = if (effectiveMin > 0) (item.item.quantity / effectiveMin).toFloat().coerceIn(0f, 1f) else 1f
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = {
                            Column {
                                Text("Qty: ${item.item.quantity.formatQty()} / ${effectiveMin.formatQty()}", color = StockYellow)
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    color = if (ratio < 0.3f) ExpiryRed else StockYellow
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                navController.navigate(Screen.AddShoppingItem.createRoute(item.item.id))
                            }) {
                                Icon(Icons.Filled.AddShoppingCart, contentDescription = "Add to shopping list")
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
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
