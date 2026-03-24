package com.inventory.app.ui.screens.cook

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inventory.app.R
import com.inventory.app.data.local.entity.SavedRecipeEntity
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.domain.model.BuilderStep
import com.inventory.app.domain.model.IngredientMatcher
import com.inventory.app.domain.model.InventorySuggestion
import com.inventory.app.domain.model.RecipeIngredient
import com.inventory.app.domain.model.RecipeSource
import com.inventory.app.domain.model.RecipeStepManager
import com.inventory.app.domain.model.StepIngredient
import com.inventory.app.domain.model.autoParseTimerFromText
import com.inventory.app.domain.model.collectAllIngredients
import com.inventory.app.domain.model.parseStepsJson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

// ── UI State ───────────────────────────────────────────────────────────────

data class RecipeBuilderUiState(
    val isEditMode: Boolean = false,
    val recipeId: Long? = null,
    // Title card
    val recipeName: String = "",
    val servings: Int = 2,
    // Steps
    val steps: List<BuilderStep> = listOf(BuilderStep()),
    val currentPage: Int = 0,       // 0=title, 1..N=steps, N+1=review
    // Review card
    val collectedIngredients: List<RecipeIngredient> = emptyList(),
    val totalTimeMinutes: Int? = null,
    // Optional details
    val showDetails: Boolean = false,
    val cuisineOrigin: String = "",
    val difficulty: String = "",
    val mealType: String = "",
    val tags: List<String> = emptyList(),
    val tips: String = "",
    val coverPhotoUri: String? = null,
    // Inventory suggestion dropdown
    val inventorySuggestions: List<InventorySuggestion> = emptyList(),
    val ingredientQuery: String = "",
    // Save state
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val isDraft: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val lastAutoSaveTime: Long = 0,
    // Delete step confirmation
    val deleteConfirmStepIndex: Int? = null,
    // Capture mode
    val isCaptureMode: Boolean = false,
    val captureTimerStepIndex: Int? = null,   // which step's timer is actively counting up
    val captureTimerSeconds: Int = 0,          // elapsed seconds (counts up during recording)
    val captureTimerRunning: Boolean = false,
    // 6C: "Structure This" AI
    val structuringStepIndex: Int? = null,             // null = idle; non-null = AI call in flight
    val structuringStepsSnapshot: List<BuilderStep>? = null  // full steps list before structuring — enables undo
) {
    /** Dynamic page count: title + steps + review */
    val totalPages: Int get() = steps.size + 2

    /** True if the current page is the review card */
    val isOnReviewCard: Boolean get() = currentPage == totalPages - 1

    /** True if the current page is the title card */
    val isOnTitleCard: Boolean get() = currentPage == 0

    /** 0-based step index for the current page (null if on title or review) */
    val currentStepIndex: Int? get() = if (currentPage in 1..steps.size) currentPage - 1 else null
}

// ── One-shot events ────────────────────────────────────────────────────────

sealed class BuilderEvent {
    data class ShowToast(val message: String) : BuilderEvent()
    data class NavigateTo(val route: String) : BuilderEvent()
    data object SaveComplete : BuilderEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipeBuilderViewModel @Inject constructor(
    private val savedRecipeRepository: SavedRecipeRepository,
    private val itemRepository: ItemRepository,
    private val gson: Gson,
    @ApplicationContext private val context: Context,
    private val grokRepository: GrokRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeBuilderUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BuilderEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var autoSaveJob: Job? = null
    // C-3: mutex prevents auto-save and explicit save from running concurrently (double-insert race)
    private val saveMutex = Mutex()
    private var captureTimerJob: Job? = null

    // Debounced ingredient query channel
    private val _ingredientQuery = MutableStateFlow("")

    init {
        startAutoSaveTimer()
        observeIngredientQuery()
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        captureTimerJob?.cancel()
        cancelCaptureTimerNotification()
        super.onCleared()
    }

    // ── Load for edit mode ─────────────────────────────────────────────────

    fun loadRecipe(recipeId: Long) {
        viewModelScope.launch {
            val entity = savedRecipeRepository.getById(recipeId) ?: return@launch
            val steps = parseStepsJson(entity.stepsJson, gson)
            val builderSteps = if (steps.isEmpty()) {
                listOf(BuilderStep())
            } else {
                RecipeStepManager.fromRecipeSteps(steps)
            }

            // Old-format ingredient migration: if steps have no ingredients but entity does
            val stepsHaveIngredients = builderSteps.any { it.ingredients.isNotEmpty() }
            val finalSteps = if (!stepsHaveIngredients && entity.ingredientsJson.isNotBlank()
                && entity.ingredientsJson != "[]"
            ) {
                try {
                    val type = object : TypeToken<List<RecipeIngredient>>() {}.type
                    val globalIngredients: List<RecipeIngredient>? = gson.fromJson(entity.ingredientsJson, type)
                    if (!globalIngredients.isNullOrEmpty()) {
                        _events.emit(BuilderEvent.ShowToast("Ingredients moved to Step 1 — you can redistribute them"))
                        val migrated = builderSteps.toMutableList()
                        migrated[0] = migrated[0].copy(
                            ingredients = globalIngredients.map {
                                StepIngredient(name = it.name, amount = it.amount, unit = it.unit)
                            }
                        )
                        migrated
                    } else builderSteps
                } catch (_: Exception) {
                    builderSteps
                }
            } else builderSteps

            // Parse tags
            val tags = try {
                if (!entity.tags.isNullOrBlank()) {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(entity.tags, type) ?: emptyList()
                } else emptyList()
            } catch (_: Exception) { emptyList() }

            _uiState.update {
                it.copy(
                    isEditMode = true,
                    recipeId = entity.id,
                    recipeName = entity.name,
                    servings = entity.servings,
                    steps = finalSteps,
                    cuisineOrigin = entity.cuisineOrigin,
                    difficulty = entity.difficulty,
                    mealType = entity.mealType ?: "",
                    tags = tags,
                    tips = entity.tips ?: "",
                    coverPhotoUri = entity.coverPhotoUri,
                    hasUnsavedChanges = false,
                    currentPage = 0
                )
            }
        }
    }

    // ── Title card ─────────────────────────────────────────────────────────

    fun updateRecipeName(name: String) {
        _uiState.update { it.copy(recipeName = name, hasUnsavedChanges = true) }
    }

    fun updateServings(delta: Int) {
        _uiState.update {
            val new = (it.servings + delta).coerceIn(1, 50)
            it.copy(servings = new, hasUnsavedChanges = true)
        }
    }

    // ── Page navigation ────────────────────────────────────────────────────

    /** Called when the pager settles on a page (user swipe or programmatic). */
    fun onPageChanged(page: Int) {
        val state = _uiState.value
        val clamped = page.coerceIn(0, state.totalPages - 1)
        // When landing on review card, collect ingredients + compute time
        if (clamped == state.totalPages - 1) refreshReviewData()
        _uiState.update { it.copy(currentPage = clamped) }
    }

    fun navigateToPage(page: Int) {
        val state = _uiState.value
        val clamped = page.coerceIn(0, state.totalPages - 1)
        if (clamped == state.totalPages - 1) refreshReviewData()
        _uiState.update { it.copy(currentPage = clamped) }
    }

    fun navigateToReview() {
        refreshReviewData()
        _uiState.update { it.copy(currentPage = it.totalPages - 1) }
    }

    fun navigateToLastStep() {
        _uiState.update { it.copy(currentPage = it.steps.size) }
    }

    // ── Step management ────────────────────────────────────────────────────

    fun addNextStep() {
        val state = _uiState.value
        val currentStepIndex = state.currentStepIndex ?: (state.steps.lastIndex)
        // In capture mode: stamp current step with wall-clock time before advancing
        val stepsWithTimestamp = if (state.isCaptureMode) {
            state.steps.toMutableList().also { steps ->
                val current = steps.getOrNull(currentStepIndex)
                if (current != null && current.captureTimestamp == null) {
                    steps[currentStepIndex] = current.copy(captureTimestamp = System.currentTimeMillis())
                }
            }
        } else state.steps
        val newSteps = RecipeStepManager.insertAfter(stepsWithTimestamp, currentStepIndex)
        val newPage = currentStepIndex + 2  // +1 for title offset, +1 for new step
        _uiState.update {
            it.copy(
                steps = newSteps,
                currentPage = newPage.coerceIn(0, newSteps.size),
                hasUnsavedChanges = true
            )
        }
    }

    fun insertStepBefore(stepIndex: Int) {
        val state = _uiState.value
        val newSteps = RecipeStepManager.insertBefore(state.steps, stepIndex)
        val newPage = stepIndex + 1  // +1 for title card offset
        _uiState.update {
            it.copy(
                steps = newSteps,
                currentPage = newPage.coerceIn(0, newSteps.size),
                hasUnsavedChanges = true
            )
        }
    }

    fun requestDeleteStep(stepIndex: Int) {
        val state = _uiState.value
        if (state.steps.size <= 1) return
        val step = state.steps.getOrNull(stepIndex) ?: return
        // Skip confirmation if step is blank
        if (step.instruction.isBlank() && step.ingredients.isEmpty() && step.timerSeconds == null) {
            confirmDeleteStep(stepIndex)
        } else {
            _uiState.update { it.copy(deleteConfirmStepIndex = stepIndex) }
        }
    }

    fun confirmDeleteStep(stepIndex: Int) {
        val state = _uiState.value
        if (state.steps.size <= 1) {
            _uiState.update { it.copy(deleteConfirmStepIndex = null) }
            return
        }
        val newSteps = RecipeStepManager.deleteStep(state.steps, stepIndex)
        // Stay on same position or move back if we deleted the last step
        val newPage = if (stepIndex >= newSteps.size) {
            state.currentPage - 1
        } else {
            state.currentPage
        }.coerceIn(0, newSteps.size + 1)  // +1 for review card
        _uiState.update {
            it.copy(
                steps = newSteps,
                currentPage = newPage,
                deleteConfirmStepIndex = null,
                hasUnsavedChanges = true
            )
        }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(deleteConfirmStepIndex = null) }
    }

    // ── Step content ───────────────────────────────────────────────────────

    fun updateInstruction(stepIndex: Int, text: String) {
        _uiState.update { state ->
            val steps = state.steps.toMutableList()
            val step = steps.getOrNull(stepIndex) ?: return@update state
            // Auto-parse timer from text (debounced effect handled via LaunchedEffect in UI)
            // We update the instruction immediately; timer auto-parse runs in the UI side
            steps[stepIndex] = step.copy(instruction = text)
            state.copy(steps = steps, hasUnsavedChanges = true)
        }
    }

    /** Called after debounce when instruction text settles. Auto-fills timer if detected. */
    fun autoParseTimerForStep(stepIndex: Int, instruction: String) {
        val detected = autoParseTimerFromText(instruction)
        _uiState.update { state ->
            val steps = state.steps.toMutableList()
            val step = steps.getOrNull(stepIndex) ?: return@update state
            // Only auto-fill if user hasn't manually set a timer
            if (!step.timerAutoDetected && step.timerSeconds != null && detected == null) {
                return@update state  // user manually set a timer, don't touch it
            }
            if (step.timerAutoDetected || step.timerSeconds == null) {
                steps[stepIndex] = step.copy(
                    timerSeconds = detected,
                    timerAutoDetected = detected != null
                )
            }
            state.copy(steps = steps)
        }
    }

    fun setStepTimer(stepIndex: Int, seconds: Int?) {
        _uiState.update { state ->
            val steps = state.steps.toMutableList()
            val step = steps.getOrNull(stepIndex) ?: return@update state
            steps[stepIndex] = step.copy(timerSeconds = seconds, timerAutoDetected = false)
            state.copy(steps = steps, hasUnsavedChanges = true)
        }
    }

    // ── Ingredient management ──────────────────────────────────────────────

    fun addIngredientToStep(stepIndex: Int, name: String, amount: String, unit: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val steps = state.steps.toMutableList()
            val step = steps.getOrNull(stepIndex) ?: return@update state
            val updated = step.ingredients + StepIngredient(name.trim(), amount.trim(), unit.trim())
            steps[stepIndex] = step.copy(ingredients = updated)
            state.copy(steps = steps, hasUnsavedChanges = true)
        }
    }

    fun removeIngredientFromStep(stepIndex: Int, ingredientIndex: Int) {
        _uiState.update { state ->
            val steps = state.steps.toMutableList()
            val step = steps.getOrNull(stepIndex) ?: return@update state
            val updated = step.ingredients.toMutableList().also { it.removeAt(ingredientIndex) }
            steps[stepIndex] = step.copy(ingredients = updated)
            state.copy(steps = steps, hasUnsavedChanges = true)
        }
    }

    fun setIngredientQuery(query: String) {
        _uiState.update { it.copy(ingredientQuery = query) }
        viewModelScope.launch { _ingredientQuery.emit(query) }
    }

    fun clearIngredientSuggestions() {
        _uiState.update { it.copy(inventorySuggestions = emptyList(), ingredientQuery = "") }
    }

    private fun observeIngredientQuery() {
        viewModelScope.launch {
            _ingredientQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length < 2) {
                        _uiState.update { it.copy(inventorySuggestions = emptyList()) }
                        return@collect
                    }
                    val allItems = itemRepository.getAllActiveWithDetails().first()
                    val suggestions = allItems
                        .filter { IngredientMatcher.matches(it.item.name, query) }
                        .take(6)
                        .map { InventorySuggestion(it.item.id, it.item.name, it.unit?.abbreviation ?: "") }
                    _uiState.update { it.copy(inventorySuggestions = suggestions) }
                }
        }
    }

    // ── Review card ────────────────────────────────────────────────────────

    private fun refreshReviewData() {
        val state = _uiState.value
        val recipeSteps = RecipeStepManager.toRecipeSteps(state.steps)
        val collected = collectAllIngredients(recipeSteps)
        val totalSeconds = RecipeStepManager.calculateTotalTime(state.steps)
        val totalMinutes = totalSeconds?.let { it / 60 }
        _uiState.update {
            it.copy(
                collectedIngredients = collected,
                totalTimeMinutes = totalMinutes
            )
        }
    }

    // ── Optional details ───────────────────────────────────────────────────

    fun toggleShowDetails() {
        _uiState.update { it.copy(showDetails = !it.showDetails) }
    }

    fun updateCuisine(cuisine: String) {
        _uiState.update { it.copy(cuisineOrigin = cuisine, hasUnsavedChanges = true) }
    }

    fun updateDifficulty(difficulty: String) {
        _uiState.update { it.copy(difficulty = difficulty, hasUnsavedChanges = true) }
    }

    fun updateMealType(mealType: String) {
        _uiState.update { it.copy(mealType = mealType, hasUnsavedChanges = true) }
    }

    fun addTag(tag: String) {
        if (tag.isBlank()) return
        _uiState.update { it.copy(tags = (it.tags + tag.trim()).distinct(), hasUnsavedChanges = true) }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag, hasUnsavedChanges = true) }
    }

    fun updateTips(tips: String) {
        _uiState.update { it.copy(tips = tips, hasUnsavedChanges = true) }
    }

    fun updateCoverPhoto(uri: String?) {
        _uiState.update { it.copy(coverPhotoUri = uri, hasUnsavedChanges = true) }
    }

    // ── Save ───────────────────────────────────────────────────────────────

    fun save() {
        autoSaveJob?.cancel()
        captureTimerJob?.cancel()           // prevent race: timer must not fire after save
        cancelCaptureTimerNotification()
        viewModelScope.launch { performSave(isDraft = false) }
    }

    fun saveDraft() {
        viewModelScope.launch { performSave(isDraft = true) }
    }

    private suspend fun performSave(isDraft: Boolean) {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.recipeName.isBlank()) {
            if (!isDraft) _events.emit(BuilderEvent.ShowToast("Please add a recipe name"))
            return
        }
        if (state.steps.isEmpty() || state.steps.all { it.instruction.isBlank() }) {
            if (!isDraft) _events.emit(BuilderEvent.ShowToast("Please add at least one step"))
            return
        }

        saveMutex.withLock {
        // Re-read state inside lock: another coroutine may have saved while we waited
        if (_uiState.value.isSaving) return@withLock
        _uiState.update { it.copy(isSaving = true) }

        try {
            val recipeSteps = RecipeStepManager.toRecipeSteps(state.steps)
            val stepsJson = gson.toJson(recipeSteps)
            val collected = collectAllIngredients(recipeSteps)
            val ingredientsJson = gson.toJson(collected)
            val totalSeconds = RecipeStepManager.calculateTotalTime(state.steps)
            val totalMinutes = totalSeconds?.let { it / 60 } ?: 0
            val tagsJson = if (state.tags.isNotEmpty()) gson.toJson(state.tags) else null
            val now = System.currentTimeMillis()
            // Capture mode → "captured" source; write mode → "manual" source
            val source = if (state.isCaptureMode) RecipeSource.CAPTURED.value else RecipeSource.MANUAL.value

            if (state.recipeId != null) {
                // Update existing
                val existing = savedRecipeRepository.getById(state.recipeId)
                if (existing != null) {
                    savedRecipeRepository.update(
                        existing.copy(
                            name = state.recipeName,
                            servings = state.servings,
                            stepsJson = stepsJson,
                            ingredientsJson = ingredientsJson,
                            timeMinutes = totalMinutes,
                            cuisineOrigin = state.cuisineOrigin,
                            difficulty = state.difficulty.ifBlank { "easy" },
                            mealType = state.mealType.ifBlank { null },
                            tags = tagsJson,
                            tips = state.tips.ifBlank { null },
                            coverPhotoUri = state.coverPhotoUri,
                            source = source,
                            isDraft = isDraft,
                            updatedAt = now
                        )
                    )
                }
            } else {
                // Insert new
                val id = savedRecipeRepository.insert(
                    SavedRecipeEntity(
                        name = state.recipeName,
                        servings = state.servings,
                        stepsJson = stepsJson,
                        ingredientsJson = ingredientsJson,
                        timeMinutes = totalMinutes,
                        cuisineOrigin = state.cuisineOrigin,
                        difficulty = state.difficulty.ifBlank { "easy" },
                        mealType = state.mealType.ifBlank { null },
                        tags = tagsJson,
                        tips = state.tips.ifBlank { null },
                        coverPhotoUri = state.coverPhotoUri,
                        source = source,
                        isDraft = isDraft,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                // Update recipeId so subsequent auto-saves use update path
                _uiState.update { it.copy(recipeId = id) }
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    hasUnsavedChanges = false,
                    isDraft = isDraft,
                    lastAutoSaveTime = if (isDraft) System.currentTimeMillis() else it.lastAutoSaveTime,
                    saveComplete = !isDraft
                )
            }

            if (!isDraft) {
                _events.emit(BuilderEvent.SaveComplete)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false) }
            if (!isDraft) _events.emit(BuilderEvent.ShowToast("Save failed — please try again"))
        }
        } // end saveMutex.withLock
    }

    // ── Capture mode ───────────────────────────────────────────────────────

    /** Called from AppNavigation after screen creation when captureMode=true nav arg is present. */
    fun enableCaptureMode() {
        _uiState.update { it.copy(isCaptureMode = true) }
    }

    /** Start counting up for the given step — records how long it actually takes to cook. */
    fun startCaptureTimer(stepIndex: Int) {
        captureTimerJob?.cancel()
        _uiState.update { it.copy(captureTimerStepIndex = stepIndex, captureTimerSeconds = 0, captureTimerRunning = true) }
        captureTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(captureTimerSeconds = it.captureTimerSeconds + 1) }
            }
        }
        postCaptureTimerNotification(stepIndex)
    }

    /** Stop capture timer for this step and record elapsed duration into the step. */
    fun stopCaptureTimer(stepIndex: Int) {
        captureTimerJob?.cancel()
        captureTimerJob = null
        val elapsed = _uiState.value.captureTimerSeconds
        _uiState.update { state ->
            val newSteps = state.steps.toMutableList()
            newSteps.getOrNull(stepIndex)?.let { step ->
                newSteps[stepIndex] = step.copy(timerSeconds = elapsed, timerAutoDetected = false)
            }
            state.copy(
                captureTimerRunning = false,
                captureTimerStepIndex = null,
                captureTimerSeconds = 0,
                steps = newSteps,
                hasUnsavedChanges = true
            )
        }
        cancelCaptureTimerNotification()
    }

    private fun postCaptureTimerNotification(stepIndex: Int) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, "cooking_timers")
            .setSmallIcon(R.drawable.ic_ink_clock)
            .setContentTitle(_uiState.value.recipeName.ifBlank { "Capturing recipe" })
            .setContentText("Step ${stepIndex + 1} — recording duration")
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        nm.notify(CAPTURE_TIMER_NOTIF_ID, notification)
    }

    private fun cancelCaptureTimerNotification() {
        context.getSystemService(NotificationManager::class.java)?.cancel(CAPTURE_TIMER_NOTIF_ID)
    }

    // ── Structure This (6C) ────────────────────────────────────────────────

    /** Sends step text to Groq to be cleaned up or split into multiple steps. */
    fun structureStep(stepIndex: Int) {
        val state = _uiState.value
        val step = state.steps.getOrNull(stepIndex) ?: return
        if (step.instruction.isBlank()) return

        // Inline connectivity check via context (no NetworkUtils class needed)
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val isOnline = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!isOnline) {
            viewModelScope.launch { _events.emit(BuilderEvent.ShowToast("No connection — structure requires internet")) }
            return
        }

        // Snapshot full steps list for undo before anything changes
        _uiState.update { it.copy(structuringStepIndex = stepIndex, structuringStepsSnapshot = state.steps) }

        viewModelScope.launch {
            val result = grokRepository.chatCompletion(
                systemPrompt = "You are a recipe assistant. Return ONLY valid JSON. No markdown, no explanation.",
                userPrompt = """The user typed this into a recipe step field:
"${step.instruction}"

Your job:
- If this describes a single cooking action, return it as 1 clean step.
- If this clearly describes multiple sequential cooking actions, split into 2 to 5 steps.
- NEVER invent ingredients, quantities, or actions not mentioned in the text.
- NEVER add tips, advice, or explanations — only what the user wrote.

Return ONLY a JSON array:
[{"instruction":"step text","timerSeconds":null,"ingredients":[{"name":"","amount":"","unit":""}]}]

If you cannot parse it as a recipe step, return the original text as a single step with empty ingredients array and null timerSeconds.""",
                temperature = 0.1,
                maxTokens = 1024
            )

            result.fold(
                onSuccess = { text ->
                    try {
                        // Strip markdown fences if present
                        val cleaned = text.trim()
                            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        val type = object : com.google.gson.reflect.TypeToken<List<com.inventory.app.domain.model.RecipeStep>>() {}.type
                        val recipeSteps: List<com.inventory.app.domain.model.RecipeStep> = gson.fromJson(cleaned, type)
                        if (recipeSteps.isEmpty()) throw IllegalStateException("Empty result")

                        val builderSteps = recipeSteps.map { rs ->
                            BuilderStep(
                                instruction = rs.instruction,
                                timerSeconds = rs.timerSeconds,
                                ingredients = rs.ingredients
                                    .filter { it.name.isNotBlank() }
                                    .map { StepIngredient(it.name, it.amount, it.unit) }
                            )
                        }
                        val currentSteps = _uiState.value.steps  // re-read in case of concurrent update
                        val newSteps = RecipeStepManager.replaceStepWithMany(currentSteps, stepIndex, builderSteps)
                        _uiState.update { it.copy(steps = newSteps, structuringStepIndex = null, hasUnsavedChanges = true) }
                        if (builderSteps.size > 1) {
                            _events.emit(BuilderEvent.ShowToast("Split into ${builderSteps.size} steps — swipe to review"))
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(structuringStepIndex = null, structuringStepsSnapshot = null) }
                        _events.emit(BuilderEvent.ShowToast("Couldn't structure — try again"))
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(structuringStepIndex = null, structuringStepsSnapshot = null) }
                    _events.emit(BuilderEvent.ShowToast("Couldn't structure — try again"))
                }
            )
        }
    }

    /** Reverts steps to the pre-structure snapshot. Only available immediately after structuring. */
    fun undoStructure() {
        val snapshot = _uiState.value.structuringStepsSnapshot ?: return
        _uiState.update { it.copy(steps = snapshot, structuringStepsSnapshot = null, hasUnsavedChanges = true) }
    }

    // ── Auto-save ──────────────────────────────────────────────────────────

    private fun startAutoSaveTimer() {
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                if (_uiState.value.hasUnsavedChanges && _uiState.value.recipeName.isNotBlank()) {
                    performSave(isDraft = true)
                }
            }
        }
    }

    companion object {
        private const val CAPTURE_TIMER_NOTIF_ID = 9001
    }
}
