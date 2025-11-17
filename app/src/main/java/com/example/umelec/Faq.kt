package com.example.umelec

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat // Required for accessing custom fonts

// --- DATA STRUCTURES ---

// Data class to hold FAQ content and its category
data class FaqItem(
    val category: String, // "General" or "Voting"
    val question: String,
    val answer: String
)

// Category Constants
private const val CATEGORY_GENERAL = "General"
private const val CATEGORY_VOTING = "Voting"


class Faq : AppCompatActivity() {

    // 1. 💡 Initial Declaration (Preserved as per user request)
    private lateinit var notificationManager: NotificationManager

    // ----------------------------------------------------------------------
    // --- LIFECYCLE ---
    // ----------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        // 2. 💡 Instantiation (Preserved as per user request)
        notificationManager = NotificationManager(this)

        // Initialize UI components and set up listeners
        setupUI()

        // Setup the persistent footer navigation
        setupFooterNavigation()

        // POPULATE FAQS DYNAMICALLY FROM FIRESTORE
        loadFaqs()
    }

    // ----------------------------------------------------------------------
    // --- DYNAMIC FAQ POPULATION LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Load FAQ data from Firestore using FirestoreFaqHelper
     */
    private fun loadFaqs() {
        android.util.Log.d("Faq", "Loading FAQs from Firestore...")
        FirestoreFaqHelper.getAllFaqs(
            onSuccess = { faqItems ->
                android.util.Log.d("Faq", "Loaded ${faqItems.size} FAQs from Firestore")
                // Convert FirestoreFaqHelper.FaqItem to local FaqItem
                val faqList = faqItems.map { faqItem ->
                    FaqItem(
                        category = faqItem.category,
                        question = faqItem.question,
                        answer = faqItem.answer
                    )
                }

                // If no FAQs found, use default FAQs as fallback
                val finalFaqList = if (faqList.isEmpty()) {
                    android.util.Log.w("Faq", "No FAQs found, using default FAQs")
                    getDefaultFaqData()
                } else {
                    faqList
                }

                populateFaqs(finalFaqList)
            },
            onFailure = { error ->
                android.util.Log.e("Faq", "Error loading FAQs: $error")
                // Use default FAQs on error
                populateFaqs(getDefaultFaqData())
            }
        )
    }

    /**
     * Default FAQ data as fallback
     */
    private fun getDefaultFaqData(): List<FaqItem> {
        return listOf(
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
            )
        )
    }

    private fun populateFaqs(faqList: List<FaqItem>) {
        val contentContainer: LinearLayout = findViewById(R.id.ContentContainer)

        // Clear existing views (keep the first child if it's a header)
        if (contentContainer.childCount > 0) {
            contentContainer.removeAllViews()
        }

        // Group FAQs by category
        val groupedFaqs = faqList.groupBy { it.category }

        // Add General FAQs first
        groupedFaqs[CATEGORY_GENERAL]?.forEach { faq ->
            val faqView = createFaqItemView(this, faq.question, faq.answer)
            contentContainer.addView(faqView)
        }

        // Add Voting FAQs
        groupedFaqs[CATEGORY_VOTING]?.forEach { faq ->
            val faqView = createFaqItemView(this, faq.question, faq.answer)
            contentContainer.addView(faqView)
        }
    }

    /**
     * Dynamically creates the interactive FAQ item view.
     * Implements the expand/collapse logic using the arrowToggle.
     */
    private fun createFaqItemView(context: Context, question: String, answer: String): View {

        // Fetch custom font once (assuming R.font.montserrat_semi_bold exists)
        val font = ResourcesCompat.getFont(context, R.font.poppins_regular)

        // --- 1. Arrow Toggle (ImageView) ---
        val arrowToggle = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(24.toPx(), 24.toPx())
            setImageResource(R.drawable.selector_arrow_toggle)
            contentDescription = "Toggle visibility"
            isClickable = true
            isFocusable = true
        }

        // --- 2. Question Text ---
        val questionText = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                0, // MATCH_CONSTRAINT
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8.toPx()
            }
            text = question
            textSize = 16f // 18sp size from XML
            setTextColor(Color.parseColor("#313131"))
            setTypeface(font) // Set custom font and bold style
        }

        // --- 3. Question Header Layout (ConstraintLayout) ---
        // This container holds the Question and Arrow
        val questionHeaderLayout = ConstraintLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Set up constraints for questionText
            addView(questionText)
            (questionText.layoutParams as ConstraintLayout.LayoutParams).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                endToStart = arrowToggle.id
            }

            // Set up constraints for arrowToggle
            addView(arrowToggle)
            (arrowToggle.layoutParams as ConstraintLayout.LayoutParams).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        // --- 4. Answer Text (Hidden by Default) ---
        val answerText = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10.toPx()
            }
            text = answer
            textSize = 12f // Slightly smaller 16sp size for body text
            setTextColor(Color.parseColor("#313131"))
            setTypeface(font) // Set custom font and normal style

            // KEY: Answer is hidden by default
            visibility = View.GONE
        }


        // --- 5. Faq Layout (Outer Container) ---
        val faqLayout = LinearLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20.toPx()
            }
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.faq_bg)
            setPadding(16.toPx(), 16.toPx(), 16.toPx(), 16.toPx())

            // Add the Question Header and the Answer
            addView(questionHeaderLayout)
            addView(answerText)
        }

        // --- 6. TOGGLE LOGIC (Interactivity) ---
        // FIX: Attach listener only to the arrowToggle, not the whole header layout.
        arrowToggle.setOnClickListener {
            // Check current visibility state of the answer
            if (answerText.visibility == View.VISIBLE) {
                // Collapse: Hide answer, point arrow down
                answerText.visibility = View.GONE
                arrowToggle.isSelected = false // Switches to down arrow
            } else {
                // Expand: Show answer, point arrow up
                answerText.visibility = View.VISIBLE
                arrowToggle.isSelected = true // Switches to up arrow
            }
        }

        return faqLayout
    }

    // ----------------------------------------------------------------------
    // --- BASE ACTIVITY LOGIC (PRESERVED AS REQUESTED) ---
    // ----------------------------------------------------------------------

    /**
     * Initializes header elements: User Name, Profile Icon, and Notification Icon.
     */
    private fun setupUI() {
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val notificationIcon: ImageView = findViewById(R.id.notificationIcon)

        // --- Profile Icon Click Listener ---
        profileIcon.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        // 3. 💡 Notification Icon Click Listener (PRESERVED)
        // This is where we call the reusable function
        notificationIcon.setOnClickListener {
            notificationManager.toggleNotificationDropdown(it as ImageView)
        }
    }

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
            }
        }

        // FOOTER NAVIGATION LOGIC PRESERVED AS REQUESTED
        navHome.setOnClickListener { navigateTo(Homepage::class.java) }
        navVote.setOnClickListener { navigateTo(Vote::class.java) }
        navCandidates.setOnClickListener { navigateTo(Candidates::class.java) }
        navResults.setOnClickListener { navigateTo(Results::class.java) }
    }

    // Utility extension function to convert DP to pixels
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}