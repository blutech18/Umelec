package com.example.umelec

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast // 🔥 NEW: Import for Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import de.hdodenhof.circleimageview.CircleImageView

// Data structure for a single candidate's tally result
data class TallyCandidate(
    val name: String,
    val votes: Int,
    val photoUrl: String
)

// Enum to define the election phase
enum class ElectionPhase { ONGOING, ENDED }

class Tallies : AppCompatActivity() {

    companion object {
        const val EXTRA_FORCE_FINAL_TALLIES = "extra_force_final_tallies"
        private const val DEFAULT_AVATAR_URL = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"
    }

    // ----------------------------------------------------------------------
    // --- BACKEND/DATABASE INTEGRATION POINTS ---
    // ----------------------------------------------------------------------

    private var currentPhase = ElectionPhase.ONGOING
    private var lastUpdateTimeMillis = System.currentTimeMillis()
    private var talliesData = emptyMap<String, List<TallyCandidate>>()
    private var currentElectionId: String? = null
    private var forceFinalTallies: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tallies)

        forceFinalTallies = intent.getBooleanExtra(EXTRA_FORCE_FINAL_TALLIES, false)

        setupFooterNavigation()
        
        // Load data from Firestore
        loadTalliesData()
    }

    /**
     * Load tallies data from Firestore
     */
    private fun loadTalliesData() {
        // Determine election phase
        FirestoreElectionHelper.determineElectionState(
            onSuccess = { state ->
                currentPhase = when (state) {
                    ElectionState.ONGOING -> ElectionPhase.ONGOING
                    ElectionState.ENDED -> ElectionPhase.ENDED
                    else -> ElectionPhase.ONGOING
                }

                if (forceFinalTallies) {
                    currentPhase = ElectionPhase.ENDED
                }

                // Get election ID
                FirestoreElectionHelper.getCurrentElectionId(
                    onSuccess = { electionId ->
                        currentElectionId = electionId
                        if (electionId != null) {
                            // Get vote tallies
                            FirestoreVoteHelper.getVoteTallies(
                                electionId = electionId,
                                onSuccess = { tallies ->
                                    // Group tallies by position and convert to TallyCandidate
                                    val talliesMap = mutableMapOf<String, MutableList<TallyCandidate>>()
                                    
                                    tallies.forEach { tally ->
                                        val positionName = tally.positionName
                                        if (!talliesMap.containsKey(positionName)) {
                                            talliesMap[positionName] = mutableListOf()
                                        }
                                        talliesMap[positionName]?.add(
                                            TallyCandidate(
                                                name = tally.candidateName,
                                                votes = tally.voteCount,
                                                photoUrl = DEFAULT_AVATAR_URL
                                            )
                                        )
                                    }

                                    // Sort candidates by vote count (descending) per position
                                    talliesData = talliesMap.mapValues { (_, candidates) ->
                                        candidates.sortedByDescending { it.votes }
                                    }

                                    // Update last update time
                                    lastUpdateTimeMillis = System.currentTimeMillis()

                                    // Setup UI
                                    displayOverallVotesCount()
                                    setupVoteTallyBehavior()
                                    setupHeaderBehavior()
                                    setupTalliesCards()
                                },
                                onFailure = { error ->
                                    android.util.Log.e("Tallies", "Error loading tallies: $error")
                                    talliesData = emptyMap()
                                    displayOverallVotesCount()
                                    setupVoteTallyBehavior()
                                    setupHeaderBehavior()
                                    setupTalliesCards()
                                }
                            )
                        } else {
                            android.util.Log.e("Tallies", "No active election")
                            talliesData = emptyMap()
                            displayOverallVotesCount()
                            setupVoteTallyBehavior()
                            setupHeaderBehavior()
                            setupTalliesCards()
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("Tallies", "Error getting election ID: $error")
                        talliesData = emptyMap()
                        displayOverallVotesCount()
                        setupVoteTallyBehavior()
                        setupHeaderBehavior()
                        setupTalliesCards()
                    }
                )
            },
            onFailure = { error ->
                android.util.Log.e("Tallies", "Error determining election state: $error")
                talliesData = emptyMap()
                displayOverallVotesCount()
                setupVoteTallyBehavior()
                setupHeaderBehavior()
                setupTalliesCards()
            }
        )
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

        if (talliesData.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No tallies are available yet. Please check back later."
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                setPadding(24.toPx(), 32.toPx(), 24.toPx(), 32.toPx())
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            outerContainer?.addView(emptyView)
            return
        }

        talliesData.forEach { (position, candidates) ->
            val sortedCandidates = candidates.sortedByDescending { it.votes }

            // Assuming R.layout.tallies_card_template is the XML layout for one position card
            val positionCardView = createPositionCardView(position, sortedCandidates)

            // Apply margin at the bottom of each dynamically created card
            positionCardView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20.toPx()
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
            val profileImageView = candidateRowView.findViewById<CircleImageView>(R.id.candidate_profile)
            ImageLoaderHelper.loadCandidateImage(
                imageView = profileImageView,
                photoUrl = candidate.photoUrl,
                defaultResource = R.drawable.ic_profile
            )
            profileImageView.borderColor = if (isWinner) Color.parseColor("#FCBE6A") else Color.parseColor("#CCCCCC")


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

    private fun setupFooterNavigation() {
        val navHome: LinearLayout? = findViewById(R.id.nav_home)
        val navVote: LinearLayout? = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout? = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout? = findViewById(R.id.nav_results)
        val navFaq: LinearLayout? = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
        }

        navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote?.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates?.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults?.setOnClickListener { navigateTo(Results::class.java) }
        navFaq?.setOnClickListener { navigateTo(Faq::class.java) }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}