package com.example.umelec

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class Leader_profile : AppCompatActivity() {

    // 1. Declare UI elements that exist in activity_leader_profile.xml
    private lateinit var emailValue: TextView
    private lateinit var collegeValue: TextView
    private lateinit var profileAcronym: TextView
    private lateinit var moduleValue: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnLogout: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_profile)

        // 1. Initialize all the UI elements by finding them by their ID
        initializeViews()

        // 2. Set up the back button and logout button listeners
        setupListeners()

        // 3. Populate the profile data
        populateProfileData()
    }

    private fun initializeViews() {
        // Find the TextViews for profile data that match the XML
        emailValue = findViewById(R.id.EmailValue)
        collegeValue = findViewById(R.id.CollegeValue)

        // Find the new profile TextViews
        profileAcronym = findViewById(R.id.profileAcronym)
        moduleValue = findViewById(R.id.moduleValue)

        // Find the ImageButton
        btnBack = findViewById(R.id.btnBack)

        // Initialize the Logout Button
        btnLogout = findViewById(R.id.btnLogout)

        // Find the Change Password TextView (maps to R.id.RegisterButton)
        val changePasswordButton: TextView = findViewById(R.id.CpassButton)

        // Set listener for Change Password
        changePasswordButton.setOnClickListener {
            startActivity(Intent(this, Changepassword::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun setupListeners() {
        // Set an OnClickListener for the back button
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Set an OnClickListener for the Logout button
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    // ----------------------------------------------------------------------
    // LOGOUT HELPER FUNCTIONS
    // ----------------------------------------------------------------------

    /**
     * Shows a custom success Toast notification.
     */
    private fun showCustomSuccessToast() {
        val inflater = LayoutInflater.from(this)
        // Assuming R.layout.custom_toast_success is available
        val layout = inflater.inflate(R.layout.custom_toast_success, null)

        // Find and customize the views
        val titleText: TextView = layout.findViewById(R.id.toast_title)
        val valueText: TextView = layout.findViewById(R.id.toast_value)
        val actionButton: AppCompatButton = layout.findViewById(R.id.btn_action)

        // Set content and hide button
        titleText.text = "Logged out successfully."
        valueText.text = "Goodbye!"
        actionButton.visibility = View.GONE // Hide the button

        // Create and show the Toast
        with (Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            @Suppress("DEPRECATION")
            view = layout
            show()
        }
    }

    /**
     * Displays the alert dialog for logout confirmation.
     */
    private fun showLogoutConfirmationDialog() {
        // 1. Inflate the custom layout
        // Assuming R.layout.custom_toast_question is available
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
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            showCustomSuccessToast()
            dialog.dismiss()
            performLogout()
        }

        // 5. Show the dialog
        dialog.show()
    }

    /**
     * Handles the actual logout process and navigates to the main activity.
     */
    private fun performLogout() {
        // 1. Sign out from Firebase
        FirebaseAuthHelper.signOut()
        android.util.Log.d("Leader_profile", "User signed out successfully")

        // 2. Navigate back to the main login/landing activity
        val intent = Intent(this, MainActivity::class.java)

        // Add flags to clear the activity stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        // Finish the current Profile activity
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }


    /**
     * Populates the leader profile data from Firestore
     */
    private fun populateProfileData() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        currentUser?.let { user ->
            // Get user email
            val userEmail = user.email ?: ""
            emailValue.text = userEmail

            // Get user data from Firestore
            FirebaseAuthHelper.getUserDataFromFirestore(
                userId = user.uid,
                onSuccess = { userData ->
                    userData?.let { data ->
                        // Set college
                        val college = data["college"] as? String ?: "Not specified"
                        collegeValue.text = college

                        // Set profile acronym (first letters of college or name)
                        val acronym = if (college.isNotEmpty()) {
                            college.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                        } else {
                            val firstName = data["firstname"] as? String ?: ""
                            val lastName = data["lastname"] as? String ?: ""
                            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                                "${firstName.first()}${lastName.first()}"
                            } else {
                                userEmail.take(2).uppercase()
                            }
                        }
                        profileAcronym.text = acronym

                        // Set module/role
                        val role = data["role"] as? String ?: "Leader"
                        moduleValue.text = role
                    } ?: run {
                        // Fallback if no user data
                        emailValue.text = userEmail
                        collegeValue.text = "Not specified"
                        profileAcronym.text = userEmail.take(2).uppercase()
                        moduleValue.text = "Leader"
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("Leader_profile", "Error loading profile: $error")
                    // Fallback on error
                    emailValue.text = userEmail
                    collegeValue.text = "Not specified"
                    profileAcronym.text = userEmail.take(2).uppercase()
                    moduleValue.text = "Leader"
                }
            )
        } ?: run {
            // No user logged in
            emailValue.text = "Not logged in"
            collegeValue.text = "Not specified"
            profileAcronym.text = "NA"
            moduleValue.text = "Leader"
        }
    }
}