package com.example.umelec

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// NOTE: Assume ElectionState, ElectionDetails, and NotificationManager are defined
// in other files (e.g., SharedData.kt and NotificationManager.kt).

// --- MAIN ACTIVITY ---

class Vote : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager
    private var currentElectionId: String? = null
    private var hasVotedInCurrentElection = false
    private var lastKnownElectionState: ElectionState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vote)

        // Initialize the NotificationManager
        notificationManager = NotificationManager(this)

        setupHeaderIcons()
        initializeElectionContext()
        setupFooterNavigation()
    }

    // --- INTEGRATED BEHAVIOR 1: HEADER ICONS (Notification & Profile) ---
    private fun setupHeaderIcons() {
        try {
            val profileIcon: ImageView? = findViewById(R.id.profileIcon)
            val notificationIcon: ImageView? = findViewById(R.id.notificationIcon)

            profileIcon?.setOnClickListener {
                val intent = Intent(this, Profile::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            }

            notificationIcon?.setOnClickListener {
                notificationManager.toggleNotificationDropdown(it as ImageView)
            }
        } catch (e: Exception) {
            android.util.Log.e("Vote", "Error setting up header icons: ${e.message}", e)
        }
    }

    // --- INTEGRATED BEHAVIOR 2: ELECTION INFO CARD ---

    private fun initializeElectionContext() {
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                currentElectionId = electionId
                if (electionId == null) {
                    hasVotedInCurrentElection = false
                } else {
                    refreshUserVoteStatus()
                }
                determineElectionState()
            },
            onFailure = { error ->
                android.util.Log.e("Vote", "Error getting election ID: $error")
                determineElectionState()
            }
        )
    }

    private fun refreshUserVoteStatus() {
        val electionId = currentElectionId ?: return
        val userId = FirebaseAuthHelper.getCurrentUser()?.uid ?: return

        FirestoreElectionHelper.hasUserVoted(
            userId = userId,
            electionId = electionId,
            onSuccess = { hasVoted ->
                hasVotedInCurrentElection = hasVoted
                lastKnownElectionState?.let { updateElectionUI(it) }
            },
            onFailure = { error ->
                android.util.Log.e("Vote", "Error checking vote status: $error")
            }
        )
    }

    private fun determineElectionState() {
        FirestoreElectionHelper.determineElectionState(
            onSuccess = { state ->
                lastKnownElectionState = state
                updateElectionUI(state)
            },
            onFailure = { error ->
                android.util.Log.e("Vote", "Error determining election state: $error")
                lastKnownElectionState = ElectionState.NO_ELECTION
                updateElectionUI(ElectionState.NO_ELECTION)
            }
        )
    }

    /**
     * Hides all election-related dynamic views and resets them.
     */
    private fun resetElectionViews(
        ongoingLayout: LinearLayout,
        noElectionText: TextView,
        upcomingLayout: LinearLayout,
        electionEndedText: TextView,
        voteNowButton: AppCompatButton
    ) {
        ongoingLayout.visibility = View.GONE
        noElectionText.visibility = View.GONE
        upcomingLayout.visibility = View.GONE
        electionEndedText.visibility = View.GONE

        // Ensure the Vote button is visible by default before state-specific logic hides it
        voteNowButton.visibility = View.VISIBLE

        // Reset enabling/alpha for clear state control in the switch block
        voteNowButton.isEnabled = false
        voteNowButton.alpha = 0.5f
    }

    /**
     * Updates the UI elements of the Election Information card based on the current state.
     */
    private fun updateElectionUI(state: ElectionState) {
        try {
            // Find all necessary views with null checks
            val ongoingLayout: LinearLayout? = findViewById(R.id.OngoingLayout)
            val upcomingLayout: LinearLayout? = findViewById(R.id.UpcomingLayout)
            val textNoElection: TextView? = findViewById(R.id.textNoElection)
            val textElectionEnded: TextView? = findViewById(R.id.textElectionEnded)
            val btnVoteNow: AppCompatButton? = findViewById(R.id.btnVoteNow)

            if (ongoingLayout == null || upcomingLayout == null || textNoElection == null || 
                textElectionEnded == null || btnVoteNow == null) {
                android.util.Log.e("Vote", "One or more views not found in layout")
                return
            }

            // Reset all views before setting the state-specific ones
            resetElectionViews(ongoingLayout, textNoElection, upcomingLayout, textElectionEnded, btnVoteNow)

            // Views for ONGOING election data
            val electionTitleValue: TextView? = findViewById(R.id.electionTitleValue)
            val votingPeriodValue: TextView? = findViewById(R.id.votingPeriodValue)
            val statusValue: TextView? = findViewById(R.id.statusValue)

            // View for UPCOMING election date data
            val upcomingDateValue: TextView? = findViewById(R.id.UpcomingDateValue)

            when (state) {
                ElectionState.ONGOING -> {
                    // PHASE 1: ONGOING (Election Info Card)
                    ongoingLayout.visibility = View.VISIBLE

                    // Fetch live data from Firestore
                    FirestoreElectionHelper.getCurrentElection(
                        onSuccess = { electionData ->
                            if (electionData != null) {
                                electionTitleValue?.text = electionData.title
                                votingPeriodValue?.text = electionData.period
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("Vote", "Error fetching election data: $error")
                        }
                    )

                    // 🚀 NEW LOGIC 1: Status text and Button enable
                    if (hasVotedInCurrentElection) {
                        statusValue?.text = "Already voted"
                        statusValue?.setTextColor(Color.parseColor("#C62828"))

                        btnVoteNow.text = "Already voted"
                        btnVoteNow.isEnabled = false
                        btnVoteNow.alpha = 0.5f
                        btnVoteNow.setOnClickListener(null)
                    } else {
                        statusValue?.text = "Eligible"
                        statusValue?.setTextColor(Color.parseColor("#333333"))

                        btnVoteNow.text = "Cast your vote"
                        btnVoteNow.isEnabled = true
                        btnVoteNow.alpha = 1.0f

                        // Add click listener for 'Cast your vote' - Navigate to Castvote (voting ballot)
                        btnVoteNow.setOnClickListener {
                            try {
                                android.util.Log.d("Vote", "Navigating to Castvote activity")
                                val intent = Intent(this, Castvote::class.java)
                                startActivity(intent)
                                @Suppress("DEPRECATION")
                                overridePendingTransition(0, 0)
                            } catch (e: Exception) {
                                android.util.Log.e("Vote", "Error navigating to Castvote: ${e.message}", e)
                                Toast.makeText(this, "Error opening voting page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                ElectionState.NO_ELECTION -> {
                    // PHASE 2: NO ELECTION
                    textNoElection.visibility = View.VISIBLE
                    btnVoteNow.text = "No Election"
                    btnVoteNow.isEnabled = false
                    btnVoteNow.alpha = 0.5f
                    btnVoteNow.setOnClickListener(null)
                }

                ElectionState.UPCOMING -> {
                    // PHASE 3: UPCOMING
                    ongoingLayout.visibility = View.VISIBLE

                    // Fetch election data from Firestore
                    FirestoreElectionHelper.getCurrentElection(
                        onSuccess = { electionData ->
                            if (electionData != null) {
                                electionTitleValue?.text = electionData.title
                                votingPeriodValue?.text = electionData.period
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("Vote", "Error fetching election data: $error")
                        }
                    )

                    statusValue?.text = "Upcoming"
                    statusValue?.setTextColor(Color.parseColor("#333333"))

                    btnVoteNow.text = "Vote Now"
                    btnVoteNow.isEnabled = false
                    btnVoteNow.alpha = 0.5f
                    btnVoteNow.setOnClickListener(null)
                }

                ElectionState.ENDED -> {
                    // PHASE 4: ENDED
                    textElectionEnded.visibility = View.VISIBLE

                    // 🚀 NEW LOGIC 3: Hide the button
                    btnVoteNow.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Vote", "Error updating election UI: ${e.message}", e)
            Toast.makeText(this, "Error loading election information", Toast.LENGTH_SHORT).show()
        }
    }


    // --- INTEGRATED BEHAVIOR 3: FOOTER NAVIGATION ---

    /**
     * Sets up click listeners for all elements in the footer navigation bar.
     */
    private fun setupFooterNavigation() {
        try {
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
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
            }

            navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
            // navVote is the current activity, no action needed on click
            navVote?.setOnClickListener { /* Already on Vote page */ }
            navCandidates?.setOnClickListener { navigateTo(Candidates::class.java) }
            navResults?.setOnClickListener { navigateTo(Results::class.java) }
            navFaq?.setOnClickListener { navigateTo(Faq::class.java) }
        } catch (e: Exception) {
            android.util.Log.e("Vote", "Error setting up footer navigation: ${e.message}", e)
        }
    }
}