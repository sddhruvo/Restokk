package com.inventory.app.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
class GlobalSearchTest : BaseUiTest() {

    @Test
    fun globalSearch_opensFromDashboard() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Tap the search icon on the Dashboard
        val searchIcon = composeTestRule.onNode(hasContentDescription("Search"))
        searchIcon.assertIsDisplayed()
        searchIcon.performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        // Search screen should open without crash
    }

    @Test
    fun globalSearch_findsSeededItems() {
        runBlocking { DbSeeder(database).seedBasicInventory() }
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Open search from Dashboard via the search icon
        composeTestRule.onNode(hasContentDescription("Search")).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Type a search query into the search field (target the TextField, not the header)
        composeTestRule.onAllNodesWithText("Search", substring = true).onLast()
            .performTextInput("Rice")
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Should find the seeded "Rice" item (may match input field + result, use onFirst for result)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Rice").fetchSemanticsNodes().size >= 2
        }
        composeTestRule.onAllNodesWithText("Rice").onFirst().assertIsDisplayed()
    }
}
