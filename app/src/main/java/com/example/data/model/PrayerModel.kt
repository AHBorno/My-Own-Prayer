package com.example.data.model

enum class PrayerType(
  val displayName: String,
  val arabicName: String,
  val isVoluntary: Boolean = false,
  val description: String = ""
) {
  FAJR("Fajr", "الفجر", false, "Dawn obligatory prayer"),
  SUNRISE("Sunrise", "الشروق", false, "Solar horizon transit"),
  ISHRAQ("Ishraq", "الإشراق", true, "15 min after sunrise (Sunnah)"),
  DUHA("Duha", "الضحى", true, "Forenoon prayer (Sunnah)"),
  DHUHR("Dhuhr", "الظهر", false, "Midday obligatory prayer"),
  ASR("Asr", "العصر", false, "Late afternoon obligatory prayer"),
  MAGHRIB("Maghrib", "المغرب", false, "Sunset obligatory prayer"),
  ISHA("Isha", "العشاء", false, "Night obligatory prayer"),
  TAHAJJUD("Tahajjud", "التهجد", true, "Last third of night (Qiyam al-Layl)");

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
  val isPassed: Boolean = false,
  val isCurrent: Boolean = false,
  val isNext: Boolean = false,
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
  val arabicName: String,
  val intervalFormatted: String, // e.g. "06:10 AM - 06:25 AM"
  val description: String,
  val startMillis: Long,
  val endMillis: Long,
  val isActiveNow: Boolean = false,
  val notificationEnabled: Boolean = false
)
