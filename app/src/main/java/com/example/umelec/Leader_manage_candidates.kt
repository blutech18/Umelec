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
// --- DATA CLASSES (RENAMED & EXPANDED FOR MOCK PROFILE DATA) ---
// ----------------------------------------------------------------------
data class ManageCandidate(
    val name: String,
    // Simulates checking if the candidate has completed their profile data
    val hasProfileData: Boolean,
    // Add mock profile data fields for Leader_manage_candidates_profile to fetch
    val mockYear: String = "1st Year",
    val mockCredentials: String = "No credentials provided yet.",
    val mockPlatform: String = "No platform provided yet.",
    val mockPhotoUri: String? = null // Null if no photo is uploaded
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

    // 3. Fake Data (Using the new data class names and mock data)
    private val positionsData = listOf(
        ManagePosition("President", listOf(
            ManageCandidate(
                name = "Alice Johnson",
                hasProfileData = true,
                mockYear = "4th Year",
                mockCredentials = "• Outstanding Leadership Award\n• Former Class President",
                mockPlatform = "To champion student welfare through digital transformation.",
                mockPhotoUri = "content://mock/uploaded/alice_johnson_photo.jpg" // Mock URI
            ),
            ManageCandidate(
                name = "Bob Williams",
                hasProfileData = false
            )
        )),
        ManagePosition("Vice President", listOf(
            ManageCandidate(
                name = "Charlie Brown",
                hasProfileData = true,
                mockYear = "3rd Year",
                mockCredentials = "• Debating Team Captain (2 years)\n• Published Research Assistant",
                mockPlatform = "Focusing on mental health and academic support initiatives.",
                mockPhotoUri = "content://mock/uploaded/charlie_brown_photo.jpg"
            )
        )),
        ManagePosition("Secretary", listOf(
            ManageCandidate(
                name = "Dana Scully",
                hasProfileData = false
            ),
            ManageCandidate(
                name = "Fox Mulder",
                hasProfileData = false
            )
        ))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_manage_candidates)

        // 4. Initialize Notification Manager (using a mock if the class isn't defined)
        notificationManager = object : NotificationManager(this) {
            override fun toggleNotificationDropdown(view: ImageView) {}
        }

        // 5. Set up all UI and navigation listeners
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // 6. New logic: Inflate content based on fake data
        inflatePositionsAndCandidates(positionsData)
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
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // --- INFLATION LOGIC (Nesting Position Cards and Candidate Rows) ---
    // ----------------------------------------------------------------------

    private fun inflatePositionsAndCandidates(positions: List<ManagePosition>) {
        val inflater = LayoutInflater.from(this)

        // Clear content container before inflating
        //contentContainer.removeAllViews()

        positions.forEach { position ->
            // 1. INFLATE POSITION CARD (item_position_card.xml)
            val positionCardView = inflater.inflate(R.layout.item_position_card, contentContainer, false)

            val tvPosition = positionCardView.findViewById<TextView>(R.id.tvPosition)
            val candidateListContainer = positionCardView.findViewById<LinearLayout>(R.id.candidate_list_container)

            // Set the Position Name
            tvPosition.text = position.name

            // 💡 LOGIC: Click listener to navigate to the profile screen (Updated for EDIT/ADD mode)
            val openProfile = { candidateName: String, positionName: String, isEdit: Boolean ->
                val intent = Intent(this, Leader_manage_candidates_profile::class.java)
                intent.putExtra(Leader_manage_candidates_profile.EXTRA_CANDIDATE_NAME, candidateName)
                intent.putExtra(Leader_manage_candidates_profile.EXTRA_POSITION_NAME, positionName)
                // Set the critical mode flag
                intent.putExtra(Leader_manage_candidates_profile.EXTRA_IS_EDIT_MODE, isEdit)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }


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
                btnEdit.setOnClickListener { openProfile(candidate.name, position.name, true) }
                // If btnAddProfile is visible, we are in Add Mode (isEdit=false)
                btnAddProfile.setOnClickListener { openProfile(candidate.name, position.name, false) }

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

    // Mock NotificationManager to allow the code to compile if you haven't provided its definition
    open class NotificationManager(private val context: AppCompatActivity) {
        open fun toggleNotificationDropdown(view: ImageView) {
            // Placeholder implementation
        }
    }
}