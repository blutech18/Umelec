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

class Castvote : AppCompatActivity() {

    private lateinit var votingContainer: LinearLayout
    private lateinit var btnSubmit: AppCompatButton
    private var allRadioGroups: List<RadioGroup> = emptyList() // Initialize to empty list to avoid lateinit error
    private var unsavedChanges = false
    private var votingPositions: List<VotingPosition> = emptyList()
    private var currentElectionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_castvote)

        try {
            // 1. Initialize views with null checks
            votingContainer = findViewById(R.id.VotingContainer) ?: run {
                android.util.Log.e("Castvote", "VotingContainer not found")
                Toast.makeText(this, "Error loading voting page", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            btnSubmit = findViewById(R.id.btnSubmit) ?: run {
                android.util.Log.e("Castvote", "btnSubmit not found")
                Toast.makeText(this, "Error loading voting page", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            val electionTitleView: TextView? = findViewById(R.id.ElectionTitle)

            // 2. Get current election ID and data
            FirestoreElectionHelper.getCurrentElectionId(
                onSuccess = { electionId ->
                    currentElectionId = electionId
                    if (electionId != null) {
                        // Fetch election title
                        FirestoreElectionHelper.getCurrentElection(
                            onSuccess = { electionData ->
                                electionTitleView?.text = electionData?.title ?: "Election"
                                // Fetch positions and candidates
                                loadVotingData(electionId)
                            },
                            onFailure = { error ->
                                android.util.Log.e("Castvote", "Error fetching election: $error")
                                electionTitleView?.text = "Election"
                                loadVotingData(electionId)
                            }
                        )
                    } else {
                        android.util.Log.e("Castvote", "No active election found")
                        Toast.makeText(this, "No active election available", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("Castvote", "Error getting election ID: $error")
                    Toast.makeText(this, "Error loading election data", Toast.LENGTH_SHORT).show()
                    finish()
                }
            )

            setupBackNavigation()
            setupSubmitButton()
            // setupChangeTracking() will be called after allRadioGroups is initialized in loadVotingData()
            setupModernBackPressHandler()
        } catch (e: Exception) {
            android.util.Log.e("Castvote", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing voting page", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadVotingData(electionId: String) {
        android.util.Log.d("Castvote", "Loading voting data for election: $electionId")
        FirestoreCandidateHelper.getPositionsForElection(
            electionId = electionId,
            onSuccess = { positions ->
                android.util.Log.d("Castvote", "Loaded ${positions.size} positions")
                if (positions.isEmpty()) {
                    android.util.Log.w("Castvote", "No positions found for election")
                    Toast.makeText(this, "No positions available for this election", Toast.LENGTH_SHORT).show()
                    finish()
                    return@getPositionsForElection
                }
                votingPositions = positions
                try {
                    allRadioGroups = inflateVotingCards()
                    // Now that allRadioGroups is initialized, set up change tracking
                    setupChangeTracking()
                } catch (e: Exception) {
                    android.util.Log.e("Castvote", "Error inflating voting cards: ${e.message}", e)
                    Toast.makeText(this, "Error loading voting interface", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onFailure = { error ->
                android.util.Log.e("Castvote", "Error loading voting data: $error")
                Toast.makeText(this, "Failed to load voting data: $error", Toast.LENGTH_LONG).show()
                finish()
            }
        )
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

        for (position in votingPositions) {
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
                // Store candidate and position data as tags for retrieval later
                radioButton.tag = mapOf(
                    "candidateId" to candidate.id,
                    "candidateName" to candidate.name,
                    "positionId" to position.id,
                    "positionName" to position.title
                )

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
            // Store abstain data as tag
            abstainRadioButton.tag = mapOf(
                "candidateId" to "ABSTAIN",
                "candidateName" to "Abstain",
                "positionId" to position.id,
                "positionName" to position.title
            )

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
     * Gathers the selected candidate data for each position.
     * @return A map where the key is positionId and the value is candidate data map.
     */
    private fun gatherSelections(): Map<String, Map<String, String>> {
        val selections = mutableMapOf<String, Map<String, String>>()

        allRadioGroups.forEachIndexed { index, radioGroup ->
            if (index >= votingPositions.size) return@forEachIndexed
            val checkedId = radioGroup.checkedRadioButtonId

            if (checkedId != -1) {
                val checkedRadioButton = findViewById<RadioButton>(checkedId)
                @Suppress("UNCHECKED_CAST")
                val tagData = checkedRadioButton.tag as? Map<String, String>
                
                tagData?.let { data ->
                    val positionId = data["positionId"] ?: return@let
                    selections[positionId] = data
                }
            }
        }
        return selections
    }
    
    /**
     * Gathers selections as simple name map for display in Castvote2
     */
    private fun gatherSelectionsForDisplay(): Map<String, String> {
        val selections = mutableMapOf<String, String>()
        gatherSelections().forEach { (_, data) ->
            val positionName = data["positionName"] ?: ""
            val candidateName = data["candidateName"] ?: ""
            selections[positionName] = candidateName
        }
        return selections
    }

    private fun setupChangeTracking() {
        // Only set up change tracking if we have radio groups
        if (allRadioGroups.isEmpty()) {
            android.util.Log.w("Castvote", "setupChangeTracking called but allRadioGroups is empty")
            return
        }
        
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
                val selectionsData = gatherSelections() // Full data with IDs
                val selectionsDisplay = gatherSelectionsForDisplay() // Simple name map for display

                val intent = Intent(this, Castvote2::class.java).apply {
                    // Convert Map keys (Positions) and values (Candidates) to String Arrays for display
                    putStringArrayListExtra("positions", ArrayList(selectionsDisplay.keys))
                    putStringArrayListExtra("candidates", ArrayList(selectionsDisplay.values))
                    // Store full selection data as serializable
                    putExtra("selectionsData", android.os.Bundle().apply {
                        selectionsData.forEach { (positionId, data) ->
                            putString("pos_$positionId", android.util.Base64.encodeToString(
                                org.json.JSONObject(data as Map<*, *>).toString().toByteArray(),
                                android.util.Base64.NO_WRAP
                            ))
                        }
                    })
                    currentElectionId?.let { putExtra("electionId", it) }
                }
                startActivity(intent)
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