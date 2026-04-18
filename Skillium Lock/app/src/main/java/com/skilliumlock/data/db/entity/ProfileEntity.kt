package com.skilliumlock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val password: String, // 4-digit PIN stored as string
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
