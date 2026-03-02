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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.inventory.app.ui.components.ThemedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.Text
import com.inventory.app.ui.components.ThemedTopAppBar
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
import com.inventory.app.ui.components.InkBackButton
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiringReportScreen(
    navController: NavController,
    viewModel: ExpiringReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val showShoppingSheet = com.inventory.app.ui.screens.shopping.LocalShowAddShoppingSheet.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ThemedScaffold(
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) },
        topBar = {
            ThemedTopAppBar(
                title = { Text("Expiring Items Report") },
                navigationIcon = {
                    InkBackButton(onClick = { navController.popBackStack() })
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@ThemedScaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            // Filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    listOf(3, 7, 14, 30).forEach { days ->
                        ThemedFilterChip(
                            selected = uiState.warningDays == days,
                            onClick = { viewModel.updateWarningDays(days) },
                            label = { Text("${days}d") }
                        )
                    }
                }
            }

            // Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                            Text("${uiState.expiredCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Expired", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                            Text("${uiState.expiringCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Expiring Soon", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Expired items
            if (uiState.expiredItems.isNotEmpty()) {
                item {
                    Text("Expired", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.statusExpired)
                }
                items(uiState.expiredItems, key = { it.item.id }) { item ->
                    val daysAgo = item.item.expiryDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = {
                            Text(
                                if (daysAgo != null) "Expired $daysAgo days ago" else "",
                                color = MaterialTheme.appColors.statusExpired
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                item.category?.let { Text(it.name, style = MaterialTheme.typography.labelSmall) }
                                IconButton(onClick = { showShoppingSheet(item.item.id, null) }) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.AddShoppingCart,
                                        inkIconRes = R.drawable.ic_ink_add_to_cart,
                                        contentDescription = "Add to shopping list",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                        }
                    )
                }
            }

            // Expiring soon items
            if (uiState.expiringItems.isNotEmpty()) {
                item {
                    Text(
                        "Expiring within ${uiState.warningDays} days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.appColors.statusExpiring
                    )
                }
                items(uiState.expiringItems, key = { it.item.id }) { item ->
                    val daysLeft = item.item.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = {
                            Text(
                                when {
                                    daysLeft == null -> ""
                                    daysLeft == 0L -> "Expires today"
                                    else -> "Expires in $daysLeft days"
                                },
                                color = MaterialTheme.appColors.statusExpiring
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                item.category?.let { Text(it.name, style = MaterialTheme.typography.labelSmall) }
                                IconButton(onClick = { showShoppingSheet(item.item.id, null) }) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.AddShoppingCart,
                                        inkIconRes = R.drawable.ic_ink_add_to_cart,
                                        contentDescription = "Add to shopping list",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ItemDetail.createRoute(item.item.id))
                        }
                    )
                }
            }

            if (uiState.expiredItems.isEmpty() && uiState.expiringItems.isEmpty() && !uiState.isLoading) {
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "No items expiring within ${uiState.warningDays} days",
                            modifier = Modifier.padding(Dimens.spacingXl),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
