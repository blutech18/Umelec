package com.example.umelec

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import java.util.ArrayList


// NOTE: Assuming Castvote3.kt is defined in the same package and handles the final step.

class Castvote2 : AppCompatActivity() {

    private lateinit var reviewVoteContainer: LinearLayout
    private lateinit var reviewedPositions: List<String>
    private lateinit var reviewedCandidates: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_castvote2)

        // 1. Initialize views
        reviewVoteContainer = findViewById(R.id.ReviewVoteContainer)
        val electionTitleView: TextView = findViewById(R.id.ElectionTitle)

        // 2. Set dynamic UI elements
        val currentElectionTitle = "UMak Student Council \nElections 2025" // Mock data
        electionTitleView.text = currentElectionTitle

        // 3. Get Data from Intent
        // These keys must match the ones used in Castvote.kt
        reviewedPositions = intent.getStringArrayListExtra("positions") ?: emptyList()
        reviewedCandidates = intent.getStringArrayListExtra("candidates") ?: emptyList()

        // 4. Inflate the Review List
        inflateReviewList()

        // 5. Setup Navigation and Confirmation
        setupBackNavigation() // btnBack (top-left) - Navigates directly back
        setupConfirmButton()
        setupBackToBallotButton() // btnBacktoBallot (bottom-left) - Navigates directly
        setupModernBackPressHandler() // Device back button - Navigates directly back
    }

    // ----------------------------------------------------------------------
    // --- UI INFLATION LOGIC ---
    // ----------------------------------------------------------------------

    private fun inflateReviewList() {
        val inflater = LayoutInflater.from(this)

        // CLEAR: Remove the template views inside the ReviewVoteContainer
        reviewVoteContainer.removeAllViews()

        // Loop through the data received from Castvote.kt
        for (i in reviewedPositions.indices) {
            val positionTitle = reviewedPositions[i]
            val candidateName = reviewedCandidates[i]

            // Inflate the ReviewlistContainer template (from activity_castvote2.xml)
            val reviewItemView = inflater.inflate(R.layout.review_list_item, reviewVoteContainer, false) as LinearLayout

            reviewItemView.findViewById<TextView>(R.id.ReviewPositionTitle).text = "$positionTitle:"

            val candidateTextView = reviewItemView.findViewById<TextView>(R.id.ReviewCandidateName)
            candidateTextView.text = candidateName

            // Optional: Set color for Abstain votes
            if (candidateName == "Abstain") {
                // Fallback to a common red color if R.color.error_red is not found in resources.
                try {
                    candidateTextView.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                } catch (e: Exception) {
                    candidateTextView.setTextColor(Color.parseColor("#E53935"))
                }
            } else {
                candidateTextView.setTextColor(ContextCompat.getColor(this, R.color.black))
            }

            reviewVoteContainer.addView(reviewItemView)
        }
    }


    // ----------------------------------------------------------------------
    // --- DIALOG STYLING HELPER ---
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
    // --- CONFIRMATION AND NAVIGATION LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Now navigates to Castvote3.kt (Signature/Final Submission screen).
     */
    private fun setupConfirmButton() {
        findViewById<AppCompatButton>(R.id.btnConfirm).setOnClickListener {
            // Navigate to Castvote3.kt for the final signature step
            navigateToCastvote3()
        }
    }

    /**
     * Navigates to Castvote3.kt, passing the selected vote data.
     */
    private fun navigateToCastvote3() {
        val intent = Intent(this, Castvote3::class.java).apply {
            // Pass the reviewed voting data to Castvote3 for the final step
            putStringArrayListExtra("positions", ArrayList(reviewedPositions))
            putStringArrayListExtra("candidates", ArrayList(reviewedCandidates))
        }

        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun setupBackToBallotButton() {
        findViewById<AppCompatButton>(R.id.btnBacktoBallot).setOnClickListener {
            // Navigates directly back to Castvote.kt
            navigateToCastvote()
        }
    }


    /**
     * Helper to navigate back to Castvote (the main ballot screen).
     */
    private fun navigateToCastvote() {
        val intent = Intent(this, Castvote::class.java)
        // Ensure Castvote is brought to the front if it's already running
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        finish() // Finish Castvote2 so they don't return here if Castvote is restarted
        overridePendingTransition(0, 0)
    }


    // --- STANDARD NAVIGATION HANDLERS ---

    /**
     * Navigates back to the previous activity (Castvote) without any warning.
     */
    private fun setupBackNavigation() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish() // Simply closes Castvote2, returning to Castvote
            overridePendingTransition(0, 0)
        }
    }

    /**
     * Handles the device's back button press, navigating back without any warning.
     */
    private fun setupModernBackPressHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // Simply closes Castvote2, returning to Castvote
                overridePendingTransition(0, 0)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}