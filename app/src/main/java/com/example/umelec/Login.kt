package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.view.Gravity
import android.view.LayoutInflater
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import android.view.inputmethod.InputMethodManager
import android.content.Context

// 1. 🚀 NEW: Data class to handle user roles and verification
data class User(
    val email: String,
    val password: String,
    val role: String, // "VOTER" or "LEADER"
    val userName: String,
    val isVerified: Boolean = false // Only relevant for LEADER role
)

class Login : AppCompatActivity() {

    // Firebase Auth Helper (No hardcoded credentials)
    private val CORRECT_DOMAIN = "@umak.edu.ph"

    // Define color constants (Used only for requirements TextView text colors)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_ERROR_RED = Color.parseColor("#D33131")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#27A688")
    private val COLOR_HINT_GRAY = Color.parseColor("#5C5C77")

    // Define field layouts outside onCreate
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        // =====================================================================
        // 🚨 1. VIEWS AND HELPER FUNCTIONS (INITIALIZATION) 🚨
        // =====================================================================

        // --- EMAIL VIEWS ---
        layoutEmail = findViewById(R.id.textInputLayoutEmail)
        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val emailRequirementsContainer = findViewById<View>(R.id.EmailRequirements)
        val reqEmail = findViewById<TextView>(R.id.reqEmail)

        // --- PASSWORD VIEWS ---
        layoutPassword = findViewById(R.id.textInputLayoutPassword)
        val inputPassword = findViewById<TextInputEditText>(R.id.inputPassword)
        val passwordRequirementsContainer = findViewById<View>(R.id.PasswordRequirements)
        val reqPassword = findViewById<TextView>(R.id.reqPassword)

        // --- BUTTONS ---
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val forgotPassword = findViewById<TextView>(R.id.ForgotPassword)
        val registerButton = findViewById<TextView>(R.id.RegisterButton)

        /**
         * 🟢 NEW HELPER: Hides the keyboard and clears focus from any active view.
         */
        fun hideKeyboardAndClearFocus() {
            // 1. Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            // 2. Clear focus from the currently focused view (like the EditText)
            currentFocus?.clearFocus()
        }

        // --- HELPER FUNCTIONS ---

        // Checks if email matches the required domain
        fun isEmailValid(email: String): Boolean {
            return email.isNotEmpty() && email.endsWith(CORRECT_DOMAIN, ignoreCase = true)
        }

        // Checks if password field is not empty (client-side check)
        fun isPasswordValid(password: String): Boolean {
            return password.isNotEmpty()
        }

        fun updateLoginButtonState() {
            val emailText = inputEmail.text.toString().trim()
            val passwordText = inputPassword.text.toString()

            // Button is enabled if both fields pass basic validation
            val allFieldsValid = isEmailValid(emailText) && isPasswordValid(passwordText)

            btnLogin.isEnabled = allFieldsValid
        }

        /**
         * 🚀 Standard Success Dialog for Voter or Verified Leader.
         */
        fun showLoginSuccessDialog(userName: String, targetActivity: Class<*>) {
            val layoutInflater = LayoutInflater.from(this)
            val dialogView = layoutInflater.inflate(R.layout.custom_toast_success, null)

            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView)
            val dialog = builder.create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.setCanceledOnTouchOutside(false)

            dialogView.findViewById<TextView>(R.id.toast_title).text = "Login Success"
            dialogView.findViewById<TextView>(R.id.toast_value).text = "Welcome back!"

            val btnAction = dialogView.findViewById<Button>(R.id.btn_action)
            btnAction.text = "Continue to Homepage" // General button text
            btnAction.setOnClickListener {
                dialog.dismiss()

                // Navigate to the dynamic target activity
                val intent = Intent(this, targetActivity)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            }

            dialog.show()
        }

        /**
         * 🚫 REMOVED: showUnverifiedLeaderDialog function is removed per user request.
         * Unverified leaders now navigate directly to the verification screen.
         */

        fun clearValidationState(layout: TextInputLayout) {
            layout.error = null // Clears RED border/error text
            layout.isActivated = false // Clears GREEN border
        }

        fun showLoginErrorDialog() {
            val layoutInflater = LayoutInflater.from(this)
            val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView)
            val dialog = builder.create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.setCanceledOnTouchOutside(false)

            dialogView.findViewById<TextView>(R.id.toast_title).text = "Login Error"
            dialogView.findViewById<TextView>(R.id.toast_value).text = "Invalid credentials. Please try again."

            dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
                dialog.dismiss()
                clearValidationState(layoutEmail)
                clearValidationState(layoutPassword)
            }

            // Trigger Red border via standard Material Error property (used as a visual flag)
            layoutEmail.error = " "
            layoutPassword.error = " "

            dialog.show()
        }


        // =====================================================================
        // 🚨 2. INITIAL SETUP & LISTENERS 🚨
        // =====================================================================

        // [Omitted: Unrelated setup code is unchanged]

        btnBack.setOnClickListener {
            if (isTaskRoot) {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            } else {
                finish()
                @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            }
        }


        forgotPassword.setOnClickListener {
            startActivity(Intent(this, Forgotpassword::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        btnLogin.isEnabled = false
        emailRequirementsContainer.visibility = View.GONE
        passwordRequirementsContainer.visibility = View.GONE

        // 🌟 NEW BEHAVIOR: CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD 🌟
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnClickListener {
            hideKeyboardAndClearFocus()
            clearValidationState(layoutEmail)
            clearValidationState(layoutPassword)
        }

        // ---------------------------------------------------------------------
        // 🔹 EMAIL FOCUS CHANGE HANDLING (MODIFIED DOMAIN REFERENCE)
        // ---------------------------------------------------------------------

        inputEmail.setOnFocusChangeListener { _, hasFocus ->
            val email = inputEmail.text.toString().trim()
            val isValid = isEmailValid(email)

            if (hasFocus) {
                emailRequirementsContainer.visibility = View.VISIBLE
            } else {
                when {
                    email.isEmpty() -> {
                        emailRequirementsContainer.visibility = View.VISIBLE
                        reqEmail.setTextColor(COLOR_ERROR_RED)
                        reqEmail.text = "• Field is required"
                        layoutEmail.error = " "
                        layoutEmail.isActivated = false
                    }
                    isValid -> {
                        emailRequirementsContainer.visibility = View.GONE
                        layoutEmail.error = null
                        layoutEmail.isActivated = true
                    }
                    else -> {
                    emailRequirementsContainer.visibility = View.VISIBLE
                    reqEmail.setTextColor(COLOR_ERROR_RED)
                    reqEmail.text = "• Please use your UMak email ($CORRECT_DOMAIN)"
                    layoutEmail.error = " "
                    layoutEmail.isActivated = false
                    }
                }
            }
            updateLoginButtonState()
        }

        // ---------------------------------------------------------------------
        // 🔹 EMAIL REAL-TIME VALIDATION (MODIFIED DOMAIN REFERENCE)
        // ---------------------------------------------------------------------

        inputEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutEmail)
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                val isValid = isEmailValid(text)

                // 2. 🔹 Validation Logic
                when {
                    // ✅ VALID MATCH: Complete and correct domain
                    isValid -> {
                        reqEmail.setTextColor(COLOR_SUCCESS_GREEN)
                        reqEmail.text = "✓ Please use your UMak email ($CORRECT_DOMAIN)"
                        emailRequirementsContainer.visibility = View.VISIBLE

                        // 🟢 LIVE FEEDBACK: Set to Green border
                        layoutEmail.boxStrokeColor = COLOR_SUCCESS_GREEN
                        layoutEmail.isActivated = true  // Triggers GREEN border
                    }

                    // ❌ INVALID FORMAT: Contains text but doesn't end with required domain
                    text.isNotEmpty() && !text.endsWith(CORRECT_DOMAIN, ignoreCase = true) -> {
                        reqEmail.setTextColor(COLOR_ERROR_RED)
                        reqEmail.text = "• Please use your UMak email ($CORRECT_DOMAIN)"
                        emailRequirementsContainer.visibility = View.VISIBLE

                        // 🔴 LIVE FEEDBACK: Set to Red border
                        layoutEmail.isActivated = false // Clear green border
                        layoutEmail.error = " "         // Triggers RED border
                    }

                    // 🩶 DEFAULT TYPING STATE: Empty or still typing
                    else -> {
                        reqEmail.setTextColor(COLOR_HINT_GRAY)
                        reqEmail.text = "• Please use your UMak email ($CORRECT_DOMAIN)"
                        emailRequirementsContainer.visibility = View.VISIBLE

                        // Reset to default/primary color border while actively typing
                        layoutEmail.isActivated = false
                        layoutEmail.error = null
                    }
                }
                updateLoginButtonState()
            }
        })

        // ---------------------------------------------------------------------
        // 🔹 PASSWORD FOCUS CHANGE HANDLING (NO CHANGE)
        // ---------------------------------------------------------------------

        inputPassword.setOnFocusChangeListener { _, hasFocus ->
            val password = inputPassword.text.toString()
            val isValid = isPasswordValid(password)

            if (hasFocus) {
                clearValidationState(layoutPassword)
            } else {
                when {
                    password.isEmpty() -> {
                        passwordRequirementsContainer.visibility = View.VISIBLE
                        reqPassword.setTextColor(COLOR_ERROR_RED)
                        reqPassword.text = "• Field is required"
                        layoutPassword.error = " "
                        layoutPassword.isActivated = false
                    }
                    isValid -> {
                        passwordRequirementsContainer.visibility = View.GONE
                        layoutPassword.error = null
                        layoutPassword.isActivated = true
                    }
                }
            }
            updateLoginButtonState()
        }

        // ---------------------------------------------------------------------
        // 🔹 PASSWORD REAL-TIME VALIDATION (NO CHANGE)
        // ---------------------------------------------------------------------

        inputPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutPassword)
                passwordRequirementsContainer.visibility = View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
                updateLoginButtonState()
            }
        })

        // =====================================================================
        // 🚨 3. LOGIN BUTTON CLICK LISTENER (Final Step) 🚨
        // =====================================================================

        btnLogin.setOnClickListener {
            hideKeyboardAndClearFocus()

            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString()

            // Validate inputs
            if (!isEmailValid(email) || !isPasswordValid(password)) {
                showLoginErrorDialog()
                return@setOnClickListener
            }

            // Disable button during login
            btnLogin.isEnabled = false

            // Firebase Authentication
            FirebaseAuthHelper.signIn(
                email = email,
                password = password,
                onSuccess = { user ->
                    // Clear any error state before navigating
                    layoutEmail.error = null
                    layoutPassword.error = null
                    
                    // Get user role and verification status from Firestore
                    FirebaseAuthHelper.getUserDataFromFirestore(
                        userId = user.uid,
                        onSuccess = { userData ->
                            val role = userData?.get("role") as? String ?: "VOTER"
                            val isVerified = userData?.get("isVerified") as? Boolean ?: false
                            val userName = userData?.get("firstname") as? String ?: (user.displayName ?: email)
                            
                            // Route based on role and verification status
                            when (role) {
                                "LEADER" -> {
                                    if (isVerified) {
                                        // 🚀 LEADER (Verified): Show standard success and go to Leader Homepage
                                        showLoginSuccessDialog(userName, Leader_homepage::class.java)
                                    } else {
                                        // 🚀 LEADER (Unverified): Go directly to verification screen
                                        val intent = Intent(this, Leader_Verification::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                        @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                                    }
                                }
                                else -> {
                                    // 🚀 VOTER: Show standard success and go to Homepage
                                    showLoginSuccessDialog(userName, Homepage::class.java)
                                }
                            }
                        },
                        onFailure = { errorMessage ->
                            // If we can't get user data, default to Voter homepage
                            val userName = user.displayName ?: email
                            showLoginSuccessDialog(userName, Homepage::class.java)
                        }
                    )
                },
                onFailure = { errorMessage ->
                    // Re-enable button
                    btnLogin.isEnabled = true
                    // Show error dialog
                    showLoginErrorDialog()
                }
            )
        }
    }
}