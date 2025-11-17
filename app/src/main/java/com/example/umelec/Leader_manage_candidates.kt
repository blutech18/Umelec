package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

// Assuming NotificationManager is defined elsewhere in the com.example.umelec package

// ----------------------------------------------------------------------
// --- DATA CLASSES ---
// ----------------------------------------------------------------------
data class ManageCandidate(
    val candidateId: String,
    val name: String,
    val hasProfileData: Boolean = false
)

data class ManagePosition(
    val name: String,
    val candidates: List<ManageCandidate>
)

class Leader_manage_candidates : AppCompatActivity() {

    // 1. Declare the NotificationManager
    private lateinit var notificationManager: NotificationManager

    // 2. Declare views used in header and content
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView
    private lateinit var btnVoters: AppCompatButton // Added
    private lateinit var contentContainer: LinearLayout // Container for position cards

    // Store current election ID
    private var currentElectionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_manage_candidates)

        // 4. Initialize Notification Manager
        notificationManager = NotificationManager(this)

        // 5. Set up all UI and navigation listeners
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // 6. Load candidates from Firestore
        loadCandidates()
    }

    // --- VIEW INITIALIZATION ---
    private fun initializeViews() {
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)
        btnVoters = findViewById(R.id.btnVoters) // Initialize new button
        contentContainer = findViewById(R.id.ContentContainer) // Initialize content container
    }

    /**
     * Initializes header and manage buttons.
     */
    private fun setupUIListeners() {
        // --- Header Listeners ---
        profileIcon.setOnClickListener {
            startActivity(Intent(this, Leader_profile::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }

        // 💡 LOGIC: Navigate to Voters management
        btnVoters.setOnClickListener {
            val intent = Intent(this, Leader_manage_voters::class.java)
            // Use FLAG_ACTIVITY_REORDER_TO_FRONT to switch tabs smoothly
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // --- INFLATION LOGIC (Nesting Position Cards and Candidate Rows) ---
    // ----------------------------------------------------------------------

    private fun inflatePositionsAndCandidates(positions: List<ManagePosition>) {
        val inflater = LayoutInflater.from(this)

        // Clear content container before inflating
        contentContainer.removeAllViews()

        positions.forEach { position ->
            // 1. INFLATE POSITION CARD (item_position_card.xml)
            val positionCardView = inflater.inflate(R.layout.item_position_card, contentContainer, false)

            val tvPosition = positionCardView.findViewById<TextView>(R.id.tvPosition)
            val candidateListContainer = positionCardView.findViewById<LinearLayout>(R.id.candidate_list_container)

            // Set the Position Name
            tvPosition.text = position.name



            // 2. INFLATE CANDIDATE ROWS (item_candidate_row.xml)
            position.candidates.forEach { candidate ->
                val candidateRowView = inflater.inflate(R.layout.item_candidate_row, candidateListContainer, false)

                val tvCandidateName = candidateRowView.findViewById<TextView>(R.id.tvCandidates)
                val btnEdit = candidateRowView.findViewById<ImageView>(R.id.btnCandidateEdit)
                val btnAddProfile = candidateRowView.findViewById<LinearLayout>(R.id.btnCandidateAddProfile)

                // Set Candidate Name
                tvCandidateName.text = "• ${candidate.name}"

                // 3. LOGIC: Show Edit or Add Profile button
                if (candidate.hasProfileData) {
                    // Profile exists: Show Edit
                    btnEdit.visibility = View.VISIBLE
                    btnAddProfile.visibility = View.GONE
                } else {
                    // Profile does not exist: Show Add Profile
                    btnEdit.visibility = View.GONE
                    btnAddProfile.visibility = View.VISIBLE
                }

                // --- Set Click Listeners ---
                // If btnEdit is visible, we are in Edit Mode (isEdit=true)
                btnEdit.setOnClickListener { 
                    val intent = Intent(this, Leader_manage_candidates_profile::class.java)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_CANDIDATE_ID, candidate.candidateId)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_CANDIDATE_NAME, candidate.name)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_POSITION_NAME, position.name)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_IS_EDIT_MODE, true)
                    startActivity(intent)
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }
                // If btnAddProfile is visible, we are in Add Mode (isEdit=false)
                btnAddProfile.setOnClickListener { 
                    val intent = Intent(this, Leader_manage_candidates_profile::class.java)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_CANDIDATE_ID, candidate.candidateId)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_CANDIDATE_NAME, candidate.name)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_POSITION_NAME, position.name)
                    intent.putExtra(Leader_manage_candidates_profile.EXTRA_IS_EDIT_MODE, false)
                    startActivity(intent)
                    @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
                }

                // Add the candidate row to the list container inside the card
                candidateListContainer.addView(candidateRowView)
            }

            // 4. Add the fully populated Position Card to the main Content Container
            contentContainer.addView(positionCardView)
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
        navManage.setOnClickListener {
            // No action needed as we are already on the Manage screen
        }
        navMonitor.setOnClickListener { navigateTo(Leader_monitor::class.java) }
        navFaq.setOnClickListener { navigateTo(Leader_faqs::class.java) }
    }

    /**
     * Load candidates from Firestore
     */
    private fun loadCandidates() {
        // Get current election ID
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                currentElectionId = electionId
                if (electionId != null) {
                    loadCandidatesForElection(electionId)
                } else {
                    // No active election
                    contentContainer.removeAllViews()
                    val noElectionView = LayoutInflater.from(this).inflate(R.layout.faq_item, contentContainer, false)
                    noElectionView.findViewById<TextView>(R.id.QuestionTextGeneral).text = "No active election"
                    noElectionView.findViewById<TextView>(R.id.AnswerTextGeneral).text = "Please create an election first"
                    contentContainer.addView(noElectionView)
                }
            },
            onFailure = { error ->
                android.util.Log.e("Leader_manage_candidates", "Error getting election ID: $error")
            }
        )
    }

    /**
     * Load candidates grouped by position for an election
     */
    private fun loadCandidatesForElection(electionId: String) {
        FirestoreCandidateHelper.getPositionsForElection(
            electionId = electionId,
            onSuccess = { positions ->
                val positionsData = positions.map { position ->
                    val candidates = position.candidates.map { candidate ->
                        // Check if candidate has profile data
                        ManageCandidate(
                            candidateId = candidate.id,
                            name = candidate.name,
                            hasProfileData = false // Will be updated by checking candidate details
                        )
                    }
                    ManagePosition(position.title, candidates)
                }
                inflatePositionsAndCandidates(positionsData)
                // Check profile data for each candidate
                checkCandidateProfiles(positionsData)
            },
            onFailure = { error ->
                android.util.Log.e("Leader_manage_candidates", "Error loading candidates: $error")
                contentContainer.removeAllViews()
            }
        )
    }

    /**
     * Check which candidates have profile data
     */
    private fun checkCandidateProfiles(positions: List<ManagePosition>) {
        positions.forEach { position ->
            position.candidates.forEach { candidate ->
                FirestoreCandidateHelper.getCandidatePlatformDetails(
                    candidateId = candidate.candidateId,
                    onSuccess = { details ->
                        if (details != null && (details.credentials.isNotEmpty() || details.advocacy.isNotEmpty())) {
                            // Refresh UI to show edit button
                            inflatePositionsAndCandidates(positions.map { 
                                if (it.name == position.name) {
                                    ManagePosition(it.name, it.candidates.map { c ->
                                        if (c.candidateId == candidate.candidateId) {
                                            candidate.copy(hasProfileData = true)
                                        } else {
                                            c
                                        }
                                    })
                                } else {
                                    it
                                }
                            })
                        }
                    },
                    onFailure = { }
                )
            }
        }
    }
}