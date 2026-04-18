package com.skilliumlock.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.skilliumlock.data.db.AppDatabase
import com.skilliumlock.data.repository.ScreenTimeRepository
import com.skilliumlock.data.repository.UsageLogRepository
import com.skilliumlock.manager.AppListManager
import com.skilliumlock.util.Constants
import kotlinx.coroutines.*

/**
 * Tracks screen time per app per profile.
 * Runs a 1-second ticker while a locked app is in foreground.
 * Updates the database every 30 seconds.
 * When the time limit is reached, triggers a callback to lock the app.
 */
class ScreenTimeTracker(context: Context) {

    private val screenTimeRepo: ScreenTimeRepository
    private val usageLogRepo: UsageLogRepository
    private val appListManager: AppListManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var trackingJob: Job? = null
    private var currentProfileId: Long = -1
    private var currentPackageName: String = ""
    private var currentTimeLimitMinutes: Int = 0
    private var elapsedSecondsThisSession: Int = 0
    private var totalUsedSeconds: Int = 0

    var onTimeLimitReached: ((profileId: Long, packageName: String) -> Unit)? = null

    init {
        val db = AppDatabase.getInstance(context)
        screenTimeRepo = ScreenTimeRepository(db.screenTimeDao())
        usageLogRepo = UsageLogRepository(db.usageLogDao())
        appListManager = AppListManager(context)
    }

    /**
     * Start tracking screen time for a specific profile and app.
     */
    fun startTracking(profileId: Long, packageName: String, timeLimitMinutes: Int) {
        // Stop any existing tracking
        stopTracking()

        currentProfileId = profileId
        currentPackageName = packageName
        currentTimeLimitMinutes = timeLimitMinutes
        elapsedSecondsThisSession = 0

        trackingJob = scope.launch {
            // Get existing usage for today
            val screenTime = screenTimeRepo.getOrCreateScreenTime(profileId, packageName)
            totalUsedSeconds = screenTime.usedSeconds

            // Check if already exceeded
            val limitSeconds = timeLimitMinutes * 60
            if (totalUsedSeconds >= limitSeconds) {
                withContext(Dispatchers.Main) {
                    onTimeLimitReached?.invoke(profileId, packageName)
                }
                return@launch
            }

            // Start ticking
            while (isActive) {
                delay(Constants.SCREEN_TIME_TICK_INTERVAL_MS)
                elapsedSecondsThisSession++
                totalUsedSeconds++

                // Save to DB every 30 seconds
                if (elapsedSecondsThisSession % 30 == 0) {
                    saveProgress()
                }

                // Check limit
                if (totalUsedSeconds >= limitSeconds) {
                    saveProgress()
                    withContext(Dispatchers.Main) {
                        onTimeLimitReached?.invoke(profileId, packageName)
                    }
                    break
                }
            }
        }
    }

    /**
     * Get remaining seconds for display purposes.
     */
    fun getRemainingSeconds(): Int {
        val limitSeconds = currentTimeLimitMinutes * 60
        return maxOf(0, limitSeconds - totalUsedSeconds)
    }

    /**
     * Get remaining minutes for toast display.
     */
    fun getRemainingMinutes(): Int {
        return (getRemainingSeconds() + 59) / 60 // Round up
    }

    /**
     * Stop tracking and save final progress.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null

        if (currentProfileId != -1L && currentPackageName.isNotEmpty() && elapsedSecondsThisSession > 0) {
            scope.launch {
                saveProgress()
                // Log usage for analytics
                val appName = appListManager.getAppName(currentPackageName)
                usageLogRepo.logUsage(
                    currentProfileId,
                    currentPackageName,
                    appName,
                    elapsedSecondsThisSession
                )
            }
        }

        elapsedSecondsThisSession = 0
    }

    private suspend fun saveProgress() {
        if (currentProfileId != -1L && currentPackageName.isNotEmpty()) {
            screenTimeRepo.updateUsedSeconds(
                currentProfileId,
                currentPackageName,
                totalUsedSeconds
            )
        }
    }

    /**
     * Check if currently tracking.
     */
    fun isTracking(): Boolean = trackingJob?.isActive == true

    /**
     * Get the currently tracked package.
     */
    fun getCurrentPackage(): String = currentPackageName

    /**
     * Get the currently tracked profile.
     */
    fun getCurrentProfileId(): Long = currentProfileId

    /**
     * Cleanup resources.
     */
    fun destroy() {
        stopTracking()
        scope.cancel()
    }
}
