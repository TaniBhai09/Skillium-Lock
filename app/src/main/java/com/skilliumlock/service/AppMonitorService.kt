package com.skilliumlock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.skilliumlock.data.db.AppDatabase
import com.skilliumlock.data.db.entity.LockedAppEntity
import com.skilliumlock.data.repository.LockedAppRepository
import com.skilliumlock.util.Constants
import com.skilliumlock.util.PrefsHelper
import kotlinx.coroutines.*

/**
 * Core Accessibility Service that monitors foreground app changes.
 * When a locked app comes to foreground, it triggers the lock overlay.
 *
 * Also intercepts Settings/Package Installer for uninstall protection.
 */
class AppMonitorService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var lockedAppRepo: LockedAppRepository
    private lateinit var overlayManager: LockOverlayManager
    private lateinit var screenTimeTracker: ScreenTimeTracker

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cachedLockedPackages: Set<String> = emptySet()
    private var lastForegroundPackage: String = ""
    private var lastOverlayPackage: String = ""
    private var unlockedPackages: MutableMap<String, Long> = mutableMapOf() // package -> unlock timestamp
    private var currentUnlockedProfile: Long = -1L

    companion object {
        var instance: AppMonitorService? = null
            private set
        private const val UNLOCK_TIMEOUT_MS = 2000L // Grace period after unlock before re-locking
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val db = AppDatabase.getInstance(this)
        lockedAppRepo = LockedAppRepository(db.lockedAppDao())

        screenTimeTracker = ScreenTimeTracker(this)
        screenTimeTracker.onTimeLimitReached = { profileId, packageName ->
            // Time limit reached - go home and show overlay
            performGlobalAction(GLOBAL_ACTION_HOME)
            // The next window state change will trigger the lock again
        }

        overlayManager = LockOverlayManager(
            context = this,
            windowManager = windowManager,
            onAppUnlocked = { profileId, packageName, lockedApp ->
                onAppUnlocked(profileId, packageName, lockedApp)
            },
            onGoHome = {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        )

        // Load locked packages cache
        refreshLockedPackagesCache()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                handleWindowChange(packageName)
            }
        }
    }

    private fun handleWindowChange(packageName: String) {
        // Skip our own package
        if (packageName == Constants.OWN_PACKAGE) return

        // Skip system UI
        if (packageName == "com.android.systemui" || packageName == "android") return

        // Skip launchers
        if (packageName in Constants.LAUNCHER_PACKAGES) {
            // User went home - stop any active tracking
            if (screenTimeTracker.isTracking()) {
                screenTimeTracker.stopTracking()
            }
            // Clear unlocked state when going home
            unlockedPackages.clear()
            lastOverlayPackage = ""
            overlayManager.dismissAll()
            return
        }

        // Check uninstall protection
        if (PrefsHelper.isUninstallProtectionEnabled(this) &&
            packageName in Constants.PROTECTED_PACKAGES
        ) {
            if (!overlayManager.isShowing()) {
                overlayManager.showProfileSelect(packageName)
                lastOverlayPackage = packageName
            }
            return
        }

        // Same package as before - skip
        if (packageName == lastForegroundPackage) return
        lastForegroundPackage = packageName

        // Check if this package is recently unlocked (grace period)
        val unlockTime = unlockedPackages[packageName]
        if (unlockTime != null && System.currentTimeMillis() - unlockTime < UNLOCK_TIMEOUT_MS) {
            return
        }

        // Check if screen time is being tracked for this package
        if (screenTimeTracker.isTracking() && screenTimeTracker.getCurrentPackage() == packageName) {
            return // Already tracking, don't re-lock
        }

        // Stop tracking if switched to different app
        if (screenTimeTracker.isTracking() && screenTimeTracker.getCurrentPackage() != packageName) {
            screenTimeTracker.stopTracking()
        }

        // Check if package is locked
        if (packageName in cachedLockedPackages) {
            // Clear any previous unlock state for different package
            unlockedPackages.remove(packageName)

            if (!overlayManager.isShowing()) {
                overlayManager.showProfileSelect(packageName)
                lastOverlayPackage = packageName
            }
        }
    }

    private fun onAppUnlocked(profileId: Long, packageName: String, lockedApp: LockedAppEntity) {
        currentUnlockedProfile = profileId
        unlockedPackages[packageName] = System.currentTimeMillis()

        // Start screen time tracking if enabled
        if (lockedApp.screenTimeEnabled) {
            screenTimeTracker.startTracking(profileId, packageName, lockedApp.timeLimitMinutes)
        }
    }

    /**
     * Refresh the cached set of locked package names.
     * Called when lock configuration changes.
     */
    fun refreshLockedPackagesCache() {
        scope.launch {
            try {
                cachedLockedPackages = lockedAppRepo.getAllLockedPackageNames().toSet()
            } catch (e: Exception) {
                // Database not ready yet
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        overlayManager.destroy()
        screenTimeTracker.destroy()
        scope.cancel()
    }
}
