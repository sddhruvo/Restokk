package com.inventory.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material3 shape scale used in `MaterialTheme(shapes = AppShapes)`. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** Micro-sizes that don't fit the Material3 Shapes scale. */
object AppShapeTokens {
    val CornerXs = 2.dp
    val CornerSm = 3.dp
    val CornerPill = 24.dp
}
