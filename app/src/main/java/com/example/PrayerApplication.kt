package com.example

import android.app.Application
import android.util.Log
import com.example.alarm.PrayerNotificationHelper
import com.example.worker.PrayerSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerApplication : Application() {
  companion object {
    private const val TAG = "PrayerApplication"
  }

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "PrayerApplication initialized")

    // Create high-importance prayer notification channel
    PrayerNotificationHelper.createNotificationChannel(this)

    // Register WorkManager daily idle sync task
    // (runs ONLY when device is idle and connected, stopping immediately after)
    PrayerSyncManager.scheduleIdleDailySync(this)

    // Ensure today's alarms are scheduled
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val city = PrayerSyncManager.getSelectedCity(this@PrayerApplication)
        PrayerSyncManager.syncNow(this@PrayerApplication, city)
      } catch (e: Exception) {
        Log.e(TAG, "Initial sync failed", e)
      }
    }
  }
}
