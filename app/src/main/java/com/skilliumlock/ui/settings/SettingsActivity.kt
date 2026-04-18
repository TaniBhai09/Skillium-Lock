package com.skilliumlock.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skilliumlock.R
import com.skilliumlock.admin.SkilliumDeviceAdmin
import com.skilliumlock.util.PrefsHelper
import com.skilliumlock.util.UsageStatsHelper

/**
 * Settings screen for managing permissions and app configuration.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var accessibilityStatus: TextView
    private lateinit var usageAccessStatus: TextView
    private lateinit var deviceAdminStatus: TextView
    private lateinit var uninstallSwitch: Switch
    private lateinit var btnAccessibility: Button
    private lateinit var btnUsageAccess: Button
    private lateinit var btnDeviceAdmin: Button
    private lateinit var versionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        accessibilityStatus = findViewById(R.id.accessibilityStatus)
        usageAccessStatus = findViewById(R.id.usageAccessStatus)
        deviceAdminStatus = findViewById(R.id.deviceAdminStatus)
        uninstallSwitch = findViewById(R.id.uninstallProtectionSwitch)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnUsageAccess = findViewById(R.id.btnUsageAccess)
        btnDeviceAdmin = findViewById(R.id.btnDeviceAdmin)
        versionText = findViewById(R.id.versionText)

        // Uninstall protection toggle
        uninstallSwitch.isChecked = PrefsHelper.isUninstallProtectionEnabled(this)
        uninstallSwitch.setOnCheckedChangeListener { _, isChecked ->
            PrefsHelper.setUninstallProtection(this, isChecked)
        }

        // Accessibility Service
        btnAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Open Settings > Accessibility manually", Toast.LENGTH_LONG).show()
            }
        }

        // Usage Access
        btnUsageAccess.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Open Settings > Usage Access manually", Toast.LENGTH_LONG).show()
            }
        }

        // Device Admin
        btnDeviceAdmin.setOnClickListener {
            val component = ComponentName(this, SkilliumDeviceAdmin::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_description)
                )
            }
            startActivity(intent)
        }

        // Version
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = getString(R.string.version_format, pInfo.versionName)
        } catch (e: Exception) {
            versionText.text = getString(R.string.version_format, "1.0")
        }

        btnAccessibility.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        updateStatuses()
    }

    private fun updateStatuses() {
        // Accessibility
        val accEnabled = isAccessibilityEnabled()
        accessibilityStatus.text = if (accEnabled) getString(R.string.enabled) else getString(R.string.disabled)
        accessibilityStatus.setTextColor(
            getColor(if (accEnabled) R.color.success else R.color.error)
        )

        // Usage Access
        val usageEnabled = UsageStatsHelper.hasUsageStatsPermission(this)
        usageAccessStatus.text = if (usageEnabled) getString(R.string.enabled) else getString(R.string.disabled)
        usageAccessStatus.setTextColor(
            getColor(if (usageEnabled) R.color.success else R.color.error)
        )

        // Device Admin
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, SkilliumDeviceAdmin::class.java)
        val adminEnabled = dpm.isAdminActive(adminComponent)
        deviceAdminStatus.text = if (adminEnabled) getString(R.string.enabled) else getString(R.string.disabled)
        deviceAdminStatus.setTextColor(
            getColor(if (adminEnabled) R.color.success else R.color.error)
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val serviceName = "$packageName/com.skilliumlock.service.AppMonitorService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }
}
