package com.skilliumlock.data.db.dao

import androidx.room.*
import com.skilliumlock.data.db.entity.LockedAppEntity

@Dao
interface LockedAppDao {

    @Query("SELECT * FROM locked_apps WHERE profileId = :profileId")
    suspend fun getLockedAppsForProfile(profileId: Long): List<LockedAppEntity>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName")
    suspend fun getLockedAppEntries(packageName: String): List<LockedAppEntity>

    @Query("SELECT * FROM locked_apps WHERE profileId = :profileId AND packageName = :packageName LIMIT 1")
    suspend fun getLockedApp(profileId: Long, packageName: String): LockedAppEntity?

    @Query("SELECT DISTINCT packageName FROM locked_apps")
    suspend fun getAllLockedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedApp(lockedApp: LockedAppEntity): Long

    @Update
    suspend fun updateLockedApp(lockedApp: LockedAppEntity)

    @Delete
    suspend fun deleteLockedApp(lockedApp: LockedAppEntity)

    @Query("DELETE FROM locked_apps WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Query("DELETE FROM locked_apps WHERE profileId = :profileId AND packageName = :packageName")
    suspend fun deleteLockedApp(profileId: Long, packageName: String)
}
