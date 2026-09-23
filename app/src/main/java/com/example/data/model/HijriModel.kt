package com.example.data.model

data class HijriDate(
  val day: Int,
  val month: Int,
  val monthNameEn: String,
  val monthNameAr: String,
  val monthNameBn: String,
  val year: Int,
  val events: List<String> = emptyList(),
  val designation: String = "AH"
)

data class HijriCalendarDay(
  val hijriDay: Int,
  val hijriMonth: Int,
  val hijriYear: Int,
  val gregorianDay: Int,
  val gregorianMonth: Int, // 1-12
  val gregorianYear: Int,
  val dayOfWeek: Int,      // 1=Sunday, 7=Saturday
  val isCurrentMonth: Boolean = true,
  val isToday: Boolean = false,
  val isWhiteDay: Boolean = false, // 13, 14, 15 of Hijri month
  val isFriday: Boolean = false,
  val isMondayOrThursday: Boolean = false,
  val events: List<IslamicEvent> = emptyList()
)

data class IslamicEvent(
  val titleEn: String,
  val titleBn: String,
  val titleAr: String,
  val descriptionEn: String = "",
  val descriptionBn: String = "",
  val isMajorHoliday: Boolean = false,
  val isFastingRecommended: Boolean = false
)

data class HijriMonthData(
  val hijriYear: Int,
  val hijriMonth: Int,
  val hijriMonthNameEn: String,
  val hijriMonthNameAr: String,
  val hijriMonthNameBn: String,
  val gregorianSpanEn: String,
  val gregorianSpanBn: String,
  val gregorianSpanAr: String,
  val days: List<HijriCalendarDay>,
  val isFromWeb: Boolean = false
)
