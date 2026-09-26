package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerNotificationHelper
import com.example.worker.PrayerSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "PrayerAlarmReceiver"
    const val ACTION_DISMISS_NOTIFICATION = "com.example.ACTION_DISMISS_NOTIFICATION"
    const val ACTION_STOP_AUDIO = "com.example.ACTION_STOP_AUDIO"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) return

    val action = intent.action
    Log.d(TAG, "PrayerAlarmReceiver woke up with action: $action")

    // Handle dismissing notification or stopping audio
    if (action == ACTION_DISMISS_NOTIFICATION || action == ACTION_STOP_AUDIO) {
      Log.d(TAG, "Notification dismissed or stop audio requested; stopping alert sound")
      com.example.util.AlertSoundManager.stopSound()
      return
    }

    // Handle silent daily midnight rollover check (00:01 AM)
    if (action == PrayerAlarmScheduler.ACTION_DAILY_ROLLOVER_ALARM) {
      Log.d(TAG, "Daily midnight rollover alarm triggered: rescheduling upcoming alarms for new day")
      val pendingResult = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        try {
          PrayerAlarmScheduler.scheduleUpcomingAlarms(context)
        } catch (e: Exception) {
          Log.e(TAG, "Error rescheduling on midnight rollover", e)
        } finally {
          pendingResult.finish()
        }
      }
      return
    }

    val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "Prayer"
    val prayerTime = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME) ?: ""
    val notificationId = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, 100)
    val isForbidden = intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_IS_FORBIDDEN, false) || notificationId in 200..299
    val ramadanEvent = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_RAMADAN_EVENT)
    val isRamadanAlarm = (action == PrayerAlarmScheduler.ACTION_RAMADAN_ALARM) || !ramadanEvent.isNullOrEmpty()

    if (isRamadanAlarm) {
      val effectiveEvent = if (!ramadanEvent.isNullOrEmpty()) ramadanEvent else prayerName
      if (effectiveEvent == PrayerAlarmScheduler.EVENT_EID_MUBARAK || effectiveEvent == "EID_MUBARAK") {
        PrayerNotificationHelper.showEidMubarakNotification(context)
        val adjustment = com.example.util.HijriDateHelper.getAdjustment(context)
        val hijriDate = com.example.util.HijriDateHelper.getHijriDate(java.util.Date(), adjustment)
        context.getSharedPreferences("ramadan_prefs", Context.MODE_PRIVATE)
          .edit()
          .putInt("key_eid_mubarak_notified_year", hijriDate.year)
          .apply()
      } else {
        PrayerNotificationHelper.showRamadanNotification(
          context = context,
          eventType = effectiveEvent,
          timeFormatted = prayerTime,
          notificationId = notificationId
        )
      }
    } else if (isForbidden) {
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

    // Reschedule alarms for upcoming prayers (today/tomorrow) and perform fallback daily check
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        PrayerAlarmScheduler.scheduleUpcomingAlarms(context)

        // If the device did not enter idle state to run the daily sync, perform the fallback timings check once a day
        if (!PrayerSyncManager.hasPerformedDailyCheckToday(context)) {
          Log.d(TAG, "Daily check has not run today; performing fallback update and timings check at notification delivery")
          PrayerSyncManager.performDailyCheckAndSync(context, "Notification Delivery Fallback")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error in background alarm notification task", e)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
