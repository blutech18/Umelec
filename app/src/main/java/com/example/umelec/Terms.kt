package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class Terms : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_terms)

            // Back button behavior with null safety
            val btnBack: ImageButton? = findViewById(R.id.btnBack)
            btnBack?.setOnClickListener {
                try {
                    finish()
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                } catch (e: Exception) {
                    android.util.Log.e("Terms", "Error in back button: ${e.message}", e)
                    finish()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Terms", "Error in onCreate: ${e.message}", e)
            android.util.Log.e("Terms", "Stack trace: ", e)
            // Try to finish gracefully
            try {
                finish()
            } catch (finishException: Exception) {
                android.util.Log.e("Terms", "Error finishing activity: ${finishException.message}", finishException)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            super.onBackPressed()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } catch (e: Exception) {
            android.util.Log.e("Terms", "Error in onBackPressed: ${e.message}", e)
            finish()
        }
    }
}
