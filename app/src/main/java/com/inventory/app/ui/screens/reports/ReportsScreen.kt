package com.inventory.app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                StaggeredAnimatedItem(index = 0) {
                    ReportCard("Expiring Items", "Items nearing expiry", Icons.Filled.Warning, ReportExpiring) {
                        navController.navigate(Screen.ExpiringReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 1) {
                    ReportCard("Low Stock", "Items below minimum", Icons.Filled.TrendingDown, ReportLowStock) {
                        navController.navigate(Screen.LowStockReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 2) {
                    ReportCard("Spending", "Purchase analysis", Icons.Filled.AttachMoney, ReportSpending) {
                        navController.navigate(Screen.SpendingReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 3) {
                    ReportCard("Usage", "Consumption tracking", Icons.Filled.Assessment, ReportUsage) {
                        navController.navigate(Screen.UsageReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 4) {
                    ReportCard("Full Inventory", "Complete inventory list", Icons.Filled.Inventory, ReportInventory) {
                        navController.navigate(Screen.InventoryReport.route)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = accentColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = "Report", modifier = Modifier.size(36.dp), tint = accentColor)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
