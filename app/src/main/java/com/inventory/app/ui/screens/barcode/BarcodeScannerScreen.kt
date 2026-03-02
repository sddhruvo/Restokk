package com.inventory.app.ui.screens.barcode

import com.inventory.app.ui.components.ThemedSnackbarHost
import com.inventory.app.ui.components.ThemedTextField
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
import com.inventory.app.ui.components.ThemedCircularProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.inventory.app.ui.components.ThemedScaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.inventory.app.ui.components.ThemedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.BarcodeCameraPreview
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.appColors

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
    val snackbarHostState = remember { SnackbarHostState() }

    // Quick add confirmation: haptic + snackbar
    LaunchedEffect(uiState.quickAddDone) {
        if (uiState.quickAddDone) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar("Item added to inventory!")
        }
    }

    ThemedScaffold(
        topBar = {
            ThemedTopAppBar(title = { Text("Barcode Scanner") })
        },
        snackbarHost = { ThemedSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLg)
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
                        ThemedIcon(
                            materialIcon = Icons.Filled.CameraAlt,
                            inkIconRes = R.drawable.ic_ink_camera,
                            contentDescription = "Camera preview",
                            modifier = Modifier.size(Dimens.iconSizeXl),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacingSm))
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
                        Spacer(modifier = Modifier.height(Dimens.spacingMd))
                        ThemedButton(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            ThemedIcon(materialIcon = Icons.Filled.CameraAlt, inkIconRes = R.drawable.ic_ink_camera, contentDescription = "Open camera", modifier = Modifier.size(Dimens.iconSizeSm))
                            Text("Grant Camera Permission", modifier = Modifier.padding(start = Dimens.spacingXs))
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
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedTextField(
                    value = uiState.manualBarcode,
                    onValueChange = { viewModel.updateManualBarcode(it) },
                    label = { Text("Enter barcode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                ThemedButton(
                    onClick = { viewModel.lookupBarcode(uiState.manualBarcode) },
                    enabled = uiState.manualBarcode.isNotBlank() && !uiState.isLookingUp
                ) {
                    ThemedIcon(materialIcon = Icons.Filled.Search, inkIconRes = R.drawable.ic_ink_search, contentDescription = "Look up barcode", modifier = Modifier.size(Dimens.iconSizeSm))
                    Text("Look Up", modifier = Modifier.padding(start = Dimens.spacingXs))
                }
            }

            // Loading state
            if (uiState.isLookingUp) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.spacingXl),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedCircularProgress(modifier = Modifier.size(Dimens.iconSizeMd))
                        Text("Looking up barcode...", modifier = Modifier.padding(start = Dimens.spacingMd))
                    }
                }
            }

            // Results
            when (val result = uiState.result) {
                is ScanResult.ExistingItem -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.appColors.statusInStock.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ThemedIcon(materialIcon = Icons.Filled.CheckCircle, inkIconRes = R.drawable.ic_ink_check_circle, contentDescription = "Item found", tint = MaterialTheme.appColors.statusInStock)
                                Text(
                                    "Item Found in Inventory",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = Dimens.spacingSm)
                                )
                            }
                            Spacer(modifier = Modifier.height(Dimens.spacingSm))
                            Text(result.item.name, style = MaterialTheme.typography.bodyLarge)
                            result.item.brand?.let { Text("Brand: $it", style = MaterialTheme.typography.bodyMedium) }
                            Text("Quantity: ${result.item.quantity}", style = MaterialTheme.typography.bodyMedium)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Dimens.spacingMd),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                ThemedButton(onClick = {
                                    navController.navigate(Screen.ItemDetail.createRoute(result.item.id))
                                }) {
                                    ThemedIcon(materialIcon = Icons.Filled.Visibility, inkIconRes = R.drawable.ic_ink_eye, contentDescription = "View item", modifier = Modifier.size(Dimens.iconSizeSm))
                                    Text("View Item", modifier = Modifier.padding(start = Dimens.spacingXs))
                                }
                                OutlinedButton(onClick = {
                                    viewModel.quickAdd(result.item.barcode ?: "", result.item.name, result.item.brand)
                                }) {
                                    ThemedIcon(materialIcon = Icons.Filled.Add, inkIconRes = R.drawable.ic_ink_add, contentDescription = "Add item", modifier = Modifier.size(Dimens.iconSizeSm))
                                    Text("+1", modifier = Modifier.padding(start = Dimens.spacingXs))
                                }
                            }
                        }
                    }
                }

                is ScanResult.NewProduct -> {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                            Text(
                                "Product Found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Dimens.spacingSm))

                            result.barcodeResult.imageUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Product image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(Dimens.spacingSm))
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
                                    .padding(top = Dimens.spacingMd),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                ThemedButton(onClick = {
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
                                    ThemedIcon(materialIcon = Icons.Filled.Edit, inkIconRes = R.drawable.ic_ink_edit, contentDescription = "Edit details", modifier = Modifier.size(Dimens.iconSizeSm))
                                    Text("Add with Details", modifier = Modifier.padding(start = Dimens.spacingXs))
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
                        Column(modifier = Modifier.padding(Dimens.spacingLg)) {
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
                            ThemedButton(
                                onClick = {
                                    navController.navigate(Screen.ItemForm.createRoute(barcode = result.barcode))
                                },
                                modifier = Modifier.padding(top = Dimens.spacingSm)
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
                            ThemedIcon(materialIcon = Icons.Filled.Error, inkIconRes = R.drawable.ic_ink_error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Text(result.message, modifier = Modifier.padding(start = Dimens.spacingSm))
                        }
                    }
                }

                ScanResult.None -> {}
            }

            // Quick add success
            if (uiState.quickAddDone) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.appColors.statusInStock.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemedIcon(materialIcon = Icons.Filled.CheckCircle, inkIconRes = R.drawable.ic_ink_check_circle, contentDescription = "Success", tint = MaterialTheme.appColors.statusInStock)
                        Text("Item added successfully!", modifier = Modifier.padding(start = Dimens.spacingSm))
                    }
                }
            }

            // Scan Again button
            if (uiState.result != ScanResult.None || uiState.quickAddDone) {
                OutlinedButton(
                    onClick = { viewModel.scanAgain() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemedIcon(materialIcon = Icons.Filled.QrCodeScanner, inkIconRes = R.drawable.ic_ink_barcode, contentDescription = "Scan again", modifier = Modifier.size(Dimens.iconSizeSm))
                    Text("Scan Again", modifier = Modifier.padding(start = Dimens.spacingSm))
                }
            }

            // Tips
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(Dimens.spacingLg)) {
                    Text("Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("- Point camera at barcode to scan automatically", style = MaterialTheme.typography.bodySmall)
                    Text("- Or type the barcode number manually above", style = MaterialTheme.typography.bodySmall)
                    Text("- Product data comes from Open Food Facts (works for UK products)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

