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
    private lateinit var leaderCourseTitle: TextView

    private lateinit var btnViewElectionSetup: AppCompatButton
    private lateinit var btnManageCandidates: AppCompatButton
    private lateinit var btnManageVoter: AppCompatButton
    private lateinit var btnOpenDashboard: AppCompatButton
    private lateinit var btnOpenDashboardResult: AppCompatButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        if (!FirebaseAuthHelper.isUserLoggedIn()) {
            // User not logged in, redirect to login
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // Check user role and redirect non-leaders to voter homepage
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUser?.let { user ->
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = user.uid,
                onSuccess = { userData ->
                    val role = userData?.get("role") as? String ?: "VOTER"
                    val isVerified = userData?.get("isVerified") as? Boolean ?: false
                    
                    // If user is not a leader, redirect to voter homepage
                    if (role != "LEADER") {
                        val intent = Intent(this, Homepage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else if (!isVerified) {
                        // If leader is not verified, redirect to verification screen
                        val intent = Intent(this, Leader_Verification::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Continue with leader homepage setup
                        initializeLeaderHomepage()
                    }
                },
                onFailure = { error ->
                    // If we can't get user data, redirect to login
                    android.util.Log.e("Leader_homepage", "Error getting user data: $error")
                    val intent = Intent(this, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            )
        } ?: run {
            // No current user, redirect to login
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    private fun initializeLeaderHomepage() {
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
        loadElectionState()
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
        leaderCourseTitle = findViewById(R.id.LeaderCourseTitle)
        btnViewElectionSetup = findViewById(R.id.btnViewElectionSetup)
        btnManageCandidates = findViewById(R.id.btnManageCandidates)
        btnManageVoter = findViewById(R.id.btnManageVoter)
        btnOpenDashboard = findViewById(R.id.btnOpenDashboard)
        btnOpenDashboardResult = findViewById(R.id.btnOpenDashboardResult)
    }

    private fun populateHeaderData() {
        // Get leader name and college from Firestore
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUser?.let { user ->
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = user.uid,
                onSuccess = { userData ->
                    // Set first name only in header
                    val firstName = userData?.get("firstname") as? String ?: ""
                    nameTitle.text = if (firstName.isNotEmpty()) {
                        firstName
                    } else {
                        user.email?.substringBefore("@") ?: "Leader"
                    }
                    
                    // Set college dynamically
                    val college = userData?.get("college") as? String ?: ""
                    leaderCourseTitle.text = if (college.isNotEmpty()) {
                        college
                    } else {
                        "College not specified"
                    }
                },
                onFailure = { error ->
                    // Fallback to email for name
                    nameTitle.text = currentUser.email?.substringBefore("@") ?: "Leader"
                    leaderCourseTitle.text = "College not specified"
                }
            )
        } ?: run {
            nameTitle.text = "Leader"
            leaderCourseTitle.text = "College not specified"
        }
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
            @Suppress("DEPRECATION")
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
     * Load election state from Firestore
     */
    private fun loadElectionState() {
        FirestoreElectionHelper.determineElectionState(
            onSuccess = { state ->
                updateElectionUI(state)
                // If ongoing, fetch end date details
                if (state == ElectionState.ONGOING) {
                    loadOngoingElectionDetails()
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_homepage", "Error loading election state: $error")
                updateElectionUI(ElectionState.NO_ELECTION)
            }
        )
    }

    /**
     * Fetch the actual end date and time from Firestore when election is ONGOING
     */
    private fun loadOngoingElectionDetails() {
        FirestoreElectionHelper.getCurrentElection(
            onSuccess = { electionDetails ->
                if (electionDetails != null) {
                    // Parse the period string to extract end date
                    // Format: "MMMM dd, yyyy - MMMM dd, yyyy"
                    val parts = electionDetails.period.split(" - ")
                    if (parts.size == 2) {
                        val endDateStr = parts[1]
                        // Get end time from Firestore directly
                        FirestoreElectionHelper.getCurrentElectionId(
                            onSuccess = { electionId ->
                                electionId?.let { id ->
                                    FirestoreLeaderHelper.getElectionById(id,
                                        onSuccess = { electionData ->
                                            electionData?.let { data ->
                                                val endDateTimestamp = data["endDate"] as? com.google.firebase.Timestamp
                                                val endDate = endDateTimestamp?.toDate()
                                                if (endDate != null) {
                                                    val formattedDate = FirestoreLeaderHelper.formatDate(endDate)
                                                    val formattedTime = FirestoreLeaderHelper.formatTime(endDate)
                                                    electionStatusDateTimeValue.text = "(Ends: $formattedDate at $formattedTime)"
                                                }
                                            }
                                        },
                                        onFailure = { }
                                    )
                                }
                            },
                            onFailure = { }
                        )
                    }
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_homepage", "Error loading election details: $error")
            }
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
                    @Suppress("DEPRECATION")
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
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                btnManageCandidates.setOnClickListener {
                    startActivity(Intent(this, Leader_manage_candidates::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                btnManageVoter.setOnClickListener {
                    startActivity(Intent(this, Leader_manage_voters::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
            }
            ElectionState.ONGOING -> {
                // Phase 3: Ongoing
                voterManagementCard.visibility = View.VISIBLE
                electionMonitoringCard.visibility = View.VISIBLE
                electionStatusDateTimeValue.visibility = View.VISIBLE

                electionStatusValue.text = "Ongoing"
                // End date/time will be loaded by loadOngoingElectionDetails()

                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                btnOpenDashboard.setOnClickListener {
                    startActivity(Intent(this, Leader_monitor::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                btnManageVoter.setOnClickListener {
                    startActivity(Intent(this, Leader_monitor::class.java))
                    @Suppress("DEPRECATION")
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
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                btnOpenDashboardResult.setOnClickListener {
                    startActivity(Intent(this, AutomatedReports::class.java))
                    @Suppress("DEPRECATION")
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
                @Suppress("DEPRECATION")
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