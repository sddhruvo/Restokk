package com.inventory.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import com.inventory.app.ui.components.ThemedButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.inventory.app.ui.navigation.LocalBottomNavHeight
import com.inventory.app.ui.theme.appColors
import com.inventory.app.ui.theme.CardStyle
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.DividerStyle
import com.inventory.app.ui.theme.InkTokens
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.isInk
import com.inventory.app.ui.theme.visuals
import com.inventory.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.inventory.app.ui.theme.LocalReduceMotion
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ─── Ink Back Button (shared across all screens) ────────────────────────

@Composable
fun InkBackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        ThemedIcon(
            materialIcon = Icons.AutoMirrored.Filled.ArrowBack,
            inkIconRes = R.drawable.ic_ink_back,
            contentDescription = "Back"
        )
    }
}

// ─── Themed Snackbar Host ────────────────────────────────────────────────

/**
 * Drop-in replacement for [SnackbarHost] that uses an [InkBorderCard] container
 * with a [PaperInkMotion.BouncySpring] entrance when Paper & Ink is active.
 *
 * Modern mode delegates to the standard [SnackbarHost].
 */
@Composable
fun ThemedSnackbarHost(hostState: SnackbarHostState) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        SnackbarHost(hostState)
        return
    }

    val reduceMotion = LocalReduceMotion.current

    SnackbarHost(hostState) { snackbarData ->
        val containerColor = MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = InkTokens.fillOpaque)
        val contentColor = MaterialTheme.colorScheme.onSurface
        val actionColor = MaterialTheme.colorScheme.primary

        AnimatedVisibility(
            visible = true,
            enter = if (reduceMotion) fadeIn(tween(0))
                    else scaleIn(
                        initialScale = 1.05f,
                        animationSpec = PaperInkMotion.BouncySpring
                    ) + fadeIn(tween(PaperInkMotion.DurationShort)),
            exit = fadeOut(tween(PaperInkMotion.DurationQuick)) +
                   slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            InkBorderCard(
                containerColor = containerColor,
                inkBorder = CardStyle.InkBorder(
                    wobbleAmplitude = InkTokens.wobbleSmall,
                    strokeWidth = InkTokens.strokeMedium,
                    segments = 4
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snackbarData.visuals.message,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    snackbarData.visuals.actionLabel?.let { label ->
                        TextButton(
                            onClick = { snackbarData.performAction() },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = actionColor,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Themed Top App Bar ─────────────────────────────────────────────────

/**
 * Drop-in replacement for [TopAppBar] that forces transparency in Paper & Ink mode
 * so the paper texture flows seamlessly under the header.
 *
 * Modern mode delegates to the standard [TopAppBar].
 *
 * Pass explicit [colors] to override the transparent default (e.g., selection mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val isInk = MaterialTheme.visuals.isInk
    val resolvedColors = colors ?: if (isInk) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    } else {
        TopAppBarDefaults.topAppBarColors()
    }
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = resolvedColors,
        scrollBehavior = scrollBehavior
    )
}

// ─── Themed Scaffold (auto-transparent in Paper & Ink) ──────────────────

/**
 * Drop-in replacement for [Scaffold] that respects the theme's background system.
 *
 * - Paper & Ink: containerColor = Transparent → root [ThemedBackground] texture shows through
 * - Modern: containerColor = background (standard Material behavior)
 *
 * Includes an "Overscroll Lift" mechanism: when a child scrollable (LazyColumn,
 * Column.verticalScroll, etc.) reaches its bottom and the user keeps scrolling,
 * the entire content translates upward to reveal items hidden behind the floating
 * bottom nav bar. Scrolling back up reverses the lift before resuming normal scroll.
 * Zero per-screen changes required — works automatically via nested scroll.
 *
 * Pass an explicit [containerColor] to override the theme default on a per-screen basis.
 */
@Composable
fun ThemedScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit
) {
    val visuals = MaterialTheme.visuals
    val resolvedContainer = when {
        containerColor != Color.Unspecified -> containerColor
        visuals.scaffoldTransparent -> Color.Transparent
        else -> MaterialTheme.colorScheme.background
    }
    val resolvedContent = if (contentColor != Color.Unspecified) {
        contentColor
    } else {
        contentColorFor(resolvedContainer)
    }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = resolvedContainer,
        contentColor = resolvedContent,
        // Outer Scaffold (MainActivity) already handles system insets + bottom nav.
        // Zero insets here prevents double-padding on nested screens.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content
    )
}

// ─── Journal Page Layout System ──────────────────────────────────────────

/**
 * Floating back button + action icons overlay for [PageScaffold].
 *
 * Renders a subtle gradient scrim so icons stay readable when content scrolls behind.
 * Paper & Ink: warm paper background scrim. Modern: Material background scrim.
 *
 * Private — used only through [PageScaffold].
 */
@Composable
private fun FloatingNavBar(
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val isInk = MaterialTheme.visuals.isInk
    val scrimBase = if (isInk) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Gradient scrim behind icons
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to scrimBase.copy(alpha = 0.92f),
                            0.6f to scrimBase.copy(alpha = 0.4f),
                            1f to Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingXs, vertical = Dimens.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                InkBackButton(onClick = onBack)
            } else {
                // Maintain layout spacing even without back button
                Spacer(modifier = Modifier.width(48.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            actions()
        }

        // 16dp soft fade zone below the row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.spacingLg)
                .align(Alignment.BottomCenter)
        )
    }
}

/**
 * Drop-in replacement for [ThemedScaffold] on journal-style screens.
 *
 * Removes the rigid top bar — the back button and actions float as a
 * lightweight overlay while the title becomes part of the scrollable content
 * (via [PageHeader]). Every screen feels like opening a page in a journal.
 *
 * @param onBack Back navigation callback. Pass `null` for screens with no back button.
 * @param actions Action icons for the floating overlay (e.g., edit, delete, favorite).
 */
@Composable
fun PageScaffold(
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit
) {
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    ThemedScaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        bottomBar = bottomBar,
        containerColor = containerColor
    ) { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Content area — gets top padding = scaffold top + overlay height
            val overlayHeightDp = with(density) { overlayHeightPx.toDp() }
            val adjustedPadding = PaddingValues(
                top = scaffoldPadding.calculateTopPadding() + overlayHeightDp,
                bottom = scaffoldPadding.calculateBottomPadding(),
                start = scaffoldPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = scaffoldPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
            )
            content(adjustedPadding)

            // Floating overlay on top
            FloatingNavBar(
                onBack = onBack,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .onGloballyPositioned { coordinates ->
                        overlayHeightPx = coordinates.size.height
                    }
            )
        }
    }
}

/**
 * Large inline title for journal-style screens. Used as the first item
 * in a [PageScaffold]'s LazyColumn/Column — scrolls with content.
 *
 * Paper & Ink: handwritten headlineSmall with wobbly ink divider.
 * Modern: standard headlineSmall with clean divider.
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spacingLg)
    ) {
        Spacer(modifier = Modifier.height(Dimens.spacingSm))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingXs)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spacingSm))
        ThemedDivider()
        Spacer(modifier = Modifier.height(Dimens.spacingMd))
    }
}

// ─── Collapsing Page Scaffold ────────────────────────────────────────────

/**
 * Journal-page scaffold with a **parallax collapsing header**.
 *
 * Two layers at different "depths" create dramatic compression:
 * - **Icon strip** (upper layer): glides at 0.4× scroll speed, pins at top
 * - **Title** (lower layer): moves at 1.0× scroll speed + "Ink Retreat"
 *   (scaleY squish from top + alpha fade — title dissolves like ink
 *   absorbed back into paper)
 *
 * Expanded ≈ 152dp → Collapsed ≈ 76dp (≈76dp compression).
 *
 * Works automatically with any scrollable child (LazyColumn, LazyVerticalGrid,
 * Column.verticalScroll) via [NestedScrollConnection] — zero per-screen wiring.
 *
 * @param title Large title in the collapsible area.
 * @param onBack Back navigation callback. `null` = no back button (uses spacer).
 * @param actions Action icons for the always-visible bar.
 * @param subtitle Optional subtitle below the title.
 */
@Composable
fun CollapsingPageScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current

    // Parallax rate — strip moves slower than scroll to create depth
    val parallaxRate = if (reduceMotion) 1.0f else 0.4f

    // Measured heights (px) — captured once via onGloballyPositioned
    var stripHeightPx by remember { mutableIntStateOf(0) }
    var titleHeightPx by remember { mutableIntStateOf(0) }
    val hasMeasured = stripHeightPx > 0 && titleHeightPx > 0

    // Collapse range = full title height (strip stays, title collapses away)
    val collapseRangePx = titleHeightPx

    // Scroll-driven offset: 0 = expanded, -collapseRangePx = fully collapsed
    var heightOffsetPx by remember { mutableFloatStateOf(0f) }

    // Derived fraction: 0 = expanded, 1 = collapsed
    val collapseFraction = if (collapseRangePx > 0) {
        (-heightOffsetPx / collapseRangePx).coerceIn(0f, 1f)
    } else 0f

    // ── Ink Retreat: title completes fade at ~67% collapse ──
    val titleFraction = (collapseFraction * 1.5f).coerceIn(0f, 1f)
    val titleAlpha = if (reduceMotion) {
        if (collapseFraction > 0.5f) 0f else 1f
    } else {
        1f - titleFraction
    }
    val titleScaleY = if (reduceMotion) 1f else 1f - 0.7f * titleFraction

    // Paper shadow between strip and title — fades as they merge
    val shadowAlpha = if (reduceMotion) 0f else (0.08f * (1f - collapseFraction)).coerceIn(0f, 0.08f)
    val shadowColor = MaterialTheme.colorScheme.onSurface

    // Ink residue line — fades IN as title's ThemedDivider dissolves
    val residueAlpha = if (reduceMotion) {
        if (collapseFraction > 0.5f) 1f else 0f
    } else {
        collapseFraction
    }

    // ── Strip padding: shrinks from 4dp → 1dp as header collapses ──
    val stripVerticalPadding = lerp(Dimens.spacingXs, 1.dp, collapseFraction)

    // ── Positional math (px) ──
    // Strip: glides at parallaxRate, pins at y=0 (starts at y=0 in expanded)
    val stripTranslationY = if (hasMeasured) {
        max(heightOffsetPx * parallaxRate, 0f)
    } else {
        0f
    }
    // Title: sits below strip, moves 1:1 with scroll
    val titleTranslationY = if (hasMeasured) {
        stripHeightPx.toFloat() + heightOffsetPx
    } else {
        stripHeightPx.toFloat()
    }
    // Content top: follows title bottom, but never goes above strip bottom
    val contentTopPx = if (hasMeasured) {
        max(
            (stripHeightPx + titleHeightPx).toFloat() + heightOffsetPx,
            stripHeightPx.toFloat()
        )
    } else {
        (stripHeightPx + titleHeightPx).toFloat()
    }
    val contentTopDp = with(density) { contentTopPx.toDp() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Read titleHeightPx STATE directly (not captured val)
                // so range stays current after onGloballyPositioned
                val range = titleHeightPx
                if (available.y < 0f && range > 0) {
                    val prevOffset = heightOffsetPx
                    heightOffsetPx = (heightOffsetPx + available.y)
                        .coerceIn(-range.toFloat(), 0f)
                    val consumed = heightOffsetPx - prevOffset
                    if (consumed != 0f) return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val range = titleHeightPx
                if (available.y > 0f && range > 0) {
                    val prevOffset = heightOffsetPx
                    heightOffsetPx = (heightOffsetPx + available.y)
                        .coerceIn(-range.toFloat(), 0f)
                    val consumed2 = heightOffsetPx - prevOffset
                    if (consumed2 != 0f) return Offset(0f, consumed2)
                }
                return Offset.Zero
            }
        }
    }

    val scrimBase = MaterialTheme.colorScheme.background
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    ThemedScaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        bottomBar = bottomBar,
        containerColor = containerColor
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // Z=1 — Content layer (dynamic top padding)
            content(
                PaddingValues(
                    top = scaffoldPadding.calculateTopPadding() + contentTopDp,
                    bottom = scaffoldPadding.calculateBottomPadding(),
                    start = scaffoldPadding.calculateLeftPadding(
                        androidx.compose.ui.unit.LayoutDirection.Ltr
                    ),
                    end = scaffoldPadding.calculateRightPadding(
                        androidx.compose.ui.unit.LayoutDirection.Ltr
                    )
                )
            )

            // Z=2 — Title layer (1.0× speed + Ink Retreat)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        if (titleHeightPx == 0) {
                            titleHeightPx = coordinates.size.height
                        }
                    }
                    .graphicsLayer {
                        translationY = titleTranslationY
                        alpha = if (hasMeasured) titleAlpha else 0f
                        scaleY = titleScaleY
                        transformOrigin = TransformOrigin(0.5f, 0f) // scale from top edge
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingLg)
                ) {
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Dimens.spacingXs)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.spacingSm))
                    ThemedDivider()
                    Spacer(modifier = Modifier.height(Dimens.spacingMd))
                }
            }

            // Z=3 — Paper shadow (depth cue between strip and title)
            if (shadowAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .graphicsLayer {
                            translationY = stripTranslationY + stripHeightPx
                        }
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to shadowColor.copy(alpha = shadowAlpha),
                                    1f to Color.Transparent
                                )
                            )
                        )
                )
            }

            // Z=4 — Icon strip + scrim (0.4× speed, pins at y=0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        stripHeightPx = coordinates.size.height
                    }
                    .graphicsLayer {
                        translationY = stripTranslationY
                    }
            ) {
                // Scrim gradient behind icons
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to scrimBase.copy(alpha = 0.92f),
                                    0.6f to scrimBase.copy(alpha = 0.4f),
                                    1f to Color.Transparent
                                )
                            )
                        )
                )

                // Icon row — vertical padding animates 4dp→1dp on collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimens.spacingXs,
                            vertical = stripVerticalPadding
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        InkBackButton(onClick = onBack)
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    actions()
                }
            }

            // Z=5 — Ink residue line (fades IN as title dissolves)
            if (residueAlpha > 0f) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Sits just below pinned strip
                            translationY = max(stripTranslationY + stripHeightPx, stripHeightPx.toFloat())
                            alpha = residueAlpha
                        },
                    thickness = 0.75.dp,
                    color = dividerColor
                )
            }
        }
    }
}

// ─── Themed Bottom Sheet ────────────────────────────────────────────────

/**
 * Drop-in replacement for [ModalBottomSheet] with paper-toned container,
 * warm sepia scrim, and an [InkDashDragHandle] in Paper & Ink mode.
 *
 * Modern mode delegates to the standard [ModalBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val isInk = MaterialTheme.visuals.isInk
    if (isInk) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            scrimColor = Color.Black.copy(alpha = InkTokens.scrimSheet),
            dragHandle = { InkDashDragHandle() },
            content = content
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            content = content
        )
    }
}

/**
 * Hand-drawn wobbly dash used as the drag handle for [ThemedBottomSheet].
 * A subtle ink line with round caps and gentle breathing animation.
 */
@Composable
fun InkDashDragHandle(modifier: Modifier = Modifier) {
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant
        .copy(alpha = InkTokens.borderSubtle)
    val wobbleSeed = remember { (Math.random() * 1000).toFloat() }

    Canvas(
        modifier = modifier
            .padding(vertical = 12.dp)
            .size(width = InkTokens.dragHandleWidth, height = InkTokens.dragHandleHeight)
            .inkBreathe()
    ) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h / 2f)
            val midX = w / 2f
            val ctrlY = h / 2f + sin(wobbleSeed.toDouble()).toFloat() * h * 0.6f
            quadraticBezierTo(midX, ctrlY, w, h / 2f)
        }
        drawPath(
            path = path,
            color = handleColor,
            style = Stroke(
                width = InkTokens.strokeMedium.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// ─── Empty State ────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector = Icons.Filled.Inbox,
    title: String,
    message: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
        if (message.isNotEmpty()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ThemedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

// ─── Ink Spinner (ThemedCircularProgress) ──────────────────────────────

/**
 * Drop-in replacement for [CircularProgressIndicator].
 *
 * Paper & Ink mode: pen-scribble loop — a sweeping arc with round caps
 * that rotates and oscillates its sweep angle, like someone absent-mindedly
 * drawing a circle with a pen.
 *
 * Modern mode: delegates to standard [CircularProgressIndicator].
 */
@Composable
fun ThemedCircularProgress(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
) {
    val isInk = MaterialTheme.visuals.isInk
    if (!isInk) {
        CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = strokeWidth)
        return
    }

    val inkColor = MaterialTheme.colorScheme.onSurface.copy(alpha = InkTokens.borderMedium)
    val density = LocalDensity.current
    val strokePx = with(density) { InkTokens.strokeBold.toPx() }

    val transition = rememberInfiniteTransition(label = "inkSpinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "spinnerRotation"
    )
    val sweep by transition.animateFloat(
        initialValue = 30f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            tween(PaperInkMotion.DurationChart, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spinnerSweep"
    )

    Canvas(modifier = modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)) {
        val inset = strokePx / 2 + 1f
        drawArc(
            color = inkColor,
            startAngle = rotation,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ThemedCircularProgress()
    }
}

// ─── Themed Dropdown Menu ───────────────────────────────────────────────

/**
 * Drop-in replacement for [DropdownMenu] that uses a paper-toned surface
 * in Paper & Ink mode by locally overriding `colorScheme.surface`.
 *
 * Modern mode delegates to the standard [DropdownMenu].
 */
@Composable
fun ThemedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isInk = MaterialTheme.visuals.isInk
    if (isInk) {
        // Override surface color so DropdownMenu's internal Surface uses paper tone
        val tweakedColors = MaterialTheme.colorScheme.copy(
            surface = MaterialTheme.colorScheme.surfaceVariant
        )
        MaterialTheme(colorScheme = tweakedColors) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                content = content
            )
        }
    } else {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ThemedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { },
        modifier = modifier
    ) {
        ThemedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.length >= 2
            },
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError,
            supportingText = supportingText,
            singleLine = true,
            inkEndcaps = true
        )
        if (suggestions.isNotEmpty() && expanded) {
            ExposedDropdownMenu(
                expanded = true,
                onDismissRequest = { expanded = false }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    allowNone: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        ThemedTextField(
            value = selectedOption?.let { optionLabel(it) } ?: if (allowNone) "None" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            inkEndcaps = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onOptionSelected(null)
                        expanded = false
                    }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    val initialMillis = remember(value) {
        if (value.isNotBlank()) {
            try {
                LocalDate.parse(value)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            } catch (_: java.time.format.DateTimeParseException) { null }
        } else null
    }

    ThemedTextField(
        value = value,
        onValueChange = {},
        label = label,
        modifier = modifier,
        readOnly = true,
        isError = isError,
        supportingText = supportingText,
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onDateSelected("") }) {
                    ThemedIcon(materialIcon = Icons.Filled.Clear, inkIconRes = R.drawable.ic_ink_close, contentDescription = "Clear date")
                }
            } else {
                IconButton(onClick = { showPicker = true }) {
                    ThemedIcon(materialIcon = Icons.Filled.DateRange, inkIconRes = R.drawable.ic_ink_calendar, contentDescription = "Pick date")
                }
            }
        },
        placeholder = { Text("Select date") },
        interactionSource = interactionSource,
        inkEndcaps = true
    )

    androidx.compose.runtime.LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                showPicker = true
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        onDateSelected(date)
                    }
                    showPicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── Shared Utilities ───────────────────────────────────────────────────

/**
 * Formats a quantity for display: shows integer if whole number, one decimal otherwise.
 * e.g. 2.0 → "2", 2.5 → "2.5"
 */
fun Double.formatQty(): String {
    return if (this % 1.0 == 0.0) this.toLong().toString()
    else String.format(java.util.Locale.US, "%.1f", this)
}

// ─── Stock Bar Computation ───────────────────────────────────────────────

/**
 * Result of [computeStockBar] — everything a UI needs to render a stock bar.
 *
 * @param ratio     0..1 fill amount (quantity / ceiling, clamped)
 * @param threshold 0..1 where the "low stock" cutoff sits on the bar
 * @param ceiling   the denominator used (for display: "qty / ceiling")
 */
data class StockBarState(
    val ratio: Float,
    val threshold: Float,
    val ceiling: Double,
)

/**
 * Central stock-bar math used everywhere items show a progress bar.
 *
 * **Ceiling priority** (what "100% full" means):
 * 1. [maxQuantity] — user-set storage capacity
 * 2. [smartMinQuantity] — auto-learned peak (highest qty ever recorded)
 * 3. max([quantity], [minQuantity] × 2) — synthetic ceiling when only min is set
 * 4. [quantity] — binary mode (has stock or doesn't)
 * 5. 1.0 — div-by-zero guard
 *
 * **Threshold** (where the "low" color band starts):
 * - If [minQuantity] > 0: minQuantity / ceiling (dynamic per-item)
 * - Else: [globalLowThreshold] (user's global setting, default 0.25)
 */
fun computeStockBar(
    quantity: Double,
    minQuantity: Double,
    smartMinQuantity: Double,
    maxQuantity: Double?,
    globalLowThreshold: Float,
): StockBarState {
    val ceiling = when {
        maxQuantity != null && maxQuantity > 0 -> maxQuantity
        smartMinQuantity > 0                   -> smartMinQuantity
        minQuantity > 0                        -> maxOf(quantity, minQuantity * 2)
        quantity > 0                           -> quantity
        else                                   -> 1.0
    }

    val threshold = if (minQuantity > 0 && ceiling > 0) {
        (minQuantity / ceiling).toFloat().coerceIn(0f, 0.9f)
    } else {
        globalLowThreshold
    }

    val ratio = (quantity / ceiling).toFloat().coerceIn(0f, 1f)

    return StockBarState(ratio = ratio, threshold = threshold, ceiling = ceiling)
}

// ─── Item Stock Bar ──────────────────────────────────────────────────────

/**
 * Compact stock-level bar for use on any screen that shows items.
 * Uses [computeStockBar] for ratio/threshold, then renders a [ThemedProgressBar].
 */
@Composable
fun ItemStockBar(
    quantity: Double,
    minQuantity: Double,
    smartMinQuantity: Double,
    lowStockThreshold: Float,
    modifier: Modifier = Modifier,
    maxQuantity: Double? = null,
    height: androidx.compose.ui.unit.Dp = 3.dp,
) {
    val state = computeStockBar(quantity, minQuantity, smartMinQuantity, maxQuantity, lowStockThreshold)
    val stockColor = MaterialTheme.appColors.stockColor(state.ratio, state.threshold)
    ThemedProgressBar(
        progress = { state.ratio },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(height / 2)),
        color = stockColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

// ─── Themed Divider ─────────────────────────────────────────────────────

/**
 * Divider that draws a wobbly ink rule line in Paper & Ink mode,
 * delegates to standard [HorizontalDivider] in Modern mode.
 */
@Composable
fun ThemedDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    when (val style = MaterialTheme.visuals.dividerStyle) {
        is DividerStyle.Standard -> {
            HorizontalDivider(modifier = modifier, thickness = thickness, color = color)
        }
        is DividerStyle.InkRule -> {
            val wobbleSeed = remember { (Math.random() * 1000).toFloat() }
            Canvas(
                modifier = modifier
                    .fillMaxWidth()
                    .height(thickness * 3) // space for wobble
            ) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f
                val segments = style.segments

                val path = Path().apply {
                    val segWidth = w / segments
                    moveTo(0f, centerY)
                    for (i in 1..segments) {
                        val endX = segWidth * i
                        val endY = centerY + sin((i + wobbleSeed) * 1.3).toFloat() * (h * style.wobbleAmplitude)
                        val ctrlX = segWidth * (i - 0.5f)
                        val ctrlY = centerY + sin((i + wobbleSeed) * 2.1 + PI / 3).toFloat() * (h * style.wobbleAmplitude * 1.6f)
                        quadraticBezierTo(ctrlX, ctrlY, endX, endY)
                    }
                }

                // Layer 1: Bleed (wider, low alpha)
                drawPath(
                    path = path,
                    color = color.copy(alpha = style.bleedAlpha),
                    style = Stroke(
                        width = thickness.toPx() * 4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Layer 2: Core stroke (narrower, high alpha)
                drawPath(
                    path = path,
                    color = color.copy(alpha = style.coreAlpha),
                    style = Stroke(
                        width = thickness.toPx() * 1.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
