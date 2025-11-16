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
            // 2. Fetch the data for both candidates from Firestore
            fetchCandidateData(candidateId1, candidateId2)
        } else {
            // Handle the case where the IDs are missing
            android.util.Log.e("Comparison", "Missing candidate IDs")
            finish()
        }

        setupBackNavigation()
        // 4. Setup the persistent footer navigation
        setupFooterNavigation()
    }

    // ----------------------------------------------------------------------
    // --- DATA FETCHING FROM FIRESTORE ---
    // ----------------------------------------------------------------------

    /**
     * Fetches candidate data for both candidates from Firestore.
     */
    private fun fetchCandidateData(id1: String, id2: String) {
        var candidate1: CandidatePlatformDetails? = null
        var candidate2: CandidatePlatformDetails? = null
        var loadCount = 0

        fun checkAndPopulate() {
            if (candidate1 != null && candidate2 != null) {
                populateComparison(candidate1!!, candidate2!!)
            }
        }

        // Fetch first candidate
        FirestoreCandidateHelper.getCandidatePlatformDetails(
            candidateId = id1,
            onSuccess = { details ->
                candidate1 = details ?: CandidatePlatformDetails(
                    candidateId = id1,
                    name = "Candidate Not Found",
                    position = "N/A",
                    courseInfo = "N/A",
                    profilePictureResource = R.drawable.ic_profile,
                    credentials = "Data not available.",
                    advocacy = "Data not available."
                )
                loadCount++
                checkAndPopulate()
            },
            onFailure = { error ->
                android.util.Log.e("Comparison", "Error fetching candidate 1: $error")
                candidate1 = CandidatePlatformDetails(
                    candidateId = id1,
                    name = "Error Loading",
                    position = "N/A",
                    courseInfo = "N/A",
                    profilePictureResource = R.drawable.ic_profile,
                    credentials = "Failed to load data.",
                    advocacy = "Please try again later."
                )
                loadCount++
                checkAndPopulate()
            }
        )

        // Fetch second candidate
        FirestoreCandidateHelper.getCandidatePlatformDetails(
            candidateId = id2,
            onSuccess = { details ->
                candidate2 = details ?: CandidatePlatformDetails(
                    candidateId = id2,
                    name = "Candidate Not Found",
                    position = "N/A",
                    courseInfo = "N/A",
                    profilePictureResource = R.drawable.ic_profile,
                    credentials = "Data not available.",
                    advocacy = "Data not available."
                )
                loadCount++
                checkAndPopulate()
            },
            onFailure = { error ->
                android.util.Log.e("Comparison", "Error fetching candidate 2: $error")
                candidate2 = CandidatePlatformDetails(
                    candidateId = id2,
                    name = "Error Loading",
                    position = "N/A",
                    courseInfo = "N/A",
                    profilePictureResource = R.drawable.ic_profile,
                    credentials = "Failed to load data.",
                    advocacy = "Please try again later."
                )
                loadCount++
                checkAndPopulate()
            }
        )
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