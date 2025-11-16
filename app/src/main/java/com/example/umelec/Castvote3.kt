package com.example.umelec

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Handler
import android.os.Looper

// REMINDER: You MUST add the following dependency to your app/build.gradle file:
// implementation 'com.github.gcacace:signature-pad:1.2.1'

class Castvote3 : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var signaturePad: SignaturePad
    private lateinit var btnClearSignature: AppCompatButton
    private lateinit var btnSubmit: Button // This corresponds to btnNext in XML

    // State
    private var isSignatureDrawn: Boolean = false
    private lateinit var reviewedPositions: List<String>
    private lateinit var reviewedCandidates: List<String>
    private var signatureBase64String: String? = null // To store the signature data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_castvote3)

        // 1. Initialize Views
        btnBack = findViewById(R.id.btnBack)
        signaturePad = findViewById(R.id.signaturePad)
        btnClearSignature = findViewById(R.id.btnClearSignature)
        btnSubmit = findViewById(R.id.btnSubmit) // Using the ID from XML: btnNext

        // 2. Retrieve Data from Castvote2.kt
        reviewedPositions = intent.getStringArrayListExtra("positions") ?: emptyList()
        reviewedCandidates = intent.getStringArrayListExtra("candidates") ?: emptyList()

        // 3. Setup Initial State
        updateSubmitButtonState()
        btnClearSignature.isEnabled = false

        // 4. Set Listeners
        // Back Button: Goes back to Castvote2 with NO WARNING, as requested.
        btnBack.setOnClickListener {
            // Since this only navigates back to Castvote2 (review screen), we simply finish().
            finish()
            overridePendingTransition(0, 0)
        }

        // 5. Working Signature Pad Logic
        signaturePad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() { /* Not used */ }

            override fun onSigned() {
                // Signature drawn: enable clear and submit buttons
                isSignatureDrawn = true
                btnClearSignature.isEnabled = true
                updateSubmitButtonState()
            }

            override fun onClear() {
                // Signature cleared: disable clear and submit buttons, reset data
                isSignatureDrawn = false
                btnClearSignature.isEnabled = false
                signatureBase64String = null
                updateSubmitButtonState()
            }
        })

        // 6. Clear Signature Button Logic
        btnClearSignature.setOnClickListener {
            signaturePad.clear() // Clears the canvas and triggers the onClear listener
        }

        // 7. Submit Button Logic
        btnSubmit.setOnClickListener {
            if (isSignatureDrawn) {
                // 1. Capture and convert signature
                val signatureBitmap: Bitmap = signaturePad.getSignatureBitmap()
                val byteArrayOutputStream = ByteArrayOutputStream()
                signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                signatureBase64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // 2. Show the final confirmation dialog before submission
                showFinalConfirmationDialog()
            }
        }
    }

    /**
     * Updates the enabled state of the Submit button based on required conditions.
     * The visual appearance (color) is handled automatically by the blue_rounded_button.xml selector.
     */
    private fun updateSubmitButtonState() {
        // Button is enabled ONLY when a signature is drawn
        btnSubmit.isEnabled = isSignatureDrawn
    }

    /**
     * Creates an AlertDialog with transparent background, centered gravity, and custom touch outside behavior.
     */
    private fun createStyledAlertDialog(dialogView: View, isCancellable: Boolean = true): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(isCancellable)

        return dialog
    }

    // ----------------------------------------------------------------------
    // --- CONFIRMATION AND SUBMISSION DIALOGS (Moved from Castvote2.kt) ---
    // ----------------------------------------------------------------------

    /**
     * Displays the FINAL CONFIRMATION dialog before submitting the vote.
     */
    private fun showFinalConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_question, null)
        val alertDialog = createStyledAlertDialog(dialogView)

        dialogView.findViewById<TextView>(R.id.toast_title).text = "Submit your vote?"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "This action cannot be undone."
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary).text = "Cancel"
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary).text = "Confirm"

        dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary).setOnClickListener {
            alertDialog.dismiss()

            // --- FAKE BACKEND SUBMISSION ---
            println("--- VOTE SUBMISSION PAYLOAD ---")
            println("Positions: $reviewedPositions")
            println("Candidates: $reviewedCandidates")
            println("Signature Base64: ${signatureBase64String?.substring(0, 50)}...")
            println("-----------------------------")

            // Show the recorded dialog with proof of submission
            showRecordedDialog()
        }

        alertDialog.show()
    }


    /**
     * Displays the VOTE RECORDED receipt dialog using custom_toast_recorded.xml.
     */
    private fun showRecordedDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_recorded, null)
        // This dialog CANNOT be dismissed by touching outside.
        val alertDialog = createStyledAlertDialog(dialogView, isCancellable = false)

        // --- MOCK DATA GENERATION ---
        // TODO: Replace this mock logic with actual data fetched from the backend after submission.
        val refCode = "ELEC-2025-" + (1000000 + (Math.random() * 9000000).toInt())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val refDate = dateFormat.format(Date())
        // Use a shortened version of the actual signature data for the "receipt"
        val refSignature = signatureBase64String?.take(8) + "..." ?: "N/A"

        // --- Set Mock Data ---
        dialogView.findViewById<TextView>(R.id.ReferenceCode).text = refCode
        dialogView.findViewById<TextView>(R.id.ReferenceDate).text = refDate
        dialogView.findViewById<TextView>(R.id.ReferenceSignature).text = refSignature

        // --- Button Handlers ---

        // Download PDF Button (btn_action_primary)
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary).setOnClickListener {
            // TODO: Implement PDF generation/download logic here
            Toast.makeText(this, "Your PDF receipt has been downloaded.", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()

            // Navigate away after final action
            navigateTo(Homepage::class.java, isFinalExit = true)
        }

        // Send to Email Button (btn_action_secondary)
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary).setOnClickListener {
            // TODO: Implement email sending logic here
            alertDialog.dismiss() // Dismiss the recorded dialog
            showSuccessToastAndNavigate() // <-- CALLS THE NEW TOAST FUNCTION

            // Navigation is handled inside showSuccessToastAndNavigate
        }

        alertDialog.show()
    }


    /**
     * DISPLAYS A CUSTOM TOAST and then navigates to the Homepage.
     * Replaces the previous AlertDialog implementation.
     */
    private fun showSuccessToastAndNavigate() {
        val inflater = LayoutInflater.from(this)
        // Inflate the custom toast layout
        val layout = inflater.inflate(R.layout.custom_toast_success, null)

        // Find and customize the views
        val titleText: TextView = layout.findViewById(R.id.toast_title)
        val valueText: TextView = layout.findViewById(R.id.toast_value)
        val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

        // Set content and hide button (Toast should be non-interactive)
        titleText.text = "Sent Successfully"
        valueText.text = "Check your email."
        actionButton.visibility = View.GONE // Hide the button

        with (Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            // Set the custom gravity and offset
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            view = layout
            show()
        }

        // Crucial: Schedule the navigation on the main thread after a minimal delay (e.g., 40ms).
        // This allows the Toast rendering command to be processed before the current activity is destroyed.
        Handler(Looper.getMainLooper()).postDelayed({
            // Execute the final action (Navigation/Exit)
            navigateTo(Homepage::class.java, isFinalExit = true)
        }, 40) // 40 milliseconds is usually enough for the Toast to register
    }

    /**
     * Helper function to handle navigation.
     */
    private fun navigateTo(activityClass: Class<*>, isFinalExit: Boolean = false) {
        val intent = Intent(this, activityClass)

        if (isFinalExit) {
            // Use flags to clear the activity stack and go to the root activity (Homepage)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        } else {
            // Default navigation behavior (e.g., going back to Castvote2)
            startActivity(intent)
            finish() // Since we are navigating back, we finish the current activity
            overridePendingTransition(0, 0)
        }
    }
}