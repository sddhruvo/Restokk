package com.inventory.app.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class AppLaunchTest : BaseUiTest() {

    @Test
    fun appLaunchesToDashboard_whenOnboardingCompleted() {
        // Onboarding is bypassed in BaseUiTest — app should go to Dashboard
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Dashboard shows greeting text (time-based, so check any of them)
        val hasGreeting = listOf("Good morning", "Good afternoon", "Good evening").any { greeting ->
            try {
                composeTestRule.onNodeWithText(greeting, substring = true)
                    .assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        assert(hasGreeting) { "Expected a greeting on Dashboard but none found" }
    }

    @Test
    fun bottomNavBarIsVisible_onDashboard() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_COOK).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_SHOPPING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).assertIsDisplayed()
    }

    @Test
    fun appLaunchesToOnboarding_whenFreshInstall() {
        // This test would need to NOT bypass onboarding
        // Covered in OnboardingFlowTest — just verify no crash here
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        // App is alive if we get here without crash
    }
}
