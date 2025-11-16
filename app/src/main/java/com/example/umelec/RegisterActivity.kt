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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.MotionEvent
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable

class RegisterActivity : AppCompatActivity() {

    // =========================================================================
    // 1. CLASS-LEVEL PROPERTIES & VIEWS
    // =========================================================================
    private var allValidationsPassed = false
    val specialChars = "!@#$%^&*-+=()_`~[]{}|\\:;\"'<,>.?/"

    // Define color constants (Used only for requirements TextView text colors)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_ERROR_RED = Color.parseColor("#D33131")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#27A688")
    private val COLOR_HINT_GRAY = Color.parseColor("#5C5C77")

    // Define field layouts outside onCreate (Required for dispatchTouchEvent)
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText

    // Requirement Views
    private lateinit var fieldRequirements: View
    private lateinit var passwordRequirements: View
    private lateinit var ConfirmpasswordRequirements: View
    private lateinit var EmailRequirements: View
    private lateinit var reqField: TextView
    private lateinit var reqEmail: TextView
    private lateinit var reqMatch: TextView
    private lateinit var reqLength: TextView
    private lateinit var reqMixedcase: TextView
    private lateinit var reqSpecial: TextView
    private lateinit var reqNumber: TextView

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

    // 🚨 HELPER: Clears all input fields and validation states
    private fun clearInputFields() {
        inputEmail.text?.clear()
        inputPassword.text?.clear()
        inputConfirmPassword.text?.clear()

        clearValidationState(layoutEmail)
        clearValidationState(layoutPassword)
        clearValidationState(layoutConfirmPassword)

        // Reset requirements visibility for a clean slate
        EmailRequirements.visibility = View.GONE
        fieldRequirements.visibility = View.GONE
        passwordRequirements.visibility = View.GONE
        ConfirmpasswordRequirements.visibility = View.GONE
    }

    // 🚨 HELPER: Custom Dialog for Email Registration Error
    private fun showEmailAlreadyRegisteredDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // Set custom text (will be updated based on error)
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Registration Error"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Email address already in use or registration failed."

        // Trigger Red border on the Email field
        layoutEmail.error = " "
        layoutPassword.error = null
        layoutConfirmPassword.error = null

        // Clear all inputs on dialog close
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            clearInputFields()
        }

        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // =====================================================================
        // 2. VIEW INITIALIZATION (FIND VIEW BY ID)
        // =====================================================================

        // 🔹 Buttons and Navigation
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val loginButton = findViewById<TextView>(R.id.LoginButton)

        // 🔹 Field Layouts/Inputs
        layoutEmail = findViewById(R.id.textInputLayoutEmail)
        inputEmail = findViewById(R.id.inputEmail)
        layoutPassword = findViewById(R.id.textInputLayoutPassword)
        inputPassword = findViewById(R.id.inputPassword)
        layoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)

        // 🔹 Requirement Containers
        EmailRequirements = findViewById(R.id.EmailRequirements)
        fieldRequirements = findViewById(R.id.fieldRequirements)
        passwordRequirements = findViewById(R.id.passwordRequirements)
        ConfirmpasswordRequirements = findViewById(R.id.ConfirmpasswordRequirements)

        // 🔹 Requirement Text Views
        reqEmail = findViewById(R.id.reqEmail)
        reqField = findViewById(R.id.reqField)
        reqLength = findViewById(R.id.reqLength)
        reqMixedcase = findViewById(R.id.reqMixedcase)
        reqSpecial = findViewById(R.id.reqSpecial)
        reqNumber = findViewById(R.id.reqNumber)
        reqMatch = findViewById(R.id.reqMatch)

        // =====================================================================
        // 3. CONFIRM PASSWORD WATCHER DECLARATION
        // =====================================================================
        lateinit var confirmPasswordWatcher: TextWatcher

        // =====================================================================
        // 4. INITIAL SETUP, HELPER, AND NAVIGATION
        // =====================================================================

        // Initial State Setup
        btnNext.isEnabled = false
        EmailRequirements.visibility = View.GONE
        fieldRequirements.visibility = View.GONE
        passwordRequirements.visibility = View.GONE
        ConfirmpasswordRequirements.visibility = View.GONE


        // 🔹 Helper to update Next button state
        fun updateNextButtonState() {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString()
            val confirmPassword = inputConfirmPassword.text.toString()

            val isEmailValid = email.endsWith("@umak.edu.ph", ignoreCase = true)
            val isConfirmMatch = password == confirmPassword && confirmPassword.isNotEmpty()

            btnNext.isEnabled = isEmailValid && allValidationsPassed && isConfirmMatch
        }

        // 🔹 Navigation Listeners
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
        loginButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            overridePendingTransition(0, 0)
        }

        btnNext.setOnClickListener {
            hideKeyboardAndClearFocus()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString()
            val confirmPassword = inputConfirmPassword.text.toString()

            // Validate that passwords match
            if (password != confirmPassword) {
                showEmailAlreadyRegisteredDialog() // Reuse for error display
                return@setOnClickListener
            }

            // Disable button during registration
            btnNext.isEnabled = false

            // Create Firebase user account
            FirebaseAuthHelper.createUser(
                email = email,
                password = password,
                onSuccess = { user ->
                    // Save credentials temporarily for next steps
                    FirebaseAuthHelper.saveTemporaryCredentials(this, email, password)
                    
                    // Navigate to next registration step
                    val intent = Intent(this, RegisterActivity2::class.java)
                    startActivity(intent)
                },
                onFailure = { errorMessage ->
                    // Re-enable button
                    btnNext.isEnabled = true
                    
                    // Check if email already exists
                    if (errorMessage.contains("email address is already", ignoreCase = true) ||
                        errorMessage.contains("already in use", ignoreCase = true)) {
                        showEmailAlreadyRegisteredDialog()
                    } else {
                        // Show generic error
                        showEmailAlreadyRegisteredDialog()
                    }
                }
            )
        }

        // 💡 Click listeners to clear errors on tap
        inputEmail.setOnClickListener { clearValidationState(layoutEmail) }
        inputPassword.setOnClickListener { clearValidationState(layoutPassword) }
        inputConfirmPassword.setOnClickListener { clearValidationState(layoutConfirmPassword) }


        // =====================================================================
        // 5. EMAIL FIELD LOGIC
        // =====================================================================

        // 🔹 Focus Change Listener for Email
        inputEmail.setOnFocusChangeListener { _, hasFocus ->
            val email = inputEmail.text.toString().trim()
            val isValid = email.endsWith("@umak.edu.ph", ignoreCase = true)

            if (hasFocus) {
                EmailRequirements.visibility = View.VISIBLE
                reqEmail.setTextColor(COLOR_HINT_GRAY)
                reqEmail.text = "• Please use your UMak email (@umak.edu.ph)"
                clearValidationState(layoutEmail)
            } else {
                when {
                    email.isEmpty() -> {
                        EmailRequirements.visibility = View.VISIBLE
                        reqEmail.setTextColor(COLOR_ERROR_RED)
                        reqEmail.text = "• Field is required"
                        layoutEmail.error = " "
                        layoutEmail.isActivated = false
                    }
                    isValid -> {
                        EmailRequirements.visibility = View.GONE
                        layoutEmail.error = null
                        layoutEmail.isActivated = true
                    }
                    else -> {
                        EmailRequirements.visibility = View.VISIBLE
                        reqEmail.setTextColor(COLOR_ERROR_RED)
                        reqEmail.text = "• Please use your UMak email (@umak.edu.ph)"
                        layoutEmail.error = " "
                        layoutEmail.isActivated = false
                    }
                }
            }
        }

        // 🔹 Text Watcher for Email
        inputEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = inputEmail.text.toString().trim()
                val isValid = email.endsWith("@umak.edu.ph", ignoreCase = true)

                clearValidationState(layoutEmail)

                if (email.isEmpty()) {
                    reqEmail.setTextColor(COLOR_HINT_GRAY)
                    reqEmail.text = "• Please use your UMak email (@umak.edu.ph)"
                    layoutEmail.isActivated = false
                } else if (isValid) {
                    reqEmail.setTextColor(COLOR_SUCCESS_GREEN)
                    reqEmail.text = "✓ Please use your UMak email (@umak.edu.ph)"
                    if (inputEmail.isFocused) { layoutEmail.isActivated = true }
                    layoutEmail.isActivated = true
                    layoutEmail.boxStrokeColor = COLOR_SUCCESS_GREEN
                } else {
                    reqEmail.setTextColor(COLOR_ERROR_RED)
                    reqEmail.text = "• Please use your UMak email (@umak.edu.ph)"
                    layoutEmail.error = " "
                }

                updateNextButtonState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            // 🚨 FIX APPLIED: Corrected signature to use 'count: Int'
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutEmail)
            }
        })

        // =====================================================================
        // 6. PASSWORD FIELD LOGIC
        // =====================================================================

        // 🔹 Focus Change Listener for Password
        inputPassword.setOnFocusChangeListener { _, hasFocus ->
            val password = inputPassword.text.toString()

            if (hasFocus) {
                // --- WHEN FOCUSED (Typing) ---
                clearValidationState(layoutPassword)

                if (password.isEmpty()) {
                    // State 1a: Empty field, focus gained: Show BOTH containers (Hint state)
                    fieldRequirements.visibility = View.VISIBLE
                    passwordRequirements.visibility = View.VISIBLE
                    reqField.setTextColor(COLOR_HINT_GRAY)
                    reqField.text = "• Field is required"
                } else {
                    // State 2a: Filled field, focus gained: Show ONLY detailed requirements
                    fieldRequirements.visibility = View.GONE
                    passwordRequirements.visibility = View.VISIBLE
                }

            } else {
                // --- WHEN UN-FOCUSED (BLUR) ---
                if (password.isEmpty()) {
                    // State 1b: Empty field, focus lost: Show ONLY "Field is required" error
                    fieldRequirements.visibility = View.VISIBLE
                    passwordRequirements.visibility = View.GONE

                    reqField.setTextColor(COLOR_ERROR_RED)
                    reqField.text = "• Field is required"
                    layoutPassword.error = " " // Show Red border
                    layoutPassword.isActivated = false
                } else {
                    // State 2b: Filled field, focus lost: Show final validation status
                    fieldRequirements.visibility = View.GONE

                    if (allValidationsPassed) {
                        passwordRequirements.visibility = View.GONE // Hide on success
                        layoutPassword.error = null
                        layoutPassword.isActivated = true
                    } else {
                        passwordRequirements.visibility = View.VISIBLE // Keep showing errors
                        layoutPassword.error = " "
                        layoutPassword.isActivated = false
                    }
                }
            }
            updateNextButtonState()
        }

        // 🔹 Text Watcher for Password
        val passwordWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = inputPassword.text.toString()

                // Hide generic 'field required' hint once typing starts
                if (password.isNotEmpty() && inputPassword.isFocused) {
                    fieldRequirements.visibility = View.GONE
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

                allValidationsPassed = isLengthValid && isMixedcaseValid && isSpecialValid && isNumberValid

                clearValidationState(layoutPassword)

                // Apply green/red border state while focused
                if (inputPassword.isFocused) {
                    if (allValidationsPassed) {
                        layoutPassword.isActivated = true
                        layoutPassword.boxStrokeColor = COLOR_SUCCESS_GREEN
                    }
                    else {
                        layoutPassword.error = " "
                    }
                }

                confirmPasswordWatcher.afterTextChanged(inputConfirmPassword.text)
                updateNextButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutPassword)
            }
        }

        inputPassword.addTextChangedListener(passwordWatcher)

        // =====================================================================
        // 7. CONFIRM PASSWORD FIELD LOGIC
        // =====================================================================

        // 🔹 Focus Change Listener for Confirm Password
        inputConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            val password = inputPassword.text.toString()
            val confirmPassword = inputConfirmPassword.text.toString()
            val isMatch = password == confirmPassword && confirmPassword.isNotEmpty()

            if (hasFocus) {
                ConfirmpasswordRequirements.visibility = View.VISIBLE
                clearValidationState(layoutConfirmPassword)
                reqMatch.setTextColor(COLOR_HINT_GRAY)
                reqMatch.text = "• Passwords must match"
            } else {
                when {
                    confirmPassword.isEmpty() -> {
                        ConfirmpasswordRequirements.visibility = View.VISIBLE
                        reqMatch.setTextColor(COLOR_ERROR_RED)
                        reqMatch.text = "• Field is required"
                        layoutConfirmPassword.error = " "
                        layoutConfirmPassword.isActivated = false
                    }
                    isMatch -> {
                        ConfirmpasswordRequirements.visibility = View.GONE
                        layoutConfirmPassword.error = null
                        layoutConfirmPassword.isActivated = true
                    }
                    else -> {
                        ConfirmpasswordRequirements.visibility = View.VISIBLE
                        reqMatch.setTextColor(COLOR_ERROR_RED)
                        reqMatch.text = "• Passwords must match"
                        layoutConfirmPassword.error = " "
                        layoutConfirmPassword.isActivated = false
                    }
                }
            }
        }

        // 🔹 Text Watcher for Confirm Password
        confirmPasswordWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = inputPassword.text.toString()
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
                    layoutConfirmPassword.boxStrokeColor = COLOR_SUCCESS_GREEN
                } else {
                    reqMatch.visibility = View.VISIBLE
                    reqMatch.setTextColor(COLOR_ERROR_RED)
                    reqMatch.text = "• Passwords must match"
                    layoutConfirmPassword.error = " "
                    layoutConfirmPassword.isActivated = false
                }

                updateNextButtonState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutConfirmPassword)
            }
        }

        inputConfirmPassword.addTextChangedListener(confirmPasswordWatcher)

        // Email TextWatcher for state updates (Fix applied here too)
        inputEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateNextButtonState() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            // 🚨 FIX APPLIED: Corrected signature to use 'count: Int'
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    } // End of onCreate

    // =========================================================================
    // 8. DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD)
    // =========================================================================
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndClearFocus()

                    // Manually trigger the blur logic for all fields on click outside
                    inputEmail.clearFocus()
                    inputPassword.clearFocus()
                    inputConfirmPassword.clearFocus()

                    clearValidationState(layoutEmail)
                    clearValidationState(layoutPassword)
                    clearValidationState(layoutConfirmPassword)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}