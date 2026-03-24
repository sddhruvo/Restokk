package com.inventory.app.util

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ScreenshotHelper {

    /**
     * Captures a screenshot of the activity and saves it to the app's external files directory.
     * Files can be pulled via: adb pull /storage/emulated/0/Android/data/com.restokk.app/files/screenshots/
     */
    fun capture(activity: Activity, name: String) {
        val dir = activity.getExternalFilesDir("screenshots")
        dir?.mkdirs() ?: return

        val timestamp = System.currentTimeMillis()
        val file = File(dir, "${name}_$timestamp.png")

        try {
            val bitmap = activity.window.decorView.rootView.drawToBitmap()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            // Fallback: PixelCopy for hardware-accelerated views (API 26+)
            try {
                captureWithPixelCopy(activity, file)
            } catch (_: Exception) {
                // Screenshot failed — non-fatal, test continues
            }
        }
    }

    private fun captureWithPixelCopy(activity: Activity, file: File) {
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)

        PixelCopy.request(
            activity.window,
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                bitmap.recycle()
                latch.countDown()
            },
            Handler(Looper.getMainLooper())
        )

        latch.await(5, TimeUnit.SECONDS)
    }
}
