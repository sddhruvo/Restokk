package com.inventory.app.ui.screens.barcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.BarcodeCameraPreview
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.StockGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    navController: NavController,
    viewModel: BarcodeScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var lastDetectedBarcode by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Barcode Scanner") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera section
            if (cameraPermissionState.status.isGranted) {
                BarcodeCameraPreview(
                    onBarcodeDetected = { barcode ->
                        if (barcode != lastDetectedBarcode) {
                            lastDetectedBarcode = barcode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onBarcodeDetected(barcode)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                // Permission not granted - show request UI
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
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Camera preview",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (cameraPermissionState.status.shouldShowRationale) {
                            Text(
                                "Camera access is needed to scan product barcodes and auto-fill item details. Your camera is only used for scanning — no images are stored.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Point your camera at a barcode to quickly look up product details and add items to your inventory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Open camera", modifier = Modifier.size(18.dp))
                            Text("Grant Camera Permission", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            // Manual barcode entry
            Text(
                "Manual Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.manualBarcode,
                    onValueChange = { viewModel.updateManualBarcode(it) },
                    label = { Text("Enter barcode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.lookupBarcode(uiState.manualBarcode) },
                    enabled = uiState.manualBarcode.isNotBlank() && !uiState.isLookingUp
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Look up barcode", modifier = Modifier.size(18.dp))
                    Text("Look Up", modifier = Modifier.padding(start = 4.dp))
                }
            }

            // Loading state
            if (uiState.isLookingUp) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Looking up barcode...", modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }

            // Results
            when (val result = uiState.result) {
                is ScanResult.ExistingItem -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = StockGreen.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Item found", tint = StockGreen)
                                Text(
                                    "Item Found in Inventory",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.item.name, style = MaterialTheme.typography.bodyLarge)
                            result.item.brand?.let { Text("Brand: $it", style = MaterialTheme.typography.bodyMedium) }
                            Text("Quantity: ${result.item.quantity}", style = MaterialTheme.typography.bodyMedium)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = {
                                    navController.navigate(Screen.ItemDetail.createRoute(result.item.id))
                                }) {
                                    Icon(Icons.Filled.Visibility, contentDescription = "View item", modifier = Modifier.size(18.dp))
                                    Text("View Item", modifier = Modifier.padding(start = 4.dp))
                                }
                                OutlinedButton(onClick = {
                                    viewModel.quickAdd(result.item.barcode ?: "", result.item.name, result.item.brand)
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add item", modifier = Modifier.size(18.dp))
                                    Text("+1", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }

                is ScanResult.NewProduct -> {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Product Found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            result.barcodeResult.imageUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Product image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            result.barcodeResult.productName?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            result.barcodeResult.brand?.let {
                                Text("Brand: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            result.barcodeResult.quantityInfo?.let {
                                Text("Size: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            result.barcodeResult.nutritionGrade?.let {
                                Text("Nutrition Grade: ${it.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = {
                                    viewModel.quickAdd(
                                        result.barcode,
                                        result.barcodeResult.productName ?: "Unknown",
                                        result.barcodeResult.brand
                                    )
                                }) {
                                    Text("Quick Add")
                                }
                                OutlinedButton(onClick = {
                                    navController.navigate(
                                        Screen.ItemForm.createRoute(
                                            barcode = result.barcode,
                                            name = result.barcodeResult.productName,
                                            brand = result.barcodeResult.brand
                                        )
                                    )
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit details", modifier = Modifier.size(18.dp))
                                    Text("Add with Details", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }

                is ScanResult.NotFound -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Product Not Found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Barcode ${result.barcode} was not found in the database.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Button(
                                onClick = {
                                    navController.navigate(Screen.ItemForm.createRoute(barcode = result.barcode))
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Add Manually")
                            }
                        }
                    }
                }

                is ScanResult.Error -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Text(result.message, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                ScanResult.None -> {}
            }

            // Quick add success
            if (uiState.quickAddDone) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = StockGreen.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = StockGreen)
                        Text("Item added successfully!", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Scan Again button
            if (uiState.result != ScanResult.None || uiState.quickAddDone) {
                OutlinedButton(
                    onClick = { viewModel.scanAgain() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan again", modifier = Modifier.size(18.dp))
                    Text("Scan Again", modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Tips
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("- Point camera at barcode to scan automatically", style = MaterialTheme.typography.bodySmall)
                    Text("- Or type the barcode number manually above", style = MaterialTheme.typography.bodySmall)
                    Text("- Product data comes from Open Food Facts (works for UK products)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

