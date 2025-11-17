package com.example.umelec

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import android.animation.AnimatorInflater
import android.os.Build


// --- DATA STRUCTURE FOR POSITIONS ---
// This is the model you would map your backend/database data to.
data class PositionItem(
    val positionId: String,
    val positionName: String,
    val candidateIds: List<String>
)

class Candidates : AppCompatActivity() {

    // 1. New: Declare the reusable NotificationManager
    private lateinit var notificationManager: NotificationManager

    // NOTE: Removed old local 'notifications' list and 'isNotificationDropdownVisible'

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidates)

        // 2. New: Initialize the NotificationManager
        notificationManager = NotificationManager(this)

        // Initialize header UI components and set up listeners
        setupUI()

        // 🆕 POPULATE POSITIONS DYNAMICALLY FROM FIRESTORE
        loadPositions()

        // Setup the persistent footer navigation
        setupFooterNavigation()
    }

    // ----------------------------------------------------------------------
    // --- DYNAMIC POSITION POPULATION LOGIC ---
    // ----------------------------------------------------------------------

    private fun loadPositions() {
        // Get current election ID
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                if (electionId != null) {
                    // Get all positions for this election
                    FirestoreCandidateHelper.getPositionsForElection(
                        electionId = electionId,
                        onSuccess = { positions ->
                            val positionContainer: LinearLayout = findViewById(R.id.PositionContainer)
                            positionContainer.removeAllViews()

                            // Convert to PositionItem list
                            val positionList = positions.map { position ->
                                PositionItem(
                                    positionId = position.id,
                                    positionName = position.title,
                                    candidateIds = position.candidates.map { it.id }
                                )
                            }

                            if (positionList.isEmpty()) {
                                showNoPositionsMessage(positionContainer)
                            } else {
                                // Populate positions
                                positionList.forEach { position ->
                                    val positionView = createPositionButtonView(
                                        context = this,
                                        positionId = position.positionId,
                                        positionName = position.positionName,
                                        candidateIds = position.candidateIds
                                    )
                                    positionContainer.addView(positionView)
                                }
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("Candidates", "Error loading positions: $error")
                            val positionContainer: LinearLayout = findViewById(R.id.PositionContainer)
                            positionContainer.removeAllViews()
                            showNoPositionsMessage(positionContainer)
                        }
                    )
                } else {
                    android.util.Log.e("Candidates", "No active election")
                    val positionContainer: LinearLayout = findViewById(R.id.PositionContainer)
                    positionContainer.removeAllViews()
                    showNoPositionsMessage(positionContainer)
                }
            },
            onFailure = { error ->
                android.util.Log.e("Candidates", "Error getting election ID: $error")
                val positionContainer: LinearLayout = findViewById(R.id.PositionContainer)
                positionContainer.removeAllViews()
                showNoPositionsMessage(positionContainer)
            }
        )
    }

    private fun showNoPositionsMessage(container: LinearLayout) {
        val messageView = TextView(this).apply {
            text = "No candidates are available right now."
            setTextColor(Color.parseColor("#555555"))
            textSize = 14f
            setPadding(16.toPx(), 24.toPx(), 16.toPx(), 24.toPx())
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            typeface = ResourcesCompat.getFont(this@Candidates, R.font.poppins_regular)
        }
        container.addView(messageView)
    }

    /**
     * Programmatically creates a single position button (the equivalent of btnPosition).
     */
    private fun createPositionButtonView(
        context: Context,
        positionId: String,
        positionName: String,
        candidateIds: List<String>
    ): View {
        // 1. Main container (btnPosition equivalent - LinearLayout)
        val btnPosition = LinearLayout(context).apply {
            id = View.generateViewId() // Generate a unique ID
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10.toPx() // Spacing between buttons
            }
            orientation = LinearLayout.VERTICAL
            setPadding(12.toPx(), 12.toPx(), 12.toPx(), 12.toPx())
            gravity = android.view.Gravity.CENTER

            // Set the background drawable (blue_rounded_button)
            background = context.resources.getDrawable(R.drawable.blue_rounded_button, null)
            isClickable = true
            isFocusable = true
            // 🔥 IMPLEMENTATION: Load and set the StateListAnimator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.button_press_animator)
            }
            // Note: The system will automatically fall back to standard press feedback on older devices.
        }

        // 2. Position TextView
        val positionText = TextView(context).apply {
            id = R.id.Positions
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = positionName
            setTextColor(Color.WHITE)
            textSize = 14f

            // ⭐️ FIX: Load and set the custom font
            try {
                val typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                setTypeface(typeface)
            } catch (e: Exception) {
                // Fallback to default bold if the font resource is missing
                setTypeface(null, android.graphics.Typeface.BOLD)
                // Log the error if necessary for debugging
            }
        }
        btnPosition.addView(positionText)

        // 3. Click Listener for the Position Button (SIMPLIFIED NAVIGATION)
        btnPosition.setOnClickListener {
            // Navigate directly, relying on the system's default visual press feedback
            val intent = Intent(context, Position::class.java).apply {
                putExtra("POSITION_ID", positionId)
                putExtra("POSITION_NAME", positionName)
                putStringArrayListExtra("CANDIDATE_IDS", ArrayList(candidateIds))
            }
            context.startActivity(intent)
            @Suppress("DEPRECATION")
            (context as? android.app.Activity)?.overridePendingTransition(0, 0)
        }

        return btnPosition
    }


    // ----------------------------------------------------------------------
    // --- HEADER AND COMPARISON BUTTON LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Initializes header elements: Profile Icon, Notification Icon, and Compare button.
     */
    private fun setupUI() {
        // Find the views defined in your XML layout
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val notificationIcon: ImageView = findViewById(R.id.notificationIcon)

        // The commented-out code below refers to XML IDs not present in the provided snippet
        // val compareButton: Button = findViewById(R.id.btnCompare)


        // --- Profile Icon Click Listener ---
        profileIcon.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // 3. New: Notification Icon Click Listener (Toggle Dropdown)
        // Uses the centralized NotificationManager
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    // ----------------------------------------------------------------------
    // --- FOOTER LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Sets up click listeners for all elements in the footer navigation bar.
     */
    private fun setupFooterNavigation() {
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navVote: LinearLayout = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout = findViewById(R.id.nav_results)
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

        navHome.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates.setOnClickListener { /* Already here, do nothing */ }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
        navFaq.setOnClickListener { navigateTo(Faq::class.java) }
    }

    // Utility extension function to convert DP to pixels
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}