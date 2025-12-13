package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for a few seconds then check authentication state
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            // Always sign out user on app start to disable auto-login
            // This ensures users must log in again every time they open the app
            if (FirebaseAuthHelper.isUserLoggedIn()) {
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                currentUser?.let { user ->
                    // Check if registration is incomplete before signing out
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
                                                // Sign out and go to login
                                                FirebaseAuthHelper.signOut()
                                                FirebaseAuthHelper.clearTemporaryCredentials(this)
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            },
                                            onFailure = { error ->
                                                android.util.Log.e("SplashActivity", "Failed to delete incomplete user: $error")
                                                // Sign out and go to login
                                                FirebaseAuthHelper.signOut()
                                                FirebaseAuthHelper.clearTemporaryCredentials(this)
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            }
                                        )
                                    }
                            } else {
                                // Registration is complete - sign out user to disable auto-login
                                android.util.Log.d("SplashActivity", "User logged in, signing out to disable auto-login")
                                FirebaseAuthHelper.signOut()
                                FirebaseAuthHelper.clearTemporaryCredentials(this)
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        },
                        onError = { error ->
                            // If registration status can't be checked, sign out and send user to login
                            android.util.Log.e("SplashActivity", "Error checking registration status: $error")
                            FirebaseAuthHelper.signOut()
                            FirebaseAuthHelper.clearTemporaryCredentials(this)
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
        }, 2000)  // 2-second splash
    }
}
