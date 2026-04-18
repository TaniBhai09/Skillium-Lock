package com.skilliumlock.manager

import android.content.Context
import com.skilliumlock.data.db.AppDatabase
import com.skilliumlock.data.db.entity.ProfileEntity
import com.skilliumlock.data.repository.*

/**
 * Central manager for all profile-related operations.
 * Handles creation, deletion, and cascading cleanup of profile data.
 */
class ProfileManager(context: Context) {

    private val db = AppDatabase.getInstance(context)
    val profileRepo = ProfileRepository(db.profileDao())
    val lockedAppRepo = LockedAppRepository(db.lockedAppDao())
    val screenTimeRepo = ScreenTimeRepository(db.screenTimeDao())
    val usageLogRepo = UsageLogRepository(db.usageLogDao())

    suspend fun createAdmin(name: String, password: String): Long {
        val profile = ProfileEntity(
            name = name,
            password = password,
            isAdmin = true
        )
        return profileRepo.insertProfile(profile)
    }

    suspend fun createProfile(name: String, password: String): Long {
        val profile = ProfileEntity(
            name = name,
            password = password,
            isAdmin = false
        )
        return profileRepo.insertProfile(profile)
    }

    suspend fun deleteProfileCascade(profile: ProfileEntity) {
        lockedAppRepo.deleteAllForProfile(profile.id)
        screenTimeRepo.deleteAllForProfile(profile.id)
        usageLogRepo.deleteAllForProfile(profile.id)
        profileRepo.deleteProfile(profile)
    }

    suspend fun updatePassword(profileId: Long, newPassword: String) {
        val profile = profileRepo.getProfileById(profileId) ?: return
        profileRepo.updateProfile(profile.copy(password = newPassword))
    }

    suspend fun updateProfileName(profileId: Long, newName: String) {
        val profile = profileRepo.getProfileById(profileId) ?: return
        profileRepo.updateProfile(profile.copy(name = newName))
    }

    suspend fun verifyAdminPassword(password: String): Boolean {
        val admin = profileRepo.getAdminProfile() ?: return false
        return admin.password == password
    }

    suspend fun isAdminSetup(): Boolean = profileRepo.isAdminSetup()
}
