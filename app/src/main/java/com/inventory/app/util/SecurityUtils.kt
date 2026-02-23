package com.inventory.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.inventory.app.BuildConfig
import java.io.File
import java.security.MessageDigest

/**
 * Lightweight security checks — APK signature verification and root detection.
 * All checks fail open (catch all exceptions, never block the app).
 */
object SecurityUtils {

    private const val TAG = "SecurityUtils"

    // Known signing certificate SHA-256 fingerprints (uppercase, colon-separated)
    private const val DEBUG_SHA256 =
        "4B:2F:31:43:1E:0B:3F:30:DB:74:DD:94:D5:A8:30:56:CF:1D:15:E0:62:C3:CB:D3:03:CB:1E:EB:86:38:4F:8A"
    private const val RELEASE_SHA256 =
        "47:D1:64:3B:59:61:94:5C:54:F5:E8:FB:E8:B8:EC:55:8D:32:B3:0D:28:84:7B:75:C2:AC:9E:F7:6E:FA:65:3D"

    /**
     * Verifies the APK signing certificate matches known debug or release fingerprints.
     * Shows a Toast warning if mismatch (does NOT block the app).
     */
    fun verifyApkSignature(context: Context) {
        try {
            val currentHash = getSigningCertHash(context) ?: return
            if (currentHash != DEBUG_SHA256 && currentHash != RELEASE_SHA256) {
                if (BuildConfig.DEBUG) Log.w(TAG, "APK signature mismatch: $currentHash")
                Toast.makeText(
                    context,
                    "Warning: This app may have been modified by a third party.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {
            // Fail open — never crash
        }
    }

    /**
     * Checks for common root indicators. Returns true if device appears rooted.
     */
    fun isDeviceRooted(): Boolean {
        return try {
            checkSuBinaries() || checkSuCommand()
        } catch (_: Exception) {
            false
        }
    }

    // ── Internal helpers ────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun getSigningCertHash(context: Context): String? {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo ?: return null
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    .signatures
            }

            val sig = signatures?.firstOrNull() ?: return null
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sig.toByteArray())
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun checkSuBinaries(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readLine()
            process.destroy()
            !result.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
