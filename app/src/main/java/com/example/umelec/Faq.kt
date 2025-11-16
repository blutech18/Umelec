package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout




// --- DATA STRUCTURES ---

/**
 * Data class to hold FAQ content and its category
 */
data class FaqItem(
    val category: String, // "General" or "Voting" (or any other dynamic category)
    val question: String,
    val answer: String
)

// Category Constants (used for grouping)
private const val CATEGORY_GENERAL = "General"
private const val CATEGORY_VOTING = "Voting"


class Faq : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager

    // --- LIFECYCLE ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        notificationManager = NotificationManager(this)

        setupUI()
        setupFooterNavigation()

        populateFaqs()
    }

    // ----------------------------------------------------------------------
    // --- CORE LOGIC (REQUIRED TO KEEP) ---
    // ----------------------------------------------------------------------

    /**
     * Initializes header elements: Profile Icon and Notification Icon.
     * (Preserved Profile/Notification logic)
     */
    private fun setupUI() {
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val notificationIcon: ImageView = findViewById(R.id.notificationIcon)

        // --- Profile Icon Click Listener ---
        profileIcon.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // --- Notification Icon Click Listener ---
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

    /**
     * Sets up click listeners for all elements in the footer navigation bar.
     * (Preserved Footer logic)
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
                overridePendingTransition(0, 0)
            }
        }

        navHome.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
        navFaq.setOnClickListener { /* Do nothing, already here */ }
    }

    // ----------------------------------------------------------------------
    // --- DYNAMIC FAQ POPULATION AND DATABASE ACCESS GUIDES ---
    // ----------------------------------------------------------------------

    /**
     * Guides the backend/database implementation.
     *
     * 🚨 BACKEND/DATABASE INTEGRATION POINT 🚨
     * Replace this hardcoded list with your data retrieval logic:
     * E.g., return faqRepository.getAllFaqs()
     */
    private fun getFaqData(): List<FaqItem> {
        return listOf(
            // General FAQs
            FaqItem(
                category = CATEGORY_GENERAL,
                question = "What is Umelec and is it secure?",
                answer = "Umelec is a secure, modern, and transparent mobile application designed to facilitate student elections. All voting data is encrypted to ensure integrity."
            ),
            FaqItem(
                category = CATEGORY_GENERAL,
                question = "Which devices support the app?",
                answer = "Umelec is compatible with all devices running Android 7.0 (Nougat) or newer. Please ensure your operating system is up-to-date for the best experience."
            ),
            // Voting Process FAQs
            FaqItem(
                category = CATEGORY_VOTING,
                question = "How do I cast my vote?",
                answer = "You can cast your vote by navigating to the 'Vote' tab, selecting the candidates you prefer for each position, and submitting your ballot before the deadline."
            ),
            FaqItem(
                category = CATEGORY_VOTING,
                question = "Can I change my vote after submitting?",
                answer = "No, once your vote is submitted, it is final and cannot be altered or withdrawn. Please review your choices carefully before confirming."
            ),
            FaqItem(
                category = CATEGORY_VOTING,
                question = "Where can I see the election results?",
                answer = "Live tallies and final results will be posted in the 'Results' section once the voting period has officially closed."
            ),
        )
    }

    /**
     * Main function to clear existing content and dynamically populate all FAQs
     * grouped by their category.
     */
    private fun populateFaqs() {
        val contentContainer: LinearLayout = findViewById(R.id.ContentContainer)

        // 1. Clear any existing static content inside the main container
        contentContainer.removeAllViews()

        // 2. Group all FAQ items by their category title
        val groupedFaqs = getFaqData().groupBy { it.category }

        // 3. Dynamically inflate and add categories and their items
        groupedFaqs.forEach { (categoryTitle, faqItems) ->
            // Create the container for the current category
            val categoryView = createCategoryView(categoryTitle, contentContainer)

            // Add all FAQ items to the created category container
            faqItems.forEach { faq ->
                val faqItemView = createFaqItemView(faq.question, faq.answer, categoryView)
                categoryView.addView(faqItemView)
            }

            // Add the fully populated category view to the main content container
            contentContainer.addView(categoryView)
        }
    }

    /**
     * Creates and configures a dynamic category view using R.layout.faq_container_voter.
     */
    private fun createCategoryView(categoryName: String, parent: ViewGroup): LinearLayout {
        val inflater = LayoutInflater.from(this)

        // Inflate faq_container_voter.xml. Pass the parent and attach=false to ensure
        // layout params (margins) for the category container are respected.
        val categoryContainer = inflater.inflate(R.layout.faq_container_voter, parent, false) as LinearLayout

        // Find and set the category title
        val categoryTitleView = categoryContainer.findViewById<TextView>(R.id.CategoryTitle)
        categoryTitleView.text = categoryName

        return categoryContainer
    }

    /**
     * Creates and configures a dynamic FAQ item view using R.layout.faq_item_voter.
     * Implements the expand/collapse logic using the arrowToggle.
     *
     * @param parent The category container where this FAQ item will be placed (used to apply margins).
     */
    private fun createFaqItemView(question: String, answer: String, parent: ViewGroup): LinearLayout {
        val inflater = LayoutInflater.from(this)

        // FIX: Pass the 'parent' and set 'attachToRoot=false'. This is crucial
        // for the LinearLayout.LayoutParams (like layout_marginBottom="20dp")
        // defined in faq_item_voter.xml to be correctly read and applied.
        val faqLayoutGeneral = inflater.inflate(R.layout.faq_item_voter, parent, false) as LinearLayout

        val questionText = faqLayoutGeneral.findViewById<TextView>(R.id.QuestionTextGeneral)
        val answerText = faqLayoutGeneral.findViewById<TextView>(R.id.AnswerTextGeneral)
        val arrowToggle = faqLayoutGeneral.findViewById<ImageView>(R.id.arrowToggle)
        val questionHeaderLayout = faqLayoutGeneral.findViewById<ConstraintLayout>(R.id.QuestionHeaderLayout)

        // Set content and initial state
        questionText.text = question
        answerText.text = answer
        answerText.visibility = View.GONE // Hidden by default
        arrowToggle.isSelected = false // Initial state for the selector

        // --- ARROW TOGGLE LOGIC (REQUIRED TO KEEP) ---
        // Listener for the arrow toggle (or the whole header)
        val toggleAction = {
            val isAnswerVisible = answerText.visibility == View.VISIBLE
            answerText.visibility = if (isAnswerVisible) View.GONE else View.VISIBLE
            // Toggle the state of the arrow (selector_arrow_toggle handles the rotation/image change)
            arrowToggle.isSelected = !isAnswerVisible
        }

        // Apply the toggle action to the whole header for better touch target (as per activity_faq.xml)
        questionHeaderLayout.setOnClickListener { toggleAction() }
        // Also apply it to the arrow icon itself
        arrowToggle.setOnClickListener { toggleAction() }

        return faqLayoutGeneral
    }

    // Utility extension function to convert DP to pixels (REQUIRED TO KEEP)
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}