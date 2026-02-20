package com.inventory.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt

enum class RhythmPhase { SILENT, LEARNING, CONFIDENT }

data class RhythmPrediction(
    val itemId: Long,
    val itemName: String,
    val predictedDate: LocalDate,
    val confidence: Float,
    val phase: RhythmPhase
)

object PurchaseRhythmCalculator {

    private const val MIN_PURCHASES = 3
    private const val MIN_HISTORY_DAYS = 7L
    private const val MIN_CONFIDENCE = 0.4f
    private const val EMA_ALPHA = 0.3

    fun calculatePredictions(
        purchasesByItem: Map<Long, List<PurchaseDataPoint>>,
        itemNames: Map<Long, String>,
        today: LocalDate = LocalDate.now()
    ): List<RhythmPrediction> {
        val predictions = mutableListOf<RhythmPrediction>()

        for ((itemId, purchases) in purchasesByItem) {
            val prediction = predictForItem(itemId, purchases, itemNames[itemId] ?: "Unknown", today)
            if (prediction != null) {
                predictions.add(prediction)
            }
        }

        return predictions.sortedByDescending { it.confidence }
    }

    private fun predictForItem(
        itemId: Long,
        purchases: List<PurchaseDataPoint>,
        itemName: String,
        today: LocalDate
    ): RhythmPrediction? {
        // Deduplicate same-day events (onboarding protection)
        val deduped = purchases
            .groupBy { it.date }
            .map { (date, group) ->
                PurchaseDataPoint(date, group.sumOf { it.quantity })
            }
            .sortedBy { it.date }

        // Require 3+ purchases on DIFFERENT days
        if (deduped.size < MIN_PURCHASES) return null

        // Require 7+ days of history
        val historySpan = ChronoUnit.DAYS.between(deduped.first().date, deduped.last().date)
        if (historySpan < MIN_HISTORY_DAYS) return null

        // Calculate intervals between consecutive purchases
        val intervals = mutableListOf<Double>()
        val quantities = mutableListOf<Double>()
        for (i in 1 until deduped.size) {
            val days = ChronoUnit.DAYS.between(deduped[i - 1].date, deduped[i].date).toDouble()
            if (days > 0) {
                intervals.add(days)
                quantities.add(deduped[i].quantity)
            }
        }

        if (intervals.size < 2) return null

        // EMA: recent intervals weighted more (alpha=0.3)
        val emaInterval = exponentialMovingAverage(intervals)
        if (emaInterval <= 0) return null

        // Adjust for quantity ratio: if they bought 2x normal, expect 2x interval
        val avgQuantity = quantities.average()
        val lastQuantity = quantities.last()
        val quantityRatio = if (avgQuantity > 0) lastQuantity / avgQuantity else 1.0
        val adjustedInterval = emaInterval * quantityRatio

        // Predict next purchase date from last purchase
        val lastPurchase = deduped.last().date
        val predictedDate = lastPurchase.plusDays(adjustedInterval.toLong().coerceAtLeast(1))

        // Staleness: skip if predicted date is > 2x interval in the past
        val daysPastDue = ChronoUnit.DAYS.between(predictedDate, today)
        if (daysPastDue > adjustedInterval * 2) return null

        // Confidence = dataScore * 0.4 + consistencyScore * 0.6
        val dataScore = (deduped.size.toFloat() / (deduped.size + 3)).coerceAtMost(0.9f)
        val consistencyScore = calculateConsistencyScore(intervals)
        val confidence = dataScore * 0.4f + consistencyScore * 0.6f

        if (confidence < MIN_CONFIDENCE) return null

        // Adaptive buffer: high confidence = no buffer
        val bufferDays = when {
            confidence > 0.7f -> 0L
            confidence > 0.5f -> 1L
            else -> 2L
        }
        val bufferedDate = predictedDate.minusDays(bufferDays)

        val phase = when {
            deduped.size >= 5 -> RhythmPhase.CONFIDENT
            deduped.size >= 3 -> RhythmPhase.LEARNING
            else -> RhythmPhase.SILENT
        }

        return RhythmPrediction(
            itemId = itemId,
            itemName = itemName,
            predictedDate = bufferedDate,
            confidence = confidence,
            phase = phase
        )
    }

    private fun exponentialMovingAverage(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        var ema = values.first()
        for (i in 1 until values.size) {
            ema = EMA_ALPHA * values[i] + (1 - EMA_ALPHA) * ema
        }
        return ema
    }

    private fun calculateConsistencyScore(intervals: List<Double>): Float {
        if (intervals.size < 2) return 0f
        val mean = intervals.average()
        if (mean <= 0) return 0f
        val variance = intervals.sumOf { (it - mean) * (it - mean) } / intervals.size
        val stdDev = sqrt(variance)
        val cv = stdDev / mean // coefficient of variation
        return (1.0f - cv.toFloat()).coerceIn(0f, 1f)
    }
}
