package com.inventory.app.reports

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
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class ReportsHubTest : BaseUiTest() {

    @Before
    override fun setUp() {
        super.setUp()
        runBlocking { DbSeeder(database).seedBasicInventory() }
    }

    private fun navigateToReports() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_MORE).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithText("Reports", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }

    @Test
    fun reportsHub_showsAllReportTypes() {
        navigateToReports()
        composeTestRule.onNodeWithText("Expiring", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Low Stock", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Spending", substring = true).assertIsDisplayed()
    }

    @Test
    fun reportsHub_expiringReportOpens() {
        navigateToReports()
        composeTestRule.onNodeWithText("Expiring", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        // Should open expiring report screen without crash
    }

    @Test
    fun reportsHub_lowStockReportOpens() {
        navigateToReports()
        composeTestRule.onNodeWithText("Low Stock", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }

    @Test
    fun reportsHub_spendingReportOpens() {
        navigateToReports()
        composeTestRule.onNodeWithText("Spending", substring = true).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }
}
