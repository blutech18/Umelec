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
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater


// NOTE: The unnecessary 'LoginActivity' placeholder class has been removed.
// We assume your login Activity is correctly named 'Login' and is in 'Login.kt'

class Resetpassword : AppCompatActivity() {

    // Define color constants (Used only for requirements TextView text colors)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_ERROR_RED = Color.parseColor("#D33131")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#27A688")
    private val COLOR_HINT_GRAY = Color.parseColor("#5C5C77")

    val specialChars = "!@#$%^&*-+=()_`~[]{}|\\:;\"'<,>.?/"

    private lateinit var btnConfirm: Button
    private lateinit var btnBack: ImageButton

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
    private var actionCode: String? = null // Action code from email link

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
     * Custom dialog for successful password reset.
     */
    private fun showResetSuccessDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_success, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        dialogView.findViewById<TextView>(R.id.toast_title).text = "Password Reset Successful!"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Your password has been reset. You can now log in with your new password."

        val btnAction = dialogView.findViewById<Button>(R.id.btn_action)
        btnAction.text = "Continue to Login"

        btnAction.setOnClickListener {
            dialog.dismiss()
            // Navigate to Login
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish ResetPassword activity
            overridePendingTransition(0, 0)
        }

        dialog.show()
    }

    /**
     * Custom dialog for password reset error.
     */
    private fun showResetErrorDialog(errorMessage: String) {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        dialogView.findViewById<TextView>(R.id.toast_title).text = "Password Reset Failed"
        dialogView.findViewById<TextView>(R.id.toast_value).text = errorMessage

        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        // Check if we have an action code from email link or intent
        // Firebase email link format: https://umelec-70618.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=CODE&apiKey=KEY
        val data = intent.data
        actionCode = intent.getStringExtra("actionCode")
        
        // Try to extract action code from Firebase email link URL
        if (actionCode == null && data != null) {
            // Extract oobCode from query parameters (Firebase uses 'oobCode' parameter)
            actionCode = data.getQueryParameter("oobCode")
        }

        // =====================================================================
        // 2. VIEW INITIALIZATION (FIND VIEW BY ID)
        // =====================================================================

        // Buttons
        btnBack = findViewById(R.id.btnBack)
        btnConfirm = findViewById(R.id.btnConfirm)

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

        btnConfirm.isEnabled = false
        // Hide containers
        fieldRequirementsContainer.visibility = View.GONE
        passwordRequirements.visibility = View.GONE
        confirmPasswordRequirements.visibility = View.GONE

        // Reset all states
        clearValidationState(layoutNewPassword)
        clearValidationState(layoutConfirmPassword)


        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // Handle password reset
        btnConfirm.setOnClickListener {
            hideKeyboardAndClearFocus()
            
            val newPassword = inputNewPassword.text.toString()
            val confirmPassword = inputConfirmPassword.text.toString()
            
            // Validate passwords match
            if (newPassword != confirmPassword) {
                // Show error - passwords don't match
                return@setOnClickListener
            }
            
            // Validate password requirements
            if (!allPasswordValidationsPassed) {
                // Show error - password doesn't meet requirements
                return@setOnClickListener
            }
            
            // Disable button during reset
            btnConfirm.isEnabled = false
            
            // Get action code from class property
            val code = actionCode
            
            if (code != null && code.isNotEmpty()) {
                // Reset password using action code from email link
                FirebaseAuthHelper.confirmPasswordReset(
                    actionCode = code,
                    newPassword = newPassword,
                    onSuccess = {
                        // Password reset successful
                        showResetSuccessDialog()
                    },
                    onFailure = { errorMessage ->
                        // Re-enable button
                        btnConfirm.isEnabled = true
                        // Show error dialog
                        showResetErrorDialog(errorMessage)
                    }
                )
            } else {
                // No action code - try to update password if user is logged in
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser != null) {
                    // User is logged in, update password directly (for change password feature)
                    currentUser.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showResetSuccessDialog()
                            } else {
                                btnConfirm.isEnabled = true
                                val errorMessage = task.exception?.message ?: "Failed to reset password"
                                showResetErrorDialog(errorMessage)
                            }
                        }
                } else {
                    // No action code and user not logged in - show error
                    btnConfirm.isEnabled = true
                    showResetErrorDialog("Invalid or expired reset link. Please request a new password reset from the login screen.")
                }
            }
        }

        // Set up the TextWatchers and Focus Listeners
        inputNewPassword.addTextChangedListener(passwordWatcher)
        inputConfirmPassword.addTextChangedListener(confirmPasswordWatcher)
        inputNewPassword.setOnFocusChangeListener(passwordFocusListener)
        inputConfirmPassword.setOnFocusChangeListener(confirmPasswordFocusListener)

        // 💡 Click listeners to clear errors on tap
        inputNewPassword.setOnClickListener { clearValidationState(layoutNewPassword) }
        inputConfirmPassword.setOnClickListener { clearValidationState(layoutConfirmPassword) }
    }

    // =========================================================================
    // 4. CORE LOGIC & LISTENERS (ADOPTED FROM RegisterActivity.kt)
    // =========================================================================

    /**
     * Updates the state of the 'Confirm' button based on all validations.
     */
    private fun updateSaveButtonState() {
        val password = inputNewPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()

        val isConfirmMatch = allPasswordValidationsPassed && (password == confirmPassword) && confirmPassword.isNotEmpty()

        btnConfirm.isEnabled = isConfirmMatch
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
                layoutNewPassword.error = " " // Show Red border
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
                    layoutNewPassword.error = " " // Show Red border
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
                    layoutNewPassword.error = " " // Triggers red border
                }
            }

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
                    layoutConfirmPassword.error = " "
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
                    layoutConfirmPassword.error = " "
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
                layoutConfirmPassword.error = " "
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
                    inputNewPassword.clearFocus()
                    inputConfirmPassword.clearFocus()
                    // Added blur state update to match your RegisterActivity.kt logic
                    clearValidationState(layoutNewPassword)
                    clearValidationState(layoutConfirmPassword)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}