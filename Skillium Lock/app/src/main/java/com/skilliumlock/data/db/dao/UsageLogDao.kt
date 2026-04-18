package com.skilliumlock.data.db.dao

import androidx.room.*
import com.skilliumlock.data.db.entity.UsageLogEntity

@Dao
interface UsageLogDao {

    @Query("SELECT * FROM usage_logs WHERE profileId = :profileId AND date = :date")
    suspend fun getLogsForProfileOnDate(profileId: Long, date: String): List<UsageLogEntity>

    @Query("SELECT * FROM usage_logs WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getLogsForDateRange(profileId: Long, startDate: String, endDate: String): List<UsageLogEntity>

    @Query("SELECT * FROM usage_logs WHERE profileId = :profileId AND packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getLog(profileId: Long, packageName: String, date: String): UsageLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UsageLogEntity): Long

    @Update
    suspend fun updateLog(log: UsageLogEntity)

    @Query("UPDATE usage_logs SET durationSeconds = durationSeconds + :additionalSeconds WHERE profileId = :profileId AND packageName = :packageName AND date = :date")
    suspend fun addDuration(profileId: Long, packageName: String, date: String, additionalSeconds: Int)

    @Query("DELETE FROM usage_logs WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Query("DELETE FROM usage_logs WHERE date < :date")
    suspend fun deleteOldLogs(date: String)
}
