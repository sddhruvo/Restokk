package com.inventory.app.util

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

/**
 * Handles system permission dialogs via UI Automator.
 * Finds and clicks "Allow" / "While using the app" / "Don't allow" buttons.
 */
object PermissionGranter {

    private const val WAIT_TIMEOUT = 3000L

    fun grantCameraPermission(device: UiDevice) {
        // Android 11+ shows "While using the app" for camera
        val whileUsing = device.findObject(
            UiSelector().textMatches("(?i)while using (the )?app")
        )
        if (whileUsing.waitForExists(WAIT_TIMEOUT)) {
            whileUsing.click()
            return
        }
        // Fallback to "Allow"
        val allow = device.findObject(
            UiSelector().textMatches("(?i)allow")
        )
        if (allow.waitForExists(WAIT_TIMEOUT)) {
            allow.click()
        }
    }

    fun grantNotificationPermission(device: UiDevice) {
        val allow = device.findObject(
            UiSelector().textMatches("(?i)allow")
        )
        if (allow.waitForExists(WAIT_TIMEOUT)) {
            allow.click()
        }
    }

    fun denyPermission(device: UiDevice) {
        val deny = device.findObject(
            UiSelector().textMatches("(?i)(don.t allow|deny)")
        )
        if (deny.waitForExists(WAIT_TIMEOUT)) {
            deny.click()
        }
    }
}
