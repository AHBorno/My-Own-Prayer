package com.example.util

import android.content.Context
import com.example.data.local.PrayerEntity
import com.example.data.model.RamadanStage
import com.example.data.model.RamadanTiming
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object RamadanHelper {

  /**
   * Checks whether the Ramadan Iftar & Suhoor section should be active:
   * - One day before Ramadan (e.g., Sha'ban 29/30 or when tomorrow is Ramadan 1)
   * - During Ramadan (Ramadan 1 through the end of Ramadan)
   * - Disappears on the conclusion of the last day of Ramadan (after the final Iftar / Shawwal 1)
   * - Can be force-previewed via test settings
   */
  fun isRamadanActive(
    context: Context,
    prayerEntity: PrayerEntity? = null,
    now: Long = System.currentTimeMillis(),
    forcePreview: Boolean = false
  ): Boolean {
    if (forcePreview) return true

    val adjustment = HijriDateHelper.getAdjustment(context)
    val todayDate = Date(now)
    val todayHijri = HijriDateHelper.getHijriDate(todayDate, adjustment)

    val tomorrowCal = Calendar.getInstance().apply {
      time = todayDate
      add(Calendar.DAY_OF_YEAR, 1)
    }
    val tomorrowHijri = HijriDateHelper.getHijriDate(tomorrowCal.time, adjustment)

    // 1. One day before Ramadan (Sha'ban 29/30 or whenever tomorrow is Ramadan 1)
    val isDayBeforeRamadan = (todayHijri.month == 8 && tomorrowHijri.month == 9) ||
        (todayHijri.month == 8 && todayHijri.day >= 29)

    // 2. During Ramadan (Month 9)
    if (todayHijri.month == 9) {
      // If it is the last day of Ramadan (tomorrow is Shawwal month 10 or day 30):
      val isLastDayOfRamadan = tomorrowHijri.month == 10 || todayHijri.day >= 30
      if (isLastDayOfRamadan && prayerEntity != null) {
        val iftarMillis = parsePrayerTimeToMillis(prayerEntity.date, prayerEntity.maghrib)
        // Disappears after the final Iftar on the last day of Ramadan
        if (now > iftarMillis + 60 * 60 * 1000L) {
          return false
        }
      }
      return true
    }

    return isDayBeforeRamadan
  }

  fun computeRamadanTiming(
    context: Context,
    prayerEntity: PrayerEntity,
    now: Long = System.currentTimeMillis(),
    forcePreview: Boolean = false,
    suhoorEnabled: Boolean = true,
    iftarEnabled: Boolean = true
  ): RamadanTiming {
    val adjustment = HijriDateHelper.getAdjustment(context)
    val todayDate = Date(now)
    val todayHijri = HijriDateHelper.getHijriDate(todayDate, adjustment)

    val tomorrowCal = Calendar.getInstance().apply {
      time = todayDate
      add(Calendar.DAY_OF_YEAR, 1)
    }
    val tomorrowHijri = HijriDateHelper.getHijriDate(tomorrowCal.time, adjustment)

    val isDayBefore = (todayHijri.month == 8 && tomorrowHijri.month == 9) ||
        (todayHijri.month == 8 && todayHijri.day >= 29)

    val suhoorEndMillis = parsePrayerTimeToMillis(prayerEntity.date, prayerEntity.fajr)
    val suhoorSoonMillis = suhoorEndMillis - 15 * 60 * 1000L

    val iftarMillis = parsePrayerTimeToMillis(prayerEntity.date, prayerEntity.maghrib)
    val iftarSoonMillis = iftarMillis - 14 * 60 * 1000L

    val suhoorEndTimeFormatted = format12h(prayerEntity.fajr)
    val suhoorSoonTimeFormatted = formatMillisTo12h(suhoorSoonMillis)

    val iftarTimeFormatted = format12h(prayerEntity.maghrib)
    val iftarSoonTimeFormatted = formatMillisTo12h(iftarSoonMillis)

    val activeStage: RamadanStage
    val suhoorCountdown: String
    val iftarCountdown: String

    when {
      now < suhoorEndMillis -> {
        // Night before Fajr: active Suhoor window
        activeStage = if (suhoorEndMillis - now <= 30 * 60 * 1000L) {
          RamadanStage.SUHOOR_TIME
        } else {
          RamadanStage.WAITING_SUHOOR
        }
        suhoorCountdown = formatCountdown(suhoorEndMillis - now)
        iftarCountdown = formatCountdown(iftarMillis - now)
      }
      now in suhoorEndMillis..iftarMillis -> {
        // Daytime between Fajr and Maghrib: active fasting
        activeStage = RamadanStage.FASTING_DAY
        suhoorCountdown = "--:--:--"
        iftarCountdown = formatCountdown(iftarMillis - now)
      }
      now in iftarMillis..(iftarMillis + 60 * 60 * 1000L) -> {
        // Maghrib to +1 hour: Iftar moment
        activeStage = RamadanStage.IFTAR_TIME
        suhoorCountdown = "--:--:--"
        iftarCountdown = "--:--:--"
      }
      else -> {
        // Night after Iftar: waiting for tomorrow's Suhoor
        activeStage = RamadanStage.WAITING_SUHOOR
        val nextSuhoorMillis = suhoorEndMillis + 24 * 3600 * 1000L
        suhoorCountdown = formatCountdown(nextSuhoorMillis - now)
        iftarCountdown = "--:--:--"
      }
    }

    return RamadanTiming(
      suhoorEndTime = suhoorEndTimeFormatted,
      suhoorSoonTime = suhoorSoonTimeFormatted,
      iftarTime = iftarTimeFormatted,
      iftarSoonTime = iftarSoonTimeFormatted,
      suhoorEndMillis = suhoorEndMillis,
      suhoorSoonMillis = suhoorSoonMillis,
      iftarMillis = iftarMillis,
      iftarSoonMillis = iftarSoonMillis,
      suhoorCountdown = suhoorCountdown,
      iftarCountdown = iftarCountdown,
      activeStage = activeStage,
      hijriDay = if (isDayBefore) 30 else todayHijri.day,
      hijriMonth = if (isDayBefore) 8 else todayHijri.month,
      hijriYear = todayHijri.year,
      isDayBeforeRamadan = isDayBefore,
      isRamadanActive = true,
      suhoorNotificationEnabled = suhoorEnabled,
      iftarNotificationEnabled = iftarEnabled
    )
  }

  fun formatCountdown(diffMillis: Long): String {
    if (diffMillis <= 0) return "00:00:00"
    val hours = diffMillis / (1000 * 60 * 60)
    val mins = (diffMillis % (1000 * 60 * 60)) / (1000 * 60)
    val secs = (diffMillis % (1000 * 60)) / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
  }

  fun parsePrayerTimeToMillis(dateStr: String, timeStr: String): Long {
    return try {
      val cleanTime = timeStr.trim().split(" ")[0]
      val combined = "$dateStr $cleanTime"
      val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
      sdf.parse(combined)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
      System.currentTimeMillis()
    }
  }

  private fun formatMillisTo12h(millis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.US)
    return sdf.format(Date(millis))
  }

  fun format12h(time24: String): String {
    return try {
      val clean = time24.trim().split(" ")[0]
      val parts = clean.split(":")
      val hour = parts[0].toInt()
      val min = parts[1].toInt()
      val isPm = hour >= 12
      val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
      }
      String.format(Locale.US, "%02d:%02d\u00A0%s", h12, min, if (isPm) "PM" else "AM")
    } catch (_: Exception) {
      time24
    }
  }
}
