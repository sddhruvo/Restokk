package com.inventory.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inventory.app.ui.components.InkBloomDot
import com.inventory.app.ui.components.InkDotState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (postRoute: String?) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.pageCount })
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    // Sync pager ↔ ViewModel + haptic on page change
    LaunchedEffect(state.currentPageIndex) {
        if (pagerState.currentPage != state.currentPageIndex) {
            pagerState.animateScrollToPage(state.currentPageIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.goToPage(pagerState.currentPage)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Back button: previous page or exit on Welcome
    BackHandler(enabled = !state.isFirstPage) {
        scope.launch { pagerState.animateScrollToPage(state.currentPageIndex - 1) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Top bar: skip button
            if (state.canSkip) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            viewModel.skipOnboarding {
                                onComplete(null)
                            }
                        }
                    ) {
                        Text("Skip")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = true
            ) { pageIndex ->
                OnboardingPageContent(
                    page = onboardingPages[pageIndex],
                    state = state,
                    onRegionConfirm = { viewModel.confirmRegionAndAdvance() },
                    onRegionChange = { viewModel.toggleRegionPicker() },
                    onRegionSelect = { viewModel.selectRegion(it) },
                    onStartChoiceSelect = { viewModel.setStartChoice(it) },
                    onGetStarted = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onComplete = {
                        viewModel.completeOnboarding { choice ->
                            val postRoute = when (choice) {
                                StartChoice.SCAN_KITCHEN -> "fridge-scan"
                                StartChoice.ADD_MANUALLY -> "items/form"
                                null -> null
                            }
                            onComplete(postRoute)
                        }
                    }
                )
            }

            // Bottom: page indicator + nav buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ink bloom dot indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    onboardingPages.forEachIndexed { index, _ ->
                        val dotState = when {
                            index < state.currentPageIndex -> InkDotState.COMPLETED
                            index == state.currentPageIndex -> InkDotState.ACTIVE
                            else -> InkDotState.PENDING
                        }
                        InkBloomDot(
                            state = dotState,
                            color = primaryColor
                        )
                    }
                }

                // Nav buttons (Back / Next) — not shown on Welcome or AllSet
                if (!state.isFirstPage && !state.isLastPage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(state.currentPageIndex - 1) }
                            }
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(state.currentPageIndex + 1) }
                            }
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}
