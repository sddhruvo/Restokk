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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.ExpiryOrange
import com.inventory.app.ui.theme.ExpiryRed
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
                title = { Text("Expiring Items Report") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 7, 14, 30).forEach { days ->
                        FilterChip(
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${uiState.expiredCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Expired", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${uiState.expiringCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Expiring Soon", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Expired items
            if (uiState.expiredItems.isNotEmpty()) {
                item {
                    Text("Expired", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpiryRed)
                }
                items(uiState.expiredItems, key = { it.item.id }) { item ->
                    val daysAgo = item.item.expiryDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }
                    ListItem(
                        headlineContent = { Text(item.item.name) },
                        supportingContent = {
                            Text(
                                if (daysAgo != null) "Expired $daysAgo days ago" else "",
                                color = ExpiryRed
                            )
                        },
                        trailingContent = {
                            item.category?.let { Text(it.name, style = MaterialTheme.typography.labelSmall) }
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
                        color = ExpiryOrange
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
                                color = ExpiryOrange
                            )
                        },
                        trailingContent = {
                            item.category?.let { Text(it.name, style = MaterialTheme.typography.labelSmall) }
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
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
