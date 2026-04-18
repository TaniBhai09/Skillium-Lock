package com.skilliumlock.data.db.dao

import androidx.room.*
import com.skilliumlock.data.db.entity.ProfileEntity

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isAdmin = 0 ORDER BY name ASC")
    suspend fun getNonAdminProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isAdmin = 1 LIMIT 1")
    suspend fun getAdminProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM profiles WHERE isAdmin = 1")
    suspend fun getAdminCount(): Int
}
