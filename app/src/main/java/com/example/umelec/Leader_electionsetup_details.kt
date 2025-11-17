package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class Leader_electionsetup_details : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_electionsetup_details)

        // Load election details from Firestore
        loadElectionDetails()
    }

    private fun loadElectionDetails() {
        // Find Views
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val tvOverallTitle: TextView = findViewById(R.id.OverallTitle)
        val tvVotingPeriodDate: TextView = findViewById(R.id.VotingPeriodDate)
        val positionContainer: LinearLayout = findViewById(R.id.PositionSectionLayout)
        val ivCheckIcon: ImageView = findViewById(R.id.ivCheckIcon)
        val tvEnableAbstain: TextView = findViewById(R.id.tvEnableAbstain)

        // Back Button Behavior
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Get current election
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                if (electionId != null) {
                    FirestoreLeaderHelper.getElectionById(
                        electionId = electionId,
                        onSuccess = { electionData ->
                            electionData?.let { data ->
                                val title = data["title"] as? String ?: "Election"
                                val isAbstainEnabled = data["isAbstainEnabled"] as? Boolean ?: false
                                
                                // Get dates
                                val startDateTimestamp = data["startDate"] as? com.google.firebase.Timestamp
                                val endDateTimestamp = data["endDate"] as? com.google.firebase.Timestamp
                                val startDate = startDateTimestamp?.toDate()
                                val endDate = endDateTimestamp?.toDate()

                                // Set title
                                tvOverallTitle.text = title

                                // Set voting period
                                if (startDate != null && endDate != null) {
                                    val startDateStr = FirestoreLeaderHelper.formatDateTime(startDate)
                                    val endDateStr = FirestoreLeaderHelper.formatDateTime(endDate)
                                    tvVotingPeriodDate.text = "Voting Period: $startDateStr to $endDateStr"
                                }

                                // Load positions and candidates
                                loadPositionsAndCandidates(electionId, positionContainer)

                                // Set abstain option
                                setupAbstainOption(ivCheckIcon, isAbstainEnabled)
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("Leader_electionsetup_details", "Error loading election: $error")
                        }
                    )
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_electionsetup_details", "Error getting election ID: $error")
            }
        )
    }

    private fun loadPositionsAndCandidates(electionId: String, container: LinearLayout) {
        FirestoreCandidateHelper.getPositionsForElection(
            electionId = electionId,
            onSuccess = { positions ->
                val positionList = positions.map { position ->
                    val candidates = position.candidates.map { Candidate(it.name) }
                    Position(position.title, candidates)
                }
                displayPositionsAndCandidates(container, positionList)
            },
            onFailure = { error ->
                android.util.Log.e("Leader_electionsetup_details", "Error loading positions: $error")
            }
        )
    }

    // Data structures for display
    data class Candidate(val name: String)
    data class Position(val name: String, val candidates: List<Candidate>)

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