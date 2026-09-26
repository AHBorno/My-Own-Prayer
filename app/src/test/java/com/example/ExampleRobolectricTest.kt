package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Own Prayer", appName)
  }

  @Test
  fun `forbidden prayer notification toggle persists in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.repository.PrayerRepository(context)

    repo.setForbiddenTimeNotificationEnabled("Sunrise Transition", true)
    assertTrue(repo.getEnabledForbiddenTimes().contains("Sunrise Transition"))

    repo.setForbiddenTimeNotificationEnabled("Sunrise Transition", false)
    assertTrue(!repo.getEnabledForbiddenTimes().contains("Sunrise Transition"))
  }

  @Test
  fun `astronomical prayer calculator produces accurate times for Dhaka without inverted AM PM`() {
    // Dhaka, Bangladesh: Lat 23.8103, Lng 90.4125, UTC+6 on Sep 25, 2026
    val times = com.example.data.util.AstronomicalPrayerCalculator.calculate(
      lat = 23.8103,
      lng = 90.4125,
      timezoneOffsetHours = 6.0,
      year = 2026,
      month = 9,
      day = 25,
      method = com.example.data.util.AstronomicalPrayerCalculator.CalculationMethod.KARACHI
    )

    // Fajr must be early morning (~04:30 AM)
    val fajrHour = times.fajr.split(":")[0].toInt()
    assertTrue("Fajr hour should be around 4 AM, but got ${times.fajr}", fajrHour in 4..5)

    // Sunrise must be morning (~05:48 AM)
    val sunriseHour = times.sunrise.split(":")[0].toInt()
    assertEquals(5, sunriseHour)

    // Dhuhr must be midday (~11:50 AM)
    val dhuhrHour = times.dhuhr.split(":")[0].toInt()
    assertEquals(11, dhuhrHour)

    // Asr must be afternoon (~15:15 / 03:15 PM)
    val asrHour = times.asr.split(":")[0].toInt()
    assertEquals(15, asrHour)

    // Maghrib must be evening (~17:52 / 05:52 PM)
    val maghribHour = times.maghrib.split(":")[0].toInt()
    assertEquals(17, maghribHour)

    // Isha must be night (~19:07 / 07:07 PM)
    val ishaHour = times.isha.split(":")[0].toInt()
    assertEquals(19, ishaHour)
  }

  @Test
  fun `astronomical prayer calculator produces valid chronological order for Makkah and London`() {
    // Makkah: Lat 21.4225, Lng 39.8262, UTC+3
    val makkah = com.example.data.util.AstronomicalPrayerCalculator.calculate(
      lat = 21.4225,
      lng = 39.8262,
      timezoneOffsetHours = 3.0,
      year = 2026,
      month = 9,
      day = 25
    )
    assertTrue(makkah.fajr < makkah.sunrise)
    assertTrue(makkah.sunrise < makkah.dhuhr)
    assertTrue(makkah.dhuhr < makkah.asr)
    assertTrue(makkah.asr < makkah.maghrib)
    assertTrue(makkah.maghrib < makkah.isha)

    // London: Lat 51.5074, Lng -0.1278, UTC+1 (BST)
    val london = com.example.data.util.AstronomicalPrayerCalculator.calculate(
      lat = 51.5074,
      lng = -0.1278,
      timezoneOffsetHours = 1.0,
      year = 2026,
      month = 9,
      day = 25
    )
    assertTrue(london.fajr < london.sunrise)
    assertTrue(london.sunrise < london.dhuhr)
    assertTrue(london.dhuhr < london.asr)
    assertTrue(london.asr < london.maghrib)
    assertTrue(london.maghrib < london.isha)
  }

  @Test
  fun `ramadan visibility is inactive during normal year and active with preview`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // In September 2026 (Rabi' al-Thani), normal mode should NOT be active
    val isNormallyActive = com.example.util.RamadanHelper.isRamadanActive(
      context = context,
      now = System.currentTimeMillis(),
      forcePreview = false
    )
    org.junit.Assert.assertFalse("Ramadan section should not be visible in September", isNormallyActive)

    // With forcePreview = true, it should be active
    val isPreviewActive = com.example.util.RamadanHelper.isRamadanActive(
      context = context,
      now = System.currentTimeMillis(),
      forcePreview = true
    )
    assertTrue("Ramadan section must be active in preview mode", isPreviewActive)
  }

  @Test
  fun `ramadan suhoor and iftar notification toggles persist in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.repository.PrayerRepository(context)

    repo.setSuhoorNotificationEnabled(false)
    org.junit.Assert.assertFalse(repo.isSuhoorNotificationEnabled())

    repo.setSuhoorNotificationEnabled(true)
    assertTrue(repo.isSuhoorNotificationEnabled())

    repo.setIftarNotificationEnabled(false)
    org.junit.Assert.assertFalse(repo.isIftarNotificationEnabled())

    repo.setIftarNotificationEnabled(true)
    assertTrue(repo.isIftarNotificationEnabled())
  }

  @Test
  fun `eid mubarak notification texts and trigger work properly across languages`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val titleEn = com.example.util.AppLanguageHelper.getEidMubarakNotificationTitle(com.example.util.AppLanguageHelper.LANG_EN)
    val titleBn = com.example.util.AppLanguageHelper.getEidMubarakNotificationTitle(com.example.util.AppLanguageHelper.LANG_BN)
    val titleAr = com.example.util.AppLanguageHelper.getEidMubarakNotificationTitle(com.example.util.AppLanguageHelper.LANG_AR)

    assertTrue(titleEn.contains("Eid Mubarak"))
    assertTrue(titleBn.contains("ঈদ মোবারক"))
    assertTrue(titleAr.contains("عيد"))

    val bodyEn = com.example.util.AppLanguageHelper.getEidMubarakNotificationBody(com.example.util.AppLanguageHelper.LANG_EN)
    val bodyBn = com.example.util.AppLanguageHelper.getEidMubarakNotificationBody(com.example.util.AppLanguageHelper.LANG_BN)
    val bodyAr = com.example.util.AppLanguageHelper.getEidMubarakNotificationBody(com.example.util.AppLanguageHelper.LANG_AR)

    assertTrue(bodyEn.isNotEmpty())
    assertTrue(bodyBn.isNotEmpty())
    assertTrue(bodyAr.isNotEmpty())

    // Verifying showEidMubarakNotification does not throw exceptions
    com.example.alarm.PrayerNotificationHelper.showEidMubarakNotification(context)
  }
}
