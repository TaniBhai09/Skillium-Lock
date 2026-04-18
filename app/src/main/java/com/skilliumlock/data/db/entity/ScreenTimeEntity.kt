package com.skilliumlock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time")
data class ScreenTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val packageName: String,
    val date: String, // format: YYYY-MM-DD
    val usedSeconds: Int = 0
)
