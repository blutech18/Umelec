package com.example.umelec

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.ByteArrayOutputStream

// REMINDER: You MUST add the following dependency to your app/build.gradle file:
// implementation 'com.github.gcacace:signature-pad:1.2.1'

class RegisterActivity3 : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var signaturePad: SignaturePad
    private lateinit var btnClearSignature: AppCompatButton
    private lateinit var tvReviewTnC: TextView
    private lateinit var tvReviewPrivacy: TextView
    private lateinit var cbAgree: CheckBox
    private lateinit var cbAgreePrivacy: CheckBox
    private lateinit var cbAgreeTerms: CheckBox
    private lateinit var btnNext: Button

    // State
    private var isSignatureDrawn: Boolean = false
    private var isTermsAgreed: Boolean = false
    private var isPrivacyAgreed: Boolean = false
    private var isFinalConditionAgreed: Boolean = false

    // Assumed target classes for navigation
    private val LOGIN_ACTIVITY_CLASS = Login::class.java
    private val TERMS_ACTIVITY_CLASS = Terms::class.java // Assumed Terms.kt exists
    private val PRIVACY_ACTIVITY_CLASS = Privacy::class.java // Assumed Privacy.kt exists

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register3)

        // 1. Initialize Views
        btnBack = findViewById(R.id.btnBack)
        signaturePad = findViewById(R.id.signaturePad)
        btnClearSignature = findViewById(R.id.btnClearSignature)

        // --- NEW/UPDATED VIEW INITIALIZATION ---
        tvReviewTnC = findViewById(R.id.tvReviewTnC)
        tvReviewPrivacy = findViewById(R.id.tvReviewPrivacy)
        cbAgree = findViewById(R.id.cbAgree) // The final condition checkbox
        cbAgreePrivacy = findViewById(R.id.cbAgreePrivacy)
        cbAgreeTerms = findViewById(R.id.cbAgreeTerms)
        // --- END NEW/UPDATED VIEW INITIALIZATION ---

        btnNext = findViewById(R.id.btnNext)

        // 2. Setup Initial State
        updateNextButtonState()
        btnClearSignature.isEnabled = false
        // Note: isTermsAgreed now tracks cbAgreeTerms, cbAgreePrivacy, and cbAgree.
        // Let's ensure the initial state reflects the unchecked boxes.
        isTermsAgreed = cbAgreeTerms.isChecked
        isPrivacyAgreed = cbAgreePrivacy.isChecked
        isFinalConditionAgreed = cbAgree.isChecked
        updateNextButtonState()


        // 3. Set Listeners
        btnBack.setOnClickListener {
            finish() // Goes back to the previous activity (RegisterActivity2)
            overridePendingTransition(0, 0)
        }

        // ⭐️ NEW BEHAVIOR: Navigate to Terms.kt ⭐️
        tvReviewTnC.setOnClickListener {
            val intent = Intent(this, TERMS_ACTIVITY_CLASS)
            startActivity(intent)
        }

        // ⭐️ NEW BEHAVIOR: Navigate to Privacy.kt ⭐️
        tvReviewPrivacy.setOnClickListener {
            val intent = Intent(this, PRIVACY_ACTIVITY_CLASS)
            startActivity(intent)
        }

        // --- NEW CHECKBOX LISTENERS ---

        // Listener for the first new CheckBox (Terms)
        cbAgreeTerms.setOnCheckedChangeListener { _, isChecked ->
            isTermsAgreed = isChecked
            updateNextButtonState()
        }

        // Listener for the second new CheckBox (Privacy)
        cbAgreePrivacy.setOnCheckedChangeListener { _, isChecked ->
            isPrivacyAgreed = isChecked
            updateNextButtonState()
        }

        // Listener for the original CheckBox (Final Condition)
        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            isFinalConditionAgreed = isChecked
            updateNextButtonState()
        }

        // 4. Working Signature Pad Logic (untouched)
        signaturePad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {
                // Not needed for state tracking, but useful for UX hints
            }

            override fun onSigned() {
                // Triggered when the user starts drawing and lifts their finger
                isSignatureDrawn = true
                btnClearSignature.isEnabled = true
                updateNextButtonState()
            }

            override fun onClear() {
                // Triggered when signaturePad.clear() is called
                isSignatureDrawn = false
                btnClearSignature.isEnabled = false
                updateNextButtonState()
            }
        })

        // 5. Clear Signature Button Logic (untouched)
        btnClearSignature.setOnClickListener {
            signaturePad.clear() // Clears the canvas and triggers the onClear listener
        }

        // 6. Next Button Logic (Data Processing) (untouched except for state check)
        btnNext.setOnClickListener {
            if (isSignatureDrawn && isTermsAgreed) {
                // Get current Firebase user
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser == null) {
                    // User not authenticated, redirect to login
                    val intent = Intent(this, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@setOnClickListener
                }

                // Disable button during save
                btnNext.isEnabled = false

                val signatureBitmap: Bitmap = signaturePad.getSignatureBitmap()
                val byteArrayOutputStream = ByteArrayOutputStream()
                signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val signatureBase64String: String = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // 3. Prepare signature data for Firestore
                val signatureData = hashMapOf<String, Any>(
                    "signature" to signatureBase64String,
                    "termsAgreed" to isTermsAgreed,
                    "registrationCompleted" to true,
                    "completedAt" to com.google.firebase.Timestamp.now()
                )

                // 4. Update user document in Firestore with signature
                FirebaseAuthHelper.updateUserDataInFirestore(
                    userId = currentUser.uid,
                    userData = signatureData,
                    onSuccess = {
                        // Clear temporary credentials
                        FirebaseAuthHelper.clearTemporaryCredentials(this)
                        // Proceed to success dialog
                        showRegistrationSuccessDialog()
                    },
                    onFailure = { errorMessage ->
                        // Re-enable button
                        btnNext.isEnabled = true
                        // Show error (you might want to create a proper error dialog here)
                        android.widget.Toast.makeText(this, "Failed to save signature: $errorMessage", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    /**
     * Updates the enabled state of the Next button based on required conditions.
     * ⭐️ UPDATED LOGIC to check all three CheckBoxes. ⭐️
     */
    private fun updateNextButtonState() {
        // Button is enabled ONLY when:
        // 1. Signature is drawn (isSignatureDrawn)
        // 2. Terms CheckBox is checked (isTermsAgreed)
        // 3. Privacy CheckBox is checked (isPrivacyAgreed)
        // 4. Final Condition CheckBox is checked (isFinalConditionAgreed)

        btnNext.isEnabled = isSignatureDrawn && isTermsAgreed && isPrivacyAgreed && isFinalConditionAgreed
    }

    /**
     * Custom AlertDialog for Registration Success. (untouched)
     */
    private fun showRegistrationSuccessDialog() {
        val layoutInflater = LayoutInflater.from(this)
        // Ensure you have R.layout.custom_toast_success in your resources
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_success, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Set dialog properties
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // Set custom title and value
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Registration Success"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Your account has been created."

        val btnAction = dialogView.findViewById<Button>(R.id.btn_action)
        btnAction.text = "Continue to Login"

        btnAction.setOnClickListener {
            dialog.dismiss()

            // Proceed to Login Activity
            val intent = Intent(this, LOGIN_ACTIVITY_CLASS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}