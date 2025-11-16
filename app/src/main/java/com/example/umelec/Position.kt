package com.example.umelec

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

// ----------------------------------------------------------------------
// DATA CLASS FOR CANDIDATES
// ----------------------------------------------------------------------

data class CandidateItem(
    val candidateId: String,
    val name: String,
    val position: String,
    val courseInfo: String,
    val profilePictureResource: Int
)

class Position : AppCompatActivity() {

    private lateinit var allCandidates: List<CandidateItem>
    private var currentPosition: String = "Position"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_position)

        currentPosition = intent.getStringExtra("POSITION_NAME") ?: "Candidates"

        // 1. Fetch data for this position
        allCandidates = getCandidatesForPosition(currentPosition)

        // 2. Set header title
        setupHeaderTitle()

        // 3. Back button
        setupBackNavigation()

        // 4. Populate all candidates
        populateCandidates()

        // 5. Compare button logic
        setupCompareButton()

        // 6. Footer navigation
        setupFooterNavigation()
    }

    // ----------------------------------------------------------------------
    // POPULATE CANDIDATE CARDS (DYNAMIC)
    // ----------------------------------------------------------------------

    private fun populateCandidates() {
        val registerContainer: LinearLayout? = findViewById(R.id.registerContainer)
        if (registerContainer == null) return

        // Save CompareLayout temporarily
        val compareLayout: LinearLayout? = registerContainer.findViewById(R.id.CompareLayout)
        if (compareLayout != null) {
            (compareLayout.parent as? LinearLayout)?.removeView(compareLayout)
        }

        // Clear old cards
        registerContainer.removeAllViews()

        // Add candidate cards
        allCandidates.forEach { candidate ->
            val cardView = createCandidateCardView(candidate, registerContainer)
            registerContainer.addView(cardView)
        }

        // Add CompareLayout back
        if (compareLayout != null) {
            registerContainer.addView(compareLayout)
        }
    }

    // ----------------------------------------------------------------------
    // COMPARE BUTTON
    // ----------------------------------------------------------------------

    private fun setupCompareButton() {
        val compareButton: AppCompatButton? = findViewById(R.id.btnCompare)
        compareButton?.setOnClickListener {
            showCompareBottomSheet()
        }
    }

    // ----------------------------------------------------------------------
    // SAMPLE DATA (REPLACE WITH BACKEND LATER)
    // ----------------------------------------------------------------------

    private fun getCandidatesForPosition(positionName: String): List<CandidateItem> {
        val defaultPic = R.drawable.ic_launcher_background

        return listOf(
            CandidateItem("JANE_D", "Jane Doe", positionName, "III - CCIS", defaultPic),
            CandidateItem("JOHN_S", "John Smith", positionName, "IV - CCIS", defaultPic),
            CandidateItem("SARAH_L", "Sarah Lee", positionName, "II - CCIS", defaultPic),
            CandidateItem("MARK_T", "Mark Tan", positionName, "I - CCIS", defaultPic)
        )
    }

    // ----------------------------------------------------------------------
    // BOTTOM SHEET FOR COMPARISON
    // ----------------------------------------------------------------------

    private fun showCompareBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_compare, null)

        dialog.setContentView(view)

        val selectedCandidates = mutableListOf<CandidateItem>()
        val selectedViews = mutableListOf<View>()

        val compareButton: AppCompatButton = view.findViewById(R.id.btnCompareInSheet)
        val container: LinearLayout = view.findViewById(R.id.candidateSelectionContainer)
        val title: TextView = view.findViewById(R.id.sheetTitle)

        title.text = "Select candidates to compare"
        compareButton.isEnabled = false

        allCandidates.forEach { candidate ->
            val itemView = createCompareCandidateItem(candidate)
            container.addView(itemView)

            itemView.setOnClickListener { v ->
                val alreadySelected = selectedCandidates.contains(candidate)

                if (alreadySelected) {
                    selectedCandidates.remove(candidate)
                    selectedViews.remove(v)
                    v.background = ContextCompat.getDrawable(this, R.drawable.compare_candidate_unselected_bg)
                } else if (selectedCandidates.size < 2) {
                    selectedCandidates.add(candidate)
                    selectedViews.add(v)
                    v.background = ContextCompat.getDrawable(this, R.drawable.rounded_yellow_gradient_bg)
                } else {
                    Toast.makeText(this, "You can only select up to two candidates.", Toast.LENGTH_SHORT).show()
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
                dialog.dismiss()
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        dialog.show()
    }

    private fun createCompareCandidateItem(candidate: CandidateItem): View {
        val view = LayoutInflater.from(this).inflate(R.layout.compare_candidate_item, null)

        val nameText: TextView? = view.findViewById(R.id.tv_name)
        val profilePic: ImageView? = view.findViewById(R.id.iv_profile_picture)

        nameText?.text = candidate.name
        profilePic?.setImageResource(candidate.profilePictureResource)

        view.background = ContextCompat.getDrawable(this, R.drawable.compare_candidate_unselected_bg)

        (view.layoutParams as? LinearLayout.LayoutParams)?.apply {
            bottomMargin = 8.toPx()
            view.layoutParams = this
        }

        return view
    }

    // ----------------------------------------------------------------------
    // HEADER + BACK BUTTON
    // ----------------------------------------------------------------------

    private fun setupHeaderTitle() {
        val nameTitle: TextView? = findViewById(R.id.NameTitle)
        nameTitle?.text = currentPosition
    }

    private fun setupBackNavigation() {
        val backButton: ImageButton? = findViewById(R.id.btnBack)
        backButton?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    // ----------------------------------------------------------------------
    // INDIVIDUAL CANDIDATE CARD IN MAIN LIST
    // ----------------------------------------------------------------------

    private fun createCandidateCardView(candidate: CandidateItem, root: LinearLayout?): View {
        val card = LayoutInflater.from(this).inflate(R.layout.candidate_card_item, root, false)

        val profilePic: ImageView? = card.findViewById(R.id.iv_profile_picture)
        val nameText: TextView? = card.findViewById(R.id.tv_name)
        val positionText: TextView? = card.findViewById(R.id.tv_position)
        val courseText: TextView? = card.findViewById(R.id.tv_course_info)
        val viewAllContainer: LinearLayout? = card.findViewById(R.id.btnViewAllContainer)

        nameText?.text = candidate.name
        positionText?.text = candidate.position
        courseText?.text = candidate.courseInfo
        profilePic?.setImageResource(candidate.profilePictureResource)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                card.stateListAnimator =
                    AnimatorInflater.loadStateListAnimator(this, R.animator.button_press_animator)
            } catch (_: Exception) {}
        }

        viewAllContainer?.setOnClickListener {
            val intent = Intent(this, Platform::class.java)
            intent.putExtra("CANDIDATE_ID", candidate.candidateId)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        return card
    }

    // ----------------------------------------------------------------------
    // FOOTER NAVIGATION
    // ----------------------------------------------------------------------

    private fun setupFooterNavigation() {
        val navHome: LinearLayout? = findViewById(R.id.nav_home)
        val navVote: LinearLayout? = findViewById(R.id.nav_vote)
        val navCandidates: LinearLayout? = findViewById(R.id.nav_candidates)
        val navResults: LinearLayout? = findViewById(R.id.nav_results)
        val navFaq: LinearLayout? = findViewById(R.id.nav_faq)

        val go = { cls: Class<*> ->
            val intent = Intent(this, cls)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        navHome?.setOnClickListener { go(Homepage::class.java) }
        navVote?.setOnClickListener { go(Vote::class.java) }
        navCandidates?.setOnClickListener { go(Candidates::class.java) }
        navResults?.setOnClickListener { go(Results::class.java) }
        navFaq?.setOnClickListener { go(Faq::class.java) }
    }

    // ----------------------------------------------------------------------
    // MISC UTILITIES
    // ----------------------------------------------------------------------

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}
