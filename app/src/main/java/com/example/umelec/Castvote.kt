package com.example.umelec

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

// --- DATA STRUCTURES (Defined outside the class for shared access with Castvote2.kt) ---

// --- MOCK DATA: Replace this with data fetched from your backend ---
private val MOCK_VOTING_DATA = listOf(
    VotingPosition("PRES", "Presidentaaaaaaaaaaaaaaaaaaaaaaaaa", listOf(
        CandidateChoices("PRES_C1", "Jane Doeaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
        CandidateChoices("PRES_C2", "John Smith"),
    )),
    VotingPosition("VPRES", "Vice President", listOf(
        CandidateChoices("VPRES_C3", "Alice Johnson"),
        CandidateChoices("VPRES_C4", "Bob Williams"),
        CandidateChoices("VPRES_C5", "Cathy Brown"),
    )),
    VotingPosition("SEC", "Secretary", listOf(
        CandidateChoices("SEC_C6", "David Lee")
    ))
)

class Castvote : AppCompatActivity() {

    private lateinit var votingContainer: LinearLayout
    private lateinit var btnSubmit: AppCompatButton
    private lateinit var allRadioGroups: List<RadioGroup>
    private var unsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_castvote)

        // 1. Initialize views
        votingContainer = findViewById(R.id.VotingContainer)
        btnSubmit = findViewById(R.id.btnSubmit)
        val electionTitleView: TextView = findViewById(R.id.ElectionTitle)

        // 2. Set dynamic UI elements
        // --- BACKEND GUIDANCE: Replace this static string with a value fetched from your database/API.
        // Example: backend.fetchCurrentElectionTitle()
        val currentElectionTitle = "UMak Student Council \nElections 2025" // Mock data
        electionTitleView.text = currentElectionTitle
        // --- END BACKEND GUIDANCE ---

        allRadioGroups = inflateVotingCards()

        setupBackNavigation()
        setupSubmitButton()
        setupChangeTracking()
        setupModernBackPressHandler()
    }

    // ----------------------------------------------------------------------
    // --- DIALOG STYLING HELPER (Unchanged) ---
    // ----------------------------------------------------------------------

    /**
     * Creates an AlertDialog with transparent background, centered gravity, and custom touch outside behavior.
     */
    private fun createStyledAlertDialog(dialogView: View, isCancellable: Boolean = true): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Apply custom styling for a cleaner look
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        // Set touch outside behavior
        dialog.setCanceledOnTouchOutside(isCancellable)

        return dialog
    }


    // ----------------------------------------------------------------------
    // --- DYNAMIC UI INFLATION LOGIC (Unchanged) ---
    // ----------------------------------------------------------------------

    private fun inflateVotingCards(): List<RadioGroup> {
        val inflater = LayoutInflater.from(this)
        val radioGroups = mutableListOf<RadioGroup>()

        votingContainer.removeAllViews()

        for (position in MOCK_VOTING_DATA) {
            val positionCardView = inflater.inflate(R.layout.position_card, votingContainer, false) as LinearLayout
            positionCardView.findViewById<TextView>(R.id.PositionTitle).text = position.title
            val radioGroup = positionCardView.findViewById<RadioGroup>(R.id.CandidateRadioGroup)
            radioGroups.add(radioGroup)

            for (i in position.candidates.indices) {
                val candidate = position.candidates[i]
                val candidateRow = inflater.inflate(R.layout.candidate_row, radioGroup, false) as LinearLayout
                val radioButton = candidateRow.findViewById<RadioButton>(R.id.CandidateRadioButton)

                candidateRow.findViewById<TextView>(R.id.CandidateName).text = candidate.name

                radioButton.id = View.generateViewId()

                // Attach listener ONLY to the RadioButton icon
                radioButton.setOnClickListener {
                    radioGroup.check(radioButton.id)
                }

                candidateRow.isClickable = false
                candidateRow.isFocusable = false

                radioGroup.addView(candidateRow)

                if (i < position.candidates.size - 1) {
                    val separator = View(this)
                    separator.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.separator_height)
                    ).apply { height = 1 }
                    separator.setBackgroundColor(Color.parseColor("#EEEEEE"))
                    radioGroup.addView(separator)
                }
            }

            // Add Abstain Row
            val abstainRow = inflater.inflate(R.layout.candidate_row, radioGroup, false) as LinearLayout
            val abstainRadioButton = abstainRow.findViewById<RadioButton>(R.id.CandidateRadioButton)
            abstainRadioButton.id = View.generateViewId()
            abstainRow.findViewById<TextView>(R.id.CandidateName).text = "Abstain"

            abstainRadioButton.setOnClickListener {
                radioGroup.check(abstainRadioButton.id)
            }

            abstainRow.isClickable = false
            abstainRow.isFocusable = false

            radioGroup.addView(abstainRow)

            votingContainer.addView(positionCardView)

            // Add the required warning layout
            val requiredRoot = inflater.inflate(R.layout.activity_castvote, null, false)
            val requiredView = requiredRoot.findViewById<LinearLayout>(R.id.Required)
            (requiredView.parent as? ViewGroup)?.removeView(requiredView)

            requiredView.findViewById<TextView>(R.id.reqText).text = "• Select or Abstain Required."
            requiredView.visibility = View.GONE
            votingContainer.addView(requiredView)
        }

        return radioGroups
    }

    // ----------------------------------------------------------------------
    // --- VALIDATION, SUBMISSION, AND DATA GATHERING LOGIC (Unchanged) ---
    // ----------------------------------------------------------------------

    /**
     * Gathers the selected candidate name for each position.
     * @return A map where the key is the Position Title and the value is the Candidate Name (or "Abstain").
     */
    private fun gatherSelections(): Map<String, String> {
        val selections = mutableMapOf<String, String>()

        allRadioGroups.forEachIndexed { index, radioGroup ->
            val position = MOCK_VOTING_DATA[index]
            val checkedId = radioGroup.checkedRadioButtonId

            val selectedCandidateName = if (checkedId != -1) {
                // Find the selected RadioButton view
                val checkedRadioButton = findViewById<RadioButton>(checkedId)

                // The RadioButton is inside a LinearLayout (candidate_row). We need the name TextView.
                val candidateRow = checkedRadioButton.parent as? LinearLayout

                // Find the TextView containing the name (CandidateName is in the layout)
                // Use the text from the TextView that is a sibling to the RadioButton
                candidateRow?.findViewById<TextView>(R.id.CandidateName)?.text.toString() ?: "Error"
            } else {
                "Not Voted" // Should not happen if validation passed
            }

            selections[position.title] = selectedCandidateName
        }
        return selections
    }

    private fun setupChangeTracking() {
        allRadioGroups.forEach { radioGroup ->
            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                unsavedChanges = true

                if (checkedId != -1) {
                    val card = group.parent as? LinearLayout ?: return@setOnCheckedChangeListener
                    val requiredLayout = (card.parent as? LinearLayout)?.getChildAt(
                        (card.parent as LinearLayout).indexOfChild(card) + 1
                    ) as? LinearLayout

                    setCardErrorState(card, false, requiredLayout)
                }
            }
        }
    }

    private fun setCardErrorState(card: LinearLayout, isError: Boolean, requiredLayout: LinearLayout?) {
        val drawableResId = if (isError) R.drawable.rounded_red_outline_bg else R.drawable.rounded_gray_bg
        card.background = ContextCompat.getDrawable(this, drawableResId)
        requiredLayout?.visibility = if (isError) View.VISIBLE else View.GONE
    }

    /**
     * Handles the submit button click. If valid, proceeds directly to Castvote2.kt (Review Screen).
     */
    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener {
            if (validateSelections()) {
                // Validation passed, gather data and navigate directly
                val selections = gatherSelections()

                val intent = Intent(this, Castvote2::class.java).apply {
                    // Convert Map keys (Positions) and values (Candidates) to String Arrays
                    putStringArrayListExtra("positions", ArrayList(selections.keys))
                    putStringArrayListExtra("candidates", ArrayList(selections.values))
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
            } else {
            }
        }
    }

    private fun validateSelections(): Boolean {
        var allValid = true
        allRadioGroups.forEach { radioGroup ->
            val card = radioGroup.parent as? LinearLayout ?: return@forEach
            val requiredLayout = (card.parent as? LinearLayout)?.getChildAt(
                (card.parent as LinearLayout).indexOfChild(card) + 1
            ) as? LinearLayout

            if (radioGroup.checkedRadioButtonId == -1) {
                setCardErrorState(card, true, requiredLayout)
                allValid = false
            } else {
                setCardErrorState(card, false, requiredLayout)
            }
        }
        return allValid
    }

    // ----------------------------------------------------------------------
    // --- NAVIGATION AND DIALOG LOGIC (Unchanged) ---
    // ----------------------------------------------------------------------

    private fun setupBackNavigation() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            handleExit() // Use handleExit to check for unsaved changes
        }
    }

    private fun setupModernBackPressHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleExit() // Use handleExit to check for unsaved changes
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * Triggers the Unsaved Changes warning dialog if votes are not submitted, otherwise proceeds to exit.
     */
    private fun handleExit() {
        if (unsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    /**
     * Displays the WARNING dialog when leaving with unsaved changes.
     */
    private fun showUnsavedChangesDialog() {
        // NOTE: Assumes R.layout.custom_toast_warning exists
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_warning, null)

        // Use the new styled helper function
        val alertDialog = createStyledAlertDialog(dialogView)

        dialogView.findViewById<TextView>(R.id.toast_title).text = "Vote Unsaved"
        dialogView.findViewById<TextView>(R.id.toast_value).text = "Are you sure you want to leave?"
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary).text = "Leave"
        dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary).text = "Stay"

        // --- REVISED: HIDE THE COLOR STRIP ---
        dialogView.findViewById<View>(R.id.color_strip).visibility = View.GONE
        // The previous line setting the background color is now removed/replaced.
        // ------------------------------------

        dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary).setOnClickListener {
            // Leave / Proceed with navigation
            unsavedChanges = false
            alertDialog.dismiss()
            finish() // Exit the current activity
        }

        dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary).setOnClickListener {
            // Stay / Cancel
            alertDialog.dismiss() // Closes the alert dialog
        }

        alertDialog.show()
    }
}