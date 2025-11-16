package com.example.umelec

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Color
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.app.AlertDialog
import android.widget.ImageButton
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.MotionEvent
import android.graphics.Rect
// 💡 REQUIRED IMPORTS FOR CUSTOM DIALOGS AND TIMER
import android.view.LayoutInflater
import android.view.Gravity
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.widget.Toast // <--- RESOLVES TOAST ERRORS
import android.text.Spannable // <--- NEW IMPORT
import android.text.SpannableString // <--- NEW IMPORT
import android.text.style.UnderlineSpan // <--- NEW IMPORT


class Verificationcode : AppCompatActivity() {

    // Define color constants (Same as your other activities)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#0039A6")
    private val COLOR_ERROR_RED = Color.parseColor("#D32F2F")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#4CAF50")

    // Define Views globally within the class for easier access in helper functions
    private lateinit var otpInputs: List<TextInputEditText>
    private lateinit var otpLayouts: List<TextInputLayout>
    private lateinit var btnSendVerification: Button
    private lateinit var textOTPtimer: TextView
    private lateinit var countDownTimer: CountDownTimer

    // Email address from intent
    private var userEmail: String = ""
    private val TIMER_DURATION_SECONDS = 60L
    private val RESEND_DIALOG_DURATION_MS = 2000L // 💡 Duration for the auto-dismiss resend dialog

    // =========================================================================
    // 💡 VALIDATION STATE HELPERS
    // =========================================================================

    private fun clearValidationState(layout: TextInputLayout) {
        layout.error = null
        layout.boxStrokeColor = COLOR_PRIMARY_BLUE
    }

    private fun showValidState(layout: TextInputLayout) {
        layout.error = null
        // Reverting to primary blue as requested by your commented out line/logic.
        layout.boxStrokeColor = COLOR_PRIMARY_BLUE
    }

    private fun showValidationError(layout: TextInputLayout) {
        layout.boxStrokeColor = COLOR_ERROR_RED
    }

    // 💡 KEYBOARD AND FOCUS HELPER
    private fun hideKeyboardAndClearFocus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        currentFocus?.clearFocus()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verificationcode)

        // Get email from intent
        userEmail = intent.getStringExtra(Forgotpassword.EXTRA_EMAIL_ADDRESS) ?: ""

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // =====================================================================
        // 🚨 1. INITIALIZATION 🚨
        // =====================================================================

        // Initialize OTP Input Fields
        otpInputs = listOf(
            findViewById(R.id.input1),
            findViewById(R.id.input2),
            findViewById(R.id.input3),
            findViewById(R.id.input4),
            findViewById(R.id.input5),
            findViewById(R.id.input6)
        )

        // 💡 Initialize OTP Layout Fields
        otpLayouts = listOf(
            findViewById(R.id.textInputLayout1),
            findViewById(R.id.textInputLayout2),
            findViewById(R.id.textInputLayout3),
            findViewById(R.id.textInputLayout4),
            findViewById(R.id.textInputLayout5),
            findViewById(R.id.textInputLayout6)
        )

        btnSendVerification = findViewById(R.id.btnSendVerification)
        textOTPtimer = findViewById(R.id.textOTPtimer)

        // Set initial button state to disabled
        btnSendVerification.isEnabled = false

        // Apply initial state to all fields
        otpLayouts.forEach { clearValidationState(it) }


        // =====================================================================
        // 🚨 2. OTP INPUT AUTOMATION AND VALIDATION 🚨
        // =====================================================================

        setupOtpListeners()

        // =====================================================================
        // 🚨 3. TIMER AND RESEND CODE LOGIC 🚨
        // =====================================================================

        startResendTimer()
        textOTPtimer.setOnClickListener {
            // Only clickable when the timer is finished and showing "Resend code"
            if (textOTPtimer.text.toString() == "Resend code") { // Ensure comparison is safe
                showResendSuccessToast()
            }
        }

        // =====================================================================
        // 🚨 4. CONFIRM BUTTON CLICK LOGIC 🚨
        // =====================================================================

        btnSendVerification.setOnClickListener {
            handleOtpConfirmation()
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 CORE LOGIC HELPERS 🔹
    // ---------------------------------------------------------------------

    private fun checkAllFieldsFilled(): Boolean {
        return otpInputs.all { it.text?.isNotEmpty() == true }
    }

    private fun handleOtpConfirmation() {
        val enteredCode = otpInputs.joinToString("") { it.text.toString() }

        if (userEmail.isEmpty()) {
            showVerificationFailureDialog("Email address not found. Please try again.")
            return
        }

        // Disable button during verification
        btnSendVerification.isEnabled = false

        // Verify code using VerificationCodeHelper
        VerificationCodeHelper.verifyCode(
            email = userEmail,
            code = enteredCode,
            onSuccess = {
                // Code is valid, generate Firebase Auth action code
                generatePasswordResetActionCode()
            },
            onFailure = { errorMessage ->
                // Re-enable button
                btnSendVerification.isEnabled = true
                
                // 🚨 Set all fields to error state on failure (Red border)
                otpLayouts.forEach { showValidationError(it) }
                showVerificationFailureDialog(errorMessage)
            }
        )
    }

    private fun updateButtonState() {
        btnSendVerification.isEnabled = checkAllFieldsFilled()
    }

    private fun clearOtpFields() {
        otpInputs.forEachIndexed { index, input ->
            input.setText("")
            clearValidationState(otpLayouts[index]) // Clear validation state when clearing fields
        }
        otpInputs.first().requestFocus()
    }

    // =========================================================================
    // 🚨 5. CUSTOM DIALOG/TOAST IMPLEMENTATIONS 🚨
    // =========================================================================

    /**
     * Generate password reset action code after verification code is verified
     */
    private fun generatePasswordResetActionCode() {
        FirebaseAuthHelper.generatePasswordResetActionCode(
            email = userEmail,
            onSuccess = { actionCode ->
                // Action code generated successfully, proceed to reset password
                showVerificationSuccessDialog(actionCode)
            },
            onFailure = { errorMessage ->
                // Re-enable button
                btnSendVerification.isEnabled = true
                
                // Show error dialog
                showVerificationFailureDialog("Failed to proceed: $errorMessage")
            }
        )
    }

    /**
     * 1. Success Dialog (imitate custom_toast_success)
     */
    private fun showVerificationSuccessDialog(actionCode: String) {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_success, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        dialogView.findViewById<TextView>(R.id.toast_title).text = "Code Verified"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Proceed to reset password."

        val btnAction = dialogView.findViewById<Button>(R.id.btn_action)
        btnAction.text = "Continue"
        btnAction.setOnClickListener {
            dialog.dismiss()

            // Proceed to Resetpassword.kt with action code
            val intent = Intent(this, Resetpassword::class.java)
            intent.putExtra("actionCode", actionCode)
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }

        dialog.show()
    }

    /**
     * 2. Resend Success TOAST (replaces AlertDialog)
     * Displays a custom Toast and automatically starts the timer after the Toast duration.
     */
    private fun showResendSuccessToast() {
        if (userEmail.isEmpty()) {
            return
        }

        // Generate new code
        val newCode = VerificationCodeHelper.generateCode()

        // Disable resend button during send
        textOTPtimer.isClickable = false

        // Resend code
        VerificationCodeHelper.resendCode(
            email = userEmail,
            code = newCode,
            onSuccess = {
                // Send email with new code using Trigger Email extension
                TriggerEmailHelper.sendPasswordResetCode(
                    email = userEmail,
                    verificationCode = newCode,
                    userName = null,
                    onSuccess = {
                        val layoutInflater = LayoutInflater.from(this)
                        val layout = layoutInflater.inflate(R.layout.custom_toast_success, null)

                        // Find and customize the views
                        val titleText: TextView = layout.findViewById(R.id.toast_title)
                        val valueText: TextView = layout.findViewById(R.id.toast_value)
                        val actionButton: Button = layout.findViewById(R.id.btn_action)

                        // Set content and hide button (Toast should be non-interactive)
                        titleText.text = "Code sent!"
                        valueText.text = "Check your UMak email inbox."
                        actionButton.visibility = View.GONE // Hide the button

                        // We use Toast.LENGTH_SHORT (approx. 2000ms)
                        val toastDurationMs = 2000L

                        with (Toast(applicationContext)) {
                            duration = Toast.LENGTH_SHORT
                            // Set the custom gravity and offset
                            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
                            view = layout
                            show()
                        }

                        // Schedule the timer restart to happen right after the Toast duration ends.
                        Handler(Looper.getMainLooper()).postDelayed({
                            startResendTimer()
                        }, toastDurationMs)
                    },
                    onFailure = { errorMessage ->
                        // Re-enable resend button
                        textOTPtimer.isClickable = true
                        // Show error toast
                        Toast.makeText(this, "Failed to send code: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = { errorMessage ->
                // Re-enable resend button
                textOTPtimer.isClickable = true
                // Show error toast
                Toast.makeText(this, "Failed to resend code: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * 3. Failure Dialog (imitate custom_toast_error)
     */
    private fun showVerificationFailureDialog(errorMessage: String = "Incorrect Code.") {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        dialogView.findViewById<TextView>(R.id.toast_title).text = "Code error"
        dialogView.findViewById<TextView>(R.id.toast_value).text = errorMessage

        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            // 💡 Reset the red border and clear fields
            clearOtpFields()
        }

        // 💡 Trigger Red border via standard Material Error property (visual flag)
        otpLayouts.forEach { it.error = " " }

        dialog.show()
    }


    // ---------------------------------------------------------------------
    // 🔹 OTP LISTENERS (Auto-Focus and Delete) 🔹
    // ---------------------------------------------------------------------
    private fun setupOtpListeners() {
        otpInputs.forEachIndexed { index, input ->
            val layout = otpLayouts[index]

            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    clearValidationState(layout)
                } else {
                    if (input.text.isNullOrEmpty()) {
                        showValidationError(layout)
                    } else {
                        showValidState(layout)
                    }
                }
            }

            input.addTextChangedListener(object : TextWatcher {
                private var currentTextLength = 0

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    currentTextLength = s?.length ?: 0
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    updateButtonState()

                    val newText = s.toString()

                    // Check if a character was ADDED (newText.length > currentTextLength)
                    // and if that new character is a digit (because inputType="number" still allows the event)
                    if (newText.length == 1 && currentTextLength == 0) {

                        // 💡 NEW LOGIC: Only move focus if the character is confirmed to be a digit.
                        // Since inputType="number" is set, this check should cover it, but checking
                        // the actual content length is the most reliable way to handle the shift.
                        if (newText.first().isDigit()) {
                            showValidState(layout)
                            if (index < otpInputs.size - 1) {
                                // Automatically move focus to the next field
                                otpInputs[index + 1].requestFocus()
                            }
                        }
                    } else if (newText.isEmpty()) {
                        // Reset when character is deleted
                        clearValidationState(layout)
                    }

                    // Failsafe for copy-paste or unexpected multiple input (keep only the first char)
                    if (newText.length > 1) {
                        input.setText(newText.substring(0, 1))
                        input.setSelection(1)
                    }
                }
            })

            input.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (input.text.isNullOrEmpty() && index > 0) {
                        otpInputs[index - 1].requestFocus()
                        otpInputs[index - 1].setSelection(otpInputs[index - 1].text?.length ?: 0)
                        return@setOnKeyListener true
                    } else if (input.text?.isNotEmpty() == true) {
                        return@setOnKeyListener false
                    }
                }
                false
            }
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 TIMER LOGIC 🔹
    // ---------------------------------------------------------------------
    private fun startResendTimer() {
        otpLayouts.forEach { clearValidationState(it) }

        textOTPtimer.setTextColor(Color.parseColor("#8C8CA1"))
        textOTPtimer.isClickable = false

        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }

        countDownTimer = object : CountDownTimer(TIMER_DURATION_SECONDS * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                textOTPtimer.text = "Resend code in $seconds seconds"
            }

            override fun onFinish() {
                val resendText = "Resend code"

                // 1. Change text color
                textOTPtimer.setTextColor(Color.parseColor("#318CE7"))

                // 2. Make it clickable
                textOTPtimer.isClickable = true

                // 3. Apply underline span
                val spannableString = SpannableString(resendText)
                spannableString.setSpan(
                    UnderlineSpan(),
                    0,
                    resendText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textOTPtimer.text = spannableString // Set the formatted text
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }

    // =========================================================================
    // 💡 DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD)
    // =========================================================================
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndClearFocus()

                    otpLayouts.forEachIndexed { index, layout ->
                        if (otpInputs[index].text.isNullOrEmpty()) {
                            showValidationError(layout)
                        } else {
                            showValidState(layout)
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}