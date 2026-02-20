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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ChartEntry
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.components.DonutChart
import com.inventory.app.ui.navigation.Screen

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
        val filteredItems = remember(searchQuery, uiState.allItems) {
            if (searchQuery.isBlank()) uiState.allItems
            else uiState.allItems.filter { it.item.name.contains(searchQuery, ignoreCase = true) }
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${uiState.totalItems}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Total Items", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${uiState.currencySymbol}${String.format("%.2f", uiState.totalValue)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Total Value", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // By category breakdown
            if (uiState.itemsByCategory.isNotEmpty()) {
                item {
                    Text("By Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        DonutChart(
                            entries = uiState.itemsByCategory.map { ChartEntry(it.label, it.count.toFloat()) },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.itemsByCategory.forEach { data ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(data.label, style = MaterialTheme.typography.bodyMedium)
                                    Text("${data.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // By location breakdown
            if (uiState.itemsByLocation.isNotEmpty()) {
                item {
                    Text("By Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.itemsByLocation.forEach { data ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(data.label, style = MaterialTheme.typography.bodyMedium)
                                    Text("${data.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // All items list
            if (uiState.allItems.isNotEmpty()) {
                item {
                    Text("All Items (${uiState.totalItems})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
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
                                    item.item.purchasePrice?.let { append(" | ${uiState.currencySymbol}${String.format("%.2f", it)}") }
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
        }
    }
}
