package com.example.umelec

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.collections.ArrayList

// Data class to hold the details of a position
data class PositionDetails(
    val title: String,
    val yearLevel: String,
    val candidates: ArrayList<String>
)

class Leader_electionsetup_position : AppCompatActivity() {

    // Helper constant for the focus color
    private val COLOR_PRIMARY_BLUE = Color.parseColor("#00537A")

    // List to store all added positions
    private val positionsList = ArrayList<PositionDetails>()

    // View references
    private lateinit var btnBack: ImageButton
    private lateinit var btnAddPosition: AppCompatButton
    private lateinit var noPositionLayout: LinearLayout
    private lateinit var positionCardContainer: LinearLayout
    private lateinit var btnSubmit: Button
    // Removed: private lateinit var positionCardTemplate: View // No longer needed as it's a separate XML file

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_electionsetup_position)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        btnAddPosition = findViewById(R.id.btnAddPosition)
        noPositionLayout = findViewById(R.id.NoPositionLayout)
        positionCardContainer = findViewById(R.id.ReportContainer) // Assuming ReportContainer is the LinearLayout parent
        btnSubmit = findViewById(R.id.btnSubmit)

        // Removed logic to find and remove PositionCardTemplate

        // 7. when btnBack is clicked goes back to the last activity finish()
        btnBack.setOnClickListener {
            finish()
        }

        // 2. when btnAddPosition is clicked shows a bottom sheet dialog
        btnAddPosition.setOnClickListener {
            showAddPositionBottomSheet(null)
        }

        // Initial check for submit button and NoPositionLayout visibility
        updateUIState()
    }

    /**
     * Updates the visibility of NoPositionLayout and the enabled state of btnSubmit
     * based on the contents of positionsList.
     */
    private fun updateUIState() {
        if (positionsList.isEmpty()) {
            noPositionLayout.visibility = View.VISIBLE
            btnSubmit.isEnabled = false
        } else {
            noPositionLayout.visibility = View.GONE
            btnSubmit.isEnabled = true
            // 6. when btnSubmit is clicked:
            btnSubmit.setOnClickListener {
                // Guide for backend/database:
                // All the data inside the positionsList (List<PositionDetails>)
                // should be stored in the database. Each PositionDetails object
                // contains the position 'title', the 'yearLevel' allowed to vote,
                // and a list of 'candidates' for that position.
            }
        }
    }

    /**
     * Inflates and adds a PositionCard to the UI for each PositionDetails object.
     */
    private fun renderPositionCards() {
        // Clear all dynamically inflated cards
        positionCardContainer.removeAllViews()

        // Re-add NoPositionLayout (temporarily, as updateUIState will control its visibility)
        positionCardContainer.addView(noPositionLayout)


        positionsList.forEachIndexed { index, position ->
            val positionCard = createPositionCardView(position, index)
            // Add the new card
            positionCardContainer.addView(positionCard)
        }

        // Call updateUIState to handle visibility of NoPositionLayout
        updateUIState()
    }

    /**
     * Creates a single PositionCard view from the item_election_position template and populates its data.
     * **Updated to use R.layout.item_election_position**
     */
    private fun createPositionCardView(position: PositionDetails, index: Int): View {
        // Use the dedicated layout file for inflation. Set attachToRoot to false.
        // FIX: Using R.layout.item_election_position
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_election_position, positionCardContainer, false)

        // 3. The data will get catched by PositionCard
        cardView.findViewById<TextView>(R.id.PositionTitle).text = position.title

        cardView.findViewById<TextView>(R.id.WhatYearLevel).text = "- ${position.yearLevel}"

        val candidateNamesView = cardView.findViewById<TextView>(R.id.CandidatesNames)
        val candidatesText = position.candidates.joinToString("\n") { "• $it" }
        candidateNamesView.text = candidatesText

        // Set up the listeners for edit and remove buttons
        val btnEdit = cardView.findViewById<ImageView>(R.id.btnFaqEdit)
        val btnRemove = cardView.findViewById<ImageView>(R.id.btnFaqRemove)

        // 4. when the btnFaqEdit in the PositionCard is clicked...
        btnEdit.setOnClickListener {
            showAddPositionBottomSheet(position)
        }

        // 5. when the btnFaqRemove in the PositionCard is clicked...
        btnRemove.setOnClickListener {
            showRemovePositionDialog(index)
        }

        return cardView
    }

    /**
     * Shows the Bottom Sheet Dialog for adding or editing a position.
     */
    private fun showAddPositionBottomSheet(positionToEdit: PositionDetails?) {
        val dialog = BottomSheetDialog(this)
        // Use the correct R.layout.bottom_sheet_addposition
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_addposition, null)
        dialog.setContentView(dialogView)

        // View references
        val sheetTitle = dialogView.findViewById<TextView>(R.id.sheetTitle)
        val inputPositionLayout = dialogView.findViewById<TextInputLayout>(R.id.AddPosition)
        val inputPosition = dialogView.findViewById<TextInputEditText>(R.id.inputPosition)
        val inputYearLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutYear)
        val inputYear = dialogView.findViewById<AutoCompleteTextView>(R.id.inputYear)
        val btnAddCandidate = dialogView.findViewById<AppCompatButton>(R.id.btnAddCandidate)
        val candidateWrapper = dialogView.findViewById<LinearLayout>(R.id.CandidateWrapper)
        val btnCancel = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnAction = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // Candidates list specific to this bottom sheet instance
        val currentCandidates = positionToEdit?.candidates?.toMutableList() ?: mutableListOf()

        // 2.4 textInputLayoutYear/inputYear is a drop down
        val yearLevels = arrayOf("1st year", "2nd year", "3rd year", "4th year", "All year level")
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearLevels)
        inputYear.setAdapter(yearAdapter)

        // 2.5 Set up the focus change listeners for blue outline
        setInputOutlineColorListener(inputPositionLayout)
        setInputOutlineColorListener(inputYearLayout)

        // Check if editing or adding
        if (positionToEdit != null) {
            sheetTitle.text = "Edit Position"
            btnAction.text = "Save"
            inputPosition.setText(positionToEdit.title)
            inputYear.setText(positionToEdit.yearLevel, false)
            renderCandidateChips(candidateWrapper, currentCandidates, dialog)
        } else {
            sheetTitle.text = "Add Position"
            btnAction.text = "Add"
        }

        // --- Candidates Logic ---
        btnAddCandidate.setOnClickListener {
            showAddCandidateBottomSheet(candidateWrapper, currentCandidates, dialog)
        }

        // --- Action Buttons Logic ---
        btnCancel.setOnClickListener {
            showUnsavedChangesDialog(dialog)
        }

        // 2.7 if btn_action_secondary in bottom_sheet_addposition is clicked...
        btnAction.setOnClickListener {
            val title = inputPosition.text.toString().trim()
            val yearLevel = inputYear.text.toString().trim()

            // Basic validation
            if (title.isEmpty() || yearLevel.isEmpty() || currentCandidates.isEmpty()) {
                if (title.isEmpty()) inputPositionLayout.error = "Position is required"
                if (yearLevel.isEmpty()) inputYearLayout.error = "Year Level is required"
                return@setOnClickListener
            }

            val newPositionDetails = PositionDetails(title, yearLevel, ArrayList(currentCandidates))

            if (positionToEdit != null) {
                // Find and update the existing position
                val index = positionsList.indexOf(positionToEdit)
                if (index != -1) {
                    positionsList[index] = newPositionDetails
                }
            } else {
                // Add new position
                positionsList.add(newPositionDetails)
            }

            renderPositionCards()
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Renders candidate names as dynamic views inside the CandidateWrapper.
     * **Uses R.layout.item_candidate_chip**
     */
    private fun renderCandidateChips(candidateWrapper: LinearLayout, candidates: MutableList<String>, positionDialog: BottomSheetDialog) {
        // Clear all existing views before re-rendering
        candidateWrapper.removeAllViews()

        if (candidates.isEmpty()) return

        val inflater = LayoutInflater.from(this)

        candidates.forEachIndexed { index, name ->
            // FIX: Directly inflate the dedicated chip layout, attaching it to the wrapper but delaying attachment
            val candidateView = inflater.inflate(R.layout.item_candidate_chip, candidateWrapper, false)

            // Now, populate and add the view
            candidateView.findViewById<TextView>(R.id.CandidatesNames).text = name

            val btnEdit = candidateView.findViewById<ImageView>(R.id.btnFaqEdit)
            val btnRemove = candidateView.findViewById<ImageView>(R.id.btnFaqRemove)

            // 2.3 when btnFaqEdit is clicked shows a bottom sheet dialog (Edit Candidate)
            btnEdit.setOnClickListener {
                showEditCandidateBottomSheet(
                    candidateWrapper,
                    candidates,
                    index,
                    name,
                    positionDialog
                )
            }

            // 2.2 when btnFaqRemove is clicked shows a warning dialog (Remove Candidate)
            btnRemove.setOnClickListener {
                showRemoveCandidateDialog(
                    candidateWrapper,
                    candidates,
                    index,
                    positionDialog
                )
            }

            // 4. ADD the now unparented view to the final container
            candidateWrapper.addView(candidateView)
        }
    }


    /**
     * Shows the Bottom Sheet Dialog for adding a new candidate.
     */
    private fun showAddCandidateBottomSheet(
        candidateWrapper: LinearLayout,
        candidates: MutableList<String>,
        positionDialog: BottomSheetDialog
    ) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_candidates, null)
        dialog.setContentView(dialogView)

        // View references
        val inputCandidateLayout = dialogView.findViewById<TextInputLayout>(R.id.AddCandidate)
        val inputCandidate = dialogView.findViewById<TextInputEditText>(R.id.inputCandidate)
        val btnCancel = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnAdd = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)
        btnAdd.isEnabled = false

        // 2.5 Set up the focus change listeners for blue outline
        setInputOutlineColorListener(inputCandidateLayout)

        // 2.1.2 if AddCandidate/inputCandidate is empty disable btn_action_secondary, if not empty enable
        inputCandidate.doOnTextChanged { text, _, _, _ ->
            btnAdd.isEnabled = !text.isNullOrBlank()
            if (btnAdd.isEnabled) {
                inputCandidateLayout.error = null
            }
        }

        // 2.1.1 when btn_action_primary is clicked closes only the add candidate dialog
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 2.1.2.1/2.1.2.2 when btn_action_secondary is clicked, adds candidate and inflates CandidateWrapper
        btnAdd.setOnClickListener {
            val candidateName = inputCandidate.text.toString().trim()

            if (candidateName.isNotEmpty()) {
                candidates.add(candidateName)
                renderCandidateChips(candidateWrapper, candidates, positionDialog)
                dialog.dismiss()
            } else {
                inputCandidateLayout.error = "Candidate name is required"
            }
        }

        dialog.show()
    }

    /**
     * Shows the Bottom Sheet Dialog for editing an existing candidate.
     */
    private fun showEditCandidateBottomSheet(
        candidateWrapper: LinearLayout,
        candidates: MutableList<String>,
        index: Int,
        originalName: String,
        positionDialog: BottomSheetDialog
    ) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_candidates, null)
        dialog.setContentView(dialogView)

        // View references
        dialogView.findViewById<TextView>(R.id.sheetTitle).text = "Edit Candidate"
        val inputCandidateLayout = dialogView.findViewById<TextInputLayout>(R.id.AddCandidate)
        val inputCandidate = dialogView.findViewById<TextInputEditText>(R.id.inputCandidate)
        val btnCancel = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnSave = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)
        btnSave.text = "Save"

        // 2.5 Set up the focus change listeners for blue outline
        setInputOutlineColorListener(inputCandidateLayout)

        // 2.3.2 inputCandidate should have the CandidatesNames of the corresponding CandidateNameLayout
        inputCandidate.setText(originalName)
        btnSave.isEnabled = true

        // Ensure it stays enabled if text changes
        inputCandidate.doOnTextChanged { text, _, _, _ ->
            btnSave.isEnabled = !text.isNullOrBlank()
            if (btnSave.isEnabled) {
                inputCandidateLayout.error = null
            }
        }

        // 2.3.4 when btn_action_primary is clicked closes only the edit candidate dialog
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 2.3.3 when btn_action_secondary is clicked updates the CandidatesNames
        btnSave.setOnClickListener {
            val newName = inputCandidate.text.toString().trim()

            if (newName.isNotEmpty()) {
                candidates[index] = newName
                renderCandidateChips(candidateWrapper, candidates, positionDialog)
                dialog.dismiss()
            } else {
                inputCandidateLayout.error = "Candidate name is required"
            }
        }

        dialog.show()
    }

    /**
     * Shows the warning dialog for removing a candidate.
     */
    private fun showRemoveCandidateDialog(
        candidateWrapper: LinearLayout,
        candidates: MutableList<String>,
        index: Int,
        positionDialog: BottomSheetDialog
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_warning, null)
        val alertDialog = createStyledAlertDialog(dialogView)

        // 2.2.1 change title to Remove Candidate?
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Remove Candidate?"
        // 2.2.2 change value to This action cannot be undone.
        dialogView.findViewById<TextView>(R.id.toast_value).text = "This action cannot be undone."

        val btnPrimary = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnSecondary = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // 2.2.3 change btn_action_primary text to Cancel
        btnPrimary.text = "Cancel"
        // 2.2.4 change btn_action_secondary text to Remove
        btnSecondary.text = "Remove"

        // --- REVISED: HIDE THE COLOR STRIP ---
        dialogView.findViewById<View>(R.id.color_strip).visibility = View.GONE
        // ------------------------------------

        // 2.2.3 which when clicked closes the warning dialog
        btnPrimary.setOnClickListener {
            alertDialog.dismiss()
        }

        // 2.2.4 which removes/deletes the whole CandidateNameLayout
        btnSecondary.setOnClickListener {
            candidates.removeAt(index)
            renderCandidateChips(candidateWrapper, candidates, positionDialog)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    /**
     * Shows the warning dialog for attempting to cancel the Add/Edit Position bottom sheet.
     */
    private fun showUnsavedChangesDialog(positionDialog: BottomSheetDialog) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_warning, null)
        val alertDialog = createStyledAlertDialog(dialogView)

        // 2.6.1 change title to Details unsaved
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Details unsaved"
        // 2.6.2 change value to Are you sure you want to leave?
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Are you sure you want to leave?"

        val btnPrimary = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnSecondary = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // btn_action_primary text is as is (Leave)
        // btn_action_secondary text is as is (Stay)

        // --- REVISED: HIDE THE COLOR STRIP ---
        dialogView.findViewById<View>(R.id.color_strip).visibility = View.GONE
        // ------------------------------------

        // 2.6.3 which when clicked closes the warning dialog and the bottom_sheet_addposition
        btnPrimary.setOnClickListener {
            alertDialog.dismiss()
            positionDialog.dismiss()
        }

        // 2.6.4 which when clicked only closes the warning dialog
        btnSecondary.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    /**
     * Shows the warning dialog for removing a position.
     */
    private fun showRemovePositionDialog(positionIndex: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_warning, null)
        val alertDialog = createStyledAlertDialog(dialogView)

        // 5.1 change title to Remove Position?
        dialogView.findViewById<TextView>(R.id.toast_title).text = "Remove Position?"
        // 5.2 change value to This action cannot be undone.
        dialogView.findViewById<TextView>(R.id.toast_value).text = "This action cannot be undone."

        val btnPrimary = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnSecondary = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // 5.3 change btn_action_primary text to Cancel
        btnPrimary.text = "Cancel"
        // 5.4 change btn_action_secondary text to Remove
        btnSecondary.text = "Remove"

        // --- REVISED: HIDE THE COLOR STRIP ---
        dialogView.findViewById<View>(R.id.color_strip).visibility = View.GONE
        // ------------------------------------

        // 5.3 which when clicked closes the warning dialog
        btnPrimary.setOnClickListener {
            alertDialog.dismiss()
        }

        // 5.4 which removes/deletes the whole PositionCard
        btnSecondary.setOnClickListener {
            positionsList.removeAt(positionIndex)
            renderPositionCards()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }


    /**
     * Helper function to create the styled AlertDialog (assumes it exists in Castvote.kt logic).
     */
    private fun createStyledAlertDialog(dialogView: View): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return alertDialog
    }

    /**
     * Helper function to apply the blue outline color when an input is in focus (Requirement 2.5).
     */
    private fun setInputOutlineColorListener(textInputLayout: TextInputLayout) {
        textInputLayout.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                textInputLayout.setBoxStrokeColor(COLOR_PRIMARY_BLUE)
                textInputLayout.defaultHintTextColor = ColorStateList.valueOf(COLOR_PRIMARY_BLUE)
            } else {
                // Restore default colors
                val defaultColor = Color.parseColor("#8C8CA1")
                textInputLayout.setBoxStrokeColor(defaultColor)
                textInputLayout.defaultHintTextColor = ColorStateList.valueOf(defaultColor)
            }
        }
    }
}

// Extension function to help find the children for clearing views
private val View.children: Sequence<View>
    get() = when (this) {
        is LinearLayout -> (0 until childCount).map { getChildAt(it) }.asSequence()
        else -> emptySequence()
    }