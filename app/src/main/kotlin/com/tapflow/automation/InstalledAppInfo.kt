package com.tapflow.automation

import android.content.Context
import android.content.Intent

/**
 * Metadata for an installed application capable of being launched.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String
) {
    companion object {
        fun queryLauncherApps(context: Context): List<InstalledAppInfo> {
            return try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val activities = pm.queryIntentActivities(intent, 0)
                activities.map { resolveInfo ->
                    InstalledAppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        appName = resolveInfo.loadLabel(pm).toString()
                    )
                }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
