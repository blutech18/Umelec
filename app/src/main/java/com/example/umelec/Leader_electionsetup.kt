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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Leader_electionsetup : AppCompatActivity() {

    // Define color constant (AS IS from Leader_manage_voters_list.kt)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_DEFAULT_GRAY = Color.parseColor("#8C8CA1") // Based on your XML

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
        loadPositionsCount()
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
            // Navigate to Leader_election_setup_position.kt
            val intent = Intent(this, Leader_electionsetup_position::class.java)
            currentElectionId?.let { intent.putExtra("electionId", it) }
            startActivity(intent)
        }

        btnPreview.setOnClickListener {
            // Navigate to Leader_electionsetup_preview.kt
            startActivity(Intent(this, Leader_electionsetup_preview::class.java))
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

        // 3. Check for Position Data (Must have positions)
        val hasPositions = positionsCount > 0

        // Enable buttons only if all conditions are met
        val isFormValid = allFieldsFilled && termsChecked && hasPositions

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
        } ?: run {
            positionsCount = 0
            updatePositionsUI()
        }
    }

    private fun updatePositionsUI() {
        if (positionsCount > 0) {
            tvPosition.text = "$positionsCount position(s) added"
        } else {
            tvPosition.text = "Add at least 1 position"
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
            // Parse date: "MMM dd, yyyy" (e.g., "Oct 15, 2025")
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val date = dateFormat.parse(dateStr) ?: return null

            // Parse time: "hh:mm AM/PM" (e.g., "05:00 PM")
            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val time = timeFormat.parse(timeStr) ?: return null

            // Combine date and time
            val calendar = java.util.Calendar.getInstance()
            val dateCal = java.util.Calendar.getInstance()
            dateCal.time = date
            val timeCal = java.util.Calendar.getInstance()
            timeCal.time = time

            calendar.set(
                dateCal.get(java.util.Calendar.YEAR),
                dateCal.get(java.util.Calendar.MONTH),
                dateCal.get(java.util.Calendar.DAY_OF_MONTH),
                timeCal.get(java.util.Calendar.HOUR_OF_DAY),
                timeCal.get(java.util.Calendar.MINUTE),
                0
            )
            calendar.time
        } catch (e: Exception) {
            android.util.Log.e("Leader_electionsetup", "Error parsing date/time: ${e.message}")
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