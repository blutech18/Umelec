package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.TextView

class Verification : AppCompatActivity() {

    // Define the same key used in ForgotPassword.kt for retrieving the data
    companion object {
        const val EXTRA_EMAIL_ADDRESS = "com.example.umelec.EMAIL_ADDRESS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)


        val btnBack = findViewById<ImageButton>(R.id.btnBack) // Assuming a back button ID
        btnBack.setOnClickListener {
            finish()
        }

        // 1. Find the TextView that will display the email
        val textEmail = findViewById<TextView>(R.id.textEmail)

        // 2. Retrieve the email string from the Intent extras
        // The second argument is a default value if the key is not found (null in this case)
        val userEmail = intent.getStringExtra(EXTRA_EMAIL_ADDRESS)

        // 3. Update the TextView with the received email
        if (userEmail != null && userEmail.isNotEmpty()) {
            textEmail.text = userEmail
        } else {
            // Fallback text if the email data was somehow missed
            textEmail.text = "Email address not available"
        }

        // =====================================================================
        // 2. BTNENTERCODE SETUP (Always Enabled)
        // =====================================================================

        // Initialize the button
        val btnEnterCode = findViewById<Button>(R.id.btnEnterCode)

        // Set the click listener to navigate to the next activity
        btnEnterCode.setOnClickListener {
            // Navigate to VerificationCode.kt
            val intent = Intent(this, Verificationcode::class.java)
            startActivity(intent)
            // Note: You might want to finish() this activity here if the user shouldn't return
            // finish()
            overridePendingTransition(0, 0)
        }
    }
}