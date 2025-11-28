package com.example.umelec

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.MotionEvent
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable // Needed for AlertDialog background
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.appcompat.widget.AppCompatButton

// We assume your login Activity is correctly named 'Login' and is in 'Login.kt'

class Changepassword : AppCompatActivity() {

    // Define color constants (Used only for requirements TextView text colors)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_ERROR_RED = Color.parseColor("#D33131")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#27A688")
    private val COLOR_HINT_GRAY = Color.parseColor("#5C5C77")

    // --- TEMPORARY TEST DATA ---
    // The test password is still defined here for the simulated check in btnConfirm
    // REMEMBER TO REMOVE THIS ENTIRE CONSTANT BEFORE FINAL DEPLOYMENT!
    private val FAKE_CURRENT_PASSWORD_FOR_TEST = "Test1234!"
    // ---------------------------

    val specialChars = "!@#$%^&*-+=()_`~[]{}|\\:;\"'<,>.?/"

    private lateinit var btnConfirm: Button
    private lateinit var btnBack: ImageButton

    // Current Password Fields
    private lateinit var layoutCurrentPassword: TextInputLayout
    private lateinit var inputCurrentPassword: TextInputEditText

    // New Password Fields
    private lateinit var layoutNewPassword: TextInputLayout
    private lateinit var inputNewPassword: TextInputEditText
    private lateinit var passwordRequirements: View
    private lateinit var reqLength: TextView
    private lateinit var reqMixedcase: TextView
    private lateinit var reqSpecial: TextView
    private lateinit var reqNumber: TextView
    private lateinit var reqField: TextView

    // Confirm Password Fields
    private lateinit var layoutConfirmPassword: TextInputLayout
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var confirmPasswordRequirements: View
    private lateinit var reqMatch: TextView

    private var allPasswordValidationsPassed = false

    // =========================================================================
    // 1. HELPER FUNCTIONS
    // =========================================================================

    // 🚨 HELPER: Reusable Logic to Clear Errors
    private fun clearValidationState(layout: TextInputLayout) {
        layout.error = null // Clear red border/error text
        layout.isActivated = false // Clear green border
    }

    // 🚨 HELPER: Hides the keyboard and clears focus
    private fun hideKeyboardAndClearFocus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        currentFocus?.clearFocus()
    }

    /**
     * Custom toast for successful password change. (UPDATED to finish activity)
     */
    private fun showChangeSuccessDialog() {
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.custom_toast_success, null)

        val titleText: TextView = layout.findViewById(R.id.toast_title)
        val valueText: TextView = layout.findViewById(R.id.toast_value)
        val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

        titleText.text = "Success!" // UPDATED TEXT
        valueText.text = "Password updated." // UPDATED TEXT
        actionButton.visibility = View.GONE

        with (Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            @Suppress("DEPRECATION")
            view = layout
            show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Action: Just finish the current activity
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }, 40)
    }

    /**
     * Custom alert dialog for password change error (e.g., old password mismatch).
     */
    private fun showChangeErrorDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // Set custom error text
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Old password incorrect."
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Please try again."

        // Close button listener
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            // Clear all three password fields' validation state
            clearValidationState(layoutCurrentPassword)
            clearValidationState(layoutNewPassword)
            clearValidationState(layoutConfirmPassword)
        }

        // Trigger Red border via standard Material Error property for ALL three fields
        //layoutCurrentPassword.error = " "
        //layoutNewPassword.error = " "
        //layoutConfirmPassword.error = " "

        dialog.show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changepassword)

        // =====================================================================
        // 2. VIEW INITIALIZATION (FIND VIEW BY ID)
        // =====================================================================

        // Buttons
        btnBack = findViewById(R.id.btnBack)
        btnConfirm = findViewById(R.id.btnConfirm)

        // Current Password Fields
        layoutCurrentPassword = findViewById(R.id.textInputLayoutCurrentPassword)
        inputCurrentPassword = findViewById(R.id.inputCurrentPassword)

        // New Password Fields
        layoutNewPassword = findViewById(R.id.textInputLayoutPassword)
        inputNewPassword = findViewById(R.id.inputPassword)
        val fieldRequirementsContainer = findViewById<View>(R.id.fieldRequirements)
        passwordRequirements = findViewById(R.id.passwordRequirements)

        // Password Requirement Texts
        reqField = findViewById(R.id.reqField)
        reqLength = findViewById(R.id.reqLength)
        reqMixedcase = findViewById(R.id.reqMixedcase)
        reqSpecial = findViewById(R.id.reqSpecial)
        reqNumber = findViewById(R.id.reqNumber)

        // Confirm Password Fields
        layoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        confirmPasswordRequirements = findViewById(R.id.ConfirmpasswordRequirements)
        reqMatch = findViewById(R.id.reqMatch)

        // =====================================================================
        // 3. INITIAL STATE & LISTENERS
        // =====================================================================

        // --------------------------------------------------------
        // START: TEMPORARY TESTING DATA - AUTOFILL REMOVED
        // The current password field will now be empty on startup.
        // --------------------------------------------------------

        btnConfirm.isEnabled = false
        // Hide containers
        fieldRequirementsContainer.visibility = View.GONE
        passwordRequirements.visibility = View.GONE
        confirmPasswordRequirements.visibility = View.GONE

        // Reset all states
        clearValidationState(layoutCurrentPassword)
        clearValidationState(layoutNewPassword)
        clearValidationState(layoutConfirmPassword)


        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // --- UPDATED CONFIRM BUTTON LOGIC ---
        btnConfirm.setOnClickListener {
            hideKeyboardAndClearFocus()

            // 1. Get the current password input
            val currentPasswordInput = inputCurrentPassword.text.toString()

            // 2. SIMULATED BACKEND CHECK: Check if the current password is correct
            // REMEMBER TO REPLACE THIS WITH A REAL API CALL LATER.
            if (currentPasswordInput != FAKE_CURRENT_PASSWORD_FOR_TEST) {
                showChangeErrorDialog() // Show error dialog
            } else {
                // 3. SUCCESS PATH
                showChangeSuccessDialog()
            }
        }
        // ------------------------------------

        // Set up the TextWatchers and Focus Listeners

        // Current Password Listeners
        inputCurrentPassword.addTextChangedListener(currentPasswordWatcher)
        inputCurrentPassword.setOnFocusChangeListener(currentPasswordFocusListener)
        inputCurrentPassword.setOnClickListener { clearValidationState(layoutCurrentPassword) }

        // New/Confirm Password Listeners
        inputNewPassword.addTextChangedListener(passwordWatcher)
        inputConfirmPassword.addTextChangedListener(confirmPasswordWatcher)
        inputNewPassword.setOnFocusChangeListener(passwordFocusListener)
        inputConfirmPassword.setOnFocusChangeListener(confirmPasswordFocusListener)

        // 💡 Click listeners to clear errors on tap
        inputNewPassword.setOnClickListener { clearValidationState(layoutNewPassword) }
        inputConfirmPassword.setOnClickListener { clearValidationState(layoutConfirmPassword) }
    }

    // =========================================================================
    // 4. CORE LOGIC & LISTENERS
    // =========================================================================

    /**
     * Updates the state of the 'Confirm' button based on all validations.
     */
    private fun updateSaveButtonState() {
        val currentPassword = inputCurrentPassword.text.toString()
        val password = inputNewPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()

        // 1. Current password must be provided
        val isCurrentPasswordValid = currentPassword.isNotEmpty()

        // 2. New password must pass all requirements AND match confirmation
        val isConfirmMatch = allPasswordValidationsPassed && (password == confirmPassword) && confirmPassword.isNotEmpty()

        // Button is enabled ONLY if BOTH conditions are met
        btnConfirm.isEnabled = isCurrentPasswordValid && isConfirmMatch
    }

    // ---------------------------------------------------------------------
    // 🔹 Current Password Focus Listener
    // ---------------------------------------------------------------------
    private val currentPasswordFocusListener = View.OnFocusChangeListener { _, hasFocus ->
        val currentPassword = inputCurrentPassword.text.toString()
        if (!hasFocus && currentPassword.isEmpty()) {
            layoutCurrentPassword.error = "Field is required"
            layoutCurrentPassword.isActivated = false
        } else if (!hasFocus) {
            // Success state on blur
            layoutCurrentPassword.error = null
            layoutCurrentPassword.isActivated = true
        } else {
            // Clear state on focus
            clearValidationState(layoutCurrentPassword)
        }
        updateSaveButtonState()
    }

    // ---------------------------------------------------------------------
    // 🔹 Current Password Text Watcher
    // ---------------------------------------------------------------------
    private val currentPasswordWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val currentPassword = inputCurrentPassword.text.toString()
            clearValidationState(layoutCurrentPassword)

            // Activate success border immediately if text exists
            if (currentPassword.isNotEmpty()) {
                layoutCurrentPassword.isActivated = true
            }
            updateSaveButtonState()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }


    // ---------------------------------------------------------------------
    // 🔹 New Password Focus Listener
    // ---------------------------------------------------------------------
    private val passwordFocusListener = View.OnFocusChangeListener { _, hasFocus ->
        val password = inputNewPassword.text.toString()
        val fieldRequirementsContainer = findViewById<View>(R.id.fieldRequirements)

        if (hasFocus) {
            // --- WHEN FOCUSED (Typing) ---
            clearValidationState(layoutNewPassword)

            if (password.isEmpty()) {
                // State 1a: Empty field, focus gained: Show BOTH containers (Hint state)
                fieldRequirementsContainer.visibility = View.VISIBLE
                passwordRequirements.visibility = View.VISIBLE
                reqField.setTextColor(COLOR_HINT_GRAY)
                reqField.text = "• Field is required"
            } else {
                // State 2a: Filled field, focus gained: Show ONLY detailed requirements
                fieldRequirementsContainer.visibility = View.GONE
                passwordRequirements.visibility = View.VISIBLE
            }

        } else {
            // --- WHEN UN-FOCUSED (BLUR) ---
            if (password.isEmpty()) {
                // State 1b: Empty field, focus lost: Show ONLY "Field is required" error
                fieldRequirementsContainer.visibility = View.VISIBLE
                passwordRequirements.visibility = View.GONE

                reqField.setTextColor(COLOR_ERROR_RED)
                reqField.text = "• Field is required"
                //layoutNewPassword.error = " " // Show Red border
                layoutNewPassword.boxStrokeColor = COLOR_ERROR_RED
                layoutNewPassword.isActivated = false
            } else {
                // State 2b: Filled field, focus lost: Show final validation status
                fieldRequirementsContainer.visibility = View.GONE

                if (allPasswordValidationsPassed) {
                    passwordRequirements.visibility = View.GONE // Hide on success
                    layoutNewPassword.error = null
                    layoutNewPassword.isActivated = true
                } else {
                    passwordRequirements.visibility = View.VISIBLE // Keep showing errors
                    //layoutNewPassword.error = " " // Show Red border
                    layoutNewPassword.boxStrokeColor = COLOR_ERROR_RED
                    layoutNewPassword.isActivated = false
                }
            }
        }
        updateSaveButtonState()
    }

    // ---------------------------------------------------------------------
    // 🔹 New Password Text Watcher
    // ---------------------------------------------------------------------
    private val passwordWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val password = inputNewPassword.text.toString()
            val fieldRequirementsContainer = findViewById<View>(R.id.fieldRequirements)

            // Hide generic 'field required' hint once typing starts
            if (password.isNotEmpty() && inputNewPassword.isFocused) {
                fieldRequirementsContainer.visibility = View.GONE
                passwordRequirements.visibility = View.VISIBLE
            }

            // Detailed validation logic
            var isLengthValid = password.length >= 8
            reqLength.setTextColor(if (isLengthValid) COLOR_SUCCESS_GREEN else COLOR_ERROR_RED)
            reqLength.text = if (isLengthValid) "✓ Must be at least 8 characters" else "• Must be at least 8 characters"

            val hasUpper = password.any { it.isUpperCase() }
            val hasLower = password.any { it.isLowerCase() }
            var isMixedcaseValid = hasUpper && hasLower
            reqMixedcase.setTextColor(if (isMixedcaseValid) COLOR_SUCCESS_GREEN else COLOR_ERROR_RED)
            reqMixedcase.text = if (isMixedcaseValid) "✓ Mixed case" else "• Mixed case"

            var isSpecialValid = password.any { it in specialChars }
            reqSpecial.setTextColor(if (isSpecialValid) COLOR_SUCCESS_GREEN else COLOR_ERROR_RED)
            reqSpecial.text = if (isSpecialValid) "✓ Must contain a special character" else "• Must contain a special character"

            var isNumberValid = password.any { it.isDigit() }
            reqNumber.setTextColor(if (isNumberValid) COLOR_SUCCESS_GREEN else COLOR_ERROR_RED)
            reqNumber.text = if (isNumberValid) "✓ Must contain a number" else "• Must contain a number"

            allPasswordValidationsPassed = isLengthValid && isMixedcaseValid && isSpecialValid && isNumberValid

            clearValidationState(layoutNewPassword)

            // Apply green/red border state while focused
            if (inputNewPassword.isFocused) {
                if (allPasswordValidationsPassed) {
                    layoutNewPassword.isActivated = true
                }
                else {
                    //layoutNewPassword.error = " " // Triggers red border
                    layoutNewPassword.boxStrokeColor = COLOR_ERROR_RED
                }
            }

            // Re-check confirm password field whenever new password changes
            confirmPasswordWatcher.afterTextChanged(inputConfirmPassword.text)
            updateSaveButtonState()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            clearValidationState(layoutNewPassword)
        }
    }


    // ---------------------------------------------------------------------
    // 🔹 Confirm Password Focus Listener
    // ---------------------------------------------------------------------
    private val confirmPasswordFocusListener = View.OnFocusChangeListener { _, hasFocus ->
        val password = inputNewPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()
        val isMatch = password == confirmPassword && confirmPassword.isNotEmpty()

        if (hasFocus) {
            confirmPasswordRequirements.visibility = View.VISIBLE
            clearValidationState(layoutConfirmPassword)
            // Use the hint color for the initial state
            reqMatch.setTextColor(COLOR_HINT_GRAY)
            reqMatch.text = "• Passwords must match"
        } else {
            when {
                confirmPassword.isEmpty() -> {
                    confirmPasswordRequirements.visibility = View.VISIBLE
                    reqMatch.setTextColor(COLOR_ERROR_RED)
                    reqMatch.text = "• Field is required"
                    //layoutConfirmPassword.error = " "
                    layoutConfirmPassword.boxStrokeColor = COLOR_ERROR_RED
                    layoutConfirmPassword.isActivated = false
                }
                isMatch -> {
                    confirmPasswordRequirements.visibility = View.GONE
                    layoutConfirmPassword.error = null
                    layoutConfirmPassword.isActivated = true
                }
                else -> {
                    confirmPasswordRequirements.visibility = View.VISIBLE
                    reqMatch.setTextColor(COLOR_ERROR_RED)
                    reqMatch.text = "• Passwords must match"
                    //layoutConfirmPassword.error = " "
                    layoutConfirmPassword.boxStrokeColor = COLOR_ERROR_RED
                    layoutConfirmPassword.isActivated = false
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Confirm Password Text Watcher
    // ---------------------------------------------------------------------
    private val confirmPasswordWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val password = inputNewPassword.text.toString()
            val confirmPassword = inputConfirmPassword.text.toString()
            val isMatch = password == confirmPassword && confirmPassword.isNotEmpty()

            clearValidationState(layoutConfirmPassword)

            if (confirmPassword.isEmpty()) {
                reqMatch.visibility = View.GONE
                layoutConfirmPassword.isActivated = false
            } else if (isMatch) {
                reqMatch.visibility = View.VISIBLE
                reqMatch.setTextColor(COLOR_SUCCESS_GREEN)
                reqMatch.text = "✓ Passwords match"
                layoutConfirmPassword.isActivated = true
            } else {
                reqMatch.visibility = View.VISIBLE
                reqMatch.setTextColor(COLOR_ERROR_RED)
                reqMatch.text = "• Passwords must match"
                //layoutConfirmPassword.error = " "
                layoutConfirmPassword.boxStrokeColor = COLOR_ERROR_RED
                layoutConfirmPassword.isActivated = false
            }

            updateSaveButtonState()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            clearValidationState(layoutConfirmPassword)
        }
    }

    // =========================================================================
    // 5. DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD)
    // =========================================================================
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndClearFocus()

                    // Manually trigger the blur logic by clearing focus on the fields
                    inputCurrentPassword.clearFocus()
                    inputNewPassword.clearFocus()
                    inputConfirmPassword.clearFocus()

                    // Added blur state update to match your RegisterActivity.kt logic
                    clearValidationState(layoutCurrentPassword)
                    clearValidationState(layoutNewPassword)
                    clearValidationState(layoutConfirmPassword)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}