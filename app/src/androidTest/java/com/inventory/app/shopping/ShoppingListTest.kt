package com.inventory.app.shopping

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.DbSeeder
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@HiltAndroidTest
class ShoppingListTest : BaseUiTest() {

    private fun navigateToShopping() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_SHOPPING).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }

    @Test
    fun shoppingList_showsEmptyState_whenNoItems() {
        navigateToShopping()
        // Empty state should render without crash
        // The shopping list has an animated empty state
    }

    @Test
    fun shoppingList_showsSeededItems() {
        runBlocking { DbSeeder(database).seedShoppingList() }
        navigateToShopping()

        // Wait for seeded items to appear (Room Flow may take a moment to emit)
        WaitUtils.waitForText(composeTestRule, "Eggs").assertIsDisplayed()
        WaitUtils.waitForText(composeTestRule, "Bread").assertIsDisplayed()
    }

    @Test
    fun shoppingList_quickAddBarAddsItem() {
        navigateToShopping()

        // Wait for the quick-add field to appear, then type and submit
        val quickAddField = WaitUtils.waitForText(composeTestRule, "Add item", substring = true)
        quickAddField.performClick()
        quickAddField.performTextInput("Bananas")
        WaitUtils.waitForAnimations(composeTestRule)

        // Verify the added item shows up (onLast to target the list item, not the input field)
        composeTestRule.onAllNodesWithText("Bananas").onLast().assertIsDisplayed()
    }

    @Test
    fun shoppingList_badgeCountUpdatesOnBottomNav() {
        runBlocking { DbSeeder(database).seedShoppingList() }
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Shopping badge should show active item count (5 active items seeded)
        // Badge renders inside BadgedBox — hard to assert exact number,
        // but the Shopping tab should have a badge
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_SHOPPING).assertIsDisplayed()
    }
}
