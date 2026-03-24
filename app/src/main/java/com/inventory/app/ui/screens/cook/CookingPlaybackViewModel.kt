package com.inventory.app.ui.screens.cook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.inventory.app.data.local.entity.CookingLogEntity
import com.inventory.app.data.local.entity.relations.ItemWithDetails
import com.inventory.app.data.local.entity.ShoppingListItemEntity
import com.inventory.app.data.repository.CookingLogRepository
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SavedRecipeRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.ShoppingListRepository
import com.inventory.app.data.repository.UsageRepository
import com.inventory.app.domain.model.DeductionCalculator
import com.inventory.app.domain.model.DeductionItem
import com.inventory.app.domain.model.RecipeStep
import com.inventory.app.domain.model.collectAllIngredients
import com.inventory.app.domain.model.parseStepsJson
import com.inventory.app.worker.CookingTimerService
import com.inventory.app.worker.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.compositionLocalOf
import javax.inject.Inject

// ── Volume button handler (Activity ↔ Compose bridge) ────────────────────────

class VolumeHandlerState {
    private var handler: ((isVolumeUp: Boolean) -> Boolean)? = null

    fun setHandler(h: (isVolumeUp: Boolean) -> Boolean) { handler = h }
    fun clearHandler() { handler = null }

    /** Returns true if the volume press was consumed (step navigation happened) */
    fun onVolumeKey(isVolumeUp: Boolean): Boolean = handler?.invoke(isVolumeUp) ?: false
}

val LocalVolumeHandler = compositionLocalOf { VolumeHandlerState() }

// ── UI State ──────────────────────────────────────────────────────────────────

data class PlaybackUiState(
    val recipeName: String = "",
    val recipeId: Long = 0,
    val steps: List<RecipeStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val isComplete: Boolean = false,
    val originalServings: Int = 2,
    val selectedServings: Int = 2,
    val scalingFactor: Float = 1f,
    val hasStarted: Boolean = false,
    val activeTimers: Map<Int, TimerState> = emptyMap(),
    val timerCompletedStepIndex: Int? = null,
    val showDeductionDetails: Boolean = false,
    val deductionItems: List<DeductionItem> = emptyList(),
    val deductableCount: Int = 0,
    val isDeducting: Boolean = false,
    val deductionDone: Boolean = false,
    val showExitDialog: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val userRating: Int = 0,
    val missingIngredientCount: Int = 0,
    val missingIngredientNames: List<String> = emptyList(),
    val shoppingAddedFromPreStart: Boolean = false,
    val restockableCount: Int = 0,
    val shoppingAddedFromCompletion: Boolean = false,
    val completedSteps: Set<Int> = emptySet(),
    val preStartDeductions: List<DeductionItem> = emptyList(),
    val showVolumeTip: Boolean = false
)

private const val PLAYBACK_RECIPE_ID = "playback_recipe_id"
private const val PLAYBACK_STEP_INDEX = "playback_step_index"
private const val PLAYBACK_SERVINGS = "playback_servings"
private const val PLAYBACK_TIMESTAMP = "playback_timestamp"
private const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L
private const val KEY_VOLUME_TIP_COUNT = "playback_volume_tip_count"

@HiltViewModel
class CookingPlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedRecipeRepository: SavedRecipeRepository,
    private val itemRepository: ItemRepository,
    private val usageRepository: UsageRepository,
    private val cookingLogRepository: CookingLogRepository,
    private val settingsRepository: SettingsRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var timerService: CookingTimerService? = null
    private var serviceBound = false
    private var inventoryItems: List<ItemWithDetails> = emptyList()
    private var explicitExit = false
    // C-1: track collector job so it can be cancelled before re-subscribing on reconnect
    private var timerCollectorJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            timerService = (binder as CookingTimerService.CookingTimerBinder).getService()
            serviceBound = true
            val state = _uiState.value
            timerService?.setRecipeContext(state.recipeId, state.recipeName)

            // C-1: cancel any existing collector before launching a new one (config change safety)
            timerCollectorJob?.cancel()
            timerCollectorJob = viewModelScope.launch {
                timerService?.timerStates?.collect { timers ->
                    val previousTimers = _uiState.value.activeTimers
                    // Detect newly completed timers (just hit 0 from a running state)
                    val completedStep = timers.entries.firstOrNull { (idx, ts) ->
                        ts.remainingSeconds == 0 && !ts.isRunning &&
                            (previousTimers[idx]?.remainingSeconds ?: 1) > 0
                    }?.key
                    _uiState.update { it.copy(
                        activeTimers = timers,
                        timerCompletedStepIndex = completedStep ?: it.timerCompletedStepIndex
                    )}
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
            timerService = null
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────

    fun loadRecipe(recipeId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val entity = savedRecipeRepository.getById(recipeId)
                if (entity == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Recipe not found") }
                    return@launch
                }
                val steps = parseStepsJson(entity.stepsJson, gson)
                inventoryItems = itemRepository.getAllActiveWithDetails().first()

                // C-2: Pre-flight check — full deduction list for ingredient checklist
                val allIngredients = collectAllIngredients(steps)
                val allDeductions = if (allIngredients.isNotEmpty()) {
                    DeductionCalculator.calculateDeductions(allIngredients, 1f, inventoryItems)
                } else emptyList()
                val missingIngredients = allDeductions.filter {
                    !it.canDeduct && it.cannotDeductReason == "not in inventory"
                }

                _uiState.update { it.copy(
                    recipeId = recipeId,
                    recipeName = entity.name,
                    steps = steps,
                    totalSteps = steps.size,
                    originalServings = entity.servings,
                    selectedServings = entity.servings,
                    scalingFactor = 1f,
                    missingIngredientCount = missingIngredients.size,
                    missingIngredientNames = missingIngredients.map { it.ingredientName },
                    preStartDeductions = allDeductions,
                    isLoading = false
                )}

                // Volume tip: show first 3 times user opens playback
                val tipCount = settingsRepository.getInt(KEY_VOLUME_TIP_COUNT, 0)
                if (tipCount < 3) {
                    _uiState.update { it.copy(showVolumeTip = true) }
                    settingsRepository.setInt(KEY_VOLUME_TIP_COUNT, tipCount + 1)
                }

                // B-3: Restore active session if within 4-hour window
                val savedId = settingsRepository.getString(PLAYBACK_RECIPE_ID, "0").toLongOrNull() ?: 0L
                val savedTs = settingsRepository.getString(PLAYBACK_TIMESTAMP, "0").toLongOrNull() ?: 0L
                val isSessionValid = savedId == recipeId
                    && savedTs > 0L
                    && (System.currentTimeMillis() - savedTs) < FOUR_HOURS_MS
                    && steps.isNotEmpty()

                if (isSessionValid) {
                    val savedStep = settingsRepository.getString(PLAYBACK_STEP_INDEX, "0").toIntOrNull() ?: 0
                    val savedServings = settingsRepository.getString(PLAYBACK_SERVINGS, entity.servings.toString())
                        .toIntOrNull() ?: entity.servings
                    val original = entity.servings.takeIf { it > 0 } ?: 1
                    val clampedStep = savedStep.coerceIn(0, steps.size - 1)
                    _uiState.update { it.copy(
                        currentStepIndex = clampedStep,
                        selectedServings = savedServings,
                        scalingFactor = savedServings.toFloat() / original.toFloat(),
                        hasStarted = true,
                        completedSteps = (0 until clampedStep).toSet()
                    )}
                    // Re-bind to timer service (may still be running)
                    val serviceIntent = Intent(context, CookingTimerService::class.java)
                    context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Servings & scaling ────────────────────────────────────────────────

    fun setServings(n: Int) {
        if (n < 1) return
        val original = _uiState.value.originalServings
        if (original <= 0) return  // H-5: prevent divide-by-zero
        val newFactor = n.toFloat() / original.toFloat()

        // Recalculate pre-start deductions with new scaling
        val steps = _uiState.value.steps
        val allIngredients = collectAllIngredients(steps)
        val recalculated = if (allIngredients.isNotEmpty()) {
            DeductionCalculator.calculateDeductions(allIngredients, newFactor, inventoryItems)
        } else emptyList()
        val missing = recalculated.filter {
            !it.canDeduct && it.cannotDeductReason == "not in inventory"
        }

        _uiState.update { it.copy(
            selectedServings = n,
            scalingFactor = newFactor,
            preStartDeductions = recalculated,
            missingIngredientCount = missing.size,
            missingIngredientNames = missing.map { it.ingredientName }
        )}
    }

    // ── Cooking start ─────────────────────────────────────────────────────

    fun startCooking() {
        val state = _uiState.value
        _uiState.update { it.copy(hasStarted = true) }
        saveSession(state.recipeId, 0, state.selectedServings)

        // Start + bind to timer service
        val serviceIntent = Intent(context, CookingTimerService::class.java)
        context.startService(serviceIntent)
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // ── Navigation ────────────────────────────────────────────────────────

    fun nextStep() {
        val state = _uiState.value
        if (state.currentStepIndex < state.totalSteps - 1) {
            val next = state.currentStepIndex + 1
            _uiState.update { it.copy(
                currentStepIndex = next,
                completedSteps = it.completedSteps + it.currentStepIndex
            )}
            saveSession(state.recipeId, next, state.selectedServings)

            // Auto-advance: if the completed timer was for this step and it's not the last, advance
        } else {
            // Last step — go to completion
            finishCooking()
        }
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.currentStepIndex > 0) {
            val prev = state.currentStepIndex - 1
            _uiState.update { it.copy(currentStepIndex = prev) }
            saveSession(state.recipeId, prev, state.selectedServings)
        }
    }

    // ── Timer delegation ──────────────────────────────────────────────────

    fun startTimer(stepIndex: Int) {
        val step = _uiState.value.steps.getOrNull(stepIndex) ?: return
        val seconds = step.timerSeconds ?: return
        val label = "Step ${stepIndex + 1}"
        timerService?.startTimer(stepIndex, seconds, label)
    }

    fun pauseTimer(stepIndex: Int) {
        timerService?.pauseTimer(stepIndex)
    }

    fun cancelTimer(stepIndex: Int) {
        timerService?.cancelTimer(stepIndex)
    }

    fun clearTimerCompletion() {
        _uiState.update { it.copy(timerCompletedStepIndex = null) }
    }

    fun jumpToStep(index: Int) {
        val state = _uiState.value
        if (index !in 0 until state.totalSteps) return
        if (!state.hasStarted || state.isComplete) return
        _uiState.update { it.copy(currentStepIndex = index) }
        saveSession(state.recipeId, index, state.selectedServings)
    }

    // ── Completion & deduction ────────────────────────────────────────────

    private fun finishCooking() {
        val state = _uiState.value
        val deductionItems = DeductionCalculator.calculateDeductions(
            ingredients = collectAllIngredients(state.steps),
            scalingFactor = state.scalingFactor,
            inventoryItems = inventoryItems
        )
        val restockableCount = deductionItems.count {
            !it.canDeduct && it.cannotDeductReason == "not in inventory"
        }
        _uiState.update { it.copy(
            isComplete = true,
            deductionItems = deductionItems,
            deductableCount = deductionItems.count { it.canDeduct },
            restockableCount = restockableCount
        )}
    }

    fun toggleDeductionItem(index: Int) {
        _uiState.update { state ->
            val updated = state.deductionItems.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(isChecked = !updated[index].isChecked)
            }
            state.copy(deductionItems = updated)
        }
    }

    fun toggleDeductionDetails() {
        _uiState.update { it.copy(showDeductionDetails = !it.showDeductionDetails) }
    }

    fun confirmDeduction() {
        val state = _uiState.value
        _uiState.update { it.copy(isDeducting = true) }
        viewModelScope.launch {
            // C-2: NonCancellable ensures deductions + log complete even if VM cleared mid-op
            withContext(NonCancellable) {
            val deducted = state.deductionItems.filter { it.canDeduct && it.isChecked }
            deducted.forEach { item ->
                item.matchedItemId?.let { itemId ->
                    val deductAmount = item.convertedAmount ?: item.scaledAmount ?: 0.0
                    usageRepository.logUsage(
                        itemId = itemId,
                        quantity = deductAmount,
                        usageType = "cooked",
                        notes = "Cooked: ${state.recipeName}"
                    )
                    // C-3: Auto-add to shopping list if item falls below threshold
                    val matchedInv = inventoryItems.firstOrNull { it.item.id == itemId }
                    if (matchedInv != null) {
                        val estimatedNewQty = matchedInv.item.quantity - deductAmount
                        val threshold = matchedInv.item.minQuantity.takeIf { it > 0.0 }
                            ?: matchedInv.item.smartMinQuantity ?: 0.0
                        if (threshold > 0.0 && estimatedNewQty <= threshold) {
                            shoppingListRepository.addItem(ShoppingListItemEntity(
                                itemId = matchedInv.item.id,
                                customName = matchedInv.item.name,
                                notes = "Auto-added: low stock after cooking",
                                quantity = threshold
                            ))
                        }
                    }
                }
            }
            // Log the cooking session
            val deductedJson = gson.toJson(deducted)
            cookingLogRepository.insert(
                CookingLogEntity(
                    recipeId = state.recipeId,
                    servings = state.selectedServings,
                    deductedItemsJson = deductedJson
                )
            )
                clearSessionSync()
            }
            _uiState.update { it.copy(isDeducting = false, deductionDone = true) }
        }
    }

    fun skipDeduction() {
        viewModelScope.launch {
            val state = _uiState.value
            withContext(NonCancellable) {
                cookingLogRepository.insert(
                    CookingLogEntity(
                        recipeId = state.recipeId,
                        servings = state.selectedServings,
                        deductedItemsJson = null
                    )
                )
                clearSessionSync()
            }
            _uiState.update { it.copy(deductionDone = true) }
        }
    }

    // ── Rating ─────────────────────────────────────────────────────────────

    fun rateRecipe(stars: Int) {
        if (stars !in 1..5) return
        val recipeId = _uiState.value.recipeId
        if (recipeId == 0L) return
        viewModelScope.launch {
            savedRecipeRepository.updateRating(recipeId, stars)
            _uiState.update { it.copy(userRating = stars) }
        }
    }

    // ── Shopping list helpers ──────────────────────────────────────────────

    fun addMissingToShoppingListFromPreStart() {
        val state = _uiState.value
        if (state.shoppingAddedFromPreStart || state.missingIngredientNames.isEmpty()) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                state.missingIngredientNames.forEach { name ->
                    shoppingListRepository.addItem(ShoppingListItemEntity(
                        customName = name,
                        notes = "For: ${state.recipeName}",
                        quantity = 1.0
                    ))
                }
            }
            _uiState.update { it.copy(shoppingAddedFromPreStart = true) }
        }
    }

    fun addNotInInventoryToShoppingList() {
        val state = _uiState.value
        if (state.shoppingAddedFromCompletion) return
        val toAdd = state.deductionItems.filter {
            !it.canDeduct && it.cannotDeductReason == "not in inventory"
        }
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                toAdd.forEach { item ->
                    shoppingListRepository.addItem(ShoppingListItemEntity(
                        customName = item.ingredientName,
                        notes = "Restock after cooking: ${state.recipeName}",
                        quantity = 1.0
                    ))
                }
            }
            _uiState.update { it.copy(shoppingAddedFromCompletion = true) }
        }
    }

    // ── Exit handling ─────────────────────────────────────────────────────

    fun showExitDialog() { _uiState.update { it.copy(showExitDialog = true) } }
    fun dismissExitDialog() { _uiState.update { it.copy(showExitDialog = false) } }

    fun onExitConfirmed() {
        explicitExit = true
        timerService?.cancelAll()
        // H-8: NonCancellable ensures session cleared even if back-stack pop cancels viewModelScope
        viewModelScope.launch {
            withContext(NonCancellable) { clearSessionSync() }
        }
    }

    // ── Session persistence ───────────────────────────────────────────────

    private fun saveSession(recipeId: Long, stepIndex: Int, servings: Int) {
        viewModelScope.launch {
            settingsRepository.set(PLAYBACK_RECIPE_ID, recipeId.toString(), description = null)
            settingsRepository.set(PLAYBACK_STEP_INDEX, stepIndex.toString(), description = null)
            settingsRepository.set(PLAYBACK_SERVINGS, servings.toString(), description = null)
            settingsRepository.set(PLAYBACK_TIMESTAMP, System.currentTimeMillis().toString(), description = null)
        }
    }

    /** Clears session persistence. Must be called from within a coroutine context. */
    private suspend fun clearSessionSync() {
        settingsRepository.set(PLAYBACK_RECIPE_ID, "", description = null)
        settingsRepository.set(PLAYBACK_STEP_INDEX, "0", description = null)
        settingsRepository.set(PLAYBACK_SERVINGS, "2", description = null)
        settingsRepository.set(PLAYBACK_TIMESTAMP, "0", description = null)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCleared() {
        timerCollectorJob?.cancel()
        if (serviceBound) {
            try { context.unbindService(serviceConnection) }
            catch (_: IllegalArgumentException) { /* already unbound */ }
            serviceBound = false
        }
        // Only cancel timers if user explicitly chose to exit (not on config change)
        if (explicitExit) {
            timerService?.cancelAll()
        }
        super.onCleared()
    }
}
