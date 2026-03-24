package com.inventory.app.ui.screens.cook

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.domain.model.DeductionItem
import com.inventory.app.domain.model.RecipeIngredient
import com.inventory.app.domain.model.RecipeStep
import com.inventory.app.domain.model.UnitSystem
import com.inventory.app.domain.model.collectAllIngredients
import com.inventory.app.domain.model.formatAmount
import com.inventory.app.domain.model.parseAmountToDouble
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkFireworks
import com.inventory.app.ui.components.PlaybackNavButtons
import com.inventory.app.ui.components.RecipeTimerBar
import com.inventory.app.ui.components.StepTimerCircle
import com.inventory.app.ui.theme.appColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingPlaybackScreen(
    navController: NavController,
    recipeId: Long,
    viewModel: CookingPlaybackViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Load recipe on first composition
    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }

    // Keep screen on while cooking
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Volume button navigation — hands-free step control
    val volumeHandler = LocalVolumeHandler.current
    DisposableEffect(Unit) {
        volumeHandler.setHandler { isVolumeUp ->
            val s = viewModel.uiState.value
            if (s.hasStarted && !s.isComplete) {
                if (isVolumeUp) viewModel.nextStep() else viewModel.previousStep()
                true
            } else false
        }
        onDispose { volumeHandler.clearHandler() }
    }

    // Haptic feedback + auto-advance when a timer completes
    LaunchedEffect(state.timerCompletedStepIndex) {
        val completedIdx = state.timerCompletedStepIndex ?: return@LaunchedEffect
        repeat(3) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(150)
        }
        // L-1: Auto-advance only when the CURRENT step's timer finishes
        if (completedIdx == state.currentStepIndex && !state.isComplete) {
            delay(800)
            viewModel.nextStep()
        }
        viewModel.clearTimerCompletion()
    }

    // Back press handling
    BackHandler {
        when {
            state.isComplete -> {
                val popped = navController.popBackStack(Screen.CookHub.route, inclusive = false)
                if (!popped) navController.navigate(Screen.CookHub.route)
            }
            state.hasStarted && state.currentStepIndex > 0 -> viewModel.previousStep()
            else -> viewModel.showExitDialog()
        }
    }

    // Exit confirmation dialog
    if (state.showExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitDialog() },
            title = { Text("Exit cooking?") },
            text = { Text("Timers will keep running in the background.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissExitDialog()
                    viewModel.onExitConfirmed()
                    navController.popBackStack()
                }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExitDialog() }) { Text("Keep cooking") }
            }
        )
    }

    when {
        state.isLoading -> LoadingScreen()
        state.error != null -> ErrorScreen(state.error!!) { navController.popBackStack() }
        !state.hasStarted -> PreStartScreen(state, viewModel)
        state.isComplete -> CompletionScreen(state, viewModel) {
            val popped = navController.popBackStack(Screen.CookHub.route, inclusive = false)
            if (!popped) navController.navigate(Screen.CookHub.route)
        }
        else -> PlaybackScreen(state, viewModel)
    }
}

// ── Pre-start screen ──────────────────────────────────────────────────────────

@Composable
private fun PreStartScreen(
    state: PlaybackUiState,
    viewModel: CookingPlaybackViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = state.recipeName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "${state.totalSteps} steps",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Ingredient checklist — full list with ready/missing status
        if (state.preStartDeductions.isNotEmpty()) {
            val ready = state.preStartDeductions.filter { it.canDeduct }
            val missing = state.preStartDeductions.filter { !it.canDeduct }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${ready.size} / ${state.preStartDeductions.size} ingredients ready",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Ready group
                    ready.forEach { item ->
                        IngredientChecklistRow(item = item, isReady = true)
                    }

                    // Missing group
                    if (missing.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        missing.forEach { item ->
                            IngredientChecklistRow(item = item, isReady = false)
                        }

                        // Add to shopping list action
                        if (!state.shoppingAddedFromPreStart) {
                            TextButton(
                                onClick = { viewModel.addMissingToShoppingListFromPreStart() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add ${missing.size} missing to shopping list") }
                        } else {
                            Text(
                                "Added to your shopping list",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        // Servings stepper
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Serves", fontSize = 16.sp)
                IconButton(
                    onClick = { viewModel.setServings(state.selectedServings - 1) },
                    enabled = state.selectedServings > 1
                ) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${state.selectedServings}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { viewModel.setServings(state.selectedServings + 1) }) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.originalServings != state.selectedServings) {
            Text(
                text = "Scaling ×%.2f".format(state.scalingFactor),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Volume button tip — shown first 3 times
        if (state.showVolumeTip) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        "Use volume buttons to navigate steps hands-free",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.startCooking() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start Cooking ▶", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Main playback screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaybackScreen(
    state: PlaybackUiState,
    viewModel: CookingPlaybackViewModel
) {
    state.steps.getOrNull(state.currentStepIndex) ?: return

    var swipeOffset = remember { 0f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ── Persistent top bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Step ${state.currentStepIndex + 1} of ${state.totalSteps}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                calculateRemainingTime(state.steps, state.currentStepIndex)?.let { secs ->
                    Text(
                        text = formatTimeRemaining(secs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dot indicator — three states: completed (tertiary), current (primary), future (outline)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(state.totalSteps) { idx ->
                    val isCurrent = idx == state.currentStepIndex
                    val isCompleted = idx in state.completedSteps
                    Box(
                        modifier = Modifier
                            .size(when {
                                isCurrent -> 10.dp
                                isCompleted -> 8.dp
                                else -> 6.dp
                            })
                            .clip(CircleShape)
                            .background(when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isCompleted -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            })
                    )
                }
            }

            IconButton(onClick = { viewModel.showExitDialog() }) {
                Icon(Icons.Default.Close, contentDescription = "Exit cooking")
            }
        }

        LinearProgressIndicator(
            progress = { (state.currentStepIndex + 1).toFloat() / state.totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )

        // ── Main content (with swipe detection) ──────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Layer 1: swipe gesture detector (background)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.currentStepIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset < -80f) viewModel.nextStep()
                                else if (swipeOffset > 80f) viewModel.previousStep()
                                swipeOffset = 0f
                            }
                        ) { _, dragAmount -> swipeOffset += dragAmount }
                    }
            )

            // Layer 2: non-interactive content
            AnimatedContent(
                targetState = state.currentStepIndex,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "stepTransition"
            ) { stepIndex ->
                val step = state.steps.getOrNull(stepIndex) ?: return@AnimatedContent
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Main instruction text
                    Text(
                        text = step.instruction,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(24.dp))

                    // Per-step ingredients (scaled)
                    val displayIngredients = if (step.ingredients.isNotEmpty()) {
                        step.ingredients
                    } else {
                        // Fallback: show all recipe ingredients on every step if none are per-step
                        state.steps.flatMap { it.ingredients }.distinctBy { it.name }
                    }

                    if (displayIngredients.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayIngredients.forEach { ingredient ->
                                IngredientChip(
                                    ingredient = ingredient,
                                    scalingFactor = state.scalingFactor
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Timer circle (if step has a timer)
                    step.timerSeconds?.let { totalSecs ->
                        StepTimerCircle(
                            timerState = state.activeTimers[stepIndex],
                            totalSeconds = totalSecs,
                            onTap = {
                                val timerState = state.activeTimers[stepIndex]
                                when {
                                    timerState == null || (!timerState.isRunning && timerState.remainingSeconds == timerState.totalSeconds) ->
                                        viewModel.startTimer(stepIndex)
                                    timerState.isRunning -> viewModel.pauseTimer(stepIndex)
                                    else -> viewModel.startTimer(stepIndex) // resume
                                }
                            },
                            onLongPress = {
                                // Reset timer — cancel and clear state so it returns to IDLE
                                viewModel.cancelTimer(stepIndex)
                            }
                        )
                    }
                }
            }

            // Layer 3: mini timer bar + nav buttons (consume events)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                RecipeTimerBar(
                    timers = state.activeTimers,
                    currentStepIndex = state.currentStepIndex,
                    onTimerTap = { stepIdx -> viewModel.jumpToStep(stepIdx) }
                )

                PlaybackNavButtons(
                    currentStep = state.currentStepIndex,
                    totalSteps = state.totalSteps,
                    onBack = { viewModel.previousStep() },
                    onNext = { viewModel.nextStep() }
                )
            }
        }
    }
}

// ── Pre-start ingredient checklist row ────────────────────────────────────────

@Composable
private fun IngredientChecklistRow(
    item: DeductionItem,
    isReady: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isReady) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isReady) MaterialTheme.colorScheme.tertiary
                   else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        Text(
            text = item.ingredientName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        // Amount + unit (scaled)
        val amtText = item.scaledAmount?.let { "${formatAmount(it)} ${item.unit}" }
            ?: "${item.amount} ${item.unit}".trim()
        if (amtText.isNotBlank()) {
            Text(
                text = amtText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Ingredient chip ───────────────────────────────────────────────────────────

@Composable
private fun IngredientChip(
    ingredient: RecipeIngredient,
    scalingFactor: Float
) {
    val displayText = buildIngredientDisplay(ingredient, scalingFactor)
    SuggestionChip(
        onClick = {},
        label = { Text(displayText, fontSize = 13.sp) }
    )
}

private fun buildIngredientDisplay(ingredient: RecipeIngredient, scalingFactor: Float): String {
    val parsed = parseAmountToDouble(ingredient.amount)
    return if (parsed != null && scalingFactor != 1f) {
        val scaled = parsed * scalingFactor
        val formatted = UnitSystem.formatScaled(scaled, ingredient.unit)
        "${ingredient.name}: $formatted"
    } else if (ingredient.amount.isNotBlank()) {
        "${ingredient.name}: ${ingredient.amount} ${ingredient.unit}".trim()
    } else {
        ingredient.name
    }
}

// ── Completion screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletionScreen(
    state: PlaybackUiState,
    viewModel: CookingPlaybackViewModel,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top)
    ) {
        // Celebration animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            InkFireworks()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(40.dp))
                Text(
                    "Well done!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    state.recipeName,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // C-4: Star rating
        if (state.userRating == 0) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "How was it?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { starIndex ->
                        IconButton(onClick = { viewModel.rateRecipe(starIndex + 1) }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "${starIndex + 1} stars",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "Thanks for the rating!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Deduction card (only if not already done)
        if (!state.deductionDone) {
            DeductionCard(state, viewModel)
        }

        // Done button
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Done — go back", fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeductionCard(
    state: PlaybackUiState,
    viewModel: CookingPlaybackViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.deductableCount > 0 -> {
                    Text(
                        "Update inventory?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "We'll deduct ${state.deductableCount} ingredient${if (state.deductableCount == 1) "" else "s"}:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Auto-deductable items list
                    state.deductionItems
                        .filter { it.canDeduct }
                        .forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = {
                                        viewModel.toggleDeductionItem(
                                            state.deductionItems.indexOf(item)
                                        )
                                    }
                                )
                                val amtDisplay = item.convertedAmount?.let {
                                    "${formatAmount(it)} ${item.matchedItemUnit ?: item.unit}"
                                } ?: item.scaledAmount?.let {
                                    "${formatAmount(it)} ${item.unit}"
                                } ?: "${item.amount} ${item.unit}"
                                Text("${item.matchedItemName ?: item.ingredientName} (−$amtDisplay)")
                            }
                        }

                    // "Review Details" toggle
                    TextButton(onClick = { viewModel.toggleDeductionDetails() }) {
                        Text(if (state.showDeductionDetails) "Hide details" else "Review Details")
                    }

                    // Expanded details: non-deductable items with reasons
                    AnimatedVisibility(visible = state.showDeductionDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.deductionItems
                                .filter { !it.canDeduct }
                                .forEach { item ->
                                    Text(
                                        text = "• ${item.ingredientName}: ${item.cannotDeductReason ?: "can't auto-match"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                        }
                    }

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.skipDeduction() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Skip") }

                        Button(
                            onClick = { viewModel.confirmDeduction() },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isDeducting
                        ) {
                            if (state.isDeducting) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Update ✓")
                            }
                        }
                    }

                    // C-1: Restock section for items not in inventory
                    if (state.restockableCount > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${state.restockableCount} not in stock",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (!state.shoppingAddedFromCompletion) {
                                TextButton(onClick = { viewModel.addNotInInventoryToShoppingList() }) {
                                    Text("Restock")
                                }
                            } else {
                                Text(
                                    "Added",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    }
                }

                state.steps.flatMap { it.ingredients }.isNotEmpty() -> {
                    // Has ingredients but none matched
                    Text(
                        "Ingredients couldn't be auto-matched",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "No inventory items matched the recipe ingredients automatically.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // C-1: Restock section for items not in inventory (no-match case)
                    if (state.restockableCount > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${state.restockableCount} not in stock",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (!state.shoppingAddedFromCompletion) {
                                TextButton(onClick = { viewModel.addNotInInventoryToShoppingList() }) {
                                    Text("Restock")
                                }
                            } else {
                                Text(
                                    "Added",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.skipDeduction() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Done") }
                }

                else -> {
                    // No ingredients — steps only recipe, skip card entirely.
                    // C-4: use LaunchedEffect so the side-effect runs AFTER composition,
                    // not DURING it. Calling skipDeduction() directly in the composition body
                    // causes duplicate DB inserts on every recomposition.
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        viewModel.skipDeduction()
                    }
                }
            }
        }
    }
}

// ── Time remaining helpers ─────────────────────────────────────────────────────

/** Sum timerSeconds from current step onward. Returns null if no timers exist. */
private fun calculateRemainingTime(steps: List<RecipeStep>, currentStepIndex: Int): Int? {
    val remaining = steps.subList(currentStepIndex, steps.size)
        .mapNotNull { it.timerSeconds }
    return if (remaining.isEmpty()) null else remaining.sum()
}

private fun formatTimeRemaining(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "~${hours}h ${minutes}m left"
        hours > 0 -> "~${hours}h left"
        minutes > 0 -> "~${minutes}m left"
        else -> "~${totalSeconds}s left"
    }
}

// ── Helper screens ────────────────────────────────────────────────────────────

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Go back") }
    }
}
