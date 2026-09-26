package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.PrayerAlarmReceiver
import com.example.util.AlertSoundManager
import com.example.util.AppLanguageHelper

object PrayerNotificationHelper {
  const val CHANNEL_ID = "prayer_time_notifications_v1"
  const val CHANNEL_ID_CUSTOM = "prayer_time_notifications_custom_v1"
  private const val CHANNEL_NAME = "Prayer Times Reminders"
  private const val CHANNEL_DESC = "Exact battery-optimized notifications for daily prayers"

  // Unified Notification ID ensures each new prayer notification replaces the previous one
  const val PRAYER_NOTIFICATION_ID = 1000
  const val EID_MUBARAK_NOTIFICATION_ID = 1005

  /**
   * Cancels any previously posted prayer or forbidden time notifications
   * to ensure only the latest active notification remains in the notification shade.
   */
  fun clearPreviousNotifications(context: Context) {
    try {
      AlertSoundManager.stopSound()
      val notificationManager = NotificationManagerCompat.from(context)
      notificationManager.cancel(PRAYER_NOTIFICATION_ID)
      notificationManager.cancel(EID_MUBARAK_NOTIFICATION_ID)
      // Also clear any legacy individual prayer/forbidden notification IDs
      val legacyIds = intArrayOf(100, 101, 102, 103, 104, 105, 106, 107, 108, 200, 201, 202, 203, 888, 999, 1005)
      for (id in legacyIds) {
        notificationManager.cancel(id)
      }
    } catch (_: Exception) {}
  }

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // Standard Channel (with default system sound)
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

      val defaultChannel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 400, 200, 400)
        setSound(soundUri, audioAttributes)
        setShowBadge(true)
      }
      notificationManager.createNotificationChannel(defaultChannel)

      // Custom Alert Channel (silent system channel so MediaPlayer can play user's custom MP3)
      val customChannel = NotificationChannel(
        CHANNEL_ID_CUSTOM,
        "$CHANNEL_NAME (Custom Sound)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Prayer alerts using your custom selected MP3 sound"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 400, 200, 400)
        setSound(null, null)
        setShowBadge(true)
      }
      notificationManager.createNotificationChannel(customChannel)
    }
  }

  private fun getActiveChannelId(context: Context): String {
    return if (AlertSoundManager.isCustomSoundEnabled(context)) {
      CHANNEL_ID_CUSTOM
    } else {
      CHANNEL_ID
    }
  }

  private fun playAlertSoundIfCustom(context: Context) {
    if (AlertSoundManager.isCustomSoundEnabled(context)) {
      AlertSoundManager.playAlertSound(context)
    }
  }

  private fun getDeletePendingIntent(context: Context, requestCode: Int): PendingIntent {
    val deleteIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = PrayerAlarmReceiver.ACTION_DISMISS_NOTIFICATION
    }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      deleteIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getStopAudioPendingIntent(context: Context, requestCode: Int): PendingIntent {
    val stopIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = PrayerAlarmReceiver.ACTION_STOP_AUDIO
    }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  fun showPrayerNotification(
    context: Context,
    prayerName: String,
    prayerTime: String,
    notificationId: Int = PRAYER_NOTIFICATION_ID,
    isRamadan: Boolean = false
  ) {
    createNotificationChannel(context)

    // Remove any previous prayer notification before showing the new one
    clearPreviousNotifications(context)

    val lang = AppLanguageHelper.getSavedLanguage(context)

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SOURCE", "NOTIFICATION")
      putExtra("PRAYER_NAME", prayerName)
      putExtra("IS_RAMADAN", isRamadan)
    }

    val targetNotificationId = PRAYER_NOTIFICATION_ID
    val pendingIntent = PendingIntent.getActivity(
      context,
      targetNotificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val deletePendingIntent = getDeletePendingIntent(context, targetNotificationId + 1000)

    val title = AppLanguageHelper.getNotificationTitle(prayerName, prayerTime, lang)
    val quranQuote = AppLanguageHelper.getPrayerQuote(prayerName, lang)
    val subtext = AppLanguageHelper.getNotificationSubtext(lang)

    val builder = NotificationCompat.Builder(context, getActiveChannelId(context))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(quranQuote)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(title)
          .bigText("$quranQuote\n\n$subtext")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setDeleteIntent(deletePendingIntent)
      .setVibrate(longArrayOf(0, 400, 200, 400))

    if (AlertSoundManager.isCustomSoundEnabled(context)) {
      val stopAudioPendingIntent = getStopAudioPendingIntent(context, targetNotificationId + 2000)
      val stopLabel = when (lang) {
        "bn" -> "সাউন্ড বন্ধ করুন"
        "ar" -> "إيقاف الصوت"
        else -> "Stop Audio"
      }
      builder.addAction(R.drawable.ic_notification, stopLabel, stopAudioPendingIntent)
    }

    val notification = builder.build()

    try {
      NotificationManagerCompat.from(context).notify(targetNotificationId, notification)
      playAlertSoundIfCustom(context)
    } catch (_: SecurityException) {
      // Permission not granted yet
    }
  }

  fun showRamadanNotification(
    context: Context,
    eventType: String,
    timeFormatted: String,
    notificationId: Int = PRAYER_NOTIFICATION_ID
  ) {
    createNotificationChannel(context)

    // Remove any previous prayer/alert notification before showing the new one
    clearPreviousNotifications(context)

    val lang = AppLanguageHelper.getSavedLanguage(context)

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SOURCE", "RAMADAN_NOTIFICATION")
      putExtra("EVENT_TYPE", eventType)
    }

    val targetNotificationId = PRAYER_NOTIFICATION_ID
    val pendingIntent = PendingIntent.getActivity(
      context,
      targetNotificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val deletePendingIntent = getDeletePendingIntent(context, targetNotificationId + 1000)

    val title = AppLanguageHelper.getRamadanNotificationTitle(eventType, lang)
    val body = AppLanguageHelper.getRamadanNotificationBody(eventType, timeFormatted, lang)

    val builder = NotificationCompat.Builder(context, getActiveChannelId(context))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(title)
          .bigText(body)
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setDeleteIntent(deletePendingIntent)
      .setVibrate(longArrayOf(0, 350, 150, 350))

    if (AlertSoundManager.isCustomSoundEnabled(context)) {
      val stopAudioPendingIntent = getStopAudioPendingIntent(context, targetNotificationId + 2000)
      val stopLabel = when (lang) {
        "bn" -> "সাউন্ড বন্ধ করুন"
        "ar" -> "إيقاف الصوت"
        else -> "Stop Audio"
      }
      builder.addAction(R.drawable.ic_notification, stopLabel, stopAudioPendingIntent)
    }

    val notification = builder.build()

    try {
      NotificationManagerCompat.from(context).notify(targetNotificationId, notification)
      playAlertSoundIfCustom(context)
    } catch (_: SecurityException) {
      // Permission not granted yet
    }
  }

  fun showForbiddenTimeNotification(
    context: Context,
    forbiddenName: String,
    intervalFormatted: String,
    notificationId: Int = PRAYER_NOTIFICATION_ID
  ) {
    createNotificationChannel(context)

    // Remove any previous prayer/forbidden notification before showing the new one
    clearPreviousNotifications(context)

    val lang = AppLanguageHelper.getSavedLanguage(context)

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SOURCE", "FORBIDDEN_NOTIFICATION")
      putExtra("FORBIDDEN_NAME", forbiddenName)
    }

    val targetNotificationId = PRAYER_NOTIFICATION_ID
    val pendingIntent = PendingIntent.getActivity(
      context,
      targetNotificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val deletePendingIntent = getDeletePendingIntent(context, targetNotificationId + 1000)

    val title = AppLanguageHelper.getForbiddenNotificationTitle(forbiddenName, lang)
    val body = AppLanguageHelper.getForbiddenNotificationBody(forbiddenName, intervalFormatted, lang)

    val builder = NotificationCompat.Builder(context, getActiveChannelId(context))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(title)
          .bigText(body)
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setDeleteIntent(deletePendingIntent)
      .setVibrate(longArrayOf(0, 300, 150, 300))

    if (AlertSoundManager.isCustomSoundEnabled(context)) {
      val stopAudioPendingIntent = getStopAudioPendingIntent(context, targetNotificationId + 2000)
      val stopLabel = when (lang) {
        "bn" -> "সাউন্ড বন্ধ করুন"
        "ar" -> "إيقاف الصوت"
        else -> "Stop Audio"
      }
      builder.addAction(R.drawable.ic_notification, stopLabel, stopAudioPendingIntent)
    }

    val notification = builder.build()

    try {
      NotificationManagerCompat.from(context).notify(targetNotificationId, notification)
      playAlertSoundIfCustom(context)
    } catch (_: SecurityException) {
      // Permission not granted yet
    }
  }

  fun showEidMubarakNotification(
    context: Context,
    notificationId: Int = EID_MUBARAK_NOTIFICATION_ID
  ) {
    createNotificationChannel(context)
    clearPreviousNotifications(context)

    val lang = AppLanguageHelper.getSavedLanguage(context)

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SOURCE", "EID_MUBARAK_NOTIFICATION")
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val deletePendingIntent = getDeletePendingIntent(context, notificationId + 1000)

    val title = AppLanguageHelper.getEidMubarakNotificationTitle(lang)
    val body = AppLanguageHelper.getEidMubarakNotificationBody(lang)

    val builder = NotificationCompat.Builder(context, getActiveChannelId(context))
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(title)
          .bigText(body)
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_EVENT)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setDeleteIntent(deletePendingIntent)
      .setVibrate(longArrayOf(0, 400, 200, 400, 200, 400))

    if (AlertSoundManager.isCustomSoundEnabled(context)) {
      val stopAudioPendingIntent = getStopAudioPendingIntent(context, notificationId + 2000)
      val stopLabel = when (lang) {
        "bn" -> "সাউন্ড বন্ধ করুন"
        "ar" -> "إيقاف الصوت"
        else -> "Stop Audio"
      }
      builder.addAction(R.drawable.ic_notification, stopLabel, stopAudioPendingIntent)
    }

    val notification = builder.build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
      playAlertSoundIfCustom(context)
    } catch (_: SecurityException) {
      // Permission not granted yet
    }
  }
}
