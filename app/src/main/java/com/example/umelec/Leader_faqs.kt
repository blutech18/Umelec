package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText


// --- DATA STRUCTURES ---
/**
 * Data class for an individual FAQ item.
 */
data class LeaderFaqItem(
    val categoryTitle: String,
    val question: String,
    val answer: String
)

private const val CATEGORY_GENERAL = "General"
private const val CATEGORY_SETUP = "Setup"
// -----------------------

class Leader_faqs : AppCompatActivity() {

    // 1. Declare the NotificationManager (from Leader_homepage.kt)
    private lateinit var notificationManager: NotificationManager

    // 2. Declare views used in header (from Leader_homepage.kt)
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView

    // 5. Declare the main content container for dynamic FAQs
    private lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_faqs)

        // 3. Initialize Notification Manager (from Leader_homepage.kt)
         notificationManager = NotificationManager(this)

        // 4. Set up all UI and navigation listeners
        initializeViews()
        setupUIListeners()
        setupFooterNavigation()

        // --- NEW: Dynamic Content Setup ---
        populateFaqs()
    }

    // --- VIEW INITIALIZATION (Simplified from Leader_homepage.kt) ---
    private fun initializeViews() {
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)
        // Initialize the content container where dynamic views will be added
        contentContainer = findViewById(R.id.ContentContainer)
    }

    /**
     * Initializes header elements: Profile Icon and Notification Icon (copied from Leader_homepage.kt).
     */
    private fun setupUIListeners() {
        // --- Profile Icon Click Listener (Navigates to Leader_profile) ---
        profileIcon.setOnClickListener {
            val intent = Intent(this, Leader_profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // --- Notification Icon Click Listener (Delegates to NotificationManager) ---
         notificationIcon.setOnClickListener {
         notificationManager.toggleNotificationDropdown(it as ImageView)
         }
    }

    // ----------------------------------------------------------------------
    // --- FOOTER NAVIGATION (copied from Leader_homepage.kt) ---
    // ----------------------------------------------------------------------
    private fun setupFooterNavigation() {
        val navHome: LinearLayout = findViewById(R.id.nav_home)
        val navSetup: LinearLayout = findViewById(R.id.nav_setup)
        val navManage: LinearLayout = findViewById(R.id.nav_manage)
        val navMonitor: LinearLayout = findViewById(R.id.nav_monitor)
        val navFaq: LinearLayout = findViewById(R.id.nav_faq)

        val navigateTo = { activityClass: Class<*> ->
            if (activityClass != this::class.java) {
                val intent = Intent(this, activityClass)
                // Use FLAG_ACTIVITY_REORDER_TO_FRONT for smooth navigation as in Leader_homepage.kt
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        // --- FOOTER NAVIGATION LOGIC (Mimics Leader_homepage.kt's navigation) ---
        // Leader_homepage is the Home screen.
        navHome.setOnClickListener { navigateTo(Leader_homepage::class.java) }
        navSetup.setOnClickListener { navigateTo(Leader_Setup::class.java) }
        navManage.setOnClickListener { navigateTo(Leader_manage_voters::class.java) }
        navMonitor.setOnClickListener { navigateTo(Leader_monitor::class.java) }
        navFaq.setOnClickListener { /* Do nothing, already here */ }
    }

    // ----------------------------------------------------------------------
    // --- DYNAMIC FAQ MANAGEMENT LOGIC ---
    // ----------------------------------------------------------------------

    /**
     * Gets fake FAQ data for demonstration.
     * 💾💻 DATABASE INTEGRATION POINT 💻💾
     */
    private fun getFaqData(): List<LeaderFaqItem> {
        return listOf(
            LeaderFaqItem(
                categoryTitle = CATEGORY_GENERAL,
                question = "What is Umelec and who is this app for?",
                answer = "Umelec is designed for leaders to manage student elections, view results, and monitor the voting process in real-time. It provides a comprehensive dashboard for administration."
            ),
            LeaderFaqItem(
                categoryTitle = CATEGORY_GENERAL,
                question = "How do I ensure security for my voters?",
                answer = "All voter data and ballots are encrypted end-to-end. As a leader, you must ensure that access credentials for the app are kept secure."
            ),
            LeaderFaqItem(
                categoryTitle = CATEGORY_SETUP,
                question = "How do I add a new election?",
                answer = "Navigate to the 'Setup' tab, click 'Add New Election', and follow the prompts to configure dates, positions, and eligible voters."
            ),
            LeaderFaqItem(
                categoryTitle = CATEGORY_SETUP,
                question = "Can I modify an election after it starts?",
                answer = "Critical election parameters cannot be modified once voting begins to ensure integrity. You can, however, update FAQ content or candidate profiles."
            )
        )
    }

    /**
     * Clears existing content, loads all FAQs, groups them by category, and dynamically inflates the views.
     */
    private fun populateFaqs() {
        val btnAddNewCategory = findViewById<LinearLayout>(R.id.btnAddNewCategory)

        if (btnAddNewCategory == null) {
            return
        }

        // FIX 1: Safely detach the button from its current parent *before* clearing the container.
        val parentLayout = btnAddNewCategory.parent
        if (parentLayout is ViewGroup) {
            parentLayout.removeView(btnAddNewCategory)
        }

        // 2. Remove all views (clears old dynamic content)
        contentContainer.removeAllViews()

        val groupedFaqs = getFaqData().groupBy { it.categoryTitle }

        // 3. Inflate each category and its FAQ items
        groupedFaqs.forEach { (categoryTitle, faqItems) ->
            val categoryView = createCategoryView(categoryTitle)

            faqItems.forEach { faq ->
                // FIX 2: Pass the categoryView (parent) to createFaqItemView for correct margin application.
                val faqItemView = createFaqItemView(faq.question, faq.answer, categoryView)
                categoryView.addView(faqItemView)
            }
            contentContainer.addView(categoryView)
        }

        // 4. Re-add the original button object to the ContentContainer as the last item
        contentContainer.addView(btnAddNewCategory)

        // Re-set the listener on the re-added button
        btnAddNewCategory.setOnClickListener { showAddCategoryDialog() }
    }


    // --- 1. Category Management Dialogs & Handlers ---

    /**
     * Shows the bottom sheet dialog for adding a new category.
     */
    private fun showAddCategoryDialog() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_category, null)
        dialog.setContentView(sheetView)

        val inputCategory = sheetView.findViewById<TextInputEditText>(R.id.inputCategory)
        val btnCancel = sheetView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnAdd = sheetView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // Initial state: Add button disabled
        btnAdd.isEnabled = false
        btnAdd.alpha = 0.5f

        // Validation: Enable Add button if input is NOT empty
        inputCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val isInputEmpty = s.isNullOrBlank()
                val shouldEnable = !isInputEmpty
                btnAdd.isEnabled = shouldEnable
                btnAdd.alpha = if (shouldEnable) 1.0f else 0.5f
            }
        })

        // Cancel button click listener
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Add button click listener
        btnAdd.setOnClickListener {
            val categoryName = inputCategory.text.toString().trim()
            if (categoryName.isNotEmpty()) {
                // 1. Inflate the new category view
                val newCategoryView = createCategoryView(categoryName)

                // 2. Add the view to the ContentContainer (before the btnAddNewCategory button)
                val btnAddNewCategory = findViewById<LinearLayout>(R.id.btnAddNewCategory) // Find the bottom button
                val index = contentContainer.indexOfChild(btnAddNewCategory)

                // Add before the button, or at the end if the button is somehow not found
                contentContainer.addView(newCategoryView, if (index != -1) index else contentContainer.childCount)

                // 3. 💾💻 DATABASE INTEGRATION POINT 💻💾
                // TODO: Send 'categoryName' to the backend to create a new FAQ category.
                // ------------------------------------

                dialog.dismiss()
            }
        }
        dialog.show()
    }

    /**
     * Creates and configures a dynamic category view from R.layout.faq_container.
     */
    private fun createCategoryView(categoryName: String): LinearLayout {
        // Inflate R.layout.faq_container
        val inflater = LayoutInflater.from(this)
        val categoryContainer = inflater.inflate(R.layout.faq_container, contentContainer, false) as LinearLayout

        // Set the category title
        val categoryTitleView = categoryContainer.findViewById<TextView>(R.id.CategoryTitle)
        categoryTitleView.text = categoryName

        // Set up the listener for 'Add New FAQ'
        val btnAddNewFaq = categoryContainer.findViewById<LinearLayout>(R.id.btnAddNewFaq)
        btnAddNewFaq.setOnClickListener {
            showAddQuestionDialog(categoryName, categoryContainer)
        }

        // Set up the listener for 'Remove Category'
        val btnRemoveCategory = categoryContainer.findViewById<ImageView>(R.id.btnRemoveCategory)
        btnRemoveCategory.setOnClickListener {
            showConfirmationDialog(
                title = "Remove Category?",
                message = "This action cannot be undone. All associated FAQs will be deleted.",
                actionButtonText = "Remove",
                onConfirm = {
                    // 1. Remove the entire category view from the parent container
                    (categoryContainer.parent as ViewGroup).removeView(categoryContainer)

                    // 2. 💾💻 DATABASE INTEGRATION POINT 💻💾
                    // TODO: Send 'categoryName' (or Category ID) to the backend to remove the category and all associated FAQs.
                    // ------------------------------------
                }
            )
        }

        return categoryContainer
    }


    // --- 2. FAQ Item Management Dialogs & Handlers ---

    /**
     * Shows the bottom sheet dialog for adding a new question/answer to a specific category.
     */
    private fun showAddQuestionDialog(categoryTitle: String, categoryContainer: LinearLayout) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_question, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<TextView>(R.id.sheetTitle).text = categoryTitle // Set title to category name

        val inputQuestion = sheetView.findViewById<TextInputEditText>(R.id.inputQuestion)
        val inputAnswer = sheetView.findViewById<TextInputEditText>(R.id.inputAnswer)
        val btnCancel = sheetView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnAdd = sheetView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // Initial state
        btnAdd.isEnabled = false
        btnAdd.alpha = 0.5f

        // Validation: Enable only if BOTH Question and Answer are NOT empty.
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val isQuestionEmpty = inputQuestion.text.isNullOrBlank()
                val isAnswerEmpty = inputAnswer.text.isNullOrBlank()
                val shouldEnable = !isQuestionEmpty && !isAnswerEmpty

                btnAdd.isEnabled = shouldEnable
                btnAdd.alpha = if (shouldEnable) 1.0f else 0.5f
            }
        }
        inputQuestion.addTextChangedListener(textWatcher)
        inputAnswer.addTextChangedListener(textWatcher)

        // Cancel button
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Add button
        btnAdd.setOnClickListener {
            val questionText = inputQuestion.text.toString().trim()
            val answerText = inputAnswer.text.toString().trim()

            if (questionText.isNotEmpty() && answerText.isNotEmpty()) {
                // 1. Inflate the new FAQ item view
                // FIX 2: Pass the categoryContainer (parent) to createFaqItemView for correct margin application.
                val newFaqItemView = createFaqItemView(questionText, answerText, categoryContainer)

                // 2. Add the FAQ item to the category's container
                val btnAddNewFaq = categoryContainer.findViewById<LinearLayout>(R.id.btnAddNewFaq)
                val index = categoryContainer.indexOfChild(btnAddNewFaq)

                // Add before the button, or at the end if the button is somehow not found
                categoryContainer.addView(newFaqItemView, if (index != -1) index else categoryContainer.childCount)

                // 3. 💾💻 DATABASE INTEGRATION POINT 💻💾
                // TODO: Send 'categoryTitle', 'questionText', and 'answerText' to the backend to create a new FAQ item.
                // ------------------------------------

                dialog.dismiss()
            }
        }
        dialog.show()
    }

    /**
     * Shows the bottom sheet dialog for editing an existing question/answer.
     */
    private fun showEditQuestionDialog(faqItemView: LinearLayout, currentQuestion: String, currentAnswer: String) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_question, null) // Re-use the layout
        dialog.setContentView(sheetView)

        sheetView.findViewById<TextView>(R.id.sheetTitle).text = "Edit Question"

        val inputQuestion = sheetView.findViewById<TextInputEditText>(R.id.inputQuestion)
        val inputAnswer = sheetView.findViewById<TextInputEditText>(R.id.inputAnswer)
        val btnCancel = sheetView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnSave = sheetView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        // Pre-fill content
        inputQuestion.setText(currentQuestion)
        inputAnswer.setText(currentAnswer)
        btnSave.text = "Save" // Change button text

        // Validation (same as Add Question)
        btnSave.isEnabled = true
        btnSave.alpha = 1.0f
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val isQuestionEmpty = inputQuestion.text.isNullOrBlank()
                val isAnswerEmpty = inputAnswer.text.isNullOrBlank()
                val shouldEnable = !isQuestionEmpty && !isAnswerEmpty

                btnSave.isEnabled = shouldEnable
                btnSave.alpha = if (shouldEnable) 1.0f else 0.5f
            }
        }
        inputQuestion.addTextChangedListener(textWatcher)
        inputAnswer.addTextChangedListener(textWatcher)

        // Cancel button
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Save button
        btnSave.setOnClickListener {
            val newQuestion = inputQuestion.text.toString().trim()
            val newAnswer = inputAnswer.text.toString().trim()

            // Find the TextViews inside the FAQ item view
            val questionTextView = faqItemView.findViewById<TextView>(R.id.QuestionTextGeneral)
            val answerTextView = faqItemView.findViewById<TextView>(R.id.AnswerTextGeneral)

            if (newQuestion.isNotEmpty() && newAnswer.isNotEmpty()) {
                // 1. Update the FAQ item view
                questionTextView.text = newQuestion
                answerTextView.text = newAnswer

                // 2. 💾💻 DATABASE INTEGRATION POINT 💻💾
                // TODO: Send the FAQ ID/key, 'newQuestion', and 'newAnswer' to the backend to update the existing FAQ item.
                // ------------------------------------

                dialog.dismiss()
            }
        }
        dialog.show()
    }


    /**
     * Creates and configures a dynamic FAQ item view from R.layout.faq_item.
     * Implements the expand/collapse logic and action buttons (Edit/Remove).
     * @param parent The view group this item will be added to (used to apply margins).
     */
    private fun createFaqItemView(question: String, answer: String, parent: ViewGroup): LinearLayout {
        val inflater = LayoutInflater.from(this)

        // FIX 2 APPLIED: Use the 'parent' and 'attachToRoot=false' to correctly read the margin from faq_item.xml
        val faqLayoutGeneral = inflater.inflate(R.layout.faq_item, parent, false) as LinearLayout

        val questionText = faqLayoutGeneral.findViewById<TextView>(R.id.QuestionTextGeneral)
        val answerText = faqLayoutGeneral.findViewById<TextView>(R.id.AnswerTextGeneral)
        val arrowToggle = faqLayoutGeneral.findViewById<ImageView>(R.id.arrowToggle)
        val questionHeaderLayout = faqLayoutGeneral.findViewById<ConstraintLayout>(R.id.QuestionHeaderLayout)
        val btnFaqEdit = faqLayoutGeneral.findViewById<ImageView>(R.id.btnFaqEdit)
        val btnFaqRemove = faqLayoutGeneral.findViewById<ImageView>(R.id.btnFaqRemove)

        // Set content and initial state
        questionText.text = question
        answerText.text = answer
        answerText.visibility = View.GONE // Hidden by default
        arrowToggle.isSelected = false // Initial state for the selector

        // --- EXPAND/COLLAPSE LOGIC (Mimicking Faq.kt logic) ---
        val toggleAction = {
            val isAnswerVisible = answerText.visibility == View.VISIBLE
            answerText.visibility = if (isAnswerVisible) View.GONE else View.VISIBLE
            // Toggle the state of the arrow (selector_arrow_toggle handles the rotation via state)
            arrowToggle.isSelected = !isAnswerVisible
        }

        questionHeaderLayout.setOnClickListener { toggleAction() }
        arrowToggle.setOnClickListener { toggleAction() }

        // --- EDIT FAQ LOGIC ---
        btnFaqEdit.setOnClickListener {
            showEditQuestionDialog(faqLayoutGeneral, questionText.text.toString(), answerText.text.toString())
        }

        // --- REMOVE FAQ LOGIC ---
        btnFaqRemove.setOnClickListener {
            showConfirmationDialog(
                title = "Remove Question?",
                message = "This action cannot be undone.",
                actionButtonText = "Remove",
                onConfirm = {
                    // 1. Remove the specific FAQ item view from its parent container
                    (faqLayoutGeneral.parent as ViewGroup).removeView(faqLayoutGeneral)

                    // 2. 💾💻 DATABASE INTEGRATION POINT 💻💾
                    // TODO: Send the FAQ ID/key to the backend to remove the specific FAQ item.
                    // ------------------------------------
                }
            )
        }

        return faqLayoutGeneral
    }


    // --- 3. Confirmation Dialog Helper (for Remove actions) ---

    /**
     * Shows a custom confirmation dialog. Assumes R.layout.custom_toast_warning exists.
     */
    private fun showConfirmationDialog(title: String, message: String, actionButtonText: String, onConfirm: () -> Unit) {
        // Assumes R.layout.custom_toast_warning exists based on context snippets
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_toast_warning, null)

        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
        val alertDialog = alertDialogBuilder.create()

        // Configuration
        dialogView.findViewById<TextView>(R.id.toast_title)?.text = title
        dialogView.findViewById<TextView>(R.id.toast_value)?.text = message

        val btnCancel = dialogView.findViewById<AppCompatButton>(R.id.btn_action_primary)
        val btnAction = dialogView.findViewById<AppCompatButton>(R.id.btn_action_secondary)

        btnCancel?.text = "Cancel"
        btnAction?.text = actionButtonText

        // Hide the color strip as per snippet context
        dialogView.findViewById<View>(R.id.color_strip)?.visibility = View.GONE

        // Action listeners
        btnCancel?.setOnClickListener {
            alertDialog.dismiss()
        }

        btnAction?.setOnClickListener {
            onConfirm()
            alertDialog.dismiss()
        }

        // Set transparent background for the custom view to show rounded corners
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }
}