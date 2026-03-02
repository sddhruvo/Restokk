package com.inventory.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inventory.app.R
import com.inventory.app.ui.components.ThemedButton
import com.inventory.app.ui.components.RuledLinesBackground
import com.inventory.app.ui.components.ThemedIcon
import com.inventory.app.ui.components.ThemedTextField
import com.inventory.app.ui.theme.LocalReduceMotion
import com.inventory.app.ui.theme.PaperInkMotion
import kotlinx.coroutines.delay

private val BouncySpring = PaperInkMotion.BouncySpring
private val WobblySpring = PaperInkMotion.WobblySpring
private val GentleSpring = PaperInkMotion.GentleSpring

// ─── Dispatcher ─────────────────────────────────────────────────────────

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    state: OnboardingViewModel.UiState,
    onRegionChange: () -> Unit,
    onRegionSelect: (RegionInfo) -> Unit,
    onPreferenceSelect: (UserPreference) -> Unit,
    onGetStarted: () -> Unit,
    onSaveAndContinue: () -> Unit,
    firstMagicContent: @Composable () -> Unit = {}
) {
    when (page) {
        OnboardingPage.StoryOpens -> StoryOpensPage(onGetStarted = onGetStarted)
        OnboardingPage.YourKitchen -> YourKitchenPage(
            selectedRegion = state.selectedRegion,
            showPicker = state.showRegionPicker,
            selectedPreference = state.userPreference,
            onRegionChange = onRegionChange,
            onRegionSelect = onRegionSelect,
            onPreferenceSelect = onPreferenceSelect,
            onSaveAndContinue = onSaveAndContinue
        )
        OnboardingPage.FirstMagic -> firstMagicContent()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Screen 1: Story Opens — emotional intro
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun StoryOpensPage(onGetStarted: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    // ── Orchestrated reveal phases (spec: ~3.5s total) ──
    var line1Ready by rememberSaveable { mutableStateOf(reduceMotion) }
    var line2Ready by rememberSaveable { mutableStateOf(reduceMotion) }
    var logoReady by rememberSaveable { mutableStateOf(reduceMotion) }
    var buttonReady by rememberSaveable { mutableStateOf(reduceMotion) }

    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        if (!line1Ready) {
            delay(400)
            line1Ready = true
        }
        if (!line2Ready) {
            delay(800)
            line2Ready = true
        }
        if (!logoReady) {
            delay(800)
            logoReady = true
        }
        if (!buttonReady) {
            delay(800)
            buttonReady = true
        }
    }

    // ── Line 1: "Write In" (slide left + fade) ──
    val density = LocalDensity.current
    val writeInPx = with(density) { 20.dp.toPx() }
    val line1X by animateFloatAsState(
        targetValue = if (line1Ready) 0f else -writeInPx,
        animationSpec = BouncySpring, label = "line1X"
    )
    val line1Alpha by animateFloatAsState(
        targetValue = if (line1Ready) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "line1Alpha"
    )

    // ── Line 2: "Write In" (slightly smaller text per spec) ──
    val line2X by animateFloatAsState(
        targetValue = if (line2Ready) 0f else -writeInPx,
        animationSpec = BouncySpring, label = "line2X"
    )
    val line2Alpha by animateFloatAsState(
        targetValue = if (line2Ready) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "line2Alpha"
    )

    // ── Logo: "Land" (scale 0.3→1.0 + drop) then breathing ──
    val logoDropPx = with(density) { 30.dp.toPx() }
    val logoScale by animateFloatAsState(
        targetValue = if (logoReady) 1f else 0.3f,
        animationSpec = BouncySpring, label = "logoScale"
    )
    val logoY by animateFloatAsState(
        targetValue = if (logoReady) 0f else -logoDropPx,
        animationSpec = BouncySpring, label = "logoDrop"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "logoAlpha"
    )

    // Logo breathing at rest (1.0↔1.02, 2500ms)
    val breathe = rememberInfiniteTransition(label = "logoBreathe")
    val breatheScale by breathe.animateFloat(
        initialValue = 1f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "breatheScale"
    )

    // ── Button: "Fade Up" (rise + fade) then breathing ──
    val btnRisePx = with(density) { 12.dp.toPx() }
    val btnY by animateFloatAsState(
        targetValue = if (buttonReady) 0f else btnRisePx,
        animationSpec = GentleSpring, label = "btnY"
    )
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "btnAlpha"
    )

    // Button breathing after appearing (spec: 1.0↔1.015, 2500ms)
    val btnBreathe = rememberInfiniteTransition(label = "btnBreathe")
    val btnBreatheScale by btnBreathe.animateFloat(
        initialValue = 1f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "btnBreatheScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RuledLinesBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Line 1 — Write In (spec: text first, then logo)
                Text(
                    text = "Every kitchen has a story.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        translationX = line1X; alpha = line1Alpha
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Line 2 — Write In, slightly smaller (spec), bold
                Text(
                    text = "Let's write yours.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        translationX = line2X; alpha = line2Alpha
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Logo — lands then breathes (spec: after text)
                Image(
                    painter = painterResource(id = R.drawable.restokk_banner),
                    contentDescription = "Restokk — AI-powered kitchen tracker",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .graphicsLayer {
                            scaleX = logoScale * (if (logoReady) breatheScale else 1f)
                            scaleY = logoScale * (if (logoReady) breatheScale else 1f)
                            translationY = logoY
                            alpha = logoAlpha
                        }
                )
            }

            // CTA — Fade Up then breathing (spec: "Open the first page")
            ThemedButton(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(52.dp)
                    .graphicsLayer {
                        translationY = btnY; alpha = btnAlpha
                        val bs = if (buttonReady) btnBreatheScale else 1f
                        scaleX = bs; scaleY = bs
                    },
                shape = MaterialTheme.shapes.large
            ) {
                Text("Open the first page", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Screen 2: Your Kitchen — region + preference
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun YourKitchenPage(
    selectedRegion: RegionInfo,
    showPicker: Boolean,
    selectedPreference: UserPreference?,
    onRegionChange: () -> Unit,
    onRegionSelect: (RegionInfo) -> Unit,
    onPreferenceSelect: (UserPreference) -> Unit,
    onSaveAndContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val writeInPx = with(density) { 20.dp.toPx() }

    // ── Auto-default INVENTORY after 5 seconds (spec) ──
    var autoDefaultApplied by remember { mutableStateOf(false) }
    LaunchedEffect(selectedPreference) {
        if (selectedPreference == null && !autoDefaultApplied) {
            delay(5000)
            // LaunchedEffect keyed on selectedPreference — if user picks before
            // 5s, this coroutine is cancelled and restarted with non-null value
            autoDefaultApplied = true
            onPreferenceSelect(UserPreference.INVENTORY)
        }
    }

    // ── Orchestrated entrance phases ──
    var headlineReady by remember { mutableStateOf(false) }
    var regionReady by remember { mutableStateOf(false) }
    var dividerReady by remember { mutableStateOf(false) }
    var prefHeaderReady by remember { mutableStateOf(false) }
    var cardsReady by remember { mutableStateOf(false) }
    var buttonReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)     // 0ms — headline
        headlineReady = true
        delay(250)     // 150ms — region
        regionReady = true
        delay(250)     // 400ms — divider pen-stroke
        dividerReady = true
        delay(100)     // 500ms — pref header
        prefHeaderReady = true
        delay(100)     // 600ms — cards
        cardsReady = true
        delay(330)     // after 3 cards stagger (3 × 70ms + settle)
        buttonReady = true
    }

    // ── Headline: Write-In ──
    val headX by animateFloatAsState(
        targetValue = if (headlineReady) 0f else -writeInPx,
        animationSpec = BouncySpring, label = "ykHeadX"
    )
    val headAlpha by animateFloatAsState(
        targetValue = if (headlineReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "ykHeadAlpha"
    )

    // ── Divider pen-stroke: draws left→right (spec: 250ms, primary 0.3 alpha) ──
    val dividerFraction by animateFloatAsState(
        targetValue = if (dividerReady) 1f else 0f,
        animationSpec = tween(250), label = "dividerDraw"
    )
    val dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    // ── "What matters most?" header: Fade Up ──
    val prefHeaderFadeUpPx = with(density) { 12.dp.toPx() }
    val prefHeaderY by animateFloatAsState(
        targetValue = if (prefHeaderReady) 0f else prefHeaderFadeUpPx,
        animationSpec = GentleSpring, label = "prefHdrY"
    )
    val prefHeaderAlpha by animateFloatAsState(
        targetValue = if (prefHeaderReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "prefHdrAlpha"
    )

    // ── Button: Fade Up ──
    val btnRisePx = with(density) { 20.dp.toPx() }
    val btnY by animateFloatAsState(
        targetValue = if (buttonReady) 0f else btnRisePx,
        animationSpec = GentleSpring, label = "ykBtnY"
    )
    val btnAlpha by animateFloatAsState(
        targetValue = if (buttonReady) 1f else 0f,
        animationSpec = tween(PaperInkMotion.DurationMedium), label = "ykBtnAlpha"
    )

    // CTA enabled when preference selected (region auto-accepted)
    val ctaEnabled = selectedPreference != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Headline — Write-In
            Text(
                text = "Your Kitchen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = headX; alpha = headAlpha
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Region section — tappable card with inline picker
            RegionSection(
                selectedRegion = selectedRegion,
                showPicker = showPicker,
                visible = regionReady,
                onChange = onRegionChange,
                onSelect = onRegionSelect
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider pen-stroke: draws left→right (spec: 250ms, primary 0.3 alpha)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                if (dividerFraction > 0f) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width * dividerFraction, size.height / 2),
                        strokeWidth = size.height
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "What matters most?" — Fade Up (keeping #26 as approved)
            Text(
                text = "What matters most?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationY = prefHeaderY; alpha = prefHeaderAlpha
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3 preference cards — staggered Land
            UserPreference.entries.forEachIndexed { index, pref ->
                PreferenceCard(
                    preference = pref,
                    isSelected = selectedPreference == pref,
                    entranceIndex = index,
                    visible = cardsReady,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        autoDefaultApplied = false
                        onPreferenceSelect(pref)
                    }
                )
                if (index < UserPreference.entries.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // CTA "Next →" — saves settings and advances to Act 2
        ThemedButton(
            onClick = onSaveAndContinue,
            enabled = ctaEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp)
                .graphicsLayer {
                    translationY = btnY
                    alpha = btnAlpha
                },
            shape = MaterialTheme.shapes.large
        ) {
            Text("Next \u2192", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Region Section — card + confirm/change + picker
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RegionSection(
    selectedRegion: RegionInfo,
    showPicker: Boolean,
    visible: Boolean,
    onChange: () -> Unit,
    onSelect: (RegionInfo) -> Unit
) {
    // ── Card: Land (scale 1.15→1.0, translateY -20→0) ──
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 1.15f,
        animationSpec = BouncySpring, label = "regCardScale"
    )
    val cardY by animateFloatAsState(
        targetValue = if (visible) 0f else -20f,
        animationSpec = BouncySpring, label = "regCardY"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250), label = "regCardAlpha"
    )

    // Card breathes at rest (1.0↔1.01, 2500ms)
    val breathe = rememberInfiniteTransition(label = "cardBreathe")
    val cardBreathe by breathe.animateFloat(
        initialValue = 1f, targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "cardBreatheScale"
    )

    // Flag wobble after card lands
    val flagRotation = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(200)
            flagRotation.animateTo(8f, animationSpec = tween(120))
            flagRotation.animateTo(-6f, animationSpec = tween(100))
            flagRotation.animateTo(3f, animationSpec = tween(90))
            flagRotation.animateTo(0f, animationSpec = tween(80))
        }
    }

    // Region card — tappable, compact layout with edit icon
    Card(
        onClick = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = cardScale * (if (visible) cardBreathe else 1f)
                scaleX = s; scaleY = s
                translationY = cardY
                alpha = cardAlpha
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag — wobbles on arrival
            Text(
                text = selectedRegion.flag,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.graphicsLayer { rotationZ = flagRotation.value }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedRegion.countryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailChip(label = "Currency", value = selectedRegion.currencySymbol)
                    DetailChip(label = "Dates", value = selectedRegion.dateFormatPreview)
                }
            }
            // Edit icon — hints card is tappable
            ThemedIcon(
                materialIcon = Icons.Filled.Edit,
                inkIconRes = R.drawable.ic_ink_edit,
                contentDescription = "Change region",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }

    // Region picker — slides in below the card when open
    AnimatedVisibility(
        visible = showPicker,
        enter = expandVertically() + fadeIn(tween(200)),
        exit = shrinkVertically() + fadeOut(tween(200))
    ) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            var searchQuery by remember { mutableStateOf("") }
            var showCustomForm by remember { mutableStateOf(false) }

            val filteredRegions = remember(searchQuery) {
                if (searchQuery.isBlank()) popularRegions
                else popularRegions.filter { region ->
                    region.countryName.contains(searchQuery, ignoreCase = true) ||
                    region.countryCode.contains(searchQuery, ignoreCase = true) ||
                    region.currencySymbol.contains(searchQuery, ignoreCase = true)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Search field
                    ThemedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            showCustomForm = false
                        },
                        placeholder = { Text("Search countries...") },
                        leadingIcon = {
                            ThemedIcon(
                                materialIcon = Icons.Filled.Search,
                                inkIconRes = R.drawable.ic_ink_search,
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    AnimatedContent(
                        targetState = showCustomForm,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(PaperInkMotion.DurationShort))
                        },
                        label = "pickerContent"
                    ) { customFormVisible ->
                        if (customFormVisible) {
                            CustomCountryForm(
                                initialCountryName = searchQuery,
                                onConfirm = { name, symbol ->
                                    val datePreview = try {
                                        java.time.LocalDate.of(2026, 2, 19)
                                            .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
                                    } catch (_: Exception) { "19 Feb 2026" }
                                    val customRegion = RegionInfo(
                                        countryCode = "CUSTOM",
                                        countryName = name.trim(),
                                        currencySymbol = symbol.trim(),
                                        dateFormatPreview = datePreview,
                                        flag = "\uD83C\uDF10"
                                    )
                                    onSelect(customRegion)
                                }
                            )
                        } else {
                            Column {
                                LazyColumn(
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    items(filteredRegions, key = { it.countryCode }) { region ->
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
                                                    ThemedIcon(
                                                        materialIcon = Icons.Filled.Check,
                                                        inkIconRes = R.drawable.ic_ink_check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.small)
                                                .clickable { onSelect(region) }
                                        )
                                    }

                                    // "Can't find your country?" option
                                    if (searchQuery.isNotBlank() && filteredRegions.size <= 3) {
                                        item(key = "custom_country_option") {
                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        "Can't find your country?",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                supportingContent = {
                                                    Text("Set up a custom region")
                                                },
                                                leadingContent = {
                                                    ThemedIcon(
                                                        materialIcon = Icons.Filled.Language,
                                                        inkIconRes = R.drawable.ic_ink_language,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                modifier = Modifier
                                                    .clip(MaterialTheme.shapes.small)
                                                    .clickable { showCustomForm = true }
                                            )
                                        }
                                    }
                                }

                                // Also show "Can't find?" as a text button when no search query
                                if (searchQuery.isBlank()) {
                                    TextButton(
                                        onClick = { showCustomForm = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    ) {
                                        Text("Can't find your country?")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Preference Card — selectable card for user preference
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PreferenceCard(
    preference: UserPreference,
    isSelected: Boolean,
    entranceIndex: Int,
    visible: Boolean,
    onClick: () -> Unit
) {
    val icon = when (preference) {
        UserPreference.WASTE -> Icons.Filled.Eco
        UserPreference.INVENTORY -> Icons.Filled.Inventory2
        UserPreference.COOK -> Icons.Filled.Restaurant
    }

    // ── Staggered Land entrance (scale 0.5→1.0, 70ms stagger) ──
    var landed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(entranceIndex * 70L)
            landed = true
        }
    }
    val entranceScale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.5f,
        animationSpec = BouncySpring, label = "prefLand$entranceIndex"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = tween(250), label = "prefFade$entranceIndex"
    )

    // ── Selection: WobblySpring pop (spec: 1.0→1.05→1.0) + color transitions ──
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = WobblySpring, label = "prefPop$entranceIndex"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200), label = "prefBorder$entranceIndex"
    )
    // Spec: fill = primaryContainer at 0.15 blend on surface (lerp for correct opaque tint)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) lerp(surfaceColor, primaryContainerColor, 0.15f)
            else surfaceColor,
        animationSpec = tween(200), label = "prefContainer$entranceIndex"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .graphicsLayer {
                scaleX = entranceScale * selectionScale
                scaleY = entranceScale * selectionScale
                alpha = entranceAlpha
            },
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        // Spec: vertical layout — icon at top, label centered, subtitle below
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = preference.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preference.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            // Animated checkmark in top-right corner
            val checkScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = if (isSelected) WobblySpring else tween(100),
                label = "check$entranceIndex"
            )
            if (checkScale > 0.01f) {
                ThemedIcon(
                    materialIcon = Icons.Filled.Check,
                    inkIconRes = R.drawable.ic_ink_check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = checkScale; scaleY = checkScale
                            alpha = checkScale
                        }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared components
// ═══════════════════════════════════════════════════════════════════════════

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

@Composable
private fun CustomCountryForm(
    initialCountryName: String,
    onConfirm: (countryName: String, currencySymbol: String) -> Unit
) {
    var countryName by remember { mutableStateOf(initialCountryName) }
    var currencySymbol by remember { mutableStateOf("") }

    val datePreview = remember {
        try {
            java.time.LocalDate.of(2026, 2, 19)
                .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
        } catch (_: Exception) { "19 Feb 2026" }
    }

    // Fade-up entrance
    var formVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        formVisible = true
    }
    val formY by animateFloatAsState(
        targetValue = if (formVisible) 0f else 16f,
        animationSpec = GentleSpring, label = "customFormY"
    )
    val formAlpha by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(250), label = "customFormAlpha"
    )

    val isValid = countryName.isNotBlank() && currencySymbol.isNotBlank()

    Column(
        modifier = Modifier
            .graphicsLayer { translationY = formY; alpha = formAlpha }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your country isn't in our list yet — no worries! Just tell us a few basics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        ThemedTextField(
            value = countryName,
            onValueChange = { countryName = it },
            label = { Text("Country name") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ThemedTextField(
            value = currencySymbol,
            onValueChange = { currencySymbol = it },
            label = { Text("Currency symbol") },
            placeholder = { Text("e.g. kr, \u20AC, \u00A5") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // Read-only date preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Date format",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$datePreview (from device)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        ThemedButton(
            onClick = { onConfirm(countryName, currencySymbol) },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Use This", style = MaterialTheme.typography.titleSmall)
        }
    }
}


