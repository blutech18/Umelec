package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView // Import ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Animation Setup ---

        // Load the slide-up animation for the bottom container
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        // Load the new fade-in animation for the logo
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Find the views
        val bottomContainer = findViewById<LinearLayout>(R.id.bottomContainer)
        val logoImage = findViewById<ImageView>(R.id.logoImage) // Find the logo ImageView

        // Start the animations
        bottomContainer.startAnimation(slideUp)
        logoImage.startAnimation(fadeIn) // Apply fade-in animation to the logo

        // --- Button Click Listeners ---

        // Register button
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Login button
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}