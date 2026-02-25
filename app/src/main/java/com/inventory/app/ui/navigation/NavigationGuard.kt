package com.inventory.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf

/**
 * NavigationGuardState — holds a single active guard that BottomNavBar checks
 * before navigating. Screens register/clear guards via [RegisterNavigationGuard].
 */
class NavigationGuardState {
    private var activeGuard: (() -> Pair<Boolean, String>)? = null

    fun setGuard(guard: () -> Pair<Boolean, String>) {
        activeGuard = guard
    }

    fun clearGuard() {
        activeGuard = null
    }

    /**
     * Returns the guard message if navigation should be blocked, null if allowed.
     */
    fun shouldBlock(): String? {
        val guard = activeGuard ?: return null
        val (blocked, message) = guard()
        return if (blocked) message else null
    }
}

val LocalNavigationGuard = compositionLocalOf { NavigationGuardState() }

/**
 * Register a navigation guard on the current screen.
 * When this composable is in the composition, bottom nav taps will check
 * [shouldBlock] before navigating, and show a discard dialog with [message].
 *
 * Both parameters are lambdas so they're evaluated at check time, not registration time.
 */
@Composable
fun RegisterNavigationGuard(
    shouldBlock: () -> Boolean,
    message: () -> String
) {
    val guardState = LocalNavigationGuard.current
    DisposableEffect(Unit) {
        guardState.setGuard { Pair(shouldBlock(), message()) }
        onDispose { guardState.clearGuard() }
    }
}
