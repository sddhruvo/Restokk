package com.inventory.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventory.app.worker.TimerState

/**
 * Floating mini-bar showing active timers from steps OTHER than the current step.
 * Visible when any other step has a running timer. Consumes pointer events.
 */
@Composable
fun RecipeTimerBar(
    timers: Map<Int, TimerState>,
    currentStepIndex: Int,
    onTimerTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show timers from other steps that are running
    val otherTimers = remember(timers, currentStepIndex) {
        timers.entries
            .filter { (idx, ts) -> idx != currentStepIndex && ts.isRunning }
            .sortedBy { it.value.remainingSeconds }
    }
    val visible = otherTimers.isNotEmpty()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .pointerInput(Unit) { detectTapGestures { /* consume */ } },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shown = otherTimers.take(3)
            val overflow = otherTimers.size - shown.size

            shown.forEach { (idx, ts) ->
                val mins = ts.remainingSeconds / 60
                val secs = ts.remainingSeconds % 60
                FilterChip(
                    selected = true,
                    onClick = { onTimerTap(idx) },
                    label = {
                        Text(
                            text = "Step ${idx + 1}: %d:%02d".format(mins, secs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            if (overflow > 0) {
                FilterChip(
                    selected = false,
                    onClick = { /* expand — future */ },
                    label = {
                        Text(
                            text = "+$overflow more",
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }
    }
}
