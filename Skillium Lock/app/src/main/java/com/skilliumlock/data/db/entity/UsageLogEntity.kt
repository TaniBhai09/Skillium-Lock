package com.skilliumlock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val packageName: String,
    val appName: String = "",
    val date: String, // format: YYYY-MM-DD
    val durationSeconds: Int = 0
)
