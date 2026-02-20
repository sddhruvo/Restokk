package com.inventory.app.util

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

    /** Short month+day for chart axes: "Feb 15" / "15 Feb" depending on locale */
    private val monthDayFormatter: DateTimeFormatter by lazy {
        // Use medium style but we want just month+day — derive from locale
        DateTimeFormatter.ofPattern(getMonthDayPattern())
    }

    fun formatDate(date: LocalDate): String = date.format(mediumDateFormatter)

    fun formatDateShort(date: LocalDate): String = date.format(shortDateFormatter)

    fun formatMonthDay(date: LocalDate): String = date.format(monthDayFormatter)

    /**
     * Returns a month-day pattern appropriate for the device locale.
     * US/Canada → "MMM d", most others → "d MMM"
     */
    private fun getMonthDayPattern(): String {
        val country = Locale.getDefault().country
        return if (country in setOf("US", "CA")) "MMM d" else "d MMM"
    }
}
