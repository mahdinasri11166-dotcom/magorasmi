package com.ghostmago.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import java.io.File

/**
 * Harder anti-root / anti-tamper checks. Optional block mode via AppState.
 */
object Attestation {
    data class Report(
        val rooted: Boolean,
        val debugger: Boolean,
        val emulator: Boolean,
        val debuggable: Boolean,
        val issues: List<String>
    ) {
        val risky get() = rooted || debugger || (emulator && issues.isNotEmpty())
    }

    fun scan(context: Context): Report {
        val issues = mutableListOf<String>()
        val rooted = AntiForensics.isRooted().also { if (it) issues.add("root/su") }
        val debugger = AntiForensics.isDebuggerAttached().also { if (it) issues.add("debugger") }
        val emulator = isEmulator().also { if (it) issues.add("emulator") }
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) issues.add("debuggable")
        // Magisk-ish paths
        listOf("/sbin/.magisk", "/data/adb/magisk", "/system/xbin/daemonsu").forEach {
            if (File(it).exists()) { issues.add("magisk-path"); }
        }
        return Report(rooted, debugger, emulator, debuggable, issues.distinct())
    }

    private fun isEmulator(): Boolean {
        val f = Build.FINGERPRINT
        return f.startsWith("generic") || f.contains("emulator") ||
            Build.MODEL.contains("Emulator") || Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk") || Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
    }
}
