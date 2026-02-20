package com.inventory.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PurchaseDataPoint(
    val date: LocalDate,
    val quantity: Double
)

data class ConsumptionPrediction(
    val itemId: Long,
    val itemName: String,
    val daysRemaining: Int,
    val dailyRate: Double,
    val confidence: Float,
    val suggestedQuantity: Double,
    val unitId: Long?
)

object ConsumptionVelocityCalculator {

    private const val MIN_DATA_POINTS = 3

    fun calculatePredictions(
        purchasesByItem: Map<Long, List<PurchaseDataPoint>>,
        currentStock: Map<Long, Double>,
        itemNames: Map<Long, String>,
        itemUnits: Map<Long, Long?>,
        today: LocalDate = LocalDate.now()
    ): List<ConsumptionPrediction> {
        val predictions = mutableListOf<ConsumptionPrediction>()

        for ((itemId, purchases) in purchasesByItem) {
            if (purchases.size < MIN_DATA_POINTS) continue

            val sorted = purchases.sortedBy { it.date }
            val intervals = mutableListOf<Long>()
            val quantities = mutableListOf<Double>()

            for (i in 1 until sorted.size) {
                val days = ChronoUnit.DAYS.between(sorted[i - 1].date, sorted[i].date)
                if (days > 0) {
                    intervals.add(days)
                    quantities.add(sorted[i].quantity)
                }
            }

            if (intervals.size < 2) continue

            // Weighted moving average — linear weights (recent intervals weigh more)
            val avgInterval = weightedAverage(intervals.map { it.toDouble() })
            val avgQuantity = weightedAverage(quantities)

            if (avgInterval <= 0 || avgQuantity <= 0) continue

            // Staleness guard: skip if last purchase is older than 2× average interval
            val daysSinceLastPurchase = ChronoUnit.DAYS.between(sorted.lastOrNull()?.date ?: continue, today)
            if (daysSinceLastPurchase > avgInterval * 2) continue

            val dailyRate = avgQuantity / avgInterval
            val currentQty = currentStock[itemId] ?: 0.0
            val daysRemaining = if (dailyRate > 0) (currentQty / dailyRate).toInt() else Int.MAX_VALUE

            // Actionability filter: skip if daysRemaining > avgInterval (not urgent yet)
            if (daysRemaining > avgInterval.toInt()) continue

            // Confidence based on number of data points (more = higher, max 0.95)
            val confidence = (intervals.size.toFloat() / (intervals.size + 2)).coerceAtMost(0.95f)

            predictions.add(
                ConsumptionPrediction(
                    itemId = itemId,
                    itemName = itemNames[itemId] ?: "Unknown",
                    daysRemaining = daysRemaining.coerceAtLeast(0),
                    dailyRate = dailyRate,
                    confidence = confidence,
                    suggestedQuantity = avgQuantity,
                    unitId = itemUnits[itemId]
                )
            )
        }

        return predictions.sortedBy { it.daysRemaining }
    }

    private fun weightedAverage(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        var weightedSum = 0.0
        var weightTotal = 0.0
        for (i in values.indices) {
            val weight = (i + 1).toDouble() // linear: 1, 2, 3, ...
            weightedSum += values[i] * weight
            weightTotal += weight
        }
        return weightedSum / weightTotal
    }
}
