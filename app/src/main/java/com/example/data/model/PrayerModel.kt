package com.example.data.model

import com.example.util.AppLanguageHelper

enum class PrayerType(
  val displayName: String,
  val arabicName: String,
  val bengaliName: String,
  val isVoluntary: Boolean = false,
  val description: String = ""
) {
  FAJR("Fajr", "الفجر", "ফজর", false, "Dawn prayer until sunrise"),
  SUNRISE("Sunrise", "الشروق", "সূর্যোদয়", false, "Solar horizon transit"),
  ISHRAQ("Ishraq", "الإشراق", "ইশরাক", true, "15 min after sunrise (Sunnah)"),
  DUHA("Duha", "الضحى", "চাশত (দুহা)", true, "Forenoon prayer until solar zenith (Sunnah)"),
  DHUHR("Dhuhr", "الظهر", "যোহর", false, "Midday obligatory prayer"),
  ASR("Asr", "العصر", "আসর", false, "Late afternoon prayer until sunset transition"),
  MAGHRIB("Maghrib", "المغرب", "মাগরিব", false, "Sunset obligatory prayer"),
  ISHA("Isha", "العشاء", "এশা", false, "Night obligatory prayer"),
  TAHAJJUD("Tahajjud", "التهجد", "তাহাজ্জুদ", true, "Last third of night (Qiyam al-Layl)");

  fun getLocalizedName(lang: String): String {
    return AppLanguageHelper.getPrayerDisplayName(this, lang)
  }

  fun getLocalizedDescription(lang: String): String {
    return AppLanguageHelper.getPrayerDescription(this, lang)
  }

  companion object {
    fun fromName(name: String): PrayerType? {
      return entries.find {
        it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true)
      }
    }
  }
}

data class PrayerItem(
  val type: PrayerType,
  val timeFormatted: String, // e.g. "05:12 AM"
  val timeMillis: Long,       // Epoch millis today
  val endTimeFormatted: String? = null, // e.g. "05:47 AM"
  val endTimeMillis: Long? = null,
  val isPassed: Boolean = false,
  val isCurrent: Boolean = false,
  val isNext: Boolean = false,
  val isMakruh: Boolean = false,
  val notificationEnabled: Boolean = true
)

data class SolarTimes(
  val sunriseFormatted: String = "--:--",
  val sunsetFormatted: String = "--:--",
  val sunriseMillis: Long = 0L,
  val sunsetMillis: Long = 0L
)

data class ForbiddenTimeItem(
  val name: String,
  val arabicName: String = "",
  val bengaliName: String = "",
  val intervalFormatted: String, // e.g. "06:10 AM - 06:25 AM"
  val description: String,
  val startMillis: Long,
  val endMillis: Long,
  val isActiveNow: Boolean = false,
  val notificationEnabled: Boolean = false
) {
  fun getLocalizedName(lang: String): String {
    return AppLanguageHelper.getForbiddenName(name, lang)
  }

  fun getLocalizedDescription(lang: String): String {
    return AppLanguageHelper.getForbiddenDescription(name, lang)
  }
}

enum class RamadanDisplayMode(val id: String) {
  AUTO("auto"),
  ALWAYS_VISIBLE("always"),
  OFF("off");

  companion object {
    fun fromId(id: String): RamadanDisplayMode {
      return entries.find { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
  }
}

data class RamadanTimingInfo(
  val isVisible: Boolean = false,
  val isDayBeforeRamadan: Boolean = false,
  val isRamadanActive: Boolean = false,
  val hijriDay: Int = 0,
  val suhoorEndTimeFormatted: String = "--:--",
  val suhoorEndMillis: Long = 0L,
  val iftarTimeFormatted: String = "--:--",
  val iftarMillis: Long = 0L,
  val countdownLabel: String = "",
  val countdownValue: String = "--:--:--",
  val isFastingInProgress: Boolean = false,
  val notificationIftarSoon: Boolean = true,
  val notificationSuhoorSoon: Boolean = true,
  val isPreviewMode: Boolean = false
)
