package com.example.umelec

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import java.util.Date
import java.util.concurrent.TimeUnit

class NotificationManager(private val activity: AppCompatActivity) {

    private var isNotificationDropdownVisible = false
    private var popupWindow: PopupWindow? = null
    private var isLoading = false
    private var latestNotifications: List<NotificationItem> = emptyList()

    private val poppinsRegularTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(activity, R.font.poppins_regular)
        } catch (e: Exception) {
            null
        }
    }

    fun toggleNotificationDropdown(anchorView: ImageView) {
        if (isNotificationDropdownVisible) {
            anchorView.setColorFilter(Color.parseColor("#FAFCFE"))
            popupWindow?.dismiss()
        } else if (!isLoading) {
            fetchNotificationsAndShow(anchorView)
        }
    }

    private fun fetchNotificationsAndShow(anchorView: ImageView) {
        val currentUserId = FirebaseAuthHelper.getCurrentUser()?.uid
        isLoading = true
        FirestoreNotificationHelper.fetchNotifications(
            userId = currentUserId,
            limit = 10,
            onSuccess = { notifications ->
                isLoading = false
                latestNotifications = notifications
                showNotificationDropdown(anchorView, notifications)
                isNotificationDropdownVisible = true
            },
            onFailure = { error ->
                isLoading = false
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showNotificationDropdown(anchorView: ImageView, notifications: List<NotificationItem>) {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.notification_dropdown, null)

        val width = ViewGroup.LayoutParams.WRAP_CONTENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        val focusable = true
        popupWindow = PopupWindow(popupView, width, height, focusable)

        popupWindow?.setOnDismissListener {
            anchorView.setColorFilter(Color.parseColor("#FAFCFE"))
            isNotificationDropdownVisible = false
            popupWindow = null
        }

        anchorView.setColorFilter(Color.parseColor("#FCBE6A"))

        val notificationContainer: LinearLayout = popupView.findViewById(R.id.notificationListContainer)
        val emptyStateContainer: LinearLayout = popupView.findViewById(R.id.dropdownEmptyState)
        popupView.findViewById<TextView>(R.id.dropdownEmptyTitle)?.typeface = poppinsRegularTypeface
        popupView.findViewById<TextView>(R.id.dropdownEmptyMessage)?.typeface = poppinsRegularTypeface

        notificationContainer.removeAllViews()

        // Filter to only show Election Reminder (REMINDER) and Vote Submitted (SUBMISSION) notifications
        val filteredNotifications = notifications.filter { notification ->
            notification.type == NotificationType.REMINDER || notification.type == NotificationType.SUBMISSION
        }
        
        val itemsToShow = filteredNotifications.sortedByDescending { it.timestamp }.take(3)
        if (itemsToShow.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
        } else {
            emptyStateContainer.visibility = View.GONE
            itemsToShow.forEachIndexed { index, item ->
                val notificationItemView = createNotificationItemView(item)
                notificationContainer.addView(notificationItemView)
                if (index < itemsToShow.size - 1) {
                    notificationContainer.addView(createSeparatorView(activity))
                }
            }
        }

        popupView.findViewById<ImageView>(R.id.closeDropdownButton).setOnClickListener {
            popupWindow?.dismiss()
        }

        val viewAllButton: TextView = popupView.findViewById(R.id.viewAllButton)
        viewAllButton.typeface = poppinsRegularTypeface
        viewAllButton.setOnClickListener {
            activity.startActivity(Intent(activity, Notification::class.java))
            popupWindow?.dismiss()
        }

        popupWindow?.showAsDropDown(anchorView, -300, 0)
    }

    private fun createNotificationItemView(item: NotificationItem): View {
        val context = activity
        val unreadIndicatorId = View.generateViewId()

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        if (!item.isRead) {
            val margin = 3.toPx()
            params.leftMargin = margin
            params.rightMargin = margin
        }

        val rootView = LinearLayout(context).apply {
            id = View.generateViewId()
            layoutParams = params
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16.toPx(), 8.toPx(), 16.toPx(), 8.toPx())
            setBackgroundColor(if (item.isRead) Color.TRANSPARENT else Color.parseColor("#ECF1F4"))

            // Only Election Reminder notifications are clickable
            if (item.type == NotificationType.REMINDER) {
                setOnClickListener { view ->
                    val currentUserId = FirebaseAuthHelper.getCurrentUser()?.uid
                    if (!item.isRead && currentUserId != null) {
                        item.isRead = true
                        view.setBackgroundColor(Color.TRANSPARENT)
                        view.findViewById<ImageView>(unreadIndicatorId)?.visibility = View.GONE
                        FirestoreNotificationHelper.markNotificationAsRead(item.id, currentUserId)
                    }

                    val intent = Intent(context, Notification2::class.java).apply {
                        putExtra("NOTIFICATION_ID", item.id)
                        putExtra("NOTIFICATION_TITLE", item.title)
                        putExtra("NOTIFICATION_FULL_TEXT", item.fullText)
                        putExtra("NOTIFICATION_TYPE", item.type.name)
                    }
                    context.startActivity(intent)
                    popupWindow?.dismiss()
                }
            } else {
                // Vote Submitted notifications are not clickable
                // Mark as read when viewed but don't navigate
                val currentUserId = FirebaseAuthHelper.getCurrentUser()?.uid
                if (!item.isRead && currentUserId != null) {
                    // Mark as read in background when displayed
                    FirestoreNotificationHelper.markNotificationAsRead(item.id, currentUserId)
                }
            }
        }

        val iconRes = when (item.type) {
            NotificationType.REMINDER -> R.drawable.ic_notif_reminder
            NotificationType.SUBMISSION -> R.drawable.ic_notif_submitted
            NotificationType.GENERIC -> android.R.drawable.ic_menu_info_details
        }

        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(30.toPx(), 30.toPx()).apply {
                marginEnd = 12.toPx()
            }
            setImageResource(iconRes)
            setColorFilter(Color.parseColor("#4A4A68"))
            contentDescription = "Notification type icon"
        }
        rootView.addView(icon)

        val textContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        rootView.addView(textContainer)

        val titleText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = item.title
            textSize = 16f
            setTypeface(poppinsRegularTypeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#4A4A68"))
        }
        textContainer.addView(titleText)

        val previewText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = item.previewText
            textSize = 12f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = poppinsRegularTypeface
            setTextColor(Color.parseColor("#8C8CA1"))
        }
        textContainer.addView(previewText)

        val timeText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = formatNotificationTime(item.timestamp)
            textSize = 10f
            typeface = poppinsRegularTypeface
            setTextColor(Color.parseColor("#8C8CA1"))
        }
        textContainer.addView(timeText)

        if (!item.isRead) {
            val indicator = ImageView(context).apply {
                id = unreadIndicatorId
                layoutParams = LinearLayout.LayoutParams(10.toPx(), 10.toPx()).apply {
                    marginStart = 8.toPx()
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_circle)
                setColorFilter(Color.parseColor("#0098E0"))
                contentDescription = "Unread Indicator"
            }
            rootView.addView(indicator)
        }

        return rootView
    }

    private fun createSeparatorView(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.toPx()
            )
            setBackgroundColor(Color.parseColor("#4A4A68"))
        }
    }

    private fun formatNotificationTime(timestamp: Long): CharSequence {
        val now = System.currentTimeMillis()
        val difference = now - timestamp
        return if (difference < TimeUnit.DAYS.toMillis(1)) {
            DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS)
        } else {
            DateFormat.format("MMM dd, yyyy", Date(timestamp))
        }
    }

    private fun Int.toPx(): Int = (this * activity.resources.displayMetrics.density).toInt()
}
