package com.inventory.app.util

import com.inventory.app.domain.model.RegionRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * Locale-aware formatting utilities for currency, dates, and prices.
 * Used across the app to ensure consistent, region-appropriate display.
 */
object FormatUtils {

    /** Override for date format: "" = auto from region, "MONTH_FIRST", "DAY_FIRST" */
    var dateFormatOverride: String = ""

    // ── Currency ────────────────────────────────────────────────────────

    /**
     * Returns the currency symbol for the device's default locale.
     * Falls back to "$" if locale detection fails.
     */
    fun getDefaultCurrencySymbol(): String {
        return try {
            Currency.getInstance(Locale.getDefault()).symbol
        } catch (_: Exception) {
            "$"
        }
    }

    /**
     * Formats a price with the given currency symbol.
     * Example: formatPrice(1.45, "£") → "£1.45"
     */
    fun formatPrice(amount: Double, currencySymbol: String): String {
        return "$currencySymbol${String.format(Locale.US, "%.2f", amount)}"
    }

    // ── Dates ───────────────────────────────────────────────────────────

    /** Locale-aware medium date: UK → "15 Feb 2026", US → "Feb 15, 2026" */
    private val mediumDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    /** Locale-aware short date for compact UI (charts, axis labels) */
    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

    /** Short month+day for chart axes: "Feb 15" / "15 Feb" depending on locale/override */
    private val monthDayFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern(getMonthDayPattern())

    fun formatDate(date: LocalDate): String = date.format(mediumDateFormatter)

    fun formatDateShort(date: LocalDate): String = date.format(shortDateFormatter)

    fun formatMonthDay(date: LocalDate): String = date.format(monthDayFormatter)

    /**
     * Returns a month-day pattern appropriate for the user's preference or locale.
     * Respects [dateFormatOverride] if set, otherwise falls back to region-based detection.
     */
    private fun getMonthDayPattern(): String {
        return when (dateFormatOverride) {
            "MONTH_FIRST" -> "MMM d"
            "DAY_FIRST" -> "d MMM"
            else -> {
                val country = Locale.getDefault().country
                if (country in RegionRegistry.monthFirstCodes) "MMM d" else "d MMM"
            }
        }
    }

    // ── Shelf Life ────────────────────────────────────────────────────────

    fun formatShelfLife(days: Int): String = when {
        days >= 365 -> "~${days / 365} year${if (days >= 730) "s" else ""}"
        days >= 30 -> "~${days / 30} month${if (days >= 60) "s" else ""}"
        else -> "~$days day${if (days != 1) "s" else ""}"
    }

    fun formatRelativeTime(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
            days < 30 -> "$days day${if (days != 1L) "s" else ""} ago"
            else -> formatDate(java.time.Instant.ofEpochMilli(timestampMs)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
        }
    }
}
