package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for a few seconds then check authentication state
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is logged in
            if (FirebaseAuthHelper.isUserLoggedIn()) {
                // User is logged in, check role and route accordingly
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                currentUser?.let { user ->
                    FirebaseAuthHelper.getUserDataFromFirestore(
                        userId = user.uid,
                        onSuccess = { userData ->
                            val role = userData?.get("role") as? String ?: "VOTER"
                            val isVerified = userData?.get("isVerified") as? Boolean ?: false
                            
                            // Route based on role and verification status
                            when (role) {
                                "LEADER" -> {
                                    // For leaders, always require fresh verification on app restart
                                    // This ensures security and prevents bypassing verification
                                    android.util.Log.d("SplashActivity", "Leader detected, signing out for fresh verification")
                                    FirebaseAuthHelper.signOut()
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                                else -> {
                                    // Voter, go to Homepage
                                    startActivity(Intent(this, Homepage::class.java))
                                }
                            }
                            finish()
                        },
                        onFailure = { error ->
                            // If we can't get user data, default to Voter homepage
                            android.util.Log.e("SplashActivity", "Error getting user data: $error")
                            startActivity(Intent(this, Homepage::class.java))
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
        }, 2000)  // 2-second splash
    }
}
