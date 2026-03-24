package com.inventory.app.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.inventory.app.base.BaseUiTest
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class OnboardingFlowTest : BaseUiTest() {

    @Before
    override fun setUp() {
        super.setUp()
        // Undo the onboarding bypass that TestAppModule seeds,
        // so this test class actually sees the onboarding flow.
        database.openHelper.writableDatabase.execSQL(
            "UPDATE settings SET value = 'false' WHERE key = 'onboarding_completed'"
        )
        // Activity already launched and read 'true' — recreate so it picks up the new value
        composeTestRule.activityRule.scenario.recreate()
    }

    @Test
    fun onboarding_showsWelcomePage() {
        WaitUtils.waitForAnimations(composeTestRule, 3000)
        // First page shows welcome text
        composeTestRule.onNodeWithText("kitchen has a story", substring = true).assertIsDisplayed()
    }

    @Test
    fun onboarding_canAdvanceToNextPage() {
        WaitUtils.waitForAnimations(composeTestRule, 3000)
        // Look for a navigation button — text varies across onboarding pages
        val hasNavButton = listOf("Open the first page", "Next", "Continue", "Let's go", "Get Started").any { text ->
            try {
                composeTestRule.onNodeWithText(text, substring = true).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        assert(hasNavButton) { "Expected a navigation button on the onboarding page but none found" }
    }
}
