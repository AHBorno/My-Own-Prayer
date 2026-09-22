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
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val cityName = prefs.getString(KEY_CITY_NAME, CityLocation.DEFAULT_CITY.name) ?: CityLocation.DEFAULT_CITY.name
      val countryName = prefs.getString(KEY_COUNTRY_NAME, CityLocation.DEFAULT_CITY.country) ?: CityLocation.DEFAULT_CITY.country
      val lat = prefs.getFloat(KEY_LAT, CityLocation.DEFAULT_CITY.latitude.toFloat()).toDouble()
      val lng = prefs.getFloat(KEY_LNG, CityLocation.DEFAULT_CITY.longitude.toFloat()).toDouble()
      val tzId = prefs.getString(KEY_TIMEZONE, CityLocation.DEFAULT_CITY.timeZoneId) ?: CityLocation.DEFAULT_CITY.timeZoneId

      val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

      var entity: PrayerEntity? = null

      // Attempt 1: Fetch from Remote API (Google/Aladhan)
      try {
        val apiService = PrayerApiService.create()
        val response = apiService.getTimingsByCity(city = cityName, country = countryName)
        val timings = response.data?.timings
        if (timings != null && timings.fajr != null) {
          entity = PrayerEntity(
            date = todayDateStr,
            city = cityName,
            country = countryName,
            fajr = cleanTime(timings.fajr),
            sunrise = cleanTime(timings.sunrise ?: "06:00"),
            dhuhr = cleanTime(timings.dhuhr ?: "12:00"),
            asr = cleanTime(timings.asr ?: "15:30"),
            maghrib = cleanTime(timings.maghrib ?: "18:00"),
            isha = cleanTime(timings.isha ?: "19:30"),
            lastSyncedAt = System.currentTimeMillis(),
            syncSource = "Google & Aladhan Cloud API (Device Idle Sync)"
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Network sync failed or device offline during idle; falling back to astronomical calculation", e)
      }

      // Attempt 2: If offline or failed, compute precise astronomical prayer times
      if (entity == null) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(tzId))
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzOffsetHours = TimeZone.getTimeZone(tzId).getOffset(cal.timeInMillis) / 3600000.0

        val calc = AstronomicalPrayerCalculator.calculate(
          lat = lat,
          lng = lng,
          timezoneOffsetHours = tzOffsetHours,
          year = year,
          month = month,
          day = day
        )

        entity = PrayerEntity(
          date = todayDateStr,
          city = cityName,
          country = countryName,
          fajr = calc.fajr,
          sunrise = calc.sunrise,
          dhuhr = calc.dhuhr,
          asr = calc.asr,
          maghrib = calc.maghrib,
          isha = calc.isha,
          lastSyncedAt = System.currentTimeMillis(),
          syncSource = "Astronomical Solar Engine (Offline Idle Sync)"
        )
      }

      // Save to Room Database
      val db = PrayerDatabase.getDatabase(context)
      db.prayerDao().insertPrayerTimes(entity)

      // Schedule exact alarms for the day
      val repo = com.example.repository.PrayerRepository(context)
      PrayerAlarmScheduler.scheduleAlarmsForToday(
        context = context,
        prayerEntity = entity,
        enabledPrayers = repo.getEnabledPrayers(),
        enabledForbidden = repo.getEnabledForbiddenTimes()
      )

      Log.d(TAG, "Idle prayer sync completed successfully for $cityName on $todayDateStr")
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
