package com.inventory.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    firstMagicViewModel: FirstMagicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Back button: previous page on Screen 2, exits app on Screen 1
    // On Screen 3 (FirstMagic), back is handled internally by PathChoicePage
    BackHandler(enabled = !state.isFirstPage && state.currentPageIndex != 2) {
        viewModel.previousPage()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = state.currentPageIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(300)))
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut(tween(300)))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            label = "onboardingPages"
        ) { pageIndex ->
            OnboardingPageContent(
                page = onboardingPages[pageIndex],
                state = state,
                onRegionChange = { viewModel.toggleRegionPicker() },
                onRegionSelect = { viewModel.selectRegion(it) },
                onPreferenceSelect = { viewModel.setPreference(it) },
                onGetStarted = { viewModel.nextPage() },
                onSaveAndContinue = { viewModel.saveSettingsAndContinue() },
                firstMagicContent = {
                    PathChoicePage(
                        viewModel = firstMagicViewModel,
                        onComplete = { viewModel.completeOnboarding(onComplete) },
                        onBackToYourKitchen = { viewModel.previousPage() }
                    )
                }
            )
        }
    }
}
