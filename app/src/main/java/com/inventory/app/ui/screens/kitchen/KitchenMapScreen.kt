package com.inventory.app.ui.screens.kitchen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.ui.components.AnimatedEmptyState
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.formatQty
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.CardOrange
import com.inventory.app.util.CategoryVisuals
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val COLLAPSE_THRESHOLD = 8

// ─── Paper & Ink Spring Presets ─────────────────────────────────────────
private val BouncySpring = spring<Float>(dampingRatio = 0.5f, stiffness = 200f)
private val GentleSpring = spring<Float>(dampingRatio = 1.0f, stiffness = 200f)
private val WobblySpring = spring<Float>(dampingRatio = 0.3f, stiffness = 200f)
private const val STAGGER_DELAY_MS = 70L
private const val MAX_STAGGER_ITEMS = 5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KitchenMapScreen(
    navController: NavController,
    viewModel: KitchenMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Kitchen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.totalItems == 0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedEmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = "Your Kitchen is Empty",
                        message = "Scan your fridge, pantry, and shelves to see everything mapped here.",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = {
                        navController.navigate(Screen.FridgeScan.route)
                    }) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Kitchen Tour")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scan CTA card — Write-In entrance
                    WriteInAnimatedItem(index = 0) {
                        ScanCtaCard(onClick = {
                            navController.navigate(Screen.FridgeScan.route)
                        })
                    }

                    // Zone cards — staggered Write-In entrance
                    uiState.zones.forEachIndexed { index, zone ->
                        WriteInAnimatedItem(index = index + 1) {
                            ZoneCard(
                                zone = zone,
                                onItemClick = { item ->
                                    navController.navigate(
                                        Screen.ItemDetail.createRoute(item.item.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Write-In Entrance (Paper & Ink primary entrance) ───────────────────

@Composable
private fun WriteInAnimatedItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(-20f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(MAX_STAGGER_ITEMS.toInt()) * STAGGER_DELAY_MS)
        // Parallel: BouncySpring for position, GentleSpring for opacity
        launch { offsetX.animateTo(0f, BouncySpring) }
        launch { alpha.animateTo(1f, GentleSpring) }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX.value * density
            this.alpha = alpha.value
        }
    ) {
        content()
    }
}

// ─── Write-In for Chips (smaller offset, faster stagger) ────────────────

@Composable
private fun WriteInChip(
    index: Int,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(-14f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val cappedIndex = index.coerceAtMost(MAX_STAGGER_ITEMS.toInt())
        delay(cappedIndex * STAGGER_DELAY_MS)
        launch { offsetX.animateTo(0f, BouncySpring) }
        launch { alpha.animateTo(1f, GentleSpring) }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX.value * density
            this.alpha = alpha.value
        }
    ) {
        content()
    }
}

// ─── Scan CTA Card ──────────────────────────────────────────────────────

@Composable
private fun ScanCtaCard(onClick: () -> Unit) {
    // Breathing animation on camera icon
    val breathing = rememberInfiniteTransition(label = "ctaBreathe")
    val breathScale = breathing.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ctaBreathScale"
    )

    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = breathScale.value
                        scaleY = breathScale.value
                    },
                tint = CardOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Scan to Add Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Take a photo of any area",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Go to scan",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Wobbly Shelf Lines (hand-drawn feel) ───────────────────────────────

@Composable
private fun WobblyShelfLines(modifier: Modifier = Modifier) {
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    Canvas(modifier = modifier) {
        val lineColor = Color.Black.copy(alpha = 0.07f)
        val lineCount = 3
        for (i in 1..lineCount) {
            val baseY = size.height * i / (lineCount + 1)
            val path = Path().apply {
                val segments = 6
                val segWidth = (size.width - 32f) / segments
                moveTo(16f, baseY)
                for (s in 1..segments) {
                    val endX = 16f + segWidth * s
                    val wobble = sin((s + wobbleSeed + i * 2.7) * 1.6).toFloat() * 3f
                    val ctrlX = 16f + segWidth * (s - 0.5f)
                    val ctrlWobble = sin((s + wobbleSeed + i * 1.3) * 2.3 + PI / 3).toFloat() * 4.5f
                    quadraticBezierTo(ctrlX, baseY + ctrlWobble, endX, baseY + wobble)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 1.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

// ─── Pen Stroke Reveal (thin line draws itself before content) ──────────

@Composable
private fun PenStrokeReveal(
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(100)
        progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val lineWidth = size.width * progress.value
        if (lineWidth > 0f) {
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2f),
                end = Offset(lineWidth, size.height / 2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

// ─── Zone Card ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneCard(
    zone: KitchenZone,
    onItemClick: (ItemWithDetails) -> Unit
) {
    var expanded by rememberSaveable(zone.locationId) {
        mutableStateOf(zone.itemCount < COLLAPSE_THRESHOLD)
    }

    // Chevron rotation animation
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "chevron"
    )

    // Count badge spring-in
    val badgeScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(200)
        badgeScale.animateTo(1f, WobblySpring)
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = zone.tintColor.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            // Wobbly shelf lines for Fridge/Pantry
            if (zone.hasShelfLines) {
                WobblyShelfLines(modifier = Modifier.matchParentSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f))
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zone icon with subtle Land entrance
                    ZoneIconAnimated(zone.icon)

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        zone.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    // Count badge — springs in
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .graphicsLayer {
                                scaleX = badgeScale.value
                                scaleY = badgeScale.value
                            }
                    ) {
                        Text(
                            "${zone.itemCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (zone.itemCount >= COLLAPSE_THRESHOLD) {
                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = chevronRotation
                                }
                            )
                        }
                    }
                }

                // Pen stroke divider
                PenStrokeReveal(
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (zone.itemCount == 0) {
                    // Floating "No items yet" — alive at rest
                    FloatingText("No items yet")
                } else {
                    val collapsedItems = zone.items.take(COLLAPSE_THRESHOLD)
                    val overflowItems = zone.items.drop(COLLAPSE_THRESHOLD)

                    // Always-visible items (first COLLAPSE_THRESHOLD)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        collapsedItems.forEachIndexed { chipIndex, item ->
                            WriteInChip(index = chipIndex) {
                                ItemChip(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }

                    // Overflow items — only visible when expanded
                    if (overflowItems.isNotEmpty()) {
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                overflowItems.forEachIndexed { chipIndex, item ->
                                    WriteInChip(index = chipIndex + COLLAPSE_THRESHOLD) {
                                        ItemChip(
                                            item = item,
                                            onClick = { onItemClick(item) }
                                        )
                                    }
                                }
                            }
                        }
                        if (!expanded) {
                            AssistChip(
                                onClick = { expanded = true },
                                label = {
                                    Text(
                                        "+${overflowItems.size} more",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Zone Icon with "Land" Entrance ─────────────────────────────────────

@Composable
private fun ZoneIconAnimated(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val scale = remember { Animatable(0.3f) }
    val offsetY = remember { Animatable(-30f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, BouncySpring) }
        launch { offsetY.animateTo(0f, BouncySpring) }
    }

    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationY = offsetY.value * density
            },
        tint = MaterialTheme.colorScheme.onSurface
    )
}

// ─── Floating Text (idle animation for empty zones) ─────────────────────

@Composable
private fun FloatingText(text: String) {
    val floating = rememberInfiniteTransition(label = "floating")
    val translateY = floating.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.graphicsLayer {
            translationY = translateY.value * density
        }
    )
}

// ─── Item Chip ──────────────────────────────────────────────────────────

@Composable
private fun ItemChip(
    item: ItemWithDetails,
    onClick: () -> Unit
) {
    val categoryName = item.category?.name ?: ""
    val categoryColor = CategoryVisuals.get(categoryName).color
    val qtyText = if (item.item.quantity > 0) " x${item.item.quantity.formatQty()}" else ""

    AssistChip(
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category color dot
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = categoryColor)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    item.item.name + qtyText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = null
    )
}
