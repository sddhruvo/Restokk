package com.inventory.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.inventory.app.MainActivity
import com.inventory.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val stepIndex: Int,
    val stepLabel: String
)

/** Foreground service that keeps cooking timers alive when the app is backgrounded.
 *  Bound by CookingPlaybackViewModel. Survives ViewModel death (config change, backgrounding).
 *  Stops itself when all timers complete or cancelAll() is called. */
class CookingTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "cooking_timers"
        const val NOTIFICATION_ID = 400
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val timerJobs = java.util.concurrent.ConcurrentHashMap<Int, Job>()

    private val _timerStates = MutableStateFlow<Map<Int, TimerState>>(emptyMap())
    val timerStates: StateFlow<Map<Int, TimerState>> = _timerStates.asStateFlow()

    private var recipeId: Long = 0L
    private var recipeName: String = ""

    inner class CookingTimerBinder : Binder() {
        fun getService(): CookingTimerService = this@CookingTimerService
    }

    private val binder = CookingTimerBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        timerJobs.clear()
        super.onDestroy()
    }

    // ── Public API ─────────────────────────────────────────────────────────

    fun setRecipeContext(id: Long, name: String) {
        recipeId = id
        recipeName = name
        updateNotification()
    }

    fun startTimer(stepIndex: Int, totalSeconds: Int, stepLabel: String) {
        // Cancel any existing timer for this step
        timerJobs[stepIndex]?.cancel()

        // Resume from paused position if available, otherwise start fresh
        val existingState = _timerStates.value[stepIndex]
        val resumeFrom = if (existingState != null && !existingState.isRunning
            && existingState.remainingSeconds > 0
            && existingState.remainingSeconds < totalSeconds
        ) existingState.remainingSeconds else totalSeconds

        timerJobs[stepIndex] = scope.launch {
            var remaining = resumeFrom
            while (remaining > 0 && isActive) {
                _timerStates.update { map ->
                    map + (stepIndex to TimerState(totalSeconds, remaining, true, stepIndex, stepLabel))
                }
                updateNotification()
                delay(1000)
                remaining--
            }
            // Timer reached zero — mark complete
            _timerStates.update { map ->
                map + (stepIndex to TimerState(totalSeconds, 0, false, stepIndex, stepLabel))
            }
            timerJobs.remove(stepIndex)
            updateNotification()
            if (!hasActiveTimers()) stopSelf()
        }
    }

    fun pauseTimer(stepIndex: Int) {
        timerJobs[stepIndex]?.cancel()
        timerJobs.remove(stepIndex)
        _timerStates.update { map ->
            val current = map[stepIndex] ?: return@update map
            map + (stepIndex to current.copy(isRunning = false))
        }
        updateNotification()
    }

    fun cancelTimer(stepIndex: Int) {
        timerJobs[stepIndex]?.cancel()
        timerJobs.remove(stepIndex)
        _timerStates.update { map -> map - stepIndex }
        updateNotification()
        if (!hasActiveTimers()) stopSelf()
    }

    fun cancelAll() {
        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        _timerStates.value = emptyMap()
        stopSelf()
    }

    fun hasActiveTimers(): Boolean = timerJobs.any { it.value.isActive }

    // ── Notification helpers ───────────────────────────────────────────────

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Cooking Timers", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "Live countdown timers while cooking" }
                )
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val activeStates = _timerStates.value.values.filter { it.isRunning }
        val contentText = when {
            activeStates.isEmpty() -> "All timers complete"
            else -> {
                val urgent = activeStates.minByOrNull { it.remainingSeconds }!!
                val mins = urgent.remainingSeconds / 60
                val secs = urgent.remainingSeconds % 60
                "Step ${urgent.stepIndex + 1}: %d:%02d remaining".format(mins, secs)
            }
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SmartNotificationWorker.EXTRA_NAV_ROUTE, "cooking-playback/$recipeId")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (recipeName.isNotBlank()) "Cooking: $recipeName" else "Cooking Timer")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIFICATION_ID, buildNotification())
    }
}
