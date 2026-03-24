package com.inventory.app.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import com.inventory.app.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Scanner status for live feedback */
enum class ScannerStatus {
    SCANNING,       // Camera running, no text found
    TEXT_FOUND,     // ML Kit found text but no date
    DATE_SPOTTED,   // Date found in 1-2 frames, building consensus
    LOCKED,         // 3/5 consensus reached
    PHOTO_PROMPT    // OCR failed, prompting user to take a photo
}

// Scan window proportions (shared between overlay and crop logic)
private const val SCAN_WINDOW_WIDTH_RATIO = 0.65f
private const val SCAN_WINDOW_HEIGHT_RATIO = 0.33f // of window width
private const val SCAN_WINDOW_Y_OFFSET = -0.05f // slight upward shift

/**
 * Camera preview for scanning expiry dates using ML Kit text recognition.
 * Features: region cropping, multi-frame consensus, confidence filtering,
 * spatial scoring, OCR normalization, tap-to-focus, pinch-to-zoom.
 */
@Composable
fun ExpiryDateCameraPreview(
    onDateDetected: (String) -> Unit,
    onPhotoFallbackNeeded: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showTorchButton: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember { PreviewView(context) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var scannerStatus by remember { mutableStateOf(ScannerStatus.SCANNING) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        var boundCameraProvider: ProcessCameraProvider? = null

        // Multi-frame consensus buffer
        val frameBuffer = mutableListOf<String?>() // last 5 results
        val maxFrames = 5
        val requiredConsensus = 3
        var locked = false

        // Photo prompt: after ~30 failed frames (~6 seconds), show capture button
        var failedFrameCount = 0
        val photoPromptThreshold = 30
        var photoPromptShown = false

        // Periodic refocus
        val refocusExecutor = Executors.newSingleThreadScheduledExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            boundCameraProvider = cameraProvider

            val preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080))
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val mainExecutor = ContextCompat.getMainExecutor(context)

            val imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1920, 1080))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            imageCaptureRef = imageCapture

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1920, 1080))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (locked) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        processFrameWithCrop(imageProxy, textRecognizer) { result ->
                            mainExecutor.execute {
                                if (locked) return@execute

                                // Update status based on result
                                when (result) {
                                    is FrameResult.NoText -> {
                                        if (!photoPromptShown) scannerStatus = ScannerStatus.SCANNING
                                        frameBuffer.add(null)
                                        failedFrameCount++
                                    }
                                    is FrameResult.TextButNoDate -> {
                                        if (!photoPromptShown) scannerStatus = ScannerStatus.TEXT_FOUND
                                        frameBuffer.add(null)
                                        failedFrameCount++
                                    }
                                    is FrameResult.DateFound -> {
                                        frameBuffer.add(result.date)
                                        failedFrameCount = 0
                                        photoPromptShown = false // reset if date found again
                                    }
                                }

                                // Keep buffer at max size
                                while (frameBuffer.size > maxFrames) {
                                    frameBuffer.removeAt(0)
                                }

                                // Check consensus
                                if (frameBuffer.isNotEmpty()) {
                                    val dateCounts = frameBuffer
                                        .filterNotNull()
                                        .groupingBy { it }
                                        .eachCount()

                                    val bestDate = dateCounts.maxByOrNull { it.value }
                                    if (bestDate != null && bestDate.value >= requiredConsensus) {
                                        locked = true
                                        scannerStatus = ScannerStatus.LOCKED
                                        onDateDetected(bestDate.key)
                                    } else if (bestDate != null && !photoPromptShown) {
                                        scannerStatus = ScannerStatus.DATE_SPOTTED
                                    }
                                }

                                // Show photo capture prompt after threshold
                                if (!photoPromptShown &&
                                    failedFrameCount >= photoPromptThreshold &&
                                    onPhotoFallbackNeeded != null
                                ) {
                                    photoPromptShown = true
                                    scannerStatus = ScannerStatus.PHOTO_PROMPT
                                }
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
                    imageAnalysis,
                    imageCapture
                )
                cameraInstance = camera

                // Initial center focus
                triggerCenterFocus(camera)

                // Gentle periodic refocus — long interval to avoid blur during OCR
                refocusExecutor.scheduleAtFixedRate({
                    mainExecutor.execute {
                        if (!locked) {
                            triggerCenterFocus(camera)
                        }
                    }
                }, 10, 10, TimeUnit.SECONDS)

            } catch (e: Exception) {
                Log.e("ExpiryDateCamera", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        // Pinch-to-zoom + tap-to-focus (combined touch handler)
        val scaleGestureDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val camera = cameraInstance ?: return false
                    val currentZoom = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                    val newZoom = (currentZoom * detector.scaleFactor).coerceIn(1f, 5f)
                    camera.cameraControl.setZoomRatio(newZoom)
                    zoomRatio = newZoom
                    return true
                }
            }
        )
        previewView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress && event.action == MotionEvent.ACTION_UP) {
                val camera = cameraInstance ?: return@setOnTouchListener false
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera.cameraControl.startFocusAndMetering(action)
                view.performClick()
            }
            true
        }

        onDispose {
            cameraInstance = null
            boundCameraProvider?.unbindAll()
            textRecognizer.close()
            cameraExecutor.shutdown()
            refocusExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Visual scan guide overlay
        ExpiryDateGuideOverlay(scannerStatus)

        // Torch toggle
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

        // Zoom indicator
        if (zoomRatio > 1.1f) {
            AppCard(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Text(
                    "%.1fx".format(zoomRatio),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom status + capture button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Take a photo" button when live scanning fails
            AnimatedVisibility(
                visible = scannerStatus == ScannerStatus.PHOTO_PROMPT && !isCapturing,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                ThemedButton(
                    onClick = {
                        val capture = imageCaptureRef ?: return@ThemedButton
                        isCapturing = true
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    val bitmap = imageProxy.toBitmap()
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val rotated = if (rotation != 0) {
                                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                    } else bitmap
                                    imageProxy.close()

                                    // Crop to scan window
                                    val imgW = rotated.width.toFloat()
                                    val imgH = rotated.height.toFloat()
                                    val windowW = imgW * SCAN_WINDOW_WIDTH_RATIO
                                    val windowH = windowW * SCAN_WINDOW_HEIGHT_RATIO
                                    val cropL = ((imgW - windowW) / 2f).toInt().coerceAtLeast(0)
                                    val cropT = ((imgH - windowH) / 2f + imgH * SCAN_WINDOW_Y_OFFSET).toInt().coerceAtLeast(0)
                                    val cropW = windowW.toInt().coerceAtMost(rotated.width - cropL)
                                    val cropH = windowH.toInt().coerceAtMost(rotated.height - cropT)

                                    if (cropW <= 0 || cropH <= 0) {
                                        ContextCompat.getMainExecutor(context).execute { isCapturing = false }
                                        if (rotated !== bitmap) rotated.recycle()
                                        return
                                    }

                                    val cropped = Bitmap.createBitmap(rotated, cropL, cropT, cropW, cropH)
                                    if (rotated !== bitmap) rotated.recycle()

                                    // Try ML Kit on the still image — reuse the existing recognizer
                                    val recognizer = textRecognizer
                                    val inputImage = InputImage.fromBitmap(cropped, 0)
                                    recognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val date = if (visionText.textBlocks.isNotEmpty()) {
                                                extractDateFromBlocks(visionText, cropped.width, cropped.height)
                                            } else null

                                            ContextCompat.getMainExecutor(context).execute {
                                                if (date != null) {
                                                    scannerStatus = ScannerStatus.LOCKED
                                                    isCapturing = false
                                                    onDateDetected(date)
                                                } else {
                                                    // ML Kit failed on still — send to AI vision
                                                    isCapturing = false
                                                    onPhotoFallbackNeeded?.invoke(cropped)
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                onPhotoFallbackNeeded?.invoke(cropped)
                                            }
                                        }
                                        .addOnCompleteListener { /* recognizer is shared, don't close */ }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("ExpiryDateCamera", "Photo capture failed", exception)
                                    ContextCompat.getMainExecutor(context).execute { isCapturing = false }
                                }
                            }
                        )
                    }
                ) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.CameraAlt,
                        inkIconRes = R.drawable.ic_ink_camera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Take a photo", modifier = Modifier.padding(start = 6.dp))
                }
            }

            // Loading indicator during capture
            if (isCapturing) {
                AppCard(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Processing photo...",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when (scannerStatus) {
                ScannerStatus.SCANNING -> "Point at the expiry date"
                ScannerStatus.TEXT_FOUND -> "Text found, looking for date..."
                ScannerStatus.DATE_SPOTTED -> "Date spotted, hold steady..."
                ScannerStatus.LOCKED -> "Date locked!"
                ScannerStatus.PHOTO_PROMPT -> "Can't read it automatically"
            }
            val statusColor by animateColorAsState(
                when (scannerStatus) {
                    ScannerStatus.SCANNING -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ScannerStatus.TEXT_FOUND -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    ScannerStatus.DATE_SPOTTED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                    ScannerStatus.LOCKED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ScannerStatus.PHOTO_PROMPT -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                },
                label = "statusColor"
            )

            AppCard(containerColor = statusColor) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.CalendarMonth,
                        inkIconRes = R.drawable.ic_ink_calendar,
                        contentDescription = "Expiry",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (scannerStatus == ScannerStatus.LOCKED) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun triggerCenterFocus(camera: Camera) {
    try {
        val factory = androidx.camera.core.SurfaceOrientedMeteringPointFactory(1f, 1f)
        val centerPoint = factory.createPoint(0.5f, 0.5f)
        val focusAction = FocusMeteringAction.Builder(centerPoint)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera.cameraControl.startFocusAndMetering(focusAction)
    } catch (e: Exception) {
        Log.e("ExpiryDateCamera", "Focus failed", e)
    }
}

/** Result from processing a single frame */
private sealed class FrameResult {
    data object NoText : FrameResult()
    data class TextButNoDate(val rawText: String) : FrameResult()
    data class DateFound(val date: String) : FrameResult()
}

/**
 * Scan guide overlay with animated brackets that change color based on status.
 */
@Composable
private fun ExpiryDateGuideOverlay(status: ScannerStatus) {
    val bracketColor by animateColorAsState(
        when (status) {
            ScannerStatus.SCANNING -> Color.White
            ScannerStatus.TEXT_FOUND -> Color(0xFFFFC107) // amber
            ScannerStatus.DATE_SPOTTED -> Color(0xFFFF9800) // orange
            ScannerStatus.LOCKED -> Color(0xFF4CAF50) // green
            ScannerStatus.PHOTO_PROMPT -> Color(0xFF2196F3) // blue
        },
        label = "bracketColor"
    )

    // Pulse animation for DATE_SPOTTED
    val infiniteTransition = rememberInfiniteTransition(label = "scanPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val effectiveAlpha = if (status == ScannerStatus.DATE_SPOTTED || status == ScannerStatus.PHOTO_PROMPT) pulseAlpha else 1f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val windowWidth = canvasWidth * SCAN_WINDOW_WIDTH_RATIO
        val windowHeight = windowWidth * SCAN_WINDOW_HEIGHT_RATIO
        val left = (canvasWidth - windowWidth) / 2f
        val top = (canvasHeight - windowHeight) / 2f + canvasHeight * SCAN_WINDOW_Y_OFFSET
        val cornerRadius = 12f

        // Semi-transparent background
        drawRect(color = Color.Black.copy(alpha = 0.5f))

        // Clear window
        val clearPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left, top, left + windowWidth, top + windowHeight,
                    CornerRadius(cornerRadius)
                )
            )
        }
        clipPath(clearPath, clipOp = ClipOp.Intersect) {
            drawRect(color = Color.Transparent)
        }
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(windowWidth, windowHeight),
            cornerRadius = CornerRadius(cornerRadius),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Corner brackets with status color
        val bracketLen = windowWidth * 0.1f
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2f
        val right = left + windowWidth
        val bottom = top + windowHeight
        val color = bracketColor.copy(alpha = effectiveAlpha)

        // Top-left
        drawLine(color, Offset(left + inset, top + cornerRadius), Offset(left + inset, top + bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left + cornerRadius, top + inset), Offset(left + bracketLen, top + inset), strokeWidth, StrokeCap.Round)
        // Top-right
        drawLine(color, Offset(right - inset, top + cornerRadius), Offset(right - inset, top + bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(right - cornerRadius, top + inset), Offset(right - bracketLen, top + inset), strokeWidth, StrokeCap.Round)
        // Bottom-left
        drawLine(color, Offset(left + inset, bottom - cornerRadius), Offset(left + inset, bottom - bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left + cornerRadius, bottom - inset), Offset(left + bracketLen, bottom - inset), strokeWidth, StrokeCap.Round)
        // Bottom-right
        drawLine(color, Offset(right - inset, bottom - cornerRadius), Offset(right - inset, bottom - bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(right - cornerRadius, bottom - inset), Offset(right - bracketLen, bottom - inset), strokeWidth, StrokeCap.Round)
    }
}

// ============================================================
// Frame processing pipeline
// ============================================================

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processFrameWithCrop(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    onResult: (FrameResult) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        onResult(FrameResult.NoText)
        return
    }

    // Crop to scan window region
    val bitmap = imageProxy.toBitmap()
    val rotation = imageProxy.imageInfo.rotationDegrees

    // Apply rotation to bitmap so we work in display coordinates
    val rotatedBitmap = if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }

    val imgW = rotatedBitmap.width.toFloat()
    val imgH = rotatedBitmap.height.toFloat()

    // Calculate crop region matching the overlay
    val windowWidth = imgW * SCAN_WINDOW_WIDTH_RATIO
    val windowHeight = windowWidth * SCAN_WINDOW_HEIGHT_RATIO
    val cropLeft = ((imgW - windowWidth) / 2f).toInt().coerceAtLeast(0)
    val cropTop = ((imgH - windowHeight) / 2f + imgH * SCAN_WINDOW_Y_OFFSET).toInt().coerceAtLeast(0)
    val cropW = windowWidth.toInt().coerceAtMost(rotatedBitmap.width - cropLeft)
    val cropH = windowHeight.toInt().coerceAtMost(rotatedBitmap.height - cropTop)

    if (cropW <= 0 || cropH <= 0) {
        imageProxy.close()
        onResult(FrameResult.NoText)
        return
    }

    val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropLeft, cropTop, cropW, cropH)

    // Send cropped region to ML Kit (rotation already applied)
    val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

    textRecognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            if (visionText.textBlocks.isEmpty()) {
                onResult(FrameResult.NoText)
                return@addOnSuccessListener
            }

            // Use structured text blocks with confidence + spatial scoring
            val date = extractDateFromBlocks(visionText, croppedBitmap.width, croppedBitmap.height)
            if (date != null) {
                onResult(FrameResult.DateFound(date))
            } else {
                onResult(FrameResult.TextButNoDate(visionText.text))
            }
        }
        .addOnFailureListener { e ->
            Log.e("ExpiryDateCamera", "Text recognition failed", e)
            onResult(FrameResult.NoText)
        }
        .addOnCompleteListener {
            imageProxy.close()
            // Clean up bitmaps
            if (rotatedBitmap !== bitmap) rotatedBitmap.recycle()
            croppedBitmap.recycle()
        }
}

// ============================================================
// Smart date extraction with confidence + spatial scoring
// ============================================================

/**
 * Extract expiry date using ML Kit's structured text blocks.
 * Scores each block by spatial position, digit content, confidence, and keyword proximity.
 */
private fun extractDateFromBlocks(
    visionText: Text,
    imageWidth: Int,
    imageHeight: Int
): String? {
    val centerX = imageWidth / 2f
    val centerY = imageHeight / 2f

    data class ScoredBlock(
        val rawText: String,
        val score: Float
    )

    val scoredBlocks = visionText.textBlocks.mapNotNull { block ->
        val box = block.boundingBox ?: return@mapNotNull null
        val blockCenterX = (box.left + box.right) / 2f
        val blockCenterY = (box.top + box.bottom) / 2f
        val rawText = block.text

        var score = 0f

        // Proximity to center (max 3 points)
        val distFromCenter = kotlin.math.sqrt(
            ((blockCenterX - centerX) / imageWidth).let { it * it } +
            ((blockCenterY - centerY) / imageHeight).let { it * it }
        )
        score += (1f - distFromCenter.coerceAtMost(1f)) * 3f

        // Contains digits (essential for dates)
        val digitRatio = rawText.count { it.isDigit() }.toFloat() / rawText.length.coerceAtLeast(1)
        if (digitRatio > 0.1f) score += 3f
        if (digitRatio > 0.3f) score += 1f

        // Short text blocks (1-2 lines) more likely to be dates
        if (block.lines.size <= 2) score += 2f

        // Contains expiry keyword
        val upperText = rawText.uppercase()
        if (EXPIRY_KEYWORDS.any { upperText.contains(it) }) score += 4f

        // Contains ignore keyword (penalize)
        if (IGNORE_KEYWORDS.any { upperText.contains(it) }) score -= 5f

        ScoredBlock(rawText, score)
    }.sortedByDescending { it.score }

    Log.d("ExpiryDateCamera", "Blocks found: ${scoredBlocks.size}, texts: ${scoredBlocks.map { "'${it.rawText}' (${it.score})" }}")

    // Strategy 1: Try raw text from highest-scored blocks first
    for (scored in scoredBlocks) {
        val date = extractExpiryDate(scored.rawText)
        if (date != null) {
            Log.d("ExpiryDateCamera", "Date from raw block: $date")
            return date
        }
    }

    // Strategy 2: Try combined raw text
    val combinedRaw = scoredBlocks.joinToString("\n") { it.rawText }
    val dateFromRaw = extractExpiryDate(combinedRaw)
    if (dateFromRaw != null) {
        Log.d("ExpiryDateCamera", "Date from combined raw: $dateFromRaw")
        return dateFromRaw
    }

    // Strategy 3: Try with OCR normalization (fixes misread characters)
    for (scored in scoredBlocks) {
        val normalized = normalizeOcrText(scored.rawText)
        if (normalized != scored.rawText) {
            val date = extractExpiryDate(normalized)
            if (date != null) {
                Log.d("ExpiryDateCamera", "Date from normalized: $date (raw was '${scored.rawText}')")
                return date
            }
        }
    }

    // Strategy 4: Most-future-date heuristic (bare dates, no keywords)
    val allDates = mutableListOf<String>()
    for (scored in scoredBlocks) {
        if (scored.score < 0) continue
        allDates.addAll(extractAllDates(scored.rawText))
    }
    if (allDates.isNotEmpty()) {
        val best = allDates.distinct().maxByOrNull { it }
        Log.d("ExpiryDateCamera", "Date from future-heuristic: $best (candidates: $allDates)")
        return best
    }

    Log.d("ExpiryDateCamera", "No date found in any block")
    return null
}

// ============================================================
// OCR Text Normalization
// ============================================================

/**
 * Fix common ML Kit misreads. Conservative approach:
 * - Keyword substitutions: only fix known misspellings (8EST→BEST, EXP1RY→EXPIRY)
 * - Date substitutions: only fix isolated letters between digits (e.g., 25/O3/2025 → 25/03/2025)
 *   Never touch letters that could be month names (JAN, FEB, etc.)
 */
private fun normalizeOcrText(raw: String): String {
    return raw.lines().joinToString("\n") { line ->
        // Step 1: Fix keyword misreads (safe — these are specific known patterns)
        var result = line
            .replace("8EST", "BEST", ignoreCase = true)
            .replace("8EFORE", "BEFORE", ignoreCase = true)
            .replace("8EF", "BEF", ignoreCase = true)
            .replace("EXP1RY", "EXPIRY", ignoreCase = true)
            .replace("EXP1R", "EXPIR", ignoreCase = true)
            .replace("C0NSUME", "CONSUME", ignoreCase = true)

        // Step 2: Strip ordinal suffixes (15th→15, 1st→1, 2nd→2, 3rd→3)
        result = result.replace(Regex("""(\d)(st|nd|rd|th)\b""", RegexOption.IGNORE_CASE)) { it.groupValues[1] }

        // Step 3: Separate keyword glued to date (EXP03/27→EXP 03/27, BB12/26→BB 12/26)
        result = result.replace(Regex("""(EXP|BB|BBE)(\d)""", RegexOption.IGNORE_CASE)) {
            "${it.groupValues[1]} ${it.groupValues[2]}"
        }

        // Step 4: Fix letter→digit substitutions ONLY when the letter is between two digits
        val sb = StringBuilder()
        for ((i, ch) in result.withIndex()) {
            val prevIsDigit = i > 0 && result[i - 1].isDigit()
            val nextIsDigit = i < result.length - 1 && result[i + 1].isDigit()
            if (prevIsDigit && nextIsDigit) {
                sb.append(when (ch) {
                    'O', 'o' -> '0'
                    'l', 'I' -> '1'
                    'S', 's' -> '5'
                    'B' -> '8'
                    'G' -> '6'
                    'Z', 'z' -> '2'
                    else -> ch
                })
            } else {
                sb.append(ch)
            }
        }
        sb.toString()
    }
}

// ============================================================
// Date extraction logic
// ============================================================

/** Keywords indicating an expiry date follows */
private val EXPIRY_KEYWORDS = listOf(
    "EXP", "EXPIRY", "EXPIRES", "EXPIRATION",
    "USE BY", "USEBY", "USE-BY",
    "BEST BEFORE", "BEST BEF", "BB", "BBE", "B.B",
    "SELL BY", "SELLBY", "SELL-BY",
    "BEST END", "BEST BY", "BEST-BY",
    "CONSUME BY", "CONSUME BEFORE",
    "VALID UNTIL", "VALID THRU",
    "BEST IF USED BY", "BEST IF USED BEFORE",
    "END"
)

/** Keywords to IGNORE -- these indicate manufacturing/production dates */
private val IGNORE_KEYWORDS = listOf(
    "MFG", "MFD", "MFGD", "MANUFACTURED",
    "PROD", "PRODUCTION", "PRODUCED",
    "PACKED", "PACK DATE", "PACKING",
    "LOT", "BATCH", "L:", "B:"
)

/** Month names — abbreviations + full names */
private val MONTH_MAP = mapOf(
    "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4,
    "MAY" to 5, "JUN" to 6, "JUL" to 7, "AUG" to 8,
    "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12,
    "JANUARY" to 1, "FEBRUARY" to 2, "MARCH" to 3, "APRIL" to 4,
    "JUNE" to 6, "JULY" to 7, "AUGUST" to 8,
    "SEPTEMBER" to 9, "OCTOBER" to 10, "NOVEMBER" to 11, "DECEMBER" to 12,
    "SEPT" to 9 // common alternate abbreviation
)

/** Regex alternation for all month names */
private val MONTH_NAMES_PATTERN = MONTH_MAP.keys.sortedByDescending { it.length }.joinToString("|")

// Pattern indices (for parseMatchToDate dispatch)
private const val PAT_DD_MM_YYYY = 0
private const val PAT_YYYY_MM_DD = 1
private const val PAT_DD_MM_YY = 2
private const val PAT_DD_MON_YYYY = 3
private const val PAT_DD_MON_YY = 4
private const val PAT_MON_DD_YYYY = 5    // US order: JAN 15 2027
private const val PAT_MON_DD_YY = 6      // US order: JAN 15 27
private const val PAT_MON_YYYY = 7
private const val PAT_MON_YY = 8
private const val PAT_MM_YYYY = 9
private const val PAT_MM_YY = 10
private const val PAT_DDMMYYYY = 11
private const val PAT_DDMMYY = 12
private const val PAT_DD_MON_NOYEAR = 13  // 15 JAN (no year)
private const val PAT_MON_DD_NOYEAR = 14  // JAN 15 (no year, US)
private const val PAT_DD_MM_NOYEAR = 15   // 15/03 (no year)
private const val PAT_MON_ONLY = 16       // JAN (month only, near keyword)

// Regex patterns for date formats (ordered by specificity — most specific first)
private val DATE_PATTERNS: List<Regex> by lazy {
    val m = MONTH_NAMES_PATTERN
    listOf(
        // 0: DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
        Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})"""),
        // 1: YYYY-MM-DD or YYYY/MM/DD
        Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})"""),
        // 2: DD/MM/YY or DD-MM-YY or DD.MM.YY
        Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})"""),
        // 3: DD MON YYYY (e.g., 15 JAN 2025, 15 JANUARY 2025)
        Regex("""(\d{1,2})\s+($m)\.?\s+(\d{4})""", RegexOption.IGNORE_CASE),
        // 4: DD MON YY (e.g., 15 JAN 25)
        Regex("""(\d{1,2})\s+($m)\.?\s+(\d{2})\b""", RegexOption.IGNORE_CASE),
        // 5: MON DD YYYY (US: JAN 15 2027, JANUARY 15 2027)
        Regex("""($m)\.?\s+(\d{1,2}),?\s+(\d{4})""", RegexOption.IGNORE_CASE),
        // 6: MON DD YY (US: JAN 15 27)
        Regex("""($m)\.?\s+(\d{1,2}),?\s+(\d{2})\b""", RegexOption.IGNORE_CASE),
        // 7: MON YYYY (e.g., JAN 2025, JANUARY 2025)
        Regex("""($m)\.?\s+(\d{4})""", RegexOption.IGNORE_CASE),
        // 8: MON YY (e.g., JAN 25)
        Regex("""($m)\.?\s+(\d{2})\b""", RegexOption.IGNORE_CASE),
        // 9: MM/YYYY or MM-YYYY or MM.YYYY
        Regex("""(\d{1,2})[/\-.](\d{4})"""),
        // 10: MM/YY or MM-YY or MM.YY (e.g., 12/25, 03-27)
        Regex("""(\d{1,2})[/\-.](\d{2})\b"""),
        // 11: DDMMYYYY (no separators)
        Regex("""(\d{2})(\d{2})(\d{4})"""),
        // 12: DDMMYY (no separators)
        Regex("""(\d{2})(\d{2})(\d{2})"""),
        // 13: DD MON (no year: 15 JAN, 3 MARCH)
        Regex("""(\d{1,2})\s+($m)\.?\s*$""", RegexOption.IGNORE_CASE),
        // 14: MON DD (no year, US: JAN 15, MARCH 3)
        Regex("""($m)\.?\s+(\d{1,2})\s*$""", RegexOption.IGNORE_CASE),
        // 15: DD/MM (no year: 15/03, 25-12)
        Regex("""(\d{1,2})[/\-.](\d{1,2})\s*$"""),
        // 16: MON only (near keyword: just "JAN" or "MARCH")
        Regex("""\b($m)\b""", RegexOption.IGNORE_CASE)
    )
}

/**
 * Extract expiry date from OCR text. Returns YYYY-MM-DD or null.
 *
 * Strategy:
 * 1. Find dates adjacent to expiry keywords (same line, next line, or previous line)
 * 2. If only one date found in text (ignoring manufacture lines), use it
 * 3. If multiple dates, pick the most future one
 */
fun extractExpiryDate(fullText: String): String? {
    val lines = fullText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return null

    // Identify ignore lines
    val ignoreLineIndices = mutableSetOf<Int>()
    for ((idx, line) in lines.withIndex()) {
        val upperLine = line.uppercase()
        if (IGNORE_KEYWORDS.any { upperLine.contains(it) }) {
            ignoreLineIndices.add(idx)
        }
    }

    // Strategy 1: Find date near expiry keyword (same line, next line, or previous line)
    for ((idx, line) in lines.withIndex()) {
        if (idx in ignoreLineIndices) continue
        val upperLine = line.uppercase()
        if (EXPIRY_KEYWORDS.any { upperLine.contains(it) }) {
            // Same line
            val date = extractFirstDate(line)
            if (date != null) return date

            // Next line
            if (idx + 1 < lines.size && (idx + 1) !in ignoreLineIndices) {
                val nextLineDate = extractFirstDate(lines[idx + 1])
                if (nextLineDate != null) return nextLineDate
            }

            // Previous line (date might be above keyword)
            if (idx - 1 >= 0 && (idx - 1) !in ignoreLineIndices) {
                val prevLineDate = extractFirstDate(lines[idx - 1])
                if (prevLineDate != null) return prevLineDate
            }

            // Two lines below (keyword, blank line, date)
            if (idx + 2 < lines.size && (idx + 2) !in ignoreLineIndices) {
                val twoLinesDate = extractFirstDate(lines[idx + 2])
                if (twoLinesDate != null) return twoLinesDate
            }
        }
    }

    // Strategy 2: Collect all dates not on ignore lines
    val allDates = mutableListOf<String>()
    for ((idx, line) in lines.withIndex()) {
        if (idx in ignoreLineIndices) continue
        allDates.addAll(extractAllDates(line))
    }

    // If exactly one date, use it (high confidence)
    if (allDates.distinct().size == 1) return allDates.first()

    // If multiple dates, pick the most future one (expiry > manufacture)
    if (allDates.isNotEmpty()) {
        val today = try { LocalDate.now().toString() } catch (_: Exception) { "2026-01-01" }
        val futureDates = allDates.distinct().filter { it >= today }
        if (futureDates.isNotEmpty()) return futureDates.max()
        // All dates are in the past — reject
        return null
    }

    return null
}

/** Extract the first valid date from a text line (includes partial patterns — used near keywords) */
private fun extractFirstDate(text: String): String? {
    return extractAllDates(text, includePartial = true).firstOrNull()
}

/**
 * Extract all valid dates from a text line as YYYY-MM-DD strings.
 * @param includePartial if true, also matches partial dates (no year, month-only).
 *        Only use this when the text is near an expiry keyword.
 */
private fun extractAllDates(text: String, includePartial: Boolean = false): List<String> {
    val results = mutableListOf<String>()
    val maxIndex = if (includePartial) DATE_PATTERNS.size else PAT_DD_MON_NOYEAR

    for (index in 0 until maxIndex) {
        val pattern = DATE_PATTERNS[index]
        for (match in pattern.findAll(text)) {
            val parsed = parseMatchToDate(index, match)
            if (parsed != null) results.add(parsed)
        }
    }

    return results.distinct()
}

/** Parse a regex match into YYYY-MM-DD. Returns null if invalid. */
private fun parseMatchToDate(patternIndex: Int, match: MatchResult): String? {
    return try {
        val g = match.groupValues

        when (patternIndex) {
            PAT_DD_MM_YYYY -> validateAndFormat(g[3].toInt(), g[2].toInt(), g[1].toInt())
            PAT_YYYY_MM_DD -> validateAndFormat(g[1].toInt(), g[2].toInt(), g[3].toInt())
            PAT_DD_MM_YY -> validateAndFormat(expandYear(g[3].toInt()), g[2].toInt(), g[1].toInt())

            PAT_DD_MON_YYYY -> {
                val month = MONTH_MAP[g[2].uppercase()] ?: return null
                validateAndFormat(g[3].toInt(), month, g[1].toInt())
            }
            PAT_DD_MON_YY -> {
                val month = MONTH_MAP[g[2].uppercase()] ?: return null
                validateAndFormat(expandYear(g[3].toInt()), month, g[1].toInt())
            }
            PAT_MON_DD_YYYY -> {
                val month = MONTH_MAP[g[1].uppercase()] ?: return null
                validateAndFormat(g[3].toInt(), month, g[2].toInt())
            }
            PAT_MON_DD_YY -> {
                val month = MONTH_MAP[g[1].uppercase()] ?: return null
                validateAndFormat(expandYear(g[3].toInt()), month, g[2].toInt())
            }
            PAT_MON_YYYY -> {
                val month = MONTH_MAP[g[1].uppercase()] ?: return null
                validateAndFormat(g[2].toInt(), month, 1)
            }
            PAT_MON_YY -> {
                val month = MONTH_MAP[g[1].uppercase()] ?: return null
                validateAndFormat(expandYear(g[2].toInt()), month, 1)
            }
            PAT_MM_YYYY -> validateAndFormat(g[2].toInt(), g[1].toInt(), 1)
            PAT_MM_YY -> {
                val month = g[1].toInt()
                if (month < 1 || month > 12) return null
                validateAndFormat(expandYear(g[2].toInt()), month, 1)
            }
            PAT_DDMMYYYY -> validateAndFormat(g[3].toInt(), g[2].toInt(), g[1].toInt())
            PAT_DDMMYY -> validateAndFormat(expandYear(g[3].toInt()), g[2].toInt(), g[1].toInt())

            // Partial dates — infer year
            PAT_DD_MON_NOYEAR -> {
                val month = MONTH_MAP[g[2].uppercase()] ?: return null
                val day = g[1].toInt()
                validateWithInferredYear(month, day)
            }
            PAT_MON_DD_NOYEAR -> {
                val month = MONTH_MAP[g[1].uppercase()] ?: return null
                val day = g[2].toInt()
                validateWithInferredYear(month, day)
            }
            PAT_DD_MM_NOYEAR -> {
                val day = g[1].toInt()
                val month = g[2].toInt()
                if (month < 1 || month > 12) return null
                if (day < 1 || day > 31) return null
                validateWithInferredYear(month, day)
            }
            PAT_MON_ONLY -> {
                val month = MONTH_MAP[g[1].uppercase()] ?: return null
                validateWithInferredYear(month, 1)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/** Expand 2-digit year to 4-digit. 00-79 -> 2000s, 80-99 -> 1900s */
private fun expandYear(shortYear: Int): Int {
    return if (shortYear >= 80) 1900 + shortYear else 2000 + shortYear
}

/**
 * For dates without a year: try current year first.
 * If that date is in the past, use next year.
 */
private fun validateWithInferredYear(month: Int, day: Int): String? {
    if (month < 1 || month > 12) return null
    if (day < 1 || day > 31) return null
    val now = LocalDate.now()
    val thisYear = now.year
    // Try this year
    val candidate = try { LocalDate.of(thisYear, month, day) } catch (_: Exception) { return null }
    val year = if (candidate.isBefore(now)) thisYear + 1 else thisYear
    // Validate the final date
    return try {
        LocalDate.of(year, month, day)
        "%04d-%02d-%02d".format(year, month, day)
    } catch (_: Exception) { null }
}

/** Validate date components and format as YYYY-MM-DD. Rejects past dates. */
private fun validateAndFormat(year: Int, month: Int, day: Int): String? {
    if (year < 2018 || year > 2050) return null
    if (month < 1 || month > 12) return null
    if (day < 1 || day > 31) return null
    return try {
        val date = LocalDate.of(year, month, day)
        if (date.isBefore(LocalDate.now())) return null
        "%04d-%02d-%02d".format(year, month, day)
    } catch (e: Exception) {
        null
    }
}
