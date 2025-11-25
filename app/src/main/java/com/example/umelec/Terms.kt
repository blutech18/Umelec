package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class Terms : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_terms)

            // Back button behavior
            val btnBack: ImageButton = findViewById(R.id.btnBack)
            btnBack.setOnClickListener {
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e("Terms", "Error in onCreate: ${e.message}", e)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
