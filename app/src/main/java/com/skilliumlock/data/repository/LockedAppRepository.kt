package com.skilliumlock.data.repository

import com.skilliumlock.data.db.dao.LockedAppDao
import com.skilliumlock.data.db.entity.LockedAppEntity

class LockedAppRepository(private val lockedAppDao: LockedAppDao) {

    suspend fun getLockedAppsForProfile(profileId: Long): List<LockedAppEntity> =
        lockedAppDao.getLockedAppsForProfile(profileId)

    suspend fun getLockedAppEntries(packageName: String): List<LockedAppEntity> =
        lockedAppDao.getLockedAppEntries(packageName)

    suspend fun getLockedApp(profileId: Long, packageName: String): LockedAppEntity? =
        lockedAppDao.getLockedApp(profileId, packageName)

    suspend fun getAllLockedPackageNames(): List<String> =
        lockedAppDao.getAllLockedPackageNames()

    suspend fun insertLockedApp(lockedApp: LockedAppEntity): Long =
        lockedAppDao.insertLockedApp(lockedApp)

    suspend fun updateLockedApp(lockedApp: LockedAppEntity) =
        lockedAppDao.updateLockedApp(lockedApp)

    suspend fun deleteLockedApp(lockedApp: LockedAppEntity) =
        lockedAppDao.deleteLockedApp(lockedApp)

    suspend fun deleteLockedApp(profileId: Long, packageName: String) =
        lockedAppDao.deleteLockedApp(profileId, packageName)

    suspend fun deleteAllForProfile(profileId: Long) =
        lockedAppDao.deleteAllForProfile(profileId)

    suspend fun isAppLocked(packageName: String): Boolean =
        lockedAppDao.getLockedAppEntries(packageName).isNotEmpty()
}
