package com.inventory.app.ui.screens.pantryhealth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.inventory.app.domain.model.ScoreFactor
import com.inventory.app.ui.components.AppCard
import com.inventory.app.ui.components.ScoreLineChart
import com.inventory.app.ui.components.TipsSection
import com.inventory.app.ui.theme.ExpiryOrange
import com.inventory.app.ui.theme.ExpiryRed
import com.inventory.app.ui.theme.ScoreBlue
import com.inventory.app.ui.theme.ScoreTeal
import com.inventory.app.ui.theme.StockGreen
import com.inventory.app.ui.theme.StockYellow
import com.inventory.app.ui.theme.scoreToColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryHealthScreen(
    navController: NavController,
    viewModel: PantryHealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home Score") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Score Hero
            ScoreHeroSection(
                score = uiState.score,
                label = uiState.scoreLabel,
                engagementScore = uiState.engagementScore,
                conditionScore = uiState.conditionScore,
                motivationText = uiState.motivationText,
                trendDelta = uiState.trendDelta
            )

            // Section 2: Score Trend
            ScoreTrendSection(
                entries = uiState.chartEntries,
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { viewModel.selectPeriod(it) }
            )

            // Section 3: Score Breakdown
            ScoreBreakdownSection(
                engagementFactors = uiState.engagementFactors,
                conditionFactors = uiState.conditionFactors,
                onFactorClick = { route -> navController.navigate(route) }
            )

            // Section 4: Tips
            TipsSection(
                tips = uiState.tips,
                onTipAction = { route -> navController.navigate(route) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Section 1: Score Hero ─────────────────────────────────────────────

@Composable
private fun ScoreHeroSection(
    score: Int,
    label: String,
    engagementScore: Int,
    conditionScore: Int,
    motivationText: String,
    trendDelta: Int
) {
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationStarted) score / 100f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "scoreArc"
    )

    val scoreColor = scoreToColor(score)

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .aspectRatio(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                drawArc(
                    color = scoreColor.copy(alpha = 0.15f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                drawArc(
                    color = scoreColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val scoreText = "$score"
                val scoreStyle = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                val scoreLayout = textMeasurer.measure(scoreText, scoreStyle)
                drawText(
                    textLayoutResult = scoreLayout,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - scoreLayout.size.width) / 2f,
                        (size.height - scoreLayout.size.height) / 2f - 10.dp.toPx()
                    )
                )

                val labelStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = scoreColor.copy(alpha = 0.8f)
                )
                val labelLayout = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - labelLayout.size.width) / 2f,
                        (size.height - labelLayout.size.height) / 2f + 28.dp.toPx()
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Engagement / Condition sub-scores
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$engagementScore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Engagement", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$conditionScore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Condition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            motivationText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (trendDelta != 0) {
            val trendColor = if (trendDelta > 0) StockGreen else ExpiryRed
            val trendIcon = if (trendDelta > 0) "+" else ""
            Text(
                "$trendIcon$trendDelta from last week",
                style = MaterialTheme.typography.bodyMedium,
                color = trendColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Section 2: Score Trend ────────────────────────────────────────────

@Composable
private fun ScoreTrendSection(
    entries: List<com.inventory.app.ui.components.DailyChartEntry>,
    selectedPeriod: Int,
    onPeriodSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Score Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedPeriod == 7,
                onClick = { onPeriodSelected(7) },
                label = { Text("7 Days") }
            )
            FilterChip(
                selected = selectedPeriod == 30,
                onClick = { onPeriodSelected(30) },
                label = { Text("30 Days") }
            )
        }

        if (entries.size >= 2) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                ScoreLineChart(
                    entries = entries,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "History will appear after a few days of use",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─── Section 3: Score Breakdown ───────────────────────────────────────

@Composable
private fun ScoreBreakdownSection(
    engagementFactors: List<ScoreFactor>,
    conditionFactors: List<ScoreFactor>,
    onFactorClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Engagement section
        if (engagementFactors.isNotEmpty()) {
            Text("Building Your Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            engagementFactors.forEach { factor ->
                EngagementFactorCard(factor = factor, onClick = { onFactorClick(factor.route) })
            }
        }

        // Condition section
        if (conditionFactors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Issues to Fix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            conditionFactors.forEach { factor ->
                ConditionFactorCard(factor = factor, onClick = { onFactorClick(factor.route) })
            }
        }

        if (engagementFactors.isEmpty() && conditionFactors.isEmpty()) {
            AllGoodSection()
        }
    }
}

@Composable
private fun EngagementFactorCard(
    factor: ScoreFactor,
    onClick: () -> Unit
) {
    val icon = when (factor.icon) {
        "items" -> Icons.Filled.Inventory2
        "quality" -> Icons.Filled.Category
        else -> Icons.Filled.Add
    }

    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = StockGreen.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = StockGreen, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        factor.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "+${factor.points} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StockGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    factor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "View",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConditionFactorCard(
    factor: ScoreFactor,
    onClick: () -> Unit
) {
    val icon = when (factor.icon) {
        "expired" -> Icons.Filled.DeleteOutline
        "out_of_stock" -> Icons.Filled.RemoveShoppingCart
        "expiring" -> Icons.Filled.Warning
        "low_stock" -> Icons.Filled.TrendingDown
        "shopping" -> Icons.Filled.ShoppingCart
        else -> Icons.Filled.Inventory2
    }
    val iconColor = when (factor.icon) {
        "expired" -> ExpiryRed
        "out_of_stock" -> ExpiryRed
        "expiring" -> ExpiryOrange
        "low_stock" -> StockYellow
        "shopping" -> ScoreBlue
        else -> MaterialTheme.colorScheme.primary
    }

    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        factor.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "-${factor.points} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ExpiryRed,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    factor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Fix",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AllGoodSection() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = StockGreen,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    "All Clear!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "No issues found. Keep adding items to grow your score!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// scoreToColor() is now centralized in Color.kt — imported at top
