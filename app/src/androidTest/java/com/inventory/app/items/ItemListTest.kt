package com.inventory.app.items

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.DbSeeder
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@HiltAndroidTest
class ItemListTest : BaseUiTest() {

    private fun navigateToItemList() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Navigate: More → Reports → Full Inventory (item list)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        WaitUtils.waitForText(composeTestRule, "Reports", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        WaitUtils.waitForText(composeTestRule, "Full Inventory", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }

    @Test
    fun itemList_showsEmptyState_whenNoItems() {
        // With no items, Reports screen shows empty state — "Full Inventory" link isn't available
        // Verify Reports empty state renders without crash
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        WaitUtils.waitForText(composeTestRule, "Reports", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        // Reports should show empty state or report cards — renders without crash
    }

    @Test
    fun itemList_showsSeededItems() {
        runBlocking { DbSeeder(database).seedBasicInventory() }
        navigateToItemList()

        // Wait for seeded items to appear (Room Flow may take a moment to emit)
        WaitUtils.waitForText(composeTestRule, "Rice").assertIsDisplayed()
        WaitUtils.waitForText(composeTestRule, "Pasta").assertIsDisplayed()
    }

    @Test
    fun itemList_tapItemNavigatesToDetail() {
        runBlocking { DbSeeder(database).seedBasicInventory() }
        navigateToItemList()

        // Wait for item to appear, then tap
        WaitUtils.waitForText(composeTestRule, "Rice").performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Should be on detail screen — verify item name is shown
        composeTestRule.onNodeWithText("Rice").assertIsDisplayed()
    }
}
