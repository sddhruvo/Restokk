package com.inventory.app.ui.screens.cook

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.inventory.app.ui.components.ThemedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.domain.model.RecipeIngredient
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkBorderCard
import com.inventory.app.ui.components.PageHeader
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.ThemedCircularProgress
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.rememberAiSignInGate
import com.inventory.app.ui.theme.visuals

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiRecipeDescriptionScreen(
    navController: NavController,
    viewModel: AiRecipeDescriptionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val aiGate = rememberAiSignInGate()

    // Back press in preview phase returns to input phase
    BackHandler(enabled = state.generatedRecipe != null) {
        viewModel.clearResult()
    }

    PageScaffold(onBack = { navController.popBackStack() }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = state.generatedRecipe,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "recipe_phase",
                modifier = Modifier.weight(1f)
            ) { recipe ->
                if (recipe == null) {
                    InputPhase(state = state, viewModel = viewModel)
                } else {
                    PreviewPhase(recipe = recipe)
                }
            }

            // Sticky bottom area
            StickyBottom(
                state = state,
                onGenerate = {
                    aiGate.requireSignIn("generate a full recipe with ingredients and steps from your description") {
                        viewModel.generateRecipe()
                    }
                },
                onRegenerate = viewModel::clearResult,
                onOpenBuilder = { viewModel.openInBuilder(navController) }
            )
        }
    }
}

// ── Input phase ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputPhase(
    state: AiRecipeDescriptionUiState,
    viewModel: AiRecipeDescriptionViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { PageHeader("Describe a Recipe") }

        // Description field
        item {
            ThemedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                placeholder = { Text("e.g. creamy pasta carbonara with pancetta…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }

        // Servings stepper
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Servings",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { viewModel.setServings(state.servings - 1) },
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("−", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    "${state.servings}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OutlinedButton(
                    onClick = { viewModel.setServings(state.servings + 1) },
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Style chips
        item {
            Column {
                Text(
                    "Style (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleTag.values().forEach { tag ->
                        FilterChip(
                            selected = tag in state.selectedTags,
                            onClick = { viewModel.toggleTag(tag) },
                            label = { Text(tag.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        }

        // Capability showcase
        item { CapabilityShowcase() }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Preview phase ──────────────────────────────────────────────────────────

@Composable
private fun PreviewPhase(recipe: GeneratedRecipePreview) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recipe.timeMinutes > 0) {
                    Text("${recipe.timeMinutes} min", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    recipe.difficulty.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("·", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Serves ${recipe.servings}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (recipe.description.isNotBlank()) {
            item {
                Text(
                    recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { IngredientMatchCard(recipe.ingredients) }

        item {
            Text(
                "${recipe.steps.size} steps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Ingredient match card ──────────────────────────────────────────────────

@Composable
private fun IngredientMatchCard(ingredients: List<RecipeIngredient>) {
    if (ingredients.isEmpty()) return

    val total = ingredients.size
    val haveCount = ingredients.count { it.have_it }
    val ratio = haveCount.toFloat() / total
    val summaryColor = when {
        ratio >= 0.5f -> Color(0xFF4CAF50)
        ratio >= 0.25f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val displayIngredients = ingredients.take(5)
    val remaining = ingredients.size - displayIngredients.size
    val isPaperInk = !MaterialTheme.visuals.useElevation

    val cardContent: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "$haveCount of $total in your kitchen",
                color = summaryColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            displayIngredients.forEach { ing ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (ing.have_it) "✓" else "✗",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ing.have_it) summaryColor else MaterialTheme.colorScheme.error,
                        modifier = Modifier.width(16.dp)
                    )
                    Text(
                        buildString {
                            append(ing.name)
                            if (ing.amount.isNotBlank()) append(" (${ing.amount}${if (ing.unit.isNotBlank()) " ${ing.unit}" else ""})")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (remaining > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "...and $remaining more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (isPaperInk) {
        InkBorderCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    } else {
        AppCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    }
}

// ── Capability showcase card ───────────────────────────────────────────────

@Composable
private fun CapabilityShowcase() {
    val capabilities: List<Pair<ImageVector, String>> = listOf(
        Icons.Filled.List to "Writes full ingredient list with amounts",
        Icons.Filled.Kitchen to "Checks what you already have vs. need to buy",
        Icons.Filled.FormatListNumbered to "Creates clear step-by-step instructions",
        Icons.Filled.Timer to "Detects cook timers automatically"
    )
    val isPaperInk = !MaterialTheme.visuals.useElevation

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "What AI will generate",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            capabilities.forEach { (icon, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (isPaperInk) {
        InkBorderCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    } else {
        AppCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    }
}

// ── Sticky bottom ──────────────────────────────────────────────────────────

@Composable
private fun StickyBottom(
    state: AiRecipeDescriptionUiState,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    onOpenBuilder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.generatedRecipe == null) {
            // Input phase bottom
            state.error?.let { err ->
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.isGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemedCircularProgress(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("AI is writing your recipe…", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                ThemedButton(
                    onClick = onGenerate,
                    enabled = state.description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.AutoAwesome,
                        inkIconRes = R.drawable.ic_ink_sparkle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Generate Recipe")
                }
            }
        } else {
            // Preview phase bottom — two side-by-side buttons
            state.error?.let { err ->
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRegenerate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("↩ Regenerate")
                }
                ThemedButton(
                    onClick = onOpenBuilder,
                    enabled = !state.isSavingToBuilder,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSavingToBuilder) {
                        ThemedCircularProgress(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Edit in Builder →")
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
