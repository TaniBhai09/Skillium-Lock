package com.skilliumlock.data.repository

import com.skilliumlock.data.db.dao.UsageLogDao
import com.skilliumlock.data.db.entity.UsageLogEntity
import java.text.SimpleDateFormat
import java.util.*

class UsageLogRepository(private val usageLogDao: UsageLogDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayDate(): String = dateFormat.format(Date())

    suspend fun logUsage(profileId: Long, packageName: String, appName: String, durationSeconds: Int) {
        val today = getTodayDate()
        val existing = usageLogDao.getLog(profileId, packageName, today)
        if (existing != null) {
            usageLogDao.updateLog(existing.copy(
                durationSeconds = existing.durationSeconds + durationSeconds,
                appName = appName
            ))
        } else {
            usageLogDao.insertLog(
                UsageLogEntity(
                    profileId = profileId,
                    packageName = packageName,
                    appName = appName,
                    date = today,
                    durationSeconds = durationSeconds
                )
            )
        }
    }

    suspend fun getLogsForToday(profileId: Long): List<UsageLogEntity> =
        usageLogDao.getLogsForProfileOnDate(profileId, getTodayDate())

    suspend fun getLogsForDateRange(profileId: Long, startDate: String, endDate: String): List<UsageLogEntity> =
        usageLogDao.getLogsForDateRange(profileId, startDate, endDate)

    suspend fun getLogsForLastNDays(profileId: Long, days: Int): List<UsageLogEntity> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val startDate = dateFormat.format(calendar.time)
        return usageLogDao.getLogsForDateRange(profileId, startDate, endDate)
    }

    suspend fun deleteAllForProfile(profileId: Long) =
        usageLogDao.deleteAllForProfile(profileId)

    suspend fun deleteOldLogs(daysToKeep: Int = 90) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        usageLogDao.deleteOldLogs(dateFormat.format(calendar.time))
    }
}
