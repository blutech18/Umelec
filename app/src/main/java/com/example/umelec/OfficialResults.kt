package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*



// Data structure for a single winner's result
data class OfficialResultCandidate(
    val name: String,
    val position: String,
    val votes: Int,
    val photoResId: Int // Resource ID for the drawable/image (e.g., R.drawable.profile_placeholder)
)

class OfficialResults : AppCompatActivity() {

    // ----------------------------------------------------------------------
    // --- BACKEND/DATABASE INTEGRATION POINTS ---
    // ----------------------------------------------------------------------

    private var officialUpdateTimeMillis = System.currentTimeMillis()
    private var winnerData = emptyList<OfficialResultCandidate>()
    private var currentElectionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_official_results)

        // Setup UI elements
        setupFooterNavigation()

        // Load winner data from Firestore
        loadWinnerData()
    }

    /**
     * Load winner data from Firestore
     */
    private fun loadWinnerData() {
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                currentElectionId = electionId
                if (electionId != null) {
                    // Get winning candidates
                    FirestoreCandidateHelper.getWinningCandidates(
                        electionId = electionId,
                        onSuccess = { winners ->
                            // Convert to OfficialResultCandidate list
                            // We need vote counts, so get tallies
                            FirestoreVoteHelper.getVoteTallies(
                                electionId = electionId,
                                onSuccess = { tallies ->
                                    // Create map of candidate votes by position
                                    val voteMap = mutableMapOf<String, Int>() // candidateName_positionName -> votes
                                    tallies.forEach { tally ->
                                        val key = "${tally.candidateName}_${tally.positionName}"
                                        voteMap[key] = tally.voteCount
                                    }

                                    // Convert winners to OfficialResultCandidate
                                    winnerData = winners.mapNotNull { winner ->
                                        val key = "${winner.name}_${winner.position}"
                                        val votes = voteMap[key] ?: 0
                                        OfficialResultCandidate(
                                            name = winner.name,
                                            position = winner.position,
                                            votes = votes,
                                            photoResId = winner.photoResource
                                        )
                                    }.sortedByDescending { it.votes }

                                    // Update timestamp
                                    officialUpdateTimeMillis = System.currentTimeMillis()

                                    // Setup UI
                                    setupHeaderBehavior()
                                    setupResultsCards()
                                },
                                onFailure = { error ->
                                    android.util.Log.e("OfficialResults", "Error loading vote counts: $error")
                                    // Use winners without vote counts
                                    winnerData = winners.map {
                                        OfficialResultCandidate(
                                            name = it.name,
                                            position = it.position,
                                            votes = 0,
                                            photoResId = it.photoResource
                                        )
                                    }
                                    officialUpdateTimeMillis = System.currentTimeMillis()
                                    setupHeaderBehavior()
                                    setupResultsCards()
                                }
                            )
                        },
                        onFailure = { error ->
                            android.util.Log.e("OfficialResults", "Error loading winners: $error")
                            winnerData = emptyList()
                            setupHeaderBehavior()
                            setupResultsCards()
                        }
                    )
                } else {
                    android.util.Log.e("OfficialResults", "No active election")
                    winnerData = emptyList()
                    setupHeaderBehavior()
                    setupResultsCards()
                }
            },
            onFailure = { error ->
                android.util.Log.e("OfficialResults", "Error getting election ID: $error")
                winnerData = emptyList()
                setupHeaderBehavior()
                setupResultsCards()
            }
        )
    }

    // ----------------------------------------------------------------------
    // --- HEADER AND STATUS TEXT BEHAVIOR ---
    // ----------------------------------------------------------------------

    private fun setupHeaderBehavior() {
        // 1. Back Button
        val btnBack: ImageButton? = findViewById(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish() // Goes back to the previous activity
        }

        // 2. Results Status Text
        val resultsText: TextView? = findViewById(R.id.ResultsText)

        // Generate the dynamic time stamp
        val formattedDateTime = formatDateTime(officialUpdateTimeMillis)

        // Set the required certified status text
        resultsText?.text = "Here are the officially declared winners and the final vote tallies per position. Results have been verified and certified by the election Adviser.\nAs of $formattedDateTime."
    }

    // ----------------------------------------------------------------------
    // --- CARD GENERATION LOGIC (FIXED) ---
    // ----------------------------------------------------------------------

    /**
     * Finds the parent container and dynamically generates a card for each winner.
     * 🔥 FIX: Sets LayoutParams with a bottom margin for each card.
     */
    private fun setupResultsCards() {
        val outerContainer: LinearLayout? = findViewById(R.id.registerContainer)
        outerContainer?.removeAllViews() // Clear any static placeholder card in the XML

        // Define the margin in DP (e.g., 20dp) and convert it to pixels
        val cardMarginBottomPx = 20.toPx()

        winnerData.forEach { winner ->
            val candidateCardView = createCandidateCardView(winner)

            // 1. Create LayoutParams for the card
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 2. Apply the bottom margin
                bottomMargin = cardMarginBottomPx
            }

            // 3. Apply the parameters to the view
            candidateCardView.layoutParams = params

            outerContainer?.addView(candidateCardView)
        }
    }

    /**
     * Inflates the list_item_winner_card template and populates it with a single winner's data.
     */
    private fun createCandidateCardView(winner: OfficialResultCandidate): View {
        val inflater = LayoutInflater.from(this)

        // Inflate the reusable XML layout file (list_item_winner_card.xml)
        // NOTE: The third argument 'false' ensures the layout parameters are not attached yet.
        val cardView = inflater.inflate(R.layout.list_item_winner_card, null, false) as LinearLayout

        // Populate data into the card views
        cardView.findViewById<CircleImageView>(R.id.candidate_profile_photo).setImageResource(winner.photoResId)
        cardView.findViewById<TextView>(R.id.candidate_name).text = winner.name
        cardView.findViewById<TextView>(R.id.candidate_position).text = winner.position

        // Format votes with thousand separator (e.g., 1,250 Votes)
        val votesString = String.format("%,d Votes", winner.votes)
        cardView.findViewById<TextView>(R.id.candidate_total_votes).text = votesString

        return cardView
    }

    // ----------------------------------------------------------------------
    // --- HELPER FUNCTIONS ---
    // ----------------------------------------------------------------------

    /**
     * Formats a time in milliseconds into a standard date and time string.
     */
    private fun formatDateTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    /**
     * 🔥 NEW: Extension function to convert DP units to screen Pixels.
     * This is required for setting margins programmatically.
     */
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION LOGIC (REMAINS THE SAME) ---
    // ----------------------------------------------------------------------

    /**
    Sets up click listeners for all elements in the footer navigation bar.*/
    private fun setupFooterNavigation() {// Find all navigation items (LinearLayouts)
        val navHome: LinearLayout? = findViewById(R.id.nav_home)
        val navVote: LinearLayout? = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout? = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout? = findViewById(R.id.nav_results)
        val navFaq: LinearLayout? = findViewById(R.id.nav_faq)

        // Helper function to navigate to a new Activity
        val navigateTo = { activityClass: Class<*> ->
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                // Use this flag for smoother tab switching
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
        }

        // Set Click Listeners (Assuming Activities exist)
        navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote?.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates?.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults?.setOnClickListener { navigateTo(Tallies::class.java) } // Assuming Results leads to Tallies/OfficialResults
        navFaq?.setOnClickListener { navigateTo(Faq::class.java) }
    }
}