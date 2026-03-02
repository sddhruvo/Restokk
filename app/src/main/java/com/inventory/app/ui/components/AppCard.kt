package com.inventory.app.ui.components

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
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.CardStyle
import com.inventory.app.ui.theme.visuals

/**
 * Unified card style used across the entire app.
 *
 * Automatically adapts to the current [VisualStyle]:
 * - Modern: Material3 Card with elevation
 * - Paper & Ink: InkBorderCard with wobble-bezier border
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    val visuals = MaterialTheme.visuals

    when (visuals.cardStyle) {
        is CardStyle.Standard -> {
            StandardCard(
                modifier = modifier,
                containerColor = containerColor,
                shape = shape,
                content = content
            )
        }
        is CardStyle.InkBorder -> {
            InkBorderCard(
                modifier = modifier,
                containerColor = containerColor,
                inkBorder = visuals.cardStyle,
            ) {
                content()
            }
        }
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
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    val visuals = MaterialTheme.visuals

    when (visuals.cardStyle) {
        is CardStyle.Standard -> {
            StandardClickableCard(
                onClick = onClick,
                modifier = modifier,
                containerColor = containerColor,
                shape = shape,
                content = content
            )
        }
        is CardStyle.InkBorder -> {
            InkBorderCard(
                modifier = modifier,
                containerColor = containerColor,
                inkBorder = visuals.cardStyle,
                onClick = onClick,
            ) {
                content()
            }
        }
    }
}

// ── Internal Material3 card implementations ─────────────────────────────

@Composable
private fun StandardCard(
    modifier: Modifier,
    containerColor: Color?,
    shape: Shape,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val fill = containerColor
        ?: if (isDark) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardClickableCard(
    onClick: () -> Unit,
    modifier: Modifier,
    containerColor: Color?,
    shape: Shape,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val fill = containerColor
        ?: if (isDark) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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
