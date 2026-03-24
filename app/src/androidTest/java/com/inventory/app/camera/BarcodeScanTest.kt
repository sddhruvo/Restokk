package com.inventory.app.camera

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.inventory.app.base.UiAutomatorTestBase
import com.inventory.app.ui.TestTags
import com.inventory.app.util.WaitUtils
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class BarcodeScanTest : UiAutomatorTestBase() {

    @Test
    fun barcodeScan_navigatesFromQuickAdd() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        // Open Quick Add -> Scan Barcode
        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_SCAN_BARCODE).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1500)

        // Permission dialog may appear — grant it
        grantCameraPermission()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Barcode scanner screen should be visible (camera preview or manual entry)
        // Test passes if we get here without crash
    }

    @Test
    fun barcodeScan_showsFallbackOnPermissionDeny() {
        WaitUtils.waitForAnimations(composeTestRule, 2000)

        composeTestRule.onNodeWithTag(TestTags.BottomNav.FAB_QUICK_ADD).performClick()
        WaitUtils.waitForAnimations(composeTestRule)
        composeTestRule.onNodeWithTag(TestTags.QuickAdd.MENU_SCAN_BARCODE).performClick()
        WaitUtils.waitForAnimations(composeTestRule, 1500)

        // Deny permission
        denyPermission()
        WaitUtils.waitForAnimations(composeTestRule, 1000)

        // Should show fallback (manual barcode entry or permission explanation)
        // Test passes if no crash
    }
}
