package com.inventory.app.ui.screens.reports

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ChartEntry
import com.inventory.app.ui.components.HorizontalBarChart
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.navigation.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryReportScreen(
    navController: NavController,
    viewModel: InventoryReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
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
                title = { Text("Full Inventory Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val filteredItems = remember(searchQuery, uiState.allItems) {
            if (searchQuery.isBlank()) uiState.allItems
            else uiState.allItems.filter {
                it.item.name.contains(searchQuery, ignoreCase = true) ||
                    it.category?.name?.contains(searchQuery, ignoreCase = true) == true ||
                    it.storageLocation?.name?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Section 1: Summary Cards (3 cards) ──
            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMiniCard(
                        label = "Total Items",
                        value = "${uiState.totalItems}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatMiniCard(
                        label = "Total Value",
                        value = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", uiState.totalValue)}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    StatMiniCard(
                        label = "Avg Value",
                        value = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", uiState.averageItemValue)}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            // ── Section 2: Items by Category (bar chart) ──
            if (uiState.itemsByCategory.isNotEmpty()) {
                item(key = "cat_header") {
                    SectionHeader("Items by Category")
                }
                item(key = "cat_chart") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        HorizontalBarChart(
                            entries = uiState.itemsByCategory.map {
                                ChartEntry(it.label, it.count.toFloat())
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // ── Section 3: Items by Location (bar chart) ──
            if (uiState.itemsByLocation.isNotEmpty()) {
                item(key = "loc_header") {
                    SectionHeader("Items by Location")
                }
                item(key = "loc_chart") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        HorizontalBarChart(
                            entries = uiState.itemsByLocation.map {
                                ChartEntry(it.label, it.count.toFloat())
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // ── Section 4: Value by Category (bar chart) ──
            if (uiState.valueByCategory.isNotEmpty()) {
                item(key = "val_cat_header") {
                    SectionHeader("Value by Category")
                }
                item(key = "val_cat_chart") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        HorizontalBarChart(
                            entries = uiState.valueByCategory.map {
                                ChartEntry(it.label, it.totalValue.toFloat())
                            },
                            modifier = Modifier.padding(16.dp),
                            valuePrefix = uiState.currencySymbol
                        )
                    }
                }
            }

            // ── Section 5: Top 5 Most Valuable Items ──
            if (uiState.topValueItems.isNotEmpty()) {
                item(key = "top_val_header") {
                    SectionHeader("Most Valuable Items")
                }
                item(key = "top_val_list") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            uiState.topValueItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate(Screen.ItemDetail.createRoute(item.id))
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", item.totalValue)}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 6: All Items (searchable list) ──
            if (uiState.allItems.isNotEmpty()) {
                item(key = "items_header") {
                    val countLabel = if (searchQuery.isNotBlank()) {
                        "${filteredItems.size} of ${uiState.totalItems}"
                    } else {
                        "${uiState.totalItems}"
                    }
                    SectionHeader("All Items ($countLabel)")
                }
                item(key = "search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search items...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    )
                }
                items(filteredItems, key = { it.item.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = {
                            Text(
                                buildString {
                                    item.category?.let { append(it.name) }
                                    append(" | Qty: ${item.item.quantity.formatQty()}")
                                    item.item.purchasePrice?.let {
                                        append(" | ${uiState.currencySymbol}${String.format(Locale.US, "%.2f", it)}")
                                    }
                                }
                            )
                        },
                        trailingContent = {
                            item.storageLocation?.let {
                                Text(it.name, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                        }
                    )
                }
            }

            // Bottom spacing
            item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    AppCard(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
