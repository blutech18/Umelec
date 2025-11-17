package com.example.umelec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit
import android.text.format.DateFormat
import androidx.core.content.res.ResourcesCompat
import android.graphics.Typeface
import android.text.format.DateUtils
import android.graphics.Color
import android.content.Context
import java.util.Date


class Notification : AppCompatActivity() {

    private val notifications: MutableList<NotificationItem> = mutableListOf()

    private val poppinsRegularTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(this, R.font.poppins_regular)
        } catch (e: Exception) {
            null
        }
    }

    private val montserratSemiBoldTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(this, R.font.montserrat_semi_bold)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        setupBackNavigation()
        applyFontsToStaticText()
        loadNotifications()
    }

    // --- FONT APPLICATION ---

    private fun applyFontsToStaticText() {
        (findViewById<LinearLayout>(R.id.newContainer).getChildAt(0) as? TextView)?.typeface = montserratSemiBoldTypeface
        (findViewById<LinearLayout>(R.id.olderContainer).getChildAt(0) as? TextView)?.typeface = montserratSemiBoldTypeface
    }

    // --- TOP NAVIGATION LOGIC ---

    private fun setupBackNavigation() {
        val backButton: ImageButton = findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }
    }

    // 💡 REMOVED: The local fetchAllNotifications function is no longer needed.

    private fun loadNotifications() {
        val userId = FirebaseAuthHelper.getCurrentUser()?.uid
        FirestoreNotificationHelper.fetchNotifications(
            userId = userId,
            limit = 50,
            onSuccess = { items ->
                notifications.clear()
                notifications.addAll(items)
                populateNotifications()
            },
            onFailure = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                notifications.clear()
                populateNotifications()
            }
        )
    }

    // --- DYNAMIC POPULATION ---

    private fun populateNotifications() {
        val newContainer: LinearLayout = findViewById(R.id.newContainer)
        val olderContainer: LinearLayout = findViewById(R.id.olderContainer)
        val emptyState: LinearLayout = findViewById(R.id.emptyStateContainer)

        // Clear existing dynamic views, but keep the header TextViews
        val newHeader = newContainer.getChildAt(0)
        val olderHeader = olderContainer.getChildAt(0)

        newContainer.removeAllViews()
        olderContainer.removeAllViews()

        newContainer.addView(newHeader) // Add the "New" TextView back
        olderContainer.addView(olderHeader) // Add the "Older" TextView back

        val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)

        // Sort and filter using the centralized data list
        val sortedNotifications = notifications.sortedByDescending { it.timestamp }
        val newNotifications = sortedNotifications.filter { it.timestamp > cutoffTime }
        val olderNotifications = sortedNotifications.filter { it.timestamp <= cutoffTime }

        // 2. Populate "New" Section
        if (newNotifications.isNotEmpty()) {
            newContainer.visibility = View.VISIBLE
            newNotifications.forEach { notification ->
                val itemView = createNotificationItemView(notification)
                newContainer.addView(itemView)

                // Always add separator after every item in the New list
                newContainer.addView(createSeparatorView(this))
            }
        } else {
            newContainer.visibility = View.GONE
        }

        // 3. Populate "Older" Section
        if (olderNotifications.isNotEmpty()) {
            olderContainer.visibility = View.VISIBLE
            olderNotifications.forEach { notification ->
                val itemView = createNotificationItemView(notification)
                olderContainer.addView(itemView)

                olderContainer.addView(createSeparatorView(this))
            }
        } else {
            olderContainer.visibility = View.GONE
        }

        emptyState.visibility =
            if (newNotifications.isEmpty() && olderNotifications.isEmpty()) View.VISIBLE else View.GONE
    }

    // --- PROGRAMMATIC VIEW CREATION ---

    /**
     * Dynamically creates the view for a single notification item based on its read status and type.
     */
    private fun createNotificationItemView(item: NotificationItem): View {
        val context = this
        val unreadIndicatorId = View.generateViewId()

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        if (!item.isRead) {
            val cornerRadiusMargin = 3.toPx()
            lp.leftMargin = cornerRadiusMargin
            lp.rightMargin = cornerRadiusMargin
        }

        val rootView = LinearLayout(context).apply {
            id = View.generateViewId()
            layoutParams = lp
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16.toPx(), 8.toPx(), 16.toPx(), 8.toPx())

            if (!item.isRead) {
                setBackgroundColor(Color.parseColor("#ECF1F4"))
            } else {
                setBackgroundColor(Color.TRANSPARENT)
            }

            setOnClickListener { view ->
                val currentUserId = FirebaseAuthHelper.getCurrentUser()?.uid
                if (!item.isRead && currentUserId != null) {
                    item.isRead = true
                    view.setBackgroundColor(Color.TRANSPARENT)
                    val indicator = view.findViewById<ImageView>(unreadIndicatorId)
                    indicator?.visibility = View.GONE
                    FirestoreNotificationHelper.markNotificationAsRead(item.id, currentUserId)
                }

                val intent = Intent(context, Notification2::class.java).apply {
                    putExtra("NOTIFICATION_ID", item.id)
                    putExtra("NOTIFICATION_TITLE", item.title)
                    putExtra("NOTIFICATION_FULL_TEXT", item.fullText)
                }
                context.startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
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
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            orientation = LinearLayout.VERTICAL
        }
        rootView.addView(textContainer)

        val titleText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = item.title
            textSize = 16f
            setTypeface(poppinsRegularTypeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#4A4A68"))
        }
        textContainer.addView(titleText)

        val previewText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = item.previewText
            textSize = 12f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = poppinsRegularTypeface
            setTextColor(Color.parseColor("#8C8CA1"))
        }
        textContainer.addView(previewText)

        val timeText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            text = formatNotificationTime(item.timestamp)
            textSize = 10f
            typeface = poppinsRegularTypeface
            setTextColor(Color.parseColor("#8C8CA1"))
        }
        textContainer.addView(timeText)

        if (!item.isRead) {
            val unreadIndicator = ImageView(context).apply {
                id = unreadIndicatorId
                layoutParams = LinearLayout.LayoutParams(10.toPx(), 10.toPx()).apply {
                    marginStart = 8.toPx()
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                setImageResource(R.drawable.ic_circle)
                setColorFilter(Color.parseColor("#0098E0"))
                contentDescription = "Unread Indicator"
            }
            rootView.addView(unreadIndicator)
        }

        return rootView
    }

    private fun createSeparatorView(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.toPx() // 1dp height
            )
            setBackgroundColor(Color.parseColor("#4A4A68"))
        }
    }


    private fun formatNotificationTime(timestamp: Long): CharSequence {
        val now = System.currentTimeMillis()
        val difference = now - timestamp

        if (difference < TimeUnit.DAYS.toMillis(1)) {
            return DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS)
        } else {
            return DateFormat.format("MMM dd, yyyy", timestamp)
        }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
}