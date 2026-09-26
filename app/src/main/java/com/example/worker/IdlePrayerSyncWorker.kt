package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.PrayerDatabase
import com.example.data.local.PrayerEntity
import com.example.data.model.CityLocation
import com.example.data.remote.PrayerApiService
import com.example.data.util.AstronomicalPrayerCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Worker that runs when the device is IDLE (e.g. overnight or charging undisturbed).
 * Fetches prayer times for today and saves them in the local Room database,
 * then schedules exact AlarmManager alarms for the day.
 * Ensures the app does NOT run continuously in the background!
 */
class IdlePrayerSyncWorker(
  private val context: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

  companion object {
    private const val TAG = "IdlePrayerSyncWorker"
    const val WORK_NAME_PERIODIC_IDLE = "periodic_idle_prayer_sync"
    const val WORK_NAME_ONE_TIME = "one_time_prayer_sync"
    const val PREFS_NAME = "prayer_app_prefs"
    const val KEY_CITY_NAME = "key_city_name"
    const val KEY_COUNTRY_NAME = "key_country_name"
    const val KEY_LAT = "key_lat"
    const val KEY_LNG = "key_lng"
    const val KEY_TIMEZONE = "key_timezone"
  }

  override suspend fun doWork(): Result {
    Log.d(TAG, "IdlePrayerSyncWorker executed while phone is idle!")

    return try {
      PrayerSyncManager.performDailyCheckAndSync(context, "Device Idle Time")
      Log.d(TAG, "Idle prayer sync & update check completed successfully")
      Result.success()
    } catch (e: Exception) {
      Log.e(TAG, "Idle prayer sync failed", e)
      Result.retry()
    }
  }

  private fun cleanTime(raw: String): String {
    return raw.trim().split(" ")[0]
  }
}
