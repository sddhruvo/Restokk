package com.inventory.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun requestUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SmallWidget().updateAll(context)
                MediumWidget().updateAll(context)
            } catch (_: Exception) {
                // Widget may not be placed — ignore
            }
        }
    }
}
