package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerNotificationHelper
import com.example.data.local.PrayerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "PrayerAlarmReceiver"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) return

    val action = intent.action
    Log.d(TAG, "PrayerAlarmReceiver woke up with action: $action")

    val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "Prayer"
    val prayerTime = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME) ?: ""
    val notificationId = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, 100)
    val isForbidden = intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_IS_FORBIDDEN, false) || notificationId in 200..299

    if (isForbidden) {
      PrayerNotificationHelper.showForbiddenTimeNotification(
        context = context,
        forbiddenName = prayerName,
        intervalFormatted = prayerTime,
        notificationId = notificationId
      )
    } else {
      // Show high-priority prayer notification immediately
      PrayerNotificationHelper.showPrayerNotification(
        context = context,
        prayerName = prayerName,
        prayerTime = prayerTime,
        notificationId = notificationId
      )
    }

    // Reschedule alarms for upcoming prayers using goAsync so process terminates immediately after
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = PrayerDatabase.getDatabase(context)
        val latest = db.prayerDao().getLatestPrayerTimesSync()
        if (latest != null) {
          val repo = com.example.repository.PrayerRepository(context)
          PrayerAlarmScheduler.scheduleAlarmsForToday(
            context = context,
            prayerEntity = latest,
            enabledPrayers = repo.getEnabledPrayers(),
            enabledForbidden = repo.getEnabledForbiddenTimes()
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error rescheduling next alarm", e)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
