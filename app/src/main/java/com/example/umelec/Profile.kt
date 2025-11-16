package com.example.umelec

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.view.Gravity
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout

class Profile : AppCompatActivity() {

    // Declare all UI elements
    private lateinit var emailValue: TextView
    private lateinit var studentIdValue: TextView
    private lateinit var yearValue: TextView
    private lateinit var collegeValue: TextView
    private lateinit var statusValue: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnLogout: AppCompatButton

    // ⭐️ NEW: Declare TextViews for the new profile fields ⭐️
    private lateinit var profileAcronym: TextView
    private lateinit var moduleValue: TextView
    private lateinit var genderValue: TextView // Using conventional Kotlin camelCase

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
        
        setContentView(R.layout.activity_profile)

        // 1. Initialize all the UI elements by finding them by their ID
        initializeViews()

        // 2. Set up the back button and logout button listeners
        setupListeners()

        // 3. Populate the profile data
        populateProfileData()
    }

    private fun initializeViews() {
        // Find the TextViews for profile data
        emailValue = findViewById(R.id.EmailValue)
        studentIdValue = findViewById(R.id.StudentIDValue)
        yearValue = findViewById(R.id.YearValue)
        collegeValue = findViewById(R.id.CollegeValue)
        statusValue = findViewById(R.id.statusValue)

        // Find the ImageButton
        btnBack = findViewById(R.id.btnBack)

        // ⭐️ Initialize the Logout Button ⭐️
        btnLogout = findViewById(R.id.btnLogout)

        // ⭐️ NEW: Initialize the new TextViews ⭐️
        profileAcronym = findViewById(R.id.profileAcronym)
        moduleValue = findViewById(R.id.moduleValue)
        genderValue = findViewById(R.id.GenderValue) // Maps to R.id.GenderValue
    }

    private fun setupListeners() {
        // Set an OnClickListener for the back button
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // ⭐️ Set an OnClickListener for the Logout button ⭐️
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    // ----------------------------------------------------------------------
// NEW FUNCTION: Custom Success Toast
// ----------------------------------------------------------------------
    // In Profile.kt

    private fun showCustomSuccessToast() {
        val inflater = LayoutInflater.from(this)
        // You should use the root view of the activity (e.g., findViewById<ViewGroup>(android.R.id.content))
        // or just 'null' as the root argument when inflating a standalone layout for a Toast.
        val layout = inflater.inflate(R.layout.custom_toast_success, null)

        // Find and customize the views
        val titleText: TextView = layout.findViewById(R.id.toast_title)
        val valueText: TextView = layout.findViewById(R.id.toast_value)
        val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

        // Set content and hide button as requested
        titleText.text = "Logged out successfully."
        valueText.text = "Goodbye!"
        actionButton.visibility = View.GONE // Hide the button

        // Create and show the Toast
        with (Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT

            // 💡 CHANGE HERE: Use Gravity.BOTTOM and Gravity.CENTER_HORIZONTAL
            // This allows the margins defined in your XML (layout_marginHorizontal="30dp")
            // to control the spacing from the walls.
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)

            // We'll keep the yOffset at 100 to lift it up from the bottom edge.

            view = layout
            show()
        }
    }

    /**
     * Displays the alert dialog for logout confirmation.
     */
    // ----------------------------------------------------------------------
// REVISED DIALOG FUNCTION: Now calls the Toast before logging out
// ----------------------------------------------------------------------
    private fun showLogoutConfirmationDialog() {
        // 1. Inflate the custom layout
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_toast_question, null)

        // 2. Find and customize the views
        val titleText: TextView = customView.findViewById(R.id.toast_title)
        val valueText: TextView = customView.findViewById(R.id.toast_value)
        val btnCancel: AppCompatButton = customView.findViewById(R.id.btn_action_primary)
        val btnConfirm: AppCompatButton = customView.findViewById(R.id.btn_action_secondary)

        // Set content and visibility
        titleText.text = "Are you sure you want to logout?"
        valueText.visibility = View.GONE

        btnCancel.text = "Cancel"
        btnConfirm.text = "Confirm"

        // 3. Create the dialog with the custom view
        val dialog = AlertDialog.Builder(this)
            .setView(customView)
            .setCancelable(true)
            .create()

        // Important: Remove the default dialog background to show the custom background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 4. Set button actions

        // Primary Button: Cancel (Dismiss)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Secondary Button: Confirm (Show Toast, Dismiss Dialog, and Logout)
        btnConfirm.setOnClickListener {
            // Step 1: Show the success toast
            showCustomSuccessToast()

            // Step 2: Dismiss the dialog
            dialog.dismiss()

            // Step 3: Perform the final logout and navigation
            performLogout()
        }

        // 5. Show the dialog
        dialog.show()
    }

    /**
     * Handles the actual logout process.
     */
    private fun performLogout() {
        // 1. Sign out from Firebase
        FirebaseAuthHelper.signOut()
        
        // 2. Clear temporary credentials if any
        FirebaseAuthHelper.clearTemporaryCredentials(this)

        // 3. Navigate back to the main login/landing activity
        val intent = Intent(this, MainActivity::class.java)

        // Add flags to clear the activity stack so the user cannot press 'back'
        // and return to the logged-in profile screens.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        // Finish the current Profile activity
        finish()
        overridePendingTransition(0, 0)
    }


    /**
     * This function fetches and displays user profile data from Firestore.
     */
    fun populateProfileData() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            // User not logged in, redirect to login
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Set email from Firebase Auth
        emailValue.text = currentUser.email ?: ""

        // Get user data from Firestore
        FirebaseAuthHelper.getUserDataFromFirestore(
            userId = currentUser.uid,
            onSuccess = { userData ->
                if (userData != null) {
                    // Set the text of each TextView using the retrieved data
                    studentIdValue.text = userData["studentId"] as? String ?: ""
                    yearValue.text = userData["year"] as? String ?: ""
                    collegeValue.text = userData["college"] as? String ?: ""
                    statusValue.text = userData["status"] as? String ?: "Ineligible"
                    
                    // ⭐️ NEW: Set the text for the new TextViews ⭐️
                    val firstname = userData["firstname"] as? String ?: ""
                    val lastname = userData["lastname"] as? String ?: ""
                    profileAcronym.text = "${firstname.take(1)}${lastname.take(1)}".uppercase()
                    moduleValue.text = userData["moduleValue"] as? String ?: "Voter"
                    genderValue.text = userData["gender"] as? String ?: ""
                } else {
                    // No user data in Firestore, use defaults
                    studentIdValue.text = ""
                    yearValue.text = ""
                    collegeValue.text = ""
                    statusValue.text = "Ineligible"
                    profileAcronym.text = currentUser.email?.substring(0, 2)?.uppercase() ?: "UN"
                    moduleValue.text = "Voter"
                    genderValue.text = ""
                }
            },
            onFailure = { errorMessage ->
                // Error loading data, use defaults
                studentIdValue.text = ""
                yearValue.text = ""
                collegeValue.text = ""
                statusValue.text = "Ineligible"
                profileAcronym.text = currentUser.email?.substring(0, 2)?.uppercase() ?: "UN"
                moduleValue.text = "Voter"
                genderValue.text = ""
            }
        )
    }
}