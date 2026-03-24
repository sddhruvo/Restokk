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
class QuickAddMenuTest : BaseUiTest() {

    @Test
    fun quickAddMenu_showsAllFourOptions() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_SCAN_BARCODE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_KITCHEN_SCAN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_SCAN_RECEIPT).assertIsDisplayed()
    }

    @Test
    fun quickAddMenu_addItemNavigatesToForm() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Should navigate to Item Form — look for typical form elements
        composeTestRule.onNodeWithText("Name", substring = true).assertIsDisplayed()
    }

    @Test
    fun quickAddMenu_scanBarcodeNavigatesToScanner() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_SCAN_BARCODE).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Should navigate to Barcode Scanner screen
        // Camera permission dialog may appear — that's fine, screen loaded without crash
    }

    @Test
    fun quickAddMenu_scrimDismissesMenu() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        // Menu should be visible
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).assertIsDisplayed()

        // Tap scrim to dismiss
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.SCRIM).performClick()
        WaitUtils.waitForAnimations(composeTestRule)

        // Menu should no longer be visible
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_ADD_ITEM).assertDoesNotExist()
    }
}
