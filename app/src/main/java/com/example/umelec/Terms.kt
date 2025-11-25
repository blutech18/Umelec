package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class Terms : AppCompatActivity() {
    
    private var backCallback: OnBackPressedCallback? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("Terms", "onCreate started")
        
        // Set up global exception handler to catch any uncaught exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e("Terms", "Uncaught exception in thread ${thread.name}: ${exception.message}", exception)
            exception.printStackTrace()
            // Restore default handler and let it handle the crash
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        try {
            setContentView(R.layout.activity_terms)
            android.util.Log.d("Terms", "setContentView completed")

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

            // Handle system back button with OnBackPressedDispatcher
            backCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    try {
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    } catch (e: Exception) {
                        android.util.Log.e("Terms", "Error in onBackPressed: ${e.message}", e)
                        finish()
                    }
                }
            }
            onBackPressedDispatcher.addCallback(this, backCallback!!)

            android.util.Log.d("Terms", "onCreate completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("Terms", "Error in onCreate: ${e.message}", e)
            android.util.Log.e("Terms", "Stack trace: ", e)
            e.printStackTrace()
            // Don't finish on error - let the activity try to display
        }
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("Terms", "onStart")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("Terms", "onResume")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("Terms", "onPause")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.d("Terms", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("Terms", "onDestroy")
        // Remove callback to prevent memory leaks
        backCallback?.remove()
    }
}
