package com.example.notificationapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

/**
 * Handles taps on the notification action buttons. The receiver runs even when
 * no activity is on screen, which is why the action buttons use a broadcast
 * PendingIntent instead of an activity one.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(IntentKeys.EXTRA_NOTIF_ID, -1)

        val message = when (intent.action) {
            ACTION_MARK_DONE -> "Marked as done"
            ACTION_SNOOZE -> "Snoozed for later"
            else -> return
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        if (notifId != -1) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.notificationapp.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.example.notificationapp.action.SNOOZE"
    }
}
