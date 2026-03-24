package com.inventory.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dot-based page indicator for the Recipe Builder pager.
 *
 * - Page 0 (title) shows a pen icon inside the dot
 * - Pages 1..N (steps) show the step number inside the dot
 * - Last page (review) shows a checkmark icon inside the dot
 * - Current page dot is filled + scaled up; others are outlined
 * - Each dot is tappable for direct navigation
 */
@Composable
fun RecipeBuilderDotIndicator(
    totalPages: Int,       // includes title + steps + review
    currentPage: Int,
    onDotTap: (page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages < 2) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { page ->
            val isActive = page == currentPage
            val label = when {
                page == 0 -> "✏"                   // title card
                page == totalPages - 1 -> "✓"       // review card
                else -> "${page}"                   // step number
            }

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.25f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "dot_scale_$page"
            )

            val size by animateDpAsState(
                targetValue = if (isActive) 26.dp else 20.dp,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "dot_size_$page"
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val onPrimary = MaterialTheme.colorScheme.onPrimary
            val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(size)
                    .scale(scale)
                    .clip(CircleShape)
                    .then(
                        if (isActive) {
                            Modifier.background(primaryColor)
                        } else {
                            Modifier
                                .background(surfaceVariant)
                                .border(1.dp, outline, CircleShape)
                        }
                    )
                    .clickable { onDotTap(page) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = if (isActive) 9.sp else 8.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
