package com.example

import android.app.Application
import android.util.Log
import com.example.alarm.PrayerNotificationHelper
import com.example.util.NetworkConnectivityMonitor
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
    PrayerSyncManager.scheduleIdleDailySync(this)

    // Register WorkManager one-time trigger that executes immediately when internet connects
    PrayerSyncManager.scheduleInternetSyncWorker(this)

    // Monitor internet connectivity in real-time:
    // If device was offline, immediately update prayer timings as soon as internet is detected!
    NetworkConnectivityMonitor.startMonitoring(this) {
      try {
        Log.d(TAG, "Network connection detected! Performing immediate prayer timing update...")
        PrayerSyncManager.syncOnInternetRestored(this@PrayerApplication)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to update prayer timings on internet restored", e)
      }
    }

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
