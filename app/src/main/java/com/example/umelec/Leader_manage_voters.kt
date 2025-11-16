package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

// Assuming NotificationManager is defined elsewhere in the com.example.umelec package
// class NotificationManager(private val context: AppCompatActivity) { ... }

class Leader_manage_voters : AppCompatActivity() {

    // 1. Declare the NotificationManager (from Leader_faqs.kt / Leader_homepage.kt)
    private lateinit var notificationManager: NotificationManager

    // 2. Declare views used in header (from Leader_faqs.kt / Leader_homepage.kt)
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView

    // --- Data structure for the bar chart (from AutomatedReports.kt) ---
    data class YearVoteData(val yearLabel: String, val voteCount: Int, val barItemViewId: Int)

    // ----------------------------------------------------------------------
    // ⭐️ BACKEND/DATABASE INTEGRATION POINT: CORE STATS ⭐️
    // These values should be fetched from the database
    // ----------------------------------------------------------------------
    private val totalEligibleVoters = 340 // Total number of students who can vote
    private val totalVoted = 34 // Total number of students who have voted (New explicit variable)

    // ----------------------------------------------------------------------

    // --- Sample Data for Bar Chart (Replace with data fetched from DB) ---
    // The sum of 'voteCount' below MUST equal 'totalVoted' above for the bar chart to render correctly.
    private val yearVoteDistribution = listOf(
        YearVoteData("1st", 15, R.id.barItem1st),
        YearVoteData("2nd", 8, R.id.barItem2nd),
        YearVoteData("3rd", 7, R.id.barItem3rd),
        YearVoteData("4th", 4, R.id.barItem4th)
    )
    // ----------------------------------------------------------------------

    private val notVotedCount = totalEligibleVoters - totalVoted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_manage_voters)

        // 3. Initialize Notification Manager (from Leader_faqs.kt / Leader_homepage.kt)
        // Assumes NotificationManager class is available.
        notificationManager = NotificationManager(this)

        // 4. Set up all UI and navigation listeners
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // --- NEW BEHAVIOURS ---
        setupVoterTurnoutMetrics()
        setupVotedStudentsCard()
        setupButtonNavigation()
    }

    // --- VIEW INITIALIZATION (Copied from Leader_faqs.kt) ---
    private fun initializeViews() {
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)
    }

    /**
     * Initializes header elements: Profile Icon and Notification Icon (Copied from Leader_faqs.kt).
     */
    private fun setupUIListeners() {
        // --- Profile Icon Click Listener (Navigates to Leader_profile) ---
        profileIcon.setOnClickListener {
            val intent = Intent(this, Leader_profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // --- Notification Icon Click Listener (Delegates to NotificationManager) ---
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    // ----------------------------------------------------------------------
    // --- VOTER TURNOUT METRICS LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Calculates and displays Total Eligible Voters and the Voted/Not Voted percentages.
     */
    private fun setupVoterTurnoutMetrics() {
        val tvTotalEligibleVotersValue: TextView = findViewById(R.id.TotalEligibleVotersValue)
        val tvPercentVoted: TextView = findViewById(R.id.tvPercentVoted)
        val tvPercentNotVoted: TextView = findViewById(R.id.tvPercentNotVoted)

        // 1. Set Total Eligible Voters (uses the new explicit variable)
        tvTotalEligibleVotersValue.text = totalEligibleVoters.toString()

        // 2. Calculate percentages using the calculateTurnout function
        val votedPercentage = calculateTurnout(totalVoted, totalEligibleVoters)
        val notVotedPercentage = 100 - votedPercentage

        // 3. Display percentages
        tvPercentVoted.text = "$votedPercentage%"
        tvPercentNotVoted.text = "$notVotedPercentage%"
    }

    /**
     * Calculates turnout percentage (Copied from AutomatedReports.kt).
     */
    private fun calculateTurnout(votedCount: Int, eligibleCount: Int): Int {
        return if (eligibleCount > 0) {
            ((votedCount.toFloat() / eligibleCount.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    // ----------------------------------------------------------------------
    // --- VOTED STUDENTS CARD LOGIC (Imitating AutomatedReports.kt Bar Chart) ---
    // ----------------------------------------------------------------------

    /**
     * Implements the bar chart logic to display voted students by year (Imitating setupYearBarChart from AutomatedReports.kt).
     */
    private fun setupVotedStudentsCard() {
        val barAreaContainer: LinearLayout = findViewById(R.id.BarArea)
        val tvTotalVotedCount: TextView = findViewById(R.id.tvTotalVotedCount)

        // totalVotes now uses the new explicit variable
        val totalVotes = totalVoted

        tvTotalVotedCount.text = totalVotes.toString()

        if (totalVotes == 0) return

        // Wait until the container has been laid out to get its width
        barAreaContainer.post {
            val containerWidth = barAreaContainer.width

            // Uses the separate yearVoteDistribution list
            yearVoteDistribution.forEach { data ->
                val barItemView = findViewById<View>(data.barItemViewId)
                val tvBarLabel: TextView = barItemView.findViewById(R.id.tvBarLabel)
                val progressBar: View = barItemView.findViewById(R.id.vBarProgress)
                val tvBarValue: TextView = barItemView.findViewById(R.id.tvBarValue)

                tvBarLabel.text = data.yearLabel
                tvBarValue.text = data.voteCount.toString()

                val votePercentage = data.voteCount.toFloat() / totalVotes.toFloat()

                // Calculate the target width based on the container width and vote percentage
                val targetWidth = (containerWidth * votePercentage).toInt()

                // Apply the new width to the progress bar view
                val params: ViewGroup.LayoutParams = progressBar.layoutParams
                params.width = targetWidth
                progressBar.layoutParams = params
            }
        }
    }

    // ----------------------------------------------------------------------
    // --- BUTTON NAVIGATION LOGIC ---
    // ----------------------------------------------------------------------
    private fun setupButtonNavigation() {
        val btnViewList: AppCompatButton = findViewById(R.id.btnViewList)
        val btnCandidates: AppCompatButton = findViewById(R.id.btnCandidates)

        // 1. Navigate to Leader_manage_voters_list
        btnViewList.setOnClickListener {
            val intent = Intent(this, Leader_manage_voters_list::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 2. Navigate to Leader_manage_candidates
        btnCandidates.setOnClickListener {
            val intent = Intent(this, Leader_manage_candidates::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION (Copied from Leader_faqs.kt) ---
    // ----------------------------------------------------------------------
    private fun setupFooterNavigation() {
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navSetup: LinearLayout = findViewById(R.id.nav_setup)
        val navManage: LinearLayout = findViewById(R.id.nav_manage)
        val navMonitor: LinearLayout = findViewById(R.id.nav_monitor)
        val navFaq: LinearLayout = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        navHome.setOnClickListener { navigateTo(Leader_homepage::class.java) }
        navSetup.setOnClickListener { navigateTo(Leader_Setup::class.java) }
        navManage.setOnClickListener { /* Do nothing, already here */ }
        navMonitor.setOnClickListener { navigateTo(Leader_monitor::class.java) }
        navFaq.setOnClickListener { navigateTo(Leader_faqs::class.java) }
    }
}