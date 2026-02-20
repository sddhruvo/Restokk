package com.inventory.app.ui.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ChartEntry
import com.inventory.app.ui.components.DailyChartEntry
import com.inventory.app.ui.components.HorizontalBarChart
import com.inventory.app.ui.components.SpendingLineChart
import com.inventory.app.ui.components.TimelineDateHeader
import com.inventory.app.ui.components.TimelinePurchaseItem
import com.inventory.app.ui.navigation.Screen
import java.time.LocalDate
import com.inventory.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingReportScreen(
    navController: NavController,
    viewModel: SpendingReportViewModel = hiltViewModel()
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
                title = { Text("Spending Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val purchasesGrouped = remember(uiState.allPurchasesForPeriod) {
            uiState.allPurchasesForPeriod.groupBy { it.purchaseDate }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Period chips
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

            // 2. Summary stat cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatCard(
                        title = "Total Spent",
                        value = "${uiState.currencySymbol}${String.format("%.2f", uiState.totalSpending)}",
                        modifier = Modifier.weight(1f),
                        changePercent = if (uiState.previousPeriodSpending > 0) {
                            ((uiState.totalSpending - uiState.previousPeriodSpending) / uiState.previousPeriodSpending * 100)
                        } else null
                    )
                    SummaryStatCard(
                        title = "Avg / Day",
                        value = "${uiState.currencySymbol}${String.format("%.2f", uiState.averagePerDay)}",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Purchases",
                        value = "${uiState.purchaseCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Period Comparison
            if (uiState.totalSpending > 0 || uiState.previousPeriodSpending > 0) {
                item {
                    Text(
                        "Period Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            PeriodComparisonRow(
                                label = "Total Spending",
                                currentValue = uiState.totalSpending,
                                previousValue = uiState.previousPeriodSpending,
                                currencySymbol = uiState.currencySymbol
                            )
                            Spacer(Modifier.height(12.dp))
                            PeriodComparisonRow(
                                label = "Avg / Active Day",
                                currentValue = uiState.fairAvgPerDay,
                                previousValue = uiState.previousFairAvgPerDay,
                                currencySymbol = uiState.currencySymbol
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Per day with purchases",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Spending Trend chart
            if (uiState.dailySpending.size >= 2) {
                item {
                    Text(
                        "Spending Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        SpendingLineChart(
                            entries = uiState.dailySpending.map { row ->
                                val date = LocalDate.ofEpochDay(row.date)
                                DailyChartEntry(
                                    label = FormatUtils.formatMonthDay(date),
                                    value = row.amount.toFloat()
                                )
                            },
                            currencySymbol = uiState.currencySymbol,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // 5. By Category
            if (uiState.spendingByCategory.isNotEmpty()) {
                item {
                    Text(
                        "By Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        HorizontalBarChart(
                            entries = uiState.spendingByCategory.map {
                                ChartEntry(it.label, it.amount.toFloat())
                            },
                            modifier = Modifier.padding(16.dp),
                            valuePrefix = uiState.currencySymbol
                        )
                    }
                }
            }

            // 6. By Store
            if (uiState.spendingByStore.isNotEmpty()) {
                item {
                    Text(
                        "By Store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        HorizontalBarChart(
                            entries = uiState.spendingByStore.map {
                                ChartEntry(it.label, it.amount.toFloat())
                            },
                            modifier = Modifier.padding(16.dp),
                            valuePrefix = uiState.currencySymbol
                        )
                    }
                }
            }

            // 7. Most Bought Items
            if (uiState.mostBoughtItems.isNotEmpty()) {
                item {
                    Text(
                        "Most Bought",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            uiState.mostBoughtItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate(
                                                Screen.ItemDetail.createRoute(item.itemId)
                                            )
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank badge
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
                                        item.itemName,
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
                                            "${item.purchaseCount}x",
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

            // 8. Purchases Timeline
            if (uiState.allPurchasesForPeriod.isNotEmpty()) {
                item {
                    Text(
                        "Purchases",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val grouped = purchasesGrouped
                grouped.forEach { (date, purchases) ->
                    item(key = "report_header_$date") {
                        TimelineDateHeader(date)
                    }
                    items(
                        items = purchases,
                        key = { "report_${it.id}" }
                    ) { purchase ->
                        val isLast = purchase == purchases.last()
                        TimelinePurchaseItem(
                            purchase = purchase,
                            isLastInGroup = isLast,
                            currencySymbol = uiState.currencySymbol,
                            onClick = {
                                navController.navigate(
                                    Screen.ItemDetail.createRoute(purchase.itemId)
                                )
                            }
                        )
                    }
                    item(key = "report_spacer_$date") {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Empty state
            if (uiState.spendingByCategory.isEmpty() && uiState.dailySpending.isEmpty() && !uiState.isLoading) {
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "No purchase data for this period",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    changePercent: Double? = null
) {
    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (changePercent != null) {
                val isNegative = changePercent <= 0
                val changeText = "${if (changePercent >= 0) "+" else ""}${String.format("%.1f", changePercent)}%"
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isNegative)
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Text(
                        changeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isNegative)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodComparisonRow(
    label: String,
    currentValue: Double,
    previousValue: Double,
    currencySymbol: String
) {
    val maxValue = maxOf(currentValue, previousValue, 0.01)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        // This period
        BarWithLabel(
            label = "This period",
            value = currentValue,
            maxValue = maxValue,
            formattedValue = "$currencySymbol${String.format("%.2f", currentValue)}",
            color = primaryColor
        )
        // Last period
        BarWithLabel(
            label = "Last period",
            value = previousValue,
            maxValue = maxValue,
            formattedValue = "$currencySymbol${String.format("%.2f", previousValue)}",
            color = secondaryColor
        )
    }
}

@Composable
private fun BarWithLabel(
    label: String,
    value: Double,
    maxValue: Double,
    formattedValue: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Box(modifier = Modifier.weight(1f).height(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fraction = if (maxValue > 0) (value / maxValue).toFloat().coerceIn(0f, 1f) else 0f
                drawRoundRect(
                    color = color,
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formattedValue,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(72.dp)
        )
    }
}
