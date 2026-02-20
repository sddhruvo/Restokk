package com.inventory.app.ui.screens.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.data.local.dao.LocationWithItemCountRow
import com.inventory.app.ui.components.ConfirmDialog
import com.inventory.app.ui.components.EmptyState
import com.inventory.app.ui.components.LoadingState
import com.inventory.app.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    navController: NavController,
    viewModel: LocationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<LocationWithItemCountRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Locations") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.LocationForm.createRoute()) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Location")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.locations.isEmpty() -> EmptyState(
                icon = Icons.Filled.Place,
                title = "No Locations",
                message = "Add storage locations to organize where items are kept",
                actionLabel = "Add Location",
                onAction = { navController.navigate(Screen.LocationForm.createRoute()) }
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                itemsIndexed(uiState.locations, key = { _, it -> it.id }) { index, location ->
                    ListItem(
                        headlineContent = { Text(location.name) },
                        supportingContent = {
                            val zone = location.temperature_zone?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: ""
                            Text("${location.itemCount} items" + if (zone.isNotEmpty()) " | $zone" else "")
                        },
                        leadingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Column {
                                    IconButton(
                                        onClick = { viewModel.moveLocation(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.KeyboardArrowUp,
                                            contentDescription = "Move up",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.moveLocation(index, index + 1) },
                                        enabled = index < uiState.locations.size - 1,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Move down",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index < uiState.locations.size - 1) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try {
                                                Color(location.color?.toColorInt() ?: 0xFF6c757d.toInt())
                                            } catch (_: Exception) {
                                                Color(0xFF6c757d)
                                            }
                                        )
                                )
                            }
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    navController.navigate(Screen.LocationForm.createRoute(location.id))
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { deleteTarget = location }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.LocationDetail.createRoute(location.id))
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { location ->
        ConfirmDialog(
            title = "Delete Location",
            message = "Are you sure you want to delete \"${location.name}\"?",
            onConfirm = {
                viewModel.deleteLocation(location.id)
                deleteTarget = null
                scope.launch { snackbarHostState.showSnackbar("Location deleted") }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
