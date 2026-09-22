package com.example

import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.PrayerEntity
import com.example.data.util.AstronomicalPrayerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun prayerCalculator_calculatesValidTimes() {
    // Makkah: 21.3891 N, 39.8579 E, UTC+3
    val times = AstronomicalPrayerCalculator.calculate(
      lat = 21.3891,
      lng = 39.8579,
      timezoneOffsetHours = 3.0,
      year = 2026,
      month = 9,
      day = 22
    )

    assertNotNull(times.fajr)
    assertNotNull(times.sunrise)
    assertNotNull(times.dhuhr)
    assertNotNull(times.asr)
    assertNotNull(times.maghrib)
    assertNotNull(times.isha)

    // Check time format "HH:mm"
    val timeRegex = Regex("""^\d{2}:\d{2}$""")
    assertTrue("Fajr format valid", timeRegex.matches(times.fajr))
    assertTrue("Sunrise format valid", timeRegex.matches(times.sunrise))
    assertTrue("Dhuhr format valid", timeRegex.matches(times.dhuhr))
    assertTrue("Asr format valid", timeRegex.matches(times.asr))
    assertTrue("Maghrib format valid", timeRegex.matches(times.maghrib))
    assertTrue("Isha format valid", timeRegex.matches(times.isha))
  }

  @Test
  fun voluntaryPrayers_calculatedAccurately() {
    val sampleEntity = PrayerEntity(
      date = "2026-09-22",
      city = "Makkah",
      country = "Saudi Arabia",
      fajr = "04:55",
      sunrise = "06:10",
      dhuhr = "12:15",
      asr = "15:38",
      maghrib = "18:18",
      isha = "19:48",
      lastSyncedAt = System.currentTimeMillis()
    )

    val voluntary = PrayerAlarmScheduler.computeVoluntaryTimes(sampleEntity)

    assertNotNull(voluntary.ishraq)
    assertNotNull(voluntary.duha)
    assertNotNull(voluntary.tahajjud)

    val timeRegex = Regex("""^\d{2}:\d{2}$""")
    assertTrue("Ishraq valid HH:mm", timeRegex.matches(voluntary.ishraq))
    assertTrue("Duha valid HH:mm", timeRegex.matches(voluntary.duha))
    assertTrue("Tahajjud valid HH:mm", timeRegex.matches(voluntary.tahajjud))

    // Ishraq should be exactly 15 minutes after Sunrise (06:10 + 15 = 06:25)
    assertEquals("06:25", voluntary.ishraq)
  }

  @Test
  fun forbiddenTimes_calculationVerifiesIntervals() {
    val sampleEntity = PrayerEntity(
      date = "2026-09-22",
      city = "Dhaka",
      country = "Bangladesh",
      fajr = "04:30",
      sunrise = "05:46",
      dhuhr = "11:50",
      asr = "15:15",
      maghrib = "17:55",
      isha = "19:10",
      lastSyncedAt = System.currentTimeMillis()
    )

    val voluntary = PrayerAlarmScheduler.computeVoluntaryTimes(sampleEntity)
    // Tulu' (Sunrise forbidden) runs from sunrise 05:46 until Ishraq 06:01 (15 mins)
    assertEquals("06:01", voluntary.ishraq)

    val sunriseMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis("2026-09-22", sampleEntity.sunrise)
    val ishraqMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis("2026-09-22", voluntary.ishraq)

    assertEquals(15 * 60 * 1000L, ishraqMillis - sunriseMillis)

    val forbiddenItems = PrayerAlarmScheduler.computeForbiddenTimesForScheduling(sampleEntity)
    assertEquals(3, forbiddenItems.size)
    assertEquals("Sunrise Transition", forbiddenItems[0].name)
    assertEquals("Midday Solar Zenith", forbiddenItems[1].name)
    assertEquals("Sunset Transition", forbiddenItems[2].name)
  }

  @Test
  fun qiblaCalculation_accurateForKnownCoordinates() {
    // Dhaka, Bangladesh: 23.8103 N, 90.4125 E -> Great circle Qibla bearing is ~277.5°
    val dhakaQibla = com.example.util.QiblaHelper.calculateQiblaBearing(23.8103, 90.4125)
    assertTrue("Dhaka Qibla is around 277.5°", dhakaQibla in 275f..282f)

    // New York, USA: 40.7128 N, -74.0060 E -> Great circle Qibla bearing is ~58.5°
    val nyQibla = com.example.util.QiblaHelper.calculateQiblaBearing(40.7128, -74.0060)
    assertTrue("New York Qibla is around 58.5°", nyQibla in 55f..62f)

    // Distance from Makkah to Kaaba is practically 0
    val makkahDistance = com.example.util.QiblaHelper.calculateDistanceKm(21.4225, 39.8262)
    assertTrue("Makkah distance close to 0 km", makkahDistance < 10)
  }
}
