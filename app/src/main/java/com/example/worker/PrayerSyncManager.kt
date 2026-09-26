package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.PrayerDatabase
import com.example.data.local.PrayerEntity
import com.example.data.model.CityLocation
import com.example.data.remote.PrayerApiService
import com.example.data.util.AstronomicalPrayerCalculator
import com.example.update.AppUpdateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object PrayerSyncManager {
  private const val TAG = "PrayerSyncManager"

  const val KEY_LAST_DAILY_CHECK_DATE = "key_last_daily_check_date"
  const val KEY_LAST_DAILY_CHECK_TIME = "key_last_daily_check_time"

  /**
   * Sets up WorkManager to sync prayer times and check updates daily when the phone is IDLE.
   * This completely respects Android's battery-saving architecture:
   * No background service or continuous process runs.
   */
  fun scheduleIdleDailySync(context: Context) {
    try {
      val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

      // Run every 24 hours (with flex interval) even when offline
      val idleSyncWork = PeriodicWorkRequestBuilder<IdlePrayerSyncWorker>(
        24, TimeUnit.HOURS,
        6, TimeUnit.HOURS // flex interval
      )
        .setConstraints(constraints)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        IdlePrayerSyncWorker.WORK_NAME_PERIODIC_IDLE,
        ExistingPeriodicWorkPolicy.UPDATE,
        idleSyncWork
      )
      Log.d(TAG, "WorkManager periodic sync scheduled")
    } catch (e: Exception) {
      Log.w(TAG, "WorkManager periodic sync initialization skipped or unavailable: ${e.message}")
    }
  }

  /**
   * Sets up WorkManager to immediately update prayer timings as soon as the device connects
   * to the internet. Ensures users who don't stay online all the time have their prayer
   * timings refreshed automatically the moment internet connectivity returns.
   */
  fun scheduleInternetSyncWorker(context: Context) {
    try {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val internetSyncWork = OneTimeWorkRequestBuilder<InternetConnectedPrayerSyncWorker>()
        .setConstraints(constraints)
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        InternetConnectedPrayerSyncWorker.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        internetSyncWork
      )
      Log.d(TAG, "WorkManager internet-connected sync task scheduled")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to schedule internet sync task: ${e.message}")
    }
  }

  /**
   * Called immediately whenever internet connectivity is restored on the device.
   * Fetches fresh official cloud timings, stores them in the Room database, and updates alarms.
   */
  suspend fun syncOnInternetRestored(context: Context): PrayerEntity = withContext(Dispatchers.IO) {
    Log.d(TAG, "Device reconnected to internet! Triggering immediate prayer timing sync...")
    val city = getSelectedCity(context)
    val entity = syncNow(context, city)
    Log.d(TAG, "Prayer timings updated immediately after getting internet: ${entity.syncSource}")
    entity
  }

  /**
   * Returns true if a daily sync & update check has already succeeded today or in the last 18 hours.
   */
  fun hasPerformedDailyCheckToday(context: Context): Boolean {
    val prefs = context.getSharedPreferences(IdlePrayerSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val lastDate = prefs.getString(KEY_LAST_DAILY_CHECK_DATE, "")
    val lastTime = prefs.getLong(KEY_LAST_DAILY_CHECK_TIME, 0L)
    val now = System.currentTimeMillis()
    val isChecked = (lastDate == todayStr) || (now - lastTime in 0..(18 * 3600 * 1000L))
    return isChecked
  }

  /**
   * Records that daily check was performed.
   */
  fun recordDailyCheck(context: Context) {
    val prefs = context.getSharedPreferences(IdlePrayerSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    prefs.edit()
      .putString(KEY_LAST_DAILY_CHECK_DATE, todayStr)
      .putLong(KEY_LAST_DAILY_CHECK_TIME, System.currentTimeMillis())
      .apply()
  }

  /**
   * Performs the daily check: updates prayer timings and checks for app updates.
   * Runs during any idle time of the day; if idle time is not found, runs once per day
   * during prayer notification delivery.
   */
  suspend fun performDailyCheckAndSync(context: Context, triggerSource: String): Boolean = withContext(Dispatchers.IO) {
    recordDailyCheck(context)
    Log.d(TAG, "Executing daily check & timing update. Trigger: $triggerSource")

    // 1. Refresh prayer timings
    try {
      val city = getSelectedCity(context)
      syncNow(context, city)
      Log.d(TAG, "Daily prayer timings updated successfully ($triggerSource)")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update prayer timings during $triggerSource: ${e.message}")
    }

    // 2. Check for App Updates
    try {
      val updateInfo = AppUpdateManager.checkForUpdates(context)
      if (updateInfo.hasUpdate) {
        Log.d(TAG, "App update found during $triggerSource: v${updateInfo.latestVersionName}")
        AppUpdateManager.showUpdateNotification(context, updateInfo)
      } else {
        Log.d(TAG, "App is up to date ($triggerSource)")
      }
    } catch (e: Exception) {
      Log.w(TAG, "App update check failed during $triggerSource: ${e.message}")
    }

    true
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
    val cityTz = TimeZone.getTimeZone(city.timeZoneId)
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = cityTz
    }.format(Date())

    var entity: PrayerEntity? = null
    val aladhanMethod = PrayerApiService.getAladhanMethodForCountry(city.country)

    // Try online API
    try {
      val apiService = PrayerApiService.create()
      val response = if (city.latitude != 0.0 || city.longitude != 0.0) {
        try {
          apiService.getTimingsByCoordinates(
            timestamp = System.currentTimeMillis() / 1000,
            latitude = city.latitude,
            longitude = city.longitude,
            method = aladhanMethod
          )
        } catch (_: Exception) {
          apiService.getTimingsByCity(city = city.name, country = city.country, method = aladhanMethod)
        }
      } else {
        apiService.getTimingsByCity(city = city.name, country = city.country, method = aladhanMethod)
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
      val cal = Calendar.getInstance(cityTz)
      val year = cal.get(Calendar.YEAR)
      val month = cal.get(Calendar.MONTH) + 1
      val day = cal.get(Calendar.DAY_OF_MONTH)
      val tzOffsetHours = cityTz.getOffset(cal.timeInMillis) / 3600000.0
      val calcMethod = AstronomicalPrayerCalculator.CalculationMethod.getMethodForCountry(city.country)

      val calc = AstronomicalPrayerCalculator.calculate(
        lat = city.latitude,
        lng = city.longitude,
        timezoneOffsetHours = tzOffsetHours,
        year = year,
        month = month,
        day = day,
        method = calcMethod
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

    // Run database persistence, offline cache pre-calculation, and alarm scheduling
    // in NonCancellable so that system alarms are reliably scheduled even if the calling job was cancelled.
    withContext(NonCancellable) {
      val db = PrayerDatabase.getDatabase(context)
      try {
        db.prayerDao().insertPrayerTimes(entity)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Failed to insert today prayer times into Room: ${e.message}")
      }

      // Pre-calculate upcoming 30 days offline schedule into Room
      // Guarantees that users who stay completely offline for 10-15+ days always have
      // accurate, mathematically calculated solar prayer timings ready on their device
      try {
        val cal = Calendar.getInstance(cityTz)
        val tzOffsetHours = cityTz.getOffset(cal.timeInMillis) / 3600000.0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
          timeZone = cityTz
        }
        val calcMethod = AstronomicalPrayerCalculator.CalculationMethod.getMethodForCountry(city.country)

        for (i in 1..30) {
          cal.add(Calendar.DAY_OF_YEAR, 1)
          val futureDateStr = dateFormat.format(cal.time)
          val futureYear = cal.get(Calendar.YEAR)
          val futureMonth = cal.get(Calendar.MONTH) + 1
          val futureDay = cal.get(Calendar.DAY_OF_MONTH)
          val futureCalc = AstronomicalPrayerCalculator.calculate(
            lat = city.latitude,
            lng = city.longitude,
            timezoneOffsetHours = tzOffsetHours,
            year = futureYear,
            month = futureMonth,
            day = futureDay,
            method = calcMethod
          )
          val futureEntity = PrayerEntity(
            date = futureDateStr,
            city = city.name,
            country = city.country,
            fajr = futureCalc.fajr,
            sunrise = futureCalc.sunrise,
            dhuhr = futureCalc.dhuhr,
            asr = futureCalc.asr,
            maghrib = futureCalc.maghrib,
            isha = futureCalc.isha,
            lastSyncedAt = System.currentTimeMillis(),
            syncSource = "Astronomical Solar Engine (Accurate Offline)"
          )
          db.prayerDao().insertPrayerTimes(futureEntity)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Failed to pre-cache upcoming offline days: ${e.message}")
      }

      try {
        PrayerAlarmScheduler.scheduleUpcomingAlarms(context, entity)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(TAG, "Failed to schedule upcoming alarms after sync", e)
      }
    }

    entity
  }
}
