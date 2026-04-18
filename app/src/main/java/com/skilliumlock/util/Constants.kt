package com.skilliumlock.util

object Constants {
    // SharedPreferences keys
    const val PREFS_NAME = "skillium_lock_prefs"
    const val KEY_SETUP_COMPLETE = "setup_complete"
    const val KEY_UNINSTALL_PROTECTION = "uninstall_protection"

    // Password
    const val PASSWORD_LENGTH = 4
    const val MAX_SHIFT = 9

    // Screen time
    const val SCREEN_TIME_UPDATE_INTERVAL_MS = 30_000L // Update DB every 30 seconds
    const val SCREEN_TIME_TICK_INTERVAL_MS = 1_000L // Tick every 1 second

    // Overlay
    const val OVERLAY_ANIMATION_DURATION = 300L

    // Packages to intercept for uninstall protection
    val PROTECTED_PACKAGES = listOf(
        "com.android.settings",
        "com.android.tv.settings",
        "com.google.android.tv.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    // Our own package (exclude from monitoring)
    const val OWN_PACKAGE = "com.skilliumlock"

    // Launcher packages to ignore
    val LAUNCHER_PACKAGES = listOf(
        "com.google.android.tvlauncher",
        "com.google.android.leanbacklauncher",
        "com.android.launcher",
        "com.android.launcher3"
    )
}
