package com.inventory.app.ui.screens.cook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.R
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.InkBorderCard
import com.inventory.app.ui.components.PageScaffold
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.navigation.Screen
import com.inventory.app.ui.theme.appColors
import com.inventory.app.ui.theme.visuals

@Composable
fun CookHubScreen(
    navController: NavController,
    viewModel: CookHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PageScaffold(
        actions = {
            IconButton(onClick = { navController.navigate(Screen.SavedRecipes.route) }) {
                BadgedBox(
                    badge = {
                        if (uiState.savedRecipeCount > 0) {
                            Badge { Text("${uiState.savedRecipeCount}") }
                        }
                    }
                ) {
                    ThemedIcon(
                        materialIcon = Icons.Filled.MenuBook,
                        inkIconRes = R.drawable.ic_ink_book,
                        contentDescription = "My Recipes"
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    com.inventory.app.ui.components.PageHeader("Cook")
                }
                // ── Dynamic context card ──────────────────────────────
                item {
                    ContextCard(
                        uiState = uiState,
                        onResumeSession = { recipeId ->
                            navController.navigate(Screen.CookingPlayback.createRoute(recipeId))
                        },
                        onContinueDraft = { recipeId ->
                            navController.navigate(Screen.RecipeBuilder.createRoute(recipeId))
                        },
                        onCookAgain = { recipeId ->
                            navController.navigate(Screen.CookingPlayback.createRoute(recipeId))
                        },
                        onBrowse = {
                            navController.navigate(Screen.SavedRecipes.route)
                        }
                    )
                }

                // ── Primary action: AI recipes ────────────────────────
                item {
                    HubActionCard(
                        title = "What Can I Cook?",
                        subtitle = when {
                            uiState.expiringItemCount > 0 -> "${uiState.expiringItemCount} items expiring — let's use them!"
                            else -> "Recipes made from your kitchen"
                        },
                        icon = Icons.Filled.AutoAwesome,
                        inkIconRes = R.drawable.ic_ink_sparkle,
                        badge = "AI",
                        onClick = {
                            if (uiState.expiringItemIds.isNotEmpty()) {
                                CookViewModel.pendingExpiringItemIds = uiState.expiringItemIds
                            }
                            navController.navigate(
                                Screen.AiCook.createRoute(
                                    uiState.expiringItemIds.ifEmpty { null }
                                )
                            )
                        }
                    )
                }

                // ── Secondary action: Manual recipe builder ───────────
                item {
                    HubActionCard(
                        title = "What I Want to Cook",
                        subtitle = "Create your own recipe",
                        icon = Icons.Filled.Create,
                        inkIconRes = R.drawable.ic_ink_edit,
                        badge = null,
                        onClick = { navController.navigate(Screen.RecipeBuilder.createRoute()) }
                    )
                }

                // ── Tertiary action: Capture while cooking ────────────
                if (uiState.showCaptureCard) {
                    item {
                        HubActionCard(
                            title = "Capture While Cooking",
                            subtitle = "Record your recipe as you cook",
                            icon = Icons.Filled.RadioButtonChecked,
                            inkIconRes = R.drawable.ic_ink_camera,
                            badge = null,
                            onClick = { navController.navigate(Screen.RecipeBuilder.createRoute(captureMode = true)) }
                        )
                    }
                }

                // ── Quaternary action: Describe a recipe with AI ─────
                item {
                    HubActionCard(
                        title = "Describe a Recipe",
                        subtitle = "Type a recipe idea — AI writes it for you",
                        icon = Icons.Filled.AutoAwesome,
                        inkIconRes = R.drawable.ic_ink_sparkle,
                        badge = "AI",
                        onClick = { navController.navigate(Screen.AiRecipeDescription.route) }
                    )
                }

                // ── My Recipes horizontal row ─────────────────────────
                if (uiState.recentRecipes.isNotEmpty()) {
                    item {
                        Text(
                            "My Recipes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            items(uiState.recentRecipes, key = { it.id }) { recipe ->
                                RecipeChip(
                                    recipe = recipe,
                                    onClick = { navController.navigate(Screen.CookingPlayback.createRoute(recipe.id)) }
                                )
                            }
                            item {
                                // "See All →" chip
                                AppCard(
                                    onClick = { navController.navigate(Screen.SavedRecipes.route) },
                                    modifier = Modifier.width(96.dp).height(80.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "See All →",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom spacing for nav bar
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Context card — shows highest-priority contextual info ────────────

@Composable
private fun ContextCard(
    uiState: CookHubUiState,
    onResumeSession: (Long) -> Unit = {},
    onContinueDraft: (Long) -> Unit,
    onCookAgain: (Long) -> Unit,
    onBrowse: () -> Unit
) {
    val isPaperInk = !MaterialTheme.visuals.useElevation

    val cardContent: @Composable () -> Unit = {
        when {
            // Priority 0: Active cooking session (resume mid-cook)
            uiState.resumeSession != null -> {
                val session = uiState.resumeSession
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Resume cooking",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            session.recipeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Step ${session.stepIndex + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onResumeSession(session.recipeId) }) {
                        Text("Continue →")
                    }
                }
                // U-4: Show draft as secondary row when both exist
                if (uiState.draftRecipe != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Draft",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                uiState.draftRecipe.recipeName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { onContinueDraft(uiState.draftRecipe.recipeId) }) {
                            Text("Edit →", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Priority 1: Draft in progress
            uiState.draftRecipe != null -> {
                val draft = uiState.draftRecipe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Continue your recipe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            draft.recipeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (draft.stepCount > 0) {
                            Text(
                                "${draft.stepCount} step${if (draft.stepCount != 1) "s" else ""} so far",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { onContinueDraft(draft.recipeId) },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Continue →", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Priority 2: Last cooked recipe
            uiState.lastCooked != null -> {
                val last = uiState.lastCooked
                val daysText = if (last.daysAgo == 0) "Today" else "${last.daysAgo}d ago"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Last cooked · $daysText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            last.recipeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { onCookAgain(last.recipeId) },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Cook Again", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Priority 3: Has saved recipes — show count
            uiState.savedRecipeCount > 0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Your collection",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${uiState.savedRecipeCount} recipe${if (uiState.savedRecipeCount != 1) "s" else ""} saved",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onBrowse,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Browse →", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Priority 4: First time — empty state encouragement
            else -> {
                Column {
                    Text(
                        "Start your recipe collection",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Create your first recipe below, or let AI suggest one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (isPaperInk) {
        InkBorderCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                cardContent()
            }
        }
    } else {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                cardContent()
            }
        }
    }
}

// ── Hub action card — large tappable card for primary actions ────────

@Composable
private fun HubActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    inkIconRes: Int,
    badge: String?,
    onClick: () -> Unit
) {
    val isPaperInk = !MaterialTheme.visuals.useElevation

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(
                materialIcon = icon,
                inkIconRes = inkIconRes,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.appColors.cookAccent
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (isPaperInk) {
        InkBorderCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    } else {
        AppCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

// ── Recent recipe chip ────────────────────────────────────────────────

@Composable
private fun RecipeChip(
    recipe: RecentRecipeInfo,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.width(120.dp).height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                recipe.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (recipe.timeMinutes > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${recipe.timeMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
