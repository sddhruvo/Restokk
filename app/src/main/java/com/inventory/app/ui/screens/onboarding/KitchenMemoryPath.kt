package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.inventory.app.ui.theme.sectionHeader
import com.inventory.app.ui.theme.statValue
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.ThemedCircularProgress
import com.inventory.app.ui.components.AnimatedCounter
import com.inventory.app.ui.components.InkFireworks
import com.inventory.app.ui.components.RuledLinesBackground
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.visuals
import com.inventory.app.util.FormatUtils
import kotlinx.coroutines.delay

private val BouncySpring = PaperInkMotion.BouncySpring
private val WobblySpring = PaperInkMotion.WobblySpring
private val GentleSpring = PaperInkMotion.GentleSpring

// ═══════════════════════════════════════════════════════════════════════════
// Kitchen Memory Selection — grid of 15 common items
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun KitchenMemoryPath(
    memoryItems: List<MemoryGridItem>,
    selectedCount: Int,
    isSaving: Boolean,
    onToggleItem: (Int) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current

    // ── Entrance animations ──
    var headerReady by remember { mutableStateOf(reduceMotion) }
    var subtitleReady by remember { mutableStateOf(reduceMotion) }
    var gridReady by remember { mutableStateOf(reduceMotion) }
    var showSkipHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(200)
        headerReady = true
        delay(300)
        subtitleReady = true
        delay(200)
        gridReady = true
    }

    // 10s timeout for skip hint if 0 selections
    LaunchedEffect(selectedCount) {
        if (selectedCount == 0) {
            delay(10000)
            showSkipHint = true
        } else {
            showSkipHint = false
        }
    }

    // Header: Write-In
    val headerX by animateFloatAsState(
        targetValue = if (headerReady) 0f else -20f,
        animationSpec = BouncySpring, label = "memHeaderX"
    )
    val headerAlpha by animateFloatAsState(
        targetValue = if (headerReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "memHeaderAlpha"
    )

    // Subtitle: Fade Up
    val subY by animateFloatAsState(
        targetValue = if (subtitleReady) 0f else 12f,
        animationSpec = GentleSpring, label = "memSubY"
    )
    val subAlpha by animateFloatAsState(
        targetValue = if (subtitleReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "memSubAlpha"
    )

    // Confirm button visibility
    val showConfirm = selectedCount >= 3
    val confirmAlpha by animateFloatAsState(
        targetValue = if (showConfirm) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "confirmAlpha"
    )
    val confirmY by animateFloatAsState(
        targetValue = if (showConfirm) 0f else 20f,
        animationSpec = GentleSpring, label = "confirmY"
    )

    // Confirm button breathing
    val breathe = rememberInfiniteTransition(label = "confirmBreathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "breatheScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RuledLinesBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Text(
                text = "No worries! Tap what you know\nis in your kitchen.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = headerX; alpha = headerAlpha
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Go from memory \u2014 you'd be surprised\nhow much you remember.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationY = subY; alpha = subAlpha
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Item grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(memoryItems, key = { _, item -> item.name }) { index, item ->
                    MemoryItemCard(
                        item = item,
                        entranceIndex = index,
                        visible = gridReady,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleItem(index)
                        }
                    )
                }
            }

            // Counter
            if (selectedCount > 0) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedCounter(targetValue = selectedCount) { count ->
                        Text(
                            text = count,
                            style = MaterialTheme.typography.sectionHeader,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = " item${if (selectedCount != 1) "s" else ""} remembered",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Confirm button (visible when 3+ selected)
            if (showConfirm || confirmAlpha > 0.01f) {
                ThemedButton(
                    onClick = onConfirm,
                    enabled = !isSaving && showConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer {
                            alpha = confirmAlpha
                            translationY = confirmY
                            val bs = if (showConfirm) breatheScale else 1f
                            scaleX = bs; scaleY = bs
                        },
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isSaving) {
                        ThemedCircularProgress(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("That's my kitchen!", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Skip hint (after 10s with 0 selections)
            if (showSkipHint) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "Not sure? You can always add items later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Memory Item Card — single item in the 3-column grid
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MemoryItemCard(
    item: MemoryGridItem,
    entranceIndex: Int,
    visible: Boolean,
    onClick: () -> Unit
) {
    // Staggered Land (70ms, cap 5 items — rest instant)
    var landed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            val staggerDelay = if (entranceIndex < 5) entranceIndex * 70L else 350L
            delay(staggerDelay)
            landed = true
        }
    }
    val landScale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.5f,
        animationSpec = BouncySpring, label = "memLand$entranceIndex"
    )
    val landAlpha by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = tween(250), label = "memFade$entranceIndex"
    )

    // Selection bounce
    val selectionScale by animateFloatAsState(
        targetValue = if (item.isSelected) 1.08f else 1f,
        animationSpec = WobblySpring, label = "memSel$entranceIndex"
    )

    // Ink-wash fill
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val containerColor by animateColorAsState(
        targetValue = if (item.isSelected) lerp(surfaceColor, primaryContainerColor, 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(200), label = "memColor$entranceIndex"
    )

    // Checkmark
    val checkScale by animateFloatAsState(
        targetValue = if (item.isSelected) 1f else 0f,
        animationSpec = if (item.isSelected) WobblySpring else tween(100),
        label = "memCheck$entranceIndex"
    )

    val icon = getItemIcon(item.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = landScale * selectionScale
                scaleY = landScale * selectionScale
                alpha = landAlpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.name,
                    modifier = Modifier.size(28.dp),
                    tint = if (item.isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = if (item.isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Checkmark in top-right
            if (checkScale > 0.01f) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Check,
                    inkIconRes = R.drawable.ic_ink_check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .graphicsLayer { scaleX = checkScale; scaleY = checkScale; alpha = checkScale }
                )
            }
        }
    }
}

/** Material outlined icons for the 15 memory items. */
private fun getItemIcon(name: String): ImageVector = when (name.lowercase()) {
    "milk" -> Icons.Outlined.LocalDrink
    "eggs" -> Icons.Outlined.Egg
    "bread" -> Icons.Outlined.BakeryDining
    "rice" -> Icons.Outlined.RiceBowl
    "pasta" -> Icons.Outlined.DinnerDining
    "chicken" -> Icons.Outlined.SetMeal
    "tomatoes" -> Icons.Outlined.Grass
    "onions" -> Icons.Outlined.Grass
    "garlic" -> Icons.Outlined.Grass
    "salt" -> Icons.Outlined.Science
    "olive oil" -> Icons.Outlined.WaterDrop
    "butter" -> Icons.Outlined.Icecream
    "cheese" -> Icons.Outlined.LunchDining
    "apples" -> Icons.Outlined.Spa
    "coffee" -> Icons.Outlined.Coffee
    else -> Icons.Outlined.Inventory2
}

// ═══════════════════════════════════════════════════════════════════════════
// Memory Aggregate Reveal
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun MemoryAggregateReveal(
    items: List<RevealItem>,
    onRevealComplete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // ── Animation phases ──
    var cardReady by remember { mutableStateOf(false) }
    var headlineReady by remember { mutableStateOf(false) }
    var listReady by remember { mutableStateOf(false) }
    var statsReady by remember { mutableStateOf(false) }
    var flavorReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)                                          // T+400: card scales in
        cardReady = true
        delay(900)                                          // T+1300: headline writes in
        headlineReady = true
        delay(800)                                          // T+2100: rows start staggering
        listReady = true
        delay(items.size.coerceAtMost(5) * 500L + 600)     // rows: 500ms each + settle
        statsReady = true
        delay(1000)                                         // time to read stats
        flavorReady = true
        delay(800)
        onRevealComplete()
    }

    val cardScale by animateFloatAsState(
        targetValue = if (cardReady) 1f else 0.85f,
        animationSpec = BouncySpring, label = "aggCardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (cardReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "aggCardAlpha"
    )

    val headX by animateFloatAsState(
        targetValue = if (headlineReady) 0f else -20f,
        animationSpec = BouncySpring, label = "aggHeadX"
    )
    val headAlpha by animateFloatAsState(
        targetValue = if (headlineReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "aggHeadAlpha"
    )

    val statsY by animateFloatAsState(
        targetValue = if (statsReady) 0f else 12f,
        animationSpec = GentleSpring, label = "statsY"
    )
    val statsAlpha by animateFloatAsState(
        targetValue = if (statsReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "statsAlpha"
    )

    val flavorY by animateFloatAsState(
        targetValue = if (flavorReady) 0f else 12f,
        animationSpec = GentleSpring, label = "aggFlavorY"
    )
    val flavorAlpha by animateFloatAsState(
        targetValue = if (flavorReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "aggFlavorAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = cardScale; scaleY = cardScale; alpha = cardAlpha },
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = if (MaterialTheme.visuals.useElevation) 4.dp else 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box {
                RuledLinesBackground(modifier = Modifier.matchParentSize())

                Column(modifier = Modifier.padding(24.dp)) {
                    // Headline
                    Text(
                        text = "From memory alone, we built this:",
                        style = MaterialTheme.typography.sectionHeader,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            translationX = headX; alpha = headAlpha
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mini-list (max 5 visible, rest summarized)
                    val visibleItems = items.take(5)
                    visibleItems.forEachIndexed { index, item ->
                        AggregateItemRow(
                            item = item,
                            index = index,
                            visible = listReady
                        )
                        if (index < visibleItems.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (items.size > 5) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+${items.size - 5} more items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pen-stroke divider
                    val underlineFraction by animateFloatAsState(
                        targetValue = if (statsReady) 1f else 0f,
                        animationSpec = tween(250), label = "aggUnderline"
                    )
                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        if (underlineFraction > 0f) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.3f),
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width * underlineFraction, size.height / 2),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats line with count-up
                    Row(
                        modifier = Modifier.graphicsLayer {
                            translationY = statsY; alpha = statsAlpha
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (statsReady) {
                            AnimatedCounter(targetValue = items.size) { count ->
                                Text(
                                    text = count,
                                    style = MaterialTheme.typography.sectionHeader,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = " items. ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AnimatedCounter(targetValue = items.size * 4) { count ->
                                Text(
                                    text = count,
                                    style = MaterialTheme.typography.sectionHeader,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = " details filled in.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "All automatic.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.graphicsLayer {
                            translationY = statsY; alpha = statsAlpha
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flavor text
        Text(
            text = "Imagine what happens when\nyou scan a real shelf.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                translationY = flavorY; alpha = flavorAlpha
            }
        )
    }
}

@Composable
private fun AggregateItemRow(
    item: RevealItem,
    index: Int,
    visible: Boolean
) {
    // Staggered Write-In (500ms each — time to read each row)
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 500L)
            appeared = true
        }
    }
    val rowX by animateFloatAsState(
        targetValue = if (appeared) 0f else -30f,
        animationSpec = BouncySpring, label = "aggRow${index}X"
    )
    val rowAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(250), label = "aggRow${index}Alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = rowX; alpha = rowAlpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = " \u2192 ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = buildString {
                item.category?.let { append(it) }
                item.location?.let {
                    if (isNotEmpty()) append(" \u2022 ")
                    append(it)
                }
                item.shelfLifeDays?.let {
                    if (isNotEmpty()) append(" \u2022 ")
                    append(FormatUtils.formatShelfLife(it))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Memory Celebration — final screen before Dashboard
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun MemoryCelebrationScreen(
    itemCount: Int,
    onDone: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var celebrating by remember { mutableStateOf(false) }
    var buttonReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        buttonReady = true
    }

    val btnY by animateFloatAsState(
        targetValue = if (buttonReady) 0f else 20f,
        animationSpec = GentleSpring, label = "memCelBtnY"
    )
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "memCelBtnAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Your kitchen is\nready to go.",
                style = MaterialTheme.typography.statValue,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$itemCount items added from memory alone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            ThemedButton(
                onClick = {
                    celebrating = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        translationY = btnY; alpha = btnAlpha
                    },
                shape = MaterialTheme.shapes.large
            ) {
                Text("See my kitchen \u2192", style = MaterialTheme.typography.titleMedium)
            }
        }

        // InkFireworks celebration
        if (celebrating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.Center)
            ) {
                InkFireworks()
            }
        }
    }
}
