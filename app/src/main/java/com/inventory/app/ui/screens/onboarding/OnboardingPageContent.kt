package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Paper & Ink spring presets (from animation-theme.md) ───────────────
private val BouncySpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 200f)
private val WobblySpring: SpringSpec<Float> = spring(dampingRatio = 0.3f, stiffness = 200f)
private val GentleSpring: SpringSpec<Float> = spring(dampingRatio = 1.0f, stiffness = 200f)

// ─── Dispatcher ─────────────────────────────────────────────────────────

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    state: OnboardingViewModel.UiState,
    onRegionConfirm: () -> Unit,
    onRegionChange: () -> Unit,
    onRegionSelect: (RegionInfo) -> Unit,
    onStartChoiceSelect: (StartChoice) -> Unit,
    onGetStarted: () -> Unit,
    onComplete: () -> Unit
) {
    when (page) {
        OnboardingPage.Welcome -> WelcomePage(onGetStarted = onGetStarted)
        OnboardingPage.RegionSetup -> RegionSetupPage(
            selectedRegion = state.selectedRegion,
            showPicker = state.showRegionPicker,
            onConfirm = onRegionConfirm,
            onChange = onRegionChange,
            onSelect = onRegionSelect
        )
        OnboardingPage.FirstItems -> FirstItemsPage(
            selectedChoice = state.startChoice,
            onChoiceSelect = onStartChoiceSelect
        )
        OnboardingPage.AllSet -> AllSetPage(
            selectedRegion = state.selectedRegion,
            startChoice = state.startChoice,
            onComplete = onComplete
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Page 1: Welcome — everything lands, writes in, and breathes
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WelcomePage(onGetStarted: () -> Unit) {
    // Orchestrated reveal phases
    var bannerLanded by remember { mutableStateOf(false) }
    var propsReady by remember { mutableStateOf(false) }
    var buttonReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        bannerLanded = true
        delay(500)
        propsReady = true
        delay(350) // after props stagger completes
        buttonReady = true
    }

    // ── Banner: "Land" (scale from small + drop from above) then breathe ──
    val bannerScale by animateFloatAsState(
        targetValue = if (bannerLanded) 1f else 0.3f,
        animationSpec = BouncySpring, label = "bannerScale"
    )
    val bannerOffsetY by animateFloatAsState(
        targetValue = if (bannerLanded) 0f else -30f,
        animationSpec = BouncySpring, label = "bannerDrop"
    )
    val bannerAlpha by animateFloatAsState(
        targetValue = if (bannerLanded) 1f else 0f,
        animationSpec = tween(300), label = "bannerAlpha"
    )

    // Breathing at rest
    val breathe = rememberInfiniteTransition(label = "bannerBreathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "breatheScale"
    )

    // ── Button: "Fade Up" ──
    val buttonY by animateFloatAsState(
        targetValue = if (buttonReady) 0f else 20f,
        animationSpec = GentleSpring, label = "btnY"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonReady) 1f else 0f,
        animationSpec = tween(300), label = "btnAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Brand banner — lands then breathes, constrained for tablets
        Image(
            painter = painterResource(id = R.drawable.restokk_banner),
            contentDescription = "Restokk — AI-powered kitchen tracker",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .graphicsLayer {
                    scaleX = bannerScale * (if (bannerLanded) breatheScale else 1f)
                    scaleY = bannerScale * (if (bannerLanded) breatheScale else 1f)
                    translationY = bannerOffsetY
                    alpha = bannerAlpha
                }
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Value props — "Write In" stagger cascade
        val valueProps = listOf(
            Triple(Icons.Filled.CameraAlt, "Scan Your Kitchen", "AI identifies items from photos"),
            Triple(Icons.Filled.ShoppingCart, "Smart Shopping Lists", "Budget tracking & auto-categorize"),
            Triple(Icons.Filled.Restaurant, "Meal Suggestions", "Recipes from what you already have"),
            Triple(Icons.Filled.QrCodeScanner, "Barcode & Receipt Scan", "Add items in seconds"),
            Triple(Icons.Filled.Eco, "Expiry & Stock Alerts", "Never waste food again"),
            Triple(Icons.Filled.Notifications, "Smart Reminders", "Restock nudges & shopping alerts"),
        )

        valueProps.forEachIndexed { index, (icon, title, subtitle) ->
            WriteInItem(
                index = index,
                visible = propsReady,
                staggerMs = 70L
            ) {
                ValuePropRow(icon = icon, title = title, subtitle = subtitle, index = index, visible = propsReady)
            }
            if (index < valueProps.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "...and more" hint
        WriteInItem(
            index = valueProps.size,
            visible = propsReady,
            staggerMs = 70L
        ) {
            Text(
                text = "...and much more inside",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CTA — fades up last
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer { translationY = buttonY; alpha = buttonAlpha },
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ValuePropRow(icon: ImageVector, title: String, subtitle: String, index: Int, visible: Boolean) {
    // Icon "Land" independently with stagger
    var iconLanded by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 70L + 50L)
            iconLanded = true
        }
    }
    val iconScale by animateFloatAsState(
        targetValue = if (iconLanded) 1f else 0.3f,
        animationSpec = WobblySpring, label = "vpIconScale$index"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Page 2: Region Setup — card lands, flag wobbles, details fade up
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RegionSetupPage(
    selectedRegion: RegionInfo,
    showPicker: Boolean,
    onConfirm: () -> Unit,
    onChange: () -> Unit,
    onSelect: (RegionInfo) -> Unit
) {
    var headlineReady by remember { mutableStateOf(false) }
    var subtitleReady by remember { mutableStateOf(false) }
    var cardReady by remember { mutableStateOf(false) }
    var buttonsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        headlineReady = true
        delay(200)
        subtitleReady = true
        delay(250)
        cardReady = true
        delay(300)
        buttonsReady = true
    }

    // Headline: Write In
    val headX by animateFloatAsState(
        targetValue = if (headlineReady) 0f else -50f,
        animationSpec = BouncySpring, label = "regHeadX"
    )
    val headAlpha by animateFloatAsState(
        targetValue = if (headlineReady) 1f else 0f,
        animationSpec = tween(250), label = "regHeadAlpha"
    )
    // Subtitle: Fade Up
    val subY by animateFloatAsState(
        targetValue = if (subtitleReady) 0f else 12f,
        animationSpec = GentleSpring, label = "regSubY"
    )
    val subAlpha by animateFloatAsState(
        targetValue = if (subtitleReady) 1f else 0f,
        animationSpec = tween(200), label = "regSubAlpha"
    )
    // Card: Land (scale down from 1.15 + slight drop)
    val cardScale by animateFloatAsState(
        targetValue = if (cardReady) 1f else 1.15f,
        animationSpec = BouncySpring, label = "regCardScale"
    )
    val cardY by animateFloatAsState(
        targetValue = if (cardReady) 0f else -20f,
        animationSpec = BouncySpring, label = "regCardY"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (cardReady) 1f else 0f,
        animationSpec = tween(250), label = "regCardAlpha"
    )
    // Card breathes at rest
    val breathe = rememberInfiniteTransition(label = "cardBreathe")
    val cardBreathe by breathe.animateFloat(
        initialValue = 1f, targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "cardBreatheScale"
    )
    // Flag wobble (slight rotation oscillation after landing)
    val flagRotation = remember { Animatable(0f) }
    LaunchedEffect(cardReady) {
        if (cardReady) {
            delay(200)
            flagRotation.animateTo(8f, animationSpec = tween(120))
            flagRotation.animateTo(-6f, animationSpec = tween(100))
            flagRotation.animateTo(3f, animationSpec = tween(90))
            flagRotation.animateTo(0f, animationSpec = tween(80))
        }
    }
    // Buttons: Fade Up
    val btnY by animateFloatAsState(
        targetValue = if (buttonsReady) 0f else 20f,
        animationSpec = GentleSpring, label = "regBtnY"
    )
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonsReady) 1f else 0f,
        animationSpec = tween(250), label = "regBtnAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your Region",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { translationX = headX; alpha = headAlpha }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll set your currency and date format",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { translationY = subY; alpha = subAlpha }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Region card — lands then breathes
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = cardScale * (if (cardReady) cardBreathe else 1f)
                    scaleX = s; scaleY = s
                    translationY = cardY
                    alpha = cardAlpha
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flag — wobbles on arrival
                Text(
                    text = selectedRegion.flag,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.graphicsLayer { rotationZ = flagRotation.value }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = selectedRegion.countryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Details fade up after card lands
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailChip(label = "Currency", value = selectedRegion.currencySymbol)
                    DetailChip(label = "Dates", value = selectedRegion.dateFormatPreview)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Box(modifier = Modifier.graphicsLayer { translationY = btnY; alpha = btnAlpha }) {
            AnimatedContent(
                targetState = showPicker,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "regionActions"
            ) { picking ->
                if (picking) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(popularRegions, key = { it.countryCode }) { region ->
                                val isSelected = region.countryCode == selectedRegion.countryCode
                                ListItem(
                                    headlineContent = {
                                        Text("${region.flag}  ${region.countryName}")
                                    },
                                    supportingContent = {
                                        Text("${region.currencySymbol} \u2022 ${region.dateFormatPreview}")
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelect(region) }
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Looks Right!", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onChange) {
                            Text("Change Region")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Page 3: First Items — cards land with stagger, selection pops with wobble
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FirstItemsPage(
    selectedChoice: StartChoice?,
    onChoiceSelect: (StartChoice) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var headlineReady by remember { mutableStateOf(false) }
    var subtitleReady by remember { mutableStateOf(false) }
    var cardsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        headlineReady = true
        delay(200)
        subtitleReady = true
        delay(250)
        cardsReady = true
    }

    val headX by animateFloatAsState(
        targetValue = if (headlineReady) 0f else -50f,
        animationSpec = BouncySpring, label = "fiHeadX"
    )
    val headAlpha by animateFloatAsState(
        targetValue = if (headlineReady) 1f else 0f,
        animationSpec = tween(250), label = "fiHeadAlpha"
    )
    val subY by animateFloatAsState(
        targetValue = if (subtitleReady) 0f else 12f,
        animationSpec = GentleSpring, label = "fiSubY"
    )
    val subAlpha by animateFloatAsState(
        targetValue = if (subtitleReady) 1f else 0f,
        animationSpec = tween(200), label = "fiSubAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "How Do You Want to Start?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { translationX = headX; alpha = headAlpha }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can always do both later",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { translationY = subY; alpha = subAlpha }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChoiceCard(
                icon = Icons.Filled.CameraAlt,
                title = "Scan Kitchen",
                subtitle = "AI identifies items from a photo",
                isSelected = selectedChoice == StartChoice.SCAN_KITCHEN,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChoiceSelect(StartChoice.SCAN_KITCHEN)
                },
                entranceIndex = 0,
                visible = cardsReady,
                modifier = Modifier.weight(1f)
            )
            ChoiceCard(
                icon = Icons.Filled.Edit,
                title = "Add Manually",
                subtitle = "Type in your first items",
                isSelected = selectedChoice == StartChoice.ADD_MANUALLY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChoiceSelect(StartChoice.ADD_MANUALLY)
                },
                entranceIndex = 1,
                visible = cardsReady,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    entranceIndex: Int,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // Staggered "Land" entrance
    var landed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(entranceIndex * 100L)
            landed = true
        }
    }
    val entranceScale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.5f,
        animationSpec = BouncySpring, label = "choiceLand$entranceIndex"
    )
    val entranceY by animateFloatAsState(
        targetValue = if (landed) 0f else 25f,
        animationSpec = BouncySpring, label = "choiceDropY$entranceIndex"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = tween(200), label = "choiceFade$entranceIndex"
    )

    // Selection animation — WobblySpring pop
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200), label = "choiceBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200), label = "choiceContainer"
    )
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = WobblySpring, label = "choicePop"
    )
    // Icon bounce on selection
    val iconBounce by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = WobblySpring, label = "iconPop"
    )

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .graphicsLayer {
                scaleX = entranceScale * selectionScale
                scaleY = entranceScale * selectionScale
                translationY = entranceY
                alpha = entranceAlpha
            },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = iconBounce; scaleY = iconBounce }
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Animated checkmark on selection
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(WobblySpring) + fadeIn(tween(150)),
                exit = scaleOut(tween(100)) + fadeOut(tween(100))
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Page 4: All Set — ink celebration, then staggered summary
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AllSetPage(
    selectedRegion: RegionInfo,
    startChoice: StartChoice?,
    onComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Phased reveal
    var celebrationDone by remember { mutableStateOf(false) }
    var headlineReady by remember { mutableStateOf(false) }
    var summaryReady by remember { mutableStateOf(false) }
    var buttonReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500) // let celebration hit its climax
        celebrationDone = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(300)
        headlineReady = true
        delay(250)
        summaryReady = true
        delay(400) // after stagger
        buttonReady = true
    }

    // Headline: scale in from center
    val headScale by animateFloatAsState(
        targetValue = if (headlineReady) 1f else 0.7f,
        animationSpec = BouncySpring, label = "asHeadScale"
    )
    val headAlpha by animateFloatAsState(
        targetValue = if (headlineReady) 1f else 0f,
        animationSpec = tween(300), label = "asHeadAlpha"
    )

    // Button breathes gently while waiting
    val btnBreathe = rememberInfiniteTransition(label = "btnBreathe")
    val btnScale by btnBreathe.animateFloat(
        initialValue = 1f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "btnBreatheScale"
    )
    val btnY by animateFloatAsState(
        targetValue = if (buttonReady) 0f else 25f,
        animationSpec = BouncySpring, label = "asBtnY"
    )
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonReady) 1f else 0f,
        animationSpec = tween(300), label = "asBtnAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ink celebration — compact version
        OnboardingInkCelebration()

        Spacer(modifier = Modifier.height(16.dp))

        // Headline
        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = headScale; scaleY = headScale; alpha = headAlpha
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Summary rows — Write In stagger
        val summaryItems = listOf(
            "Region" to "${selectedRegion.flag} ${selectedRegion.countryName}",
            "Currency" to selectedRegion.currencySymbol,
            "First step" to when (startChoice) {
                StartChoice.SCAN_KITCHEN -> "Scan your kitchen"
                StartChoice.ADD_MANUALLY -> "Add items manually"
                null -> "Explore the app"
            }
        )

        summaryItems.forEachIndexed { index, (label, value) ->
            WriteInItem(index = index, visible = summaryReady, staggerMs = 80L) {
                SummaryRow(label = label, value = value)
            }
            if (index < summaryItems.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // CTA — breathes gently
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = if (buttonReady) btnScale else 1f
                    scaleY = if (buttonReady) btnScale else 1f
                    translationY = btnY; alpha = btnAlpha
                },
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Let's Go!", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared animation primitives
// ═══════════════════════════════════════════════════════════════════════════

/** "Write In" entrance: slides from left with BouncySpring, fades in. Staggered per index. */
@Composable
private fun WriteInItem(
    index: Int,
    visible: Boolean,
    staggerMs: Long = 70L,
    content: @Composable () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * staggerMs)
            appeared = true
        }
    }

    val offsetX by animateFloatAsState(
        targetValue = if (appeared) 0f else -40f,
        animationSpec = BouncySpring, label = "writeX$index"
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(250), label = "writeAlpha$index"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX; this.alpha = alpha
        }
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Onboarding Ink Celebration — compact fireworks for the AllSet page
// ═══════════════════════════════════════════════════════════════════════════

private val CelebrationColors = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
)

private data class InkParticle(
    val angle: Float,
    val force: Float,
    val color: Color,
    val radius: Float,
    val delay: Float
)

@Composable
private fun OnboardingInkCelebration() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(1000, easing = EaseOutCubic))
    }

    val particles = remember {
        val colors = CelebrationColors.shuffled()
        List(14) { i ->
            InkParticle(
                angle = -70f + Random.nextFloat() * 140f,
                force = 0.45f + Random.nextFloat() * 0.55f,
                color = colors[i % colors.size],
                radius = 2f + Random.nextFloat() * 2.5f,
                delay = Random.nextFloat() * 0.2f
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val launchY = h * 0.9f

        particles.forEach { p ->
            val t = ((progress.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach

            val radians = ((-90f + p.angle) * PI / 180f).toFloat()
            val maxDist = p.force * h * 0.8f
            val gravity = h * 0.5f

            val x = cx + cos(radians) * maxDist * t
            val y = launchY + sin(radians) * maxDist * t + gravity * t * t

            val alpha = when {
                t < 0.1f -> t / 0.1f
                t > 0.65f -> (1f - (t - 0.65f) / 0.35f) * 0.8f
                else -> 0.8f
            }.coerceIn(0f, 1f)

            val sizeScale = when {
                t < 0.4f -> 0.4f + t * 1.5f
                else -> 1f - (t - 0.4f) * 0.5f
            }.coerceIn(0.2f, 1.2f)

            // Splatter halo near apex
            if (t in 0.3f..0.6f) {
                val haloAlpha = (1f - kotlin.math.abs(t - 0.45f) / 0.15f).coerceIn(0f, 1f) * 0.18f
                drawCircle(
                    color = p.color.copy(alpha = haloAlpha),
                    radius = p.radius.dp.toPx() * sizeScale * 2.5f,
                    center = Offset(x, y)
                )
            }

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius.dp.toPx() * sizeScale,
                center = Offset(x, y)
            )
        }
    }
}
