package com.example.umelec

import android.os.Bundle
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import android.view.ViewGroup


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
// --- End MOCK DATA ---

class Leader_electionsetup_preview : AppCompatActivity() {

    private lateinit var votingContainer: LinearLayout
    private lateinit var btnPreview: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_electionsetup_preview)

        // 1. Initialize views
        votingContainer = findViewById(R.id.VotingContainer)
        btnPreview = findViewById(R.id.btnPreview)
        val electionTitleView: TextView = findViewById(R.id.ElectionTitle)

        // 2. Set dynamic UI elements
        val currentElectionTitle = "UMak Student Council \nElections 2025"
        electionTitleView.text = currentElectionTitle

        // 3. Inflate and setup the preview cards
        inflatePreviewCards()

        // 4. Setup navigation
        setupNavigation()
    }

    /**
     * Replicates the inflation logic from Castvote.kt.
     * REMOVED: Disabling the RadioButtons/RadioGroup to allow interaction.
     */
    private fun inflatePreviewCards() {
        val inflater = LayoutInflater.from(this)

        // Clear the container before inflating
        votingContainer.removeAllViews()

        for (position in MOCK_VOTING_DATA) {

            val positionCardView = inflater.inflate(R.layout.position_card, votingContainer, false) as LinearLayout

            positionCardView.findViewById<TextView>(R.id.PositionTitle).text = position.title
            val radioGroup = positionCardView.findViewById<RadioGroup>(R.id.CandidateRadioGroup)

            // ⚠️ CHANGES: Keep RadioGroup enabled and focusable for interaction
            radioGroup.isEnabled = true
            radioGroup.isFocusable = true

            // Clear the placeholder content in the radio group
            radioGroup.removeAllViews()

            for (i in position.candidates.indices) {
                val candidate = position.candidates[i]

                val candidateRow = inflater.inflate(R.layout.candidate_row, radioGroup, false) as LinearLayout
                val radioButton = candidateRow.findViewById<RadioButton>(R.id.CandidateRadioButton)

                candidateRow.findViewById<TextView>(R.id.CandidateName).text = candidate.name

                radioButton.id = View.generateViewId()

                // ⚠️ CHANGES: Keep RadioButton enabled and clickable
                radioButton.isEnabled = true
                radioButton.isClickable = true

                // Add the listener to the RadioButton icon, replicating Castvote.kt's logic
                radioButton.setOnClickListener {
                    radioGroup.check(radioButton.id)
                }

                // Keep the row itself non-interactive, relying on the RadioButton
                candidateRow.isClickable = false
                candidateRow.isFocusable = false

                radioGroup.addView(candidateRow)

                if (i < position.candidates.size - 1) {
                    val separator = View(this)
                    separator.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.separator_height) // R.dimen.separator_height must exist
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

            // ⚠️ CHANGES: Keep Abstain RadioButton enabled and clickable
            abstainRadioButton.isEnabled = true
            abstainRadioButton.isClickable = true

            // Add the listener to the RadioButton icon, replicating Castvote.kt's logic
            abstainRadioButton.setOnClickListener {
                radioGroup.check(abstainRadioButton.id)
            }

            // Keep the row itself non-interactive
            abstainRow.isClickable = false
            abstainRow.isFocusable = false

            radioGroup.addView(abstainRow)

            votingContainer.addView(positionCardView)

            // Add the required warning layout template (R.id.Required)
            val requiredRoot = inflater.inflate(R.layout.activity_leader_electionsetup_preview, null, false)
            val requiredView = requiredRoot.findViewById<LinearLayout>(R.id.Required)
            (requiredView.parent as? ViewGroup)?.removeView(requiredView)

            requiredView.findViewById<TextView>(R.id.reqText).text = "• Select or Abstain Required."
            requiredView.visibility = View.GONE // Hidden in preview

            votingContainer.addView(requiredView)
        }
    }

    /**
     * Sets up the Back and Close Preview button actions.
     */
    private fun setupNavigation() {
        // Back Button
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // When clicked, finish the current activity to return to the previous one
            overridePendingTransition(0, 0)
        }

        // Close Preview Button (btnPreview)
        btnPreview.setOnClickListener {
            finish() // Navigates back to the previous activity
            overridePendingTransition(0, 0)
        }
    }
}