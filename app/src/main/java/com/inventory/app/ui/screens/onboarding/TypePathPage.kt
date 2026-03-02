package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inventory.app.domain.model.ItemDefaults
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.RuledLinesBackground
import com.inventory.app.ui.components.ThemedTextField
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay

private val BouncySpring = PaperInkMotion.BouncySpring
private val GentleSpring = PaperInkMotion.GentleSpring

/**
 * Type Path — user types an item name and sees SmartDefaults appear as animated chips.
 */
@Composable
internal fun TypePathPage(
    typedName: String,
    smartDefaults: ItemDefaults?,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    // ── Entrance animations ──
    var promptReady by remember { mutableStateOf(false) }
    var inputReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        promptReady = true
        delay(300)
        inputReady = true
    }

    val promptX by animateFloatAsState(
        targetValue = if (promptReady) 0f else -20f,
        animationSpec = BouncySpring, label = "typePromptX"
    )
    val promptAlpha by animateFloatAsState(
        targetValue = if (promptReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "typePromptAlpha"
    )

    val inputY by animateFloatAsState(
        targetValue = if (inputReady) 0f else 20f,
        animationSpec = GentleSpring, label = "inputY"
    )
    val inputAlpha by animateFloatAsState(
        targetValue = if (inputReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "inputAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RuledLinesBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Prompt
            Text(
                text = "What's one thing in your\nkitchen right now?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = promptX; alpha = promptAlpha
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Input field
            ThemedTextField(
                value = typedName,
                onValueChange = onNameChange,
                label = { Text("Item name") },
                placeholder = { Text("e.g. milk, rice, eggs...") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = inputY; alpha = inputAlpha
                    },
                textStyle = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated SmartDefaults chips
            SmartDefaultsChips(defaults = smartDefaults)

            Spacer(modifier = Modifier.weight(1f))

            // Flavor text
            if (smartDefaults != null) {
                Text(
                    text = "We know a thing or two about kitchens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Submit button
            ThemedButton(
                onClick = onSubmit,
                enabled = typedName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Add to my kitchen", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Animated SmartDefaults chips
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SmartDefaultsChips(defaults: ItemDefaults?) {
    // Track which fields should be visible (stagger 150ms apart)
    var categoryVisible by remember { mutableStateOf(false) }
    var locationVisible by remember { mutableStateOf(false) }
    var shelfLifeVisible by remember { mutableStateOf(false) }

    // Reset and re-animate when defaults change
    LaunchedEffect(defaults) {
        categoryVisible = false
        locationVisible = false
        shelfLifeVisible = false
        if (defaults != null) {
            delay(150)
            if (defaults.category != null) categoryVisible = true
            delay(150)
            if (defaults.location != null) locationVisible = true
            delay(150)
            if (defaults.shelfLifeDays != null) shelfLifeVisible = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (defaults != null) {
            DefaultChip(
                icon = Icons.Outlined.Category,
                label = "Category",
                value = defaults.category,
                visible = categoryVisible,
                index = 0
            )
        }
        defaults?.location?.let { loc ->
            DefaultChip(
                icon = Icons.Outlined.Kitchen,
                label = "Location",
                value = loc,
                visible = locationVisible,
                index = 1
            )
        }
        defaults?.shelfLifeDays?.let { days ->
            val displayText = when {
                days >= 365 -> "~${days / 365} year${if (days >= 730) "s" else ""}"
                days >= 30 -> "~${days / 30} month${if (days >= 60) "s" else ""}"
                else -> "~$days day${if (days != 1) "s" else ""}"
            }
            DefaultChip(
                icon = Icons.Outlined.Schedule,
                label = "Shelf Life",
                value = displayText,
                visible = shelfLifeVisible,
                index = 2
            )
        }
    }
}

@Composable
private fun DefaultChip(
    icon: ImageVector,
    label: String,
    value: String,
    visible: Boolean,
    index: Int
) {
    // Write-In from left (BouncySpring)
    val chipX by animateFloatAsState(
        targetValue = if (visible) 0f else -40f,
        animationSpec = BouncySpring, label = "chip${index}X"
    )
    val chipAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250), label = "chip${index}Alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = chipX; alpha = chipAlpha
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
