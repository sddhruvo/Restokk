package com.inventory.app.cook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inventory.app.base.BaseUiTest
import com.inventory.app.ui.TestTags
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class CookHubTest : BaseUiTest() {

    private fun navigateToCookHub() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)
        composeTestRule.onNodeWithTag(TestTags.BottomNav.TAB_COOK).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
    }

    @Test
    fun cookHub_displaysWithoutCrash() {
        navigateToCookHub()
        // Cook Hub should render — at minimum we should see some content
    }

    @Test
    fun cookHub_showsActionCards() {
        navigateToCookHub()
        // Look for cook-related action text
        composeTestRule.onNodeWithText("What Can I Cook", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookHub_savedRecipesAccessible() {
        navigateToCookHub()
        // "My Recipes" text only shows when recipes exist; use the icon button in top bar
        val recipesIcon = composeTestRule.onNode(hasContentDescription("My Recipes"))
        recipesIcon.assertIsDisplayed()
        recipesIcon.performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1000)
        // Should navigate to saved recipes — empty state expected
    }
}
