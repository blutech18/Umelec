package com.example.umelec

import android.widget.ImageButton
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.content.Intent
import android.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.MotionEvent
import android.graphics.Rect
import android.view.Gravity
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater

class RegisterActivity2 : AppCompatActivity() {

    // Declare the constants here
    companion object {
        const val MIN_NAME_LENGTH = 2
        const val MAX_NAME_LENGTH = 50
        val VALID_NAME_PATTERN = Regex("^[a-zA-Z\\s'-]+\$")
        val VALID_ID_PATTERN = Regex("^[A-Z][0-9]{8}\$")
    }

    // Define color constants (Used only for requirements TextView text colors)
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")
    private val COLOR_ERROR_RED = Color.parseColor("#D33131")
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#27A688")
    private val COLOR_HINT_GRAY = Color.parseColor("#5C5C77")

    // Dropdown options (Defined at class level for use in helper functions)
    private val genderLevels = listOf("Female", "Male", "Prefer not to say") // 🚨 NEW GENDER OPTIONS
    private val colleges = listOf(
        "College of Liberal Arts and Sciences (CLAS)",
        "College of Human Kinetics (CHK)",
        "College of Continuing, Advanced and Professional Studies (CCAPS)",
        "College of Business and Financial Science (CBFS)",
        "Institute of Arts and Design (IAD)",
        "College of Innovative Teacher Education (CITE)",
        "College of Computing and Information Sciences (CCIS)",
        "Institute of Technical Education and Skills Training (ITEST)"
    )
    private val yearLevels = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")

    // ⚠️ SIMULATION CONSTANT: Use this to test the error dialog
    private val REGISTERED_STUDENT_ID_SIMULATION = "K12345678"

    // Declare all layouts/inputs at class level for use in dispatchTouchEvent and helpers
    private lateinit var layoutStudentID: TextInputLayout
    private lateinit var inputStudentID: TextInputEditText
    private lateinit var layoutFirstname: TextInputLayout
    private lateinit var inputFirstname: TextInputEditText
    private lateinit var layoutLastname: TextInputLayout
    private lateinit var inputLastname: TextInputEditText

    private lateinit var layoutGender: TextInputLayout // 🚨 NEW
    private lateinit var inputGender: AutoCompleteTextView // 🚨 NEW

    private lateinit var layoutYear: TextInputLayout // Redefined for visibility (REORDERED)
    private lateinit var inputYear: AutoCompleteTextView // (REORDERED)

    private lateinit var layoutCollege: TextInputLayout // Redefined for visibility (REORDERED)
    private lateinit var inputCollege: AutoCompleteTextView // (REORDERED)


    // =========================================================================
    // 🚨 0. CORE HELPER FUNCTIONS 🚨
    // =========================================================================

    private fun clearValidationState(layout: TextInputLayout) {
        layout.error = null
        layout.isActivated = false
    }

    private fun hideKeyboardAndClearFocus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        currentFocus?.clearFocus()
    }

    // Firstname validation check function
    private fun isFirstnameValid(name: String): Boolean {
        return name.isNotEmpty() &&
                name.length >= MIN_NAME_LENGTH &&
                name.length <= MAX_NAME_LENGTH &&
                VALID_NAME_PATTERN.matches(name)
    }

    // Lastname validation check function
    private fun isLastnameValid(name: String): Boolean {
        return name.isNotEmpty() &&
                name.length >= MIN_NAME_LENGTH &&
                name.length <= MAX_NAME_LENGTH &&
                VALID_NAME_PATTERN.matches(name)
    }

    // 🚨 NEW GENDER VALIDATION
    private fun isGenderValid(selection: String): Boolean {
        return selection.isNotEmpty() && genderLevels.contains(selection)
    }

    // Year validation check function
    private fun isYearValid(selection: String): Boolean {
        return selection.isNotEmpty() && yearLevels.contains(selection)
    }

    // College validation check function
    private fun isCollegeValid(selection: String): Boolean {
        return selection.isNotEmpty() && colleges.contains(selection)
    }

    /**
     * Shows a custom AlertDialog for a save error.
     */
    private fun showStudentIDErrorDialog(errorMessage: String = "Failed to save user data. Please try again.") {
        val layoutInflater = LayoutInflater.from(this)
        // Ensure R.layout.custom_toast_error exists in your resources
        val dialogView = layoutInflater.inflate(R.layout.custom_toast_error, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        hideKeyboardAndClearFocus()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)

        // Set custom title and value with actual error message
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Registration Error"
        dialogView.findViewById<TextView>(R.id.toast_value).text = errorMessage
        dialogView.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
            // Clear the error/activation state on the Student ID field only
            clearValidationState(layoutStudentID)
        }

        // Trigger Red border ONLY on the Student ID field
        layoutStudentID.error = " "

        // Clear error state on other fields just in case
        layoutFirstname.error = null
        layoutLastname.error = null
        layoutCollege.error = null
        layoutYear.error = null
        layoutGender.error = null // 🚨 NEW

        dialog.show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register2)


        // 🔹 Back button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // =====================================================================
        // 🚨 1. VIEW INITIALIZATION (USING CLASS-LEVEL DECLARATIONS) 🚨
        // =====================================================================

        // ---------------------------------------------------------------------
        // 1.1 Text Field View Initialization (UNCHANGED)
        // ---------------------------------------------------------------------
        layoutStudentID = findViewById(R.id.textInputLayoutStudentID)
        inputStudentID = findViewById(R.id.inputStudentID)
        val studentIDRequirementsContainer = findViewById<View>(R.id.StudentIDRequirements)
        val reqStudentID = findViewById<TextView>(R.id.reqStudentID)

        layoutFirstname = findViewById(R.id.textInputLayoutFirstname)
        inputFirstname = findViewById(R.id.inputFirstname)
        val firstnameRequirementsContainer = findViewById<View>(R.id.FirstnameRequirements)
        val reqFirstname = findViewById<TextView>(R.id.reqFirstname)

        layoutLastname = findViewById(R.id.textInputLayoutLastname)
        inputLastname = findViewById(R.id.inputLastname)
        val lastnameRequirementsContainer = findViewById<View>(R.id.LastnameRequirements)
        val reqLastname = findViewById<TextView>(R.id.reqLastname)

        // ---------------------------------------------------------------------
        // 1.2 Dropdown View Initialization and Setup (REORDERED + NEW GENDER)
        // ---------------------------------------------------------------------

        // 🚨 NEW GENDER DROPDOWN 🚨
        layoutGender = findViewById(R.id.textInputLayoutGender)
        inputGender = findViewById(R.id.inputGender)
        val genderRequirementsContainer = findViewById<View>(R.id.GenderRequirements)
        val reqGender = findViewById<TextView>(R.id.reqGender)

        val genderAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item_custom,
            genderLevels
        )
        inputGender.setAdapter(genderAdapter)

        // YEAR DROPDOWN (Now second dropdown)
        layoutYear = findViewById(R.id.textInputLayoutYear)
        inputYear = findViewById(R.id.inputYear)
        val yearRequirementsContainer = findViewById<View>(R.id.YearRequirements)
        val reqYear = findViewById<TextView>(R.id.reqYear)

        val yearAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item_custom,
            yearLevels
        )
        inputYear.setAdapter(yearAdapter)

        // COLLEGE DROPDOWN (Now third dropdown)
        layoutCollege = findViewById(R.id.textInputLayoutCollege)
        inputCollege = findViewById(R.id.inputCollege)
        val collegeRequirementsContainer = findViewById<View>(R.id.CollegeRequirements)
        val reqCollege = findViewById<TextView>(R.id.reqCollege)

        val collegeAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item_custom,
            colleges
        )
        inputCollege.setAdapter(collegeAdapter)

        // ---------------------------------------------------------------------
        // 1.3 Button Initialization
        // ---------------------------------------------------------------------
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)
        btnConfirm.isEnabled = false


        // =====================================================================
        // 🚨 2. UPDATE BUTTON HELPER 🚨
        // =====================================================================

        // Helper function to update Confirm button state
        fun updateConfirmButtonState() {
            // 1. Get current values from all fields
            val studentID = inputStudentID.text.toString().trim()
            val firstname = inputFirstname.text.toString().trim()
            val lastname = inputLastname.text.toString().trim()
            val genderSelection = inputGender.text.toString().trim() // 🚨 NEW
            val yearSelection = inputYear.text.toString().trim()
            val collegeSelection = inputCollege.text.toString().trim()

            // 2. Check validity
            val isIDValid = VALID_ID_PATTERN.matches(studentID)
            val isFirstnameValid = isFirstnameValid(firstname)
            val isLastnameValid = isLastnameValid(lastname)
            val isGenderValid = isGenderValid(genderSelection) // 🚨 NEW
            val isYearValid = isYearValid(yearSelection)
            val isCollegeValid = isCollegeValid(collegeSelection)

            // 3. Combine all valid checks (UPDATED)
            val allFieldsValid = isIDValid && isFirstnameValid && isLastnameValid && isGenderValid && isYearValid && isCollegeValid

            // 4. Set the button state
            btnConfirm.isEnabled = allFieldsValid
        }


        // =====================================================================
        // 🚨 3. VALIDATION LISTENERS BLOCK (CLEANED UP STATE HANDLING) 🚨
        // =====================================================================

        // ---------------------------------------------------------------------
        // 🔹 STUDENT ID VALIDATION LISTENERS (UNCHANGED)
        // ---------------------------------------------------------------------
        studentIDRequirementsContainer.visibility = View.GONE

        inputStudentID.setOnFocusChangeListener { _, hasFocus ->
            val studentID = inputStudentID.text.toString().trim()
            val isValid = VALID_ID_PATTERN.matches(studentID)

            clearValidationState(layoutStudentID)

            if (hasFocus) {
                studentIDRequirementsContainer.visibility = View.VISIBLE
                if (isValid) {
                    reqStudentID.setTextColor(COLOR_SUCCESS_GREEN)
                    layoutStudentID.boxStrokeColor = COLOR_SUCCESS_GREEN
                } else {
                    reqStudentID.setTextColor(COLOR_HINT_GRAY)
                    layoutStudentID.boxStrokeColor = COLOR_PRIMARY_BLUE
                }
            } else {
                if (studentID.isEmpty()) {
                    studentIDRequirementsContainer.visibility = View.VISIBLE
                    reqStudentID.setTextColor(COLOR_ERROR_RED)
                    reqStudentID.text = "• Field is required"
                    layoutStudentID.error = " " // Show red border
                } else if (isValid) {
                    studentIDRequirementsContainer.visibility = View.GONE
                    layoutStudentID.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutStudentID.isActivated = true
                } else {
                    studentIDRequirementsContainer.visibility = View.VISIBLE
                    layoutStudentID.error = " " // Show red border
                }
            }
            updateConfirmButtonState()
        }

        inputStudentID.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutStudentID)
            }

            override fun afterTextChanged(s: Editable?) {
                var text = s.toString().trim()

                // 1. 🔠 Auto uppercase
                if (text != text.uppercase()) {
                    inputStudentID.removeTextChangedListener(this)
                    inputStudentID.setText(text.uppercase())
                    inputStudentID.setSelection(inputStudentID.text?.length ?: 0)
                    inputStudentID.addTextChangedListener(this)
                    text = inputStudentID.text.toString().trim()
                }

                clearValidationState(layoutStudentID)

                // 2. 🔹 Validation Logic
                when {
                    // 🚫 ID is too long OR ID is invalid format
                    text.length > 9 || (text.isNotEmpty() && !VALID_ID_PATTERN.matches(text) && text.length == 9) -> {
                        reqStudentID.setTextColor(COLOR_ERROR_RED)
                        reqStudentID.text = "• Invalid Student ID (Format: K12345678)"
                        layoutStudentID.boxStrokeColor = COLOR_ERROR_RED
                        studentIDRequirementsContainer.visibility = View.VISIBLE
                    }

                    // ✅ ID is complete AND matches the pattern
                    VALID_ID_PATTERN.matches(text) -> {
                        reqStudentID.setTextColor(COLOR_SUCCESS_GREEN)
                        reqStudentID.text = "✓ Valid Student ID"
                        layoutStudentID.boxStrokeColor = COLOR_SUCCESS_GREEN
                        studentIDRequirementsContainer.visibility = View.VISIBLE
                        if (inputStudentID.isFocused) layoutStudentID.isActivated = true
                    }

                    // 🩶 Still typing, following the pattern (or empty)
                    else -> {
                        reqStudentID.setTextColor(COLOR_HINT_GRAY)
                        reqStudentID.text = "• Valid ID: K12345678"
                        layoutStudentID.boxStrokeColor = COLOR_PRIMARY_BLUE
                        studentIDRequirementsContainer.visibility = View.VISIBLE
                    }
                }
                updateConfirmButtonState()
            }
        })


        // ---------------------------------------------------------------------
        // 🔹 FIRSTNAME VALIDATION LISTENERS (UNCHANGED)
        // ---------------------------------------------------------------------
        firstnameRequirementsContainer.visibility = View.GONE

        inputFirstname.setOnFocusChangeListener { _, hasFocus ->
            val firstname = inputFirstname.text.toString().trim()
            val isValid = isFirstnameValid(firstname)

            clearValidationState(layoutFirstname)

            if (hasFocus) {
                firstnameRequirementsContainer.visibility = View.VISIBLE
                if (isValid) {
                    //reqFirstname.setTextColor(COLOR_SUCCESS_GREEN)
                    //layoutFirstname.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutFirstname.boxStrokeColor = COLOR_PRIMARY_BLUE
                } else {
                    reqFirstname.setTextColor(COLOR_HINT_GRAY)
                    layoutFirstname.boxStrokeColor = COLOR_PRIMARY_BLUE
                }
            } else {
                if (firstname.isEmpty()) {
                    firstnameRequirementsContainer.visibility = View.VISIBLE
                    reqFirstname.setTextColor(COLOR_ERROR_RED)
                    reqFirstname.text = "• Field is required"
                    layoutFirstname.error = " "
                } else if (isValid) {
                    firstnameRequirementsContainer.visibility = View.GONE
                    layoutFirstname.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutFirstname.isActivated = true
                } else {
                    firstnameRequirementsContainer.visibility = View.VISIBLE
                    layoutFirstname.error = " "
                }
            }
            updateConfirmButtonState()
        }

        inputFirstname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutFirstname)
            }

            override fun afterTextChanged(s: Editable?) {
                var text = s.toString()

                // 1. 🔠 Auto capitalize first letter of each word
                val capitalizedText = text.lowercase().split(' ').joinToString(" ") {
                    it.replaceFirstChar { char -> if (char.isLetter()) char.uppercase() else char.toString() }
                }

                if (text != capitalizedText) {
                    inputFirstname.removeTextChangedListener(this)
                    inputFirstname.setText(capitalizedText)
                    inputFirstname.setSelection(capitalizedText.length)
                    inputFirstname.addTextChangedListener(this)
                    text = inputFirstname.text.toString()
                }

                clearValidationState(layoutFirstname)

                // 2. 🔹 Validation Logic
                when {
                    // 🚫 Invalid characters
                    !VALID_NAME_PATTERN.matches(text) && text.isNotEmpty() -> {
                        reqFirstname.setTextColor(COLOR_ERROR_RED)
                        reqFirstname.text = "• Firstname cannot contain numbers or symbols."
                        layoutFirstname.boxStrokeColor = COLOR_ERROR_RED
                    }

                    // 🚫 Too short
                    text.length > 0 && text.length < MIN_NAME_LENGTH -> {
                        //reqFirstname.setTextColor(COLOR_ERROR_RED)
                        //reqFirstname.text = "• Firstname must be at least ${MIN_NAME_LENGTH} characters."
                        //layoutFirstname.boxStrokeColor = COLOR_ERROR_RED
                        layoutFirstname.boxStrokeColor = COLOR_PRIMARY_BLUE
                    }

                    // ✅ All good
                    isFirstnameValid(text) -> {
                        //reqFirstname.setTextColor(COLOR_SUCCESS_GREEN)
                        //reqFirstname.text = "✓ Valid Firstname"
                        //layoutFirstname.boxStrokeColor = COLOR_SUCCESS_GREEN
                        //if (inputFirstname.isFocused) layoutFirstname.isActivated = true
                        //firstnameRequirementsContainer.visibility = View.VISIBLE
                        layoutFirstname.boxStrokeColor = COLOR_PRIMARY_BLUE
                    }

                    // 🩶 Default typing state
                    else -> {
                        reqFirstname.setTextColor(COLOR_HINT_GRAY)
                        reqFirstname.text = "• Field is required."
                        layoutFirstname.boxStrokeColor = COLOR_PRIMARY_BLUE
                        firstnameRequirementsContainer.visibility = View.VISIBLE
                    }
                }
                updateConfirmButtonState()
            }
        })

        // ---------------------------------------------------------------------
        // 🔹 LASTNAME VALIDATION LISTENERS (UNCHANGED)
        // ---------------------------------------------------------------------
        lastnameRequirementsContainer.visibility = View.GONE

        inputLastname.setOnFocusChangeListener { _, hasFocus ->
            val lastname = inputLastname.text.toString().trim()
            val isValid = isLastnameValid(lastname)

            clearValidationState(layoutLastname)

            if (hasFocus) {
                lastnameRequirementsContainer.visibility = View.VISIBLE
                if (isValid) {
                    //reqLastname.setTextColor(COLOR_SUCCESS_GREEN)
                    //layoutLastname.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutLastname.boxStrokeColor = COLOR_PRIMARY_BLUE
                } else {
                    reqLastname.setTextColor(COLOR_HINT_GRAY)
                    layoutLastname.boxStrokeColor = COLOR_PRIMARY_BLUE
                }
            } else {
                if (lastname.isEmpty()) {
                    lastnameRequirementsContainer.visibility = View.VISIBLE
                    reqLastname.setTextColor(COLOR_ERROR_RED)
                    reqLastname.text = "• Field is required"
                    layoutLastname.error = " "
                } else if (isValid) {
                    lastnameRequirementsContainer.visibility = View.GONE
                    layoutLastname.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutLastname.isActivated = true
                } else {
                    lastnameRequirementsContainer.visibility = View.VISIBLE
                    layoutLastname.error = " "
                }
            }
            updateConfirmButtonState()
        }

        inputLastname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutLastname)
            }

            override fun afterTextChanged(s: Editable?) {
                var text = s.toString()

                // 1. 🔠 Auto capitalize first letter of each word
                val capitalizedText = text.lowercase().split(' ').joinToString(" ") {
                    it.replaceFirstChar { char -> if (char.isLetter()) char.uppercase() else char.toString() }
                }

                if (text != capitalizedText) {
                    inputLastname.removeTextChangedListener(this)
                    inputLastname.setText(capitalizedText)
                    inputLastname.setSelection(capitalizedText.length)
                    inputLastname.addTextChangedListener(this)
                    text = inputLastname.text.toString()
                }

                clearValidationState(layoutLastname)

                // 2. 🔹 Validation Logic
                when {
                    // 🚫 Invalid characters
                    !VALID_NAME_PATTERN.matches(text) && text.isNotEmpty() -> {
                        reqLastname.setTextColor(COLOR_ERROR_RED)
                        reqLastname.text = "• Lastname cannot contain numbers or symbols."
                        layoutLastname.boxStrokeColor = COLOR_ERROR_RED
                    }

                    // 🚫 Too short
                    text.length > 0 && text.length < MIN_NAME_LENGTH -> {
                        //reqLastname.setTextColor(COLOR_ERROR_RED)
                        //reqLastname.text = "• Lastname must be at least ${MIN_NAME_LENGTH} characters."
                        //layoutLastname.boxStrokeColor = COLOR_ERROR_RED
                        layoutLastname.boxStrokeColor = COLOR_PRIMARY_BLUE
                    }

                    // ✅ All good
                    isLastnameValid(text) -> {
                        //reqLastname.setTextColor(COLOR_SUCCESS_GREEN)
                        //reqLastname.text = "✓ Valid Lastname"
                        //layoutLastname.boxStrokeColor = COLOR_SUCCESS_GREEN
                        //if (inputLastname.isFocused) layoutLastname.isActivated = true
                        //lastnameRequirementsContainer.visibility = View.VISIBLE
                        layoutLastname.boxStrokeColor = COLOR_PRIMARY_BLUE
                    }

                    // 🩶 Default typing state
                    else -> {
                        reqLastname.setTextColor(COLOR_HINT_GRAY)
                        reqLastname.text = "• Field is required."
                        layoutLastname.boxStrokeColor = COLOR_PRIMARY_BLUE
                        lastnameRequirementsContainer.visibility = View.VISIBLE
                    }
                }
                updateConfirmButtonState()
            }
        })


        // ---------------------------------------------------------------------
        // 🔹 GENDER DROPDOWN VALIDATION LISTENERS (NEW FIELD)
        // ---------------------------------------------------------------------
        genderRequirementsContainer.visibility = View.GONE
        inputGender.setOnFocusChangeListener { _, hasFocus ->
            val genderSelection = inputGender.text.toString().trim()
            val isValid = isGenderValid(genderSelection)

            clearValidationState(layoutGender)

            if (hasFocus) {
                genderRequirementsContainer.visibility = View.VISIBLE
                layoutGender.boxStrokeColor = COLOR_PRIMARY_BLUE

                if (isValid) {
                    //reqGender.setTextColor(COLOR_SUCCESS_GREEN)
                    //reqGender.text = "✓ Gender selected"
                } else {
                    reqGender.setTextColor(COLOR_HINT_GRAY)
                    reqGender.text = "• Select your gender from the list"
                }
            } else {
                if (genderSelection.isEmpty()) {
                    genderRequirementsContainer.visibility = View.VISIBLE
                    reqGender.setTextColor(COLOR_ERROR_RED)
                    reqGender.text = "• Field is required"
                    layoutGender.error = " "
                } else if (isValid) {
                    genderRequirementsContainer.visibility = View.GONE
                    layoutGender.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutGender.isActivated = true
                } else {
                    genderRequirementsContainer.visibility = View.VISIBLE
                    layoutGender.error = " "
                }
            }
            updateConfirmButtonState()
        }

        inputGender.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutGender)
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()

                clearValidationState(layoutGender)

                if (text.isEmpty()) {
                    reqGender.setTextColor(COLOR_HINT_GRAY)
                    reqGender.text = "• Select your gender from the list"
                    layoutGender.boxStrokeColor = COLOR_PRIMARY_BLUE
                    genderRequirementsContainer.visibility = View.VISIBLE
                } else if (genderLevels.contains(text)) {
                    //reqGender.setTextColor(COLOR_SUCCESS_GREEN)
                    //reqGender.text = "✓ Gender selected"
                    //layoutGender.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutGender.boxStrokeColor = COLOR_PRIMARY_BLUE
                    if (inputGender.isFocused) layoutGender.isActivated = true
                    genderRequirementsContainer.visibility = View.VISIBLE
                } else {
                    reqGender.setTextColor(COLOR_ERROR_RED)
                    reqGender.text = "• Invalid selection. Please choose from the options."
                    layoutGender.boxStrokeColor = COLOR_ERROR_RED
                    genderRequirementsContainer.visibility = View.VISIBLE
                }
                updateConfirmButtonState()
            }
        })


        // ---------------------------------------------------------------------
        // 🔹 YEAR DROPDOWN VALIDATION LISTENERS (REORDERED)
        // ---------------------------------------------------------------------
        yearRequirementsContainer.visibility = View.GONE

        inputYear.setOnFocusChangeListener { _, hasFocus ->
            val yearSelection = inputYear.text.toString().trim()
            val isValid = isYearValid(yearSelection)

            clearValidationState(layoutYear)

            if (hasFocus) {
                yearRequirementsContainer.visibility = View.VISIBLE
                layoutYear.boxStrokeColor = COLOR_PRIMARY_BLUE

                if (isValid) {
                    //reqYear.setTextColor(COLOR_SUCCESS_GREEN)
                    //reqYear.text = "✓ Year level selected"
                } else {
                    reqYear.setTextColor(COLOR_HINT_GRAY)
                    reqYear.text = "• Select a year level from the options"
                }
            } else {
                if (yearSelection.isEmpty()) {
                    yearRequirementsContainer.visibility = View.VISIBLE
                    reqYear.setTextColor(COLOR_ERROR_RED)
                    reqYear.text = "• Field is required"
                    layoutYear.error = " "
                } else if (isValid) {
                    yearRequirementsContainer.visibility = View.GONE
                    layoutYear.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutYear.isActivated = true
                } else {
                    yearRequirementsContainer.visibility = View.VISIBLE
                    layoutYear.error = " "
                }
            }
            updateConfirmButtonState()
        }

        inputYear.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutYear)
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()

                clearValidationState(layoutYear)

                if (text.isEmpty()) {
                    reqYear.setTextColor(COLOR_HINT_GRAY)
                    reqYear.text = "• Select a year level from the options"
                    layoutYear.boxStrokeColor = COLOR_PRIMARY_BLUE
                    yearRequirementsContainer.visibility = View.VISIBLE
                } else if (yearLevels.contains(text)) {
                    //reqYear.setTextColor(COLOR_SUCCESS_GREEN)
                    //reqYear.text = "✓ Year level selected"
                    //layoutYear.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutYear.boxStrokeColor = COLOR_PRIMARY_BLUE
                    if (inputYear.isFocused) layoutYear.isActivated = true
                    yearRequirementsContainer.visibility = View.VISIBLE
                } else {
                    reqYear.setTextColor(COLOR_ERROR_RED)
                    reqYear.text = "• Invalid selection. Please choose from the options."
                    layoutYear.boxStrokeColor = COLOR_ERROR_RED
                    yearRequirementsContainer.visibility = View.VISIBLE
                }
                updateConfirmButtonState()
            }
        })

        // ---------------------------------------------------------------------
        // 🔹 COLLEGE DROPDOWN VALIDATION LISTENERS (REORDERED)
        // ---------------------------------------------------------------------
        collegeRequirementsContainer.visibility = View.GONE
        inputCollege.setOnFocusChangeListener { _, hasFocus ->
            val collegeSelection = inputCollege.text.toString().trim()
            val isValid = isCollegeValid(collegeSelection)

            clearValidationState(layoutCollege)

            if (hasFocus) {
                collegeRequirementsContainer.visibility = View.VISIBLE
                layoutCollege.boxStrokeColor = COLOR_PRIMARY_BLUE

                if (isValid) {
                    //reqCollege.setTextColor(COLOR_SUCCESS_GREEN)
                    //reqCollege.text = "✓ College selected"
                } else {
                    reqCollege.setTextColor(COLOR_HINT_GRAY)
                    reqCollege.text = "• Select a college from the list"
                }
            } else {
                if (collegeSelection.isEmpty()) {
                    collegeRequirementsContainer.visibility = View.VISIBLE
                    reqCollege.setTextColor(COLOR_ERROR_RED)
                    reqCollege.text = "• Field is required"
                    layoutCollege.error = " "
                } else if (isValid) {
                    collegeRequirementsContainer.visibility = View.GONE
                    layoutCollege.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutCollege.isActivated = true
                } else {
                    collegeRequirementsContainer.visibility = View.VISIBLE
                    layoutCollege.error = " "
                }
            }
            updateConfirmButtonState()
        }

        inputCollege.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearValidationState(layoutCollege)
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()

                clearValidationState(layoutCollege)

                if (text.isEmpty()) {
                    reqCollege.setTextColor(COLOR_HINT_GRAY)
                    reqCollege.text = "• Select a college from the list"
                    layoutCollege.boxStrokeColor = COLOR_PRIMARY_BLUE
                    collegeRequirementsContainer.visibility = View.VISIBLE
                } else if (colleges.contains(text)) {
                    //reqCollege.setTextColor(COLOR_SUCCESS_GREEN)
                    //reqCollege.text = "✓ College selected"
                    //layoutCollege.boxStrokeColor = COLOR_SUCCESS_GREEN
                    layoutCollege.boxStrokeColor = COLOR_PRIMARY_BLUE
                    if (inputCollege.isFocused) layoutCollege.isActivated = true
                    collegeRequirementsContainer.visibility = View.VISIBLE
                } else {
                    reqCollege.setTextColor(COLOR_ERROR_RED)
                    reqCollege.text = "• Invalid selection. Please choose from the options."
                    layoutCollege.boxStrokeColor = COLOR_ERROR_RED
                    collegeRequirementsContainer.visibility = View.VISIBLE
                }
                updateConfirmButtonState()
            }
        })

        // ---------------------------------------------------------------------
        // 🔹 BUTTON CLICK LISTENER (Final step)
        // ---------------------------------------------------------------------

        btnConfirm.setOnClickListener {
            hideKeyboardAndClearFocus()

            val studentID = inputStudentID.text.toString().trim()
            val firstname = inputFirstname.text.toString().trim()
            val lastname = inputLastname.text.toString().trim()
            val gender = inputGender.text.toString().trim()
            val year = inputYear.text.toString().trim()
            val college = inputCollege.text.toString().trim()

            // Get current Firebase user
            val currentUser = FirebaseAuthHelper.getCurrentUser()
            if (currentUser == null) {
                // User not authenticated, redirect to login
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                overridePendingTransition(0, 0)
                return@setOnClickListener
            }

            // Disable button during save
            btnConfirm.isEnabled = false

            // Prepare user data for Firestore
            val userData = hashMapOf<String, Any>(
                "studentId" to studentID,
                "firstname" to firstname,
                "lastname" to lastname,
                "gender" to gender,
                "year" to year,
                "college" to college,
                "email" to (currentUser.email ?: ""),
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            // Save user data to Firestore
            FirebaseAuthHelper.saveUserDataToFirestore(
                userId = currentUser.uid,
                userData = userData,
                onSuccess = {
                    // Success: Proceed to the final registration step
                    val intent = Intent(this, RegisterActivity3::class.java)
                    startActivity(intent)
                },
                onFailure = { errorMessage ->
                    // Re-enable button
                    btnConfirm.isEnabled = true
                    
                    // Log error for debugging
                    android.util.Log.e("RegisterActivity2", "Failed to save user data: $errorMessage")
                    
                    // Show error dialog with actual error message
                    // Format error message for user display
                    val displayMessage = when {
                        errorMessage.contains("PERMISSION_DENIED", ignoreCase = true) -> 
                            "Permission denied. Please check your internet connection and try again."
                        errorMessage.contains("network", ignoreCase = true) -> 
                            "Network error. Please check your internet connection and try again."
                        errorMessage.contains("already exists", ignoreCase = true) -> 
                            "User data already exists. Please continue to the next step."
                        else -> 
                            "Failed to save user data: $errorMessage"
                    }
                    
                    showStudentIDErrorDialog(displayMessage)
                }
            )
        }
    } // End of onCreate

    // =========================================================================
    // 8. DISPATCH TOUCH EVENT (CLICK OUTSIDE TO UNFOCUS/HIDE KEYBOARD)
    // =========================================================================
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText || v is AutoCompleteTextView) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndClearFocus()

                    // Manually trigger the blur logic for all fields on click outside
                    //inputStudentID.clearFocus()
                    //inputFirstname.clearFocus()
                    //inputLastname.clearFocus()
                    //inputCollege.clearFocus()
                    //inputYear.clearFocus()
                    clearValidationState(layoutStudentID)
                    clearValidationState(layoutFirstname)
                    clearValidationState(layoutLastname)
                    clearValidationState(layoutGender) // 🚨 NEW
                    clearValidationState(layoutCollege)
                    clearValidationState(layoutYear)

                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}