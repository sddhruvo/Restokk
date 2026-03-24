package com.inventory.app.ui.screens.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.inventory.app.ui.theme.formSectionLabel
import com.inventory.app.ui.theme.statValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ExpiryDateCameraPreview
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.ThemedIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ExpiryDateScannerScreen(
    navController: NavController,
    viewModel: ExpiryDateScannerViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var detectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var isScanning by rememberSaveable { mutableStateOf(true) }
    val aiState by viewModel.uiState.collectAsState()

    PageScaffold(
        onBack = { navController.popBackStack() }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageHeader("Scan Expiry Date")
            // Camera section
            if (cameraPermissionState.status.isGranted) {
                if (isScanning) {
                    ExpiryDateCameraPreview(
                        onDateDetected = { date ->
                            detectedDate = date
                            isScanning = false
                        },
                        onPhotoFallbackNeeded = { bitmap ->
                            viewModel.extractExpiryDateFromImage(bitmap) { aiDate ->
                                detectedDate = aiDate
                                isScanning = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                } else {
                    // Show a placeholder when not scanning
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.CalendarMonth,
                                    inkIconRes = R.drawable.ic_ink_calendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Date detected!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Permission request
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.CameraAlt,
                            inkIconRes = R.drawable.ic_ink_camera,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (cameraPermissionState.status.shouldShowRationale)
                                "Camera access is needed to scan expiry dates from product labels."
                            else
                                "Point your camera at an expiry date to scan it automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemedButton(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            ThemedIcon(materialIcon = Icons.Filled.CameraAlt, inkIconRes = R.drawable.ic_ink_camera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Grant Camera Permission", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            // AI error message
            if (aiState.aiError != null) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        aiState.aiError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Detected date confirmation
            AnimatedVisibility(
                visible = detectedDate != null,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                detectedDate?.let { date ->
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Expiry Date Detected",
                                style = MaterialTheme.typography.formSectionLabel,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                formatDisplayDate(date),
                                style = MaterialTheme.typography.statValue
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemedButton(
                                    onClick = {
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("scannedExpiryDate", date)
                                        navController.popBackStack()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ThemedIcon(materialIcon = Icons.Filled.Check, inkIconRes = R.drawable.ic_ink_check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Use This Date", modifier = Modifier.padding(start = 4.dp))
                                }
                                OutlinedButton(
                                    onClick = {
                                        detectedDate = null
                                        isScanning = true
                                        viewModel.clearError()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ThemedIcon(materialIcon = Icons.Filled.Refresh, inkIconRes = R.drawable.ic_ink_refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Scan Again", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Tips
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tips", style = MaterialTheme.typography.formSectionLabel)
                    Text("- Keep the date inside the scan window", style = MaterialTheme.typography.bodySmall)
                    Text("- Hold steady until the brackets turn green", style = MaterialTheme.typography.bodySmall)
                    Text("- Pinch to zoom in on small text", style = MaterialTheme.typography.bodySmall)
                    Text("- Tap on the date to focus the camera there", style = MaterialTheme.typography.bodySmall)
                    Text("- Use the flash button in dim lighting", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Format YYYY-MM-DD to a user-friendly display string */
private fun formatDisplayDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    } catch (e: Exception) {
        isoDate
    }
}
