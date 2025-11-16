package com.example.umelec

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// NOTE: NotificationItem, NotificationType, and allNotifications are now defined in NotificationData.kt

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

        // 💡 CHANGE: Use the centralized list to find the full notification object
        val notificationItem = allNotifications.find { it.id == notificationId }

        if (notificationItem != null) {
            findViewById<TextView>(R.id.notification_title).text = notificationItem.title
            // Use the fullText field from the unified data structure
            findViewById<TextView>(R.id.notification_preview_text).text = notificationItem.fullText
        } else {
            // This fallback handles any ID not found in the master list
            findViewById<TextView>(R.id.notification_title).text = "Error: Content Not Found"
            findViewById<TextView>(R.id.notification_preview_text).text = "The system received ID '$notificationId' but no matching notification was found in the data source."
        }
    }
}