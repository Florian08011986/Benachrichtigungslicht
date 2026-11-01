package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** Eine startbare App auf dem Gerät. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Liest die auf dem Gerät installierten, startbaren Apps (Launcher-Einträge) – rein lokal.
 * Sichtbarkeit ab Android 11 über <queries> (MAIN/LAUNCHER) im Manifest, ohne QUERY_ALL_PACKAGES.
 */
object InstalledApps {

    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (pkg, _) -> pkg != context.packageName }
            .distinctBy { it.first }
            .map { (pkg, label) -> InstalledApp(pkg, label) }
            .sortedBy { it.label.lowercase() }
    }

    fun icon(context: Context, packageName: String): Drawable? =
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
}
