package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.local.PrayerDatabase
import com.example.data.local.PrayerEntity
import com.example.data.model.CityLocation
import com.example.data.util.AstronomicalPrayerCalculator
import com.example.receiver.PrayerAlarmReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PrayerAlarmScheduler {
  private const val TAG = "PrayerAlarmScheduler"

  const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
  const val ACTION_TEST_ALARM = "com.example.ACTION_TEST_ALARM"
  const val ACTION_RAMADAN_ALARM = "com.example.ACTION_RAMADAN_ALARM"
  const val ACTION_DAILY_ROLLOVER_ALARM = "com.example.ACTION_DAILY_ROLLOVER_ALARM"

  const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
  const val EXTRA_PRAYER_TIME = "EXTRA_PRAYER_TIME"
  const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
  const val EXTRA_IS_FORBIDDEN = "EXTRA_IS_FORBIDDEN"
  const val EXTRA_RAMADAN_EVENT = "EXTRA_RAMADAN_EVENT"

  // Base request codes for each prayer
  const val REQUEST_CODE_FAJR = 101
  const val REQUEST_CODE_DHUHR = 102
  const val REQUEST_CODE_ASR = 103
  const val REQUEST_CODE_MAGHRIB = 104
  const val REQUEST_CODE_ISHA = 105
  const val REQUEST_CODE_ISHRAQ = 106
  const val REQUEST_CODE_DUHA = 107
  const val REQUEST_CODE_TAHAJJUD = 108
  const val REQUEST_CODE_FORBIDDEN_SUNRISE = 201
  const val REQUEST_CODE_FORBIDDEN_ZENITH = 202
  const val REQUEST_CODE_FORBIDDEN_SUNSET = 203
  const val REQUEST_CODE_SUHOOR_SOON = 301
  const val REQUEST_CODE_SUHOOR_EXACT = 302
  const val REQUEST_CODE_IFTAR_SOON = 303
  const val REQUEST_CODE_IFTAR_EXACT = 304
  const val REQUEST_CODE_EID_MUBARAK = 305
  const val REQUEST_CODE_MIDNIGHT_ROLLOVER = 400
  const val REQUEST_CODE_TEST = 999

  const val EVENT_EID_MUBARAK = "EID_MUBARAK"

  data class PrayerScheduleDefinition(
    val name: String,
    val todayTime: String,
    val tomorrowTime: String,
    val requestCode: Int
  )

  /**
   * Schedules exact alarms for all enabled prayers for the day, including voluntary prayers
   * such as Ishraq, Duha, and Tahajjud, plus optional Forbidden Prayer Time warnings.
   */
  fun scheduleAlarmsForToday(
    context: Context,
    prayerEntity: PrayerEntity,
    enabledPrayers: Set<String> = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
    enabledForbidden: Set<String> = emptySet()
  ) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        scheduleUpcomingAlarms(context, prayerEntity)
      } catch (e: Exception) {
        if (e is CancellationException) return@launch
        Log.e(TAG, "Failed to schedule upcoming alarms in background", e)
      }
    }
  }

  /**
   * Reschedules alarms for all upcoming prayers for today and tomorrow.
   * Guarantees that when offline:
   * 1. Alarms are ALWAYS scheduled for the true next prayer (today or tomorrow).
   * 2. Pre-caches and replenishes 30 days of offline astronomical prayer timings into Room.
   * 3. Sets a daily midnight rollover alarm to guarantee fresh schedule every morning even if offline.
   */
  suspend fun scheduleUpcomingAlarms(
    context: Context,
    explicitEntity: PrayerEntity? = null
  ) = withContext(Dispatchers.IO + NonCancellable) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@withContext
    val repo = com.example.repository.PrayerRepository(context)
    val city = repo.getSelectedCity()
    val cityTz = TimeZone.getTimeZone(city.timeZoneId)
    val now = System.currentTimeMillis()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = cityTz
    }
    val todayDateStr = dateFormat.format(Date(now))

    val tomorrowCal = Calendar.getInstance(cityTz).apply {
      timeInMillis = now
      add(Calendar.DAY_OF_YEAR, 1)
    }
    val tomorrowDateStr = dateFormat.format(tomorrowCal.time)

    val todayEntity = if (explicitEntity != null && explicitEntity.date == todayDateStr) {
      explicitEntity
    } else {
      getOrComputePrayerEntity(context, todayDateStr, city)
    }

    val tomorrowEntity = getOrComputePrayerEntity(context, tomorrowDateStr, city)

    // Keep future offline cache replenished so offline use NEVER runs out of dates
    replenishOfflineCacheIfLow(context, city)

    val enabledPrayers = repo.getEnabledPrayers()
    val enabledForbidden = repo.getEnabledForbiddenTimes()

    // 1. OBLIGATORY & VOLUNTARY PRAYERS
    val todayVoluntary = computeVoluntaryTimes(todayEntity, cityTz)
    val tomorrowVoluntary = computeVoluntaryTimes(tomorrowEntity, cityTz)

    val prayerList = listOf(
      PrayerScheduleDefinition("Fajr", todayEntity.fajr, tomorrowEntity.fajr, REQUEST_CODE_FAJR),
      PrayerScheduleDefinition("Ishraq", todayVoluntary.ishraq, tomorrowVoluntary.ishraq, REQUEST_CODE_ISHRAQ),
      PrayerScheduleDefinition("Duha", todayVoluntary.duha, tomorrowVoluntary.duha, REQUEST_CODE_DUHA),
      PrayerScheduleDefinition("Dhuhr", todayEntity.dhuhr, tomorrowEntity.dhuhr, REQUEST_CODE_DHUHR),
      PrayerScheduleDefinition("Asr", todayEntity.asr, tomorrowEntity.asr, REQUEST_CODE_ASR),
      PrayerScheduleDefinition("Maghrib", todayEntity.maghrib, tomorrowEntity.maghrib, REQUEST_CODE_MAGHRIB),
      PrayerScheduleDefinition("Isha", todayEntity.isha, tomorrowEntity.isha, REQUEST_CODE_ISHA),
      PrayerScheduleDefinition("Tahajjud", todayVoluntary.tahajjud, tomorrowVoluntary.tahajjud, REQUEST_CODE_TAHAJJUD)
    )

    for (def in prayerList) {
      if (!enabledPrayers.contains(def.name)) {
        cancelAlarm(context, def.requestCode)
        continue
      }

      val todayMillis = parsePrayerTimeToMillis(todayDateStr, def.todayTime, cityTz)
      if (todayMillis > now) {
        setExactAlarm(
          context = context,
          alarmManager = alarmManager,
          triggerMillis = todayMillis,
          requestCode = def.requestCode,
          prayerName = def.name,
          prayerTime = def.todayTime,
          isForbidden = false
        )
      } else {
        // Today's prayer time has passed -> schedule for tomorrow's exact prayer time!
        val tomorrowMillis = parsePrayerTimeToMillis(tomorrowDateStr, def.tomorrowTime, cityTz)
        if (tomorrowMillis > now) {
          setExactAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerMillis = tomorrowMillis,
            requestCode = def.requestCode,
            prayerName = def.name,
            prayerTime = def.tomorrowTime,
            isForbidden = false
          )
        }
      }
    }

    // 2. FORBIDDEN PRAYER TIMES
    val todayForbidden = computeForbiddenTimesForScheduling(todayEntity, cityTz)
    val tomorrowForbidden = computeForbiddenTimesForScheduling(tomorrowEntity, cityTz)

    for (i in todayForbidden.indices) {
      val itemToday = todayForbidden[i]
      val itemTomorrow = tomorrowForbidden[i]

      if (!enabledForbidden.contains(itemToday.name)) {
        cancelAlarm(context, itemToday.requestCode)
        continue
      }

      if (itemToday.startMillis > now) {
        setExactAlarm(
          context = context,
          alarmManager = alarmManager,
          triggerMillis = itemToday.startMillis,
          requestCode = itemToday.requestCode,
          prayerName = itemToday.name,
          prayerTime = itemToday.intervalFormatted,
          isForbidden = true
        )
      } else if (itemTomorrow.startMillis > now) {
        setExactAlarm(
          context = context,
          alarmManager = alarmManager,
          triggerMillis = itemTomorrow.startMillis,
          requestCode = itemTomorrow.requestCode,
          prayerName = itemTomorrow.name,
          prayerTime = itemTomorrow.intervalFormatted,
          isForbidden = true
        )
      }
    }

    // 3. RAMADAN EXTRA NOTIFICATIONS: Suhoor Ending Soon & Iftar Approaching Soon (~15 min)
    val isRamadanToday = com.example.util.RamadanHelper.isRamadanActive(
      context = context,
      prayerEntity = todayEntity,
      now = now,
      forcePreview = repo.isPreviewRamadanMode()
    )
    val isRamadanTomorrow = com.example.util.RamadanHelper.isRamadanActive(
      context = context,
      prayerEntity = tomorrowEntity,
      now = now + (24 * 3600 * 1000L),
      forcePreview = repo.isPreviewRamadanMode()
    )

    val suhoorEnabled = repo.isSuhoorNotificationEnabled()
    val iftarEnabled = repo.isIftarNotificationEnabled()

    if (suhoorEnabled && (isRamadanToday || isRamadanTomorrow)) {
      val todayFajrMillis = parsePrayerTimeToMillis(todayDateStr, todayEntity.fajr, cityTz)
      val todaySuhoorSoon = todayFajrMillis - (15 * 60 * 1000L)

      val tomorrowFajrMillis = parsePrayerTimeToMillis(tomorrowDateStr, tomorrowEntity.fajr, cityTz)
      val tomorrowSuhoorSoon = tomorrowFajrMillis - (15 * 60 * 1000L)

      if (isRamadanToday && todaySuhoorSoon > now) {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { this.timeZone = cityTz }.format(Date(todaySuhoorSoon))
        setExactRamadanAlarm(context, alarmManager, todaySuhoorSoon, REQUEST_CODE_SUHOOR_SOON, "SUHOOR_SOON", timeFmt)
      } else if (isRamadanTomorrow && tomorrowSuhoorSoon > now) {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { this.timeZone = cityTz }.format(Date(tomorrowSuhoorSoon))
        setExactRamadanAlarm(context, alarmManager, tomorrowSuhoorSoon, REQUEST_CODE_SUHOOR_SOON, "SUHOOR_SOON", timeFmt)
      } else {
        cancelRamadanAlarm(context, REQUEST_CODE_SUHOOR_SOON, "SUHOOR_SOON")
      }
    } else {
      cancelRamadanAlarm(context, REQUEST_CODE_SUHOOR_SOON, "SUHOOR_SOON")
    }

    if (iftarEnabled && (isRamadanToday || isRamadanTomorrow)) {
      val todayMaghribMillis = parsePrayerTimeToMillis(todayDateStr, todayEntity.maghrib, cityTz)
      val todayIftarSoon = todayMaghribMillis - (14 * 60 * 1000L)

      val tomorrowMaghribMillis = parsePrayerTimeToMillis(tomorrowDateStr, tomorrowEntity.maghrib, cityTz)
      val tomorrowIftarSoon = tomorrowMaghribMillis - (14 * 60 * 1000L)

      if (isRamadanToday && todayIftarSoon > now) {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { this.timeZone = cityTz }.format(Date(todayIftarSoon))
        setExactRamadanAlarm(context, alarmManager, todayIftarSoon, REQUEST_CODE_IFTAR_SOON, "IFTAR_SOON", timeFmt)
      } else if (isRamadanTomorrow && tomorrowIftarSoon > now) {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { this.timeZone = cityTz }.format(Date(tomorrowIftarSoon))
        setExactRamadanAlarm(context, alarmManager, tomorrowIftarSoon, REQUEST_CODE_IFTAR_SOON, "IFTAR_SOON", timeFmt)
      } else {
        cancelRamadanAlarm(context, REQUEST_CODE_IFTAR_SOON, "IFTAR_SOON")
      }
    } else {
      cancelRamadanAlarm(context, REQUEST_CODE_IFTAR_SOON, "IFTAR_SOON")
    }

    // 3b. EID MUBARAK NOTIFICATION: Scheduled when Ramadan section disappears after last Iftar of Ramadan
    scheduleEidMubarakAlarmIfNeeded(
      context = context,
      alarmManager = alarmManager,
      todayEntity = todayEntity,
      tomorrowEntity = tomorrowEntity,
      todayDateStr = todayDateStr,
      tomorrowDateStr = tomorrowDateStr,
      cityTz = cityTz,
      now = now
    )

    // 4. DAILY MIDNIGHT ROLLOVER ALARM (00:01 AM)
    scheduleDailyMidnightRollover(context, alarmManager, cityTz, now)
  }

  /**
   * Retrieves or computes a valid PrayerEntity for any date entirely offline without internet.
   */
  suspend fun getOrComputePrayerEntity(
    context: Context,
    dateStr: String,
    city: CityLocation
  ): PrayerEntity = withContext(NonCancellable) {
    val db = PrayerDatabase.getDatabase(context)
    val cached = db.prayerDao().getPrayerTimesForDateSync(dateStr)
    if (cached != null) return@withContext cached

    val cityTz = TimeZone.getTimeZone(city.timeZoneId)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = cityTz }
    val parsedDate = try { sdf.parse(dateStr) ?: Date() } catch (_: Exception) { Date() }
    val cal = Calendar.getInstance(cityTz).apply { time = parsedDate }
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

    val computedEntity = PrayerEntity(
      date = dateStr,
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
    db.prayerDao().insertPrayerTimes(computedEntity)
    computedEntity
  }

  /**
   * Ensures that at least 30 days of offline astronomical prayer timings are always pre-cached in Room.
   */
  suspend fun replenishOfflineCacheIfLow(context: Context, city: CityLocation) = withContext(NonCancellable) {
    try {
      val db = PrayerDatabase.getDatabase(context)
      val cityTz = TimeZone.getTimeZone(city.timeZoneId)
      val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = cityTz }
      val todayStr = dateFormat.format(Date())
      val cachedCount = db.prayerDao().countFutureCachedDays(todayStr)
      if (cachedCount < 7) {
        Log.d(TAG, "Offline cache low ($cachedCount days left). Pre-calculating 30 days of offline timings...")
        val cal = Calendar.getInstance(cityTz)
        val tzOffsetHours = cityTz.getOffset(cal.timeInMillis) / 3600000.0
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
        Log.d(TAG, "Offline cache replenished for 30 days successfully")
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.w(TAG, "Failed to replenish offline cache: ${e.message}")
    }
  }

  /**
   * Sets a silent midnight rollover alarm at 00:01 AM so the scheduler wakes up each morning
   * and refreshes all alarms for the new day even if the user never opens the app while offline.
   */
  fun scheduleDailyMidnightRollover(
    context: Context,
    alarmManager: AlarmManager,
    cityTz: TimeZone,
    now: Long
  ) {
    val midnightCal = Calendar.getInstance(cityTz).apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 1)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= now) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_DAILY_ROLLOVER_ALARM
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_MIDNIGHT_ROLLOVER,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightCal.timeInMillis, pendingIntent)
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightCal.timeInMillis, pendingIntent)
      }
      Log.d(TAG, "Scheduled daily midnight rollover check at ${midnightCal.time}")
    } catch (e: SecurityException) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightCal.timeInMillis, pendingIntent)
    }
  }

  fun setExactRamadanAlarm(
    context: Context,
    alarmManager: AlarmManager,
    triggerMillis: Long,
    requestCode: Int,
    eventType: String,
    timeFormatted: String
  ) {
    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_RAMADAN_ALARM
      data = Uri.parse("ramadan://alarm/$eventType/$requestCode")
      putExtra(EXTRA_RAMADAN_EVENT, eventType)
      putExtra(EXTRA_PRAYER_NAME, eventType)
      putExtra(EXTRA_PRAYER_TIME, timeFormatted)
      putExtra(EXTRA_NOTIFICATION_ID, requestCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      }
      Log.d(TAG, "Scheduled exact Ramadan alarm for $eventType at $triggerMillis")
    } catch (e: SecurityException) {
      Log.e(TAG, "Failed to schedule exact Ramadan alarm", e)
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  fun cancelRamadanAlarm(context: Context, requestCode: Int, eventType: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_RAMADAN_ALARM
      data = Uri.parse("ramadan://alarm/$eventType/$requestCode")
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }

  fun scheduleEidMubarakAlarmIfNeeded(
    context: Context,
    alarmManager: AlarmManager,
    todayEntity: PrayerEntity,
    tomorrowEntity: PrayerEntity,
    todayDateStr: String,
    tomorrowDateStr: String,
    cityTz: TimeZone,
    now: Long
  ) {
    val adjustment = com.example.util.HijriDateHelper.getAdjustment(context)
    val todayDate = Date(now)
    val todayHijri = com.example.util.HijriDateHelper.getHijriDate(todayDate, adjustment)

    val prefs = context.getSharedPreferences("ramadan_prefs", Context.MODE_PRIVATE)
    val lastNotifiedYear = prefs.getInt("key_eid_mubarak_notified_year", 0)

    val isTodayLastDay = com.example.util.HijriDateHelper.isLastDayOfRamadan(todayDate, adjustment)

    val tomorrowCal = Calendar.getInstance().apply {
      time = todayDate
      add(Calendar.DAY_OF_YEAR, 1)
    }
    val isTomorrowLastDay = com.example.util.HijriDateHelper.isLastDayOfRamadan(tomorrowCal.time, adjustment)

    // Case 1: Today is the last day of Ramadan
    if (isTodayLastDay) {
      val todayMaghribMillis = parsePrayerTimeToMillis(todayDateStr, todayEntity.maghrib, cityTz)
      // Disappears 1 hour after final Iftar (Maghrib)
      val disappearMillis = todayMaghribMillis + 60 * 60 * 1000L

      if (lastNotifiedYear != todayHijri.year) {
        if (now < disappearMillis) {
          val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { this.timeZone = cityTz }.format(Date(disappearMillis))
          setExactRamadanAlarm(context, alarmManager, disappearMillis, REQUEST_CODE_EID_MUBARAK, EVENT_EID_MUBARAK, timeFmt)
          return
        } else if (now in disappearMillis..(disappearMillis + 14 * 3600 * 1000L)) {
          // If we passed disappearance today and have not notified yet (e.g. app freshly opened or device restarted):
          PrayerNotificationHelper.showEidMubarakNotification(context)
          prefs.edit().putInt("key_eid_mubarak_notified_year", todayHijri.year).apply()
          cancelRamadanAlarm(context, REQUEST_CODE_EID_MUBARAK, EVENT_EID_MUBARAK)
          return
        }
      }
    } else if (isTomorrowLastDay) {
      // Case 2: Tomorrow is the last day of Ramadan
      val tomorrowMaghribMillis = parsePrayerTimeToMillis(tomorrowDateStr, tomorrowEntity.maghrib, cityTz)
      val tomorrowDisappearMillis = tomorrowMaghribMillis + 60 * 60 * 1000L
      val tomorrowHijri = com.example.util.HijriDateHelper.getHijriDate(tomorrowCal.time, adjustment)

      if (lastNotifiedYear != tomorrowHijri.year && now < tomorrowDisappearMillis) {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { this.timeZone = cityTz }.format(Date(tomorrowDisappearMillis))
        setExactRamadanAlarm(context, alarmManager, tomorrowDisappearMillis, REQUEST_CODE_EID_MUBARAK, EVENT_EID_MUBARAK, timeFmt)
        return
      }
    } else if (todayHijri.month == 10 && todayHijri.day == 1 && lastNotifiedYear != todayHijri.year) {
      // Case 3: 1st of Shawwal (Eid Day) and haven't notified yet
      PrayerNotificationHelper.showEidMubarakNotification(context)
      prefs.edit().putInt("key_eid_mubarak_notified_year", todayHijri.year).apply()
      cancelRamadanAlarm(context, REQUEST_CODE_EID_MUBARAK, EVENT_EID_MUBARAK)
      return
    }

    cancelRamadanAlarm(context, REQUEST_CODE_EID_MUBARAK, EVENT_EID_MUBARAK)
  }

  fun scheduleTestRamadanAlarm(context: Context, delaySeconds: Int = 10, eventType: String = "SUHOOR_SOON") {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
    val reqCode = if (eventType == "SUHOOR_SOON") REQUEST_CODE_SUHOOR_SOON else REQUEST_CODE_IFTAR_SOON

    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_RAMADAN_ALARM
      data = Uri.parse("ramadan://test/$eventType")
      putExtra(EXTRA_RAMADAN_EVENT, eventType)
      putExtra(EXTRA_PRAYER_NAME, eventType)
      putExtra(EXTRA_PRAYER_TIME, "Exact Alarm Verified")
      putExtra(EXTRA_NOTIFICATION_ID, reqCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      reqCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      }
      Log.d(TAG, "Scheduled test Ramadan alarm for $eventType in $delaySeconds seconds")
    } catch (e: SecurityException) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  /**
   * Schedule a quick test alarm to demonstrate waking up and firing exact notification while minimized.
   */
  fun scheduleTestAlarm(context: Context, delaySeconds: Int = 10) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_TEST_ALARM
      putExtra(EXTRA_PRAYER_NAME, "Test Prayer Notification")
      putExtra(EXTRA_PRAYER_TIME, "Exact Alarm Verified")
      putExtra(EXTRA_NOTIFICATION_ID, REQUEST_CODE_TEST)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_TEST,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      }
      Log.d(TAG, "Scheduled test alarm for $delaySeconds seconds in the future")
    } catch (e: SecurityException) {
      Log.e(TAG, "Exact alarm permission issue", e)
      alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  private fun setExactAlarm(
    context: Context,
    alarmManager: AlarmManager,
    triggerMillis: Long,
    requestCode: Int,
    prayerName: String,
    prayerTime: String,
    isForbidden: Boolean = false
  ) {
    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_PRAYER_ALARM
      putExtra(EXTRA_PRAYER_NAME, prayerName)
      putExtra(EXTRA_PRAYER_TIME, prayerTime)
      putExtra(EXTRA_NOTIFICATION_ID, requestCode)
      putExtra(EXTRA_IS_FORBIDDEN, isForbidden)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      }
      Log.d(TAG, "Scheduled exact alarm for $prayerName at $triggerMillis")
    } catch (e: SecurityException) {
      Log.e(TAG, "Exact alarm not allowed, falling back to setAndAllowWhileIdle", e)
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  fun cancelAlarm(context: Context, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
      action = ACTION_PRAYER_ALARM
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
    }
  }

  fun parsePrayerTimeToMillis(dateStr: String, timeStr: String, timeZone: TimeZone? = null): Long {
    // dateStr e.g. "2026-09-22", timeStr e.g. "05:12" or "05:12 (EEST)"
    val cleanTime = timeStr.trim().split(" ")[0]
    val fullStr = "$dateStr $cleanTime"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
      if (timeZone != null) {
        this.timeZone = timeZone
      }
    }
    return try {
      sdf.parse(fullStr)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
      System.currentTimeMillis()
    }
  }

  data class VoluntaryTimes(
    val ishraq: String,
    val duha: String,
    val tahajjud: String
  )

  fun computeVoluntaryTimes(prayerEntity: PrayerEntity, timeZone: TimeZone? = null): VoluntaryTimes {
    val date = prayerEntity.date
    val sunriseMillis = parsePrayerTimeToMillis(date, prayerEntity.sunrise, timeZone)
    val dhuhrMillis = parsePrayerTimeToMillis(date, prayerEntity.dhuhr, timeZone)
    val maghribMillis = parsePrayerTimeToMillis(date, prayerEntity.maghrib, timeZone)
    val fajrMillis = parsePrayerTimeToMillis(date, prayerEntity.fajr, timeZone)

    // Ishraq: 15 minutes after sunrise
    val ishraqMillis = sunriseMillis + (15 * 60 * 1000L)

    // Duha: midpoint between sunrise and dhuhr
    val duhaMillis = (sunriseMillis + dhuhrMillis) / 2

    // Tahajjud: last third of the night (between Maghrib and tomorrow's Fajr)
    val nextFajrMillis = fajrMillis + (24 * 3600 * 1000L)
    val nightDuration = (nextFajrMillis - maghribMillis).coerceAtLeast(6 * 3600 * 1000L)
    val tahajjudMillis = nextFajrMillis - (nightDuration / 3)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
      if (timeZone != null) this.timeZone = timeZone
    }
    return VoluntaryTimes(
      ishraq = timeFormat.format(java.util.Date(ishraqMillis)),
      duha = timeFormat.format(java.util.Date(duhaMillis)),
      tahajjud = timeFormat.format(java.util.Date(tahajjudMillis))
    )
  }

  data class ForbiddenScheduleItem(
    val name: String,
    val startMillis: Long,
    val intervalFormatted: String,
    val requestCode: Int
  )

  fun computeForbiddenTimesForScheduling(prayerEntity: PrayerEntity, timeZone: TimeZone? = null): List<ForbiddenScheduleItem> {
    val date = prayerEntity.date
    val sunriseMillis = parsePrayerTimeToMillis(date, prayerEntity.sunrise, timeZone)
    val dhuhrMillis = parsePrayerTimeToMillis(date, prayerEntity.dhuhr, timeZone)
    val maghribMillis = parsePrayerTimeToMillis(date, prayerEntity.maghrib, timeZone)

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US).apply {
      if (timeZone != null) this.timeZone = timeZone
    }
    val ishraqMillis = sunriseMillis + (15 * 60 * 1000L)
    val zenithStartMillis = dhuhrMillis - (15 * 60 * 1000L)
    val sunsetStartMillis = maghribMillis - (15 * 60 * 1000L)

    return listOf(
      ForbiddenScheduleItem(
        name = "Sunrise Transition",
        startMillis = sunriseMillis,
        intervalFormatted = "${timeFormat.format(java.util.Date(sunriseMillis))} - ${timeFormat.format(java.util.Date(ishraqMillis))}",
        requestCode = REQUEST_CODE_FORBIDDEN_SUNRISE
      ),
      ForbiddenScheduleItem(
        name = "Midday Solar Zenith",
        startMillis = zenithStartMillis,
        intervalFormatted = "${timeFormat.format(java.util.Date(zenithStartMillis))} - ${timeFormat.format(java.util.Date(dhuhrMillis))}",
        requestCode = REQUEST_CODE_FORBIDDEN_ZENITH
      ),
      ForbiddenScheduleItem(
        name = "Sunset Transition",
        startMillis = sunsetStartMillis,
        intervalFormatted = "${timeFormat.format(java.util.Date(sunsetStartMillis))} - ${timeFormat.format(java.util.Date(maghribMillis))}",
        requestCode = REQUEST_CODE_FORBIDDEN_SUNSET
      )
    )
  }
}
