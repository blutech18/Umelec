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
    private lateinit var leaderCourseTitle: TextView

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
        loadElectionState()
    }

    private fun initializeViews() {
        // Header Views
        nameTitle = findViewById(R.id.NameTitle)
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)

        // 💡 Initialize Card Views (Used for status and to be hidden)
        collegeElectionStatusCard = findViewById(R.id.CollegeElectionStatusCard)

        // 💡 Initialize Status and Button Views
        leaderCourseTitle = findViewById(R.id.LeaderCourseTitle)
        electionStatusValue = findViewById(R.id.ElectionStatusValue)
        electionStatusDateTimeValue = findViewById(R.id.ElectionStatusDateTimeValue)
        btnViewElectionSetup = findViewById(R.id.btnViewElectionSetup)
        
        // Set Setup page specific title
        leaderCourseTitle.text = "Election Setup"

        // 💡 Dummy Initialization for buttons not used in NO_ELECTION phase, but declared in homepage (Required for strict imitation of declarations)
        // NOTE: These IDs must exist in activity_leader_setup.xml to avoid a crash.
        // If they don't exist, you must add them or change the imitation scope.
        val dummyButton = AppCompatButton(this)
        val dummyTextView = TextView(this)
    }

    private fun populateHeaderData() {
        // Get leader name from Firestore
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUser?.let { user ->
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = user.uid,
                onSuccess = { userData ->
                    val firstName = userData?.get("firstname") as? String ?: ""
                    val lastName = userData?.get("lastname") as? String ?: ""
                    val fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                        "$firstName $lastName".trim()
                    } else {
                        user.email?.substringBefore("@") ?: "Leader"
                    }
                    nameTitle.text = fullName
                },
                onFailure = { error ->
                    // Fallback to email
                    nameTitle.text = currentUser.email?.substringBefore("@") ?: "Leader"
                }
            )
        } ?: run {
            nameTitle.text = "Leader"
        }
    }

    /**
     * Initializes header elements: Profile Icon and Notification Icon.
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
    // --- ELECTION PHASE HANDLERS (IMITATED) ---
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
                android.util.Log.e("Leader_Setup", "Error loading election state: $error")
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
                android.util.Log.e("Leader_Setup", "Error loading election details: $error")
            }
        )
    }

    /**
     * Updates the visibility, text, and actions of all election-related cards
     * based on the current ElectionState. Setup page shows setup-specific content.
     */
    private fun updateElectionUI(state: ElectionState) {
        // The status card is always visible in all phases
        collegeElectionStatusCard.visibility = View.VISIBLE

        when (state) {
            ElectionState.NO_ELECTION -> {
                // Phase 1: No Election - Show create election option
                electionStatusDateTimeValue.visibility = View.GONE
                electionStatusValue.text = "No election"
                btnViewElectionSetup.text = "Create New Election"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
            }
            ElectionState.UPCOMING -> {
                // Phase 2: Upcoming - Show setup details and management options
                electionStatusDateTimeValue.visibility = View.GONE
                electionStatusValue.text = "Upcoming"
                btnViewElectionSetup.text = "Manage Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                // Load setup details (positions, candidates count)
                loadSetupDetails()
            }
            ElectionState.ONGOING -> {
                // Phase 3: Ongoing - Show setup details
                electionStatusDateTimeValue.visibility = View.VISIBLE
                electionStatusValue.text = "Ongoing"
                // End date/time will be loaded by loadOngoingElectionDetails()
                btnViewElectionSetup.text = "View Election Setup"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup_details::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                // Load setup details
                loadSetupDetails()
            }
            ElectionState.ENDED -> {
                // Phase 4: Ended - Allow creating new election
                electionStatusDateTimeValue.visibility = View.GONE
                electionStatusValue.text = "Ended"
                btnViewElectionSetup.text = "Create New Election"
                btnViewElectionSetup.setOnClickListener {
                    startActivity(Intent(this, Leader_electionsetup::class.java))
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
            }
        }
    }

    /**
     * Load setup details (positions count, candidates count) to show on Setup page
     */
    private fun loadSetupDetails() {
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                electionId?.let { id ->
                    // Load positions count
                    FirestoreLeaderHelper.getPositionsForElection(
                        electionId = id,
                        onSuccess = { positions ->
                            android.util.Log.d("Leader_Setup", "Found ${positions.size} positions for election")
                            // You can display this info in the UI if needed
                            // For now, we'll just log it
                        },
                        onFailure = { error ->
                            android.util.Log.e("Leader_Setup", "Error loading positions: $error")
                        }
                    )
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_Setup", "Error getting election ID: $error")
            }
        )
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
                @Suppress("DEPRECATION")
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