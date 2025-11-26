package com.example.umelec

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide


// Define the possible states for the election card UI
//enum class ElectionState { ONGOING, NO_ELECTION, UPCOMING, ENDED }

// Data class to easily handle election details for the ONGOING phase
//data class ElectionDetails(val title: String, val period: String, val status: String)

// Data class to represent a single candidate's information
data class Candidate(
    val name: String,
    val position: String,
    val photoResource: Int = R.drawable.ic_profile, // Resource ID for the drawable/image (fallback)
    val photoUrl: String? = null // URL for candidate photo (optional)
)

data class WinningCandidate(
    val name: String,
    val position: String,
    val photoResource: Int // Resource ID for the drawable/image
)


// The main activity for the Homepage screen
class Homepage : AppCompatActivity() {

    // 1. 庁 NEW: Declare the reusable NotificationManager
    private lateinit var notificationManager: NotificationManager

    // NOTE: Removed old local 'notifications' list and 'isNotificationDropdownVisible'

    // Shared width variable for candidate and result preview
    private var candidateItemWidth = 0
    private var currentElectionId: String? = null
    private var hasVotedInCurrentElection = false
    private var lastKnownElectionState: ElectionState? = null

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
        
        // Check user role and redirect leaders to their homepage
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUser?.let { user ->
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = user.uid,
                onSuccess = { userData ->
                    val role = userData?.get("role") as? String ?: "VOTER"
                    val isVerified = userData?.get("isVerified") as? Boolean ?: false
                    
                    // If user is a leader, redirect to leader homepage
                    if (role == "LEADER") {
                        if (isVerified) {
                            val intent = Intent(this, Leader_homepage::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            val intent = Intent(this, Leader_Verification::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // Continue with voter homepage setup
                        initializeHomepage()
                    }
                },
                onFailure = { error ->
                    // If we can't get user data, continue with voter homepage (default)
                    android.util.Log.e("Homepage", "Error getting user data: $error")
                    initializeHomepage()
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
    
    private fun initializeHomepage() {
        // Set the content view
        setContentView(R.layout.activity_homepage)

        // 2. 庁 NEW: Initialize the NotificationManager
        notificationManager = NotificationManager(this)

        // Initialize UI components and set up listeners
        setupUI()


        // --- NEW ELECTION INITIALIZATION ---
        loadElectionContext()
        // -----------------------------------

        // --- NEW FOOTER NAVIGATION SETUP ---
        setupFooterNavigation() // <--- ADD THIS LINE


        // -----------------------------------
    }

    private fun setupUI() {
        // Find the views defined in your XML layout
        val nameTextView: TextView = findViewById(R.id.NameTitle)
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val notificationIcon: ImageView = findViewById(R.id.notificationIcon)

        // --- Dynamic Greeting Implementation ---

        // Get user's firstname from Firestore
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser != null) {
            // Fetch user data from Firestore to get firstname only
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = currentUser.uid,
                onSuccess = { userData ->
                    if (userData != null) {
                        // Try to get firstname - check both lowercase and camelCase
                        val firstname = (userData["firstname"] as? String)?.trim() 
                            ?: (userData["firstName"] as? String)?.trim()
                            ?: ""
                        
                        // Display only firstname if available
                        nameTextView.text = if (firstname.isNotEmpty() && firstname.lowercase() != "email") {
                            firstname
                        } else {
                            // Fallback: extract username from email (part before @)
                            val email = currentUser.email ?: ""
                            val emailUsername = if (email.contains("@")) {
                                email.substringBefore("@").trim()
                            } else {
                                email.trim()
                            }
                            
                            // Only use email username if it's not empty and not "email"
                            if (emailUsername.isNotEmpty() && emailUsername.lowercase() != "email") {
                                emailUsername
                            } else {
                                "User"
                            }
                        }
                    } else {
                        // No user data in Firestore, use email username as fallback
                        val email = currentUser.email ?: ""
                        val emailUsername = if (email.contains("@")) {
                            email.substringBefore("@").trim()
                        } else {
                            email.trim()
                        }
                        nameTextView.text = if (emailUsername.isNotEmpty() && emailUsername.lowercase() != "email") {
                            emailUsername
                        } else {
                            "User"
                        }
                    }
                },
                onFailure = { errorMessage ->
                    // If Firestore fetch fails, use email username as fallback
                    Log.e("Homepage", "Failed to fetch user data: $errorMessage")
                    val email = currentUser.email ?: ""
                    val emailUsername = if (email.contains("@")) {
                        email.substringBefore("@").trim()
                    } else {
                        email.trim()
                    }
                    nameTextView.text = if (emailUsername.isNotEmpty() && emailUsername.lowercase() != "email") {
                        emailUsername
                    } else {
                        "User"
                    }
                }
            )
        } else {
            nameTextView.text = "User"
        }

        // --- Profile Icon Click Listener ---

        // Make the profile icon clickable to navigate to the Profile screen.
        profileIcon.setOnClickListener {
            // Create an Intent to start the ProfileActivity class (assuming it's named Profile.kt)
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        // 3. 庁 NEW: Notification Icon Click Listener (Integrate NotificationManager)
        notificationIcon.setOnClickListener {
            // Call the reusable manager to handle the dropdown logic
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    // --- ELECTION LOGIC START ---

    private fun loadElectionContext() {
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                currentElectionId = electionId
                if (electionId == null) {
                    hasVotedInCurrentElection = false
                } else {
                    refreshUserVoteStatus()
                }
                determineElectionState()
            },
            onFailure = { error ->
                android.util.Log.e("Homepage", "Error getting election ID: $error")
                determineElectionState()
            }
        )
    }

    private fun refreshUserVoteStatus() {
        val electionId = currentElectionId ?: return
        val userId = FirebaseAuthHelper.getCurrentUser()?.uid ?: return

        FirestoreElectionHelper.hasUserVoted(
            userId = userId,
            electionId = electionId,
            onSuccess = { hasVoted ->
                hasVotedInCurrentElection = hasVoted
                lastKnownElectionState?.let { updateElectionUI(it) }
            },
            onFailure = { error ->
                android.util.Log.e("Homepage", "Error checking vote status: $error")
            }
        )
    }

    // Determine election state from Firestore
    private fun determineElectionState() {
        FirestoreElectionHelper.determineElectionState(
            onSuccess = { state ->
                lastKnownElectionState = state
                updateElectionUI(state)
            },
            onFailure = { error ->
                android.util.Log.e("Homepage", "Error determining election state: $error")
                lastKnownElectionState = ElectionState.NO_ELECTION
                updateElectionUI(ElectionState.NO_ELECTION)
            }
        )
    }

    /**
     * Hides all election-related dynamic views and resets them.
     */
    private fun resetElectionViews(
        ongoingLayout: LinearLayout,
        noElectionText: TextView,
        upcomingLayout: LinearLayout,
        electionEndedText: TextView,
        voteNowButton: AppCompatButton,
        electionTitleValue: TextView,
        votingPeriodValue: TextView,
        statusValue: TextView,
        candidatesContainer: ConstraintLayout,
        candidatesCardTitle: TextView,
        textNoCandidates: TextView,
        textCandidatesEnded: TextView,
        btnViewAll: TextView,
        candidateListContainer: LinearLayout,
        electionLoadingGroup: LinearLayout,
        candidateLoadingGroup: LinearLayout
    ) {
        ongoingLayout.visibility = View.GONE
        noElectionText.visibility = View.GONE
        upcomingLayout.visibility = View.GONE
        electionEndedText.visibility = View.GONE


        // Disable Vote Now button by default
        voteNowButton.isEnabled = false // Disable by default
        voteNowButton.alpha = 0.5f // Optional: Dim the button when disabled
        voteNowButton.visibility = View.VISIBLE // Ensure it's visible before state logic might hide it

        // Clear election info placeholders to avoid flashing stale data
        electionTitleValue.text = ""
        votingPeriodValue.text = ""
        statusValue.text = ""
        voteNowButton.text = "Vote now"
        voteNowButton.setOnClickListener(null)

        // Reset candidate/results section
        candidatesContainer.visibility = View.GONE
        candidatesCardTitle.text = "Candidates Preview"
        textNoCandidates.visibility = View.GONE
        textCandidatesEnded.visibility = View.GONE
        btnViewAll.isClickable = false
        btnViewAll.setTextColor(Color.parseColor("#AAAAAA"))
        btnViewAll.setOnClickListener(null)
        candidateListContainer.removeAllViews()

        // Show loading placeholders until new data arrives
        electionLoadingGroup.visibility = View.VISIBLE
        candidateLoadingGroup.visibility = View.VISIBLE
    }

    /**
     * Updates the UI elements of the Election Information card based on the current state.
     * This function is designed to make it easy to manage data from the backend.
     */
    private fun updateElectionUI(state: ElectionState) {
        // Find all necessary views
        val ongoingLayout: LinearLayout = findViewById(R.id.OngoingLayout)
        val upcomingLayout: LinearLayout = findViewById(R.id.UpcomingLayout)
        val textNoElection: TextView = findViewById(R.id.textNoElection)
        val textElectionEnded: TextView = findViewById(R.id.textElectionEnded)
        val btnVoteNow: AppCompatButton = findViewById(R.id.btnVoteNow)

        val electionTitleValue: TextView = findViewById(R.id.electionTitleValue)
        val votingPeriodValue: TextView = findViewById(R.id.votingPeriodValue)
        val statusValue: TextView = findViewById(R.id.statusValue)
        val electionLoadingGroup: LinearLayout = findViewById(R.id.electionLoadingGroup)

        // Candidate Preview Card Views
        val candidatesContainer: ConstraintLayout = findViewById(R.id.CandidateContainer)
        val textNoCandidates: TextView = findViewById(R.id.textNoCandidates)
        val textCandidatesEnded: TextView = findViewById(R.id.textCandidatesEnded)
        val btnViewAll: TextView = findViewById(R.id.btnViewAll)
        val candidateListContainer: LinearLayout = findViewById(R.id.candidateListContainer)
        val candidatesCardTitle: TextView = findViewById(R.id.candidatesCardTitle)
        val candidateLoadingGroup: LinearLayout = findViewById(R.id.candidateLoadingGroup)

        // Reset all views before setting the state-specific ones
        resetElectionViews(
            ongoingLayout = ongoingLayout,
            noElectionText = textNoElection,
            upcomingLayout = upcomingLayout,
            electionEndedText = textElectionEnded,
            voteNowButton = btnVoteNow,
            electionTitleValue = electionTitleValue,
            votingPeriodValue = votingPeriodValue,
            statusValue = statusValue,
            candidatesContainer = candidatesContainer,
            candidatesCardTitle = candidatesCardTitle,
            textNoCandidates = textNoCandidates,
            textCandidatesEnded = textCandidatesEnded,
            btnViewAll = btnViewAll,
            candidateListContainer = candidateListContainer,
            electionLoadingGroup = electionLoadingGroup,
            candidateLoadingGroup = candidateLoadingGroup
        )

        when (state) {

            ElectionState.ONGOING -> {
                // PHASE 1: ONGOING (Election Info Card)

                ongoingLayout.visibility = View.VISIBLE
                btnVoteNow.isEnabled = true


                btnVoteNow.alpha = 1.0f // Restore full opacity


                // **Backend Integration Point (ONGOING)**
                fetchElectionData { electionData ->
                    electionLoadingGroup.visibility = View.GONE
                    electionTitleValue.text = electionData.title
                    votingPeriodValue.text = electionData.period

                    if (hasVotedInCurrentElection) {
                        statusValue.text = "Already voted"
                        statusValue.setTextColor(Color.parseColor("#C62828"))
                        btnVoteNow.text = "Already voted"
                        btnVoteNow.isEnabled = false
                        btnVoteNow.alpha = 0.5f
                        btnVoteNow.setOnClickListener(null)
                    } else {
                        // 🚀 UPDATED LOGIC (From Vote.kt): Status text and button text
                        statusValue.text = "Eligible"
                        statusValue.setTextColor(Color.parseColor("#333333"))
                        btnVoteNow.text = "Vote now"
                        btnVoteNow.isEnabled = true
                        btnVoteNow.alpha = 1.0f

                        // Set click listener for Vote Now button
                        btnVoteNow.setOnClickListener {
                            val intent = Intent(this, Vote::class.java)
                            startActivity(intent)
                        }
                    }

                    // PHASE 1 & 3: ONGOING and UPCOMING (Candidate Preview Card)
                    candidatesContainer.visibility = View.VISIBLE
                    btnViewAll.isClickable = true
                    btnViewAll.setTextColor(Color.parseColor("#0039A6"))

                    // Fetch and populate candidates
                    currentElectionId?.let { electionId ->
                        FirestoreCandidateHelper.getCandidatesForPreview(
                            electionId = electionId,
                            limit = 5,
                            onSuccess = { candidates ->
                                candidateLoadingGroup.visibility = View.GONE
                                populateCandidateList(candidateListContainer, candidates)
                                setupCandidateScrollControls()
                            },
                            onFailure = { error ->
                                android.util.Log.e("Homepage", "Error fetching candidates: $error")
                                candidateLoadingGroup.visibility = View.GONE
                                textNoCandidates.visibility = View.VISIBLE
                            }
                        )
                    } ?: run {
                        candidateLoadingGroup.visibility = View.GONE
                        textNoCandidates.visibility = View.VISIBLE
                    }

                    // Set View All button click listener
                    btnViewAll.setOnClickListener {
                        val intent = Intent(this, Candidates::class.java)
                        startActivity(intent)
                    }
                }


            }

            ElectionState.NO_ELECTION -> {
                // PHASE 2: NO ELECTION (Election Info Card)
                textNoElection.visibility = View.VISIBLE
                electionLoadingGroup.visibility = View.GONE
                candidateLoadingGroup.visibility = View.GONE

                // btnVoteNow remains disabled

                // PHASE 2: NO ELECTION (Candidate Preview Card)

                textNoCandidates.visibility = View.VISIBLE
                // btnViewAll remains disabled/non-clickable

            }

            ElectionState.UPCOMING -> {
                // PHASE 3: UPCOMING (Election Info Card)

                // 🚀 UPDATED LOGIC (From Vote.kt): Use OngoingLayout
                ongoingLayout.visibility = View.VISIBLE
                upcomingLayout.visibility = View.GONE // Ensure original upcoming layout is hidden

                // **Backend Integration Point (UPCOMING)**
                fetchElectionData { electionData ->
                    electionLoadingGroup.visibility = View.GONE
                    electionTitleValue.text = electionData.title
                    votingPeriodValue.text = electionData.period

                    // 🚀 UPDATED LOGIC (From Vote.kt): Status text and Button state
                    statusValue.text = "Upcoming"
                    statusValue.setTextColor(Color.parseColor("#333333"))
                    btnVoteNow.text = "Vote Now"
                    btnVoteNow.isEnabled = false
                    btnVoteNow.alpha = 0.5f
                    btnVoteNow.setOnClickListener(null)

                    // PHASE 1 & 3: ONGOING and UPCOMING (Candidate Preview Card)
                    candidatesContainer.visibility = View.VISIBLE
                    btnViewAll.isClickable = true
                    btnViewAll.setTextColor(Color.parseColor("#0039A6"))

                    // Fetch and populate candidates
                    currentElectionId?.let { electionId ->
                        FirestoreCandidateHelper.getCandidatesForPreview(
                            electionId = electionId,
                            limit = 5,
                            onSuccess = { candidates ->
                                candidateLoadingGroup.visibility = View.GONE
                                populateCandidateList(candidateListContainer, candidates)
                                setupCandidateScrollControls()
                            },
                            onFailure = { error ->
                                android.util.Log.e("Homepage", "Error fetching candidates: $error")
                                candidateLoadingGroup.visibility = View.GONE
                                textNoCandidates.visibility = View.VISIBLE
                            }
                        )
                    } ?: run {
                        candidateLoadingGroup.visibility = View.GONE
                        textNoCandidates.visibility = View.VISIBLE
                    }

                    // Set View All button click listener
                    btnViewAll.setOnClickListener {
                        val intent = Intent(this, Candidates::class.java)
                        startActivity(intent)
                    }
                }

            }

            ElectionState.ENDED -> {


                // PHASE 4: ENDED (Election Info Card)
                // Show the "Voting opens on" layout as a container, then the "Election has ended" text
                //upcomingLayout.visibility = View.VISIBLE
                textElectionEnded.visibility = View.VISIBLE

                // 🚀 UPDATED LOGIC (From Vote.kt): Hide the Vote button
                btnVoteNow.visibility = View.GONE

                fetchElectionData { electionData ->
                    electionLoadingGroup.visibility = View.GONE
                    electionTitleValue.text = electionData.title
                    votingPeriodValue.text = electionData.period
                    statusValue.text = "Ended"
                    statusValue.setTextColor(Color.parseColor("#333333"))
                }


                // PHASE 4: ENDED (Candidate Preview Card)

                //textCandidatesEnded.visibility = View.VISIBLE
                // btnViewAll remains disabled/non-clickable

                // NEW PHASE 4: ENDED (Results Preview Card)

                // 1. Change the title to "Results Preview"
                candidatesCardTitle.text = "Results Preview"


                // 2. Display the candidates/winners list
                candidatesContainer.visibility = View.VISIBLE

                // textCandidatesEnded is hidden, as we are showing the list

                // 3. Make the "View All" button clickable
                btnViewAll.isClickable = true

                btnViewAll.setTextColor(Color.parseColor("#0039A6")) // Active Blue color

                // 4. Populate with winning candidates
                // **Backend Integration Point (ENDED):** Use the list of winning candidates
                currentElectionId?.let { electionId ->
                    FirestoreCandidateHelper.getWinningCandidates(
                        electionId = electionId,
                        onSuccess = { winners ->
                            candidateLoadingGroup.visibility = View.GONE
                            val candidateList = winners.map {
                                Candidate(it.name, it.position, it.photoResource)
                            }
                            populateCandidateList(candidateListContainer, candidateList)
                            setupCandidateScrollControls()
                        },
                        onFailure = { error ->
                            android.util.Log.e("Homepage", "Error fetching winners: $error")
                            candidateLoadingGroup.visibility = View.GONE
                            textCandidatesEnded.visibility = View.VISIBLE
                        }
                    )
                } ?: run {
                    android.util.Log.e("Homepage", "No election ID available")
                    candidateLoadingGroup.visibility = View.GONE
                    textCandidatesEnded.visibility = View.VISIBLE
                }

                // 5. Set View All button click listener
                btnViewAll.setOnClickListener {
                    val intent = Intent(this, Results::class.java)
                    startActivity(intent)
                }


            }

        }
    }

    // --- Backend Data Integration ---
    private fun fetchElectionData(callback: (ElectionDetails) -> Unit) {
        FirestoreElectionHelper.getCurrentElection(
            onSuccess = { electionData ->
                if (electionData != null) {
                    callback(electionData)
                } else {
                    // Default fallback
                    callback(ElectionDetails(
                        title = "No Active Election",
                        period = "",
                        status = "None"
                    ))
                }
            },
            onFailure = { error ->
                android.util.Log.e("Homepage", "Error fetching election data: $error")
                // Default fallback
                callback(ElectionDetails(
                    title = "Error Loading Election",
                    period = "",
                    status = "Error"
                ))
            }
        )
    }

// --- ELECTION LOGIC END ---



    // --- CANDIDATE PREVIEW LOGIC START ---

    /**
     * Dynamically populates
     * the HorizontalScrollView with candidate items.
     * @param container The LinearLayout inside the HorizontalScrollView.
     * @param candidates The list of candidates to display.
     */
    private fun populateCandidateList(container: LinearLayout, candidates: List<Candidate>) {
        // 1. Get the width of the main container (CandidateContainer) to calculate candidate item width
        val constraintLayout: ConstraintLayout = findViewById(R.id.CandidateContainer)
        constraintLayout.post {
            // Calculate the width for one candidate item (e.g., half the screen minus padding for arrows)

            val viewWidth = constraintLayout.width


            val arrowWidth = findViewById<ImageButton>(R.id.btnPrevCandidate).width +
                    findViewById<ImageButton>(R.id.btnNextCandidate).width +
                    (resources.getDimensionPixelSize(R.dimen.candidate_padding) * 2) // Add padding for safety

            // We want to show roughly two candidates at a time.
            candidateItemWidth = (viewWidth - arrowWidth) / 2

            // 2. Clear any existing views
            container.removeAllViews()

            // 3. Dynamically create and add a view for each candidate
            candidates.forEach { candidate ->
                val candidateItemView = createCandidateItemView(candidate)


                container.addView(candidateItemView)
            }
        }
    }

    /**
     * Creates a single candidate item view programmatically.
     */
    private fun createCandidateItemView(candidate: Candidate): View {
        // You must replace this with your actual candidate item XML if you have one.
        // For now, we recreate the structure from the XML dynamically.
        val context = this

        // Root LinearLayout for the candidate item
        val itemLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                candidateItemWidth.coerceAtLeast(200), // Ensures minimum width if calculations fail initially
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(8.toPx(), 8.toPx(), 8.toPx(), 8.toPx()) // Convert DP to pixels
        }

        // CircleImageView for the photo (circular profile picture)
        val photoView = de.hdodenhof.circleimageview.CircleImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(80.toPx(), 80.toPx())
            contentDescription = "Candidate Photo"
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        
        // Load image from URL if available, otherwise use default drawable
        if (!candidate.photoUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(candidate.photoUrl)
                .placeholder(candidate.photoResource)
                .error(candidate.photoResource)
                .centerCrop()
                .into(photoView)
        } else {
            photoView.setImageResource(candidate.photoResource)
        }
        
        itemLayout.addView(photoView)

        // TextView for the Name
        val nameView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.toPx()
            }
            text = candidate.name
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            gravity = android.view.Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setLineSpacing(0f, 1.1f)
            // Note: setting custom font programmatically is complex;
            // relies on XML definition
        }
        itemLayout.addView(nameView)

        // TextView for the Position
        val positionView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT


            )
            text = candidate.position
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            gravity = android.view.Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            // Note: setting custom font programmatically is complex;
            // relies on XML definition
        }
        itemLayout.addView(positionView)

        return itemLayout
    }

    /**
     * Handles the click of the previous/next arrow buttons to scroll the candidate list.
     */
    private fun setupCandidateScrollControls() {
        val scrollView: HorizontalScrollView = findViewById(R.id.candidateScrollView)
        val btnPrev: ImageButton = findViewById(R.id.btnPrevCandidate)
        val btnNext: ImageButton = findViewById(R.id.btnNextCandidate)

        // Previous button logic: scroll left by the width of one candidate item
        btnPrev.setOnClickListener {
            scrollView.smoothScrollBy(-candidateItemWidth, 0)
        }


        // Next button logic: scroll right by the width of one candidate item
        btnNext.setOnClickListener {
            scrollView.smoothScrollBy(candidateItemWidth, 0)
        }

        // Initial check (hiding one button if list is short or at the start/end)
        scrollView.post {
            // You would typically monitor scroll position to hide/show buttons,


            // but for a fixed step scroll, enabling both is often simpler for a preview.
            // For now, we'll keep both visible unless the candidate list is very short.
        }
    }

    // Utility extension function to convert DP to pixels
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

// --- CANDIDATE PREVIEW LOGIC END ---


    // --- FOOTER NAVIGATION LOGIC START ---

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
            // Only start the activity if it's not the current one (to prevent unnecessary restarts)
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                startActivity(intent)


                // Optional: Add finish() if you don't want the user to return here via back button
                // finish()
            }
        }

        // Set Click Listeners

        // Home (Current Activity - No action needed unless reloading is desired)
        // We can keep this listener
        // empty or make it re-initialize the current activity.
        navHome.setOnClickListener {
            // Since we are already on Homepage.kt, we typically do nothing or smooth scroll to top.
        }
        navVote.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
        navFaq.setOnClickListener { navigateTo(Faq::class.java) }
    }

// --- FOOTER NAVIGATION LOGIC END ---

    // NOTE: The local notification logic (toggleNotificationDropdown and showNotificationDropdown)
    // has been removed and replaced by the single call to notificationManager.toggleNotificationDropdown(it as ImageView)
}