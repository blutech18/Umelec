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
import com.github.gcacace.signaturepad.views.SignaturePad // Import remains for the view's original context, but we use the custom one
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
import android.app.DownloadManager
import android.content.ContentValues
import android.content.ContentResolver
import android.provider.MediaStore
import android.os.Environment
import java.io.FileInputStream

// REMINDER: You MUST add the following dependency to your app/build.gradle file:
// implementation 'com.github.gcacace:signature-pad:1.3.1'

class Castvote3 : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageButton
    // FIX 1: Change type to the custom view
    private lateinit var signaturePad: ConstrainedSignaturePad
    private lateinit var btnClearSignature: AppCompatButton
    private lateinit var btnSubmit: Button // This corresponds to btnNext in XML
    // FIX 2: Add the error text view for constraint feedback
    private lateinit var signaturePadReqText: TextView

    // State
    private var isSignatureDrawn: Boolean = false
    // FIX 3: Add validation state, controlled by the custom view
    private var isSignatureValid: Boolean = true
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
        // FIX 4: Initialize as the custom view type
        signaturePad = findViewById(R.id.signaturePad)
        btnClearSignature = findViewById(R.id.btnClearSignature)
        btnSubmit = findViewById(R.id.btnSubmit) // Using the ID from XML: btnNext
        // FIX 5: Initialize the error text view
        signaturePadReqText = findViewById(R.id.SignaturePadReqText)

        // 2. Retrieve Data from Castvote2.kt
        reviewedPositions = intent.getStringArrayListExtra("positions") ?: emptyList()
        reviewedCandidates = intent.getStringArrayListExtra("candidates") ?: emptyList()
        selectionsDataBundle = intent.getBundleExtra("selectionsData")
        electionId = intent.getStringExtra("electionId")

        // Get current user ID and info (unchanged logic)
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUserId = currentUser?.uid
        currentUserEmail = currentUser?.email ?: ""

        // Get user name from Firestore (unchanged logic)
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

        // Get election title (unchanged logic)
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
        // FIX 6: Hide constraint error text initially
        signaturePadReqText.visibility = View.GONE

        // FIX 7: Implement the listener from the custom ConstrainedSignaturePad
        signaturePad.onBoundaryCrossedListener = { isCrossed ->
            // Update the activity's main state based on the custom view's report
            isSignatureValid = !isCrossed
            signaturePadReqText.visibility = if (isCrossed) View.VISIBLE else View.GONE
            updateSubmitButtonState()
        }

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
                // Signature drawn: enable clear and set flag
                isSignatureDrawn = true
                btnClearSignature.isEnabled = true
                // Validation state (isSignatureValid) is handled by the custom view's listener (FIX 7)
                updateSubmitButtonState()
            }

            override fun onClear() {
                // Signature cleared: disable clear and submit buttons, reset data
                isSignatureDrawn = false
                // FIX 8: Reset validation state
                isSignatureValid = true
                btnClearSignature.isEnabled = false
                signatureBase64String = null
                // FIX 9: Hide error text on clear
                signaturePadReqText.visibility = View.GONE
                updateSubmitButtonState()
            }
        })

        // 6. Clear Signature Button Logic
        btnClearSignature.setOnClickListener {
            signaturePad.clear() // Clears the canvas and triggers the onClear listener
        }

        // 7. Submit Button Logic
        btnSubmit.setOnClickListener {
            // FIX 10: Check signature validity before proceeding
            if (isSignatureDrawn && isSignatureValid) {
                // 1. Capture and convert signature
                val signatureBitmap: Bitmap = signaturePad.getSignatureBitmap()
                val byteArrayOutputStream = ByteArrayOutputStream()
                signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                signatureBase64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // 2. Show the final confirmation dialog before submission
                showFinalConfirmationDialog()
            } else if (!isSignatureValid) {
                // Show a toast and the error if validation failed
                //Toast.makeText(this, "Please clear and re-sign inside the constraint box.", Toast.LENGTH_SHORT).show()
                signaturePadReqText.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Updates the enabled state of the Submit button based on required conditions.
     */
    private fun updateSubmitButtonState() {
        // FIX 11: Button is enabled ONLY when a signature is drawn AND the signature is valid
        btnSubmit.isEnabled = isSignatureDrawn && isSignatureValid
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

    // --- REST OF THE METHODS ARE UNCHANGED (showFinalConfirmationDialog, submitVote, etc.) ---

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

                // Fetch cryptographic metadata for the vote
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("votes")
                    .document(voteId)
                    .get()
                    .addOnSuccessListener { document ->
                        val digitalSignaturePreview = document.getString("signaturePreview")
                        val storedDigitalSignature = document.getString("digitalSignature")
                        val storedPublicKey = document.getString("dsaPublicKey")

                        val voteDataString = VoteCryptographyHelper.buildVoteDataString(
                            voteId = voteId,
                            electionId = electionIdValue,
                            selections = selections
                        )

                        val isSignatureVerified =
                            if (!storedPublicKey.isNullOrBlank() && !storedDigitalSignature.isNullOrBlank()) {
                                VoteCryptographyHelper.verifyVoteSignature(
                                    voteData = voteDataString,
                                    signatureBase64 = storedDigitalSignature,
                                    publicKeyBase64 = storedPublicKey
                                )
                            } else {
                                false
                            }

                        voteReceiptData = ReceiptPdfHelper.ReceiptData(
                            voteId = voteId,
                            electionId = electionIdValue,
                            electionTitle = electionTitle ?: "Election",
                            userName = currentUserName ?: "User",
                            userEmail = currentUserEmail ?: "",
                            submittedAt = Date(),
                            signatureBase64 = signatureBase64String,
                            selections = selections,
                            digitalSignaturePreview = digitalSignaturePreview ?: storedDigitalSignature?.take(8),
                            digitalSignature = storedDigitalSignature,
                            dsaPublicKey = storedPublicKey,
                            isSignatureVerified = isSignatureVerified
                        )

                        createVoteSubmittedNotification(voteId, electionTitle ?: "Election")
                        showRecordedDialog(voteId)
                    }
                    .addOnFailureListener { error ->
                        android.util.Log.e("Castvote3", "Error fetching vote metadata: ${error.message}", error)

                        voteReceiptData = ReceiptPdfHelper.ReceiptData(
                            voteId = voteId,
                            electionId = electionIdValue,
                            electionTitle = electionTitle ?: "Election",
                            userName = currentUserName ?: "User",
                            userEmail = currentUserEmail ?: "",
                            submittedAt = Date(),
                            signatureBase64 = signatureBase64String,
                            selections = selections
                        )

                        createVoteSubmittedNotification(voteId, electionTitle ?: "Election")
                        showRecordedDialog(voteId)
                    }
            },
            onFailure = { error ->
                android.util.Log.e("Castvote3", "Error submitting vote: $error")
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Failed to submit vote: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Create "Vote Submitted" notification automatically after vote submission
     */
    private fun createVoteSubmittedNotification(voteId: String, electionTitle: String) {
        val userId = FirebaseAuthHelper.getCurrentUser()?.uid
        if (userId == null) {
            android.util.Log.w("Castvote3", "Cannot create notification: User ID is null")
            return
        }

        val title = "Vote Submitted"
        val previewText = "Your vote has been successfully submitted."
        val fullText = "Your vote for \"$electionTitle\" has been successfully submitted. Your Vote ID is: $voteId. Thank you for participating in the election!"

        FirestoreNotificationHelper.createNotification(
            title = title,
            previewText = previewText,
            fullText = fullText,
            type = NotificationType.SUBMISSION,
            targetUserId = userId,
            onSuccess = { notificationId ->
                android.util.Log.d("Castvote3", "Vote Submitted notification created: $notificationId")
            },
            onFailure = { error ->
                android.util.Log.e("Castvote3", "Failed to create Vote Submitted notification: $error")
            }
        )
    }

    private fun showRecordedDialog(voteId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_recorded, null)
        // This dialog CANNOT be dismissed by touching outside.
        val alertDialog = createStyledAlertDialog(dialogView, isCancellable = false)

        // Format receipt data
        val refCode = voteId
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val refDate = dateFormat.format(Date())
        val signatureSnippet = voteReceiptData?.digitalSignaturePreview
            ?: (signatureBase64String?.take(8) ?: "N/A")
        val refSignature = if (voteReceiptData?.isSignatureVerified == true) {
            "$signatureSnippet (Verified)"
        } else {
            "$signatureSnippet (Verification Pending)"
        }

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
     * Re-verify receipt signature using stored public key before generating receipts
     */
    private fun refreshReceiptSignatureVerification(): ReceiptPdfHelper.ReceiptData? {
        val receipt = voteReceiptData ?: return null
        val isVerified = verifyReceiptSignature(receipt)
        val updatedReceipt = receipt.copy(
            isSignatureVerified = isVerified,
            digitalSignaturePreview = receipt.digitalSignaturePreview
                ?: receipt.digitalSignature?.take(8)
        )
        voteReceiptData = updatedReceipt
        return updatedReceipt
    }

    /**
     * Verify DSA signature using stored public key
     */
    private fun verifyReceiptSignature(receiptData: ReceiptPdfHelper.ReceiptData): Boolean {
        val signature = receiptData.digitalSignature
        val publicKey = receiptData.dsaPublicKey
        if (signature.isNullOrBlank() || publicKey.isNullOrBlank()) {
            return false
        }

        return try {
            val voteDataString = VoteCryptographyHelper.buildVoteDataString(
                voteId = receiptData.voteId,
                electionId = receiptData.electionId,
                selections = receiptData.selections
            )
            VoteCryptographyHelper.verifyVoteSignature(
                voteData = voteDataString,
                signatureBase64 = signature,
                publicKeyBase64 = publicKey
            )
        } catch (e: Exception) {
            android.util.Log.e("Castvote3", "Error verifying receipt signature: ${e.message}", e)
            false
        }
    }


    /**
     * Shows a custom success toast message
     */
    private fun showSuccessToast(title: String, message: String) {
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.custom_toast_success, null)

        val titleText: TextView = layout.findViewById(R.id.toast_title)
        val valueText: TextView = layout.findViewById(R.id.toast_value)
        val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

        titleText.text = title
        valueText.text = message
        actionButton.visibility = View.GONE

        with (Toast(applicationContext)) {
            duration = Toast.LENGTH_LONG
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            @Suppress("DEPRECATION")
            view = layout
            show()
        }
    }

    /**
     * DISPLAYS A CUSTOM TOAST and then navigates to the Homepage.
     * Replaces the previous AlertDialog implementation.
     */
    private fun showSuccessToastAndNavigate() {
        showSuccessToast("Sent Successfully", "Check your email.")

        // Navigate after showing toast
        Handler(Looper.getMainLooper()).postDelayed({
            navigateTo(Homepage::class.java, isFinalExit = true)
        }, 2000)
    }

    /**
     * Generate and download PDF receipt
     */
    private fun downloadPdfReceipt() {
        val receiptData = refreshReceiptSignatureVerification()
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
     * Save PDF file to Downloads folder using MediaStore API (Android 10+) or DownloadManager
     */
    private fun sharePdfFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show()
                navigateTo(Homepage::class.java, isFinalExit = true)
                return
            }

            val fileName = "vote_receipt_${voteReceiptData?.voteId ?: System.currentTimeMillis()}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore API for Android 10+ (API 29+)
                savePdfToDownloadsMediaStore(file, fileName)
            } else {
                // Use DownloadManager for older Android versions
                savePdfToDownloadsLegacy(file, fileName)
            }
        } catch (e: Exception) {
            android.util.Log.e("Castvote3", "Error saving PDF: ${e.message}", e)
            Toast.makeText(this, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
        }
    }

    /**
     * Save PDF to Downloads using MediaStore API (Android 10+)
     */
    private fun savePdfToDownloadsMediaStore(sourceFile: File, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val contentResolver = contentResolver
            // Use MediaStore.Downloads (available on Android 10+)
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Clean up temporary file
                ReceiptPdfHelper.deletePdfFile(sourceFile.absolutePath)

                // Show success toast
                showSuccessToast("PDF Downloaded Successfully", "Your vote receipt has been saved to Downloads folder")

                // Navigate after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateTo(Homepage::class.java, isFinalExit = true)
                }, 2000)
            } else {
                throw Exception("Failed to create file in Downloads")
            }
        } catch (e: Exception) {
            android.util.Log.e("Castvote3", "Error saving PDF via MediaStore: ${e.message}", e)
            Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
        }
    }

    /**
     * Save PDF to Downloads using DownloadManager (Android 9 and below)
     */
    private fun savePdfToDownloadsLegacy(sourceFile: File, fileName: String) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    sourceFile
                )
            } else {
                Uri.fromFile(sourceFile)
            }

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri).apply {
                setTitle("Vote Receipt - ${voteReceiptData?.voteId}")
                setDescription("Your vote receipt PDF")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/pdf")
            }

            val downloadId = downloadManager.enqueue(request)

            // Clean up temporary file after a delay (DownloadManager copies it)
            Handler(Looper.getMainLooper()).postDelayed({
                ReceiptPdfHelper.deletePdfFile(sourceFile.absolutePath)

                // Show success toast
                showSuccessToast("PDF Downloaded Successfully", "Your vote receipt has been saved to Downloads folder")

                // Navigate after showing toast
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateTo(Homepage::class.java, isFinalExit = true)
                }, 2000)
            }, 1000)
        } catch (e: Exception) {
            android.util.Log.e("Castvote3", "Error saving PDF via DownloadManager: ${e.message}", e)
            Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
            navigateTo(Homepage::class.java, isFinalExit = true)
        }
    }

    /**
     * Send receipt via email
     */
    private fun sendReceiptViaEmail() {
        val receiptData = refreshReceiptSignatureVerification()
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
                // Note: Selections are NOT included to protect voter privacy
                val voteDetails = mutableMapOf<String, Any>(
                    "voteId" to receiptData.voteId,
                    "electionTitle" to receiptData.electionTitle,
                    "submittedAt" to SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(receiptData.submittedAt)
                )

                // Selections removed - not included in email to protect voter privacy
                // The PDF receipt also does not contain candidate selections

                voteDetails["pdfBase64"] = pdfBase64
                voteDetails["pdfFileName"] = "vote_receipt_${receiptData.voteId}.pdf"
                voteDetails["signatureSnippet"] = receiptData.digitalSignaturePreview ?: "N/A"
                voteDetails["signatureVerification"] = if (receiptData.isSignatureVerified) {
                    "Verified via DSA public key"
                } else {
                    "Verification failed or unavailable"
                }
                voteDetails["publicKeyPreview"] = receiptData.dsaPublicKey?.let {
                    if (it.length > 64) "${it.take(64)}..." else it
                } ?: "Not available"

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