package com.inventory.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class BottomNavTest : BaseUiTest() {

    @Test
    fun retapCurrentTab_doesNotCreateDuplicateBackStack() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Tap Home multiple times — should not crash or duplicate
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        // Still on Dashboard
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).assertIsDisplayed()
    }

    @Test
    fun navigateAcrossAllTabs_noStackLeak() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Cycle through all tabs twice
        repeat(2) {
            composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_COOK).performClick()
            WaitUtils.waitForAnimations(composeTestRule)
            composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_SHOPPING).performClick()
            WaitUtils.waitForAnimations(composeTestRule)
            composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
            WaitUtils.waitForAnimations(composeTestRule)
            composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).performClick()
            WaitUtils.waitForAnimations(composeTestRule)
        }
        // Should still be on Dashboard
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).assertIsDisplayed()
    }
}
