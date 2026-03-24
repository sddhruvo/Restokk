package com.inventory.app.ui.screens.cook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.inventory.app.ui.components.ThemedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.domain.model.BuilderStep
import com.inventory.app.domain.model.InventorySuggestion
import com.inventory.app.domain.model.RecipeIngredient
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.IngredientAutoSuggest
import com.inventory.app.ui.components.InkBorderCard
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.RecipeBuilderDotIndicator
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.rememberAiSignInGate
import com.inventory.app.ui.navigation.RegisterNavigationGuard
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.visuals
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipeBuilderScreen(
    navController: NavController,
    recipeId: Long? = null,
    captureMode: Boolean = false,
    viewModel: RecipeBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val aiGate = rememberAiSignInGate()

    // Load for edit mode
    LaunchedEffect(recipeId) {
        if (recipeId != null && recipeId > 0) {
            viewModel.loadRecipe(recipeId)
        }
    }

    // Enable capture mode (only fires once on first composition)
    LaunchedEffect(captureMode) {
        if (captureMode) viewModel.enableCaptureMode()
    }

    // Consume one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BuilderEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
                is BuilderEvent.SaveComplete -> {
                    navController.navigate(Screen.SavedRecipes.route) {
                        popUpTo(Screen.CookHub.route) { inclusive = false }
                    }
                }
                is BuilderEvent.NavigateTo -> navController.navigate(event.route)
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { state.totalPages })

    // VM → Pager: programmatic page changes (addNextStep, insertBefore, navigateToReview)
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    // Pager → VM: user swipe. Use settledPage (not currentPage) to prevent loop
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageChanged(page)
        }
    }

    // Unsaved changes guard (bottom nav protection)
    RegisterNavigationGuard(
        shouldBlock = { state.hasUnsavedChanges },
        message = { "Discard this recipe?" }
    )

    // Delete step confirmation dialog
    if (state.deleteConfirmStepIndex != null) {
        val idx = state.deleteConfirmStepIndex!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("Delete step ${idx + 1}?") },
            text = { Text("This step and its ingredients will be removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteStep(idx) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text("Cancel")
                }
            }
        )
    }

    PageScaffold(
        onBack = { navController.popBackStack() },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dot indicator — persistent across all pages
            RecipeBuilderDotIndicator(
                totalPages = state.totalPages,
                currentPage = pagerState.currentPage,
                onDotTap = { page ->
                    scope.launch { pagerState.animateScrollToPage(page) }
                }
            )

            // The card pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp
            ) { page ->
                when {
                    page == 0 -> TitleCard(
                        state = state,
                        isEditMode = state.isEditMode,
                        onNameChange = viewModel::updateRecipeName,
                        onServingsChange = viewModel::updateServings,
                        onNext = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    page <= state.steps.size -> StepCard(
                        stepIndex = page - 1,
                        step = state.steps.getOrElse(page - 1) { BuilderStep() },
                        stepCount = state.steps.size,
                        suggestions = state.inventorySuggestions,
                        isCaptureMode = state.isCaptureMode,
                        isLocked = state.isCaptureMode && (page - 1) < state.steps.lastIndex,
                        captureTimerRunning = state.captureTimerRunning && state.captureTimerStepIndex == page - 1,
                        captureTimerSeconds = state.captureTimerSeconds,
                        isStructuring = state.structuringStepIndex == page - 1,
                        canUndoStructure = state.structuringStepsSnapshot != null,
                        onInstructionChange = { text -> viewModel.updateInstruction(page - 1, text) },
                        onAutoParseTimer = { text -> viewModel.autoParseTimerForStep(page - 1, text) },
                        onSetTimer = { seconds -> viewModel.setStepTimer(page - 1, seconds) },
                        onAddIngredient = { name, amount, unit ->
                            viewModel.addIngredientToStep(page - 1, name, amount, unit)
                        },
                        onRemoveIngredient = { idx -> viewModel.removeIngredientFromStep(page - 1, idx) },
                        onQueryChange = viewModel::setIngredientQuery,
                        onSuggestionSelected = { suggestion ->
                            viewModel.setIngredientQuery("")
                            viewModel.clearIngredientSuggestions()
                        },
                        onClearSuggestions = viewModel::clearIngredientSuggestions,
                        onInsertAbove = { viewModel.insertStepBefore(page - 1) },
                        onNextStep = { viewModel.addNextStep() },
                        onFinish = { viewModel.navigateToReview() },
                        onDeleteStep = { viewModel.requestDeleteStep(page - 1) },
                        onStartCaptureTimer = { viewModel.startCaptureTimer(page - 1) },
                        onStopCaptureTimer = { viewModel.stopCaptureTimer(page - 1) },
                        onStructureStep = {
                            aiGate.requireSignIn("refine and structure recipe steps with AI") {
                                viewModel.structureStep(page - 1)
                            }
                        },
                        onUndoStructure = viewModel::undoStructure
                    )
                    else -> ReviewCard(
                        state = state,
                        onEditSteps = { viewModel.navigateToLastStep() },
                        onToggleDetails = viewModel::toggleShowDetails,
                        onCuisineChange = viewModel::updateCuisine,
                        onDifficultyChange = viewModel::updateDifficulty,
                        onMealTypeChange = viewModel::updateMealType,
                        onAddTag = viewModel::addTag,
                        onRemoveTag = viewModel::removeTag,
                        onTipsChange = viewModel::updateTips,
                        onSave = viewModel::save
                    )
                }
            }
        }
    }
}

// ── Title Card ─────────────────────────────────────────────────────────────

@Composable
private fun TitleCard(
    state: RecipeBuilderUiState,
    isEditMode: Boolean,
    onNameChange: (String) -> Unit,
    onServingsChange: (Int) -> Unit,
    onNext: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isEditMode && !focused) {
            focused = true
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isEditMode) "Edit Recipe" else "What are we making?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        ThemedTextField(
            value = state.recipeName,
            onValueChange = onNameChange,
            label = { Text("Recipe name") },
            placeholder = { Text("Maa ki Dal, Pasta Aglio e Olio…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (state.recipeName.isNotBlank()) onNext() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Servings stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Serves:", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { onServingsChange(-1) }) {
                Text("−", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
            }
            Text(
                text = "${state.servings}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(onClick = { onServingsChange(+1) }) {
                Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        ThemedButton(
            onClick = onNext,
            enabled = state.recipeName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isEditMode) "Edit Steps →" else "Let's Go →",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ── Step Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StepCard(
    stepIndex: Int,
    step: BuilderStep,
    stepCount: Int,
    suggestions: List<InventorySuggestion>,
    isCaptureMode: Boolean = false,
    isLocked: Boolean = false,
    captureTimerRunning: Boolean = false,
    captureTimerSeconds: Int = 0,
    isStructuring: Boolean = false,
    canUndoStructure: Boolean = false,
    onInstructionChange: (String) -> Unit,
    onAutoParseTimer: (String) -> Unit,
    onSetTimer: (Int?) -> Unit,
    onAddIngredient: (name: String, amount: String, unit: String) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onSuggestionSelected: (InventorySuggestion) -> Unit,
    onClearSuggestions: () -> Unit,
    onInsertAbove: () -> Unit,
    onNextStep: () -> Unit,
    onFinish: () -> Unit,
    onDeleteStep: () -> Unit,
    onStartCaptureTimer: () -> Unit = {},
    onStopCaptureTimer: () -> Unit = {},
    onStructureStep: () -> Unit = {},
    onUndoStructure: () -> Unit = {}
) {
    var showAddIngredient by remember { mutableStateOf(false) }
    var showTimerEdit by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var timerInputMinutes by remember(step.timerSeconds) {
        mutableStateOf(step.timerSeconds?.let { "${it / 60}" } ?: "")
    }
    // Local ingredient form state
    var ingName by remember { mutableStateOf("") }
    var ingAmount by remember { mutableStateOf("") }
    var ingUnit by remember { mutableStateOf("") }

    // Auto-parse timer after text settles — only in write mode (not locked/capture)
    LaunchedEffect(step.instruction) {
        if (!isLocked) {
            delay(500)
            onAutoParseTimer(step.instruction)
        }
    }

    // ── Locked step (capture mode: previous step, read-only) ───────────────
    if (isLocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${stepIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                step.captureTimestamp?.let { ts ->
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(ts))
                    Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (step.instruction.isNotBlank()) {
                Text(
                    step.instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (step.timerSeconds != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val mins = step.timerSeconds / 60
                val secs = step.timerSeconds % 60
                val display = when {
                    mins > 0 && secs > 0 -> "${mins}m ${secs}s"
                    mins > 0 -> "${mins}m"
                    else -> "${secs}s"
                }
                Text("⏱ $display recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (step.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                step.ingredients.forEach { ing ->
                    Text(
                        "• ${ing.name}${if (ing.amount.isNotBlank()) ": ${ing.amount}" else ""}${if (ing.unit.isNotBlank()) " ${ing.unit}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding()
    ) {
        // Step header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Step ${stepIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                // Capture mode: "● Recording" indicator
                if (isCaptureMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "● Recording",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Insert step above link (hidden in capture mode — not relevant while cooking)
            if (!isCaptureMode) {
                TextButton(onClick = onInsertAbove) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Insert step above", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Instruction text area
        ThemedTextField(
            value = step.instruction,
            onValueChange = onInstructionChange,
            placeholder = { Text("Describe what to do in this step…") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action chips row: Timer + Add Ingredient
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Timer chip
            if (step.timerSeconds != null) {
                FilterChip(
                    selected = true,
                    onClick = { showTimerEdit = !showTimerEdit },
                    label = {
                        val mins = step.timerSeconds / 60
                        val secs = step.timerSeconds % 60
                        val display = when {
                            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
                            mins > 0 -> "${mins}m"
                            else -> "${secs}s"
                        }
                        Text(
                            "⏱ $display${if (step.timerAutoDetected) " ✓" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Timer, null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { onSetTimer(null) }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                )
            } else {
                AssistChip(
                    onClick = { showTimerEdit = !showTimerEdit },
                    label = { Text("+ Timer", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Timer, null) }
                )
            }

            // Add ingredient chip
            AssistChip(
                onClick = { showAddIngredient = !showAddIngredient },
                label = { Text("+ Ingredient", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Add, null) }
            )

            // Capture timer chip (capture mode only)
            if (isCaptureMode) {
                if (captureTimerRunning) {
                    val mins = captureTimerSeconds / 60
                    val secs = captureTimerSeconds % 60
                    val display = "%d:%02d".format(mins, secs)
                    FilterChip(
                        selected = true,
                        onClick = onStopCaptureTimer,
                        label = { Text("■ $display", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null, tint = MaterialTheme.colorScheme.error) }
                    )
                } else {
                    AssistChip(
                        onClick = onStartCaptureTimer,
                        label = { Text("⏺ Time this step", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null) }
                    )
                }
            }

            // AI Refine Step chip — write mode only, hidden while structuring is active
            if (!isCaptureMode && !isStructuring) {
                AssistChip(
                    onClick = { showAiSheet = true },
                    label = { Text("✨ Refine Step", style = MaterialTheme.typography.bodySmall) },
                    enabled = step.instruction.isNotBlank()
                )
            }

            // Undo Refine chip — shown immediately after structuring
            if (canUndoStructure && !isCaptureMode) {
                AssistChip(
                    onClick = onUndoStructure,
                    label = { Text("↩ Undo Refine", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Undo, contentDescription = null) }
                )
            }
        }

        // Inline loading indicator while AI is structuring
        AnimatedVisibility(visible = isStructuring) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    "Refining step…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // AI Step Refiner bottom sheet
        if (showAiSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAiSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AiStepRefinerSheetContent(
                    stepText = step.instruction,
                    isStructuring = isStructuring,
                    onRefine = {
                        showAiSheet = false
                        onStructureStep()
                    },
                    onDismiss = { showAiSheet = false }
                )
            }
        }

        // Timer edit inline form
        AnimatedVisibility(
            visible = showTimerEdit,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemedTextField(
                    value = timerInputMinutes,
                    onValueChange = { timerInputMinutes = it },
                    label = { Text("Minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val mins = timerInputMinutes.toIntOrNull()
                            onSetTimer(if (mins != null && mins > 0) mins * 60 else null)
                            showTimerEdit = false
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )
                ThemedButton(onClick = {
                    val mins = timerInputMinutes.toIntOrNull()
                    onSetTimer(if (mins != null && mins > 0) mins * 60 else null)
                    showTimerEdit = false
                }) {
                    Text("Set")
                }
            }
        }

        // Add ingredient inline form
        AnimatedVisibility(
            visible = showAddIngredient,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            InkBorderCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    IngredientAutoSuggest(
                        nameValue = ingName,
                        amountValue = ingAmount,
                        unitValue = ingUnit,
                        suggestions = suggestions,
                        onNameChange = { ingName = it },
                        onAmountChange = { ingAmount = it },
                        onUnitChange = { ingUnit = it },
                        onQueryChange = onQueryChange,
                        onSuggestionSelected = { suggestion ->
                            ingName = suggestion.name
                            ingUnit = suggestion.unit
                            onSuggestionSelected(suggestion)
                        },
                        onClearSuggestions = onClearSuggestions
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            ingName = ""; ingAmount = ""; ingUnit = ""
                            showAddIngredient = false
                            onClearSuggestions()
                        }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemedButton(
                            onClick = {
                                onAddIngredient(ingName, ingAmount, ingUnit)
                                ingName = ""; ingAmount = ""; ingUnit = ""
                                onClearSuggestions()
                            },
                            enabled = ingName.isNotBlank()
                        ) { Text("Add") }
                    }
                }
            }
        }

        // Ingredient pills
        if (step.ingredients.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                step.ingredients.forEachIndexed { idx, ingredient ->
                    val label = buildString {
                        append(ingredient.name)
                        if (ingredient.amount.isNotBlank()) append(": ${ingredient.amount}")
                        if (ingredient.unit.isNotBlank()) append(" ${ingredient.unit}")
                    }
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = {
                            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        },
                        trailingIcon = {
                            IconButton(onClick = { onRemoveIngredient(idx) }) {
                                Icon(Icons.Default.Close, "Remove ingredient", modifier = Modifier.padding(2.dp))
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemedButton(
                onClick = onNextStep,
                // In capture mode: full-width emphasis on Next Step
                modifier = if (isCaptureMode) Modifier.weight(1.4f) else Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 6.dp))
                Text("Next Step")
            }
            ThemedButton(
                onClick = onFinish,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.padding(end = 6.dp))
                Text(if (isCaptureMode) "Done Cooking" else "Finish")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Delete step (secondary, only if more than 1 step)
        if (stepCount > 1) {
            TextButton(
                onClick = onDeleteStep,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    "Delete Step",
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Review Card ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCard(
    state: RecipeBuilderUiState,
    onEditSteps: () -> Unit,
    onToggleDetails: () -> Unit,
    onCuisineChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onMealTypeChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onTipsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var tagInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Recipe summary
        Text(
            text = (if (state.isCaptureMode) "🔴 " else "✨ ") + state.recipeName.ifBlank { "Untitled Recipe" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        val subtitle = buildString {
            append("Serves ${state.servings}")
            append(" · ${state.steps.size} step${if (state.steps.size != 1) "s" else ""}")
            if (state.totalTimeMinutes != null && state.totalTimeMinutes > 0) {
                val h = state.totalTimeMinutes / 60
                val m = state.totalTimeMinutes % 60
                val time = when {
                    h > 0 && m > 0 -> "${h}h ${m}m"
                    h > 0 -> "${h}h"
                    else -> "${m}m"
                }
                append(" · ~$time")
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Ingredients section
        SectionHeader("Ingredients")
        if (state.collectedIngredients.isEmpty()) {
            Text(
                "No ingredients listed — you can add them by editing steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            state.collectedIngredients.forEach { ingredient ->
                IngredientRow(ingredient)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Steps section
        SectionHeader("Steps")
        state.steps.forEachIndexed { idx, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "${idx + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp, top = 1.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = step.instruction.take(80).let { if (step.instruction.length > 80) "$it…" else it },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (step.timerSeconds != null) {
                    val mins = step.timerSeconds / 60
                    Text(
                        text = " ⏱${mins}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save button
        ThemedButton(
            onClick = onSave,
            enabled = !state.isSaving && state.recipeName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = if (state.isEditMode) "Save Changes" else "Save Recipe",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onEditSteps) {
                Text("← Edit Steps")
            }
            TextButton(onClick = onToggleDetails) {
                Icon(Icons.Default.ExpandMore, null, modifier = Modifier.padding(end = 4.dp))
                Text(if (state.showDetails) "Hide Details" else "+ Details")
            }
        }

        // Optional details section
        AnimatedVisibility(
            visible = state.showDetails,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "More Details (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Cuisine chips
                Text("Cuisine", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf("Indian", "Italian", "Chinese", "Mexican", "Thai", "Other").forEach { cuisine ->
                        FilterChip(
                            selected = state.cuisineOrigin == cuisine,
                            onClick = { onCuisineChange(if (state.cuisineOrigin == cuisine) "" else cuisine) },
                            label = { Text(cuisine, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Difficulty chips
                Text("Difficulty", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = state.difficulty.equals(diff, ignoreCase = true),
                            onClick = { onDifficultyChange(diff.lowercase()) },
                            label = { Text(diff, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meal type chips
                Text("Meal Type", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snack", "Dessert").forEach { meal ->
                        FilterChip(
                            selected = state.mealType.equals(meal, ignoreCase = true),
                            onClick = { onMealTypeChange(if (state.mealType.equals(meal, ignoreCase = true)) "" else meal.lowercase()) },
                            label = { Text(meal, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tags
                Text("Tags", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    state.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.bodySmall) },
                            trailingIcon = {
                                IconButton(onClick = { onRemoveTag(tag) }) {
                                    Icon(Icons.Default.Close, "Remove tag")
                                }
                            }
                        )
                    }
                    // Tag input
                    ThemedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("Add tag…", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (tagInput.isNotBlank()) {
                                onAddTag(tagInput.trim())
                                tagInput = ""
                            }
                        }),
                        modifier = Modifier.width(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tips
                ThemedTextField(
                    value = state.tips,
                    onValueChange = onTipsChange,
                    label = { Text("Tips & Notes (optional)") },
                    placeholder = { Text("Add cooking tips, variations, or notes…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun IngredientRow(ingredient: RecipeIngredient) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
        Text(
            text = buildString {
                if (ingredient.amount.isNotBlank()) append("${ingredient.amount} ")
                if (ingredient.unit.isNotBlank()) append("${ingredient.unit} ")
                append(ingredient.name)
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ── AI Step Refiner Sheet ──────────────────────────────────────────────────

@Composable
private fun AiStepRefinerSheetContent(
    stepText: String,
    isStructuring: Boolean,
    onRefine: () -> Unit,
    onDismiss: () -> Unit
) {
    val isPaperInk = !MaterialTheme.visuals.useElevation

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.then(
                    Modifier.size(22.dp)
                ),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "AI Step Refiner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Capability bullets
        val bullets = listOf(
            "Splits complex multi-action steps into clean, individual steps",
            "Extracts and formats timers (e.g. '15 minutes' → timer chip)",
            "Extracts ingredients mentioned in your step text",
            "Cleans up grammar and adds precision to instructions"
        )
        bullets.forEach { bullet ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                )
                Text(bullet, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Step preview
        Text(
            "Your step:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        val previewContent: @Composable () -> Unit = {
            Text(
                stepText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }
        if (isPaperInk) {
            InkBorderCard(modifier = Modifier.fillMaxWidth()) { previewContent() }
        } else {
            AppCard(modifier = Modifier.fillMaxWidth()) { previewContent() }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action — loading or button
        if (isStructuring) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Refining…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            ThemedButton(
                onClick = onRefine,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✨ Refine This Step")
            }
        }
    }
}
