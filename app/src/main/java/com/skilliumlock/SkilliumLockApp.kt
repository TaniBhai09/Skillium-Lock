package com.skilliumlock

import android.app.Application
import com.skilliumlock.data.db.AppDatabase

class SkilliumLockApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: SkilliumLockApp
            private set
    }
}
