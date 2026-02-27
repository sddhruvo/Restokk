package com.inventory.app.ui.screens.onboarding

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.local.entity.ItemEntity
import com.inventory.app.data.repository.BarcodeRepository
import com.inventory.app.data.repository.CategoryRepository
import com.inventory.app.data.repository.GrokRepository
import com.inventory.app.data.repository.ImageConfig
import com.inventory.app.data.repository.ItemRepository
import com.inventory.app.data.repository.SettingsRepository
import com.inventory.app.data.repository.StorageLocationRepository
import com.inventory.app.data.repository.UnitRepository
import com.inventory.app.domain.model.ItemDefaults
import com.inventory.app.domain.model.SmartDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

// ─── Act 2 Step State Machine ──────────────────────────────────────────────

sealed class Act2Step {
    data object PathSelection : Act2Step()
    data object TypeInput : Act2Step()
    data class Reveal(val item: RevealItem) : Act2Step()
    data class AddMore(val count: Int) : Act2Step()
    data object MemorySelection : Act2Step()
    data class MemoryReveal(val items: List<RevealItem>) : Act2Step()
    data object MemoryCelebration : Act2Step()
    data object CameraProcessing : Act2Step()
    data object BarcodeScanning : Act2Step()
    data object TransitionToDashboard : Act2Step()
}

/** Path the user chose on Screen 3. */
enum class Act2Path { TYPE, MEMORY, CAMERA, BARCODE }

/** Data shown on the RevealCard. */
data class RevealItem(
    val name: String,
    val category: String? = null,
    val location: String? = null,
    val shelfLifeDays: Int? = null,
    val unit: String? = null,
    val source: Act2Path = Act2Path.TYPE
)

/** Pre-resolved memory item for the grid. */
data class MemoryGridItem(
    val name: String,
    val defaults: ItemDefaults?,
    val isSelected: Boolean = false
)

// ─── UI State ──────────────────────────────────────────────────────────────

/** Camera processing phase indicator. */
enum class CameraPhase { PROCESSING, SLOW, VERY_SLOW, FAILED }

data class FirstMagicUiState(
    val step: Act2Step = Act2Step.PathSelection,
    val chosenPath: Act2Path? = null,
    val savedItemCount: Int = 0,
    val typedName: String = "",
    val smartDefaults: ItemDefaults? = null,
    val memoryItems: List<MemoryGridItem> = emptyList(),
    val isSaving: Boolean = false,
    // Camera path
    val cameraPhase: CameraPhase = CameraPhase.PROCESSING,
    val cameraError: String? = null,
    // Barcode path
    val isLookingUpBarcode: Boolean = false,
    val barcodeNotFound: Boolean = false,
    val lastScannedBarcode: String? = null
) {
    val memorySelectedCount: Int get() = memoryItems.count { it.isSelected }
}

// ─── ViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class FirstMagicViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val storageLocationRepository: StorageLocationRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val grokRepository: GrokRepository,
    private val barcodeRepository: BarcodeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val MEMORY_ITEMS = listOf(
            "Milk", "Eggs", "Bread", "Rice", "Pasta", "Chicken",
            "Tomatoes", "Onions", "Garlic", "Salt", "Olive Oil",
            "Butter", "Cheese", "Apples", "Coffee"
        )
        private const val SS_STEP = "fm_step"
        private const val SS_PATH = "fm_path"
        private const val SS_SAVED_COUNT = "fm_saved_count"
    }

    private val _uiState = MutableStateFlow(FirstMagicUiState())
    val uiState: StateFlow<FirstMagicUiState> = _uiState.asStateFlow()

    private var regionCode: String = "US"

    init {
        // Restore minimal state from SavedStateHandle
        val savedCount = savedStateHandle.get<Int>(SS_SAVED_COUNT) ?: 0
        val savedPath = savedStateHandle.get<String>(SS_PATH)?.let {
            try { Act2Path.valueOf(it) } catch (_: Exception) { null }
        }
        val restoredStep = savedStateHandle.get<String>(SS_STEP)?.let { stepName ->
            when (stepName) {
                "TypeInput" -> Act2Step.TypeInput
                "BarcodeScanning" -> Act2Step.BarcodeScanning
                "MemorySelection" -> Act2Step.MemorySelection
                else -> null // Camera/Reveal/AddMore etc → stay at PathSelection
            }
        } ?: Act2Step.PathSelection
        _uiState.update { it.copy(
            savedItemCount = savedCount,
            chosenPath = savedPath,
            step = restoredStep
        ) }

        // Auto-persist step changes
        viewModelScope.launch {
            _uiState.collect { persistStep(it.step) }
        }

        // Load region code and pre-resolve memory items
        viewModelScope.launch {
            regionCode = settingsRepository.getRegionCode()
            val memItems = MEMORY_ITEMS.map { name ->
                MemoryGridItem(
                    name = name,
                    defaults = SmartDefaults.lookup(name, regionCode)
                )
            }
            _uiState.update { it.copy(memoryItems = memItems) }
        }
    }

    private fun persistStep(step: Act2Step) {
        val stepName = when (step) {
            is Act2Step.PathSelection -> "PathSelection"
            is Act2Step.TypeInput -> "TypeInput"
            is Act2Step.BarcodeScanning -> "BarcodeScanning"
            is Act2Step.MemorySelection -> "MemorySelection"
            is Act2Step.CameraProcessing -> "CameraProcessing"
            is Act2Step.Reveal -> "Reveal"
            is Act2Step.AddMore -> "AddMore"
            is Act2Step.MemoryReveal -> "MemoryReveal"
            is Act2Step.MemoryCelebration -> "MemoryCelebration"
            is Act2Step.TransitionToDashboard -> "TransitionToDashboard"
        }
        savedStateHandle[SS_STEP] = stepName
    }

    // ─── Path Selection ────────────────────────────────────────────────

    fun onPathSelected(path: Act2Path) {
        savedStateHandle[SS_PATH] = path.name
        _uiState.update { state ->
            when (path) {
                Act2Path.TYPE -> state.copy(
                    step = Act2Step.TypeInput,
                    chosenPath = path,
                    typedName = "",
                    smartDefaults = null
                )
                Act2Path.MEMORY -> state.copy(
                    step = Act2Step.MemorySelection,
                    chosenPath = path
                )
                Act2Path.CAMERA -> state.copy(
                    chosenPath = path
                    // Don't set step yet — PathChoicePage launches camera first
                )
                Act2Path.BARCODE -> state.copy(
                    step = Act2Step.BarcodeScanning,
                    chosenPath = path,
                    barcodeNotFound = false,
                    lastScannedBarcode = null
                )
            }
        }
    }

    fun onSkip(onComplete: () -> Unit) {
        completeAct2(onComplete)
    }

    // ─── Type Path ─────────────────────────────────────────────────────

    fun onTypedNameChange(name: String) {
        val defaults = if (name.length >= 2) {
            SmartDefaults.lookup(name.trim(), regionCode)
        } else null
        _uiState.update { it.copy(typedName = name, smartDefaults = defaults) }
    }

    fun onSubmitTypedItem() {
        val state = _uiState.value
        val name = state.typedName.trim()
        if (name.isBlank()) return

        val revealItem = RevealItem(
            name = name,
            category = state.smartDefaults?.category,
            location = state.smartDefaults?.location,
            shelfLifeDays = state.smartDefaults?.shelfLifeDays,
            unit = state.smartDefaults?.unit,
            source = Act2Path.TYPE
        )
        _uiState.update { it.copy(step = Act2Step.Reveal(revealItem)) }
    }

    fun onSaveRevealedItem(item: RevealItem) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            resolveAndSaveItem(item)
            val newCount = _uiState.value.savedItemCount + 1
            savedStateHandle[SS_SAVED_COUNT] = newCount
            _uiState.update { it.copy(
                savedItemCount = newCount,
                isSaving = false,
                step = Act2Step.AddMore(newCount)
            )}
        }
    }

    fun correctItemName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val defaults = SmartDefaults.lookup(trimmed, regionCode)
        val step = _uiState.value.step
        val source = (step as? Act2Step.Reveal)?.item?.source ?: Act2Path.TYPE
        val corrected = RevealItem(
            name = trimmed,
            category = defaults?.category,
            location = defaults?.location,
            shelfLifeDays = defaults?.shelfLifeDays,
            unit = defaults?.unit,
            source = source
        )
        _uiState.update { it.copy(step = Act2Step.Reveal(corrected)) }
    }

    fun onAddAnother() {
        _uiState.update { it.copy(
            step = Act2Step.TypeInput,
            typedName = "",
            smartDefaults = null
        )}
    }

    fun onDoneAdding(onComplete: () -> Unit) {
        completeAct2(onComplete)
    }

    // ─── Memory Path ───────────────────────────────────────────────────

    fun onToggleMemoryItem(index: Int) {
        _uiState.update { state ->
            val items = state.memoryItems.toMutableList()
            if (index in items.indices) {
                items[index] = items[index].copy(isSelected = !items[index].isSelected)
            }
            state.copy(memoryItems = items)
        }
    }

    fun onConfirmMemoryItems() {
        val state = _uiState.value
        val selected = state.memoryItems.filter { it.isSelected }
        if (selected.isEmpty()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val revealItems = selected.map { mem ->
                val item = RevealItem(
                    name = mem.name,
                    category = mem.defaults?.category,
                    location = mem.defaults?.location,
                    shelfLifeDays = mem.defaults?.shelfLifeDays,
                    unit = mem.defaults?.unit,
                    source = Act2Path.MEMORY
                )
                resolveAndSaveItem(item)
                item
            }
            savedStateHandle[SS_SAVED_COUNT] = revealItems.size
            _uiState.update { it.copy(
                savedItemCount = revealItems.size,
                isSaving = false,
                step = Act2Step.MemoryReveal(revealItems)
            )}
        }
    }

    fun onMemoryRevealComplete() {
        _uiState.update { it.copy(step = Act2Step.MemoryCelebration) }
    }

    fun onMemoryCelebrationDone(onComplete: () -> Unit) {
        completeAct2(onComplete)
    }

    // ─── Camera Path ────────────────────────────────────────────────────

    private var cameraTimeoutJob: Job? = null

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(
            step = Act2Step.CameraProcessing,
            cameraPhase = CameraPhase.PROCESSING,
            cameraError = null
        )}

        // Timeout escalation: 5s → "slow", 12s → "very slow"
        cameraTimeoutJob?.cancel()
        cameraTimeoutJob = viewModelScope.launch {
            delay(5_000)
            _uiState.update { it.copy(cameraPhase = CameraPhase.SLOW) }
            delay(7_000) // 12s total
            _uiState.update { it.copy(cameraPhase = CameraPhase.VERY_SLOW) }
        }

        viewModelScope.launch {
            try {
                val config = grokRepository.getImageConfig()
                val base64 = compressAndEncode(bitmap, config)
                if (base64 == null) {
                    cameraTimeoutJob?.cancel()
                    _uiState.update { it.copy(cameraPhase = CameraPhase.FAILED, cameraError = "Couldn't process photo") }
                    return@launch
                }

                val result = grokRepository.visionAnalysis(
                    systemPrompt = """You identify kitchen/grocery items from photos. Return exactly ONE item as JSON:
{"name":"item name","category":"Dairy/Produce/Meat/Bakery/Beverages/Snacks/Condiments/Frozen/Canned/Grains/Spices/Other","location":"Fridge/Freezer/Pantry/Counter","shelfLifeDays":7,"unit":"pcs/kg/L/g/ml"}
Only the most prominent/visible item. Be specific (e.g. "Whole Milk" not just "Milk"). If uncertain about a field, omit it. Return ONLY the JSON object, no other text.""",
                    userPrompt = "What is the main item in this photo?",
                    imageBase64 = base64,
                    maxTokens = 256
                )

                cameraTimeoutJob?.cancel()

                result.fold(
                    onSuccess = { response ->
                        val item = parseSingleItemJson(response)
                        if (item != null) {
                            _uiState.update { it.copy(step = Act2Step.Reveal(item)) }
                        } else {
                            _uiState.update { it.copy(cameraPhase = CameraPhase.FAILED, cameraError = "Couldn't identify the item") }
                        }
                    },
                    onFailure = { e ->
                        Log.e("FirstMagic", "Vision analysis failed", e)
                        _uiState.update { it.copy(cameraPhase = CameraPhase.FAILED, cameraError = "AI couldn't read the photo") }
                    }
                )
            } catch (e: Exception) {
                cameraTimeoutJob?.cancel()
                Log.e("FirstMagic", "Camera processing error", e)
                _uiState.update { it.copy(cameraPhase = CameraPhase.FAILED, cameraError = "Something went wrong") }
            }
        }
    }

    fun onCameraFallbackToType() {
        _uiState.update { it.copy(
            step = Act2Step.TypeInput,
            chosenPath = Act2Path.TYPE,
            typedName = "",
            smartDefaults = null,
            cameraError = null
        )}
    }

    fun onCameraRetry() {
        // Go back to PathSelection so the camera launcher can fire again
        _uiState.update { it.copy(
            step = Act2Step.PathSelection,
            chosenPath = null,
            cameraError = null
        )}
    }

    // ─── Barcode Path ───────────────────────────────────────────────────

    fun onBarcodeDetected(barcode: String) {
        // Guard against duplicate fires
        if (_uiState.value.isLookingUpBarcode) return
        if (_uiState.value.lastScannedBarcode == barcode) return

        _uiState.update { it.copy(
            isLookingUpBarcode = true,
            barcodeNotFound = false,
            lastScannedBarcode = barcode
        )}

        viewModelScope.launch {
            try {
                val result = barcodeRepository.lookup(barcode)
                if (result.found && result.productName != null) {
                    // Look up SmartDefaults for category/location/shelfLife
                    val defaults = SmartDefaults.lookup(result.productName, regionCode)
                    val revealItem = RevealItem(
                        name = result.productName,
                        category = result.categories?.split(",")?.firstOrNull()?.trim()
                            ?: defaults?.category,
                        location = defaults?.location,
                        shelfLifeDays = defaults?.shelfLifeDays,
                        unit = defaults?.unit,
                        source = Act2Path.BARCODE
                    )
                    _uiState.update { it.copy(
                        isLookingUpBarcode = false,
                        step = Act2Step.Reveal(revealItem)
                    )}
                } else {
                    _uiState.update { it.copy(
                        isLookingUpBarcode = false,
                        barcodeNotFound = true
                    )}
                }
            } catch (e: Exception) {
                Log.e("FirstMagic", "Barcode lookup failed", e)
                _uiState.update { it.copy(
                    isLookingUpBarcode = false,
                    barcodeNotFound = true
                )}
            }
        }
    }

    fun onBarcodeFallbackToType() {
        _uiState.update { it.copy(
            step = Act2Step.TypeInput,
            chosenPath = Act2Path.TYPE,
            typedName = "",
            smartDefaults = null,
            barcodeNotFound = false,
            lastScannedBarcode = null
        )}
    }

    fun onBarcodeScanAgain() {
        _uiState.update { it.copy(
            barcodeNotFound = false,
            lastScannedBarcode = null
        )}
    }

    // ─── Back navigation ───────────────────────────────────────────────

    fun onBackFromSubStep(): Boolean {
        val step = _uiState.value.step
        return when (step) {
            is Act2Step.TypeInput, is Act2Step.MemorySelection,
            is Act2Step.BarcodeScanning -> {
                cameraTimeoutJob?.cancel()
                _uiState.update { it.copy(
                    step = Act2Step.PathSelection,
                    chosenPath = null,
                    typedName = "",
                    smartDefaults = null,
                    barcodeNotFound = false,
                    lastScannedBarcode = null
                )}
                true
            }
            is Act2Step.CameraProcessing -> {
                cameraTimeoutJob?.cancel()
                _uiState.update { it.copy(
                    step = Act2Step.PathSelection,
                    chosenPath = null,
                    cameraError = null
                )}
                true
            }
            // Block back during Reveal/Celebration/AddMore — forward only
            else -> false
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────

    private suspend fun resolveAndSaveItem(item: RevealItem) {
        val categoryId = item.category?.let { name ->
            categoryRepository.findCategoryByNameIgnoreCase(name)?.id
        }
        val locationId = item.location?.let { name ->
            storageLocationRepository.findByName(name)?.id
        }
        val unitId = item.unit?.let { name ->
            unitRepository.findByName(name)?.id
                ?: unitRepository.findByAbbreviation(name)?.id
        }
        val expiryDate = item.shelfLifeDays?.let { days ->
            LocalDate.now().plusDays(days.toLong())
        }

        val entity = ItemEntity(
            name = item.name,
            categoryId = categoryId,
            storageLocationId = locationId,
            unitId = unitId,
            expiryDate = expiryDate,
            quantity = 1.0,
            minQuantity = 1.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        itemRepository.insert(entity)
    }

    private fun completeAct2(onComplete: () -> Unit) {
        _uiState.update { it.copy(step = Act2Step.TransitionToDashboard) }
        viewModelScope.launch {
            settingsRepository.setBoolean(
                OnboardingViewModel.KEY_ONBOARDING_COMPLETED,
                true
            )
            onComplete()
        }
    }

    private fun parseSingleItemJson(response: String): RevealItem? {
        return try {
            // Extract JSON from response (may have surrounding text)
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd < 0) return null
            val jsonStr = response.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonStr)
            val name = json.optString("name", "").ifBlank { return null }
            RevealItem(
                name = name,
                category = json.optString("category", "").ifBlank { null },
                location = json.optString("location", "").ifBlank { null },
                shelfLifeDays = if (json.has("shelfLifeDays")) json.optInt("shelfLifeDays") else null,
                unit = json.optString("unit", "").ifBlank { null },
                source = Act2Path.CAMERA
            )
        } catch (e: Exception) {
            Log.e("FirstMagic", "Failed to parse AI response", e)
            null
        }
    }

    private fun compressAndEncode(bitmap: Bitmap, config: ImageConfig): String? {
        return try {
            var scaled = if (bitmap.width > config.maxDimension || bitmap.height > config.maxDimension) {
                val scale = config.maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else bitmap

            var quality = config.startQuality
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            var bytes = stream.toByteArray()

            while (bytes.size > config.maxBytes && quality > config.minQuality) {
                quality -= 10
                stream.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                bytes = stream.toByteArray()
            }

            if (bytes.size > config.maxBytes) {
                val oldScaled = scaled
                scaled = Bitmap.createScaledBitmap(
                    scaled,
                    (scaled.width * config.fallbackScale).toInt().coerceAtLeast(1),
                    (scaled.height * config.fallbackScale).toInt().coerceAtLeast(1),
                    true
                )
                if (oldScaled !== bitmap) oldScaled.recycle()
                stream.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                bytes = stream.toByteArray()
            }

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (scaled !== bitmap) scaled.recycle()
            base64
        } catch (e: Exception) {
            Log.e("FirstMagic", "Image compression failed", e)
            null
        }
    }
}
