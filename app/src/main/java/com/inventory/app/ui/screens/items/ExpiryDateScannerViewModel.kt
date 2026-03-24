package com.inventory.app.ui.screens.items

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.data.repository.GrokRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.inject.Inject

data class ExpiryDateScannerUiState(
    val isAiLoading: Boolean = false,
    val aiError: String? = null
)

@HiltViewModel
class ExpiryDateScannerViewModel @Inject constructor(
    private val grokRepository: GrokRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiryDateScannerUiState())
    val uiState: StateFlow<ExpiryDateScannerUiState> = _uiState

    /**
     * Send a cropped photo to AI vision for expiry date extraction.
     * Called when ML Kit fails on both live frames and the still capture.
     */
    fun extractExpiryDateFromImage(
        bitmap: Bitmap,
        onDateExtracted: (String) -> Unit
    ) {
        if (_uiState.value.isAiLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null) }

            try {
                val config = grokRepository.getImageConfig()
                val base64 = compressBitmapToBase64(bitmap, config.maxDimension, config.startQuality, config.maxBytes, config.minQuality, config.fallbackScale)

                val result = grokRepository.extractExpiryDateFromImage(base64)

                result.fold(
                    onSuccess = { dateStr ->
                        if (dateStr == null) {
                            _uiState.update { it.copy(isAiLoading = false, aiError = "No expiry date found in photo") }
                            return@fold
                        }

                        try {
                            val parsed = LocalDate.parse(dateStr)
                            val today = LocalDate.now()

                            if (parsed.isBefore(today)) {
                                _uiState.update { it.copy(
                                    isAiLoading = false,
                                    aiError = "Date $dateStr is in the past"
                                ) }
                            } else {
                                _uiState.update { it.copy(isAiLoading = false, aiError = null) }
                                onDateExtracted(dateStr)
                            }
                        } catch (e: Exception) {
                            _uiState.update { it.copy(isAiLoading = false, aiError = "Invalid date: $dateStr") }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isAiLoading = false, aiError = e.message ?: "AI scan failed") }
                    }
                )
            } catch (e: Exception) {
                Log.e("ExpiryDateScanner", "AI image extraction failed", e)
                _uiState.update { it.copy(isAiLoading = false, aiError = e.message ?: "AI scan failed") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(aiError = null) }
    }

    private fun compressBitmapToBase64(
        bitmap: Bitmap,
        maxDimension: Int,
        startQuality: Int,
        maxBytes: Int,
        minQuality: Int,
        fallbackScale: Float
    ): String {
        var bmp = bitmap
        // Scale down if needed
        val maxDim = maxOf(bmp.width, bmp.height)
        if (maxDim > maxDimension) {
            val scale = maxDimension.toFloat() / maxDim
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        }

        var quality = startQuality
        var bytes: ByteArray
        do {
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality >= minQuality)

        // If still too large, scale down further
        if (bytes.size > maxBytes) {
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * fallbackScale).toInt(), (bmp.height * fallbackScale).toInt(), true)
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, minQuality, stream)
            bytes = stream.toByteArray()
        }

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
