package com.example.umelec

// --- Data Classes: Defined Once Here ---

enum class NotificationType {
    REMINDER, SUBMISSION, GENERIC
}

/**
 * Defines the structure for a complete notification item, including the full text.
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val previewText: String,
    val fullText: String,
    val type: NotificationType,
    val timestamp: Long,
    var isRead: Boolean,
    val targetUserId: String? = null
)
