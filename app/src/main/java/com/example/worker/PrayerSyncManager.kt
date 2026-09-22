package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.PrayerDatabase
import com.example.data.local.PrayerEntity
import com.example.data.model.CityLocation
import com.example.data.remote.PrayerApiService
import com.example.data.util.AstronomicalPrayerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object PrayerSyncManager {

  /**
   * Sets up WorkManager to sync prayer times daily when the phone is IDLE and connected to a network.
   * This completely respects Android's battery-saving architecture:
   * No background service or continuous process runs.
   */
  fun scheduleIdleDailySync(context: Context) {
    try {
      val constraints = Constraints.Builder()
        .setRequiresDeviceIdle(true)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      // Run every 24 hours while device is idle
      val idleSyncWork = PeriodicWorkRequestBuilder<IdlePrayerSyncWorker>(
        24, TimeUnit.HOURS,
        6, TimeUnit.HOURS // flex interval
      )
        .setConstraints(constraints)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        IdlePrayerSyncWorker.WORK_NAME_PERIODIC_IDLE,
        ExistingPeriodicWorkPolicy.KEEP,
        idleSyncWork
      )
    } catch (e: Exception) {
      android.util.Log.w("PrayerSyncManager", "WorkManager idle sync initialization skipped or unavailable: ${e.message}")
    }
  }

  fun saveSelectedCity(context: Context, city: CityLocation) {
    val prefs = context.getSharedPreferences(IdlePrayerSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putString(IdlePrayerSyncWorker.KEY_CITY_NAME, city.name)
      .putString(IdlePrayerSyncWorker.KEY_COUNTRY_NAME, city.country)
      .putFloat(IdlePrayerSyncWorker.KEY_LAT, city.latitude.toFloat())
      .putFloat(IdlePrayerSyncWorker.KEY_LNG, city.longitude.toFloat())
      .putString(IdlePrayerSyncWorker.KEY_TIMEZONE, city.timeZoneId)
      .apply()
  }

  fun getSelectedCity(context: Context): CityLocation {
    val prefs = context.getSharedPreferences(IdlePrayerSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString(IdlePrayerSyncWorker.KEY_CITY_NAME, CityLocation.DEFAULT_CITY.name) ?: CityLocation.DEFAULT_CITY.name
    val country = prefs.getString(IdlePrayerSyncWorker.KEY_COUNTRY_NAME, CityLocation.DEFAULT_CITY.country) ?: CityLocation.DEFAULT_CITY.country
    val lat = prefs.getFloat(IdlePrayerSyncWorker.KEY_LAT, CityLocation.DEFAULT_CITY.latitude.toFloat()).toDouble()
    val lng = prefs.getFloat(IdlePrayerSyncWorker.KEY_LNG, CityLocation.DEFAULT_CITY.longitude.toFloat()).toDouble()
    val tz = prefs.getString(IdlePrayerSyncWorker.KEY_TIMEZONE, CityLocation.DEFAULT_CITY.timeZoneId) ?: CityLocation.DEFAULT_CITY.timeZoneId
    return CityLocation(name, country, lat, lng, tz)
  }

  /**
   * Synchronizes prayer times immediately (e.g. on first launch or city change),
   * saves to Room, and sets up exact alarms for today.
   */
  suspend fun syncNow(context: Context, city: CityLocation): PrayerEntity = withContext(Dispatchers.IO) {
    saveSelectedCity(context, city)
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    var entity: PrayerEntity? = null

    // Try online API
    try {
      val apiService = PrayerApiService.create()
      val response = if (city.latitude != 0.0 || city.longitude != 0.0) {
        try {
          apiService.getTimingsByCoordinates(
            timestamp = System.currentTimeMillis() / 1000,
            latitude = city.latitude,
            longitude = city.longitude
          )
        } catch (_: Exception) {
          apiService.getTimingsByCity(city = city.name, country = city.country)
        }
      } else {
        apiService.getTimingsByCity(city = city.name, country = city.country)
      }
      val timings = response.data?.timings
      if (timings != null && timings.fajr != null) {
        entity = PrayerEntity(
          date = todayDateStr,
          city = city.name,
          country = city.country,
          fajr = timings.fajr.trim().split(" ")[0],
          sunrise = (timings.sunrise ?: "06:00").trim().split(" ")[0],
          dhuhr = (timings.dhuhr ?: "12:00").trim().split(" ")[0],
          asr = (timings.asr ?: "15:30").trim().split(" ")[0],
          maghrib = (timings.maghrib ?: "18:00").trim().split(" ")[0],
          isha = (timings.isha ?: "19:30").trim().split(" ")[0],
          lastSyncedAt = System.currentTimeMillis(),
          syncSource = "Google & Aladhan Cloud API (Online GPS)"
        )
      }
    } catch (_: Exception) {
      // Offline fallback
    }

    if (entity == null) {
      val cal = Calendar.getInstance(TimeZone.getTimeZone(city.timeZoneId))
      val year = cal.get(Calendar.YEAR)
      val month = cal.get(Calendar.MONTH) + 1
      val day = cal.get(Calendar.DAY_OF_MONTH)
      val tzOffsetHours = TimeZone.getTimeZone(city.timeZoneId).getOffset(cal.timeInMillis) / 3600000.0

      val calc = AstronomicalPrayerCalculator.calculate(
        lat = city.latitude,
        lng = city.longitude,
        timezoneOffsetHours = tzOffsetHours,
        year = year,
        month = month,
        day = day
      )

      entity = PrayerEntity(
        date = todayDateStr,
        city = city.name,
        country = city.country,
        fajr = calc.fajr,
        sunrise = calc.sunrise,
        dhuhr = calc.dhuhr,
        asr = calc.asr,
        maghrib = calc.maghrib,
        isha = calc.isha,
        lastSyncedAt = System.currentTimeMillis(),
        syncSource = "Astronomical Solar Engine (Accurate Offline)"
      )
    }

    val db = PrayerDatabase.getDatabase(context)
    db.prayerDao().insertPrayerTimes(entity)
    val repo = com.example.repository.PrayerRepository(context)
    PrayerAlarmScheduler.scheduleAlarmsForToday(
      context = context,
      prayerEntity = entity,
      enabledPrayers = repo.getEnabledPrayers(),
      enabledForbidden = repo.getEnabledForbiddenTimes()
    )

    entity
  }
}
