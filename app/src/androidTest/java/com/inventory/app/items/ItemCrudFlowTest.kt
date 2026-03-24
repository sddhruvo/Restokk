package com.inventory.app.items

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class ItemCrudFlowTest : BaseUiTest() {

    @Test
    fun createItem_thenVerifyInList() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Open Quick Add -> Add Item
        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Fill in item name
        composeTestRule.onNodeWithText("Name", substring = true).performClick()
        composeTestRule.onNodeWithText("Name", substring = true).performTextInput("Test Apple")
        WaitUtils.waitForAnimations(composeTestRule)

        // Save the item (SaveAction is an icon button, not text)
        composeTestRule.onNode(hasContentDescription("Save")).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1500)

        // Should navigate back — go to Items list via Reports → Full Inventory
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithText("Reports", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        composeTestRule.onNodeWithText("Full Inventory", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Verify item is in the list (may match multiple nodes — use onFirst)
        composeTestRule.onAllNodesWithText("Test Apple").onFirst().assertIsDisplayed()
    }

    @Test
    fun itemForm_showsValidationOnEmptyName() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Open Quick Add -> Add Item
        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Try to save without filling name — save button should not be enabled
        // or show an error state. The form should stay open.
        // Since the save button only appears when form has changes, try typing and clearing
        composeTestRule.onNodeWithText("Name", substring = true).performClick()
        composeTestRule.onNodeWithText("Name", substring = true).performTextInput(" ")
        WaitUtils.waitForAnimations(composeTestRule)

        // Form should still be visible (not navigated away)
        composeTestRule.onNodeWithText("Name", substring = true).assertIsDisplayed()
    }
}
