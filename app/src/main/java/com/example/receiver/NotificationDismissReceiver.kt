package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.alarm.PrayerNotificationHelper
import com.example.util.AlertSoundManager

class NotificationDismissReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "NotificationDismiss"
    const val ACTION_DISMISS_NOTIFICATION_SOUND = "com.example.ACTION_DISMISS_NOTIFICATION_SOUND"
    const val ACTION_STOP_NOTIFICATION_ACTION = "com.example.ACTION_STOP_NOTIFICATION_ACTION"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) return
    Log.d(TAG, "NotificationDismissReceiver received action: ${intent.action}")

    // Immediately stop any playing custom/alert audio
    AlertSoundManager.stopSound()

    // If tapped "Stop Audio" action button, also dismiss the notification if requested
    if (intent.action == ACTION_STOP_NOTIFICATION_ACTION) {
      val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, PrayerNotificationHelper.PRAYER_NOTIFICATION_ID)
      try {
        NotificationManagerCompat.from(context).cancel(notificationId)
      } catch (_: Exception) {}
    }
  }
}
