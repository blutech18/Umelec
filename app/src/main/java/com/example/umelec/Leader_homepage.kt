package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

// ----------------------------------------------------------------------
// --- ELECTION PHASE LOGIC ---
// ----------------------------------------------------------------------

// 💡 RENAMED: Data class to hold election details for the ONGOING phase


// ----------------------------------------------------------------------
// --- ACTIVITY START ---
// ----------------------------------------------------------------------

class Leader_homepage : AppCompatActivity() {

    // 1. Declare the NotificationManager (Copied from Faq.kt)
    private lateinit var notificationManager: NotificationManager

    // 2. Declare views used in header
    private lateinit var nameTitle: TextView
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView

    // 💡 NEW: Declare views for the dynamic election cards
    private lateinit var collegeElectionStatusCard: LinearLayout
    private lateinit var candidatesManagementCard: LinearLayout
    private lateinit var voterManagementCard: LinearLayout
    private lateinit var electionMonitoringCard: LinearLayout
    private lateinit var resultPreviewCard: LinearLayout
    private lateinit var electionStatusValue: TextView
    private lateinit var electionStatusDateTimeValue: TextView

    private lateinit var btnViewElectionSetup: AppCompatButton
    private lateinit var btnManageCandidates: AppCompatButton
    private lateinit var btnManageVoter: AppCompatButton
    private lateinit var btnOpenDashboard: AppCompatButton
    private lateinit var btnOpenDashboardResult: AppCompatButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_homepage)

        // 3. Initialize Notification Manager (Copied from Faq.kt)
        notificationManager = NotificationManager(this)

        // 4. Set up all UI and navigation listeners
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // 5. Populate dynamic data (Name)
        populateHeaderData()

        // 💡 NEW: Update the main content UI based on the current election phase
        updateElectionUI(determineElectionState())
    }

    private fun initializeViews() {
        // Header Views
        nameTitle = findViewById(R.id.NameTitle)
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)

        // 💡 NEW: Initialize Card Views
        collegeElectionStatusCard = findViewById(R.id.CollegeElectionStatusCard)
        candidatesManagementCard = findViewById(R.id.CandidatesManagementCard)
        voterManagementCard = findViewById(R.id.VoterManagementCard)
        electionMonitoringCard = findViewById(R.id.ElectionMonitoringCard)
        resultPreviewCard = findViewById(R.id.ResultPreviewCard)

        // 💡 NEW: Initialize Status and Button Views
        electionStatusValue = findViewById(R.id.ElectionStatusValue)
        electionStatusDateTimeValue = findViewById(R.id.ElectionStatusDateTimeValue)
        btnViewElectionSetup = findViewById(R.id.btnViewElectionSetup)
        btnManageCandidates = findViewById(R.id.btnManageCandidates)
        btnManageVoter = findViewById(R.id.btnManageVoter)
        btnOpenDashboard = findViewById(R.id.btnOpenDashboard)
        btnOpenDashboardResult = findViewById(R.id.btnOpenDashboardResult)
    }

    private fun populateHeaderData() {
        // ⚠️ BACKEND GUIDE: Replace this static data fetch with a call to your data layer
        val leaderName = "Juan"
        nameTitle.text = leaderName
    }

    /**
     * Initializes header elements: Profile Icon and Notification Icon.
     * This setup is modeled directly after the logic in Faq.kt's setupUI().
     */
    private fun setupUIListeners() {
        // --- Profile Icon Click Listener (Navigates to Leader_profile) ---
        profileIcon.setOnClickListener {
            val intent = Intent(this, Leader_profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // --- Notification Icon Click Listener (Delegates to NotificationManager, copied from Faq.kt) ---
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    // ----------------------------------------------------------------------
    // --- ELECTION PHASE HANDLERS ---
    // ----------------------------------------------------------------------

    /**
     * ⚠️ BACKEND INTEGRATION POINT:
     * This function should be replaced with a real API call to determine the current
     * election phase (ONGOING, UPCOMING, NO_ELECTION, ENDED) for the leader's college.
     * For demonstration, it is currently hardcoded.
     */
    private fun determineElectionState(): ElectionState {
        return ElectionState.ONGOING // Change this value to test different phases
    }

    /**
     * ⚠️ BACKEND INTEGRATION POINT:
     * This function should fetch the actual end date and time from the database
     * when the election is ONGOING.
     * * The return type uses the renamed data class.
     */
    private fun fetchOngoingElectionData(): ElectionDateandTimeDetails {
        return ElectionDateandTimeDetails(
            endDate = "October 15, 2025",
            endTime = "5:00 PM"
        )
    }

    /**
     * Updates the visibility, text, and actions of all election-related cards
     * based on the current ElectionState.
     */
    private fun updateElectionUI(state: ElectionState) {
        // Hide all action cards by default
        candidatesManagementCard.visibility = View.GONE
        voterManagementCard.visibility = View.GONE
        electionMonitoringCard.visibility = View.GONE
        resultPreviewCard.visibility = View.GONE

        // The status card is always visible in all phases
        collegeElectionStatusCard.visibility = View.VISIBLE

        when (state) {
            ElectionState.NO_ELECTION -> {
                // Phase 1: No Election
                electionStatusDateTimeValue.visibility = View.GONE
                electionStatusValue.text = "No election"
                btnViewElectionSetup.text = "Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup::class.java))
                    overridePendingTransition(0, 0)
                }
            }
            ElectionState.UPCOMING -> {
                // Phase 2: Upcoming
                electionStatusDateTimeValue.visibility = View.GONE
                voterManagementCard.visibility = View.VISIBLE
                candidatesManagementCard.visibility = View.VISIBLE

                electionStatusValue.text = "Upcoming"
                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    overridePendingTransition(0, 0)
                }
                btnManageCandidates.setOnClickListener {
                    startActivity(Intent(this, Leader_manage_candidates::class.java))
                    overridePendingTransition(0, 0)
                }
                btnManageVoter.setOnClickListener {
                    startActivity(Intent(this, Leader_manage_voters::class.java))
                    overridePendingTransition(0, 0)
                }
            }
            ElectionState.ONGOING -> {
                // Phase 3: Ongoing
                voterManagementCard.visibility = View.VISIBLE
                electionMonitoringCard.visibility = View.VISIBLE

                // 💡 UPDATED: Variable declaration uses the renamed data class
                val details = fetchOngoingElectionData()
                electionStatusValue.text = "Ongoing"
                electionStatusDateTimeValue.text = "(Ends: ${details.endDate} at ${details.endTime})"


                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    overridePendingTransition(0, 0)
                }
                btnOpenDashboard.setOnClickListener {
                    startActivity(Intent(this, Leader_monitor::class.java))
                    overridePendingTransition(0, 0)
                }
                btnManageVoter.setOnClickListener {
                    startActivity(Intent(this, Leader_monitor::class.java))
                    overridePendingTransition(0, 0)
                }
            }
            ElectionState.ENDED -> {
                // Phase 4: Ended
                resultPreviewCard.visibility = View.VISIBLE
                electionStatusDateTimeValue.visibility = View.GONE

                electionStatusValue.text = "Ended"
                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    overridePendingTransition(0, 0)
                }
                btnOpenDashboardResult.setOnClickListener {
                    startActivity(Intent(this, AutomatedReports::class.java))
                    overridePendingTransition(0, 0)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION ---
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

        // --- FOOTER NAVIGATION LOGIC ---
        navHome.setOnClickListener { /* Do nothing, already here */ }
        navSetup.setOnClickListener { navigateTo(Leader_Setup::class.java) }
        navManage.setOnClickListener { navigateTo(Leader_manage_voters::class.java) }
        navMonitor.setOnClickListener { navigateTo(Leader_monitor::class.java) }
        navFaq.setOnClickListener { navigateTo(Leader_faqs::class.java) }
    }
}