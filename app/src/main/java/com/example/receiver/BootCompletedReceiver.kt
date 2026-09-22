package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.PrayerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "BootCompletedReceiver"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    Log.d(TAG, "BootCompletedReceiver triggered on action: $action")

    if (action == Intent.ACTION_BOOT_COMPLETED ||
        action == Intent.ACTION_MY_PACKAGE_REPLACED ||
        action == Intent.ACTION_TIME_CHANGED ||
        action == Intent.ACTION_TIMEZONE_CHANGED) {

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
            Log.d(TAG, "Rescheduled prayer alarms after reboot/time change")
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to reschedule on boot", e)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }
}
