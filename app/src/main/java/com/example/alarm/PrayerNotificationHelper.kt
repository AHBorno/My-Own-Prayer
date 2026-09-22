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

object PrayerNotificationHelper {
  const val CHANNEL_ID = "prayer_time_notifications_v1"
  private const val CHANNEL_NAME = "Prayer Times Reminders"
  private const val CHANNEL_DESC = "Exact battery-optimized notifications for daily prayers"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

      val channel = NotificationChannel(
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

      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showPrayerNotification(
    context: Context,
    prayerName: String,
    prayerTime: String,
    notificationId: Int
  ) {
    createNotificationChannel(context)

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SOURCE", "NOTIFICATION")
      putExtra("PRAYER_NAME", prayerName)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val quranQuote = getPrayerQuote(prayerName)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("🕌 Time for $prayerName Prayer ($prayerTime)")
      .setContentText(quranQuote)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle("🕌 Time for $prayerName Prayer ($prayerTime)")
          .bigText("$quranQuote\n\nTake a mindful break to establish your prayer on time.")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setVibrate(longArrayOf(0, 400, 200, 400))
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission not granted yet
    }
  }

  fun showForbiddenTimeNotification(
    context: Context,
    forbiddenName: String,
    intervalFormatted: String,
    notificationId: Int
  ) {
    createNotificationChannel(context)

    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SOURCE", "FORBIDDEN_NOTIFICATION")
      putExtra("FORBIDDEN_NAME", forbiddenName)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val warningDetails = when {
      forbiddenName.contains("Sunrise", ignoreCase = true) ->
        "The sun is rising above the horizon. Voluntary (Nafl) prayers are prohibited until the sun has fully risen (Ishraq time)."
      forbiddenName.contains("Zenith", ignoreCase = true) ->
        "The sun is at its astronomical meridian peak (Istiwa / Zawal). Voluntary prayers are prohibited until the sun begins to decline into Dhuhr."
      else ->
        "The sun is setting into the horizon. Voluntary prayers are prohibited during this interval until Maghrib."
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("⚠️ Forbidden Prayer Time: $forbiddenName")
      .setContentText("Voluntary prayers prohibited now ($intervalFormatted)")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle("⚠️ Forbidden Prayer Time: $forbiddenName")
          .bigText("$warningDetails\n\nInterval: $intervalFormatted\nPlease refrain from offering voluntary prayers until this interval completes.")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setVibrate(longArrayOf(0, 300, 150, 300))
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission not granted yet
    }
  }

  private fun getPrayerQuote(prayerName: String): String {
    return when (prayerName.lowercase()) {
      "fajr" -> "Indeed, the recitation of dawn is ever witnessed. (Surah Al-Isra 17:78)"
      "ishraq" -> "Whoever prays Fajr, remembers Allah until sunrise, and prays two rak'ahs gets the reward of Hajj and Umrah. (Tirmidhi)"
      "duha" -> "Charity is due upon every joint of your body; and two rak'ahs of Duha suffices for all of that. (Sahih Muslim)"
      "dhuhr" -> "Establish prayer at the decline of the sun. (Surah Al-Isra 17:78)"
      "asr" -> "Maintain with care the [obligatory] prayers and [in particular] the middle prayer. (Surah Al-Baqarah 2:238)"
      "maghrib" -> "And remember the name of your Lord morning and evening. (Surah Al-Insan 76:25)"
      "isha" -> "And during a part of the night, prostrate to Him and exalt Him a long night. (Surah Al-Insan 76:26)"
      "tahajjud" -> "The best prayer after the obligatory prayers is the night prayer (Tahajjud / Qiyam al-Layl). (Sahih Muslim)"
      else -> "Indeed, prayer has been decreed upon the believers a decree of specified times. (Surah An-Nisa 4:103)"
    }
  }
}
