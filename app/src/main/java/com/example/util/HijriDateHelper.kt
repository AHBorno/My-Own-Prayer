package com.example.util

import android.content.Context
import com.example.data.model.HijriCalendarDay
import com.example.data.model.HijriDate
import com.example.data.model.HijriMonthData
import com.example.data.model.IslamicEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object HijriDateHelper {

  private val HIJRI_MONTHS_EN = listOf(
    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
    "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
  )

  private val HIJRI_MONTHS_AR = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
    "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
  )

  private val HIJRI_MONTHS_BN = listOf(
    "মুহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
    "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান",
    "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
  )

  private val GREG_MONTHS_BN = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
  )

  private val GREG_MONTHS_AR = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
  )

  private val GREG_MONTHS_EN = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  private val WEEKDAYS_BN = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
  private val WEEKDAYS_AR = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
  private val WEEKDAYS_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

  private val httpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .build()
  }

  private val webCache = mutableMapOf<String, HijriMonthData>()

  fun getAdjustment(context: Context): Int {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("hijri_adjustment_days", 0)
  }

  fun setAdjustment(context: Context, adjustment: Int) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("hijri_adjustment_days", adjustment).apply()
    webCache.clear()
  }

  /**
   * Convert Gregorian Date to HijriDate using Umm al-Qura standard astronomical algorithm.
   */
  fun getHijriDate(date: Date, adjustmentDays: Int = 0): HijriDate {
    val cal = Calendar.getInstance()
    cal.time = date
    if (adjustmentDays != 0) {
      cal.add(Calendar.DAY_OF_YEAR, adjustmentDays)
    }

    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1 // 1-12
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val jd = gregorianToJulianDay(year, month, day)
    val (hYear, hMonth, hDay) = julianDayToHijri(jd)

    val safeMonthIdx = (hMonth - 1).coerceIn(0, 11)
    val events = getIslamicEvents(hDay, hMonth)

    return HijriDate(
      day = hDay,
      month = hMonth,
      monthNameEn = HIJRI_MONTHS_EN[safeMonthIdx],
      monthNameAr = HIJRI_MONTHS_AR[safeMonthIdx],
      monthNameBn = HIJRI_MONTHS_BN[safeMonthIdx],
      year = hYear,
      events = events.map { it.titleEn }
    )
  }

  fun formatHijriDate(hijriDate: HijriDate, lang: String): String {
    val dayStr = AppLanguageHelper.localizeNumbers(hijriDate.day.toString(), lang)
    val yearStr = AppLanguageHelper.localizeNumbers(hijriDate.year.toString(), lang)
    return when (lang) {
      AppLanguageHelper.LANG_BN -> "$dayStr ${hijriDate.monthNameBn} $yearStr হিজরি"
      AppLanguageHelper.LANG_AR -> "$dayStr ${hijriDate.monthNameAr} $yearStr هـ"
      else -> "$dayStr ${hijriDate.monthNameEn} $yearStr AH"
    }
  }

  fun formatGregorianDate(date: Date, lang: String): String {
    val cal = Calendar.getInstance().apply { time = date }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
    val year = cal.get(Calendar.YEAR)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun .. 7=Sat

    val dayStr = AppLanguageHelper.localizeNumbers(day.toString(), lang)
    val yearStr = AppLanguageHelper.localizeNumbers(year.toString(), lang)

    return when (lang) {
      AppLanguageHelper.LANG_BN -> {
        val weekday = when (dayOfWeek) {
          Calendar.SUNDAY -> "রবিবার"
          Calendar.MONDAY -> "সোমবার"
          Calendar.TUESDAY -> "মঙ্গলবার"
          Calendar.WEDNESDAY -> "বুধবার"
          Calendar.THURSDAY -> "বৃহস্পতিবার"
          Calendar.FRIDAY -> "শুক্রবার"
          else -> "শনিবার"
        }
        "$weekday, $dayStr ${GREG_MONTHS_BN[monthIdx]} $yearStr"
      }
      AppLanguageHelper.LANG_AR -> {
        val weekday = when (dayOfWeek) {
          Calendar.SUNDAY -> "الأحد"
          Calendar.MONDAY -> "الاثنين"
          Calendar.TUESDAY -> "الثلاثاء"
          Calendar.WEDNESDAY -> "الأربعاء"
          Calendar.THURSDAY -> "الخميس"
          Calendar.FRIDAY -> "الجمعة"
          else -> "السبت"
        }
        "$weekday، $dayStr ${GREG_MONTHS_AR[monthIdx]} $yearStr"
      }
      else -> {
        val weekday = when (dayOfWeek) {
          Calendar.SUNDAY -> "Sun"
          Calendar.MONDAY -> "Mon"
          Calendar.TUESDAY -> "Tue"
          Calendar.WEDNESDAY -> "Wed"
          Calendar.THURSDAY -> "Thu"
          Calendar.FRIDAY -> "Fri"
          else -> "Sat"
        }
        "$weekday, ${GREG_MONTHS_EN[monthIdx].take(3)} $day, $year"
      }
    }
  }

  fun getWeekdayLabels(lang: String): List<String> {
    return when (lang) {
      AppLanguageHelper.LANG_BN -> WEEKDAYS_BN
      AppLanguageHelper.LANG_AR -> WEEKDAYS_AR
      else -> WEEKDAYS_EN
    }
  }

  /**
   * Generates calendar month data for a given Hijri month and year.
   * If online, attempts to fetch latest authoritative calendar from AlAdhan API and updates cache.
   */
  suspend fun getHijriMonthCalendar(
    targetHijriYear: Int,
    targetHijriMonth: Int,
    adjustmentDays: Int = 0,
    forceRefresh: Boolean = false
  ): HijriMonthData = withContext(Dispatchers.IO) {
    val cacheKey = "$targetHijriYear-$targetHijriMonth-$adjustmentDays"
    if (!forceRefresh && webCache.containsKey(cacheKey)) {
      return@withContext webCache[cacheKey]!!
    }

    // 1. First generate local astronomical month data as instant fallback
    val localData = computeLocalHijriMonth(targetHijriYear, targetHijriMonth, adjustmentDays)

    // 2. Try fetching from Web (AlAdhan API)
    try {
      val webData = fetchHijriMonthFromWeb(targetHijriYear, targetHijriMonth, adjustmentDays)
      if (webData != null) {
        webCache[cacheKey] = webData
        return@withContext webData
      }
    } catch (_: Exception) {
      // Fallback to local computed data
    }

    webCache[cacheKey] = localData
    return@withContext localData
  }

  private fun fetchHijriMonthFromWeb(hYear: Int, hMonth: Int, adjustment: Int): HijriMonthData? {
    // AlAdhan API endpoint: https://api.aladhan.com/v1/hijriCalendar/{year}/{month}?adjustment={adj}
    val url = "https://api.aladhan.com/v1/hijriCalendar/$hYear/$hMonth?adjustment=$adjustment"
    val request = Request.Builder().url(url).build()
    val response = httpClient.newCall(request).execute()
    if (!response.isSuccessful) return null

    val body = response.body?.string() ?: return null
    val rootJson = JSONObject(body)
    if (rootJson.optInt("code") != 200) return null

    val dataArray = rootJson.optJSONArray("data") ?: return null
    if (dataArray.length() == 0) return null

    val days = mutableListOf<HijriCalendarDay>()
    val todayCal = Calendar.getInstance()
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayYear = todayCal.get(Calendar.YEAR)

    var firstGregMonth = 1
    var firstGregYear = todayYear
    var lastGregMonth = 1
    var lastGregYear = todayYear

    for (i in 0 until dataArray.length()) {
      val dayObj = dataArray.getJSONObject(i)
      val hijriObj = dayObj.getJSONObject("date").getJSONObject("hijri")
      val gregObj = dayObj.getJSONObject("date").getJSONObject("gregorian")

      val hD = hijriObj.getString("day").toIntOrNull() ?: (i + 1)
      val hM = hijriObj.getJSONObject("month").getInt("number")
      val hY = hijriObj.getString("year").toIntOrNull() ?: hYear

      val gD = gregObj.getString("day").toIntOrNull() ?: 1
      val gM = gregObj.getJSONObject("month").getInt("number")
      val gY = gregObj.getString("year").toIntOrNull() ?: todayYear

      val cal = Calendar.getInstance().apply { set(gY, gM - 1, gD) }
      val dow = cal.get(Calendar.DAY_OF_WEEK)

      if (i == 0) {
        firstGregMonth = gM
        firstGregYear = gY
      }
      if (i == dataArray.length() - 1) {
        lastGregMonth = gM
        lastGregYear = gY
      }

      val isToday = (gD == todayDay && gM == todayMonth && gY == todayYear)
      val isWhiteDay = (hD in 13..15)
      val isFriday = (dow == Calendar.FRIDAY)
      val isMonOrThu = (dow == Calendar.MONDAY || dow == Calendar.THURSDAY)
      val events = getIslamicEvents(hD, hM)

      days.add(
        HijriCalendarDay(
          hijriDay = hD,
          hijriMonth = hM,
          hijriYear = hY,
          gregorianDay = gD,
          gregorianMonth = gM,
          gregorianYear = gY,
          dayOfWeek = dow,
          isCurrentMonth = true,
          isToday = isToday,
          isWhiteDay = isWhiteDay,
          isFriday = isFriday,
          isMondayOrThursday = isMonOrThu,
          events = events
        )
      )
    }

    // Pad beginning of week (if 1st day of month is not Sunday)
    val firstDayDow = days.first().dayOfWeek
    val paddedDays = mutableListOf<HijriCalendarDay>()
    if (firstDayDow > Calendar.SUNDAY) {
      val calFirst = Calendar.getInstance().apply {
        set(days.first().gregorianYear, days.first().gregorianMonth - 1, days.first().gregorianDay)
      }
      for (offset in (firstDayDow - 1) downTo 1) {
        val prevCal = Calendar.getInstance().apply {
          timeInMillis = calFirst.timeInMillis - (offset * 24 * 3600 * 1000L)
        }
        val prevHijri = getHijriDate(prevCal.time, adjustment)
        paddedDays.add(
          HijriCalendarDay(
            hijriDay = prevHijri.day,
            hijriMonth = prevHijri.month,
            hijriYear = prevHijri.year,
            gregorianDay = prevCal.get(Calendar.DAY_OF_MONTH),
            gregorianMonth = prevCal.get(Calendar.MONTH) + 1,
            gregorianYear = prevCal.get(Calendar.YEAR),
            dayOfWeek = prevCal.get(Calendar.DAY_OF_WEEK),
            isCurrentMonth = false,
            isToday = false,
            isWhiteDay = false,
            events = emptyList()
          )
        )
      }
    }
    paddedDays.addAll(days)

    val monthIdx = (hMonth - 1).coerceIn(0, 11)
    val spanEn = "${GREG_MONTHS_EN[firstGregMonth - 1].take(3)} $firstGregYear - ${GREG_MONTHS_EN[lastGregMonth - 1].take(3)} $lastGregYear"
    val spanBn = "${GREG_MONTHS_BN[firstGregMonth - 1]} - ${GREG_MONTHS_BN[lastGregMonth - 1]} $lastGregYear"
    val spanAr = "${GREG_MONTHS_AR[firstGregMonth - 1]} - ${GREG_MONTHS_AR[lastGregMonth - 1]} $lastGregYear"

    return HijriMonthData(
      hijriYear = hYear,
      hijriMonth = hMonth,
      hijriMonthNameEn = HIJRI_MONTHS_EN[monthIdx],
      hijriMonthNameAr = HIJRI_MONTHS_AR[monthIdx],
      hijriMonthNameBn = HIJRI_MONTHS_BN[monthIdx],
      gregorianSpanEn = spanEn,
      gregorianSpanBn = spanBn,
      gregorianSpanAr = spanAr,
      days = paddedDays,
      isFromWeb = true
    )
  }

  private fun computeLocalHijriMonth(hYear: Int, hMonth: Int, adjustment: Int): HijriMonthData {
    val monthIdx = (hMonth - 1).coerceIn(0, 11)
    val daysInMonth = if (hMonth % 2 == 1 || (hMonth == 12 && isHijriLeapYear(hYear))) 30 else 29

    val approxGregDate = hijriToApproximateGregorian(hYear, hMonth, 1)
    val cal = Calendar.getInstance().apply { time = approxGregDate }

    // Align to exact 1st of Hijri month
    var attempts = 0
    while (attempts < 10) {
      val h = getHijriDate(cal.time, adjustment)
      if (h.year == hYear && h.month == hMonth && h.day == 1) break
      if (h.year < hYear || (h.year == hYear && h.month < hMonth) || (h.year == hYear && h.month == hMonth && h.day > 15)) {
        cal.add(Calendar.DAY_OF_YEAR, 1)
      } else if (h.day > 1) {
        cal.add(Calendar.DAY_OF_YEAR, -(h.day - 1))
      } else {
        cal.add(Calendar.DAY_OF_YEAR, 1)
      }
      attempts++
    }

    val todayCal = Calendar.getInstance()
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayYear = todayCal.get(Calendar.YEAR)

    val days = mutableListOf<HijriCalendarDay>()
    val startCal = cal.clone() as Calendar

    for (d in 1..daysInMonth) {
      val gD = cal.get(Calendar.DAY_OF_MONTH)
      val gM = cal.get(Calendar.MONTH) + 1
      val gY = cal.get(Calendar.YEAR)
      val dow = cal.get(Calendar.DAY_OF_WEEK)

      val isToday = (gD == todayDay && gM == todayMonth && gY == todayYear)
      val isWhiteDay = (d in 13..15)
      val isFriday = (dow == Calendar.FRIDAY)
      val isMonOrThu = (dow == Calendar.MONDAY || dow == Calendar.THURSDAY)
      val events = getIslamicEvents(d, hMonth)

      days.add(
        HijriCalendarDay(
          hijriDay = d,
          hijriMonth = hMonth,
          hijriYear = hYear,
          gregorianDay = gD,
          gregorianMonth = gM,
          gregorianYear = gY,
          dayOfWeek = dow,
          isCurrentMonth = true,
          isToday = isToday,
          isWhiteDay = isWhiteDay,
          isFriday = isFriday,
          isMondayOrThursday = isMonOrThu,
          events = events
        )
      )
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    val firstDayDow = days.first().dayOfWeek
    val paddedDays = mutableListOf<HijriCalendarDay>()
    if (firstDayDow > Calendar.SUNDAY) {
      for (offset in (firstDayDow - 1) downTo 1) {
        val prevCal = (startCal.clone() as Calendar).apply {
          add(Calendar.DAY_OF_YEAR, -offset)
        }
        val prevHijri = getHijriDate(prevCal.time, adjustment)
        paddedDays.add(
          HijriCalendarDay(
            hijriDay = prevHijri.day,
            hijriMonth = prevHijri.month,
            hijriYear = prevHijri.year,
            gregorianDay = prevCal.get(Calendar.DAY_OF_MONTH),
            gregorianMonth = prevCal.get(Calendar.MONTH) + 1,
            gregorianYear = prevCal.get(Calendar.YEAR),
            dayOfWeek = prevCal.get(Calendar.DAY_OF_WEEK),
            isCurrentMonth = false,
            isToday = false,
            isWhiteDay = false,
            events = emptyList()
          )
        )
      }
    }
    paddedDays.addAll(days)

    val firstG = days.first()
    val lastG = days.last()
    val spanEn = "${GREG_MONTHS_EN[firstG.gregorianMonth - 1].take(3)} - ${GREG_MONTHS_EN[lastG.gregorianMonth - 1].take(3)} ${lastG.gregorianYear}"
    val spanBn = "${GREG_MONTHS_BN[firstG.gregorianMonth - 1]} - ${GREG_MONTHS_BN[lastG.gregorianMonth - 1]} ${lastG.gregorianYear}"
    val spanAr = "${GREG_MONTHS_AR[firstG.gregorianMonth - 1]} - ${GREG_MONTHS_AR[lastG.gregorianMonth - 1]} ${lastG.gregorianYear}"

    return HijriMonthData(
      hijriYear = hYear,
      hijriMonth = hMonth,
      hijriMonthNameEn = HIJRI_MONTHS_EN[monthIdx],
      hijriMonthNameAr = HIJRI_MONTHS_AR[monthIdx],
      hijriMonthNameBn = HIJRI_MONTHS_BN[monthIdx],
      gregorianSpanEn = spanEn,
      gregorianSpanBn = spanBn,
      gregorianSpanAr = spanAr,
      days = paddedDays,
      isFromWeb = false
    )
  }

  fun getIslamicEvents(hDay: Int, hMonth: Int): List<IslamicEvent> {
    val events = mutableListOf<IslamicEvent>()

    when (hMonth) {
      1 -> { // Muharram
        if (hDay == 1) events.add(IslamicEvent("Islamic New Year", "হিজরি নববর্ষ", "رأس السنة الهجرية", isMajorHoliday = true))
        if (hDay in 9..10) events.add(IslamicEvent("Ashura & Tasu'a", "আশুরা ও তাসূআ", "عاشوراء وتاسوعاء", isFastingRecommended = true))
      }
      3 -> { // Rabi' al-Awwal
        if (hDay == 12) events.add(IslamicEvent("Mawlid an-Nabi", "ঈদে মিলাদুন্নবী (সা.)", "المولد النبوي الشريف", isMajorHoliday = true))
      }
      7 -> { // Rajab
        if (hDay == 27) events.add(IslamicEvent("Isra and Mi'raj", "শবে মেরাজ", "الإسراء والمعراج", isMajorHoliday = true))
      }
      8 -> { // Sha'ban
        if (hDay == 15) events.add(IslamicEvent("Shab-e-Barat (Mid-Sha'ban)", "শবে বরাত (লাইলাতুল বারাআত)", "ليلة النصف من شعبان", isFastingRecommended = true))
      }
      9 -> { // Ramadan
        if (hDay == 1) events.add(IslamicEvent("1st of Ramadan", "রমজানের ১ম দিন", "أول أيام رمضان المبارك", isMajorHoliday = true, isFastingRecommended = true))
        if (hDay in listOf(21, 23, 25, 27, 29)) events.add(IslamicEvent("Laylat al-Qadr (Odd Night)", "শবে কদর (বেজোড় রাত)", "ليلة القدر", isMajorHoliday = true))
      }
      10 -> { // Shawwal
        if (hDay == 1) events.add(IslamicEvent("Eid al-Fitr", "ঈদুল ফিতর", "عيد الفطر المبارك", isMajorHoliday = true))
        if (hDay in 2..3) events.add(IslamicEvent("Eid al-Fitr Days", "ঈদুল ফিতরের দিনসমূহ", "أيام عيد الفطر", isMajorHoliday = true))
        if (hDay in 2..30) events.add(IslamicEvent("Six Fasts of Shawwal", "শাওয়ালের ৬ রোজা (মুস্তাহাব)", "ست من شوال", isFastingRecommended = true))
      }
      11 -> { // Dhu al-Qi'dah
        if (hDay == 1) events.add(IslamicEvent("Sacred Month", "পবিত্র হারাম মাস", "الأشهر الحرم"))
      }
      12 -> { // Dhu al-Hijjah
        if (hDay in 1..9) events.add(IslamicEvent("First 10 Days of Dhu al-Hijjah", "জিলহজের প্রথম ১০ দিন (ফজিলতপূর্ণ)", "عشر ذي الحجة", isFastingRecommended = true))
        if (hDay == 8) events.add(IslamicEvent("Day of Tarwiyah", "তারবিয়ার দিন (হজের সূচনা)", "يوم التروية"))
        if (hDay == 9) events.add(IslamicEvent("Day of Arafah", "আরাফার দিন (রোজা সুন্নত)", "يوم عرفة", isMajorHoliday = true, isFastingRecommended = true))
        if (hDay == 10) events.add(IslamicEvent("Eid al-Adha", "ঈদুল আজহা", "عيد الأضحى المبارك", isMajorHoliday = true))
        if (hDay in 11..13) events.add(IslamicEvent("Ayyam at-Tashriq", "আইয়ামে তাশরিক", "أيام التشريق"))
      }
    }

    if (hDay in 13..15) {
      events.add(IslamicEvent("Ayyam al-Beed (White Days)", "আইয়ামে বিজ (নফল রোজা)", "الأيام البيض", isFastingRecommended = true))
    }

    return events
  }

  // --- Algorithmic Astronomy Helpers ---

  private fun isHijriLeapYear(year: Int): Boolean {
    val mod = ((11 * year) + 14) % 30
    return mod < 11
  }

  private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
      y -= 1
      m += 12
    }
    val a = (y / 100).toDouble().toInt()
    val b = 2 - a + (a / 4)
    return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5
  }

  private fun julianDayToHijri(jd: Double): Triple<Int, Int, Int> {
    val epoch = 1948439.5
    val l = (jd - epoch).toInt() + 10632
    val n = ((l - 1) / 10631).toInt()
    val l1 = l - 10631 * n + 354
    val j = (((10985 - l1) / 5316).toInt()) * (((50 * l1) / 17719).toInt()) +
      ((l1 / 5670).toInt()) * (((43 * l1) / 15238).toInt())
    val l2 = l1 - (((30 - j) / 15).toInt()) * (((17719 * j) / 50).toInt()) -
      ((j / 16).toInt()) * (((15238 * j) / 43).toInt()) + 29
    val m = ((24 * l2) / 709).toInt()
    val d = l2 - ((709 * m) / 24).toInt()
    val y = 30 * n + j - 30

    return Triple(y, m, d)
  }

  private fun hijriToApproximateGregorian(hYear: Int, hMonth: Int, hDay: Int): Date {
    val epoch = 1948439.5
    val jd = ((11 * hYear + 3) / 30).toInt() + 354 * hYear + 30 * hMonth - ((hMonth - 1) / 2).toInt() + hDay + epoch - 385
    val z = (jd + 0.5).toInt()
    val a = if (z < 2299161) z else {
      val alpha = (((z - 1867216.25) / 36524.25).toInt())
      z + 1 + alpha - (alpha / 4).toInt()
    }
    val b = a + 1524
    val c = (((b - 122.1) / 365.25).toInt())
    val d = (365.25 * c).toInt()
    val e = (((b - d) / 30.6001).toInt())
    val day = b - d - (30.6001 * e).toInt()
    val month = if (e < 14) e - 1 else e - 13
    val year = if (month > 2) c - 4716 else c - 4715

    return Calendar.getInstance().apply {
      set(year, month - 1, day, 12, 0, 0)
    }.time
  }
}
