package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.PrayerEntity
import com.example.receiver.PrayerAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PrayerAlarmScheduler {
  private const val TAG = "PrayerAlarmScheduler"

  const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
  const val ACTION_TEST_ALARM = "com.example.ACTION_TEST_ALARM"

  const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
  const val EXTRA_PRAYER_TIME = "EXTRA_PRAYER_TIME"
  const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
  const val EXTRA_IS_FORBIDDEN = "EXTRA_IS_FORBIDDEN"

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
  const val REQUEST_CODE_TEST = 999

  /**
   * Schedules exact alarms for all enabled prayers for the day, including voluntary prayers
   * such as Ishraq, Duha, and Tahajjud, plus optional Forbidden Prayer Time warnings.
   * Wakes the device via AlarmManager.setExactAndAllowWhileIdle at the exact minute.
   * Consumes ZERO battery while waiting because no background service or process runs.
   */
  fun scheduleAlarmsForToday(
    context: Context,
    prayerEntity: PrayerEntity,
    enabledPrayers: Set<String> = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
    enabledForbidden: Set<String> = emptySet()
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val now = System.currentTimeMillis()

    val voluntary = computeVoluntaryTimes(prayerEntity)

    val prayers = listOf(
      Triple("Fajr", prayerEntity.fajr, REQUEST_CODE_FAJR),
      Triple("Ishraq", voluntary.ishraq, REQUEST_CODE_ISHRAQ),
      Triple("Duha", voluntary.duha, REQUEST_CODE_DUHA),
      Triple("Dhuhr", prayerEntity.dhuhr, REQUEST_CODE_DHUHR),
      Triple("Asr", prayerEntity.asr, REQUEST_CODE_ASR),
      Triple("Maghrib", prayerEntity.maghrib, REQUEST_CODE_MAGHRIB),
      Triple("Isha", prayerEntity.isha, REQUEST_CODE_ISHA),
      Triple("Tahajjud", voluntary.tahajjud, REQUEST_CODE_TAHAJJUD)
    )

    for ((name, timeStr, reqCode) in prayers) {
      if (!enabledPrayers.contains(name)) {
        cancelAlarm(context, reqCode)
        continue
      }

      val triggerMillis = parsePrayerTimeToMillis(prayerEntity.date, timeStr)
      if (triggerMillis > now) {
        setExactAlarm(
          context = context,
          alarmManager = alarmManager,
          triggerMillis = triggerMillis,
          requestCode = reqCode,
          prayerName = name,
          prayerTime = timeStr,
          isForbidden = false
        )
      } else {
        // Today's prayer has already passed, check tomorrow
        val tomorrowCalendar = Calendar.getInstance().apply {
          timeInMillis = triggerMillis
          add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowMillis = tomorrowCalendar.timeInMillis
        if (tomorrowMillis > now) {
          setExactAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerMillis = tomorrowMillis,
            requestCode = reqCode,
            prayerName = name,
            prayerTime = timeStr,
            isForbidden = false
          )
        }
      }
    }

    // Schedule Forbidden Prayer Times alarms
    val forbiddenItems = computeForbiddenTimesForScheduling(prayerEntity)
    for (item in forbiddenItems) {
      if (!enabledForbidden.contains(item.name)) {
        cancelAlarm(context, item.requestCode)
        continue
      }

      val triggerMillis = item.startMillis
      if (triggerMillis > now) {
        setExactAlarm(
          context = context,
          alarmManager = alarmManager,
          triggerMillis = triggerMillis,
          requestCode = item.requestCode,
          prayerName = item.name,
          prayerTime = item.intervalFormatted,
          isForbidden = true
        )
      } else {
        val tomorrowCalendar = Calendar.getInstance().apply {
          timeInMillis = triggerMillis
          add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowMillis = tomorrowCalendar.timeInMillis
        if (tomorrowMillis > now) {
          setExactAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerMillis = tomorrowMillis,
            requestCode = item.requestCode,
            prayerName = item.name,
            prayerTime = item.intervalFormatted,
            isForbidden = true
          )
        }
      }
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

  fun parsePrayerTimeToMillis(dateStr: String, timeStr: String): Long {
    // dateStr e.g. "2026-09-22", timeStr e.g. "05:12" or "05:12 (EEST)"
    val cleanTime = timeStr.trim().split(" ")[0]
    val fullStr = "$dateStr $cleanTime"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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

  fun computeVoluntaryTimes(prayerEntity: PrayerEntity): VoluntaryTimes {
    val date = prayerEntity.date
    val sunriseMillis = parsePrayerTimeToMillis(date, prayerEntity.sunrise)
    val dhuhrMillis = parsePrayerTimeToMillis(date, prayerEntity.dhuhr)
    val maghribMillis = parsePrayerTimeToMillis(date, prayerEntity.maghrib)
    val fajrMillis = parsePrayerTimeToMillis(date, prayerEntity.fajr)

    // Ishraq: 15 minutes after sunrise
    val ishraqMillis = sunriseMillis + (15 * 60 * 1000L)

    // Duha: midpoint between sunrise and dhuhr
    val duhaMillis = (sunriseMillis + dhuhrMillis) / 2

    // Tahajjud: last third of the night (between Maghrib and tomorrow's Fajr)
    val nextFajrMillis = fajrMillis + (24 * 3600 * 1000L)
    val nightDuration = (nextFajrMillis - maghribMillis).coerceAtLeast(6 * 3600 * 1000L)
    val tahajjudMillis = nextFajrMillis - (nightDuration / 3)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
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

  fun computeForbiddenTimesForScheduling(prayerEntity: PrayerEntity): List<ForbiddenScheduleItem> {
    val date = prayerEntity.date
    val sunriseMillis = parsePrayerTimeToMillis(date, prayerEntity.sunrise)
    val dhuhrMillis = parsePrayerTimeToMillis(date, prayerEntity.dhuhr)
    val maghribMillis = parsePrayerTimeToMillis(date, prayerEntity.maghrib)

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
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
