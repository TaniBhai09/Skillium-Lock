package com.skilliumlock.util

import android.content.Context
import android.content.SharedPreferences

object PrefsHelper {

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun isSetupComplete(context: Context): Boolean =
        getPrefs(context).getBoolean(Constants.KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(context: Context, complete: Boolean) {
        getPrefs(context).edit().putBoolean(Constants.KEY_SETUP_COMPLETE, complete).apply()
    }

    fun isUninstallProtectionEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(Constants.KEY_UNINSTALL_PROTECTION, true)

    fun setUninstallProtection(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(Constants.KEY_UNINSTALL_PROTECTION, enabled).apply()
    }
}
