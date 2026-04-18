package com.skilliumlock.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

/**
 * Discovers installed apps suitable for locking.
 * Filters out system-critical apps and our own app.
 */
class AppListManager(private val context: Context) {

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable?
    )

    /**
     * Get all launchable (user-facing) apps installed on the device.
     * Excludes our own app and core system services.
     */
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val leanbackApps = pm.queryIntentActivities(intent, 0)

        // Also get standard launcher apps (some TV apps only register LAUNCHER category)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = pm.queryIntentActivities(launcherIntent, 0)

        val allApps = mutableMapOf<String, ResolveInfo>()
        (leanbackApps + launcherApps).forEach { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (!allApps.containsKey(pkg)) {
                allApps[pkg] = resolveInfo
            }
        }

        return allApps.values
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    appName = resolveInfo.loadLabel(pm).toString(),
                    icon = try {
                        resolveInfo.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Get app name for a package.
     */
    fun getAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Get app icon for a package.
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
