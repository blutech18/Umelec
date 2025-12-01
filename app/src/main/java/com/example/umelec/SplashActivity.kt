package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView // Import for TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    // --- FRONTEND / PROGRESS ANIMATION VARIABLES ---
    private lateinit var progressPercentTextView: TextView
    private val SPLASH_DURATION_MS: Long = 2000 // Total time for the animation
    private val PROGRESS_UPDATE_INTERVAL_MS: Long = 20 // How often to update the screen (20ms = 50 FPS)
    private val handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0

    // Runnable to update the percentage text
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = ((elapsed.toFloat() / SPLASH_DURATION_MS.toFloat()) * 100).toInt()

            if (progress < 100) {
                // Update the text view with the current percentage
                progressPercentTextView.text = "$progress%"
                // Schedule the next update
                handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            } else {
                // Ensure it maxes out at 100%
                progressPercentTextView.text = "100%"
            }
        }
    }
    // -----------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // --- FRONTEND: Initialize and Start Progress Animation ---
        // You MUST have a TextView with the ID 'progressPercent' in your XML for this to work
        progressPercentTextView = findViewById(R.id.progressPercent)
        startTime = System.currentTimeMillis()
        handler.post(updateProgressRunnable)
        // --------------------------------------------------------

        // Delay for a few seconds then check authentication state
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            // --- BACKEND LOGIC: (ORIGINAL CODE - DO NOT TOUCH) ---

            // Check if user is logged in
            if (FirebaseAuthHelper.isUserLoggedIn()) {
                // User is logged in, check role and route accordingly
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                currentUser?.let { user ->
                    // 1) Ensure multi-step registration is fully completed
                    FirebaseAuthHelper.isRegistrationComplete(
                        userId = user.uid,
                        onComplete = { isComplete ->
                            if (!isComplete) {
                                // Registration NOT complete (user exited before Step 3).
                                // Clean up Firestore doc + Auth account so email can be reused.
                                val firestore = FirebaseFirestore.getInstance()
                                firestore.collection("users")
                                    .document(user.uid)
                                    .delete()
                                    .addOnCompleteListener {
                                        FirebaseAuthHelper.deleteCurrentUser(
                                            onSuccess = {
                                                android.util.Log.d("SplashActivity", "Incomplete registration cleaned on app start.")
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            },
                                            onFailure = { error ->
                                                android.util.Log.e("SplashActivity", "Failed to delete incomplete user: $error")
                                                FirebaseAuthHelper.signOut()
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            }
                                        )
                                    }
                            } else {
                                // 2) Registration is complete – proceed with existing role-based routing
                                FirebaseAuthHelper.getUserDataFromFirestore(
                                    userId = user.uid,
                                    onSuccess = { userData ->
                                        val role = userData?.get("role") as? String ?: "VOTER"
                                        val isVerified = userData?.get("isVerified") as? Boolean ?: false

                                        when (role) {
                                            "LEADER" -> {
                                                // Leaders: always require fresh verification on app restart
                                                android.util.Log.d("SplashActivity", "Leader detected, signing out for fresh verification")
                                                FirebaseAuthHelper.signOut()
                                                startActivity(Intent(this, MainActivity::class.java))
                                            }
                                            else -> {
                                                // Voter, go directly to Homepage
                                                startActivity(Intent(this, Homepage::class.java))
                                            }
                                        }
                                        finish()
                                    },
                                    onFailure = { error ->
                                        // If we can't get user data, default to voter homepage
                                        android.util.Log.e("SplashActivity", "Error getting user data: $error")
                                        startActivity(Intent(this, Homepage::class.java))
                                        finish()
                                    }
                                )
                            }
                        },
                        onError = { error ->
                            // If registration status can't be checked, be safe and send user to login
                            android.util.Log.e("SplashActivity", "Error checking registration status: $error")
                            FirebaseAuthHelper.signOut()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                } ?: run {
                    // No current user, go to login
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            } else {
                // User is not logged in, go to MainActivity (login/register screen)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            // ----------------------------------------------------
        }, SPLASH_DURATION_MS) // Use the defined constant for the duration
    }

    // --- FRONTEND: Cleanup the handler when the activity is destroyed ---
    override fun onDestroy() {
        super.onDestroy()
        // Stop the progress updates to prevent memory leaks
        handler.removeCallbacks(updateProgressRunnable)
    }
}