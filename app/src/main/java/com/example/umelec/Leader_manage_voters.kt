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

    // Voter statistics
    private var totalEligibleVoters = 0
    private var totalVoted = 0
    private var currentElectionId: String? = null

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
        loadVoterStatistics()
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
            @Suppress("DEPRECATION")
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
     * Load voter statistics from Firestore
     */
    private fun loadVoterStatistics() {
        android.util.Log.d("Leader_manage_voters", "Loading voter statistics...")
        // Get current election ID
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                android.util.Log.d("Leader_manage_voters", "Current election ID: $electionId")
                currentElectionId = electionId
                if (electionId != null) {
                    // Load total eligible voters
                    FirestoreVoterHelper.getTotalEligibleVoters(
                        onSuccess = { eligibleCount ->
                            android.util.Log.d("Leader_manage_voters", "Total eligible voters: $eligibleCount")
                            totalEligibleVoters = eligibleCount
                            // Load total voted
                            FirestoreVoterHelper.getTotalVoted(
                                electionId = electionId,
                                onSuccess = { votedCount ->
                                    android.util.Log.d("Leader_manage_voters", "Total voted: $votedCount")
                                    totalVoted = votedCount
                                    // Load year distribution
                                    loadYearDistribution(electionId)
                                    // Update UI
                                    setupVoterTurnoutMetrics()
                                },
                                onFailure = { error ->
                                    android.util.Log.e("Leader_manage_voters", "Error getting voted count: $error")
                                    totalVoted = 0
                                    setupVoterTurnoutMetrics()
                                }
                            )
                        },
                        onFailure = { error ->
                            android.util.Log.e("Leader_manage_voters", "Error getting eligible voters: $error")
                            totalEligibleVoters = 0
                            setupVoterTurnoutMetrics()
                        }
                    )
                } else {
                    // No active election
                    android.util.Log.w("Leader_manage_voters", "No active election found")
                    totalEligibleVoters = 0
                    totalVoted = 0
                    setupVoterTurnoutMetrics()
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_manage_voters", "Error getting election ID: $error")
                totalEligibleVoters = 0
                totalVoted = 0
                setupVoterTurnoutMetrics()
            }
        )
    }

    /**
     * Load year distribution from Firestore
     */
    private fun loadYearDistribution(electionId: String) {
        android.util.Log.d("Leader_manage_voters", "Loading year distribution for election: $electionId")
        FirestoreVoterHelper.getVoterStatisticsByYear(
            electionId = electionId,
            onSuccess = { yearCounts ->
                android.util.Log.d("Leader_manage_voters", "Year distribution loaded: $yearCounts")
                setupVotedStudentsCard(yearCounts)
            },
            onFailure = { error ->
                android.util.Log.e("Leader_manage_voters", "Error getting year distribution: $error")
                setupVotedStudentsCard(emptyMap())
            }
        )
    }

    /**
     * Calculates and displays Total Eligible Voters and the Voted/Not Voted percentages.
     */
    private fun setupVoterTurnoutMetrics() {
        val tvTotalEligibleVotersValue: TextView = findViewById(R.id.TotalEligibleVotersValue)
        val tvPercentVoted: TextView = findViewById(R.id.tvPercentVoted)
        val tvPercentNotVoted: TextView = findViewById(R.id.tvPercentNotVoted)

        android.util.Log.d("Leader_manage_voters", "Setting up voter turnout metrics: eligible=$totalEligibleVoters, voted=$totalVoted")

        // 1. Set Total Eligible Voters
        tvTotalEligibleVotersValue.text = totalEligibleVoters.toString()

        // 2. Calculate percentages using the calculateTurnout function
        val votedPercentage = calculateTurnout(totalVoted, totalEligibleVoters)
        val notVotedPercentage = 100 - votedPercentage

        android.util.Log.d("Leader_manage_voters", "Calculated percentages: voted=$votedPercentage%, notVoted=$notVotedPercentage%")

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
     * Implements the bar chart logic to display voted students by year
     */
    private fun setupVotedStudentsCard(yearCounts: Map<String, Int>) {
        val barAreaContainer: LinearLayout = findViewById(R.id.BarArea)
        val tvTotalVotedCount: TextView = findViewById(R.id.tvTotalVotedCount)

        android.util.Log.d("Leader_manage_voters", "Setting up voted students card: totalVoted=$totalVoted, yearCounts=$yearCounts")

        tvTotalVotedCount.text = totalVoted.toString()

        if (totalVoted == 0) {
            android.util.Log.w("Leader_manage_voters", "No votes found, skipping year distribution")
            return
        }

        // Map year labels to view IDs
        val yearViewMap = mapOf(
            "1st" to R.id.barItem1st,
            "2nd" to R.id.barItem2nd,
            "3rd" to R.id.barItem3rd,
            "4th" to R.id.barItem4th
        )

        // Wait until the container has been laid out to get its width
        barAreaContainer.post {
            val containerWidth = barAreaContainer.width

            yearViewMap.forEach { (yearLabel, viewId) ->
                val voteCount = yearCounts[yearLabel] ?: 0
                android.util.Log.d("Leader_manage_voters", "Year $yearLabel: $voteCount votes")
                
                val barItemView = findViewById<View>(viewId)
                val tvBarLabel: TextView = barItemView.findViewById(R.id.tvBarLabel)
                val progressBar: View = barItemView.findViewById(R.id.vBarProgress)
                val tvBarValue: TextView = barItemView.findViewById(R.id.tvBarValue)

                tvBarLabel.text = yearLabel
                tvBarValue.text = voteCount.toString()

                val votePercentage = if (totalVoted > 0) {
                    voteCount.toFloat() / totalVoted.toFloat()
                } else {
                    0f
                }

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
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // 2. Navigate to Leader_manage_candidates
        btnCandidates.setOnClickListener {
            val intent = Intent(this, Leader_manage_candidates::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
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
                @Suppress("DEPRECATION")
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