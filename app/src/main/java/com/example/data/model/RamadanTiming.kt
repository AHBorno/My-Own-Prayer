package com.example.data.model

data class RamadanTiming(
  val suhoorEndTime: String = "--:--",
  val suhoorSoonTime: String = "--:--",
  val iftarTime: String = "--:--",
  val iftarSoonTime: String = "--:--",
  val suhoorEndMillis: Long = 0L,
  val suhoorSoonMillis: Long = 0L,
  val iftarMillis: Long = 0L,
  val iftarSoonMillis: Long = 0L,
  val suhoorCountdown: String = "--:--:--",
  val iftarCountdown: String = "--:--:--",
  val activeStage: RamadanStage = RamadanStage.FASTING_DAY,
  val hijriDay: Int = 1,
  val hijriMonth: Int = 9,
  val hijriYear: Int = 1447,
  val isDayBeforeRamadan: Boolean = false,
  val isRamadanActive: Boolean = false,
  val suhoorNotificationEnabled: Boolean = true,
  val iftarNotificationEnabled: Boolean = true
)

enum class RamadanStage {
  SUHOOR_TIME,     // Active Suhoor meal window (before Fajr)
  FASTING_DAY,     // Daylight fasting period (Fajr to Maghrib)
  IFTAR_TIME,      // Iftar moment and post-fast celebration
  WAITING_SUHOOR   // Night period awaiting next Suhoor
}
