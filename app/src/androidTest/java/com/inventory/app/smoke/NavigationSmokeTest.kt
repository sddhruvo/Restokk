package com.inventory.app.smoke

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
class NavigationSmokeTest : BaseUiTest() {

    @Test
    fun navigateToAllBottomNavTabs() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Start on Dashboard (Home tab)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).assertIsDisplayed()

        // Navigate to Cook tab
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_COOK).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        // Cook Hub should show some content (at least the screen rendered without crash)

        // Navigate to Shopping tab
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_SHOPPING).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        // Navigate to More tab
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        // More screen should show Settings or other items
        composeTestRule.onNodeWithText("Settings", substring = true).assertIsDisplayed()

        // Navigate back to Home
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_HOME).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
    }

    @Test
    fun quickAddFabOpensMenu() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Tap FAB
        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        // Menu items should be visible
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_SCAN_BARCODE).assertIsDisplayed()

        // Dismiss by tapping scrim
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.SCRIM).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
    }

    @Test
    fun moreScreen_showsAllMenuCards() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        // Verify key items on the More screen
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reports", substring = true).assertIsDisplayed()
    }
}
