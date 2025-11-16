package com.example.umelec

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import de.hdodenhof.circleimageview.CircleImageView

// Data structure for a single candidate's tally result
data class TallyCandidate(
    val name: String,
    val votes: Int,
    val photoResId: Int // Resource ID for the drawable/image (e.g., R.drawable.profile_placeholder)
)

// Enum to define the election phase
enum class ElectionPhase { ONGOING, ENDED }

class Tallies : AppCompatActivity() {

    // ----------------------------------------------------------------------
    // --- BACKEND/DATABASE INTEGRATION POINTS ---
    // ----------------------------------------------------------------------

    /** * DB/BACKEND GUIDE:
     * Fetch the current state of the election from the database or configuration.
     * Use ElectionPhase.ONGOING for "Live Tallies" and ElectionPhase.ENDED for "Final Tallies".
     */
    private val currentPhase =
        //ElectionPhase.ENDED // Current setting
        ElectionPhase.ONGOING // Uncomment this line to test the ONGOING phase behavior

    /** * DB/BACKEND GUIDE:
     * Fetch the timestamp (in milliseconds) of the latest data update from the database.
     * This is used to display the "As of [Date], [Time]" text.
     */
    private val lastUpdateTimeMillis = System.currentTimeMillis()

    /** * DB/BACKEND GUIDE:
     * This Map holds the results. It should be populated by querying your database:
     * - Key (String): The position name (e.g., "Chairperson").
     * - Value (List<TallyCandidate>): A list of all candidates for that position,
     * ALREADY SORTED by vote count (DESCENDING) from the database for efficiency.
     */
    private val talliesData = mapOf(
        "Chairperson" to listOf(
            TallyCandidate("Jane Doe", 580, R.drawable.ic_launcher_background),
            TallyCandidate("John Smith", 450, R.drawable.ic_launcher_background),
            TallyCandidate("Alex Johnson", 320, R.drawable.ic_launcher_background)
        ),
        "Vice-Chairperson" to listOf(
            TallyCandidate("Mark Tan", 710, R.drawable.ic_launcher_background),
            TallyCandidate("Sarah Lee", 600, R.drawable.ic_launcher_background)
        ),
        // ⭐️ ADDED DATA FOR SCROLL TESTING ⭐️
        "Secretary" to listOf(
            TallyCandidate("David Chan", 900, R.drawable.ic_launcher_background),
            TallyCandidate("Emily Wong", 510, R.drawable.ic_launcher_background),
            TallyCandidate("Peter King", 300, R.drawable.ic_launcher_background)
        ),
        "Treasurer" to listOf(
            TallyCandidate("Maria Dela Cruz", 1200, R.drawable.ic_launcher_background), // <-- MAX VOTES HERE
            TallyCandidate("Jose Rizal", 850, R.drawable.ic_launcher_background)
        ),
        "Auditor" to listOf(
            TallyCandidate("Kenji Sato", 770, R.drawable.ic_launcher_background),
            TallyCandidate("Lina Reyes", 760, R.drawable.ic_launcher_background),
            TallyCandidate("Mike Chen", 650, R.drawable.ic_launcher_background),
            TallyCandidate("Nancy Lim", 590, R.drawable.ic_launcher_background)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tallies)

        displayOverallVotesCount()
        setupVoteTallyBehavior()
        // setupFooterNavigation() // REMOVED
        setupHeaderBehavior()
        setupTalliesCards()
    }

    // ----------------------------------------------------------------------
    // --- VOTE TALLY CARD LOGIC (NEW) ---
    // ----------------------------------------------------------------------

    /**
     * Controls the visibility and behavior of the Download link based on the election phase.
     */
    private fun setupVoteTallyBehavior() {
        // votes_title and votes_value are always visible in both phases
        // as the parent LinearLayout (VoteTally) is not hidden.

        val downloadTitle: TextView? = findViewById(R.id.downloadTitle)

        if (currentPhase == ElectionPhase.ENDED) {
            // Requirement: SHOW downloadTitle when phase is ENDED
            downloadTitle?.visibility = View.VISIBLE

            // Requirement: Set click listener for downloadTitle
            downloadTitle?.setOnClickListener {
                // Display the placeholder Toast message
                Toast.makeText(this, "PDF downloaded", Toast.LENGTH_SHORT).show()

                // ⭐️ BACKEND/DATABASE GUIDE:
                // This is the correct location to trigger the API call to generate
                // and initiate the download of the official tallies report (e.g., PDF)
                // for the user's device.
                // Example: apiService.downloadTalliesReport()
            }
        } else {
            // Requirement: HIDE downloadTitle when phase is ONGOING
            downloadTitle?.visibility = View.GONE
            // Optional: Remove any click listener when hidden
            downloadTitle?.setOnClickListener(null)
        }
    }

    // ----------------------------------------------------------------------
    // --- VOTE COUNT LOGIC (CORRECTED) ---
    // ----------------------------------------------------------------------

    /**
     * Calculates the overall number of UNIQUE participants who voted.
     * This is determined by finding the single position that received the highest number of votes.
     * This maximum vote count represents the total number of unique ballots cast in the election.
     */
    private fun calculateOverallVotes(): Int {
        // Step 1: Flatten all candidate lists into a single list of vote counts.
        val allVoteCounts = talliesData.values.flatMap { candidates ->
            candidates.map { it.votes }
        }

        // Step 2: Find the maximum value in that list.
        return allVoteCounts.maxOrNull() ?: 0 // Finds the maximum vote count across *all* candidates, or 0 if empty
    }

    /**
     * Finds the TextView and displays the calculated overall vote count.
     */
    private fun displayOverallVotesCount() {
        val overallVotes = calculateOverallVotes()
        val votesValueTextView: TextView? = findViewById(R.id.votes_value)

        // Format the number with commas (e.g., 1,234)
        val formattedVotes = NumberFormat.getNumberInstance(Locale.US).format(overallVotes)

        votesValueTextView?.text = formattedVotes
    }

    // ----------------------------------------------------------------------
    // --- TALLIES CARD GENERATION LOGIC (FIXED) ---
    // ----------------------------------------------------------------------

    /**
     * Finds the parent container in activity_tallies.xml and generates a full card for each position.
     */
    private fun setupTalliesCards() {
        // Find the new container ID where dynamic cards should be added.
        val outerContainer: LinearLayout? = findViewById(R.id.dynamic_tallies_list)

        // Only clear the container holding the dynamic content, leaving the VoteTally card untouched.
        outerContainer?.removeAllViews()

        // 🔥 FIX: Define horizontal margin (20dp) once and convert to pixels.
        val horizontalMarginPx = 20.toPx()

        talliesData.forEach { (position, candidates) ->
            val sortedCandidates = candidates.sortedByDescending { it.votes }

            // Inflate the position card template. Use 'null' for root since we'll apply margins later.
            val positionCardView = createPositionCardView(position, sortedCandidates)

            // Apply layout parameters including the bottom and horizontal margins.
            // This is the step that makes the margins (removed from XML) work.
            positionCardView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20.toPx()
                marginStart = horizontalMarginPx // 🔥 ADDED horizontal margin
                marginEnd = horizontalMarginPx   // 🔥 ADDED horizontal margin
            }

            outerContainer?.addView(positionCardView)
        }
    }

    /**
     * Inflates the tallies_card_template, populates the position title, and then populates
     * the candidate list container using the list_item_candidate_tally template.
     */
    private fun createPositionCardView(positionTitle: String, candidates: List<TallyCandidate>): View {
        val inflater = LayoutInflater.from(this)

        // 1. Inflate the full card template
        // Note: The root parameter is null, so XML margins are ignored, but we fix that in setupTalliesCards.
        val cardView = inflater.inflate(R.layout.tallies_card_template, null) as LinearLayout

        // 2. Set the position title
        cardView.findViewById<TextView>(R.id.position_title).text = positionTitle

        // 3. Get the inner list container where rows will be added
        val listContainer = cardView.findViewById<LinearLayout>(R.id.candidate_results_list_container)

        // 4. Populate the list container with inflated candidate rows
        candidates.forEachIndexed { index, candidate ->
            val rank = index + 1
            val isWinner = rank == 1 && currentPhase == ElectionPhase.ENDED

            // a. Inflate the single candidate row XML
            val candidateRowView = inflater.inflate(R.layout.list_item_candidate_tally, listContainer, false)

            // b. Find views and set data
            candidateRowView.findViewById<TextView>(R.id.candidate_rank).apply {
                text = rank.toString()
                // Set text color dynamically based on rank
                setTextColor(Color.parseColor(if (rank == 1) "#FCBE6A" else "#4A4A68"))
            }

            // Set the profile picture and border (assuming CircleImageView is used in list_item_candidate_tally)
            val profileImageView = candidateRowView.findViewById<ImageView>(R.id.candidate_profile)
            if (profileImageView is CircleImageView) {
                profileImageView.setImageResource(candidate.photoResId)
                // Highlight winner's border if final tallies
                profileImageView.borderColor = if (isWinner) Color.parseColor("#FCBE6A") else Color.parseColor("#CCCCCC")
            } else {
                profileImageView.setImageResource(candidate.photoResId)
            }


            candidateRowView.findViewById<TextView>(R.id.candidate_name).text = candidate.name

            val votesString = String.format("%,d Votes", candidate.votes)
            candidateRowView.findViewById<TextView>(R.id.candidate_votes).text = votesString

            // c. Add the row to the list container
            listContainer.addView(candidateRowView)

            // d. Add separator if not the last item
            if (index < candidates.size - 1) {
                // We must inflate the separator as a separate View to ensure it's not part of the row's padding
                val separator = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.toPx()
                    ).apply {
                        topMargin = 12.toPx()
                        bottomMargin = 12.toPx()
                    }
                    setBackgroundColor(Color.parseColor("#DDDDDD"))
                }
                listContainer.addView(separator)
            }
        }

        return cardView
    }


    // ----------------------------------------------------------------------
    // --- EXISTING LOGIC ---
    // ----------------------------------------------------------------------

    private fun setupHeaderBehavior() {
        val btnBack: ImageButton? = findViewById(R.id.btnBack)
        val tallyTitle: TextView? = findViewById(R.id.TallyTitle)
        val talliesText: TextView? = findViewById(R.id.TalliesText)

        btnBack?.setOnClickListener {
            finish()
        }

        val formattedDateTime = formatDateTime(lastUpdateTimeMillis)

        when (currentPhase) {
            ElectionPhase.ONGOING -> {
                tallyTitle?.text = "Live Tallies"
                talliesText?.text = "Partial and unofficial results aggregated data \nAs of $formattedDateTime."
            }
            ElectionPhase.ENDED -> {
                tallyTitle?.text = "Final Tallies"
                talliesText?.text = "Below are the verified and official vote counts for each position. These tallies are final and reflect all valid votes cast during the election.\n\nAs of $formattedDateTime."
            }
        }
    }

    private fun formatDateTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}