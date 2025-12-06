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
import androidx.core.content.res.ResourcesCompat
import android.view.LayoutInflater // Import required for XML inflation
import android.view.ViewGroup // Import required for createCategoryView/createFaqItemView

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

     /*** Load FAQ data from Firestore using FirestoreFaqHelper */
     private fun loadFaqs() {
          android.util.Log.d("Faq", "Loading FAQs from Firestore...")
          // NOTE: Assuming FirestoreFaqHelper and its methods are correctly defined elsewhere.
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

    /**
     * Main function to clear existing content and dynamically populate all FAQs
     * grouped by their category. (RESTORED ORIGINAL LOGIC)
     */
    private fun populateFaqs(faqList: List<FaqItem>) {
        val contentContainer: LinearLayout = findViewById(R.id.ContentContainer)

        // 1. Clear any existing content inside the main container
        contentContainer.removeAllViews()

        // 2. Group all FAQ items by their category title
        val groupedFaqs = faqList.groupBy { it.category }

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
     * (RESTORED ORIGINAL FUNCTION)
     */
    private fun createCategoryView(categoryName: String, parent: ViewGroup): LinearLayout {
        val inflater = LayoutInflater.from(this)

        // Assuming R.layout.faq_container_voter exists and contains R.id.CategoryTitle
        val categoryContainer = inflater.inflate(R.layout.faq_container_voter, parent, false) as LinearLayout

        // Find and set the category title
        val categoryTitleView = categoryContainer.findViewById<TextView>(R.id.CategoryTitle)
        // Adjust the category display name if needed (e.g., to "General / System")
        categoryTitleView.text = when (categoryName) {
            CATEGORY_GENERAL -> "General / System"
            CATEGORY_VOTING -> "Voting Process"
            else -> categoryName
        }

        return categoryContainer
    }

    /**
     * Creates and configures a dynamic FAQ item view using R.layout.faq_item_voter.
     * Implements the expand/collapse logic using the arrowToggle.
     * (RESTORED ORIGINAL FUNCTION)
     */
    private fun createFaqItemView(question: String, answer: String, parent: ViewGroup): LinearLayout {
        val inflater = LayoutInflater.from(this)

        // Assuming R.layout.faq_item_voter exists and contains R.id.QuestionTextGeneral, etc.
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

        // Apply the toggle action to the whole header for better touch target
        questionHeaderLayout.setOnClickListener { toggleAction() }
        // Also apply it to the arrow icon itself
        arrowToggle.setOnClickListener { toggleAction() }

        return faqLayoutGeneral
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
          navFaq.setOnClickListener { /* Do nothing, already here */ } // Added back for completeness
         }

     // Utility extension function to convert DP to pixels
     private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}