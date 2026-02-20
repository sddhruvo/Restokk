package com.inventory.app.ui.screens.reports

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.inventory.app.domain.model.UsageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageReportScreen(
    navController: NavController,
    viewModel: UsageReportViewModel = hiltViewModel()
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
                title = { Text("Usage Report") },
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
            // Period chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpendingPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = uiState.selectedPeriod == period,
                            onClick = { viewModel.updatePeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            // Total usage
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Total Usage", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${uiState.totalUsage.formatQty()} items",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Usage by type
            if (uiState.usageByType.isNotEmpty()) {
                item {
                    Text("By Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                val totalByType = uiState.usageByType.sumOf { it.totalQuantity }.coerceAtLeast(1.0)
                items(uiState.usageByType) { row ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    UsageType.fromValue(row.type).label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${row.count} times (${row.totalQuantity.formatQty()} units)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (row.totalQuantity / totalByType).toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Top consumed
            if (uiState.topConsumed.isNotEmpty()) {
                item {
                    Text("Most Consumed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                val maxConsumed = uiState.topConsumed.maxOfOrNull { it.totalQuantity } ?: 1.0
                items(uiState.topConsumed) { row ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.itemName, style = MaterialTheme.typography.bodyMedium)
                                Text(row.totalQuantity.formatQty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { (row.totalQuantity / maxConsumed).toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Top wasted
            if (uiState.topWasted.isNotEmpty()) {
                item {
                    Text("Most Wasted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                val maxWasted = uiState.topWasted.maxOfOrNull { it.totalQuantity } ?: 1.0
                items(uiState.topWasted) { row ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.itemName, style = MaterialTheme.typography.bodyMedium)
                                Text(row.totalQuantity.formatQty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { (row.totalQuantity / maxWasted).toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (uiState.usageByType.isEmpty() && !uiState.isLoading) {
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "No usage data for this period",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
