package com.skilliumlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/**
 * Receives BOOT_COMPLETED broadcast to ensure the accessibility service
 * is re-enabled after device restarts.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if accessibility service is still enabled
            if (!isAccessibilityServiceEnabled(context)) {
                // Show a notification or toast to remind user
                Toast.makeText(
                    context,
                    "Skillium Lock: Please re-enable accessibility service",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceName = "${context.packageName}/${AppMonitorService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(serviceName)
    }
}
