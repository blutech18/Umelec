package com.example.umelec

import android.animation.AnimatorInflater // 🔥 NEW: Import for AnimatorInflater
import android.content.Intent
import android.os.Build // 🔥 NEW: Import for Build class
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

// --- DATA STRUCTURE FOR CANDIDATES ---
data class CandidateItem(
    val candidateId: String,
    val name: String,
    val position: String,
    val courseInfo: String,
    // val previewText: String, // COMMENTED OUT: Removed the preview text field
    val profilePictureResource: Int // Use R.drawable.your_image
)

class Position : AppCompatActivity() {

    private lateinit var allCandidates: List<CandidateItem>
    private var currentPosition: String = "Position"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_position)

        currentPosition = intent.getStringExtra("POSITION_NAME") ?: "Candidates"

        // 2. Setup the header title to display the position
        setupHeaderTitle()

        // 3. Setup the back button functionality
        setupBackNavigation()

        // 5. Setup the compare button logic (FIXED: Removed custom animator)
        setupCompareButton()

        // 6. Setup the persistent footer navigation
        setupFooterNavigation()

        // 1. Fetch the data from Firestore
        loadCandidatesForPosition(currentPosition)
    }

    // ----------------------------------------------------------------------
    // --- DYNAMIC CARD POPULATION LOGIC (FIXED) ---
    // ----------------------------------------------------------------------

    /**
     * Fetches candidate data and dynamically creates cards, ensuring static elements are preserved.
     */
    private fun populateCandidates() {
        val registerContainer: LinearLayout? = findViewById(R.id.registerContainer)

        // Exit if the container cannot be found (crash-proof)
        if (registerContainer == null) return

        // 1. Find and temporarily detach the static CompareLayout before clearing
        val compareLayout: LinearLayout? = registerContainer.findViewById(R.id.CompareLayout)

        // Safely detach the CompareLayout from the registerContainer
        if (compareLayout != null) {
            (compareLayout.parent as? LinearLayout)?.removeView(compareLayout)
        }

        // 2. Clear out the dynamic content and any static card templates (like cardContainer)
        registerContainer.removeAllViews()

        // 3. Add all dynamic candidate cards
        allCandidates.forEach { candidate ->
            val cardView = createCandidateCardView(candidate, registerContainer)
            registerContainer.addView(cardView)
        }

        // 4. Re-add the CompareLayout at the bottom
        if (compareLayout != null) {
            registerContainer.addView(compareLayout)
        }
    }


    // ----------------------------------------------------------------------
    // --- COMPARE BUTTON LOGIC (REVISED) ---
    // ----------------------------------------------------------------------

    /**
     * Finds the Compare button and sets its click listener to show the bottom sheet.
     */
    private fun setupCompareButton() {
        // Use AppCompatButton? and safe call ?. to prevent crashes if btnCompare is missing
        val compareButton: AppCompatButton? = findViewById(R.id.btnCompare)

        // Only proceed if the button exists
        compareButton?.setOnClickListener {
            // REVISION: Removed the call to animateClickFeedback, now calls action directly.
            showCompareBottomSheet()
        }
    }

    // ----------------------------------------------------------------------
    // --- DATA FETCHING FROM FIRESTORE ---
    // ----------------------------------------------------------------------

    private fun loadCandidatesForPosition(positionName: String) {
        // First get current election ID
        FirestoreElectionHelper.getCurrentElectionId(
            onSuccess = { electionId ->
                if (electionId != null) {
                    // Find position ID by matching position name
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("candidates")
                        .whereEqualTo("electionId", electionId)
                        .whereEqualTo("isActive", true)
                        .get()
                        .addOnSuccessListener { documents ->
                            // Find all unique positions and match by name
                            val positionsMap = mutableMapOf<String, String>() // positionName -> positionId
                            documents.documents.forEach { doc ->
                                val data = doc.data ?: return@forEach
                                val posName = data["positionName"] as? String ?: return@forEach
                                val posId = data["positionId"] as? String ?: return@forEach
                                positionsMap[posName] = posId
                            }

                            val positionId = positionsMap[positionName]
                            if (positionId != null) {
                                // Get candidates for this position
                                FirestoreCandidateHelper.getCandidatesForPosition(
                                    electionId = electionId,
                                    positionId = positionId,
                                    onSuccess = { candidates ->
                                        // Convert to CandidateItem list with full details
                                        val candidateItems = mutableListOf<CandidateItem>()
                                        var loadedCount = 0

                                        if (candidates.isEmpty()) {
                                            allCandidates = emptyList()
                                            populateCandidates()
                                            return@getCandidatesForPosition
                                        }

                                        candidates.forEach { candidate ->
                                            // Get full candidate details including courseInfo
                                            FirestoreCandidateHelper.getCandidateDetails(
                                                candidateId = candidate.id,
                                                onSuccess = { details ->
                                                    candidateItems.add(
                                                        CandidateItem(
                                                            candidateId = details["candidateId"] ?: candidate.id,
                                                            name = details["name"] ?: candidate.name,
                                                            position = details["positionName"] ?: positionName,
                                                            courseInfo = details["courseInfo"] ?: "",
                                                            profilePictureResource = R.drawable.ic_profile
                                                        )
                                                    )
                                                    loadedCount++
                                                    if (loadedCount == candidates.size) {
                                                        allCandidates = candidateItems.sortedBy { it.name }
                                                        populateCandidates()
                                                    }
                                                },
                                                onFailure = { error ->
                                                    android.util.Log.e("Position", "Error loading candidate details for ${candidate.id}: $error")
                                                    // Add with minimal data
                                                    candidateItems.add(
                                                        CandidateItem(
                                                            candidateId = candidate.id,
                                                            name = candidate.name,
                                                            position = positionName,
                                                            courseInfo = "",
                                                            profilePictureResource = R.drawable.ic_profile
                                                        )
                                                    )
                                                    loadedCount++
                                                    if (loadedCount == candidates.size) {
                                                        allCandidates = candidateItems.sortedBy { it.name }
                                                        populateCandidates()
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onFailure = { error ->
                                        android.util.Log.e("Position", "Error loading candidates: $error")
                                        allCandidates = emptyList()
                                        populateCandidates()
                                    }
                                )
                            } else {
                                android.util.Log.e("Position", "Position not found: $positionName")
                                allCandidates = emptyList()
                                populateCandidates()
                            }
                        }
                        .addOnFailureListener { error ->
                            android.util.Log.e("Position", "Error finding position: $error")
                            allCandidates = emptyList()
                            populateCandidates()
                        }
                } else {
                    android.util.Log.e("Position", "No active election")
                    allCandidates = emptyList()
                    populateCandidates()
                }
            },
            onFailure = { error ->
                android.util.Log.e("Position", "Error getting election ID: $error")
                allCandidates = emptyList()
                populateCandidates()
            }
        )
    }

    // ----------------------------------------------------------------------
    // --- BOTTOM SHEET LOGIC ---
    // ----------------------------------------------------------------------

    private fun showCompareBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_compare, null)
        bottomSheetDialog.setContentView(view)

        val selectedCandidates = mutableListOf<CandidateItem>()
        val selectedViews = mutableListOf<View>()

        val compareButton: AppCompatButton = view.findViewById(R.id.btnCompareInSheet)
        val container: LinearLayout = view.findViewById(R.id.candidateSelectionContainer)
        val titleTextView: TextView = view.findViewById(R.id.sheetTitle)

        titleTextView.text = "Select candidates to compare"
        compareButton.isEnabled = false

        allCandidates.forEach { candidate ->
            val candidateView = createCompareCandidateItem(candidate)
            container.addView(candidateView)

            candidateView.setOnClickListener { v ->
                val isSelected = selectedCandidates.contains(candidate)

                if (isSelected) {
                    selectedCandidates.remove(candidate)
                    selectedViews.remove(v)
                    v.background = ContextCompat.getDrawable(this, R.drawable.compare_candidate_unselected_bg)
                } else if (selectedCandidates.size < 2) {
                    selectedCandidates.add(candidate)
                    selectedViews.add(v)
                    v.background = ContextCompat.getDrawable(this, R.drawable.rounded_yellow_gradient_bg)
                } else {
                    Toast.makeText(this, "You can only select a maximum of two candidates.", Toast.LENGTH_SHORT).show()
                }

                compareButton.isEnabled = selectedCandidates.size == 2
            }
        }

        compareButton.setOnClickListener {
            if (selectedCandidates.size == 2) {
                val intent = Intent(this, Comparison::class.java).apply {
                    putExtra("CANDIDATE_ID_1", selectedCandidates[0].candidateId)
                    putExtra("CANDIDATE_ID_2", selectedCandidates[1].candidateId)
                }
                bottomSheetDialog.dismiss()
                startActivity(intent)
            }
        }

        bottomSheetDialog.show()
    }

    private fun createCompareCandidateItem(candidate: CandidateItem): View {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.compare_candidate_item, null)

        val nameTextView: TextView? = view.findViewById(R.id.tv_name)
        // val positionTextView: TextView? = view.findViewById(R.id.tv_position) // COMMENTED OUT: Removed position text view logic for comparison item
        val profileImageView: ImageView? = view.findViewById(R.id.iv_profile_picture)

        nameTextView?.text = candidate.name
        // positionTextView?.text = candidate.position // COMMENTED OUT: Removed position text assignment for comparison item
        profileImageView?.setImageResource(candidate.profilePictureResource)

        view.background = ContextCompat.getDrawable(this, R.drawable.compare_candidate_unselected_bg)

        (view.layoutParams as? LinearLayout.LayoutParams)?.let {
            it.bottomMargin = 8.toPx()
            view.layoutParams = it
        }

        return view
    }

    // ----------------------------------------------------------------------
    // --- REMAINING HELPER FUNCTIONS ---
    // ----------------------------------------------------------------------

    private fun setupHeaderTitle() {
        val nameTitle: TextView? = findViewById(R.id.NameTitle)
        nameTitle?.text = currentPosition
    }

    private fun setupBackNavigation() {
        val backButton: ImageButton? = findViewById(R.id.btnBack)
        backButton?.setOnClickListener {
            finish()
        }
    }

    private fun createCandidateCardView(candidate: CandidateItem, root: LinearLayout?): View {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.candidate_card_item, root, false) // Assumes candidate_card_item.xml exists

        val profilePic: ImageView? = cardView.findViewById(R.id.iv_profile_picture)
        val nameText: TextView? = cardView.findViewById(R.id.tv_name)
        val positionText: TextView? = cardView.findViewById(R.id.tv_position)
        val courseText: TextView? = cardView.findViewById(R.id.tv_course_info)
        // val previewText: TextView? = cardView.findViewById(R.id.tv_preview_text) // COMMENTED OUT: Removed TextView find
        val viewAllLinkContainer: LinearLayout? = cardView.findViewById(R.id.btnViewAllContainer)

        nameText?.text = candidate.name
        positionText?.text = candidate.position
        courseText?.text = candidate.courseInfo
        // previewText?.text = candidate.previewText // COMMENTED OUT: Removed text assignment
        profilePic?.setImageResource(candidate.profilePictureResource)

        // 🔥 NEW: Apply the StateListAnimator to the card view for press feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // This applies the same press animation as the position buttons
                cardView.stateListAnimator = AnimatorInflater.loadStateListAnimator(this, R.animator.button_press_animator)
            } catch (e: Exception) {
                // Log error if the animator resource is missing (e.g., R.animator.button_press_animator)
                // Log.e("Position", "Could not load StateListAnimator for card: ${e.message}")
            }
        }


        viewAllLinkContainer?.setOnClickListener {
            // No animation needed for a text link, just navigate immediately
            val intent = Intent(this, Platform::class.java).apply {
                putExtra("CANDIDATE_ID", candidate.candidateId)
            }
            startActivity(intent)
        }

        return cardView
    }

    private fun setupFooterNavigation() {
        val navHome: LinearLayout? = findViewById(R.id.nav_home)
        val navVote: LinearLayout? = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout? = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout? = findViewById(R.id.nav_results)
        val navFaq: LinearLayout? = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            val intent = Intent(this, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote?.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates?.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults?.setOnClickListener { navigateTo(Results::class.java) }
        navFaq?.setOnClickListener { navigateTo(Faq::class.java) }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    // REMOVED: The animateClickFeedback function has been removed in the previous step.
}
