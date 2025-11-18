package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class Notification2 : AppCompatActivity() {

    private var notificationType: NotificationType? = null

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
        val typeExtra = intent.getStringExtra("NOTIFICATION_TYPE")

        val userId = FirebaseAuthHelper.getCurrentUser()?.uid

        // Parse notification type if provided
        if (!typeExtra.isNullOrBlank()) {
            notificationType = try {
                NotificationType.valueOf(typeExtra)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        if (!titleExtra.isNullOrBlank() && !fullTextExtra.isNullOrBlank()) {
            findViewById<TextView>(R.id.notification_title).text = titleExtra
            findViewById<TextView>(R.id.notification_preview_text).text = fullTextExtra
            if (!userId.isNullOrEmpty()) {
                FirestoreNotificationHelper.markNotificationAsRead(notificationId, userId)
            }
            setupViewCandidatesButton()
        } else {
            FirestoreNotificationHelper.getNotificationById(
                notificationId = notificationId,
                userId = userId,
                onSuccess = { notification ->
                    if (notification != null) {
                        findViewById<TextView>(R.id.notification_title).text = notification.title
                        findViewById<TextView>(R.id.notification_preview_text).text = notification.fullText
                        notificationType = notification.type
                        if (!userId.isNullOrEmpty()) {
                            FirestoreNotificationHelper.markNotificationAsRead(notificationId, userId)
                        }
                        setupViewCandidatesButton()
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

    private fun setupViewCandidatesButton() {
        val btnViewCandidates: AppCompatButton = findViewById(R.id.btnViewCandidates)
        
        // Show button only for Election Reminder notifications
        if (notificationType == NotificationType.REMINDER) {
            btnViewCandidates.visibility = android.view.View.VISIBLE
            btnViewCandidates.setOnClickListener {
                val intent = Intent(this, Candidates::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        } else {
            btnViewCandidates.visibility = android.view.View.GONE
        }
    }
}