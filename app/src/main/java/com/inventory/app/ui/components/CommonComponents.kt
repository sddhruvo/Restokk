package com.inventory.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.theme.CardStyle
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
import kotlin.math.PI
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
            singleLine = true
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
                .menuAnchor()
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
        interactionSource = interactionSource
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
