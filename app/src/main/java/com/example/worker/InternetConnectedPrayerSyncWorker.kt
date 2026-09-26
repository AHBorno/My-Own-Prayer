package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Worker triggered by Android WorkManager immediately when the device connects to the internet.
 * Guarantees that users who stay offline have their timings refreshed the moment their device
 * reconnects to the network, even when the app is running in the background or closed.
 */
class InternetConnectedPrayerSyncWorker(
  private val context: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

  companion object {
    private const val TAG = "InternetSyncWorker"
    const val WORK_NAME = "one_time_internet_connected_sync"
  }

  override suspend fun doWork(): Result {
    if (isStopped) return Result.success()
    Log.d(TAG, "Internet connection detected by WorkManager! Updating prayer timings immediately...")
    return try {
      val city = PrayerSyncManager.getSelectedCity(context)
      val entity = PrayerSyncManager.syncNow(context, city)
      Log.d(TAG, "Timings updated successfully upon internet connection: ${entity.syncSource}")

      // Reschedule worker so the next internet reconnection will trigger again
      PrayerSyncManager.scheduleInternetSyncWorker(context)

      Result.success()
    } catch (e: kotlinx.coroutines.CancellationException) {
      Log.d(TAG, "Internet sync worker job was cancelled or replaced")
      Result.success()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update prayer timings on internet connection: ${e.message}")
      Result.success()
    }
  }
}
