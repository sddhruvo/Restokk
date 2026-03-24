package com.inventory.app.base

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.inventory.app.util.PermissionGranter
import dagger.hilt.android.testing.HiltAndroidTest

/**
 * Base class for tests requiring system-level interactions via UI Automator.
 * Use this for: permission dialogs, notification testing, cross-app flows.
 */
@HiltAndroidTest
abstract class UiAutomatorTestBase : BaseUiTest() {

    protected val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    fun grantCameraPermission() {
        PermissionGranter.grantCameraPermission(device)
    }

    fun grantNotificationPermission() {
        PermissionGranter.grantNotificationPermission(device)
    }

    fun denyPermission() {
        PermissionGranter.denyPermission(device)
    }
}
