package com.skilliumlock.ui.setup

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.skilliumlock.R
import com.skilliumlock.manager.ProfileManager
import com.skilliumlock.ui.main.MainActivity
import com.skilliumlock.util.PrefsHelper
import com.skilliumlock.util.UsageStatsHelper
import kotlinx.coroutines.*

/**
 * First-launch setup screen. Creates the admin profile and guides
 * the user through enabling required permissions.
 */
class SetupActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profileManager: ProfileManager

    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var createButton: Button
    private lateinit var statusText: TextView

    private var setupStep = 0 // 0: create admin, 1: accessibility, 2: usage stats, 3: done

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        profileManager = ProfileManager(this)

        nameInput = findViewById(R.id.nameInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmInput = findViewById(R.id.confirmInput)
        createButton = findViewById(R.id.createButton)
        statusText = findViewById(R.id.statusText)

        createButton.setOnClickListener { handleCreate() }

        // Check if admin already exists
        scope.launch {
            val isSetup = withContext(Dispatchers.IO) {
                profileManager.isAdminSetup()
            }
            if (isSetup) {
                showPermissionStep()
            }
        }
    }

    private fun handleCreate() {
        when (setupStep) {
            0 -> createAdmin()
            1 -> openAccessibilitySettings()
            2 -> openUsageStatsSettings()
            3 -> finishSetup()
        }
    }

    private fun createAdmin() {
        val name = nameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirm = confirmInput.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter admin name", Toast.LENGTH_SHORT).show()
            nameInput.requestFocus()
            return
        }

        if (password.length != 4 || !password.all { it.isDigit() }) {
            Toast.makeText(this, getString(R.string.setup_password_length), Toast.LENGTH_SHORT).show()
            passwordInput.requestFocus()
            return
        }

        if (password != confirm) {
            Toast.makeText(this, getString(R.string.setup_password_mismatch), Toast.LENGTH_SHORT).show()
            confirmInput.requestFocus()
            return
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                profileManager.createAdmin(name, password)
            }
            Toast.makeText(this@SetupActivity, "Admin account created!", Toast.LENGTH_SHORT).show()
            showPermissionStep()
        }
    }

    private fun showPermissionStep() {
        setupStep = 1

        nameInput.visibility = EditText.GONE
        passwordInput.visibility = EditText.GONE
        confirmInput.visibility = EditText.GONE

        statusText.text = getString(R.string.permission_accessibility_desc)
        createButton.text = "Enable Accessibility Service"
        createButton.requestFocus()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Please enable accessibility manually in Settings", Toast.LENGTH_LONG).show()
        }
        setupStep = 2
        statusText.text = getString(R.string.permission_usage_desc)
        createButton.text = "Enable Usage Access"
    }

    private fun openUsageStatsSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Please enable usage access manually in Settings", Toast.LENGTH_LONG).show()
        }
        setupStep = 3
        statusText.text = "Setup complete! You can now start using Skillium Lock."
        createButton.text = "Finish Setup"
    }

    private fun finishSetup() {
        PrefsHelper.setSetupComplete(this, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Update button text based on current permissions
        when (setupStep) {
            1 -> {
                if (isAccessibilityEnabled()) {
                    setupStep = 2
                    statusText.text = getString(R.string.permission_usage_desc)
                    createButton.text = "Enable Usage Access"
                }
            }
            2 -> {
                if (UsageStatsHelper.hasUsageStatsPermission(this)) {
                    setupStep = 3
                    statusText.text = "All permissions granted! Tap to finish setup."
                    createButton.text = "Finish Setup"
                }
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val serviceName = "$packageName/com.skilliumlock.service.AppMonitorService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
