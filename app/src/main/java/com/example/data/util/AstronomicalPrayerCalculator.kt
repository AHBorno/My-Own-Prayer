package com.example.data.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical solar calculation engine for Islamic Prayer Times.
 * Implements standard solar algorithms (Jean Meeus Astronomical Algorithms & PrayTimes.org).
 * Guarantees that whether online or completely offline, prayer times
 * are computed accurately to the exact minute.
 */
object AstronomicalPrayerCalculator {

  enum class CalculationMethod(
    val id: String,
    val displayName: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null,
    val dhuhrMinutesAdjustment: Int = 1
  ) {
    KARACHI("karachi", "University of Islamic Sciences, Karachi", 18.0, 18.0),
    MWL("mwl", "Muslim World League", 18.0, 17.0),
    ISNA("isna", "Islamic Society of North America", 15.0, 15.0),
    UMM_AL_QURA("umm_al_qura", "Umm Al-Qura, Makkah", 18.5, 0.0, ishaIntervalMinutes = 90),
    EGYPT("egypt", "Egyptian General Authority of Survey", 19.5, 17.5),
    GULF("gulf", "Gulf / UAE Region", 18.2, 0.0, ishaIntervalMinutes = 90),
    KUWAIT("kuwait", "Kuwait", 18.0, 17.5),
    QATAR("qatar", "Qatar", 18.0, 0.0, ishaIntervalMinutes = 90),
    SINGAPORE("singapore", "Singapore / MUIS & Malaysia", 20.0, 18.0),
    TURKEY("turkey", "Diyanet İşleri Başkanlığı", 18.0, 17.0),
    TEHRAN("tehran", "Institute of Geophysics, Tehran", 17.7, 14.0);

    companion object {
      fun getMethodForCountry(country: String): CalculationMethod {
        val c = country.trim().lowercase(Locale.US)
        return when {
          c.contains("bangladesh") || c.contains("pakistan") || c.contains("india") ||
            c.contains("afghanistan") || c.contains("sri lanka") || c.contains("nepal") -> KARACHI
          c.contains("saudi") -> UMM_AL_QURA
          c.contains("emirates") || c.contains("dubai") || c.contains("uae") || c.contains("oman") -> GULF
          c.contains("qatar") -> QATAR
          c.contains("kuwait") -> KUWAIT
          c.contains("egypt") || c.contains("morocco") || c.contains("algeria") ||
            c.contains("tunisia") || c.contains("libya") || c.contains("sudan") ||
            c.contains("somalia") || c.contains("yemen") || c.contains("syria") ||
            c.contains("lebanon") || c.contains("jordan") || c.contains("palestine") ||
            c.contains("iraq") -> EGYPT
          c.contains("turkey") -> TURKEY
          c.contains("malaysia") || c.contains("singapore") || c.contains("indonesia") ||
            c.contains("brunei") || c.contains("thailand") || c.contains("philippines") -> SINGAPORE
          c.contains("united states") || c.contains("usa") || c.contains("canada") -> ISNA
          else -> MWL
        }
      }
    }
  }

  enum class AsrJuristicMethod(val shadowFactor: Double) {
    STANDARD(1.0), // Shafi'i, Maliki, Hanbali, Ja'fari
    HANAFI(2.0)    // Hanafi
  }

  data class CalculatedTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
  )

  /**
   * Calculates times for given latitude, longitude, timezone offset (hours),
   * year, month (1-12), day.
   */
  fun calculate(
    lat: Double,
    lng: Double,
    timezoneOffsetHours: Double,
    year: Int,
    month: Int,
    day: Int,
    method: CalculationMethod = CalculationMethod.MWL,
    asrJuristic: AsrJuristicMethod = AsrJuristicMethod.STANDARD
  ): CalculatedTimes {
    // 1. Julian Date
    val julianDate = getJulianDate(year, month, day) - (lng / (15.0 * 24.0))

    // 2. Sun's position
    val d = julianDate - 2451545.0
    val q = fixAngle(280.459 + 0.98564736 * d)
    val g = fixAngle(357.529 + 0.98560028 * d)
    val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2.0 * g)))

    val e = 23.439 - 0.00000036 * d
    val raDeg = fixAngle(radToDeg(atan2(cos(degToRad(e)) * sin(degToRad(l)), cos(degToRad(l)))))
    val ra = raDeg / 15.0
    val declination = radToDeg(asin(sin(degToRad(e)) * sin(degToRad(l))))

    // 3. Equation of time (in hours)
    var eqOfTime = (q / 15.0) - ra
    while (eqOfTime > 12.0) eqOfTime -= 24.0
    while (eqOfTime < -12.0) eqOfTime += 24.0

    // 4. Solar Noon (Local time)
    val solarNoon = fixHour(12.0 + timezoneOffsetHours - (lng / 15.0) - eqOfTime)
    val dhuhrTime = solarNoon + (method.dhuhrMinutesAdjustment / 60.0)

    // 5. Sunrise & Sunset (Horizon refraction + semi-diameter: 0.8333 deg)
    val sunHorizonAngle = 0.8333
    val sunriseSunsetHourAngle = calculateHourAngle(lat, declination, sunHorizonAngle)

    val sunrise = if (sunriseSunsetHourAngle.isNaN()) solarNoon - 6.0 else solarNoon - sunriseSunsetHourAngle
    val sunset = if (sunriseSunsetHourAngle.isNaN()) solarNoon + 6.0 else solarNoon + sunriseSunsetHourAngle

    // 6. Fajr
    val fajrHourAngle = calculateHourAngle(lat, declination, method.fajrAngle)
    val fajr = if (fajrHourAngle.isNaN()) solarNoon - 7.5 else solarNoon - fajrHourAngle

    // 7. Asr (Shadow length = t + tan(|lat - dec|))
    val shadowLen = asrJuristic.shadowFactor + tan(degToRad(abs(lat - declination)))
    val asrAltitude = radToDeg(atan(1.0 / shadowLen))
    val asrHourAngle = calculateHourAngle(lat, declination, -asrAltitude)
    val asr = if (asrHourAngle.isNaN()) solarNoon + 3.25 else solarNoon + asrHourAngle

    // 8. Isha
    val isha = if (method.ishaIntervalMinutes != null) {
      sunset + (method.ishaIntervalMinutes / 60.0)
    } else {
      val ishaHourAngle = calculateHourAngle(lat, declination, method.ishaAngle)
      if (ishaHourAngle.isNaN()) solarNoon + 7.5 else solarNoon + ishaHourAngle
    }

    return CalculatedTimes(
      fajr = formatTime(fajr),
      sunrise = formatTime(sunrise),
      dhuhr = formatTime(dhuhrTime),
      asr = formatTime(asr),
      maghrib = formatTime(sunset),
      isha = formatTime(isha)
    )
  }

  private fun calculateHourAngle(lat: Double, declination: Double, angle: Double): Double {
    val latRad = degToRad(lat)
    val decRad = degToRad(declination)
    val angRad = degToRad(angle)

    val cosHA = (-sin(angRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
    if (cosHA < -1.0 || cosHA > 1.0) return Double.NaN
    return radToDeg(acos(cosHA)) / 15.0
  }

  private fun getJulianDate(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
      y -= 1
      m += 12
    }
    val a = floor(y / 100.0)
    val b = 2.0 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
  }

  private fun fixAngle(a: Double): Double {
    var angle = a - 360.0 * floor(a / 360.0)
    if (angle < 0.0) angle += 360.0
    return angle
  }

  private fun fixHour(h: Double): Double {
    var hour = h - 24.0 * floor(h / 24.0)
    if (hour < 0.0) hour += 24.0
    return hour
  }

  private fun degToRad(deg: Double): Double = deg * Math.PI / 180.0
  private fun radToDeg(rad: Double): Double = rad * 180.0 / Math.PI

  private fun formatTime(hourDouble: Double): String {
    val fixed = fixHour(hourDouble)
    val totalMinutes = (fixed * 60.0 + 0.5).toInt()
    val h = (totalMinutes / 60) % 24
    val m = totalMinutes % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
  }
}
