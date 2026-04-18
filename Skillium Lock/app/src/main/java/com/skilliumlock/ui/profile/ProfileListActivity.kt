package com.skilliumlock.ui.profile

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.skilliumlock.R
import com.skilliumlock.data.db.entity.ProfileEntity
import com.skilliumlock.manager.ProfileManager
import kotlinx.coroutines.*

/**
 * List and manage user profiles. Admin can add, edit, and delete profiles.
 */
class ProfileListActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profileManager: ProfileManager
    private lateinit var profileContainer: LinearLayout
    private lateinit var addButton: Button
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_list)

        profileManager = ProfileManager(this)
        profileContainer = findViewById(R.id.profileContainer)
        addButton = findViewById(R.id.addProfileButton)
        emptyText = findViewById(R.id.emptyText)

        addButton.setOnClickListener { showAddDialog() }

        loadProfiles()
    }

    private fun loadProfiles() {
        scope.launch {
            val profiles = withContext(Dispatchers.IO) {
                profileManager.profileRepo.getNonAdminProfiles()
            }

            profileContainer.removeAllViews()

            if (profiles.isEmpty()) {
                emptyText.visibility = View.VISIBLE
            } else {
                emptyText.visibility = View.GONE
                profiles.forEach { profile -> addProfileRow(profile) }
            }

            addButton.requestFocus()
        }
    }

    private fun addProfileRow(profile: ProfileEntity) {
        val row = layoutInflater.inflate(R.layout.item_profile, profileContainer, false)

        val nameText = row.findViewById<TextView>(R.id.profileName)
        val editButton = row.findViewById<Button>(R.id.editProfileButton)
        val deleteButton = row.findViewById<Button>(R.id.deleteProfileButton)

        nameText.text = profile.name

        editButton.setOnClickListener { showEditDialog(profile) }
        deleteButton.setOnClickListener { showDeleteConfirmation(profile) }

        profileContainer.addView(row)
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_edit, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.dialogPasswordInput)

        val dialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.add_profile))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = nameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (name.isNotEmpty() && password.length == 4 && password.all { it.isDigit() }) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            profileManager.createProfile(name, password)
                        }
                        loadProfiles()
                        Toast.makeText(this@ProfileListActivity, "Profile created", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Enter name and 4-digit PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()
        nameInput.requestFocus()
    }

    private fun showEditDialog(profile: ProfileEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_edit, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.dialogPasswordInput)

        nameInput.setText(profile.name)
        passwordInput.setText(profile.password)

        val dialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.edit_profile))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = nameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (name.isNotEmpty() && password.length == 4 && password.all { it.isDigit() }) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            profileManager.updateProfileName(profile.id, name)
                            profileManager.updatePassword(profile.id, password)
                        }
                        loadProfiles()
                        Toast.makeText(this@ProfileListActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()
        nameInput.requestFocus()
    }

    private fun showDeleteConfirmation(profile: ProfileEntity) {
        AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.delete_profile))
            .setMessage("${getString(R.string.confirm_delete)}\n\nProfile: ${profile.name}")
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        profileManager.deleteProfileCascade(profile)
                    }
                    loadProfiles()
                    Toast.makeText(this@ProfileListActivity, "Profile deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .create()
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
