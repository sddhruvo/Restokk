package com.inventory.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.inventory.app.ui.components.ThemedIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.TestTags
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.visuals
import kotlinx.coroutines.delay

@Composable
fun QuickAddMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (QuickAddMenuItem) -> Unit
) {
    // Track staggered visibility per item (bottom-up entrance)
    val items = quickAddMenuItems
    val itemCount = items.size
    var visibleCount by remember { mutableStateOf(0) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            visibleCount = 0
            // Stagger items bottom-up with 70ms delay
            for (i in 1..itemCount) {
                delay(70)
                visibleCount = i
            }
        } else {
            visibleCount = 0
        }
    }

    if (isVisible) {
        // Full-screen scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.QuickAdd.SCRIM)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
    }

    // Menu items positioned above the nav bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 88.dp) // above floating pill + FAB
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Render items bottom-up: last item appears first
            items.forEachIndexed { index, item ->
                // Bottom-up: item at index 0 (top) appears last
                val reverseIndex = itemCount - 1 - index
                val isItemVisible = isVisible && visibleCount > reverseIndex

                AnimatedVisibility(
                    visible = isItemVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(PaperInkMotion.DurationShort)
                    ) + fadeOut(animationSpec = tween(PaperInkMotion.DurationShort))
                ) {
                    val menuTestTag = when (item.label) {
                        "Add Item" -> TestTags.QuickAdd.MENU_ADD_ITEM
                        "Scan Barcode" -> TestTags.QuickAdd.MENU_SCAN_BARCODE
                        "Kitchen Scan" -> TestTags.QuickAdd.MENU_KITCHEN_SCAN
                        "Scan Receipt" -> TestTags.QuickAdd.MENU_SCAN_RECEIPT
                        else -> "quickAdd.menu.unknown"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(menuTestTag)
                            .clickable {
                                onItemClick(item)
                            },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = if (MaterialTheme.visuals.useElevation) 4.dp else 0.dp,
                        tonalElevation = if (MaterialTheme.visuals.useElevation) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemedIcon(
                                materialIcon = item.icon,
                                inkIconRes = item.inkIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                item.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
