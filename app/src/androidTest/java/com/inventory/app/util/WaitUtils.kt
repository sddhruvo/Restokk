package com.inventory.app.util

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText

/**
 * Utility functions for waiting on Compose elements during tests.
 */
object WaitUtils {

    /**
     * Waits until a node with the given testTag exists, up to [timeoutMs].
     * Throws AssertionError if not found within timeout.
     */
    fun waitForTag(
        rule: ComposeTestRule,
        tag: String,
        timeoutMs: Long = 5000
    ): SemanticsNodeInteraction {
        rule.waitUntil(timeoutMs) {
            rule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(tag)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        return rule.onNodeWithTag(tag)
    }

    /**
     * Waits until a node with the given text exists, up to [timeoutMs].
     */
    fun waitForText(
        rule: ComposeTestRule,
        text: String,
        timeoutMs: Long = 5000,
        substring: Boolean = false
    ): SemanticsNodeInteraction {
        rule.waitUntil(timeoutMs) {
            rule.onAllNodes(
                if (substring) androidx.compose.ui.test.hasText(text, substring = true)
                else androidx.compose.ui.test.hasText(text)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        return if (substring) rule.onNodeWithText(text, substring = true)
        else rule.onNodeWithText(text)
    }

    /**
     * Waits for compose idle state + extra delay for Paper & Ink animations.
     */
    fun waitForAnimations(rule: ComposeTestRule, extraMs: Long = 800) {
        rule.waitForIdle()
        Thread.sleep(extraMs)
    }
}
