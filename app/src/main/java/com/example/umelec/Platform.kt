package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.ImageButton // ⭐️ Import ImageButton
import androidx.appcompat.app.AppCompatActivity

// --- DATA STRUCTURE FOR CANDIDATE'S PLATFORM DETAILS ---


class Platform : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_platform)

        // 1. Get the candidate ID passed from Position.kt
        val candidateId = intent.getStringExtra("CANDIDATE_ID")

        if (candidateId != null) {
            // 2. Fetch candidate data from Firestore
            fetchCandidateData(candidateId)
        } else {
            // Show error if no candidate ID provided
            finish()
        }

        // ⭐️ NEW: Setup the back button functionality
        setupBackNavigation()

        // 4. Setup the persistent footer navigation
        setupFooterNavigation()
    }

    // ----------------------------------------------------------------------
    // --- TOP NAVIGATION LOGIC (Back Button) ---
    // ----------------------------------------------------------------------

    /**
     * Sets up the click listener for the back button to navigate to the previous screen.
     */
    private fun setupBackNavigation() {
        // Find the back button ID provided in your XML
        val backButton: ImageButton = findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // ⭐️ FIX: Closes the current activity and returns to the previous one
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // --- DATA FETCHING FROM FIRESTORE ---
    // ----------------------------------------------------------------------

    /**
     * Fetches candidate platform details from Firestore.
     * @param id The unique ID of the candidate.
     */
    private fun fetchCandidateData(id: String) {
        FirestoreCandidateHelper.getCandidatePlatformDetails(
            candidateId = id,
            onSuccess = { details ->
                if (details != null) {
                    populatePlatform(details)
                } else {
                    // Show error candidate
                    populatePlatform(CandidatePlatformDetails(
                        candidateId = id,
                        name = "Candidate Not Found",
                        position = "N/A",
                        courseInfo = "N/A",
                        profilePictureResource = R.drawable.ic_profile,
                        credentials = "Data not available.",
                        advocacy = "Data not available."
                    ))
                }
            },
            onFailure = { error ->
                android.util.Log.e("Platform", "Error fetching candidate data: $error")
                // Show error candidate
                populatePlatform(CandidatePlatformDetails(
                    candidateId = id,
                    name = "Error Loading Candidate",
                    position = "N/A",
                    courseInfo = "N/A",
                    profilePictureResource = R.drawable.ic_profile,
                    credentials = "Failed to load data.",
                    advocacy = "Please try again later."
                ))
            }
        )
    }

    // ----------------------------------------------------------------------
    // --- UI POPULATION LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Maps the fetched data structure to the views in activity_platform.xml.
     */
    private fun populatePlatform(details: CandidatePlatformDetails) {
        // Header Views
        val profilePic: ImageView = findViewById(R.id.iv_header_profile_picture)
        val nameText: TextView = findViewById(R.id.tv_header_name)
        val positionText: TextView = findViewById(R.id.tv_header_position)
        val courseText: TextView = findViewById(R.id.tv_header_course_info)

        // Platform Content Views
        val credentialsValue: TextView = findViewById(R.id.credentialsValue)
        val advocacyValue: TextView = findViewById(R.id.advocacyValue)

        // Set the content
        profilePic.setImageResource(details.profilePictureResource)
        nameText.text = details.name
        positionText.text = details.position
        courseText.text = details.courseInfo
        credentialsValue.text = details.credentials
        advocacyValue.text = details.advocacy
    }

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Sets up click listeners for all elements in the footer navigation bar.
     */
    private fun setupFooterNavigation() {
        // Find all navigation items (LinearLayouts)
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navVote: LinearLayout = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout = findViewById(R.id.nav_results)
        val navFaq: LinearLayout = findViewById(R.id.nav_faq)

        // Helper function to navigate to a new Activity
        val navigateTo = { activityClass: Class<*> ->
            val intent = Intent(this, activityClass)
            // Use this flag for smoother tab switching
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Set Click Listeners
        navHome.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
        navFaq.setOnClickListener { navigateTo(Faq::class.java) }
    }
}