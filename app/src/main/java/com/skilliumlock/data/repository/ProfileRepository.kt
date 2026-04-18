package com.skilliumlock.data.repository

import com.skilliumlock.data.db.dao.ProfileDao
import com.skilliumlock.data.db.entity.ProfileEntity

class ProfileRepository(private val profileDao: ProfileDao) {

    suspend fun getAllProfiles(): List<ProfileEntity> = profileDao.getAllProfiles()

    suspend fun getNonAdminProfiles(): List<ProfileEntity> = profileDao.getNonAdminProfiles()

    suspend fun getAdminProfile(): ProfileEntity? = profileDao.getAdminProfile()

    suspend fun getProfileById(id: Long): ProfileEntity? = profileDao.getProfileById(id)

    suspend fun insertProfile(profile: ProfileEntity): Long = profileDao.insertProfile(profile)

    suspend fun updateProfile(profile: ProfileEntity) = profileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: ProfileEntity) = profileDao.deleteProfile(profile)

    suspend fun isAdminSetup(): Boolean = profileDao.getAdminCount() > 0
}
