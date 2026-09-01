package com.example.notificationapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Central place for everything notification related: channel setup, the
 * PendingIntents that make a notification tappable, and one builder per
 * notification style demonstrated in this experiment.
 */
object NotificationHelper {

    const val CHANNEL_GENERAL = "general_updates"
    const val CHANNEL_ALERTS = "important_alerts"

    const val ID_SIMPLE = 1001
    const val ID_BIG_TEXT = 1002
    const val ID_INBOX = 1003
    const val ID_ACTIONS = 1004
    const val ID_PROGRESS = 1005
    const val ID_ALERT = 1006

    /**
     * Channels must exist before any notification is posted on Android 8.0+.
     * Creating an existing channel again is a no-op, so this is safe to call
     * on every app start.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val general = NotificationChannel(
            CHANNEL_GENERAL,
            "General updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Everyday notifications from the demo app"
        }

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Important alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Heads-up notifications that pop over the screen"
            enableVibration(true)
        }

        manager.createNotificationChannel(general)
        manager.createNotificationChannel(alerts)
    }

    /** Opens [NotificationDetailActivity] and hands the notification data to it. */
    private fun contentIntent(
        context: Context,
        requestCode: Int,
        title: String,
        message: String,
        type: String
    ): PendingIntent {
        val intent = Intent(context, NotificationDetailActivity::class.java).apply {
            putExtra(IntentKeys.EXTRA_NOTIF_TITLE, title)
            putExtra(IntentKeys.EXTRA_NOTIF_MESSAGE, message)
            putExtra(IntentKeys.EXTRA_NOTIF_TYPE, type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Fires a broadcast handled by [NotificationActionReceiver]. */
    private fun actionIntent(context: Context, requestCode: Int, action: String, notifId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(IntentKeys.EXTRA_NOTIF_ID, notifId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun baseBuilder(context: Context, channelId: String) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)

    /** 1. The plain title + one line of text notification. */
    fun showSimple(context: Context, user: String) {
        val title = "Hello ${user.ifEmpty { "there" }}!"
        val message = "This is a simple notification created with NotificationCompat."

        val notification = baseBuilder(context, CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(context, ID_SIMPLE, title, message, "Simple"))
            .build()

        post(context, ID_SIMPLE, notification)
    }

    /** 2. BigTextStyle — collapses to one line, expands to a paragraph. */
    fun showBigText(context: Context) {
        val title = "Expandable notification"
        val message = "Tap the arrow to expand me."
        val longText = "BigTextStyle lets a notification show a long block of text once the " +
            "user expands it. It is the right style for messages, article summaries or any " +
            "content that does not fit on the single collapsed line. Collapsed, the system " +
            "only shows the setContentText() value; expanded, it shows this paragraph instead."

        val notification = baseBuilder(context, CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(context, ID_BIG_TEXT, title, longText, "Big text"))
            .build()

        post(context, ID_BIG_TEXT, notification)
    }

    /** 3. InboxStyle — a summary notification holding several lines. */
    fun showInbox(context: Context) {
        val title = "3 new lab updates"
        val lines = listOf(
            "Exp 3 — Fragments report approved",
            "Exp 4 — Login page marks published",
            "Exp 5 — Notifications submission due Friday"
        )

        val style = NotificationCompat.InboxStyle().setSummaryText("Android Lab")
        lines.forEach { style.addLine(it) }

        val notification = baseBuilder(context, CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setNumber(lines.size)
            .setContentIntent(contentIntent(context, ID_INBOX, title, lines.joinToString("\n"), "Inbox"))
            .build()

        post(context, ID_INBOX, notification)
    }

    /** 4. Notification carrying two action buttons handled by a BroadcastReceiver. */
    fun showWithActions(context: Context) {
        val title = "Assignment reminder"
        val message = "Submit the notifications experiment before Friday."

        val notification = baseBuilder(context, CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent(context, ID_ACTIONS, title, message, "With actions"))
            .addAction(
                R.drawable.ic_notification,
                "Mark done",
                actionIntent(context, 2001, NotificationActionReceiver.ACTION_MARK_DONE, ID_ACTIONS)
            )
            .addAction(
                R.drawable.ic_notification,
                "Snooze",
                actionIntent(context, 2002, NotificationActionReceiver.ACTION_SNOOZE, ID_ACTIONS)
            )
            .build()

        post(context, ID_ACTIONS, notification)
    }

    /** 5a. Ongoing notification with a determinate progress bar. */
    fun showProgress(context: Context, progress: Int) {
        val notification = baseBuilder(context, CHANNEL_GENERAL)
            .setContentTitle("Downloading study material")
            .setContentText("$progress% complete")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        post(context, ID_PROGRESS, notification)
    }

    /** 5b. Replaces the progress notification with a completed one. */
    fun showProgressComplete(context: Context) {
        val title = "Download complete"
        val message = "study-material.pdf is ready to open."

        val notification = baseBuilder(context, CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setContentIntent(contentIntent(context, ID_PROGRESS, title, message, "Progress"))
            .build()

        post(context, ID_PROGRESS, notification)
    }

    /** 6. High importance channel + high priority = heads-up notification. */
    fun showHeadsUp(context: Context) {
        val title = "Class starting now"
        val message = "Mobile Application Development lab, Room 204."

        val notification = baseBuilder(context, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent(context, ID_ALERT, title, message, "Heads-up"))
            .build()

        post(context, ID_ALERT, notification)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    /**
     * Posting throws a SecurityException when POST_NOTIFICATIONS was never
     * granted, so the caller-facing screen checks the permission first and this
     * guard is only a safety net.
     */
    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post — nothing to do.
        }
    }
}
