package com.inventory.app.ui.screens.reports

import com.inventory.app.ui.components.ThemedSnackbarHost
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.inventory.app.ui.components.ThemedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkBackButton
import com.inventory.app.ui.screens.onboarding.OnboardingViewModel
import com.inventory.app.ui.components.EmptyStateIllustration
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {
    private val _totalItemCount = MutableStateFlow(0)
    val totalItemCount = _totalItemCount.asStateFlow()

    private val _userPreference = MutableStateFlow("INVENTORY")
    val userPreference = _userPreference.asStateFlow()

    init {
        viewModelScope.launch {
            itemRepository.getTotalItemCount().collect { count ->
                _totalItemCount.value = count
            }
        }
        viewModelScope.launch {
            _userPreference.value = settingsRepository.getString(
                OnboardingViewModel.KEY_USER_PREFERENCE, "INVENTORY"
            )
        }
    }

    fun markReportsViewed() {
        viewModelScope.launch {
            settingsRepository.setBoolean("reports_viewed", true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController) {
    val viewModel: ReportsViewModel = hiltViewModel()
    val totalItems by viewModel.totalItemCount.collectAsState()
    val preference by viewModel.userPreference.collectAsState()
    LaunchedEffect(Unit) { viewModel.markReportsViewed() }
    val snackbarHostState = remember { SnackbarHostState() }
    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    InkBackButton(onClick = { navController.popBackStack() })
                }
            )
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
    ) { padding ->
        if (totalItems == 0) {
            val emptyBody = when (preference) {
                "WASTE" -> "Track your items for a few days and we'll show you what expires, what you waste, and how to save."
                "COOK" -> "Use the app for a few days and we'll show you cooking patterns, favourite recipes, and kitchen insights."
                else -> "Use the app for a few days and we'll show you patterns \u2014 what you spend, what expires, what you use most."
            }
            EmptyStateIllustration(
                icon = Icons.Filled.Assessment,
                headline = "Insights are brewing",
                body = emptyBody,
                ctaLabel = "Stock your shelves",
                onCtaClick = { navController.navigate(Screen.KitchenMap.route) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@ThemedScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                StaggeredAnimatedItem(index = 0) {
                    ReportCard("Expiring Items", "Items nearing expiry", Icons.Filled.Warning, MaterialTheme.appColors.reportExpiring) {
                        navController.navigate(Screen.ExpiringReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 1) {
                    ReportCard("Low Stock", "Items below minimum", Icons.Filled.TrendingDown, MaterialTheme.appColors.reportLowStock) {
                        navController.navigate(Screen.LowStockReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 2) {
                    ReportCard("Spending", "Purchase analysis", Icons.Filled.AttachMoney, MaterialTheme.appColors.reportSpending) {
                        navController.navigate(Screen.SpendingReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 3) {
                    ReportCard("Usage", "Consumption tracking", Icons.Filled.Assessment, MaterialTheme.appColors.reportUsage) {
                        navController.navigate(Screen.UsageReport.route)
                    }
                }
            }
            item {
                StaggeredAnimatedItem(index = 4) {
                    ReportCard("Full Inventory", "Complete inventory list", Icons.Filled.Inventory, MaterialTheme.appColors.reportInventory) {
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
            modifier = Modifier.padding(Dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            Icon(icon, contentDescription = "Report", modifier = Modifier.size(36.dp), tint = accentColor)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
