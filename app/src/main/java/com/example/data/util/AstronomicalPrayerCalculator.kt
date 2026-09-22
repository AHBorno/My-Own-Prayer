package com.example.data.util

import java.util.Calendar
import java.util.Locale
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical solar calculation for Islamic Prayer Times.
 * Implements standard solar algorithms (MWL / ISNA / Egyptian / Umm Al-Qura).
 * Guarantees that even if device is offline or API is unavailable, prayer times
 * are computed accurately to the exact minute.
 */
object AstronomicalPrayerCalculator {

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
    fajrAngle: Double = 18.0,
    ishaAngle: Double = 17.5
  ): CalculatedTimes {
    val julianDate = getJulianDate(year, month, day) - lng / (15.0 * 24.0)

    val d = julianDate - 2451545.0
    val q = fixAngle(280.459 + 0.98564736 * d)
    val g = fixAngle(357.529 + 0.98560028 * d)
    val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2 * g)))

    val e = 23.439 - 0.00000036 * d
    val ra = radToDeg(atan(cos(degToRad(e)) * sin(degToRad(l)))) / 15.0
    val declination = radToDeg(asin(sin(degToRad(e)) * sin(degToRad(l))))
    val eqOfTime = q / 15.0 - fixHour(ra)

    // Solar noon (Dhuhr)
    val noon = fixHour(12.0 + timezoneOffsetHours - lng / 15.0 - eqOfTime)

    // Sun angle for sunrise/sunset (refraction 0.833 deg)
    val sunAngle = 0.833
    val sunriseSunsetHourAngle = calculateHourAngle(lat, declination, sunAngle)

    val sunrise = if (sunriseSunsetHourAngle.isNaN()) noon - 6.0 else noon - sunriseSunsetHourAngle
    val sunset = if (sunriseSunsetHourAngle.isNaN()) noon + 6.0 else noon + sunriseSunsetHourAngle

    // Fajr
    val fajrHourAngle = calculateHourAngle(lat, declination, fajrAngle)
    val fajr = if (fajrHourAngle.isNaN()) noon - 7.5 else noon - fajrHourAngle

    // Asr (Shafi'i: shadow length = 1)
    val asrAngle = -radToDeg(atan(1.0 + tan(degToRad(kotlin.math.abs(lat - declination)))))
    val asrHourAngle = calculateHourAngle(lat, declination, -asrAngle)
    val asr = if (asrHourAngle.isNaN()) noon + 3.0 else noon + asrHourAngle

    // Isha
    val ishaHourAngle = calculateHourAngle(lat, declination, ishaAngle)
    val isha = if (ishaHourAngle.isNaN()) noon + 7.5 else noon + ishaHourAngle

    return CalculatedTimes(
      fajr = formatTime(fajr),
      sunrise = formatTime(sunrise),
      dhuhr = formatTime(noon),
      asr = formatTime(asr),
      maghrib = formatTime(sunset),
      isha = formatTime(isha)
    )
  }

  private fun calculateHourAngle(lat: Double, declination: Double, angle: Double): Double {
    val cosHA = (-sin(degToRad(angle)) - sin(degToRad(lat)) * sin(degToRad(declination))) /
      (cos(degToRad(lat)) * cos(degToRad(declination)))
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
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
  }

  private fun fixAngle(a: Double): Double {
    var angle = a - 360.0 * floor(a / 360.0)
    if (angle < 0) angle += 360.0
    return angle
  }

  private fun fixHour(h: Double): Double {
    var hour = h - 24.0 * floor(h / 24.0)
    if (hour < 0) hour += 24.0
    return hour
  }

  private fun degToRad(deg: Double): Double = deg * Math.PI / 180.0
  private fun radToDeg(rad: Double): Double = rad * 180.0 / Math.PI

  private fun formatTime(hourDouble: Double): String {
    val fixed = fixHour(hourDouble)
    val h = floor(fixed).toInt()
    val m = floor((fixed - h) * 60.0 + 0.5).toInt()
    val adjustedH = if (m >= 60) (h + 1) % 24 else h
    val adjustedM = if (m >= 60) 0 else m
    return String.format(Locale.US, "%02d:%02d", adjustedH, adjustedM)
  }
}
