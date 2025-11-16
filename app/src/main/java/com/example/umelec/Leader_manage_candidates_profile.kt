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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Leader_manage_candidates_profile : AppCompatActivity() {

    // --- Intent Keys & Mock Data Lookup ---
    companion object {
        const val EXTRA_CANDIDATE_NAME = "CANDIDATE_NAME"
        const val EXTRA_POSITION_NAME = "POSITION_NAME"
        const val EXTRA_IS_EDIT_MODE = "IS_EDIT_MODE"

        /**
         * 💡 MOCK DATA LOOKUP: This simulates querying a database to get a candidate's profile.
         * It uses the mock data structure defined in Leader_manage_candidates.kt.
         * In a real application, this function would call your backend/repository.
         */
        fun getMockProfileData(candidateName: String): ManageCandidate? {
            // Re-creating the fake data structure here for lookup in this activity
            val allPositionsData = listOf(
                ManagePosition("President", listOf(
                    ManageCandidate(
                        name = "Alice Johnson",
                        hasProfileData = true,
                        mockYear = "4th Year",
                        mockCredentials = "• Outstanding Leadership Award\n• Former Class President",
                        mockPlatform = "To champion student welfare through digital transformation.",
                        mockPhotoUri = "content://mock/uploaded/alice_johnson_photo.jpg"
                    ),
                    ManageCandidate(name = "Bob Williams", hasProfileData = false)
                )),
                ManagePosition("Vice President", listOf(
                    ManageCandidate(
                        name = "Charlie Brown",
                        hasProfileData = true,
                        mockYear = "3rd Year",
                        mockCredentials = "• Debating Team Captain (2 years)\n• Published Research Assistant",
                        mockPlatform = "Focusing on mental health and academic support initiatives.",
                        mockPhotoUri = "content://mock/uploaded/charlie_brown_photo.jpg"
                    )
                )),
                ManagePosition("Secretary", listOf(
                    ManageCandidate(name = "Dana Scully", hasProfileData = false),
                    ManageCandidate(name = "Fox Mulder", hasProfileData = false)
                ))
            )

            return allPositionsData
                .flatMap { it.candidates }
                .firstOrNull { it.name == candidateName && it.hasProfileData }
        }
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
        candidateName = intent.getStringExtra(EXTRA_CANDIDATE_NAME) ?: "Candidate Name"
        positionName = intent.getStringExtra(EXTRA_POSITION_NAME) ?: "Position"
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)

        tvCandidateName.text = "$candidateName"
    }

    private fun applyModeUI() {
        if (isEditMode) {
            tvReportTitle.text = "Edit Candidate Profile"
            btnAdd.text = "Save Changes"
            // 💡 Fetch unique data for the specific candidate
            loadMockCandidateData()
        } else {
            tvReportTitle.text = "Add Candidate Profile"
            btnAdd.text = "Add Profile"
        }
        updateButtonState()
    }

    /**
     * MOCK function to simulate loading existing candidate data for editing.
     * Now uses the lookup function to get dynamic data.
     */
    private fun loadMockCandidateData() {
        val profile = getMockProfileData(candidateName)

        if (profile != null) {
            // Fill form fields with mock data
            inputYear.setText(profile.mockYear, false)
            inputCredentials.setText(profile.mockCredentials, TextView.BufferType.EDITABLE)
            inputPlatform.setText(profile.mockPlatform, TextView.BufferType.EDITABLE)

            // Mock photo upload if URI exists
            profile.mockPhotoUri?.let { uriString ->
                // In a real scenario, you might need permission to persist access to this URI.
                val mockUri = Uri.parse(uriString)
                handlePhotoUploadSuccess(mockUri)
            }
        }
        // Note: If profile is null, it means either they shouldn't be in edit mode
        // or there's no data, so fields remain empty.
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
            if (isEditMode) {
                mockSaveToBackend(isEdit = true)
            } else {
                mockSaveToBackend(isEdit = false)
            }
        }
    }

    private fun updateButtonState() {
        val isYearSelected = inputYear.text.toString().isNotEmpty()
        val isCredentialsFilled = inputCredentials.text.toString().trim().isNotEmpty()
        val isPlatformFilled = inputPlatform.text.toString().trim().isNotEmpty()
        val isPhotoUploaded = uploadedPhotoUri != null

        btnAdd.isEnabled = isYearSelected && isCredentialsFilled && isPlatformFilled && isPhotoUploaded
    }

    // --- TOAST & MOCK BACKEND LOGIC (No change) ---
    private fun mockSaveToBackend(isEdit: Boolean) {
        /* ... BACKEND GUIDE COMMENT ... */
        showSuccessToastAndNavigate(isEdit)
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
            view = layout
            show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 40)
    }
}