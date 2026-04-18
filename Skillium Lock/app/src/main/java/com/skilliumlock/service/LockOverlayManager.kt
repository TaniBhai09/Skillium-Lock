package com.skilliumlock.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.skilliumlock.R
import com.skilliumlock.data.db.AppDatabase
import com.skilliumlock.data.db.entity.LockedAppEntity
import com.skilliumlock.data.db.entity.ProfileEntity
import com.skilliumlock.data.repository.LockedAppRepository
import com.skilliumlock.data.repository.ProfileRepository
import com.skilliumlock.data.repository.ScreenTimeRepository
import com.skilliumlock.engine.PasswordEngine
import kotlinx.coroutines.*

/**
 * Manages all lock overlay views:
 * 1. Profile selection overlay
 * 2. Password input overlay (with dynamic challenge)
 * 3. Screen time warning toast
 *
 * Uses TYPE_ACCESSIBILITY_OVERLAY for maximum security.
 */
class LockOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onAppUnlocked: (profileId: Long, packageName: String, lockedApp: LockedAppEntity) -> Unit,
    private val onGoHome: () -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private val db = AppDatabase.getInstance(context)
    private val profileRepo = ProfileRepository(db.profileDao())
    private val lockedAppRepo = LockedAppRepository(db.lockedAppDao())
    private val screenTimeRepo = ScreenTimeRepository(db.screenTimeDao())

    private var profileSelectView: View? = null
    private var passwordView: View? = null
    private var isOverlayShowing = false

    // Password state
    private var currentChallenge: PasswordEngine.Challenge? = null
    private var enteredDigits = StringBuilder()
    private var currentProfile: ProfileEntity? = null
    private var currentPackageName: String = ""
    private var hideFocusMode = false

    /**
     * Show the profile selection overlay for a locked app.
     */
    fun showProfileSelect(packageName: String) {
        if (isOverlayShowing) return
        currentPackageName = packageName

        scope.launch {
            val profiles = withContext(Dispatchers.IO) {
                profileRepo.getNonAdminProfiles()
            }

            if (profiles.isEmpty()) {
                // No profiles set up - just show password for admin
                return@launch
            }

            handler.post {
                showProfileSelectUI(profiles, packageName)
            }
        }
    }

    private fun showProfileSelectUI(profiles: List<ProfileEntity>, packageName: String) {
        if (isOverlayShowing) return

        val inflater = LayoutInflater.from(context)
        profileSelectView = inflater.inflate(R.layout.overlay_profile_select, null)

        val container = profileSelectView!!.findViewById<LinearLayout>(R.id.profileButtonContainer)

        profiles.forEachIndexed { index, profile ->
            val button = Button(context).apply {
                text = profile.name
                id = View.generateViewId()
                setTextColor(context.getColor(R.color.text_primary))
                textSize = 22f
                setBackgroundResource(R.drawable.bg_button_focusable)
                setPadding(32, 24, 32, 24)
                isFocusable = true
                isFocusableInTouchMode = true
                minimumHeight = 72

                setOnClickListener {
                    onProfileSelected(profile, packageName)
                }

                // Handle D-pad center/enter
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        onProfileSelected(profile, packageName)
                        true
                    } else false
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }

            container.addView(button, params)

            // First button gets focus
            if (index == 0) {
                button.requestFocus()
            }
        }

        val layoutParams = createOverlayParams()
        windowManager.addView(profileSelectView, layoutParams)
        isOverlayShowing = true
    }

    private fun onProfileSelected(profile: ProfileEntity, packageName: String) {
        currentProfile = profile

        scope.launch {
            val lockedApp = withContext(Dispatchers.IO) {
                lockedAppRepo.getLockedApp(profile.id, packageName)
            }

            if (lockedApp == null) {
                // App is not locked for this profile - dismiss
                dismissAll()
                return@launch
            }

            if (lockedApp.screenTimeEnabled) {
                // Check remaining screen time
                val remaining = withContext(Dispatchers.IO) {
                    screenTimeRepo.getRemainingSeconds(
                        profile.id, packageName, lockedApp.timeLimitMinutes
                    )
                }

                if (remaining > 0) {
                    // Screen time available - let them in without password
                    dismissAll()
                    val remainingMin = (remaining + 59) / 60
                    handler.post {
                        Toast.makeText(
                            context,
                            "Remaining: $remainingMin min",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onAppUnlocked(profile.id, packageName, lockedApp)
                    return@launch
                }
            }

            // Need password - show password screen
            handler.post {
                dismissProfileSelect()
                showPasswordInput(profile, packageName)
            }
        }
    }

    /**
     * Show the password input overlay with dynamic challenge.
     */
    private fun showPasswordInput(profile: ProfileEntity, packageName: String) {
        currentProfile = profile
        currentPackageName = packageName
        currentChallenge = PasswordEngine.generateChallenge()
        enteredDigits.clear()
        hideFocusMode = false

        val inflater = LayoutInflater.from(context)
        passwordView = inflater.inflate(R.layout.overlay_password_input, null)

        // Set challenge text
        val challengeText = passwordView!!.findViewById<TextView>(R.id.challengeText)
        challengeText.text = currentChallenge!!.displayText

        // Set up dots
        updateDots()

        // Set up numpad buttons
        setupNumpad()

        val layoutParams = createOverlayParams()
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        windowManager.addView(passwordView, layoutParams)
        isOverlayShowing = true

        // Focus on button "1"
        passwordView?.findViewById<Button>(R.id.btn1)?.requestFocus()
    }

    private fun setupNumpad() {
        val view = passwordView ?: return

        val digitButtons = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )

        digitButtons.forEach { (id, digit) ->
            view.findViewById<Button>(id)?.apply {
                setOnClickListener { onDigitPressed(digit) }
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        onDigitPressed(digit)
                        true
                    } else false
                }
                // Toggle focus visibility based on hideFocusMode
                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    if (hideFocusMode) {
                        v.setBackgroundResource(R.drawable.bg_numpad_button)
                    } else {
                        v.setBackgroundResource(
                            if (hasFocus) R.drawable.bg_numpad_button_focused
                            else R.drawable.bg_numpad_button
                        )
                    }
                }
            }
        }

        // Hide focus button (H)
        view.findViewById<Button>(R.id.btnHide)?.apply {
            setOnClickListener { toggleHideFocus() }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    toggleHideFocus()
                    true
                } else false
            }
        }

        // Submit button (✔)
        view.findViewById<Button>(R.id.btnSubmit)?.apply {
            setOnClickListener { onSubmit() }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    onSubmit()
                    true
                } else false
            }
        }
    }

    private fun onDigitPressed(digit: String) {
        if (enteredDigits.length < 4) {
            enteredDigits.append(digit)
            updateDots()

            // Auto-submit when 4 digits entered
            if (enteredDigits.length == 4) {
                handler.postDelayed({ onSubmit() }, 200)
            }
        }
    }

    private fun onSubmit() {
        val profile = currentProfile ?: return
        val challenge = currentChallenge ?: return

        if (enteredDigits.length != 4) {
            // Not enough digits
            return
        }

        val isValid = PasswordEngine.validatePassword(
            enteredDigits.toString(),
            profile.password,
            challenge
        )

        if (isValid) {
            // Password correct
            scope.launch {
                val lockedApp = withContext(Dispatchers.IO) {
                    lockedAppRepo.getLockedApp(profile.id, currentPackageName)
                }
                dismissAll()
                if (lockedApp != null) {
                    onAppUnlocked(profile.id, currentPackageName, lockedApp)
                }
            }
        } else {
            // Wrong password - shake animation and reset
            enteredDigits.clear()
            updateDots()

            passwordView?.let { view ->
                val dotsContainer = view.findViewById<LinearLayout>(R.id.dotsContainer)
                val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
                dotsContainer?.startAnimation(shake)
            }

            // Generate new challenge
            currentChallenge = PasswordEngine.generateChallenge()
            passwordView?.findViewById<TextView>(R.id.challengeText)?.text =
                currentChallenge!!.displayText

            Toast.makeText(context, "Wrong PIN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleHideFocus() {
        hideFocusMode = !hideFocusMode

        // Update all numpad buttons to show/hide focus
        val view = passwordView ?: return
        val allButtonIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnHide, R.id.btnSubmit
        )

        allButtonIds.forEach { id ->
            val btn = view.findViewById<Button>(id)
            if (hideFocusMode) {
                btn?.setBackgroundResource(R.drawable.bg_numpad_button)
            } else {
                btn?.setBackgroundResource(
                    if (btn.isFocused) R.drawable.bg_numpad_button_focused
                    else R.drawable.bg_numpad_button
                )
            }
        }

        val hideBtn = view.findViewById<Button>(R.id.btnHide)
        hideBtn?.text = if (hideFocusMode) "S" else "H" // S = Show, H = Hide
    }

    private fun updateDots() {
        val view = passwordView ?: return
        val dotViews = listOf(
            view.findViewById<View>(R.id.dot1),
            view.findViewById<View>(R.id.dot2),
            view.findViewById<View>(R.id.dot3),
            view.findViewById<View>(R.id.dot4)
        )

        dotViews.forEachIndexed { index, dot ->
            dot?.setBackgroundResource(
                if (index < enteredDigits.length) R.drawable.dot_filled
                else R.drawable.dot_empty
            )
        }
    }

    fun dismissAll() {
        dismissProfileSelect()
        dismissPassword()
        isOverlayShowing = false
    }

    private fun dismissProfileSelect() {
        profileSelectView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) { /* already removed */ }
            profileSelectView = null
        }
    }

    private fun dismissPassword() {
        passwordView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) { /* already removed */ }
            passwordView = null
        }
    }

    fun isShowing(): Boolean = isOverlayShowing

    private fun createOverlayParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    fun destroy() {
        dismissAll()
        scope.cancel()
    }
}
