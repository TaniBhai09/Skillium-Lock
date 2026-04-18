package com.skilliumlock.ui.applock

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.skilliumlock.R
import com.skilliumlock.data.db.entity.LockedAppEntity
import com.skilliumlock.data.db.entity.ProfileEntity
import com.skilliumlock.manager.AppListManager
import com.skilliumlock.manager.ProfileManager
import com.skilliumlock.service.AppMonitorService
import kotlinx.coroutines.*

/**
 * Configure which apps are locked per profile.
 * Shows list of installed apps with lock toggles.
 */
class AppLockConfigActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profileManager: ProfileManager
    private lateinit var appListManager: AppListManager
    private lateinit var profileSpinner: Spinner
    private lateinit var appListContainer: LinearLayout
    private lateinit var loadingText: TextView

    private var profiles: List<ProfileEntity> = emptyList()
    private var selectedProfile: ProfileEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock_config)

        profileManager = ProfileManager(this)
        appListManager = AppListManager(this)
        profileSpinner = findViewById(R.id.profileSpinner)
        appListContainer = findViewById(R.id.appListContainer)
        loadingText = findViewById(R.id.loadingText)

        loadProfiles()
    }

    private fun loadProfiles() {
        scope.launch {
            profiles = withContext(Dispatchers.IO) {
                profileManager.profileRepo.getNonAdminProfiles()
            }

            if (profiles.isEmpty()) {
                Toast.makeText(this@AppLockConfigActivity, "Create profiles first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val adapter = ArrayAdapter(
                this@AppLockConfigActivity,
                android.R.layout.simple_spinner_dropdown_item,
                profiles.map { it.name }
            )
            profileSpinner.adapter = adapter

            profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedProfile = profiles[position]
                    loadApps()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadApps() {
        val profile = selectedProfile ?: return
        loadingText.visibility = View.VISIBLE
        appListContainer.removeAllViews()

        scope.launch {
            val installedApps = withContext(Dispatchers.IO) {
                appListManager.getInstalledApps()
            }
            val lockedApps = withContext(Dispatchers.IO) {
                profileManager.lockedAppRepo.getLockedAppsForProfile(profile.id)
            }
            val lockedPackages = lockedApps.map { it.packageName }.toSet()

            loadingText.visibility = View.GONE

            installedApps.forEach { appInfo ->
                val row = layoutInflater.inflate(R.layout.item_app_lock, appListContainer, false)

                val appIcon = row.findViewById<ImageView>(R.id.appIcon)
                val appName = row.findViewById<TextView>(R.id.appName)
                val lockToggle = row.findViewById<Switch>(R.id.lockToggle)

                appInfo.icon?.let { appIcon.setImageDrawable(it) }
                appName.text = appInfo.appName
                lockToggle.isChecked = appInfo.packageName in lockedPackages

                // Make the entire row focusable for D-pad
                row.isFocusable = true
                row.isFocusableInTouchMode = true
                row.setBackgroundResource(R.drawable.bg_button_focusable)

                row.setOnClickListener {
                    lockToggle.isChecked = !lockToggle.isChecked
                    toggleAppLock(profile, appInfo, lockToggle.isChecked)
                }

                row.setOnKeyListener { _, keyCode, event ->
                    if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                        (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                keyCode == android.view.KeyEvent.KEYCODE_ENTER)
                    ) {
                        lockToggle.isChecked = !lockToggle.isChecked
                        toggleAppLock(profile, appInfo, lockToggle.isChecked)
                        true
                    } else false
                }

                appListContainer.addView(row)
            }

            // Focus first app row
            if (appListContainer.childCount > 0) {
                appListContainer.getChildAt(0).requestFocus()
            }
        }
    }

    private fun toggleAppLock(profile: ProfileEntity, appInfo: AppListManager.AppInfo, isLocked: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (isLocked) {
                    profileManager.lockedAppRepo.insertLockedApp(
                        LockedAppEntity(
                            profileId = profile.id,
                            packageName = appInfo.packageName,
                            appName = appInfo.appName
                        )
                    )
                } else {
                    profileManager.lockedAppRepo.deleteLockedApp(profile.id, appInfo.packageName)
                }
            }
            // Refresh the accessibility service cache
            AppMonitorService.instance?.refreshLockedPackagesCache()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
