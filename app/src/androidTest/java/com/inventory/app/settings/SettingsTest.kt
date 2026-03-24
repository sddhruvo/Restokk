package com.inventory.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsTest : BaseUiTest() {

    private fun navigateToSettings() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithText("Settings").performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }

    @Test
    fun settings_showsThemeSection() {
        navigateToSettings()
        // Theme section is near the bottom — scroll to it first
        composeTestRule.onNodeWithText("Theme", substring = true).performScrollTo()
        composeTestRule.onNodeWithText("Theme", substring = true).assertIsDisplayed()
    }

    @Test
    fun settings_showsNotificationSection() {
        navigateToSettings()
        composeTestRule.onAllNodesWithText("Notification", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun settings_showsAccountSection() {
        navigateToSettings()
        WaitUtils.waitForText(composeTestRule, "Account", substring = true).assertIsDisplayed()
    }

    @Test
    fun settings_exportImportNavigates() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        // Export / Import is on the More screen, not Settings
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        val exportNode = WaitUtils.waitForText(composeTestRule, "Export", substring = true)
        exportNode.performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        // Should navigate to Export/Import screen without crash
    }
}
