package com.inventory.app.ui.screens.cook

import android.content.Intent
import com.inventory.app.ui.components.ThemedSnackbarHost
import com.inventory.app.ui.components.ThemedTextField
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.PageHeader
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkWashSwipeBackground
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.ThemedDivider
import com.inventory.app.ui.components.ThemedProgressBar
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.Dimens
import com.inventory.app.ui.theme.formSectionLabel
import com.inventory.app.ui.theme.sectionHeader
import com.inventory.app.ui.theme.appColors
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecipesScreen(
    navController: NavController,
    viewModel: SavedRecipesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var hasShownPeekHint by rememberSaveable { mutableStateOf(false) }

    // Handle one-shot events (shopping added)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SavedRecipesEvent.ShoppingAdded -> {
                    snackbarHostState.showSnackbar(
                        message = "Added ${event.count} item${if (event.count == 1) "" else "s"} to shopping list",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Undo snackbar when recipe deleted
    LaunchedEffect(uiState.lastDeletedRecipeId) {
        val deletedId = uiState.lastDeletedRecipeId ?: return@LaunchedEffect
        val name = uiState.lastDeletedRecipeName ?: "Recipe"
        val result = snackbarHostState.showSnackbar(
            message = "$name deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.restoreRecipe(deletedId)
        } else {
            viewModel.clearDeletedState()
        }
    }

    PageScaffold(
        onBack = { navController.popBackStack() },
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = 80.dp)) {
                ThemedSnackbarHost(snackbarHostState)
            }
        }
    ) { contentPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.recipes.isEmpty() && uiState.drafts.isEmpty() && !uiState.isSearching -> {
                EmptyRecipesState(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    onCookNow = {
                        navController.popBackStack()
                        navController.navigate(Screen.AiCook.route)
                    },
                    onCreateOwn = {
                        navController.navigate(Screen.RecipeBuilder.route)
                    }
                )
            }

            else -> {
                val peekTargetId = uiState.drafts.firstOrNull()?.entity?.id
                    ?: uiState.recipes.firstOrNull()?.entity?.id

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    PageHeader("My Recipes")

                    // Search bar
                    ThemedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingSm),
                        placeholder = { Text("Search recipes...") },
                        leadingIcon = {
                            ThemedIcon(
                                materialIcon = Icons.Filled.Search,
                                inkIconRes = R.drawable.ic_ink_search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearch("") }) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.Clear,
                                        inkIconRes = R.drawable.ic_ink_close,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                    ) {
                        // Drafts section — always pinned, not affected by search
                        if (uiState.drafts.isNotEmpty()) {
                            item(key = "draft_header") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.appColors.accentOrange)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "In Progress",
                                        style = MaterialTheme.typography.formSectionLabel
                                    )
                                }
                            }
                            items(uiState.drafts, key = { "draft_${it.entity.id}" }) { draft ->
                                SavedRecipeCard(
                                    recipe = draft,
                                    isDraft = true,
                                    showPeekHint = !hasShownPeekHint && draft.entity.id == peekTargetId,
                                    onPeekComplete = { hasShownPeekHint = true },
                                    isExpanded = uiState.expandedRecipeId == draft.entity.id,
                                    isEditingNotes = uiState.editingNotesId == draft.entity.id,
                                    onToggleExpand = { viewModel.toggleExpanded(draft.entity.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(draft.entity.id) },
                                    onDelete = { viewModel.deleteRecipe(draft.entity.id) },
                                    onUpdateNotes = { viewModel.updateNotes(draft.entity.id, it) },
                                    onUpdateRating = { viewModel.updateRating(draft.entity.id, it) },
                                    onEditNotes = { viewModel.setEditingNotes(draft.entity.id) },
                                    onDismissEditNotes = { viewModel.setEditingNotes(null) },
                                    onCookAgain = {},
                                    onCookThis = {
                                        navController.navigate(Screen.RecipeBuilder.createRoute(draft.entity.id))
                                    },
                                    onEdit = {
                                        navController.navigate(Screen.RecipeBuilder.createRoute(draft.entity.id))
                                    },
                                    onAddToShopping = { viewModel.addMissingToShoppingList(draft) },
                                    onShare = {
                                        val text = viewModel.getShareText(draft)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share recipe"))
                                    }
                                )
                            }
                            item(key = "draft_divider") {
                                ThemedDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        // Favorites section
                        if (uiState.favorites.isNotEmpty() && !uiState.isSearching) {
                            item(key = "fav_header") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.Favorite,
                                        inkIconRes = R.drawable.ic_ink_heart,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.appColors.cookAccent
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Favorites",
                                        style = MaterialTheme.typography.formSectionLabel
                                    )
                                }
                            }
                            items(uiState.favorites, key = { "fav_${it.entity.id}" }) { recipe ->
                                SavedRecipeCard(
                                    recipe = recipe,
                                    showPeekHint = !hasShownPeekHint && recipe.entity.id == peekTargetId,
                                    onPeekComplete = { hasShownPeekHint = true },
                                    isExpanded = uiState.expandedRecipeId == recipe.entity.id,
                                    isEditingNotes = uiState.editingNotesId == recipe.entity.id,
                                    onToggleExpand = { viewModel.toggleExpanded(recipe.entity.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(recipe.entity.id) },
                                    onDelete = { viewModel.deleteRecipe(recipe.entity.id) },
                                    onUpdateNotes = { viewModel.updateNotes(recipe.entity.id, it) },
                                    onUpdateRating = { viewModel.updateRating(recipe.entity.id, it) },
                                    onEditNotes = { viewModel.setEditingNotes(recipe.entity.id) },
                                    onDismissEditNotes = { viewModel.setEditingNotes(null) },
                                    onCookAgain = {
                                        val json = viewModel.getCookAgainSettingsJson(recipe)
                                        CookViewModel.pendingCookAgainSettings = json
                                        navController.navigate(Screen.AiCook.route)
                                    },
                                    onCookThis = {
                                        navController.navigate(Screen.CookingPlayback.createRoute(recipe.entity.id))
                                    },
                                    onEdit = {
                                        navController.navigate(Screen.RecipeBuilder.createRoute(recipe.entity.id))
                                    },
                                    onAddToShopping = { viewModel.addMissingToShoppingList(recipe) },
                                    onShare = {
                                        val text = viewModel.getShareText(recipe)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share recipe"))
                                    }
                                )
                            }
                            item(key = "fav_divider") {
                                ThemedDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        // All recipes header
                        if (uiState.favorites.isNotEmpty() && !uiState.isSearching) {
                            item(key = "all_header") {
                                Text(
                                    "All Recipes",
                                    style = MaterialTheme.typography.formSectionLabel,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        // All recipes (or search results) — exclude favorites to avoid duplicates
                        val nonFavoriteRecipes = if (!uiState.isSearching && uiState.favorites.isNotEmpty()) {
                            uiState.recipes.filter { !it.entity.isFavorite }
                        } else {
                            uiState.recipes
                        }
                        items(nonFavoriteRecipes, key = { "recipe_${it.entity.id}" }) { recipe ->
                            SavedRecipeCard(
                                recipe = recipe,
                                showPeekHint = !hasShownPeekHint && recipe.entity.id == peekTargetId,
                                onPeekComplete = { hasShownPeekHint = true },
                                isExpanded = uiState.expandedRecipeId == recipe.entity.id,
                                isEditingNotes = uiState.editingNotesId == recipe.entity.id,
                                onToggleExpand = { viewModel.toggleExpanded(recipe.entity.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(recipe.entity.id) },
                                onDelete = { viewModel.deleteRecipe(recipe.entity.id) },
                                onUpdateNotes = { viewModel.updateNotes(recipe.entity.id, it) },
                                onUpdateRating = { viewModel.updateRating(recipe.entity.id, it) },
                                onEditNotes = { viewModel.setEditingNotes(recipe.entity.id) },
                                onDismissEditNotes = { viewModel.setEditingNotes(null) },
                                onCookAgain = {
                                    CookViewModel.pendingCookAgainSettings = viewModel.getCookAgainSettingsJson(recipe)
                                    navController.navigate(Screen.AiCook.route)
                                },
                                onCookThis = {
                                    navController.navigate(Screen.CookingPlayback.createRoute(recipe.entity.id))
                                },
                                onEdit = {
                                    navController.navigate(Screen.RecipeBuilder.createRoute(recipe.entity.id))
                                },
                                onAddToShopping = { viewModel.addMissingToShoppingList(recipe) },
                                onShare = {
                                    val text = viewModel.getShareText(recipe)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share recipe"))
                                }
                            )
                        }

                        item(key = "bottom_spacer") {
                            Spacer(Modifier.height(Dimens.spacingXl))
                        }
                    }
                }
            }
        }
    }
}

// ── Saved Recipe Card ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedRecipeCard(
    recipe: SavedRecipeDisplay,
    isDraft: Boolean = false,
    showPeekHint: Boolean = false,
    onPeekComplete: () -> Unit = {},
    isExpanded: Boolean,
    isEditingNotes: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onUpdateNotes: (String?) -> Unit,
    onUpdateRating: (Int) -> Unit,
    onEditNotes: () -> Unit,
    onDismissEditNotes: () -> Unit,
    onCookAgain: () -> Unit,
    onCookThis: () -> Unit = {},
    onEdit: () -> Unit = {},
    onAddToShopping: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val density = LocalDensity.current
    val peekOffsetPx = remember { Animatable(0f) }
    val isPeekingLeft = peekOffsetPx.value < -1f

    // One-time peek: slides card left to reveal delete ink wash, then snaps back
    LaunchedEffect(showPeekHint) {
        if (showPeekHint) {
            delay(900)
            val targetPx = with(density) { 44.dp.toPx() }
            peekOffsetPx.animateTo(-targetPx, tween(320, easing = FastOutSlowInEasing))
            delay(380)
            peekOffsetPx.animateTo(0f, PaperInkMotion.BouncySpring)
            onPeekComplete()
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    // Cancel peek if user starts swiping
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled && peekOffsetPx.value != 0f) {
            peekOffsetPx.snapTo(0f)
        }
    }

    Box(modifier = Modifier.graphicsLayer { clip = true }) {
        // Peek background: ink wash visible during peek slide
        if (isPeekingLeft) {
            val peekFraction = (kotlin.math.abs(peekOffsetPx.value) / with(density) { 200.dp.toPx() })
                .coerceIn(0f, 1f)
            InkWashSwipeBackground(
                progress = peekFraction,
                direction = SwipeToDismissBoxValue.EndToStart,
                modifier = Modifier.matchParentSize()
            )
        }

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.offset { IntOffset(peekOffsetPx.value.roundToInt(), 0) },
            backgroundContent = {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val widthPx = with(density) { maxWidth.toPx() }
                    val rawOffset = try { kotlin.math.abs(dismissState.requireOffset()) } catch (_: Exception) { 0f }
                    val visualProgress = if (widthPx > 0f) (rawOffset / widthPx).coerceIn(0f, 1f)
                                         else dismissState.progress
                    InkWashSwipeBackground(
                        progress = visualProgress,
                        direction = if (rawOffset > 1f) SwipeToDismissBoxValue.EndToStart
                                    else dismissState.targetValue
                    )
                }
            },
            enableDismissFromStartToEnd = false
        ) {
        AppCard(
            onClick = onToggleExpand,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(Dimens.spacingLg)) {

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Source badge + cook count
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDraft) {
                                Text(
                                    "Draft",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.appColors.accentOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                val (sourceLabel, sourceColor) = when (recipe.entity.source) {
                                    "manual" -> "My Recipe" to MaterialTheme.appColors.accentGreen
                                    "captured" -> "Captured" to MaterialTheme.appColors.accentOrange
                                    else -> "AI" to MaterialTheme.appColors.cookAccent
                                }
                                Text(
                                    sourceLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = sourceColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (recipe.cookCount > 0) {
                                Text(
                                    "·",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text(
                                    "Cooked ${recipe.cookCount}×",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(recipe.entity.name, style = MaterialTheme.typography.sectionHeader)
                        if (recipe.entity.cuisineOrigin.isNotBlank()) {
                            Text(
                                recipe.entity.cuisineOrigin,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.Schedule,
                                    inkIconRes = R.drawable.ic_ink_clock,
                                    contentDescription = "Time",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "${recipe.entity.timeMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                recipe.entity.difficulty.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (recipe.entity.difficulty) {
                                    "easy" -> MaterialTheme.appColors.difficultyEasy
                                    "hard" -> MaterialTheme.appColors.difficultyHard
                                    else -> MaterialTheme.appColors.difficultyMedium
                                }
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Favorite toggle (hidden for drafts)
                        if (!isDraft) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    if (recipe.entity.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Toggle favorite",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (recipe.entity.isFavorite) MaterialTheme.appColors.cookAccent
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.spacingSm))

                // Match bar (skip for drafts with no ingredients yet)
                if (!isDraft || recipe.totalCount > 0) {
                    val matchColor = when {
                        recipe.matchPercentage >= 90 -> MaterialTheme.appColors.accentGreen
                        recipe.matchPercentage >= 70 -> MaterialTheme.appColors.accentOrange
                        else -> MaterialTheme.appColors.difficultyHard
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ingredients: ${recipe.matchedCount}/${recipe.totalCount}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "${recipe.matchPercentage}% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = matchColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(Dimens.spacingXs))
                    val animatedProgress by animateFloatAsState(
                        targetValue = recipe.matchPercentage / 100f,
                        animationSpec = tween(400, easing = EaseOutCubic),
                        label = "matchBar"
                    )
                    ThemedProgressBar(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = matchColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Rating (if any)
                if (recipe.entity.rating > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.semantics {
                        contentDescription = "${recipe.entity.rating} out of 5 stars"
                    }) {
                        (1..5).forEach { star ->
                            ThemedIcon(
                                materialIcon = if (star <= recipe.entity.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                inkIconRes = if (star <= recipe.entity.rating) R.drawable.ic_ink_star else R.drawable.ic_ink_star_outline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (star <= recipe.entity.rating) MaterialTheme.appColors.starRating
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // Primary action button — always visible (not inside AnimatedVisibility)
                Spacer(Modifier.height(Dimens.spacingSm))
                if (isDraft) {
                    ThemedButton(
                        onClick = onCookThis,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.accentOrange
                        )
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.Edit,
                            inkIconRes = R.drawable.ic_ink_edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Continue editing",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    ThemedButton(
                        onClick = onCookThis,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        ThemedIcon(
                            materialIcon = Icons.Filled.PlayArrow,
                            inkIconRes = R.drawable.ic_ink_cook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cook This",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expanded details
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        ThemedDivider(modifier = Modifier.padding(vertical = Dimens.spacingMd))

                        // Description
                        if (recipe.entity.description.isNotBlank()) {
                            Text(
                                recipe.entity.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(Dimens.spacingMd))
                        }

                        // Ingredients header + "Add missing" button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ingredients", style = MaterialTheme.typography.formSectionLabel)
                            if (recipe.ingredients.any { !it.have_it }) {
                                TextButton(
                                    onClick = onAddToShopping,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AddShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add missing", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(Dimens.spacingSm))
                        recipe.ingredients.forEach { ing ->
                            val haveIt = ing.have_it
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (haveIt) MaterialTheme.appColors.accentGreen
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                    )
                                    Spacer(Modifier.width(Dimens.spacingSm))
                                    Text(
                                        ing.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (haveIt) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${ing.amount} ${ing.unit}".trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ThemedDivider(modifier = Modifier.padding(vertical = Dimens.spacingLg))

                        // Steps
                        Text("Steps", style = MaterialTheme.typography.formSectionLabel)
                        Spacer(Modifier.height(6.dp))
                        recipe.steps.forEachIndexed { idx, step ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    "${idx + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.appColors.cookAccent,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(step.instruction, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Tips
                        if (!recipe.entity.tips.isNullOrBlank()) {
                            ThemedDivider(modifier = Modifier.padding(vertical = Dimens.spacingLg))
                            AppCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                Row(modifier = Modifier.padding(10.dp)) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.Lightbulb,
                                        inkIconRes = R.drawable.ic_ink_lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        recipe.entity.tips.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        // Personal notes
                        ThemedDivider(modifier = Modifier.padding(vertical = Dimens.spacingLg))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Personal Notes", style = MaterialTheme.typography.formSectionLabel)
                            IconButton(onClick = onEditNotes, modifier = Modifier.size(28.dp)) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.Edit,
                                    inkIconRes = R.drawable.ic_ink_edit,
                                    contentDescription = "Edit notes",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (isEditingNotes) {
                            var notesText by remember(recipe.entity.id) {
                                mutableStateOf(recipe.entity.personalNotes ?: "")
                            }
                            ThemedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add your notes...") },
                                minLines = 2,
                                maxLines = 4,
                                shape = MaterialTheme.shapes.small
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismissEditNotes) { Text("Cancel") }
                                TextButton(onClick = {
                                    onUpdateNotes(notesText.ifBlank { null })
                                    onDismissEditNotes()
                                }) { Text("Save") }
                            }
                        } else {
                            Text(
                                recipe.entity.personalNotes ?: "No notes yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (recipe.entity.personalNotes != null)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = if (recipe.entity.personalNotes == null) FontStyle.Italic else FontStyle.Normal
                            )
                        }

                        // Rating
                        ThemedDivider(modifier = Modifier.padding(vertical = Dimens.spacingLg))
                        Text("Your Rating", style = MaterialTheme.typography.formSectionLabel)
                        Spacer(Modifier.height(Dimens.spacingSm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
                            (1..5).forEach { star ->
                                ThemedIcon(
                                    materialIcon = if (star <= recipe.entity.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    inkIconRes = if (star <= recipe.entity.rating) R.drawable.ic_ink_star else R.drawable.ic_ink_star_outline,
                                    contentDescription = "Rate $star",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            onUpdateRating(if (recipe.entity.rating == star) 0 else star)
                                        },
                                    tint = if (star <= recipe.entity.rating) MaterialTheme.appColors.starRating
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Action buttons row
                        Spacer(Modifier.height(Dimens.spacingLg))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                                ThemedIcon(
                                    materialIcon = Icons.Filled.Edit,
                                    inkIconRes = R.drawable.ic_ink_edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                            TextButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                            // Cook Again — only for AI recipes with saved settings
                            if (!isDraft && recipe.entity.source == "ai" && recipe.entity.sourceSettingsJson != null) {
                                ThemedButton(
                                    onClick = onCookAgain,
                                    modifier = Modifier.weight(2f),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.appColors.cookAccent
                                    )
                                ) {
                                    ThemedIcon(
                                        materialIcon = Icons.Filled.Restaurant,
                                        inkIconRes = R.drawable.ic_ink_cook,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Cook Again",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Expand hint
                Spacer(Modifier.height(Dimens.spacingXs))
                Text(
                    if (isExpanded) "Tap to collapse" else "Tap for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    } // end Box(graphicsLayer clip)
}

// ── Empty State ──────────────────────────────────────────────────────────

@Composable
private fun EmptyRecipesState(
    modifier: Modifier = Modifier,
    onCookNow: () -> Unit,
    onCreateOwn: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimens.spacingXxl)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "float")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "float_y"
            )
            ThemedIcon(
                materialIcon = Icons.Filled.MenuBook,
                inkIconRes = R.drawable.ic_ink_book,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { translationY = offsetY.dp.toPx() },
                tint = MaterialTheme.appColors.cookAccent.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(Dimens.spacingXl))

            val taglines = listOf(
                "Your recipe collection awaits",
                "Save recipes you love",
                "Build your cookbook",
                "Never lose a great recipe"
            )
            var tagIndex by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(4000)
                    tagIndex = (tagIndex + 1) % taglines.size
                }
            }
            AnimatedContent(
                targetState = taglines[tagIndex],
                transitionSpec = {
                    fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it }
                },
                label = "tagline"
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Dimens.spacingSm))
            Text(
                "Save AI-generated recipes or create your own",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(Dimens.spacingXl))
            ThemedButton(
                onClick = onCookNow,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.cookAccent)
            ) {
                ThemedIcon(materialIcon = Icons.Filled.Restaurant, inkIconRes = R.drawable.ic_ink_cook, contentDescription = null)
                Spacer(Modifier.width(Dimens.spacingSm))
                Text("What Can I Cook?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(Dimens.spacingMd))
            OutlinedButton(
                onClick = onCreateOwn,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                ThemedIcon(materialIcon = Icons.Filled.Edit, inkIconRes = R.drawable.ic_ink_edit, contentDescription = null)
                Spacer(Modifier.width(Dimens.spacingSm))
                Text("Create Your Own", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
