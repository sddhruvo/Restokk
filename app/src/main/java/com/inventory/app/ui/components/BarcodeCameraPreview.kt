package com.inventory.app.ui.components

import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.inventory.app.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Shared camera preview composable for barcode scanning.
 * Used by BarcodeScannerScreen and receipt pager's per-item barcode scan.
 *
 * Features: autofocus, target resolution, torch toggle, visual scan guide overlay.
 */
@Composable
fun BarcodeCameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showOverlay: Boolean = true,
    showTorchButton: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember { PreviewView(context) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var boundCameraProvider: ProcessCameraProvider? = null
        var boundBarcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner? = null
        var lastDetectedBarcode: String? = null

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            boundCameraProvider = cameraProvider

            val preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val barcodeOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39
                )
                .build()

            val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)
            boundBarcodeScanner = barcodeScanner
            val mainExecutor = ContextCompat.getMainExecutor(context)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy, barcodeScanner) { barcode ->
                            if (barcode != lastDetectedBarcode) {
                                lastDetectedBarcode = barcode
                                mainExecutor.execute { onBarcodeDetected(barcode) }
                            }
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                cameraInstance = camera

                // Start continuous center-weighted autofocus
                val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                val centerPoint = factory.createPoint(0.5f, 0.5f)
                val focusAction = FocusMeteringAction.Builder(centerPoint)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera.cameraControl.startFocusAndMetering(focusAction)
            } catch (e: Exception) {
                Log.e("BarcodeCameraPreview", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraInstance = null
            boundCameraProvider?.unbindAll()
            boundBarcodeScanner?.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Visual scan guide overlay
        if (showOverlay) {
            ScanGuideOverlay()
        }

        // Torch toggle button (top-right)
        if (showTorchButton && cameraInstance?.cameraInfo?.hasFlashUnit() == true) {
            FilledTonalIconButton(
                onClick = {
                    isTorchOn = !isTorchOn
                    cameraInstance?.cameraControl?.enableTorch(isTorchOn)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                ThemedIcon(
                    materialIcon = if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    inkIconRes = if (isTorchOn) R.drawable.ic_ink_flash_on else R.drawable.ic_ink_flash_off,
                    contentDescription = if (isTorchOn) "Turn off flash" else "Turn on flash"
                )
            }
        }

        // Bottom hint card
        if (showOverlay) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppCard(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.QrCodeScanner,
                            inkIconRes = R.drawable.ic_ink_barcode,
                            contentDescription = "Camera",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Point at a barcode",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Semi-transparent overlay with a clear scan window and corner brackets.
 */
@Composable
private fun ScanGuideOverlay() {
    val bracketColor = Color.White
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Scan window: 75% width, aspect ratio ~2:1 for barcodes
        val windowWidth = canvasWidth * 0.75f
        val windowHeight = windowWidth * 0.5f
        val left = (canvasWidth - windowWidth) / 2f
        val top = (canvasHeight - windowHeight) / 2f - canvasHeight * 0.05f
        val cornerRadius = 16f

        // Draw semi-transparent background
        drawRect(color = Color.Black.copy(alpha = 0.5f))

        // Punch clear window
        val clearPath = Path().apply {
            addRoundRect(RoundRect(left, top, left + windowWidth, top + windowHeight, CornerRadius(cornerRadius)))
        }
        clipPath(clearPath, clipOp = ClipOp.Intersect) {
            drawRect(color = Color.Transparent)
        }
        // Actually clear the window by drawing with BlendMode
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(windowWidth, windowHeight),
            cornerRadius = CornerRadius(cornerRadius),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Draw corner brackets (Paper & Ink style)
        val bracketLen = windowWidth * 0.12f
        val strokeWidth = 4.dp.toPx()
        val bracketStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val inset = strokeWidth / 2f

        // Top-left
        drawLine(bracketColor, Offset(left + inset, top + cornerRadius), Offset(left + inset, top + bracketLen), bracketStroke)
        drawLine(bracketColor, Offset(left + cornerRadius, top + inset), Offset(left + bracketLen, top + inset), bracketStroke)

        // Top-right
        val right = left + windowWidth
        drawLine(bracketColor, Offset(right - inset, top + cornerRadius), Offset(right - inset, top + bracketLen), bracketStroke)
        drawLine(bracketColor, Offset(right - cornerRadius, top + inset), Offset(right - bracketLen, top + inset), bracketStroke)

        // Bottom-left
        val bottom = top + windowHeight
        drawLine(bracketColor, Offset(left + inset, bottom - cornerRadius), Offset(left + inset, bottom - bracketLen), bracketStroke)
        drawLine(bracketColor, Offset(left + cornerRadius, bottom - inset), Offset(left + bracketLen, bottom - inset), bracketStroke)

        // Bottom-right
        drawLine(bracketColor, Offset(right - inset, bottom - cornerRadius), Offset(right - inset, bottom - bracketLen), bracketStroke)
        drawLine(bracketColor, Offset(right - cornerRadius, bottom - inset), Offset(right - bracketLen, bottom - inset), bracketStroke)
    }
}

// Reusable drawLine extension for Stroke
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLine(
    color: Color,
    start: Offset,
    end: Offset,
    stroke: Stroke
) {
    drawLine(color = color, start = start, end = end, strokeWidth = stroke.width, cap = stroke.cap)
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { value ->
                    onBarcodeDetected(value)
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeCameraPreview", "Barcode scan failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
