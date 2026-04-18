package com.skilliumlock.data.db.dao

import androidx.room.*
import com.skilliumlock.data.db.entity.ScreenTimeEntity

@Dao
interface ScreenTimeDao {

    @Query("SELECT * FROM screen_time WHERE profileId = :profileId AND packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getScreenTime(profileId: Long, packageName: String, date: String): ScreenTimeEntity?

    @Query("SELECT * FROM screen_time WHERE profileId = :profileId AND date = :date")
    suspend fun getScreenTimeForProfileToday(profileId: Long, date: String): List<ScreenTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenTime(screenTime: ScreenTimeEntity): Long

    @Update
    suspend fun updateScreenTime(screenTime: ScreenTimeEntity)

    @Query("UPDATE screen_time SET usedSeconds = :usedSeconds WHERE profileId = :profileId AND packageName = :packageName AND date = :date")
    suspend fun updateUsedSeconds(profileId: Long, packageName: String, date: String, usedSeconds: Int)

    @Query("DELETE FROM screen_time WHERE date < :date")
    suspend fun deleteOldEntries(date: String)

    @Query("DELETE FROM screen_time WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)
}
