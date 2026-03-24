package com.inventory.app.ui.screens.reports

import com.inventory.app.ui.components.ThemedSnackbarHost
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.inventory.app.ui.components.ThemedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.inventory.app.ui.components.ThemedProgressBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.theme.emphasisBody
import com.inventory.app.ui.theme.sectionHeader
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
                .padding(horizontal = 16.dp),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PageHeader("Usage Report") }
            // Period chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpendingPeriod.entries.forEach { period ->
                        ThemedFilterChip(
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
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Total Usage", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${uiState.totalUsage.formatQty()} items",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Usage by type
            if (uiState.usageByType.isNotEmpty()) {
                item {
                    Text("By Type", style = MaterialTheme.typography.sectionHeader)
                }
                val totalByType = uiState.usageByType.sumOf { it.totalQuantity }.coerceAtLeast(1.0)
                items(uiState.usageByType, key = { it.type }) { row ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    UsageType.fromValue(row.type).label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "${row.count} times (${row.totalQuantity.formatQty()} units)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            ThemedProgressBar(
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
                    Text("Most Consumed", style = MaterialTheme.typography.sectionHeader)
                }
                val maxConsumed = uiState.topConsumed.maxOfOrNull { it.totalQuantity } ?: 1.0
                items(uiState.topConsumed, key = { "consumed_${it.itemName}" }) { row ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.itemName, style = MaterialTheme.typography.bodyMedium)
                                Text(row.totalQuantity.formatQty(), style = MaterialTheme.typography.emphasisBody)
                            }
                            ThemedProgressBar(
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
                    Text("Most Wasted", style = MaterialTheme.typography.sectionHeader)
                }
                val maxWasted = uiState.topWasted.maxOfOrNull { it.totalQuantity } ?: 1.0
                items(uiState.topWasted, key = { "wasted_${it.itemName}" }) { row ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.itemName, style = MaterialTheme.typography.bodyMedium)
                                Text(row.totalQuantity.formatQty(), style = MaterialTheme.typography.emphasisBody)
                            }
                            ThemedProgressBar(
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
