package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.PrayerAlarmScheduler
import com.example.worker.PrayerSyncManager
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

      // Ensure periodic WorkManager task and internet-connected sync are active
      PrayerSyncManager.scheduleIdleDailySync(context)
      PrayerSyncManager.scheduleInternetSyncWorker(context)

      val pendingResult = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        try {
          PrayerAlarmScheduler.scheduleUpcomingAlarms(context)
          Log.d(TAG, "Rescheduled upcoming prayer alarms after reboot or system time/timezone change")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to reschedule on boot", e)
        } finally {
          pendingResult.finish()
        }
      }
    }
  }
}
