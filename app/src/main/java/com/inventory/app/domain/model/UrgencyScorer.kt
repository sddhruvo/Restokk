package com.inventory.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class UrgencyLevel(val pulseMs: Int, val maxAlpha: Float) {
    URGENT(1000, 0.35f),
    ATTENTION(1500, 0.25f),
    INFO(2000, 0.15f),
    NONE(0, 0f)
}

enum class UrgencyTarget { EXPIRED, EXPIRING, LOW_STOCK, SHOPPING, TOTAL_ITEMS, NONE }

data class UrgencyResult(
    val target: UrgencyTarget,
    val score: Int,
    val level: UrgencyLevel
)

/**
 * Computes which dashboard stat card deserves the urgency highlight.
 * Max 1 card wins. Shared between Dashboard and SmartNotificationWorker.
 */
object UrgencyScorer {

    private fun scoreToLevel(score: Int): UrgencyLevel = when {
        score >= 70 -> UrgencyLevel.URGENT
        score >= 40 -> UrgencyLevel.ATTENTION
        score > 0 -> UrgencyLevel.INFO
        else -> UrgencyLevel.NONE
    }

    /**
     * Dashboard entry point — takes expiring item dates and computes urgency.
     * Null dates are filtered out.
     */
    fun compute(
        expiredCount: Int = 0,
        expiringDates: List<LocalDate?>,
        lowStockCount: Int,
        shoppingActiveCount: Int,
        totalItems: Int,
        today: LocalDate = LocalDate.now()
    ): UrgencyResult {
        var todayCount = 0
        var oneToTwoCount = 0
        var threeToFiveCount = 0
        var sixToSevenCount = 0

        for (date in expiringDates) {
            if (date == null) continue
            val days = ChronoUnit.DAYS.between(today, date)
            when {
                days <= 0 -> todayCount++
                days <= 2 -> oneToTwoCount++
                days <= 5 -> threeToFiveCount++
                days <= 7 -> sixToSevenCount++
            }
        }

        return computeFromCounts(
            expiredCount = expiredCount,
            expiringTodayCount = todayCount,
            expiring1to2DaysCount = oneToTwoCount,
            expiring3to5DaysCount = threeToFiveCount,
            expiring6to7DaysCount = sixToSevenCount,
            lowStockCount = lowStockCount,
            shoppingActiveCount = shoppingActiveCount,
            totalItems = totalItems
        )
    }

    /**
     * Shared core — takes pre-categorized counts.
     * Used by both [compute] and SmartNotificationWorker.
     * TOTAL_ITEMS only scores when totalItems in 1..19 (new user nudge).
     */
    fun computeFromCounts(
        expiredCount: Int = 0,
        expiringTodayCount: Int,
        expiring1to2DaysCount: Int,
        expiring3to5DaysCount: Int,
        expiring6to7DaysCount: Int,
        lowStockCount: Int,
        shoppingActiveCount: Int,
        totalItems: Int
    ): UrgencyResult {
        // Expired items always win — highest possible score
        val expiredScore = if (expiredCount > 0) 110 else 0

        // Compute each target's score
        val expiryScore = when {
            expiringTodayCount > 0 -> 100
            expiring1to2DaysCount > 0 -> 80
            expiring3to5DaysCount > 0 -> 60
            expiring6to7DaysCount > 0 -> 40
            else -> 0
        }

        val lowStockScore = when {
            lowStockCount >= 3 -> 55
            lowStockCount >= 1 -> 35
            else -> 0
        }

        val shoppingScore = if (shoppingActiveCount > 0) 20 else 0

        val totalItemsScore = if (totalItems in 1..19) 5 else 0

        // Pick highest score; tie-break order: EXPIRED > EXPIRING > LOW_STOCK > SHOPPING > TOTAL_ITEMS
        data class Candidate(val target: UrgencyTarget, val score: Int)
        val candidates = listOf(
            Candidate(UrgencyTarget.EXPIRED, expiredScore),
            Candidate(UrgencyTarget.EXPIRING, expiryScore),
            Candidate(UrgencyTarget.LOW_STOCK, lowStockScore),
            Candidate(UrgencyTarget.SHOPPING, shoppingScore),
            Candidate(UrgencyTarget.TOTAL_ITEMS, totalItemsScore)
        )

        val winner = candidates.maxByOrNull { it.score } ?: return UrgencyResult(UrgencyTarget.NONE, 0, UrgencyLevel.NONE)

        return if (winner.score > 0) {
            UrgencyResult(winner.target, winner.score, scoreToLevel(winner.score))
        } else {
            UrgencyResult(UrgencyTarget.NONE, 0, UrgencyLevel.NONE)
        }
    }
}
