package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class Leader_electionsetup_details : AppCompatActivity() {

    // ⭐️ 1. Data Structures to simulate database data ⭐️
    data class Candidate(val name: String)
    data class Position(val name: String, val candidates: List<Candidate>)
    data class ElectionSetup(
        val title: String,
        val startDate: String,
        val startTime: String,
        val endDate: String,
        val endTime: String,
        val positions: List<Position>,
        val isAbstainEnabled: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_electionsetup_details)

        // ⭐️ All setup logic is centralized here ⭐️
        setupElectionDetails()
    }

    private fun setupElectionDetails() {
        // Find Views
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val tvOverallTitle: TextView = findViewById(R.id.OverallTitle)
        val tvVotingPeriodDate: TextView = findViewById(R.id.VotingPeriodDate)
        val positionContainer: LinearLayout = findViewById(R.id.PositionSectionLayout)
        val ivCheckIcon: ImageView = findViewById(R.id.ivCheckIcon)
        val tvEnableAbstain: TextView = findViewById(R.id.tvEnableAbstain)

        // ----------------------------------------------------------------------
        // ⭐️ 2. Fake Data (Simulates Database Fetch) ⭐️
        // ----------------------------------------------------------------------
        val fakeElectionData = ElectionSetup(
            title = "Student Council Election 2025",
            startDate = "March 10, 2025",
            startTime = "1:00 PM",
            endDate = "March 20, 2025",
            endTime = "8:00 PM",
            positions = listOf(
                Position("President",
                    listOf(
                        Candidate("Anne Garcia"),
                        Candidate("Ben Torres"))),
                Position("Vice President",
                    listOf(
                        Candidate("Cathy Lim"))),
                Position("Secretary",
                    listOf(
                        Candidate("David Lee"),
                        Candidate("Elisa Reyes"),
                        Candidate("Francis Dee")))
            ),
            isAbstainEnabled = true // Change to false to test the error icon
        )
        // ----------------------------------------------------------------------

        // 1. Back Button Behavior
        btnBack.setOnClickListener {
            finish() // Goes back to the previous activity
            overridePendingTransition(0, 0)
        }

        // 2. Set Overall Title
        tvOverallTitle.text = fakeElectionData.title

        // 3. Set Voting Period Date
        // FIX: Using direct string interpolation instead of missing R.string.voting_period_format
        tvVotingPeriodDate.text =
            "Voting Period: ${fakeElectionData.startDate} ${fakeElectionData.startTime} to ${fakeElectionData.endDate} ${fakeElectionData.endTime}"

        // 4. Dynamic Position and Candidate Display
        displayPositionsAndCandidates(positionContainer, fakeElectionData.positions)

        // 5. Abstain Option Icon Logic
        setupAbstainOption(ivCheckIcon, fakeElectionData.isAbstainEnabled)
    }

    /**
     * Inflates the position cards and candidate rows dynamically.
     */
    private fun displayPositionsAndCandidates(container: LinearLayout, positions: List<Position>) {
        val inflater = layoutInflater
        container.removeAllViews() // Clear any existing static views (though we removed them)

        for (position in positions) {
            // A. Inflate the Position Card Template
            // Assumes R.layout.position_detail_template exists
            val positionView = inflater.inflate(R.layout.position_detail_template, container, false) as LinearLayout

            // Find views inside the newly inflated position card
            val tvPosition: TextView = positionView.findViewById(R.id.tvPosition)
            val llCandidatesContainer: LinearLayout = positionView.findViewById(R.id.llCandidatesContainer)

            // Set the position name
            tvPosition.text = position.name

            // B. Loop through candidates and inflate rows inside the candidate container
            for (candidate in position.candidates) {
                // Assumes R.layout.candidate_detail_row exists
                val candidateView = inflater.inflate(R.layout.candidate_detail_row, llCandidatesContainer, false) as TextView

                // Find and set the candidate name
                candidateView.text = "• ${candidate.name}"

                // Add the candidate row to the inner container
                llCandidatesContainer.addView(candidateView)
            }

            // Add the entire position card to the main container
            container.addView(positionView)
        }
    }

    /**
     * Sets the icon and updates the description based on the abstain option status.
     */
    private fun setupAbstainOption(iconView: ImageView, isEnabled: Boolean) {

        if (isEnabled) {
            // Abstain is enabled (use ic_toast_check)
            iconView.setImageResource(R.drawable.ic_toast_check)
        } else {
            // Abstain is disabled (use ic_toast_error)
            iconView.setImageResource(R.drawable.ic_toast_error)
        }
    }
}