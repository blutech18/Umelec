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
import kotlin.collections.ArrayList
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

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
    private var selectionsDataBundle: android.os.Bundle? = null
    private var electionId: String? = null
    private var currentUserId: String? = null
    private var currentUserName: String? = ""
    private var currentUserEmail: String? = ""
    private var electionTitle: String? = "Election"
    private var voteReceiptData: ReceiptPdfHelper.ReceiptData? = null

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
        selectionsDataBundle = intent.getBundleExtra("selectionsData")
        electionId = intent.getStringExtra("electionId")
        
        // Get current user ID and info
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUserId = currentUser?.uid
        currentUserEmail = currentUser?.email ?: ""
        
        // Get user name from Firestore
        currentUserId?.let { userId: String ->
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = userId,
                onSuccess = { userData: Map<String, Any>? ->
                    currentUserName = userData?.get("name") as? String ?: userData?.get("firstName") as? String ?: ""
                    if (currentUserName.isNullOrBlank()) {
                        currentUserName = currentUserEmail?.substringBefore("@") ?: "User"
                    }
                },
                onFailure = { error: String ->
                    // Use email as fallback
                    currentUserName = currentUserEmail?.substringBefore("@") ?: "User"
                }
            )
        }
        
        // Get election title
        electionId?.let { id: String ->
            FirestoreElectionHelper.getCurrentElection(
                onSuccess = { electionData: ElectionDetails? ->
                    electionTitle = electionData?.title ?: "Election"
                },
                onFailure = { error: String ->
                    electionTitle = "Election"
                }
            )
        }

        // 3. Setup Initial State
        updateSubmitButtonState()
        btnClearSignature.isEnabled = false

        // 4. Set Listeners
        // Back Button: Goes back to Castvote2 with NO WARNING, as requested.
        btnBack.setOnClickListener {
            // Since this only navigates back to Castvote2 (review screen), we simply finish().
            finish()
            @Suppress("DEPRECATION")
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

            // Submit vote to Firestore
            submitVote()
        }

        alertDialog.show()
    }


    /**
     * Submits the vote to Firestore
     */
    private fun submitVote() {
        val userId = currentUserId
        val electionIdValue = electionId
        
        if (userId == null || electionIdValue == null) {
            Toast.makeText(this, "Error: User or election not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Reconstruct selections map from bundle
        val selections = mutableMapOf<String, Map<String, String>>()
        selectionsDataBundle?.let { bundle ->
            bundle.keySet().forEach { key ->
                if (key.startsWith("pos_")) {
                    val positionId = key.removePrefix("pos_")
                    val encodedData = bundle.getString(key)
                    encodedData?.let {
                        try {
                            val jsonString = String(android.util.Base64.decode(it, android.util.Base64.NO_WRAP))
                            val jsonObject = org.json.JSONObject(jsonString)
                            val data = mutableMapOf<String, String>()
                            jsonObject.keys().forEach { jsonKey ->
                                data[jsonKey] = jsonObject.getString(jsonKey)
                            }
                            selections[positionId] = data
                        } catch (e: Exception) {
                            android.util.Log.e("Castvote3", "Error parsing selection data: ${e.message}")
                        }
                    }
                }
            }
        }

        // Disable submit button during submission
        btnSubmit.isEnabled = false

        // Submit vote
        FirestoreVoteHelper.submitVote(
            userId = userId,
            electionId = electionIdValue,
            selections = selections,
            signatureBase64 = signatureBase64String,
            onSuccess = { voteId ->
                android.util.Log.d("Castvote3", "Vote submitted successfully: $voteId")
                
                // Prepare receipt data
                voteReceiptData = ReceiptPdfHelper.ReceiptData(
                    voteId = voteId,
                    electionTitle = electionTitle ?: "Election",
                    userName = currentUserName ?: "User",
                    userEmail = currentUserEmail ?: "",
                    submittedAt = Date(),
                    signatureBase64 = signatureBase64String,
                    selections = selections
                )
                
                // Show the recorded dialog with receipt
                showRecordedDialog(voteId)
            },
            onFailure = { error ->
                android.util.Log.e("Castvote3", "Error submitting vote: $error")
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Failed to submit vote: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Displays the VOTE RECORDED receipt dialog using custom_toast_recorded.xml.
     */
    private fun showRecordedDialog(voteId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_recorded, null)
        // This dialog CANNOT be dismissed by touching outside.
        val alertDialog = createStyledAlertDialog(dialogView, isCancellable = false)

        // Format receipt data
        val refCode = voteId
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val refDate = dateFormat.format(Date())
        val refSignature = signatureBase64String?.take(8) + "..." ?: "N/A"

        // Set receipt data
        dialogView.findViewById<TextView>(R.id.ReferenceCode).text = refCode
        dialogView.findViewById<TextView>(R.id.ReferenceDate).text = refDate
        dialogView.findViewById<TextView>(R.id.ReferenceSignature).text = refSignature

        // --- Button Handlers ---

        // Download PDF Button (btn_action_primary)
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary).setOnClickListener {
            alertDialog.dismiss()
            downloadPdfReceipt()
        }

        // Send to Email Button (btn_action_secondary)
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary).setOnClickListener {
            alertDialog.dismiss()
            sendReceiptViaEmail()
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
            @Suppress("DEPRECATION")
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
     * Generate and download PDF receipt
     */
    private fun downloadPdfReceipt() {
        val receiptData = voteReceiptData
        if (receiptData == null) {
            Toast.makeText(this, "Error: Receipt data not available", Toast.LENGTH_SHORT).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
            return
        }

        // Show loading message
        Toast.makeText(this, "Generating PDF receipt...", Toast.LENGTH_SHORT).show()

        ReceiptPdfHelper.generateReceiptPdf(
            context = this,
            receiptData = receiptData,
            onSuccess = { filePath ->
                // Share/download the PDF file
                sharePdfFile(filePath)
            },
            onFailure = { error ->
                android.util.Log.e("Castvote3", "Error generating PDF: $error")
                Toast.makeText(this, "Failed to generate PDF: $error", Toast.LENGTH_LONG).show()
                navigateTo(Homepage::class.java, isFinalExit = true)
            }
        )
    }

    /**
     * Share/download PDF file using Android's share intent
     */
    private fun sharePdfFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show()
                navigateTo(Homepage::class.java, isFinalExit = true)
                return
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Vote Receipt - ${voteReceiptData?.voteId}")
                putExtra(Intent.EXTRA_TEXT, "Please find attached your vote receipt.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Also create a download intent for direct download
            val downloadIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Use chooser to let user choose between share/download
            val chooserIntent = Intent.createChooser(shareIntent, "Save or Share Receipt").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(downloadIntent))
            }

            startActivity(chooserIntent)
            
            // Show success message after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, "Your PDF receipt has been saved.", Toast.LENGTH_SHORT).show()
                navigateTo(Homepage::class.java, isFinalExit = true)
            }, 1000)
        } catch (e: Exception) {
            android.util.Log.e("Castvote3", "Error sharing PDF: ${e.message}", e)
            Toast.makeText(this, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
        }
    }

    /**
     * Send receipt via email
     */
    private fun sendReceiptViaEmail() {
        val receiptData = voteReceiptData
        val userEmail = currentUserEmail

        if (receiptData == null) {
            Toast.makeText(this, "Error: Receipt data not available", Toast.LENGTH_SHORT).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
            return
        }

        if (userEmail.isNullOrBlank()) {
            Toast.makeText(this, "Error: Email address not found", Toast.LENGTH_SHORT).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
            return
        }

        // Show loading message
        Toast.makeText(this, "Preparing email...", Toast.LENGTH_SHORT).show()

        // Generate PDF first
        ReceiptPdfHelper.generateReceiptPdf(
            context = this,
            receiptData = receiptData,
            onSuccess = { filePath ->
                // Read PDF as byte array
                val pdfBytes = ReceiptPdfHelper.getPdfAsByteArray(filePath)
                
                if (pdfBytes == null) {
                    Toast.makeText(this, "Error: Failed to read PDF file", Toast.LENGTH_SHORT).show()
                    ReceiptPdfHelper.deletePdfFile(filePath)
                    navigateTo(Homepage::class.java, isFinalExit = true)
                    return@generateReceiptPdf
                }

                // Convert PDF to base64 for email
                val pdfBase64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                
                // Prepare vote details for email
                val voteDetails = mutableMapOf<String, Any>(
                    "voteId" to receiptData.voteId,
                    "electionTitle" to receiptData.electionTitle,
                    "submittedAt" to SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(receiptData.submittedAt)
                )

                // Build selections text
                val selectionsText = receiptData.selections.map { (positionId, candidateData) ->
                    val positionName = candidateData["positionName"] ?: "Unknown Position"
                    val candidateName = candidateData["candidateName"] ?: "Unknown Candidate"
                    "$positionName: $candidateName"
                }.joinToString("\n")

                voteDetails["selections"] = selectionsText
                voteDetails["pdfBase64"] = pdfBase64
                voteDetails["pdfFileName"] = "vote_receipt_${receiptData.voteId}.pdf"

                // Send email using EmailService
                EmailService.sendVoteConfirmationEmail(
                    email = userEmail,
                    userName = receiptData.userName,
                    voteDetails = voteDetails,
                    onSuccess = {
                        // Clean up PDF file
                        ReceiptPdfHelper.deletePdfFile(filePath)
                        // Show success message
                        showSuccessToastAndNavigate()
                    },
                    onFailure = { error ->
                        android.util.Log.e("Castvote3", "Error sending email: $error")
                        // Clean up PDF file
                        ReceiptPdfHelper.deletePdfFile(filePath)
                        Toast.makeText(this, "Failed to send email: $error", Toast.LENGTH_LONG).show()
                        navigateTo(Homepage::class.java, isFinalExit = true)
                    }
                )
            },
            onFailure = { error ->
                android.util.Log.e("Castvote3", "Error generating PDF for email: $error")
                Toast.makeText(this, "Failed to generate PDF: $error", Toast.LENGTH_LONG).show()
                navigateTo(Homepage::class.java, isFinalExit = true)
            }
        )
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
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else {
            // Default navigation behavior (e.g., going back to Castvote2)
            startActivity(intent)
            finish() // Since we are navigating back, we finish the current activity
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}