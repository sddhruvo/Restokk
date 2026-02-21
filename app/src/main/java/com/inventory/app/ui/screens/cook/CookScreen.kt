package com.inventory.app.ui.screens.cook

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.domain.model.CuisineData
import com.inventory.app.domain.model.RegionalCuisine
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.StaggeredAnimatedItem
import com.inventory.app.ui.navigation.Screen

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(20.dp)

// ── Warm accent color for Cook feature ──────────────────────────────────
private val CookAccent = Color(0xFFE85D3A) // Warm orange-red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookScreen(
    navController: NavController,
    viewModel: CookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // System back key: return to configurator when showing results
    BackHandler(enabled = uiState.showResults) {
        viewModel.backToConfigurator()
    }

    // Show snackbar when recipe saved (keyed on token so same recipe can re-trigger)
    LaunchedEffect(uiState.lastSavedRecipeToken) {
        if (uiState.lastSavedRecipeToken == 0) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Recipe saved!",
            actionLabel = "View",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            navController.navigate(Screen.SavedRecipes.route)
        }
        viewModel.clearLastSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What Can I Cook?") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.showResults) viewModel.backToConfigurator()
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // My Recipes button with count badge
                    if (uiState.savedRecipeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text("${uiState.savedRecipeCount}") }
                            }
                        ) {
                            IconButton(onClick = { navController.navigate(Screen.SavedRecipes.route) }) {
                                Icon(Icons.Filled.MenuBook, contentDescription = "My Recipes")
                            }
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(Screen.SavedRecipes.route) }) {
                            Icon(Icons.Filled.MenuBook, contentDescription = "My Recipes")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = uiState.showResults,
                transitionSpec = {
                    slideInHorizontally { it / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { -it / 3 } + fadeOut()
                },
                label = "cook_content"
            ) { showResults ->
                if (showResults) {
                    ResultsScreen(uiState, viewModel, navController)
                } else {
                    ConfiguratorScreen(uiState, viewModel, navController)
                }
            }
        }
    }

    // Cuisine Passport bottom sheet
    if (uiState.showCuisinePassport) {
        CuisinePassportSheet(uiState, viewModel)
    }

    // Hero Ingredient picker bottom sheet
    if (uiState.showHeroPicker) {
        HeroPickerSheet(uiState, viewModel)
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CONFIGURATOR (Recipe Order Card)
// ═════════════════════════════════════════════════════════════════════════

@Composable
private fun ConfiguratorScreen(uiState: CookUiState, viewModel: CookViewModel, navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Smart tip
        if (uiState.currentTip != null && !uiState.showResults) {
            item(key = "tip_card") {
                CookTipCard(
                    tip = uiState.currentTip,
                    onDismiss = { viewModel.dismissTip() },
                    onCtaClick = uiState.currentTip.ctaRoute?.let { route ->
                        { navController.navigate(route) }
                    }
                )
            }
        }

        // Section 1: Mood Cards
        item(key = "mood_header") {
            StaggeredAnimatedItem(index = 0) {
                SectionLabel("What's the vibe?")
            }
        }
        item(key = "mood_cards") {
            StaggeredAnimatedItem(index = 1) {
                MoodCardsRow(uiState.selectedMood) { viewModel.selectMood(it) }
            }
        }

        // Section 2: Hero Ingredient
        item(key = "hero_header") {
            StaggeredAnimatedItem(index = 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Build around...")
                    TextButton(onClick = { viewModel.showHeroPicker() }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick items")
                    }
                }
            }
        }
        if (uiState.heroIngredients.isNotEmpty()) {
            item(key = "hero_chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.heroIngredients, key = { it.id }) { item ->
                        val isExpiring = (item.daysUntilExpiry ?: Long.MAX_VALUE) <= 3
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleHeroIngredient(item) },
                            label = { Text(item.name) },
                            trailingIcon = { Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(16.dp)) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = if (isExpiring)
                                    MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        } else {
            item(key = "hero_hint") {
                Text(
                    "Optional — select up to 3 items from your inventory to feature",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 3: Cuisine
        item(key = "cuisine_header") {
            StaggeredAnimatedItem(index = 3) {
                SectionLabel("Cuisine")
            }
        }
        item(key = "cuisine_chips") {
            StaggeredAnimatedItem(index = 4) {
                CuisineSelector(uiState.selectedCuisine, viewModel)
            }
        }

        // Section 4: Taste & Style
        item(key = "taste_header") {
            StaggeredAnimatedItem(index = 5) {
                SectionLabel("Taste & Style")
            }
        }
        item(key = "taste_controls") {
            StaggeredAnimatedItem(index = 6) {
                TasteControls(uiState, viewModel)
            }
        }

        // Section 5: Meal Type & Servings
        item(key = "meal_row") {
            StaggeredAnimatedItem(index = 7) {
                MealAndServingsRow(uiState, viewModel)
            }
        }

        // Section 6: Dietary
        item(key = "dietary_header") {
            StaggeredAnimatedItem(index = 8) {
                SectionLabel("Dietary")
            }
        }
        item(key = "dietary_chips") {
            StaggeredAnimatedItem(index = 9) {
                DietaryChips(uiState.dietaryFilters) { viewModel.toggleDietary(it) }
            }
        }

        // Section 7: More Options (collapsible)
        item(key = "more_options") {
            Column {
                TextButton(
                    onClick = { viewModel.toggleMoreOptions() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.showMoreOptions) "Less options" else "More options")
                    Icon(
                        if (uiState.showMoreOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(visible = uiState.showMoreOptions) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Equipment")
                        EquipmentChips(uiState.equipment) { viewModel.toggleEquipment(it) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = uiState.flexibleIngredients,
                                    onValueChange = { viewModel.setFlexibleIngredients(it) },
                                    role = Role.Switch
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Suggest extra ingredients?", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Allow recipes needing 1-2 items to buy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.flexibleIngredients,
                                onCheckedChange = null
                            )
                        }
                    }
                }
            }
        }

        // Cook button
        item(key = "cook_button") {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.cook() },
                enabled = uiState.canCook,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CookAccent)
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cook!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }

        // Inventory summary
        item(key = "inventory_summary") {
            Text(
                "${uiState.inventoryItems.size} items in your inventory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Mood Cards Row ─────────────────────────────────────────────────────

@Composable
private fun MoodCardsRow(selected: MealMood?, onSelect: (MealMood) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(MealMood.entries.toList(), key = { it.name }) { mood ->
            val isSelected = mood == selected
            AppCard(
                onClick = { onSelect(mood) },
                modifier = Modifier.width(100.dp).height(84.dp),
                shape = CardShape,
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else null
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(mood.emoji, fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        mood.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Cuisine Selector ───────────────────────────────────────────────────

@Composable
private fun CuisineSelector(selected: RegionalCuisine?, viewModel: CookViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick chips: popular + Any
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // "Any" chip
            item(key = "any") {
                FilterChip(
                    selected = selected == null,
                    onClick = { viewModel.selectCuisine(null) },
                    label = { Text("Any") }
                )
            }
            items(CuisineData.popular, key = { "pop_${it.name}" }) { cuisine ->
                FilterChip(
                    selected = selected?.name == cuisine.name,
                    onClick = { viewModel.selectCuisine(cuisine) },
                    label = { Text(cuisine.name) }
                )
            }
        }

        // Selected cuisine badge + Explore button
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null && CuisineData.popular.none { it.name == selected.name }) {
                InputChip(
                    selected = true,
                    onClick = { viewModel.selectCuisine(null) },
                    label = { Text("${selected.name} (${selected.country})") },
                    trailingIcon = { Icon(Icons.Filled.Close, "Clear", modifier = Modifier.size(16.dp)) }
                )
            }
            OutlinedButton(
                onClick = { viewModel.showCuisinePassport() },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Explore Cuisines", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Taste Controls ─────────────────────────────────────────────────────

@Composable
private fun TasteControls(uiState: CookUiState, viewModel: CookViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Spice Level
        ToggleRow("Spice", SpiceLevel.entries.toList(), uiState.spiceLevel, { it.label }) {
            viewModel.setSpiceLevel(it)
        }
        // Effort Level
        ToggleRow("Effort", EffortLevel.entries.toList(), uiState.effortLevel, { "${it.label} (${it.minutes})" }) {
            viewModel.setEffortLevel(it)
        }
        // Style Level
        ToggleRow("Style", StyleLevel.entries.toList(), uiState.styleLevel, { it.label }) {
            viewModel.setStyleLevel(it)
        }
    }
}

@Composable
private fun <T> ToggleRow(
    label: String,
    options: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(52.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = { Text(labelProvider(option), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

// ── Meal Type & Servings ───────────────────────────────────────────────

@Composable
private fun MealAndServingsRow(uiState: CookUiState, viewModel: CookViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Meal type chips
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel("Meal")
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(MealType.entries.toList(), key = { it.name }) { type ->
                    FilterChip(
                        selected = type == uiState.mealType,
                        onClick = { viewModel.setMealType(type) },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        // Servings stepper
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel("Serves")
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setServings(uiState.servings - 1) },
                    enabled = uiState.servings > 1,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Remove, "Less", modifier = Modifier.size(20.dp))
                }
                Text(
                    "${uiState.servings}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { viewModel.setServings(uiState.servings + 1) },
                    enabled = uiState.servings < 8,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Add, "More", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Dietary & Equipment Chips ──────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietaryChips(selected: Set<DietaryFilter>, onToggle: (DietaryFilter) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DietaryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter in selected,
                onClick = { onToggle(filter) },
                label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentChips(selected: Set<Equipment>, onToggle: (Equipment) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Equipment.entries.forEach { equip ->
            FilterChip(
                selected = equip in selected,
                onClick = { onToggle(equip) },
                label = { Text(equip.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// RESULTS SCREEN (Recipe Cards)
// ═════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultsScreen(uiState: CookUiState, viewModel: CookViewModel, navController: NavController) {
    if (uiState.isLoading) {
        LoadingAnimation()
        return
    }

    if (uiState.error != null) {
        ErrorState(uiState.error, onRetry = { viewModel.cook() }, onBack = { viewModel.backToConfigurator() })
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "results_header") {
            Text(
                "Here's what you can make",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Smart tip (results context)
        if (uiState.currentTip != null && uiState.showResults) {
            item(key = "results_tip") {
                CookTipCard(
                    tip = uiState.currentTip,
                    onDismiss = { viewModel.dismissTip() },
                    onCtaClick = uiState.currentTip.ctaRoute?.let { route ->
                        { navController.navigate(route) }
                    }
                )
            }
        }

        itemsIndexed(uiState.recipes, key = { index, r -> "${index}_${r.name}" }) { index, recipe ->
            WriteInAnimatedItem(index = index) {
                RecipeCard(recipe, uiState.addedToShoppingList, uiState.savedRecipeNames, viewModel)
            }
        }

        // Refinement section
        item(key = "refinement") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Not quite right?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val refinements = listOf(
                        "Different protein" to "Use a different main protein",
                        "Simpler" to "Suggest simpler, fewer-step recipes",
                        "More traditional" to "More authentic, traditional preparation",
                        "More fusion" to "Creative fusion or modern interpretation",
                        "Completely different" to "Suggest completely different recipes"
                    )
                    items(refinements, key = { it.first }) { (label, hint) ->
                        OutlinedButton(
                            onClick = { viewModel.tryAgain(hint) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                TextButton(onClick = { viewModel.backToConfigurator() }) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Change settings")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Recipe Card ────────────────────────────────────────────────────────

@Composable
private fun RecipeCard(
    recipe: SuggestedRecipe,
    addedToShoppingList: Set<String>,
    savedRecipeNames: Set<String>,
    viewModel: CookViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    AppCard(
        onClick = { expanded = !expanded },
        shape = CardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        recipe.cuisine_origin,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Time + difficulty + bookmark
                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = "Time",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "${recipe.time_minutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            recipe.difficulty.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (recipe.difficulty) {
                                "easy" -> Color(0xFF34C759)
                                "hard" -> Color(0xFFFF3B30)
                                else -> Color(0xFFFF9500)
                            }
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    val isSaved = recipe.name in savedRecipeNames
                    AnimatedBookmarkIcon(
                        isSaved = isSaved,
                        onClick = {
                            if (isSaved) viewModel.unsaveRecipe(recipe.name)
                            else viewModel.saveRecipe(recipe)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Ingredient match bar (animated)
            val matchColor = when {
                recipe.matchPercentage >= 90 -> Color(0xFF34C759)
                recipe.matchPercentage >= 70 -> Color(0xFFFF9500)
                else -> Color(0xFFFF3B30)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ingredients: ${recipe.matchedCount}/${recipe.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${recipe.matchPercentage}% match",
                    style = MaterialTheme.typography.labelSmall,
                    color = matchColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            AnimatedMatchBar(matchPercentage = recipe.matchPercentage)

            // Missing ingredients quick view
            if (recipe.missingIngredients.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Need: ${recipe.missingIngredients.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val allAdded = recipe.missingIngredients.all { it.name in addedToShoppingList }
                    if (!allAdded) {
                        TextButton(
                            onClick = { viewModel.addMissingToShoppingList(recipe) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Add all", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text(
                            "Added!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34C759),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expanded details
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Full ingredient list
                    Text("Ingredients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    recipe.ingredients.forEach { ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (ing.have_it) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = if (ing.have_it) "In stock" else "Missing",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (ing.have_it) Color(0xFF34C759) else MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    ing.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (ing.have_it) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                "${ing.amount} ${ing.unit}".trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Individual add to shopping list
                            if (!ing.have_it && ing.name !in addedToShoppingList) {
                                IconButton(
                                    onClick = { viewModel.addSingleToShoppingList(ing.name, recipe.name) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.AddShoppingCart, "Add to list", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Steps
                    Spacer(Modifier.height(12.dp))
                    Text("Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    recipe.steps.forEachIndexed { idx, step ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                "${idx + 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CookAccent,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(step, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Tips
                    if (!recipe.tips.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        AppCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                            Row(modifier = Modifier.padding(10.dp)) {
                                Icon(
                                    Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    recipe.tips,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Expand hint
            Spacer(Modifier.height(4.dp))
            Text(
                if (expanded) "Tap to collapse" else "Tap for full recipe",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CUISINE PASSPORT (Bottom Sheet)
// ═════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuisinePassportSheet(uiState: CookUiState, viewModel: CookViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissCuisinePassport() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Title
            Text(
                "Explore Cuisines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = uiState.cuisineSearchQuery,
                onValueChange = { viewModel.setCuisineSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search cuisines...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (uiState.cuisineSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setCuisineSearch("") }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            if (uiState.cuisineSearchQuery.isNotBlank()) {
                // Search results
                val results = CuisineData.search(uiState.cuisineSearchQuery)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (results.isEmpty()) {
                        item {
                            Text(
                                "No cuisines found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(results, key = { "${it.name}_${it.country}" }) { cuisine ->
                        CuisineListItem(cuisine) { viewModel.selectCuisine(cuisine) }
                    }
                }
            } else {
                // Continent tabs
                ScrollableTabRow(
                    selectedTabIndex = CuisineData.continents.indexOf(uiState.selectedContinent).coerceAtLeast(0),
                    edgePadding = 0.dp
                ) {
                    CuisineData.continents.forEach { continent ->
                        Tab(
                            selected = continent == uiState.selectedContinent,
                            onClick = { viewModel.selectContinent(continent) },
                            text = { Text(continent, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Countries + regional cuisines
                val continent = uiState.selectedContinent ?: CuisineData.continents.first()
                val countries = CuisineData.countriesForContinent(continent)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    countries.forEach { country ->
                        val cuisines = CuisineData.cuisinesForCountry(country)
                        val isExpanded = uiState.expandedCountry == country

                        item(key = "country_$country") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleCountry(country) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    country,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${cuisines.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isExpanded) {
                            items(cuisines, key = { "cuisine_${it.name}_${it.country}" }) { cuisine ->
                                CuisineListItem(cuisine) { viewModel.selectCuisine(cuisine) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CuisineListItem(cuisine: RegionalCuisine, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(cuisine.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                cuisine.country,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
// HERO INGREDIENT PICKER (Bottom Sheet)
// ═════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroPickerSheet(uiState: CookUiState, viewModel: CookViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    // Precompute hero ingredient IDs for O(1) lookup
    val heroIds = remember(uiState.heroIngredients) { uiState.heroIngredients.map { it.id }.toSet() }

    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissHeroPicker() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Pick Hero Ingredients",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Select up to 3 items to build recipes around (${uiState.heroIngredients.size}/3 selected)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your inventory...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Expiring items section
            val expiringItems = uiState.inventoryItems.filter { (it.daysUntilExpiry ?: Long.MAX_VALUE) <= 5 }
            val filteredItems = if (searchQuery.isBlank()) uiState.inventoryItems
            else uiState.inventoryItems.filter { it.name.lowercase().contains(searchQuery.lowercase()) }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Expiring soon section
                if (searchQuery.isBlank() && expiringItems.isNotEmpty()) {
                    item(key = "expiring_header") {
                        Text(
                            "Expiring Soon",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(expiringItems, key = { "exp_${it.id}" }) { item ->
                        InventoryPickItem(item, item.id in heroIds) {
                            viewModel.toggleHeroIngredient(item)
                        }
                    }
                    item(key = "all_header") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "All Items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                items(filteredItems, key = { "inv_${it.id}" }) { item ->
                    InventoryPickItem(item, item.id in heroIds) {
                        viewModel.toggleHeroIngredient(item)
                    }
                }
            }

            // Done button
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.dismissHeroPicker() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InventoryPickItem(item: InventoryItemSummary, isSelected: Boolean, onToggle: () -> Unit) {
    val isExpiring = (item.daysUntilExpiry ?: Long.MAX_VALUE) <= 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                Row {
                    Text(
                        "${item.quantity} ${item.unit ?: ""}".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isExpiring && item.daysUntilExpiry != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (item.daysUntilExpiry <= 0) "Expired!"
                            else "${item.daysUntilExpiry}d left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// LOADING & ERROR STATES
// ═════════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingAnimation() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated cooking icon
            val infiniteTransition = rememberInfiniteTransition(label = "cook_loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "stir"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathe"
            )
            Icon(
                Icons.Filled.Restaurant,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                    },
                tint = CookAccent
            )

            Spacer(Modifier.height(24.dp))

            // Animated messages
            val messages = listOf(
                "Checking your pantry...",
                "Finding the perfect recipes...",
                "Matching ingredients...",
                "Almost ready to serve!"
            )
            var messageIndex by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    messageIndex = (messageIndex + 1) % messages.size
                }
            }
            AnimatedContent(
                targetState = messages[messageIndex],
                transitionSpec = { fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it } },
                label = "loading_text"
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text("Go back")
                }
                Button(onClick = onRetry) {
                    Text("Try again")
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// SHARED HELPERS
// ═════════════════════════════════════════════════════════════════════════

// ── Paper & Ink Animations ────────────────────────────────────────────

// Write-In entrance: slide from left (-20dp) + BouncySpring, 70ms stagger
@Composable
private fun WriteInAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        visible = true
    }

    val offsetX by animateFloatAsState(
        targetValue = if (visible) 0f else -20f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "writeIn_x"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 1.0f, stiffness = 200f),
        label = "writeIn_alpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationX = offsetX * density
            this.alpha = alpha
        }
    ) {
        content()
    }
}

// Animated match bar: fills from 0% to actual%
@Composable
private fun AnimatedMatchBar(
    matchPercentage: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = matchPercentage / 100f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "matchBar"
    )
    val matchColor = when {
        matchPercentage >= 90 -> Color(0xFF34C759)
        matchPercentage >= 70 -> Color(0xFFFF9500)
        else -> Color(0xFFFF3B30)
    }
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = matchColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

// Bookmark bounce animation
@Composable
private fun AnimatedBookmarkIcon(
    isSaved: Boolean,
    onClick: () -> Unit
) {
    var wasJustSaved by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (wasJustSaved) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 200f),
        finishedListener = { wasJustSaved = false },
        label = "bookmark_scale"
    )

    IconButton(
        onClick = {
            if (!isSaved) wasJustSaved = true
            onClick()
        },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = if (isSaved) "Unsave recipe" else "Save recipe",
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            tint = if (isSaved) CookAccent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Smart Tip Card ────────────────────────────────────────────────────

@Composable
private fun CookTipCard(
    tip: CookTip,
    onDismiss: () -> Unit,
    onCtaClick: (() -> Unit)? = null
) {
    // Auto-dismiss after 6 seconds
    LaunchedEffect(tip.id) {
        kotlinx.coroutines.delay(6000)
        onDismiss()
    }

    // Slide in from top with SettleSpring
    var tipVisible by remember { mutableStateOf(false) }
    LaunchedEffect(tip.id) { tipVisible = true }
    val tipOffsetY by animateFloatAsState(
        targetValue = if (tipVisible) 0f else -30f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f),
        label = "tip_slide"
    )
    val tipAlpha by animateFloatAsState(
        targetValue = if (tipVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "tip_alpha"
    )

    AppCard(
        modifier = Modifier.graphicsLayer {
            translationY = tipOffsetY * density
            alpha = tipAlpha
        },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left accent bar (pen-stroke style)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(CookAccent, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tip.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (tip.ctaLabel != null && onCtaClick != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onCtaClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(tip.ctaLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Close, "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}
