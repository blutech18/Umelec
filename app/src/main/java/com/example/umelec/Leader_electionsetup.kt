package com.example.umelec

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Leader_electionsetup : AppCompatActivity() {

    companion object {
        // Temporary storage for positions before election is created
        // Format: List of (positionName, yearLevel, candidates)
        var temporaryPositions: List<Triple<String, String, List<String>>> = emptyList()
    }

    // Define color constant (AS IS from Leader_manage_voters_list.kt)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_DEFAULT_GRAY = Color.parseColor("#8C8CA1") // Based on your XML

    // Modern Activity Result API launcher
    private val positionSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Refresh position cards when returning from position setup
        if (result.resultCode == RESULT_OK) {
            // If there are temporary positions, display them
            if (temporaryPositions.isNotEmpty()) {
                displayTemporaryPositions()
            } else {
                loadPositionsAndDisplay()
            }
        } else {
            loadPositionsCount()
        }
    }

    // 1. View References
    private lateinit var btnBack: ImageButton
    private lateinit var inputTitle: TextInputEditText // Renamed from inputQuestion
    private lateinit var inputStartDate: TextInputEditText
    private lateinit var inputStartTime: TextInputEditText
    private lateinit var inputEndDate: TextInputEditText
    private lateinit var inputEndTime: TextInputEditText
    private lateinit var tvPosition: TextView
    private lateinit var btnViewPosition: AppCompatButton
    private lateinit var cbAgreeTerms: CheckBox
    private lateinit var btnPreview: AppCompatButton
    private lateinit var btnSubmit: Button // Renamed from btnAdd to btnSubmit

    // 2. TextInputLayout References (for focus outline change)
    private lateinit var layoutTitle: TextInputLayout // Renamed from AddQuestion
    private lateinit var layoutStartDate: TextInputLayout
    private lateinit var layoutStartTime: TextInputLayout
    private lateinit var layoutEndDate: TextInputLayout
    private lateinit var layoutEndTime: TextInputLayout

    // Store current election ID for positions
    private var currentElectionId: String? = null
    private var positionsCount = 0
    private lateinit var positionListContainer: LinearLayout

    // 4. List of all required input fields and layouts for validation/focus
    private val inputFields: List<TextInputEditText> by lazy {
        listOf(inputTitle, inputStartDate, inputStartTime, inputEndDate, inputEndTime)
    }
    private val inputLayouts: List<TextInputLayout> by lazy {
        listOf(layoutTitle, layoutStartDate, layoutStartTime, layoutEndDate, layoutEndTime)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_electionsetup)

        initializeViews()
        setupListeners()
        // Load positions - check temporary first, then from Firestore
        if (temporaryPositions.isNotEmpty()) {
            displayTemporaryPositions()
        } else {
            loadPositionsAndDisplay()
        }
        checkFormValidity() // Set initial button state
    }

    private fun initializeViews() {
        // Find EditText/Button views
        btnBack = findViewById(R.id.btnBack)
        inputTitle = findViewById(R.id.inputTitle) // Used inputQuestion as Election Title
        inputStartDate = findViewById(R.id.inputStartDate)
        inputStartTime = findViewById(R.id.inputStartTime)
        inputEndDate = findViewById(R.id.inputEndDate)
        inputEndTime = findViewById(R.id.inputEndTime)
        tvPosition = findViewById(R.id.tvPosition)
        btnViewPosition = findViewById(R.id.btnViewPosition)
        positionListContainer = findViewById(R.id.PositionListContainer)
        cbAgreeTerms = findViewById(R.id.cbAgreeTerms)
        btnPreview = findViewById(R.id.btnPreview)
        btnSubmit = findViewById(R.id.btnSubmit) // Ensure you renamed btnAdd to btnSubmit in XML

        // Find TextInputLayout views
        layoutTitle = findViewById(R.id.AddTitle)
        layoutStartDate = findViewById(R.id.SelectStartDateLayout)
        layoutStartTime = findViewById(R.id.SelectStartTimeLayout)
        layoutEndDate = findViewById(R.id.SelectEndDateLayout)
        layoutEndTime = findViewById(R.id.SelectEndTimeLayout)
    }

    private fun setupListeners() {
        // --- 1. Back Button
        btnBack.setOnClickListener { finish() }

        // --- 2. Focus Change and Text Watchers for all fields
        setupFieldFocusAndValidation()

        // --- 3. Date and Time Pickers (UPDATED TO USE CUSTOM PICKERS)
        inputStartDate.setOnClickListener { showCustomDatePicker(inputStartDate) }
        inputEndDate.setOnClickListener { showCustomDatePicker(inputEndDate) }
        inputStartTime.setOnClickListener { showCustomTimePicker(inputStartTime) }
        inputEndTime.setOnClickListener { showCustomTimePicker(inputEndTime) }

        // --- 4. Checkbox Listener (For Validation)
        cbAgreeTerms.setOnCheckedChangeListener { _, _ ->
            checkFormValidity()
        }

        // --- 5. Navigation Listeners
        btnViewPosition.setOnClickListener {
            // Navigate to Leader_election_setup_position.kt using modern Activity Result API
            val intent = Intent(this, Leader_electionsetup_position::class.java)
            currentElectionId?.let { intent.putExtra("electionId", it) }
            positionSetupLauncher.launch(intent)
        }

        btnPreview.setOnClickListener {
            // Navigate to Leader_electionsetup_preview.kt
            val intent = Intent(this, Leader_electionsetup_preview::class.java)
            // Pass election title if available (from input field or temporary)
            val electionTitle = inputTitle.text.toString().trim()
            if (electionTitle.isNotEmpty()) {
                intent.putExtra("electionTitle", electionTitle)
            }
            startActivity(intent)
        }

        btnSubmit.setOnClickListener {
            submitElection()
        }
    }

    // =========================================================================
    // CUSTOM FOCUS AND VALIDATION LOGIC (Imitated from Leader_manage_voters_list.kt)
    // =========================================================================

    private fun setupFieldFocusAndValidation() {
        // Apply focus change listener to all fields to change the outline color
        for (i in inputFields.indices) {
            val inputField = inputFields[i]
            val inputLayout = inputLayouts[i]

            // 2. Focus Change Listener for Outline Color
            inputField.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    inputLayout.boxStrokeColor = COLOR_PRIMARY_BLUE
                } else {
                    // Only reset if it is not a non-user-editable field that was clicked (like date/time pickers)
                    if (inputField.inputType != 0) { // Check if inputType is NOT 'none' (0)
                        inputLayout.boxStrokeColor = COLOR_DEFAULT_GRAY
                    }
                }
            }

            // 3. TextWatcher for validation
            inputField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    checkFormValidity()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun checkFormValidity() {
        // 1. Check if all TextInputEditText fields have text
        val allFieldsFilled = inputFields.all { it.text.toString().trim().isNotEmpty() }

        // 2. Check if the Checkbox is checked
        val termsChecked = cbAgreeTerms.isChecked

        // 3. For new elections, positions can be added later
        // Enable form submission with basic election details
        val isFormValid = allFieldsFilled && termsChecked

        btnSubmit.isEnabled = isFormValid
        btnPreview.isEnabled = isFormValid
    }

    // =========================================================================
    // CUSTOM SCROLLABLE DATE AND TIME PICKER LOGIC (Using Custom Dialogs)
    // =========================================================================

    private fun showCustomDatePicker(targetField: TextInputEditText) {
        val dialog = CustomDatePickerDialog(this) { selectedDate ->
            // This is the callback when "Apply" is clicked
            targetField.setText(selectedDate)
            checkFormValidity()
        }
        dialog.show()
    }

    private fun showCustomTimePicker(targetField: TextInputEditText) {
        val dialog = CustomTimePickerDialog(this) { selectedTime ->
            // This is the callback when "Apply" is clicked
            targetField.setText(selectedTime)
            checkFormValidity()
        }
        dialog.show()
    }

    // =========================================================================
    // POSITIONS CARD LOGIC (Load from Firestore)
    // =========================================================================

    private fun loadPositionsCount() {
        // For new election setup, start with 0 positions
        // Positions will be managed through the position setup page
        positionsCount = 0
        updatePositionsUI()
        
        // Get positions count for current election (if election ID exists)
        currentElectionId?.let { electionId ->
            FirestoreLeaderHelper.getPositionsForElection(
                electionId = electionId,
                onSuccess = { positions ->
                    positionsCount = positions.size
                    updatePositionsUI()
                    checkFormValidity()
                },
                onFailure = { error ->
                    android.util.Log.e("Leader_electionsetup", "Error loading positions: $error")
                    positionsCount = 0
                    updatePositionsUI()
                    checkFormValidity()
                }
            )
        }
    }

    private fun loadPositionsAndDisplay() {
        // First try to get election ID from currentElectionId, if not, fetch it
        val electionIdToUse = currentElectionId ?: run {
            // Try to get current election ID
            FirestoreElectionHelper.getCurrentElectionId(
                onSuccess = { fetchedId ->
                    if (!fetchedId.isNullOrEmpty()) {
                        currentElectionId = fetchedId
                        loadPositionsForDisplay(fetchedId)
                    } else {
                        clearPositionList()
                        updatePositionsUI()
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("Leader_electionsetup", "Error getting election ID: $error")
                    clearPositionList()
                    updatePositionsUI()
                }
            )
            return
        }
        
        loadPositionsForDisplay(electionIdToUse)
    }

    private fun loadPositionsForDisplay(electionId: String) {
        FirestoreLeaderHelper.getPositionsForElection(
            electionId = electionId,
            onSuccess = { positions ->
                positionsCount = positions.size
                displayPositionList(positions)
                updatePositionsUI()
                checkFormValidity()
            },
            onFailure = { error ->
                android.util.Log.e("Leader_electionsetup", "Error loading positions: $error")
                positionsCount = 0
                clearPositionList()
                updatePositionsUI()
                checkFormValidity()
            }
        )
    }


    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun displayTemporaryPositions() {
        positionListContainer.removeAllViews()
        
        if (temporaryPositions.isEmpty()) {
            clearPositionList()
            updatePositionsUI()
            return
        }

        positionsCount = temporaryPositions.size
        
        temporaryPositions.forEach { (positionName, _, _) ->
            // Create simple text view for position name
            val positionTextView = TextView(this).apply {
                text = positionName
                textSize = 16f
                setTextColor(Color.parseColor("#313131"))
                setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                typeface = resources.getFont(R.font.poppins_regular)
            }
            
            positionListContainer.addView(positionTextView)
        }
        
        updatePositionsUI()
    }
    
    private fun displayPositionList(positions: List<Map<String, Any>>) {
        positionListContainer.removeAllViews()
        
        if (positions.isEmpty()) {
            clearPositionList()
            updatePositionsUI()
            return
        }

        positionsCount = positions.size
        
        positions.forEach { positionData ->
            val positionName = positionData["positionName"] as? String ?: ""
            
            // Create simple text view for position name
            val positionTextView = TextView(this).apply {
                text = positionName
                textSize = 16f
                setTextColor(Color.parseColor("#313131"))
                setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                typeface = resources.getFont(R.font.poppins_regular)
            }
            
            positionListContainer.addView(positionTextView)
        }
        
        updatePositionsUI()
    }
    
    private fun clearPositionList() {
        positionListContainer.removeAllViews()
    }

    private fun updatePositionsUI() {
        if (positionsCount > 0) {
            tvPosition.text = "$positionsCount position(s) added"
            tvPosition.visibility = View.GONE
        } else {
            tvPosition.text = "Add at least 1 position"
            tvPosition.visibility = View.VISIBLE
        }
    }

    // =========================================================================
    // SAVE TEMPORARY POSITIONS
    // =========================================================================

    private fun saveTemporaryPositions(electionId: String) {
        if (temporaryPositions.isEmpty()) return

        var completed = 0
        var failed = 0
        val total = temporaryPositions.size
        
        // Get current leader's UID
        val currentLeaderId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentLeaderId == null) {
            android.util.Log.e("Leader_electionsetup", "Current user not found, cannot save candidates")
            return
        }

        temporaryPositions.forEach { (positionName, yearLevel, candidates) ->
            FirestoreLeaderHelper.addPosition(
                electionId = electionId,
                positionName = positionName,
                onSuccess = { positionId ->
                    // Save candidates for this position
                    candidates.forEach { candidateName ->
                        val defaultAvatarUrl = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("candidates")
                            .add(hashMapOf(
                                "electionId" to electionId,
                                "positionId" to positionId,
                                "positionName" to positionName,
                                "name" to candidateName,
                                "isActive" to true,
                                "photoUrl" to defaultAvatarUrl,
                                "createdBy" to currentLeaderId, // Add leader ID so they show up in Manage Candidates
                                "createdAt" to com.google.firebase.Timestamp.now()
                            ))
                    }
                    completed++
                    if (completed + failed == total) {
                        // Clear temporary positions after saving
                        temporaryPositions = emptyList()
                        android.util.Log.d("Leader_electionsetup", "Saved $completed temporary positions")
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("Leader_electionsetup", "Error saving temporary position: $error")
                    failed++
                    if (completed + failed == total) {
                        android.util.Log.e("Leader_electionsetup", "Failed to save $failed positions")
                    }
                }
            )
        }
    }

    // =========================================================================
    // SUBMIT ELECTION LOGIC
    // =========================================================================

    private fun submitElection() {
        val title = inputTitle.text.toString().trim()
        val startDateStr = inputStartDate.text.toString().trim()
        val startTimeStr = inputStartTime.text.toString().trim()
        val endDateStr = inputEndDate.text.toString().trim()
        val endTimeStr = inputEndTime.text.toString().trim()

        // Parse dates and times
        val startDate = parseDateTime(startDateStr, startTimeStr)
        val endDate = parseDateTime(endDateStr, endTimeStr)

        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Invalid date or time format", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        btnSubmit.isEnabled = false
        Toast.makeText(this, "Creating election...", Toast.LENGTH_SHORT).show()

        // Create election in Firestore
        FirestoreLeaderHelper.createElection(
            title = title,
            startDate = startDate,
            endDate = endDate,
            isAbstainEnabled = cbAgreeTerms.isChecked,
            onSuccess = { electionId ->
                currentElectionId = electionId
                
                // Save temporary positions if any
                if (temporaryPositions.isNotEmpty()) {
                    saveTemporaryPositions(electionId)
                }
                
                // Show success toast
                val inflater = LayoutInflater.from(this)
                val layout = inflater.inflate(R.layout.custom_toast_success, null)
                val titleText: TextView = layout.findViewById(R.id.toast_title)
                val valueText: TextView = layout.findViewById(R.id.toast_value)
                val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

                titleText.text = "Setup submitted!"
                valueText.text = "Awaiting final approval from Election Adviser."
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
            },
            onFailure = { error ->
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun parseDateTime(dateStr: String, timeStr: String): java.util.Date? {
        return try {
            // Use Philippines timezone (UTC+8) explicitly
            val timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
            
            // Parse date: "MMM dd, yyyy" (e.g., "Nov 19, 2025")
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            dateFormat.timeZone = timeZone
            val date = dateFormat.parse(dateStr) ?: return null

            // Manually parse time: "hh:mm AM/PM" (e.g., "11:03 AM" or "07:00 PM")
            val timeParts = timeStr.trim().split(" ")
            if (timeParts.size != 2) return null
            
            val timeComponent = timeParts[0] // "11:03" or "07:00"
            val amPm = timeParts[1].uppercase() // "AM" or "PM"
            
            val hourMinute = timeComponent.split(":")
            if (hourMinute.size != 2) return null
            
            var hour = hourMinute[0].toIntOrNull() ?: return null
            val minute = hourMinute[1].toIntOrNull() ?: return null
            
            // Convert 12-hour format to 24-hour format
            if (amPm == "PM" && hour != 12) {
                hour += 12
            } else if (amPm == "AM" && hour == 12) {
                hour = 0
            }

            // Create calendar in the correct timezone and set all components
            val calendar = java.util.Calendar.getInstance(timeZone)
            val dateCal = java.util.Calendar.getInstance(timeZone)
            dateCal.time = date
            
            // Set all components directly in the calendar with the correct timezone
            calendar.set(
                java.util.Calendar.YEAR,
                dateCal.get(java.util.Calendar.YEAR)
            )
            calendar.set(
                java.util.Calendar.MONTH,
                dateCal.get(java.util.Calendar.MONTH)
            )
            calendar.set(
                java.util.Calendar.DAY_OF_MONTH,
                dateCal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
            calendar.set(java.util.Calendar.MINUTE, minute)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            // Return the Date object (which will be correctly stored in Firestore)
            calendar.time
        } catch (e: Exception) {
            android.util.Log.e("Leader_electionsetup", "Error parsing date/time: ${e.message}", e)
            null
        }
    }


    // =========================================================================
    // DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD) (AS IS)
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

    private fun hideKeyboardAndClearFocus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        currentFocus?.clearFocus()
    }
}