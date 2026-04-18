package com.skilliumlock.ui.main

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skilliumlock.R
import com.skilliumlock.admin.SkilliumDeviceAdmin
import com.skilliumlock.manager.ProfileManager
import com.skilliumlock.service.AppMonitorService
import com.skilliumlock.ui.analytics.AnalyticsActivity
import com.skilliumlock.ui.applock.AppLockConfigActivity
import com.skilliumlock.ui.profile.ProfileListActivity
import com.skilliumlock.ui.screentime.ScreenTimeConfigActivity
import com.skilliumlock.ui.settings.SettingsActivity
import com.skilliumlock.ui.setup.SetupActivity
import com.skilliumlock.util.PrefsHelper
import kotlinx.coroutines.*

/**
 * Main admin dashboard. Requires admin password to access.
 * Provides navigation to all configuration screens.
 */
class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profileManager: ProfileManager
    private var isAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if setup is complete
        if (!PrefsHelper.isSetupComplete(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        profileManager = ProfileManager(this)

        // Show login screen first
        showAdminLogin()
    }

    private fun showAdminLogin() {
        setContentView(R.layout.activity_admin_login)

        val passwordInput = findViewById<EditText>(R.id.adminPasswordInput)
        val loginButton = findViewById<Button>(R.id.adminLoginButton)

        loginButton.setOnClickListener { verifyAdminPassword(passwordInput) }

        // Handle enter key on password field
        passwordInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                verifyAdminPassword(passwordInput)
                true
            } else false
        }

        passwordInput.requestFocus()
    }

    private fun verifyAdminPassword(passwordInput: EditText) {
        val password = passwordInput.text.toString().trim()
        if (password.length != 4) {
            Toast.makeText(this, "Enter 4-digit PIN", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val isValid = withContext(Dispatchers.IO) {
                profileManager.verifyAdminPassword(password)
            }

            if (isValid) {
                isAuthenticated = true
                showDashboard()
            } else {
                Toast.makeText(this@MainActivity, "Wrong PIN", Toast.LENGTH_SHORT).show()
                passwordInput.text.clear()
                passwordInput.requestFocus()
            }
        }
    }

    private fun showDashboard() {
        setContentView(R.layout.activity_main)

        val btnProfiles = findViewById<Button>(R.id.btnProfiles)
        val btnAppLock = findViewById<Button>(R.id.btnAppLock)
        val btnScreenTime = findViewById<Button>(R.id.btnScreenTime)
        val btnAnalytics = findViewById<Button>(R.id.btnAnalytics)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnProfiles.setOnClickListener {
            startActivity(Intent(this, ProfileListActivity::class.java))
        }

        btnAppLock.setOnClickListener {
            startActivity(Intent(this, AppLockConfigActivity::class.java))
        }

        btnScreenTime.setOnClickListener {
            startActivity(Intent(this, ScreenTimeConfigActivity::class.java))
        }

        btnAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Focus first button
        btnProfiles.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        // Refresh locked packages cache
        AppMonitorService.instance?.refreshLockedPackagesCache()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
