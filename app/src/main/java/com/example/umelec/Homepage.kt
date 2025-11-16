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



// Data class to represent a single candidate's information
data class Candidate(
    val name: String,
    val position: String,
    val photoResource: Int // Resource ID for the drawable/image (e.g., R.drawable.profile_pic)
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
        
        // Set the content view
        setContentView(R.layout.activity_homepage)

        // 2. 庁 NEW: Initialize the NotificationManager
        notificationManager = NotificationManager(this)

        // Initialize UI components and set up listeners
        setupUI()


        // --- NEW ELECTION INITIALIZATION ---
        // Determine the current election status from the backend/database
        val currentElectionState = determineElectionState()

        // Update the UI based on the state
        updateElectionUI(currentElectionState)
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

        // Get user's full name from Firestore
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser != null) {
            // Fetch user data from Firestore to get firstname and lastname
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = currentUser.uid,
                onSuccess = { userData ->
                    if (userData != null) {
                        val firstname = userData["firstname"] as? String ?: ""
                        val lastname = userData["lastname"] as? String ?: ""
                        val fullName = if (firstname.isNotEmpty() || lastname.isNotEmpty()) {
                            "$firstname $lastname".trim()
                        } else {
                            // Fallback to email username if name not available
                            currentUser.email?.substringBefore("@") ?: "User"
                        }
                        nameTextView.text = fullName
                    } else {
                        // No user data in Firestore, use email as fallback
                        nameTextView.text = currentUser.email?.substringBefore("@") ?: "User"
                    }
                },
                onFailure = { errorMessage ->
                    // If Firestore fetch fails, use email as fallback
                    Log.e("Homepage", "Failed to fetch user data: $errorMessage")
                    nameTextView.text = currentUser.email?.substringBefore("@") ?: "User"
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
            overridePendingTransition(0, 0)

        }

        // 3. 庁 NEW: Notification Icon Click Listener (Integrate NotificationManager)
        notificationIcon.setOnClickListener {
            // Call the reusable manager to handle the dropdown logic
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    // --- ELECTION LOGIC START ---

    // Placeholder function to simulate fetching the election state and data
    private fun determineElectionState(): ElectionState {
        // **IMPORTANT:** Replace this with your actual backend call logic.
        // Use the desired state for testing:
        return ElectionState.ONGOING
        //return ElectionState.NO_ELECTION
        // return ElectionState.UPCOMING
        //return ElectionState.ENDED
    }

    /**
     * Hides all election-related dynamic views and resets them.
     */
    private fun resetElectionViews(
        ongoingLayout: LinearLayout,
        noElectionText: TextView,
        upcomingLayout: LinearLayout,
        electionEndedText: TextView,
        voteNowButton: AppCompatButton
    ) {
        ongoingLayout.visibility = View.GONE
        noElectionText.visibility = View.GONE
        upcomingLayout.visibility = View.GONE
        electionEndedText.visibility = View.GONE


        // Disable Vote Now button by default
        voteNowButton.isEnabled = false // Disable by default
        voteNowButton.alpha = 0.5f // Optional: Dim the button when disabled
        voteNowButton.visibility = View.VISIBLE // Ensure it's visible before state logic might hide it
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

        // Candidate Preview Card Views


        val candidatesContainer: ConstraintLayout = findViewById(R.id.CandidateContainer)
        val textNoCandidates: TextView = findViewById(R.id.textNoCandidates)
        val textCandidatesEnded: TextView = findViewById(R.id.textCandidatesEnded)
        // FIX: Change type from AppCompatButton to TextView
        val btnViewAll: TextView = findViewById(R.id.btnViewAll) // <--- FIXED TYPE
        val candidateListContainer: LinearLayout = findViewById(R.id.candidateListContainer)
        val candidatesCardTitle: TextView = findViewById(R.id.candidatesCardTitle)



        // Reset all views before setting the state-specific ones

        resetElectionViews(ongoingLayout, textNoElection, upcomingLayout, textElectionEnded, btnVoteNow)
        candidatesContainer.visibility = View.GONE
        candidatesCardTitle.text = "Candidates Preview"


        textNoCandidates.visibility = View.GONE
        textCandidatesEnded.visibility = View.GONE
        // FIX: Use isClickable and set text color instead of isEnabled/alpha
        btnViewAll.isClickable = false // Make non-clickable by default
        btnViewAll.setTextColor(Color.parseColor("#AAAAAA")) // Dim the text color (optional)




        // Views for ONGOING election data (for easy backend integration)
        val electionTitleValue: TextView = findViewById(R.id.electionTitleValue)
        val votingPeriodValue: TextView = findViewById(R.id.votingPeriodValue)
        val statusValue: TextView = findViewById(R.id.statusValue)

        // View for UPCOMING election date data

        val upcomingDateValue: TextView = findViewById(R.id.UpcomingDateValue)

        when (state) {

            ElectionState.ONGOING -> {
                // PHASE 1: ONGOING (Election Info Card)

                ongoingLayout.visibility = View.VISIBLE
                btnVoteNow.isEnabled = true


                btnVoteNow.alpha = 1.0f // Restore full opacity


                // **Backend Integration Point (ONGOING)**
                val electionData = fetchOngoingElectionData()
                electionTitleValue.text = electionData.title

                votingPeriodValue.text = electionData.period

                // 🚀 UPDATED LOGIC (From Vote.kt): Status text and button text
                statusValue.text = "Ongoing" // Change from electionData.status
                statusValue.setTextColor(Color.parseColor("#333333")) // Change from Green
                btnVoteNow.text = "Vote now" // Explicitly set button text


                // Set click listener for Vote Now button
                btnVoteNow.setOnClickListener {

                    // Assuming Vote.kt is Vote Activity
                    val intent = Intent(this, Castvote::class.java)

                    startActivity(intent)
                    overridePendingTransition(0, 0)

                }

                // PHASE 1 & 3: ONGOING and UPCOMING (Candidate Preview Card)
                candidatesContainer.visibility = View.VISIBLE


                // textCandidatesEnded and textNoCandidates are already hidden by reset


                // FIX: Use isClickable and set text color back to active
                btnViewAll.isClickable = true
                btnViewAll.setTextColor(Color.parseColor("#0039A6")) // Example: Active Blue color (optional)


                // Populate and set up scrolling
                populateCandidateList(candidateListContainer, candidates)

                setupCandidateScrollControls()

                // Set View All button click listener
                btnViewAll.setOnClickListener {
                    // Assuming Candidates.kt is Candidates Activity
                    val intent = Intent(this, Candidates::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)

                }


            }

            ElectionState.NO_ELECTION -> {
                // PHASE 2: NO ELECTION (Election Info Card)
                textNoElection.visibility = View.VISIBLE


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
                val upcomingDate = fetchUpcomingElectionDate()
                val electionData = fetchOngoingElectionData() // For title

                electionTitleValue.text = electionData.title
                votingPeriodValue.text = upcomingDate // Re-using this field for the key date


                // 🚀 UPDATED LOGIC (From Vote.kt): Status text and Button state
                statusValue.text = "Upcoming"
                statusValue.setTextColor(Color.parseColor("#333333")) // Neutral/Default color

                btnVoteNow.text = "Vote Now"
                btnVoteNow.isEnabled = false // Disable button
                btnVoteNow.alpha = 0.5f
                btnVoteNow.setOnClickListener(null) // Remove any potential click listener


                // PHASE 1 & 3: ONGOING and UPCOMING (Candidate Preview Card)
                candidatesContainer.visibility = View.VISIBLE
                // textCandidatesEnded and textNoCandidates are already hidden by reset


                // FIX: Use isClickable and set text color back to active
                btnViewAll.isClickable = true
                btnViewAll.setTextColor(Color.parseColor("#0039A6")) // Example: Active Blue color (optional)


                // Populate and set up scrolling
                populateCandidateList(candidateListContainer, candidates)

                setupCandidateScrollControls()

                // Set View All button click listener
                btnViewAll.setOnClickListener {
                    val intent = Intent(this, Candidates::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }

            }

            ElectionState.ENDED -> {


                // PHASE 4: ENDED (Election Info Card)
                // Show the "Voting opens on" layout as a container, then the "Election has ended" text
                //upcomingLayout.visibility = View.VISIBLE
                textElectionEnded.visibility = View.VISIBLE

                // 🚀 UPDATED LOGIC (From Vote.kt): Hide the Vote button
                btnVoteNow.visibility = View.GONE


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
                populateCandidateList(candidateListContainer, winningCandidates.map {

                    Candidate(it.name, it.position, it.photoResource)
                })

                setupCandidateScrollControls()

                // 5. Set View All button click listener (Assuming Results.kt is Results Activity)
                btnViewAll.setOnClickListener {
                    val intent = Intent(this, Results::class.java) // Navigate to Results
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }


            }

        }
    }

    // --- Backend Data Simulation (Replace with actual backend calls) ---
    private fun fetchOngoingElectionData(): ElectionDetails {
        // **CODE IT
        return ElectionDetails(
            title = "Student Council Leadership Election",
            period = "March 10, 2025 1:00 PM to March 20, 2025 8:00 pm",
            status = "Active"
        )
    }

    private fun fetchUpcomingElectionDate(): String {
        // **CODE IT FOR EASY BACKEND/DATABASE ACCESS**
        return "March 10, 2025 1:00 PM to March 20, 2025 8:00 pm"
    }

// --- ELECTION LOGIC END ---



    // --- CANDIDATE PREVIEW LOGIC START ---

    // The current candidate list (simulated data)

    private val candidates = listOf(
        Candidate("John Doe", "President", R.drawable.ic_profile), // Use a placeholder drawable ID
        Candidate("Jane Smith", "VP", R.drawable.ic_notification), // Use a placeholder drawable ID
        Candidate("Bob Johnson", "Secretary", R.drawable.ic_profile),
        Candidate("Alice Williams", "Treasurer", R.drawable.ic_notification),

        Candidate("Chris Lee", "Auditor", R.drawable.ic_profile)
    )

    private val winningCandidates = listOf(
        // **IMPORTANT:** Replace with actual winning data from your backend/database
        WinningCandidate("Maya Lopez", "President", R.drawable.ic_profile), // Use a placeholder drawable ID
        WinningCandidate("Daniel Kim", "VP", R.drawable.ic_notification), // Use a placeholder drawable ID
        WinningCandidate("Sarah Chen", "Secretary", R.drawable.ic_profile)
    )

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

        // ImageView for the photo
        val photoView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(80.toPx(), 80.toPx())


            setImageResource(candidate.photoResource)
            contentDescription = "Candidate Photo"
            scaleType = ImageView.ScaleType.CENTER_CROP
            // Note: You need to handle image loading (e.g., rounded corners/image loading library) here
        }
        itemLayout.addView(photoView)

        // TextView for the Name
        val nameView = TextView(context).apply {


            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.toPx()
            }
            text = candidate.name


            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            // Note: setting custom font programmatically is complex;
            // relies on XML definition
        }
        itemLayout.addView(nameView)

        // TextView for the Position
        val positionView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT


            )
            text = candidate.position
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
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
    private fun setupFooterNavigation() {
        // Find all navigation items (LinearLayouts)
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navVote: LinearLayout = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout = findViewById(R.id.nav_results)
        val navFaq: LinearLayout = findViewById(R.id.nav_faq)
        val navigateTo = { activityClass: Class<*> ->
            // Only start the activity if it's not the current one (to prevent unnecessary restarts)
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                startActivity(intent)
                overridePendingTransition(0, 0)
                // Optional: Add finish() if you don't want the user to return here via back button
                // finish()
            }
        }

        navHome.setOnClickListener {}
        navVote.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
        navFaq.setOnClickListener { navigateTo(Faq::class.java) }
    }
}