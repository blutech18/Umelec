package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.app.AlertDialog

// 💡 NEW IMPORTS for Keyboard and Focus Management
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.MotionEvent
import android.graphics.Rect


class Forgotpassword : AppCompatActivity() {

    // Define the key for passing data between activities
    companion object {
        const val EXTRA_EMAIL_ADDRESS = "com.example.umelec.EMAIL_ADDRESS"
        // Define color constants (Used only for requirements TextView text colors)
        private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
        private val COLOR_ERROR_RED = Color.parseColor("#D33131")
        private val COLOR_SUCCESS_GREEN = Color.parseColor("#27A688")
        private val COLOR_HINT_GRAY = Color.parseColor("#5C5C77")
    }

    // 🚨 1. DECLARE VIEWS AS CLASS PROPERTIES (Used by helper functions) 🚨
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var emailRequirementsContainer: View
    private lateinit var reqEmail: TextView
    private lateinit var btnSendVerification: Button

    // --- Validation Helper Functions ---

    private fun isFieldNotEmpty(text: String): Boolean {
        return text.trim().isNotEmpty()
    }

    private fun isUmakEmail(email: String): Boolean {
        return email.trim().endsWith("@umak.edu.ph", ignoreCase = true)
    }

    // Function to clear validation state for a single TextInputLayout
    private fun clearValidationState(layout: TextInputLayout) {
        layout.error = null
        layout.boxStrokeColor = COLOR_PRIMARY_BLUE
    }

    private fun showValidationError(layout: TextInputLayout, requirementContainer: View, reqText: TextView, message: String) {
        layout.boxStrokeColor = COLOR_ERROR_RED
        requirementContainer.visibility = View.VISIBLE
        reqText.text = message
        reqText.setTextColor(COLOR_ERROR_RED)
    }

    private fun showValidState(layout: TextInputLayout, requirementContainer: View, reqText: TextView, message: String) {
        layout.error = null
        layout.boxStrokeColor = COLOR_SUCCESS_GREEN
        requirementContainer.visibility = View.VISIBLE
        reqText.text = message
        reqText.setTextColor(COLOR_SUCCESS_GREEN)
    }

    // =========================================================================
    // 2. KEYBOARD AND FOCUS HELPER 💡 ADDED THIS FUNCTION 💡
    // =========================================================================

    // 🚨 HELPER: Hides the keyboard and clears focus
    private fun hideKeyboardAndClearFocus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        currentFocus?.clearFocus()
    }

    // =========================================================================
    // 3. CUSTOM ALERT DIALOG FUNCTION
    // =========================================================================

    /**
     * Shows a custom error dialog for server/database errors (e.g., email not found).
     */
    private fun showEmailErrorDialog(errorMessage: String = "Failed to send verification code. Please try again.") {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Apply custom styling for a rounded, centered, transparent dialog
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // Set custom title and message with actual error
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Error"
        
        // Format error message for user display
        val displayMessage = when {
            errorMessage.contains("PERMISSION_DENIED", ignoreCase = true) -> 
                "Permission denied. Please check your internet connection and try again."
            errorMessage.contains("network", ignoreCase = true) || errorMessage.contains("UNAVAILABLE", ignoreCase = true) -> 
                "Network error. Please check your internet connection and try again."
            errorMessage.contains("INTERNAL", ignoreCase = true) -> 
                "Server error. Please try again later."
            else -> 
                errorMessage.takeIf { it.isNotEmpty() } ?: "Failed to send verification code. Please try again."
        }
        
        dialogView.findViewById<TextView>(R.id.toast_value).text = displayMessage

        // Set click listener for the close button
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            // Clear the validation state of the email field on dialog close
            clearValidationState(layoutEmail)
        }

        // Trigger Red border via standard Material Error property (visual flag)
        layoutEmail.error = " "
        emailRequirementsContainer.visibility = View.GONE

        dialog.show()
    }


    // =========================================================================
    // 4. ONCREATE
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgotpassword)

        // 🚨 Initialize lateinit properties inside onCreate 🚨
        layoutEmail = findViewById(R.id.textInputLayoutEmail)
        inputEmail = findViewById(R.id.inputEmail)
        emailRequirementsContainer = findViewById(R.id.EmailRequirements)
        reqEmail = findViewById(R.id.reqEmail)
        btnSendVerification = findViewById(R.id.btnSendVerification)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val defaultEmailRequirementText = "• Email must end with @umak.edu.ph"

        // --- HELPER FUNCTIONS ---
        // Function to update the Send Code button's enabled/disabled state
        fun updateButtonState() {
            val emailText = inputEmail.text.toString().trim()
            // Button is enabled only if the field is NOT empty AND is a UMak email
            btnSendVerification.isEnabled = isFieldNotEmpty(emailText) && isUmakEmail(emailText)
        }

        // =====================================================================
        // 5. INITIAL SETUP & LISTENERS
        // =====================================================================

        // BACK BUTTON SETUP
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Initial state: Button is disabled and help text is visible
        btnSendVerification.isEnabled = false
        clearValidationState(layoutEmail)


        // ---------------------------------------------------------------------
        // 🔹 EMAIL FOCUS CHANGE HANDLING (Blur/Validation)
        // ---------------------------------------------------------------------
        inputEmail.setOnFocusChangeListener { _, hasFocus ->
            val email = inputEmail.text.toString().trim()

            if (hasFocus) {
                // --- WHEN FOCUSED ---
                // Reset to default/typing state
                clearValidationState(layoutEmail)
                reqEmail.text = defaultEmailRequirementText
                reqEmail.setTextColor(COLOR_HINT_GRAY)
                emailRequirementsContainer.visibility = View.VISIBLE
            } else {
                // --- WHEN UN-FOCUSED (BLUR) ---
                when {
                    email.isEmpty() -> {
                        // ❌ EMPTY on BLUR: Show required error
                        showValidationError(layoutEmail, emailRequirementsContainer, reqEmail, "• Field is required")
                    }
                    !isUmakEmail(email) -> {
                        // ❌ INVALID EMAIL on BLUR: Show email format error
                        showValidationError(layoutEmail, emailRequirementsContainer, reqEmail, defaultEmailRequirementText)
                    }
                    else -> {
                        // ✅ VALID on BLUR: Show success state (ready for submission)
                        showValidState(layoutEmail, emailRequirementsContainer, reqEmail, "✓ Please use your UMak email (@umak.edu.ph)")
                    }
                }
            }
            updateButtonState()
        }

        // ---------------------------------------------------------------------
        // 🔹 EMAIL REAL-TIME VALIDATION (Typing/Input Check)
        // ---------------------------------------------------------------------
        inputEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()

                when {
                    text.isEmpty() -> {
                        // 🩶 EMPTY (Default typing state)
                        clearValidationState(layoutEmail)
                        reqEmail.text = "• Field is required"
                        reqEmail.setTextColor(COLOR_HINT_GRAY)
                    }
                    !isUmakEmail(text) -> {
                        // ❌ INVALID FORMAT during typing
                        showValidationError(layoutEmail, emailRequirementsContainer, reqEmail, defaultEmailRequirementText)
                    }
                    else -> {
                        // ✅ VALID during typing
                        showValidState(layoutEmail, emailRequirementsContainer, reqEmail, "✓ Please use your UMak email (@umak.edu.ph)")
                    }
                }
                emailRequirementsContainer.visibility = View.VISIBLE
                updateButtonState()
            }
        })

        // =====================================================================
        // 6. SEND CODE BUTTON CLICK LISTENER
        // =====================================================================

        btnSendVerification.setOnClickListener {
            val email = inputEmail.text.toString().trim()

            if (!isUmakEmail(email)) {
                // Failsafe: Should not be hit if button is disabled correctly
                showValidationError(layoutEmail, emailRequirementsContainer, reqEmail, defaultEmailRequirementText)
                return@setOnClickListener
            }

            // Disable button during send
            btnSendVerification.isEnabled = false

            // Generate 6-digit verification code
            val verificationCode = VerificationCodeHelper.generateCode()
            
            // Save code to Firestore with expiration
            VerificationCodeHelper.saveVerificationCode(
                email = email,
                code = verificationCode,
                onSuccess = {
                    // Send verification code via email using Trigger Email extension
                    TriggerEmailHelper.sendPasswordResetCode(
                        email = email,
                        verificationCode = verificationCode,
                        userName = null,
                        onSuccess = {
                            // Navigate to verification code page
                            val intent = Intent(this, Verificationcode::class.java)
                            intent.putExtra(EXTRA_EMAIL_ADDRESS, email)
                            startActivity(intent)
                            finish()
                        },
                        onFailure = { errorMessage ->
                            // Re-enable button
                            btnSendVerification.isEnabled = true
                            
                            // Log error for debugging
                            Log.e("Forgotpassword", "Failed to send email: $errorMessage")
                            
                            // Show error dialog with actual error message
                            showEmailErrorDialog(errorMessage)
                        }
                    )
                },
                onFailure = { errorMessage ->
                    // Re-enable button
                    btnSendVerification.isEnabled = true
                    
                    // Log error for debugging
                    Log.e("Forgotpassword", "Failed to save verification code: $errorMessage")
                    
                    // Show error dialog with actual error message
                    showEmailErrorDialog(errorMessage)
                }
            )
        }
    } // End of onCreate

    // =========================================================================
    // 7. DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD) 💡 ADDED THIS OVERRIDE 💡
    // =========================================================================
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            // Only proceed if the current focus is a TextInputEditText
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                // Check if the click coordinates are outside the TextInputEditText bounds
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndClearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}