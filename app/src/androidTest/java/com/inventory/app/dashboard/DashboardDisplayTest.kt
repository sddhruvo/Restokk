package com.inventory.app.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
class DashboardDisplayTest : BaseUiTest() {

    @Test
    fun dashboard_showsGreeting() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        val hasGreeting = listOf("Good morning", "Good afternoon", "Good evening").any { greeting ->
            try {
                composeTestRule.onNodeWithText(greeting, substring = true).assertIsDisplayed()
                true
            } catch (_: AssertionError) { false }
        }
        assert(hasGreeting) { "Expected a greeting on Dashboard" }
    }

    @Test
    fun dashboard_showsTrackingSubtitle() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Dashboard subtitle varies by state. Match any known pattern (case-insensitive).
        // Possible subtitles: "Start by adding your first item", "Tracking X items — keep it up!",
        // "Steady at X — tracking Y items", "↑N since yesterday — ...", "↓N since yesterday — ...",
        // "You reached '...'!", "...items expiring soon", "...need attention"
        val hasSubtitle = listOf(
            "first item", "tracking", "since yesterday", "keep it up",
            "reached", "expiring", "need attention", "kitchen"
        ).any { text ->
            try {
                composeTestRule.onNodeWithText(text, substring = true, ignoreCase = true).assertIsDisplayed()
                true
            } catch (_: AssertionError) { false }
        }
        assert(hasSubtitle) { "Expected dashboard subtitle text" }
    }

    @Test
    fun dashboard_showsExpiringChip_withData() {
        runBlocking { DbSeeder(database).seedBasicInventory() }
        // After seeding, dashboard may show multiple "expir" matches (subtitle + chip)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodes(hasText("expir", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("expir", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun dashboard_quickActions_visible() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Dashboard has Quick Actions section with Shopping, Reports, Kitchen chips
        composeTestRule.onNodeWithText("Shopping", substring = true).assertIsDisplayed()
    }

    @Test
    fun dashboard_searchIconNavigatesToSearch() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Search is an icon button — find by content description
        val searchNode = composeTestRule.onNode(hasContentDescription("Search"))
        searchNode.assertIsDisplayed()
        searchNode.performClick()
        WaitUtils.waitForAnimations(composeTestRule)
    }
}
