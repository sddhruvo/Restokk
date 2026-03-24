package com.inventory.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** Section headers — "Quick Actions", "Usage History", feature titles. */
val Typography.sectionHeader: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)

/** Form section labels — "Icon", "Color", "Ingredients", "Steps". */
val Typography.formSectionLabel: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)

/** Alert/modal titles, celebration text, empty state headlines. */
val Typography.alertTitle: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)

/** Large stat numbers in reports and dashboards. */
val Typography.statValue: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)

/** Emphasized body text — data values, highlighted info in lists. */
val Typography.emphasisBody: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
