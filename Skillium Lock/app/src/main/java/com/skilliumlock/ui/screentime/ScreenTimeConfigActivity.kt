package com.skilliumlock.ui.screentime

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.skilliumlock.R
import com.skilliumlock.data.db.entity.LockedAppEntity
import com.skilliumlock.data.db.entity.ProfileEntity
import com.skilliumlock.manager.ProfileManager
import kotlinx.coroutines.*

/**
 * Configure screen time limits per locked app per profile.
 * Only shows apps that are already locked for the selected profile.
 */
class ScreenTimeConfigActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profileManager: ProfileManager
    private lateinit var profileSpinner: Spinner
    private lateinit var appListContainer: LinearLayout
    private lateinit var emptyText: TextView

    private var profiles: List<ProfileEntity> = emptyList()
    private var selectedProfile: ProfileEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_time_config)

        profileManager = ProfileManager(this)
        profileSpinner = findViewById(R.id.profileSpinner)
        appListContainer = findViewById(R.id.appListContainer)
        emptyText = findViewById(R.id.emptyText)

        loadProfiles()
    }

    private fun loadProfiles() {
        scope.launch {
            profiles = withContext(Dispatchers.IO) {
                profileManager.profileRepo.getNonAdminProfiles()
            }

            if (profiles.isEmpty()) {
                Toast.makeText(this@ScreenTimeConfigActivity, "Create profiles first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val adapter = ArrayAdapter(
                this@ScreenTimeConfigActivity,
                android.R.layout.simple_spinner_dropdown_item,
                profiles.map { it.name }
            )
            profileSpinner.adapter = adapter

            profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedProfile = profiles[position]
                    loadLockedApps()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadLockedApps() {
        val profile = selectedProfile ?: return
        appListContainer.removeAllViews()

        scope.launch {
            val lockedApps = withContext(Dispatchers.IO) {
                profileManager.lockedAppRepo.getLockedAppsForProfile(profile.id)
            }

            if (lockedApps.isEmpty()) {
                emptyText.visibility = View.VISIBLE
                emptyText.text = getString(R.string.no_apps_locked)
            } else {
                emptyText.visibility = View.GONE
                lockedApps.forEach { lockedApp -> addScreenTimeRow(lockedApp) }
            }
        }
    }

    private fun addScreenTimeRow(lockedApp: LockedAppEntity) {
        val row = layoutInflater.inflate(R.layout.item_screen_time, appListContainer, false)

        val appName = row.findViewById<TextView>(R.id.appName)
        val screenTimeToggle = row.findViewById<Switch>(R.id.screenTimeToggle)
        val timeLimitText = row.findViewById<TextView>(R.id.timeLimitText)
        val decreaseButton = row.findViewById<Button>(R.id.decreaseButton)
        val increaseButton = row.findViewById<Button>(R.id.increaseButton)
        val timeControlsContainer = row.findViewById<LinearLayout>(R.id.timeControlsContainer)

        appName.text = lockedApp.appName
        screenTimeToggle.isChecked = lockedApp.screenTimeEnabled
        timeLimitText.text = getString(R.string.minutes_format, lockedApp.timeLimitMinutes)
        timeControlsContainer.visibility = if (lockedApp.screenTimeEnabled) View.VISIBLE else View.GONE

        // Make row focusable
        row.isFocusable = true
        row.isFocusableInTouchMode = true
        row.setBackgroundResource(R.drawable.bg_button_focusable)

        screenTimeToggle.setOnCheckedChangeListener { _, isChecked ->
            timeControlsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateLockedApp(lockedApp.copy(screenTimeEnabled = isChecked))
        }

        row.setOnClickListener {
            screenTimeToggle.isChecked = !screenTimeToggle.isChecked
        }

        var currentLimit = lockedApp.timeLimitMinutes

        decreaseButton.setOnClickListener {
            if (currentLimit > 5) {
                currentLimit -= 5
                timeLimitText.text = getString(R.string.minutes_format, currentLimit)
                updateLockedApp(lockedApp.copy(timeLimitMinutes = currentLimit))
            }
        }

        increaseButton.setOnClickListener {
            if (currentLimit < 480) { // max 8 hours
                currentLimit += 5
                timeLimitText.text = getString(R.string.minutes_format, currentLimit)
                updateLockedApp(lockedApp.copy(timeLimitMinutes = currentLimit))
            }
        }

        appListContainer.addView(row)
    }

    private fun updateLockedApp(lockedApp: LockedAppEntity) {
        scope.launch {
            withContext(Dispatchers.IO) {
                profileManager.lockedAppRepo.updateLockedApp(lockedApp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
