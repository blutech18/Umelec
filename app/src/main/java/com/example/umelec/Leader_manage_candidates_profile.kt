package com.example.umelec

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Leader_manage_candidates_profile : AppCompatActivity() {

    // --- Intent Keys ---
    companion object {
        const val EXTRA_CANDIDATE_ID = "CANDIDATE_ID"
        const val EXTRA_CANDIDATE_NAME = "CANDIDATE_NAME"
        const val EXTRA_POSITION_NAME = "POSITION_NAME"
        const val EXTRA_IS_EDIT_MODE = "IS_EDIT_MODE"
    }

    // --- View References ---
    private lateinit var btnBack: ImageButton
    private lateinit var tvReportTitle: TextView
    private lateinit var tvCandidateName: TextView
    private lateinit var inputYear: AutoCompleteTextView
    private lateinit var inputCredentials: TextInputEditText
    private lateinit var inputPlatform: TextInputEditText
    private lateinit var btnUploadPhoto: LinearLayout
    private lateinit var ivUpload: ImageView
    private lateinit var tvUploadText: TextView
    private lateinit var ivRemove: ImageView
    private lateinit var btnAdd: Button

    // --- State Variables ---
    private var isEditMode: Boolean = false
    private var candidateId: String = ""
    private var candidateName: String = ""
    private var positionName: String = ""
    private var uploadedPhotoUri: Uri? = null

    // --- Photo Picker Launcher ---
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handlePhotoUploadSuccess(uri)
        }
        updateButtonState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_manage_candidates_profile)

        // 1. Initialize Views
        initializeViews()

        // 2. Process Intent Data
        processIntentData()

        // 3. Set up UI and Listeners
        setupYearLevelDropdown()
        setupPhotoUploadListeners()
        setupValidationListeners()

        // 4. Apply UI based on Add/Edit Mode
        applyModeUI()
    }

    // --- INITIALIZATION ---
    private fun initializeViews() {
        // ... (Views initialization remains the same)
        btnBack = findViewById(R.id.btnBack)
        tvReportTitle = findViewById(R.id.ReportTitle)
        tvCandidateName = findViewById(R.id.tvCandidateName)
        inputYear = findViewById(R.id.inputYear)
        inputCredentials = findViewById(R.id.inputCredentials)
        inputPlatform = findViewById(R.id.inputPlatform)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)
        ivUpload = findViewById(R.id.ivUpload)
        tvUploadText = findViewById(R.id.tvUploadText)
        ivRemove = findViewById(R.id.ivRemove)
        btnAdd = findViewById(R.id.btnAdd)

        ivRemove.visibility = View.GONE
        btnBack.setOnClickListener { finish() }
    }

    // --- DATA PROCESSING AND MODE SETUP ---
    private fun processIntentData() {
        candidateId = intent.getStringExtra(EXTRA_CANDIDATE_ID) ?: ""
        candidateName = intent.getStringExtra(EXTRA_CANDIDATE_NAME) ?: "Candidate Name"
        positionName = intent.getStringExtra(EXTRA_POSITION_NAME) ?: "Position"
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)

        tvCandidateName.text = "$candidateName"
    }

    private fun applyModeUI() {
        if (isEditMode) {
            tvReportTitle.text = "Edit Candidate Profile"
            btnAdd.text = "Save Changes"
            // Load candidate data from Firestore
            loadCandidateData()
        } else {
            tvReportTitle.text = "Add Candidate Profile"
            btnAdd.text = "Add Profile"
        }
        updateButtonState()
    }

    /**
     * Load candidate data from Firestore
     */
    private fun loadCandidateData() {
        if (candidateId.isEmpty()) {
            android.util.Log.w("Leader_manage_candidates_profile", "Candidate ID is empty, cannot load data")
            return
        }

        android.util.Log.d("Leader_manage_candidates_profile", "Loading candidate data for ID: $candidateId")
        FirestoreCandidateHelper.getCandidatePlatformDetails(
            candidateId = candidateId,
            onSuccess = { details ->
                if (details != null) {
                    android.util.Log.d("Leader_manage_candidates_profile", "Loaded candidate: ${details.name}")
                    // Fill form fields with candidate data
                    inputYear.setText(details.courseInfo, false)
                    inputCredentials.setText(details.credentials, TextView.BufferType.EDITABLE)
                    inputPlatform.setText(details.advocacy, TextView.BufferType.EDITABLE)
                    
                    // Load existing photo if available
                    if (!details.photoUrl.isNullOrEmpty() && FirebaseStorageHelper.isFirebaseStorageUrl(details.photoUrl)) {
                        loadExistingPhoto(details.photoUrl)
                    }
                    
                    // Update button state after loading data
                    updateButtonState()
                } else {
                    android.util.Log.w("Leader_manage_candidates_profile", "Candidate details are null")
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_manage_candidates_profile", "Error loading candidate: $error")
                Toast.makeText(this, "Error loading candidate data: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- DROPDOWN & INPUT SETUP (No change) ---
    private fun setupYearLevelDropdown() {
        val years = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years) // Changed custom layout to standard for simplicity
        inputYear.setAdapter(adapter)
        inputYear.addTextChangedListener { updateButtonState() }
    }

    // --- PHOTO PICKER LOGIC (No change) ---
    private fun setupPhotoUploadListeners() {
        btnUploadPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        ivRemove.setOnClickListener {
            resetPhotoUploadState()
            updateButtonState()
        }
    }

    private fun handlePhotoUploadSuccess(uri: Uri) {
        uploadedPhotoUri = uri

        val pathSegment = uri.pathSegments.lastOrNull() ?: "Photo"
        val displayName = if (pathSegment.length > 10) {
            "${pathSegment.substring(0, 7)}..."
        } else {
            pathSegment
        }

        ivUpload.setImageResource(R.drawable.ic_photo)
        tvUploadText.text = displayName
        ivRemove.visibility = View.VISIBLE
    }

    private fun resetPhotoUploadState() {
        uploadedPhotoUri = null
        ivUpload.setImageResource(R.drawable.ic_upload)
        tvUploadText.text = "Upload"
        ivRemove.visibility = View.GONE
    }

    /**
     * Load existing photo from Firebase Storage URL using Glide
     */
    private fun loadExistingPhoto(photoUrl: String) {
        android.util.Log.d("Leader_manage_candidates_profile", "Loading existing photo: $photoUrl")
        
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_upload)
            .error(R.drawable.ic_upload)
            .into(ivUpload)
        
        // Update UI to show that photo is loaded
        tvUploadText.text = "Current Photo"
        ivRemove.visibility = View.VISIBLE
    }

    // --- VALIDATION LOGIC (No change) ---
    private fun setupValidationListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        inputCredentials.addTextChangedListener(textWatcher)
        inputPlatform.addTextChangedListener(textWatcher)

        // Final Button Listener
        btnAdd.setOnClickListener {
            saveCandidateProfile()
        }
    }

    private fun updateButtonState() {
        val isYearSelected = inputYear.text.toString().isNotEmpty()
        val isCredentialsFilled = inputCredentials.text.toString().trim().isNotEmpty()
        val isPlatformFilled = inputPlatform.text.toString().trim().isNotEmpty()
        val isPhotoUploaded = uploadedPhotoUri != null

        // In edit mode, photo is optional if other fields are filled
        // In add mode, photo is required
        if (isEditMode) {
            btnAdd.isEnabled = isYearSelected && isCredentialsFilled && isPlatformFilled
        } else {
            btnAdd.isEnabled = isYearSelected && isCredentialsFilled && isPlatformFilled && isPhotoUploaded
        }
    }

    // --- SAVE TO FIRESTORE ---
    private fun saveCandidateProfile() {
        if (candidateId.isEmpty()) {
            android.util.Log.e("Leader_manage_candidates_profile", "Candidate ID is empty")
            Toast.makeText(this, "Error: Candidate ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        val year = inputYear.text.toString().trim()
        val credentials = inputCredentials.text.toString().trim()
        val platform = inputPlatform.text.toString().trim()

        if (year.isEmpty() || credentials.isEmpty() || platform.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // In add mode, photo is required
        if (!isEditMode && uploadedPhotoUri == null) {
            Toast.makeText(this, "Please upload a photo", Toast.LENGTH_SHORT).show()
            return
        }

        btnAdd.isEnabled = false
        Toast.makeText(this, if (isEditMode) "Saving changes..." else "Adding profile...", Toast.LENGTH_SHORT).show()

        android.util.Log.d("Leader_manage_candidates_profile", "Saving candidate profile: $candidateId")
        android.util.Log.d("Leader_manage_candidates_profile", "Year: $year, Credentials: $credentials, Platform: $platform")

        // If photo is uploaded, upload it first, then save profile
        if (uploadedPhotoUri != null) {
            uploadPhotoAndSaveProfile(year, credentials, platform)
        } else {
            // No photo to upload, save profile directly
            saveProfileToFirestore(year, credentials, platform, null)
        }
    }

    private fun uploadPhotoAndSaveProfile(year: String, credentials: String, platform: String) {
        android.util.Log.d("Leader_manage_candidates_profile", "Uploading photo for candidate: $candidateId")
        
        FirebaseStorageHelper.uploadCandidatePhoto(
            imageUri = uploadedPhotoUri!!,
            candidateId = candidateId,
            onSuccess = { downloadUrl ->
                android.util.Log.d("Leader_manage_candidates_profile", "Photo uploaded successfully: $downloadUrl")
                saveProfileToFirestore(year, credentials, platform, downloadUrl)
            },
            onFailure = { error ->
                btnAdd.isEnabled = true
                android.util.Log.e("Leader_manage_candidates_profile", "Photo upload failed: $error")
                Toast.makeText(this, "Photo upload failed: $error", Toast.LENGTH_LONG).show()
            },
            onProgress = { progress ->
                // Update UI with upload progress if needed
                android.util.Log.d("Leader_manage_candidates_profile", "Upload progress: $progress%")
            }
        )
    }

    private fun saveProfileToFirestore(year: String, credentials: String, platform: String, photoUrl: String?) {
        FirestoreCandidateHelper.updateCandidateProfile(
            candidateId = candidateId,
            courseInfo = year,
            credentials = credentials,
            advocacy = platform,
            photoUrl = photoUrl,
            onSuccess = {
                android.util.Log.d("Leader_manage_candidates_profile", "Candidate profile saved successfully: $candidateId")
                showSuccessToastAndNavigate(isEditMode)
            },
            onFailure = { error ->
                btnAdd.isEnabled = true
                android.util.Log.e("Leader_manage_candidates_profile", "Error saving profile: $error")
                Toast.makeText(this, "Error saving: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showSuccessToastAndNavigate(isEdit: Boolean) {
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.custom_toast_success, null)

        val titleText: TextView = layout.findViewById(R.id.toast_title)
        val valueText: TextView = layout.findViewById(R.id.toast_value)
        val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

        if (isEdit) {
            titleText.text = "Successfully Edited"
            valueText.text = "You have updated $candidateName's profile."
        } else {
            titleText.text = "Profile Added!"
            valueText.text = "$candidateName is now a candidate for $positionName."
        }

        actionButton.visibility = View.GONE

        with (Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            @Suppress("DEPRECATION")
            view = layout
            show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 40)
    }
}