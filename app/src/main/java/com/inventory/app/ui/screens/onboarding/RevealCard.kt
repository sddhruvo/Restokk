package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import com.inventory.app.ui.components.InkFireworks
import com.inventory.app.ui.components.ThemedCircularProgress
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.RuledLinesBackground
import com.inventory.app.ui.components.ThemedTextField
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import com.inventory.app.ui.theme.visuals
import com.inventory.app.util.FormatUtils
import kotlinx.coroutines.delay

private val BouncySpring = PaperInkMotion.BouncySpring
private val WobblySpring = PaperInkMotion.WobblySpring
private val GentleSpring = PaperInkMotion.GentleSpring

/**
 * Merged Reveal + Celebration screen.
 * Fields fill in one by one → button fades up → tap saves + fireworks → transitions to AddMore.
 */
@Composable
internal fun RevealCard(
    item: RevealItem,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCorrectName: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val reduceMotion = LocalReduceMotion.current

    // ── "Not quite?" edit state ──
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(item.name) }

    // ── Field-by-field reveal sequence ──
    var cardReady by remember { mutableStateOf(reduceMotion) }
    var nameReady by remember { mutableStateOf(reduceMotion) }
    var categoryReady by remember { mutableStateOf(reduceMotion) }
    var locationReady by remember { mutableStateOf(reduceMotion) }
    var shelfLifeReady by remember { mutableStateOf(reduceMotion) }
    var underlineReady by remember { mutableStateOf(reduceMotion) }
    var flavorReady by remember { mutableStateOf(reduceMotion) }
    var buttonReady by remember { mutableStateOf(reduceMotion) }

    // Celebration state (after user taps save)
    var celebrating by remember { mutableStateOf(false) }

    LaunchedEffect(item) {
        if (reduceMotion) return@LaunchedEffect
        delay(400)                // T+400: card scales in
        cardReady = true
        delay(1000)               // T+1400: name writes in
        nameReady = true
        delay(1100)               // T+2500: category
        categoryReady = true
        delay(1100)               // T+3600: location
        locationReady = true
        delay(1100)               // T+4700: shelf life
        shelfLifeReady = true
        delay(800)                // T+5500: underline draws
        underlineReady = true
        delay(700)                // T+6200: flavor text
        flavorReady = true
        delay(800)                // T+7000: button fades up
        buttonReady = true
    }

    // Per-field haptic ticks
    LaunchedEffect(nameReady) { if (nameReady) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(categoryReady) { if (categoryReady) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(locationReady) { if (locationReady) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(shelfLifeReady) { if (shelfLifeReady) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    // Card entrance: scale 0.85→1.0
    val cardScale by animateFloatAsState(
        targetValue = if (cardReady) 1f else 0.85f,
        animationSpec = BouncySpring, label = "revealCardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (cardReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "revealCardAlpha"
    )

    // Card pop on save
    val cardPop by animateFloatAsState(
        targetValue = if (celebrating) 1.03f else 1f,
        animationSpec = WobblySpring, label = "cardPop"
    )

    // Pen-stroke underline
    val underlineFraction by animateFloatAsState(
        targetValue = if (underlineReady) 1f else 0f,
        animationSpec = tween(250), label = "underline"
    )

    // Flavor text: Fade Up
    val flavorY by animateFloatAsState(
        targetValue = if (flavorReady) 0f else 12f,
        animationSpec = GentleSpring, label = "flavorY"
    )
    val flavorAlpha by animateFloatAsState(
        targetValue = if (flavorReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "flavorAlpha"
    )

    // Button: Fade Up + breathing
    val btnY by animateFloatAsState(
        targetValue = if (buttonReady) 0f else 20f,
        animationSpec = GentleSpring, label = "revBtnY"
    )
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "revBtnAlpha"
    )
    val breathe = rememberInfiniteTransition(label = "btnBreathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "revBtnBreathe"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── The reveal card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = cardScale * cardPop
                        scaleX = s; scaleY = s
                        alpha = cardAlpha
                    },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = if (MaterialTheme.visuals.useElevation) 4.dp else 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box {
                    RuledLinesBackground(modifier = Modifier.matchParentSize())

                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RevealField(
                            icon = Icons.Outlined.Inventory2,
                            label = "Name",
                            value = item.name,
                            visible = nameReady,
                            index = 0
                        )
                        // "Not quite?" edit button
                        if (nameReady && onCorrectName != null && !isEditing) {
                            TextButton(
                                onClick = {
                                    isEditing = true
                                    editedName = item.name
                                }
                            ) {
                                Text("Not quite?", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (isEditing && onCorrectName != null) {
                            ThemedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Correct name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        isEditing = false
                                        if (editedName.isNotBlank() && editedName.trim() != item.name) {
                                            onCorrectName(editedName.trim())
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (item.category != null) {
                            RevealField(
                                icon = Icons.Outlined.Category,
                                label = "Category",
                                value = item.category,
                                visible = categoryReady,
                                index = 1
                            )
                        }
                        if (item.location != null) {
                            RevealField(
                                icon = Icons.Outlined.Kitchen,
                                label = "Location",
                                value = item.location,
                                visible = locationReady,
                                index = 2
                            )
                        }
                        if (item.shelfLifeDays != null) {
                            RevealField(
                                icon = Icons.Outlined.Schedule,
                                label = "Shelf Life",
                                value = FormatUtils.formatShelfLife(item.shelfLifeDays),
                                visible = shelfLifeReady,
                                index = 3
                            )
                        }

                        // Pen-stroke underline
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        ) {
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Flavor text ──
            val flavorText = when (item.source) {
                Act2Path.TYPE -> "We know a thing or two about kitchens."
                Act2Path.CAMERA -> "All that from one photo."
                Act2Path.BARCODE -> "Found it in our database."
                Act2Path.MEMORY -> ""
            }
            if (flavorText.isNotEmpty()) {
                Text(
                    text = flavorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        translationY = flavorY; alpha = flavorAlpha
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Save button (fades up after reveal completes) ──
            if (buttonReady || btnAlpha > 0.01f) {
                ThemedButton(
                    onClick = {
                        celebrating = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave()
                    },
                    enabled = !isSaving && !celebrating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer {
                            translationY = btnY; alpha = btnAlpha
                            val bs = if (buttonReady && !celebrating) breatheScale else 1f
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
                        Text("Add to my kitchen", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // ── InkFireworks overlay ──
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

// ═══════════════════════════════════════════════════════════════════════════
// Single reveal field row
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RevealField(
    icon: ImageVector,
    label: String,
    value: String,
    visible: Boolean,
    index: Int
) {
    val fieldX by animateFloatAsState(
        targetValue = if (visible) 0f else -30f,
        animationSpec = BouncySpring, label = "field${index}X"
    )
    val fieldAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250), label = "field${index}Alpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = BouncySpring, label = "icon${index}Scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = fieldX; alpha = fieldAlpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

