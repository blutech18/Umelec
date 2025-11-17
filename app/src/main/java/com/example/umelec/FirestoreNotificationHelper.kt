package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirestoreNotificationHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val COLLECTION = "notifications"
    private const val TAG = "FirestoreNotifHelper"

    fun fetchNotifications(
        userId: String?,
        limit: Int,
        onSuccess: (List<NotificationItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                val notifications = snapshot.documents.mapNotNull { doc ->
                    val targetUserId = doc.getString("targetUserId")
                    if (targetUserId != null && targetUserId != userId) {
                        return@mapNotNull null
                    }

                    val title = doc.getString("title") ?: return@mapNotNull null
                    val previewText = doc.getString("previewText") ?: title
                    val fullText = doc.getString("fullText") ?: previewText
                    val typeString = doc.getString("type") ?: NotificationType.GENERIC.name
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                        ?: System.currentTimeMillis()
                    val readBy = doc.get("readBy") as? List<*>
                    val isRead = userId != null && readBy?.contains(userId) == true

                    NotificationItem(
                        id = doc.id,
                        title = title,
                        previewText = previewText,
                        fullText = fullText,
                        type = runCatching { NotificationType.valueOf(typeString) }.getOrDefault(NotificationType.GENERIC),
                        timestamp = timestamp,
                        isRead = isRead,
                        targetUserId = targetUserId
                    )
                }
                onSuccess(notifications)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching notifications: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to load notifications")
            }
    }

    fun markNotificationAsRead(
        notificationId: String,
        userId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        firestore.collection(COLLECTION)
            .document(notificationId)
            .update("readBy", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to mark notification as read: ${exception.message}", exception)
                onComplete(false)
            }
    }

    fun getNotificationById(
        notificationId: String,
        userId: String?,
        onSuccess: (NotificationItem?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(COLLECTION)
            .document(notificationId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val targetUserId = doc.getString("targetUserId")
                if (targetUserId != null && targetUserId != userId) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val title = doc.getString("title") ?: return@addOnSuccessListener onSuccess(null)
                val previewText = doc.getString("previewText") ?: title
                val fullText = doc.getString("fullText") ?: previewText
                val typeString = doc.getString("type") ?: NotificationType.GENERIC.name
                val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                    ?: System.currentTimeMillis()
                val readBy = doc.get("readBy") as? List<*>
                val isRead = userId != null && readBy?.contains(userId) == true

                val notification = NotificationItem(
                    id = doc.id,
                    title = title,
                    previewText = previewText,
                    fullText = fullText,
                    type = runCatching { NotificationType.valueOf(typeString) }.getOrDefault(NotificationType.GENERIC),
                    timestamp = timestamp,
                    isRead = isRead,
                    targetUserId = targetUserId
                )
                onSuccess(notification)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting notification by id: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to load notification")
            }
    }
}

