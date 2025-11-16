package com.example.umelec

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

// NOTE: Assume ElectionState, ElectionDetails, and NotificationManager are defined
// in other files (e.g., SharedData.kt and NotificationManager.kt).

// --- MAIN ACTIVITY ---

class Vote : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vote)

        // Initialize the NotificationManager
        notificationManager = NotificationManager(this)

        setupHeaderIcons()
        updateElectionUI(determineElectionState())
        setupFooterNavigation()
    }

    // --- INTEGRATED BEHAVIOR 1: HEADER ICONS (Notification & Profile) ---
    private fun setupHeaderIcons() {
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val notificationIcon: ImageView = findViewById(R.id.notificationIcon)

        profileIcon.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    // --- INTEGRATED BEHAVIOR 2: ELECTION INFO CARD ---

    private fun determineElectionState(): ElectionState {
        // **IMPORTANT:** Replace this with your actual backend call logic.
        return ElectionState.ONGOING
        //return ElectionState.NO_ELECTION
        //return ElectionState.UPCOMING
        //return ElectionState.ENDED
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
        // Find all necessary views
        val ongoingLayout: LinearLayout = findViewById(R.id.OngoingLayout)
        val upcomingLayout: LinearLayout = findViewById(R.id.UpcomingLayout)
        val textNoElection: TextView = findViewById(R.id.textNoElection)
        val textElectionEnded: TextView = findViewById(R.id.textElectionEnded)
        val btnVoteNow: AppCompatButton = findViewById(R.id.btnVoteNow)

        // Reset all views before setting the state-specific ones
        resetElectionViews(ongoingLayout, textNoElection, upcomingLayout, textElectionEnded, btnVoteNow)

        // Views for ONGOING election data
        val electionTitleValue: TextView = findViewById(R.id.electionTitleValue)
        val votingPeriodValue: TextView = findViewById(R.id.votingPeriodValue)
        val statusValue: TextView = findViewById(R.id.statusValue)

        // View for UPCOMING election date data
        val upcomingDateValue: TextView = findViewById(R.id.UpcomingDateValue)

        when (state) {
            ElectionState.ONGOING -> {
                // PHASE 1: ONGOING (Election Info Card)
                ongoingLayout.visibility = View.VISIBLE

                // Fetch live data
                val electionData = fetchOngoingElectionData()
                electionTitleValue.text = electionData.title
                votingPeriodValue.text = electionData.period

                // 🚀 NEW LOGIC 1: Status text and Button enable
                statusValue.text = "Ongoing"
                statusValue.setTextColor(Color.parseColor("#333333")) // Neutral/Default color

                btnVoteNow.text = "Vote now"
                btnVoteNow.isEnabled = true
                btnVoteNow.alpha = 1.0f

                // Add click listener for 'Vote now' if it scrolls the screen or starts a process
                btnVoteNow.setOnClickListener {
                    // Implement vote initiation logic here (e.g., scroll to content, or next step)
                    val intent = Intent(this, Castvote::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }

            ElectionState.NO_ELECTION -> {
                // PHASE 2: NO ELECTION
                textNoElection.visibility = View.VISIBLE
                btnVoteNow.text = "No Election"
            }

            ElectionState.UPCOMING -> {
                // PHASE 3: UPCOMING
                // 🚀 NEW LOGIC 2: Revise visibility to use OngoingLayout structure
                ongoingLayout.visibility = View.VISIBLE

                // Fetch upcoming date data, but fill the 'OngoingLayout' fields
                val upcomingDate = fetchUpcomingElectionDate()
                val electionData = fetchOngoingElectionData()

                electionTitleValue.text = electionData.title
                votingPeriodValue.text = upcomingDate // Re-using this field for the key date

                statusValue.text = "Upcoming" // 🚀 NEW LOGIC 2: Status text
                statusValue.setTextColor(Color.parseColor("#333333")) // Neutral/Default color

                btnVoteNow.text = "Vote Now"
                btnVoteNow.isEnabled = false // 🚀 NEW LOGIC 2: Disable button
                btnVoteNow.alpha = 0.5f

                // Remove existing click listener if any
                btnVoteNow.setOnClickListener(null)
            }

            ElectionState.ENDED -> {
                // PHASE 4: ENDED
                textElectionEnded.visibility = View.VISIBLE

                // 🚀 NEW LOGIC 3: Hide the button
                btnVoteNow.visibility = View.GONE

                // The following logic is now redundant since the button is hidden,
                // but can be kept if the button is un-hidden later:
                // btnVoteNow.text = "View Results"
                // btnVoteNow.isEnabled = true
                // btnVoteNow.alpha = 1.0f
                // btnVoteNow.setOnClickListener { /* navigate to results */ }
            }
        }
    }

    // Data fetching functions (Simulated)
    private fun fetchOngoingElectionData(): ElectionDetails {
        return ElectionDetails(
            title = "UMak Student Council Elections",
            period = "March 10, 2025 1:00 PM to March 20, 2025 8:00 pm",
            status = "Active"
        )
    }

    private fun fetchUpcomingElectionDate(): String {
        return "March 10, 2025 1:00 PM to March 20, 2025 8:00 pm"
    }

    // --- INTEGRATED BEHAVIOR 3: FOOTER NAVIGATION ---

    /**
     * Sets up click listeners for all elements in the footer navigation bar.
     */
    private fun setupFooterNavigation() {
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navCandidates: LinearLayout = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout = findViewById(R.id.nav_results)
        val navFaq: LinearLayout = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            val intent = Intent(this, activityClass)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        navHome.setOnClickListener { navigateTo(Homepage::class.java) }
        // navVote is the current activity, no action needed on click
        navCandidates.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
        navFaq.setOnClickListener { navigateTo(Faq::class.java) }
    }
}