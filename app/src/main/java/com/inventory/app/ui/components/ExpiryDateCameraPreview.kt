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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Camera preview for scanning expiry dates using ML Kit text recognition.
 * Detects date-like text in real-time and returns parsed YYYY-MM-DD strings.
 */
@Composable
fun ExpiryDateCameraPreview(
    onDateDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var lastDetectedDate: String? = null

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            boundCameraProvider = cameraProvider

            val preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val mainExecutor = ContextCompat.getMainExecutor(context)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processTextFrame(imageProxy, textRecognizer) { date ->
                            if (date != lastDetectedDate) {
                                lastDetectedDate = date
                                mainExecutor.execute { onDateDetected(date) }
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

                // Continuous center-weighted autofocus
                val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                val centerPoint = factory.createPoint(0.5f, 0.5f)
                val focusAction = FocusMeteringAction.Builder(centerPoint)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera.cameraControl.startFocusAndMetering(focusAction)
            } catch (e: Exception) {
                Log.e("ExpiryDateCamera", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraInstance = null
            boundCameraProvider?.unbindAll()
            textRecognizer.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Visual scan guide overlay (smaller, date-label sized)
        ExpiryDateGuideOverlay()

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

        // Bottom hint
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
                        materialIcon = Icons.Filled.CalendarMonth,
                        inkIconRes = R.drawable.ic_ink_calendar,
                        contentDescription = "Expiry",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Point at the expiry date",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Smaller scan guide overlay sized for date labels.
 */
@Composable
private fun ExpiryDateGuideOverlay() {
    val bracketColor = Color.White
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Smaller window for date labels: 65% width, ~3:1 aspect ratio
        val windowWidth = canvasWidth * 0.65f
        val windowHeight = windowWidth * 0.33f
        val left = (canvasWidth - windowWidth) / 2f
        val top = (canvasHeight - windowHeight) / 2f - canvasHeight * 0.05f
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

        // Corner brackets
        val bracketLen = windowWidth * 0.1f
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2f
        val right = left + windowWidth
        val bottom = top + windowHeight

        // Top-left
        drawLine(bracketColor, Offset(left + inset, top + cornerRadius), Offset(left + inset, top + bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + cornerRadius, top + inset), Offset(left + bracketLen, top + inset), strokeWidth, StrokeCap.Round)

        // Top-right
        drawLine(bracketColor, Offset(right - inset, top + cornerRadius), Offset(right - inset, top + bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(right - cornerRadius, top + inset), Offset(right - bracketLen, top + inset), strokeWidth, StrokeCap.Round)

        // Bottom-left
        drawLine(bracketColor, Offset(left + inset, bottom - cornerRadius), Offset(left + inset, bottom - bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + cornerRadius, bottom - inset), Offset(left + bracketLen, bottom - inset), strokeWidth, StrokeCap.Round)

        // Bottom-right
        drawLine(bracketColor, Offset(right - inset, bottom - cornerRadius), Offset(right - inset, bottom - bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(right - cornerRadius, bottom - inset), Offset(right - bracketLen, bottom - inset), strokeWidth, StrokeCap.Round)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLine(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    cap: StrokeCap
) {
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = cap)
}

// --- Date extraction logic ---

/** Keywords indicating an expiry date follows */
private val EXPIRY_KEYWORDS = listOf(
    "EXP", "EXPIRY", "EXPIRES", "EXPIRATION",
    "USE BY", "USEBY",
    "BEST BEFORE", "BEST BEF", "BB", "BBE", "B.B",
    "SELL BY", "SELLBY",
    "BEST END", "BEST BY",
    "CONSUME BY", "CONSUME BEFORE"
)

/** Keywords to IGNORE — these indicate manufacturing/production dates */
private val IGNORE_KEYWORDS = listOf(
    "MFG", "MFD", "MFGD", "MANUFACTURED",
    "PROD", "PRODUCTION", "PRODUCED",
    "PACKED", "PACK DATE", "PACKING",
    "LOT", "BATCH", "L:", "B:"
)

/** Month abbreviations for MMM-format dates */
private val MONTH_MAP = mapOf(
    "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4,
    "MAY" to 5, "JUN" to 6, "JUL" to 7, "AUG" to 8,
    "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12
)

// Regex patterns for date formats
private val DATE_PATTERNS = listOf(
    // DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
    Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})"""),
    // DD/MM/YY or DD-MM-YY or DD.MM.YY
    Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})"""),
    // YYYY-MM-DD or YYYY/MM/DD
    Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})"""),
    // MM/YYYY or MM-YYYY or MM.YYYY
    Regex("""(\d{1,2})[/\-.](\d{4})"""),
    // DD MMM YYYY (e.g., 15 JAN 2025)
    Regex("""(\d{1,2})\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+(\d{4})""", RegexOption.IGNORE_CASE),
    // MMM YYYY (e.g., JAN 2025)
    Regex("""(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+(\d{4})""", RegexOption.IGNORE_CASE),
    // MMM YY (e.g., JAN 25)
    Regex("""(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+(\d{2})""", RegexOption.IGNORE_CASE)
)

/**
 * Extract expiry date from OCR text. Returns YYYY-MM-DD or null.
 *
 * Strategy:
 * 1. Find dates adjacent to expiry keywords
 * 2. Check next line after keyword line
 * 3. If only one date found in entire text, use it as fallback
 */
fun extractExpiryDate(fullText: String): String? {
    val lines = fullText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val upperText = fullText.uppercase()

    // Check if text is near an IGNORE keyword line — skip those dates
    val ignoreLineIndices = mutableSetOf<Int>()
    for ((idx, line) in lines.withIndex()) {
        val upperLine = line.uppercase()
        if (IGNORE_KEYWORDS.any { upperLine.contains(it) }) {
            ignoreLineIndices.add(idx)
        }
    }

    // Strategy 1: Find date on same line as expiry keyword
    for ((idx, line) in lines.withIndex()) {
        if (idx in ignoreLineIndices) continue
        val upperLine = line.uppercase()
        if (EXPIRY_KEYWORDS.any { upperLine.contains(it) }) {
            val date = extractFirstDate(line)
            if (date != null) return date

            // Strategy 2: Check next line
            if (idx + 1 < lines.size && (idx + 1) !in ignoreLineIndices) {
                val nextLineDate = extractFirstDate(lines[idx + 1])
                if (nextLineDate != null) return nextLineDate
            }
        }
    }

    // Strategy 3: Fallback — if exactly one date in entire text (not on ignore lines)
    val allDates = mutableListOf<String>()
    for ((idx, line) in lines.withIndex()) {
        if (idx in ignoreLineIndices) continue
        val dates = extractAllDates(line)
        allDates.addAll(dates)
    }
    if (allDates.size == 1) return allDates.first()

    return null
}

/** Extract the first valid date from a text line, returns YYYY-MM-DD or null */
private fun extractFirstDate(text: String): String? {
    return extractAllDates(text).firstOrNull()
}

/** Extract all valid dates from a text line as YYYY-MM-DD strings */
private fun extractAllDates(text: String): List<String> {
    val results = mutableListOf<String>()

    for (pattern in DATE_PATTERNS) {
        for (match in pattern.findAll(text)) {
            val parsed = parseMatchToDate(pattern, match)
            if (parsed != null) results.add(parsed)
        }
    }

    return results.distinct()
}

/** Parse a regex match into YYYY-MM-DD. Returns null if invalid. */
private fun parseMatchToDate(pattern: Regex, match: MatchResult): String? {
    return try {
        val groups = match.groupValues

        when {
            // DD/MM/YYYY
            pattern == DATE_PATTERNS[0] -> {
                val day = groups[1].toInt()
                val month = groups[2].toInt()
                val year = groups[3].toInt()
                validateAndFormat(year, month, day)
            }
            // DD/MM/YY
            pattern == DATE_PATTERNS[1] -> {
                val day = groups[1].toInt()
                val month = groups[2].toInt()
                val shortYear = groups[3].toInt()
                val year = if (shortYear >= 70) 1900 + shortYear else 2000 + shortYear
                validateAndFormat(year, month, day)
            }
            // YYYY-MM-DD
            pattern == DATE_PATTERNS[2] -> {
                val year = groups[1].toInt()
                val month = groups[2].toInt()
                val day = groups[3].toInt()
                validateAndFormat(year, month, day)
            }
            // MM/YYYY
            pattern == DATE_PATTERNS[3] -> {
                val month = groups[1].toInt()
                val year = groups[2].toInt()
                validateAndFormat(year, month, 1)
            }
            // DD MMM YYYY
            pattern == DATE_PATTERNS[4] -> {
                val day = groups[1].toInt()
                val month = MONTH_MAP[groups[2].uppercase()] ?: return null
                val year = groups[3].toInt()
                validateAndFormat(year, month, day)
            }
            // MMM YYYY
            pattern == DATE_PATTERNS[5] -> {
                val month = MONTH_MAP[groups[1].uppercase()] ?: return null
                val year = groups[2].toInt()
                validateAndFormat(year, month, 1)
            }
            // MMM YY
            pattern == DATE_PATTERNS[6] -> {
                val month = MONTH_MAP[groups[1].uppercase()] ?: return null
                val shortYear = groups[2].toInt()
                val year = if (shortYear >= 70) 1900 + shortYear else 2000 + shortYear
                validateAndFormat(year, month, 1)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/** Validate date components and format as YYYY-MM-DD */
private fun validateAndFormat(year: Int, month: Int, day: Int): String? {
    if (year < 2020 || year > 2040) return null
    if (month < 1 || month > 12) return null
    if (day < 1 || day > 31) return null
    return "%04d-%02d-%02d".format(year, month, day)
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processTextFrame(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    onDateDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val date = extractExpiryDate(visionText.text)
                if (date != null) {
                    onDateDetected(date)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExpiryDateCamera", "Text recognition failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
