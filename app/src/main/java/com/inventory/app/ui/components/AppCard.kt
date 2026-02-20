package com.inventory.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val AppCardShape = RoundedCornerShape(16.dp)

/**
 * Unified card style used across the entire app.
 *
 * - Light theme: surfaceColorAtElevation(2.dp) + 2dp elevation
 * - Dark/AMOLED theme: White@10% + 0dp elevation (no glow between adjacent cards)
 *
 * Pass [containerColor] to override the fill for semantic cards
 * (errorContainer, primaryContainer, accent tints, etc.).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    shape: Shape = AppCardShape,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f

    val fill = containerColor
        ?: if (isDark) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

    // No elevation when: dark theme default (avoids glow), OR custom color (avoids border-like gap)
    val cardElevation = if (containerColor != null || isDark) 0.dp else 2.dp

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = fill),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        content()
    }
}

/**
 * Clickable variant of [AppCard].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    shape: Shape = AppCardShape,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f

    val fill = containerColor
        ?: if (isDark) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

    // No elevation when: dark theme default (avoids glow), OR custom color (avoids border-like gap)
    val cardElevation = if (containerColor != null || isDark) 0.dp else 2.dp

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = fill),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        content()
    }
}
