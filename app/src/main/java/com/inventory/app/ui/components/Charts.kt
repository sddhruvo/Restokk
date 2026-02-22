package com.inventory.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.StockGreen
import com.inventory.app.ui.theme.StockYellow

private val chartColors = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF795548),
    Color(0xFF607D8B), Color(0xFF3F51B5), Color(0xFFCDDC39), Color(0xFF009688)
)

data class ChartEntry(val label: String, val value: Float)

@Composable
fun DonutChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return
    val total = entries.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.widthIn(min = 80.dp, max = 160.dp).aspectRatio(1f)) {
            val strokeWidth = 28f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            var startAngle = -90f
            entries.forEachIndexed { index, entry ->
                val sweep = (entry.value / total) * 360f
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEachIndexed { index, entry ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = chartColors[index % chartColors.size])
                    }
                    Text(
                        "${entry.label} (${entry.value.toDouble().formatQty()})",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalBarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    valuePrefix: String = ""
) {
    if (entries.isEmpty()) return
    val maxValue = entries.maxOf { it.value }
    if (maxValue <= 0f) return
    val barBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEachIndexed { index, entry ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "$valuePrefix${if (valuePrefix.isNotEmpty()) String.format(java.util.Locale.US, "%.2f", entry.value) else if (entry.value % 1f == 0f) entry.value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", entry.value)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(top = 4.dp)
                ) {
                    val barWidth = (entry.value / maxValue) * size.width
                    // Background
                    drawRoundRect(
                        color = barBackground,
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                    )
                    // Bar
                    drawRoundRect(
                        color = chartColors[index % chartColors.size],
                        size = Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                    )
                }
            }
        }
    }
}

data class DailyChartEntry(val label: String, val value: Float)

@Composable
fun SpendingLineChart(
    entries: List<DailyChartEntry>,
    currencySymbol: String = "",
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1f)
    val peakIndex = entries.indexOfFirst { it.value == entries.maxOf { e -> e.value } }

    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = onSurfaceColor.copy(alpha = 0.6f)
    )
    val peakStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = lineColor
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val paddingLeft = 8f
            val paddingRight = 8f
            val paddingTop = 28f
            val paddingBottom = 24f
            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            val stepX = chartWidth / (entries.size - 1).coerceAtLeast(1)

            // Calculate points
            val points = entries.mapIndexed { index, entry ->
                Offset(
                    x = paddingLeft + index * stepX,
                    y = paddingTop + chartHeight * (1f - entry.value / maxValue)
                )
            }
            if (points.isEmpty()) return@Canvas

            // Gradient fill area
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.3f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                ),
                style = Fill
            )

            // Line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Dots
            points.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = point
                )
            }

            // Peak value label
            if (entries[peakIndex].value > 0) {
                val peakText = "$currencySymbol${String.format(java.util.Locale.US, "%.0f", entries[peakIndex].value)}"
                val peakLayout = textMeasurer.measure(peakText, peakStyle)
                val peakX = (points[peakIndex].x - peakLayout.size.width / 2)
                    .coerceIn(0f, size.width - peakLayout.size.width)
                drawText(
                    textLayoutResult = peakLayout,
                    topLeft = Offset(peakX, points[peakIndex].y - peakLayout.size.height - 6f)
                )
            }

            // Date labels along bottom (show ~5 evenly spaced)
            val labelCount = minOf(entries.size, 5)
            val labelStep = (entries.size - 1) / (labelCount - 1).coerceAtLeast(1)
            for (i in 0 until labelCount) {
                val idx = (i * labelStep).coerceAtMost(entries.size - 1)
                val layout = textMeasurer.measure(entries[idx].label, labelStyle)
                val x = (points[idx].x - layout.size.width / 2)
                    .coerceIn(0f, size.width - layout.size.width)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x, paddingTop + chartHeight + 4f)
                )
            }
        }
    }
}

@Composable
fun ScoreLineChart(
    entries: List<DailyChartEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) return

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = onSurfaceColor.copy(alpha = 0.6f)
    )
    val refLineStyle = TextStyle(
        fontSize = 9.sp,
        color = onSurfaceColor.copy(alpha = 0.4f)
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val paddingLeft = 28f
            val paddingRight = 8f
            val paddingTop = 12f
            val paddingBottom = 24f
            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            val stepX = chartWidth / (entries.size - 1).coerceAtLeast(1)

            // Reference lines at 40, 60, 80
            for (refScore in listOf(40, 60, 80)) {
                val y = paddingTop + chartHeight * (1f - refScore / 100f)
                drawLine(
                    color = surfaceVariant,
                    start = Offset(paddingLeft, y),
                    end = Offset(paddingLeft + chartWidth, y),
                    strokeWidth = 1f
                )
                val refLayout = textMeasurer.measure("$refScore", refLineStyle)
                drawText(
                    textLayoutResult = refLayout,
                    topLeft = Offset(0f, y - refLayout.size.height / 2f)
                )
            }

            // Calculate points (y-axis 0-100)
            val points = entries.mapIndexed { index, entry ->
                Offset(
                    x = paddingLeft + index * stepX,
                    y = paddingTop + chartHeight * (1f - entry.value.coerceIn(0f, 100f) / 100f)
                )
            }
            if (points.isEmpty()) return@Canvas

            // Color based on last score
            val lastScore = entries.last().value
            val lineColor = when {
                lastScore >= 80 -> StockGreen
                lastScore >= 50 -> StockYellow
                else -> ExpiryRed
            }

            // Gradient fill
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.25f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                ),
                style = Fill
            )

            // Line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Dots
            points.forEach { point ->
                drawCircle(color = lineColor, radius = 4f, center = point)
            }

            // Date labels
            val labelCount = minOf(entries.size, 5)
            val labelStep = (entries.size - 1) / (labelCount - 1).coerceAtLeast(1)
            for (i in 0 until labelCount) {
                val idx = (i * labelStep).coerceAtMost(entries.size - 1)
                val layout = textMeasurer.measure(entries[idx].label, labelStyle)
                val x = (points[idx].x - layout.size.width / 2)
                    .coerceIn(0f, size.width - layout.size.width)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x, paddingTop + chartHeight + 4f)
                )
            }
        }
    }
}
