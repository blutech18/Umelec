package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

// --- REUSED DATA STRUCTURE FOR CANDIDATE'S PLATFORM DETAILS ---


class Comparison : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparison)

        // 1. Retrieve the two candidate IDs passed from Position.kt
        val candidateId1 = intent.getStringExtra("CANDIDATE_ID_1")
        val candidateId2 = intent.getStringExtra("CANDIDATE_ID_2")

        if (candidateId1 != null && candidateId2 != null) {
            // 2. Fetch the data for both candidates
            val candidate1Details = fetchCandidateData(candidateId1)
            val candidate2Details = fetchCandidateData(candidateId2)

            // 3. Populate the UI elements
            populateComparison(candidate1Details, candidate2Details)
        } else {
            // Handle the case where the IDs are missing (e.g., show an error or close the activity)
            // For production, you might want a Toast message here.
            // Toast.makeText(this, "Error: Missing candidate data for comparison.", Toast.LENGTH_LONG).show()
        }

        setupBackNavigation()
        // 4. Setup the persistent footer navigation
        setupFooterNavigation()
    }

    // ----------------------------------------------------------------------
    // --- DATA FETCHING LOGIC (Copied from Platform.kt) ---
    // ----------------------------------------------------------------------

    /**
     * Simulates fetching a full candidate profile from a database or API.
     * This function must be kept synchronized with the one in Platform.kt.
     */
    private fun fetchCandidateData(id: String): CandidatePlatformDetails {
        // NOTE: Ensure R.drawable.profile exists in your project.
        return when (id) {
            "JANE_D" -> CandidatePlatformDetails(
                candidateId = "JANE_D",
                name = "Jane Doe",
                position = "Marketing Director",
                courseInfo = "III - BCSAD",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Graduated with honors. Former Editor-in-Chief of the student paper and team lead for two successful university events.",
                advocacy = "My platform focuses on modernizing student services through digitalization and creating a more inclusive community by funding new cultural organizations."
            )
            "JOHN_S" -> CandidatePlatformDetails(
                candidateId = "JOHN_S",
                name = "John Smith",
                position = "Marketing Director",
                courseInfo = "IV - BSIT",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Lead programmer for the university's attendance system. Holds multiple certifications in project management and database administration.",
                advocacy = "I advocate for better student technological infrastructure, including faster campus Wi-Fi and subsidized cloud storage for all students."
            )
            "SARAH_L" -> CandidatePlatformDetails(
                candidateId = "SARAH_L",
                name = "Sarah Lee",
                position = "Treasurer",
                courseInfo = "II - BSBA",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Top student in Accounting and Finance. Managed the budget for the university's largest annual fundraiser.",
                advocacy = "Focused on maximizing transparency in student funds and introducing new, low-cost financial literacy workshops."
            )
            "MARK_T" -> CandidatePlatformDetails(
                candidateId = "MARK_T",
                name = "Mark Tan",
                position = "President",
                courseInfo = "I - BSED",
                profilePictureResource = R.drawable.ic_profile,
                credentials = "Founder of the Peer Mentorship Program. Proven leadership skills across multiple community and academic organizations.",
                advocacy = "My core platform is centered on student welfare, mental health support, and enhancing the feedback loop between students and administration."
            )
            else -> CandidatePlatformDetails( // Default/Error case
                candidateId = id,
                name = "Error Loading Data",
                position = "N/A",
                courseInfo = "N/A",
                profilePictureResource = R.drawable.ic_profile, // Use a generic placeholder
                credentials = "Data not available.",
                advocacy = "Data not available."
            )
        }
    }

    // ----------------------------------------------------------------------
    // --- UI POPULATION LOGIC (New for Comparison.kt) ---
    // ----------------------------------------------------------------------

    /**
     * Populates the comparison layout with data for both Candidate 1 and Candidate 2.
     * Uses safe calls (`?`) for all view lookups to prevent crashes if IDs are missing.
     */
    private fun populateComparison(c1: CandidatePlatformDetails, c2: CandidatePlatformDetails) {

        // --- Candidate 1 Views ---
        findViewById<ImageView>(R.id.iv_profile_picture1)?.setImageResource(c1.profilePictureResource)
        findViewById<TextView>(R.id.tv_name1)?.text = c1.name
        findViewById<TextView>(R.id.credentialsValue1)?.text = c1.credentials
        findViewById<TextView>(R.id.advocacyValue1)?.text = c1.advocacy

        // --- Candidate 2 Views ---
        findViewById<ImageView>(R.id.iv_profile_picture2)?.setImageResource(c2.profilePictureResource)
        findViewById<TextView>(R.id.tv_name2)?.text = c2.name
        findViewById<TextView>(R.id.credentialsValue2)?.text = c2.credentials
        findViewById<TextView>(R.id.advocacyValue2)?.text = c2.advocacy
    }

    // ----------------------------------------------------------------------
    // --- TOP NAVIGATION LOGIC ---
    // ----------------------------------------------------------------------

    private fun setupBackNavigation() {
        val backButton: ImageButton? = findViewById(R.id.btnBack)
        backButton?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION LOGIC (CRASH-PROOFED) ---
    // ----------------------------------------------------------------------

    /**
     * Sets up click listeners for all elements in the footer navigation bar, using safe calls.
     */
    private fun setupFooterNavigation() {
        // Use nullable LinearLayouts (`LinearLayout?`) and the safe call operator (`?.`)
        val navHome: LinearLayout? = findViewById(R.id.nav_home)
        val navVote: LinearLayout? = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout? = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout? = findViewById(R.id.nav_results)
        val navFaq: LinearLayout? = findViewById(R.id.nav_faq)

        // Helper function to navigate to a new Activity
        val navigateTo = { activityClass: Class<*> ->
            val intent = Intent(this, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Set Click Listeners (Only set listener if view is found)
        navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote?.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates?.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults?.setOnClickListener { navigateTo(Results::class.java) }
        navFaq?.setOnClickListener { navigateTo(Faq::class.java) }
    }
}