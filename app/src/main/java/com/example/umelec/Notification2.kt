package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Notification2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification2)

        setupBackNavigation()
        displayNotificationContent()
    }

    private fun setupBackNavigation() {
        val backButton: ImageButton = findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun displayNotificationContent() {
        val notificationId = intent.getStringExtra("NOTIFICATION_ID")

        if (notificationId.isNullOrEmpty()) {
            findViewById<TextView>(R.id.notification_title).text = "Error"
            findViewById<TextView>(R.id.notification_preview_text).text = "Notification ID not found (No ID passed in Intent)."
            return
        }

        val titleExtra = intent.getStringExtra("NOTIFICATION_TITLE")
        val fullTextExtra = intent.getStringExtra("NOTIFICATION_FULL_TEXT")

        val userId = FirebaseAuthHelper.getCurrentUser()?.uid

        if (!titleExtra.isNullOrBlank() && !fullTextExtra.isNullOrBlank()) {
            findViewById<TextView>(R.id.notification_title).text = titleExtra
            findViewById<TextView>(R.id.notification_preview_text).text = fullTextExtra
            if (!userId.isNullOrEmpty()) {
                FirestoreNotificationHelper.markNotificationAsRead(notificationId, userId)
            }
        } else {
            FirestoreNotificationHelper.getNotificationById(
                notificationId = notificationId,
                userId = userId,
                onSuccess = { notification ->
                    if (notification != null) {
                        findViewById<TextView>(R.id.notification_title).text = notification.title
                        findViewById<TextView>(R.id.notification_preview_text).text = notification.fullText
                        if (!userId.isNullOrEmpty()) {
                            FirestoreNotificationHelper.markNotificationAsRead(notificationId, userId)
                        }
                    } else {
                        findViewById<TextView>(R.id.notification_title).text = "Error: Content Not Found"
                        findViewById<TextView>(R.id.notification_preview_text).text =
                            "The system received ID '$notificationId' but no matching notification was found."
                    }
                },
                onFailure = {
                    findViewById<TextView>(R.id.notification_title).text = "Error"
                    findViewById<TextView>(R.id.notification_preview_text).text = it
                }
            )
        }
    }
}