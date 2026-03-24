package com.inventory.app.base

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.inventory.app.MainActivity
import com.inventory.app.data.local.db.InventoryDatabase
import com.inventory.app.util.ScreenshotHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base class for all UI tests. Provides:
 * - Hilt injection with in-memory Room DB (TestAppModule)
 * - Compose test rule that launches MainActivity
 * - Onboarding bypass (seeded in TestAppModule's DB callback before Activity reads it)
 * - Screenshot helper
 */
@HiltAndroidTest
abstract class BaseUiTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: InventoryDatabase

    @Before
    open fun setUp() {
        hiltRule.inject()
    }

    @After
    open fun tearDown() {
        database.close()
    }

    /** Wait for Compose to settle + extra delay for animations. */
    fun waitForIdle(extraMs: Long = 500) {
        composeTestRule.waitForIdle()
        if (extraMs > 0) Thread.sleep(extraMs)
    }

    /** Take a screenshot and save it for later retrieval via adb pull. */
    fun takeScreenshot(name: String) {
        ScreenshotHelper.capture(composeTestRule.activity, name)
    }
}
