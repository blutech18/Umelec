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
// --- ELECTION PHASE LOGIC (IMITATED) ---
// ----------------------------------------------------------------------



// ----------------------------------------------------------------------
// --- ACTIVITY START ---
// ----------------------------------------------------------------------

class Leader_Setup : AppCompatActivity() {

    // 1. Declare the NotificationManager (Copied from Leader_homepage.kt)
    // NOTE: NotificationManager class must be defined elsewhere in your project (e.g., Faq.kt or its own file)
    private lateinit var notificationManager: NotificationManager

    // 2. Declare views used in header and CollegeElectionStatusCard (Copied from Leader_homepage.kt)
    private lateinit var nameTitle: TextView
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView

    private lateinit var collegeElectionStatusCard: LinearLayout
    private lateinit var electionStatusValue: TextView
    private lateinit var electionStatusDateTimeValue: TextView

    private lateinit var btnViewElectionSetup: AppCompatButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_setup) // Assumes this layout contains the needed IDs

        // 3. Initialize Notification Manager (Copied from Leader_homepage.kt)
        notificationManager = NotificationManager(this)

        // 4. Set up all UI and navigation listeners (Copied from Leader_homepage.kt)
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // 5. Populate dynamic data (Name) (Copied from Leader_homepage.kt)
        populateHeaderData()

        // 💡 NEW: Update the main content UI based on the current election phase (Copied from Leader_homepage.kt)
        // This is key to showing the CollegeElectionStatusCard logic
        updateElectionUI(determineElectionState())
    }

    private fun initializeViews() {
        // Header Views
        nameTitle = findViewById(R.id.NameTitle)
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)

        // 💡 Initialize Card Views (Used for status and to be hidden)
        collegeElectionStatusCard = findViewById(R.id.CollegeElectionStatusCard)

        // 💡 Initialize Status and Button Views
        electionStatusValue = findViewById(R.id.ElectionStatusValue)
        electionStatusDateTimeValue = findViewById(R.id.ElectionStatusDateTimeValue)
        btnViewElectionSetup = findViewById(R.id.btnViewElectionSetup)

        // 💡 Dummy Initialization for buttons not used in NO_ELECTION phase, but declared in homepage (Required for strict imitation of declarations)
        // NOTE: These IDs must exist in activity_leader_setup.xml to avoid a crash.
        // If they don't exist, you must add them or change the imitation scope.
        val dummyButton = AppCompatButton(this)
        val dummyTextView = TextView(this)
    }

    private fun populateHeaderData() {
        // ⚠️ BACKEND GUIDE: Replace this static data fetch with a call to your data layer
        val leaderName = "Juan"
        nameTitle.text = leaderName
    }

    /**
     * Initializes header elements: Profile Icon and Notification Icon.
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
    // --- ELECTION PHASE HANDLERS (IMITATED) ---
    // ----------------------------------------------------------------------

    /**
     * ⚠️ IMITATION: Determines the election state.
     * We will force the state to NO_ELECTION or UPCOMING to show setup relevance,
     * but strictly follow homepage's logic for the function structure.
     */
    private fun determineElectionState(): ElectionState {
        // Since this is the setup screen, let's imitate a state where setup is relevant.
        // Using NO_ELECTION to focus on initiating setup.
        return ElectionState.NO_ELECTION
    }

    /**
     * ⚠️ IMITATION: Fetches ONGOING election data (Empty function for NO_ELECTION state).
     */
    private fun fetchOngoingElectionData(): ElectionDateandTimeDetails {
        return ElectionDateandTimeDetails(
            endDate = "October 15, 2025",
            endTime = "5:00 PM"
        )
    }

    /**
     * Updates the visibility, text, and actions of all election-related cards
     * based on the current ElectionState. (Copied and only using necessary parts)
     */
    private fun updateElectionUI(state: ElectionState) {
        // The status card is always visible in all phases (Copied from homepage)
        collegeElectionStatusCard.visibility = View.VISIBLE

        when (state) {
            ElectionState.NO_ELECTION -> {
                // Phase 1: No Election (Copied from homepage)
                electionStatusDateTimeValue.visibility = View.GONE
                electionStatusValue.text = "No election"
                btnViewElectionSetup.text = "Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup::class.java))
                    overridePendingTransition(0, 0)
                }
            }
            ElectionState.UPCOMING -> {
                // Phase 2: Upcoming (Imitating only the relevant status and view button)
                electionStatusDateTimeValue.visibility = View.GONE
                // Hide other cards (already done above)

                electionStatusValue.text = "Upcoming"
                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    overridePendingTransition(0, 0)
                }
                // NOTE: manage candidate/voter buttons are not initialized/handled here
            }
            ElectionState.ONGOING -> {
                // Phase 3: Ongoing (Imitating only the relevant status and view button)
                val details = fetchOngoingElectionData()
                electionStatusValue.text = "Ongoing"
                electionStatusDateTimeValue.text = "(Ends: ${details.endDate} at ${details.endTime})"

                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    overridePendingTransition(0, 0)
                }
                // NOTE: monitor/voter buttons are not initialized/handled here
            }
            ElectionState.ENDED -> {
                // Phase 4: Ended (Imitating only the relevant status and view button)
                electionStatusDateTimeValue.visibility = View.GONE

                electionStatusValue.text = "Ended"
                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    overridePendingTransition(0, 0)
                }
                // NOTE: result button is not initialized/handled here
            }
        }
    }

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION (IMITATED) ---
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
        navHome.setOnClickListener { navigateTo(Leader_homepage::class.java) }
        navSetup.setOnClickListener { /* Do nothing, already here */ }
        navManage.setOnClickListener { navigateTo(Leader_manage_voters::class.java) }
        navMonitor.setOnClickListener { navigateTo(Leader_monitor::class.java) }
        navFaq.setOnClickListener { navigateTo(Leader_faqs::class.java) }
    }
}