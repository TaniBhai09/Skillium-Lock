package com.skilliumlock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val packageName: String,
    val appName: String,
    val screenTimeEnabled: Boolean = false,
    val timeLimitMinutes: Int = 30 // default 30 min
)
