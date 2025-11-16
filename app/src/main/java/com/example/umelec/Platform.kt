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
            // 2. Fetch or simulate candidate data based on the ID
            val candidateDetails = fetchCandidateData(candidateId)

            // 3. Populate the UI elements with the fetched data
            populatePlatform(candidateDetails)
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
    // --- DATA FETCHING (Replace this with your actual backend API call) ---
    // ----------------------------------------------------------------------

    /**
     * Simulates fetching a full candidate profile from a database or API.
     * @param id The unique ID of the candidate.
     * @return A CandidatePlatformDetails object containing all required UI data.
     */
    private fun fetchCandidateData(id: String): CandidatePlatformDetails {
        // ⭐️ In a real application, you would replace this entire 'when' block
        // with a call to your backend API using the 'id'.

        return when (id) {
            "JANE_D" -> CandidatePlatformDetails(
                candidateId = "JANE_D",
                name = "Jane Doe",
                position = "Marketing Director",
                courseInfo = "III - CCIS",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Graduated with honors. Former Editor-in-Chief of the student paper and team lead for two successful university events.",
                advocacy = "My platform focuses on modernizing student services through digitalization and creating a more inclusive community by funding new cultural organizations."
            )
            "JOHN_S" -> CandidatePlatformDetails(
                candidateId = "JOHN_S",
                name = "John Smith",
                position = "Marketing Director",
                courseInfo = "IV - CCIS",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Lead programmer for the university's attendance system. Holds multiple certifications in project management and database administration.",
                advocacy = "I advocate for better student technological infrastructure, including faster campus Wi-Fi and subsidized cloud storage for all students."
            )
            // Add other candidates here as necessary...
            else -> CandidatePlatformDetails( // Default/Error case
                candidateId = id,
                name = "Candidate Not Found",
                position = "N/A",
                courseInfo = "N/A",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Data not available.",
                advocacy = "Data not available."
            )
        }
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