package com.skilliumlock.data.repository

import com.skilliumlock.data.db.dao.ScreenTimeDao
import com.skilliumlock.data.db.entity.ScreenTimeEntity
import java.text.SimpleDateFormat
import java.util.*

class ScreenTimeRepository(private val screenTimeDao: ScreenTimeDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayDate(): String = dateFormat.format(Date())

    suspend fun getScreenTime(profileId: Long, packageName: String): ScreenTimeEntity? =
        screenTimeDao.getScreenTime(profileId, packageName, getTodayDate())

    suspend fun getOrCreateScreenTime(profileId: Long, packageName: String): ScreenTimeEntity {
        val today = getTodayDate()
        val existing = screenTimeDao.getScreenTime(profileId, packageName, today)
        if (existing != null) return existing

        val entity = ScreenTimeEntity(
            profileId = profileId,
            packageName = packageName,
            date = today
        )
        val id = screenTimeDao.insertScreenTime(entity)
        return entity.copy(id = id)
    }

    suspend fun updateUsedSeconds(profileId: Long, packageName: String, usedSeconds: Int) {
        val today = getTodayDate()
        val existing = screenTimeDao.getScreenTime(profileId, packageName, today)
        if (existing != null) {
            screenTimeDao.updateUsedSeconds(profileId, packageName, today, usedSeconds)
        } else {
            screenTimeDao.insertScreenTime(
                ScreenTimeEntity(
                    profileId = profileId,
                    packageName = packageName,
                    date = today,
                    usedSeconds = usedSeconds
                )
            )
        }
    }

    suspend fun getRemainingSeconds(profileId: Long, packageName: String, timeLimitMinutes: Int): Int {
        val screenTime = getScreenTime(profileId, packageName)
        val usedSeconds = screenTime?.usedSeconds ?: 0
        val limitSeconds = timeLimitMinutes * 60
        return maxOf(0, limitSeconds - usedSeconds)
    }

    suspend fun isTimeLimitReached(profileId: Long, packageName: String, timeLimitMinutes: Int): Boolean {
        return getRemainingSeconds(profileId, packageName, timeLimitMinutes) <= 0
    }

    suspend fun getScreenTimeForProfileToday(profileId: Long): List<ScreenTimeEntity> =
        screenTimeDao.getScreenTimeForProfileToday(profileId, getTodayDate())

    suspend fun deleteOldEntries(daysToKeep: Int = 30) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        screenTimeDao.deleteOldEntries(dateFormat.format(calendar.time))
    }

    suspend fun deleteAllForProfile(profileId: Long) =
        screenTimeDao.deleteAllForProfile(profileId)
}
