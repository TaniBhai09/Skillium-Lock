package com.skilliumlock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Admin Receiver for uninstall protection.
 * Makes it harder (but not impossible) for users to deactivate admin
 * and uninstall the app. Combined with Accessibility-based Settings
 * interception, this provides strong uninstall protection.
 */
class SkilliumDeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Skillium Lock: Device admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // This message is shown when user tries to deactivate admin
        return "WARNING: Disabling admin will remove uninstall protection for Skillium Lock. " +
                "This action requires the admin password."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Skillium Lock: Device admin disabled", Toast.LENGTH_SHORT).show()
    }
}
