package com.example.repository

import android.content.Context
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerNotificationHelper
import com.example.data.local.PrayerDatabase
import com.example.data.local.PrayerEntity
import com.example.data.model.CityLocation
import com.example.worker.PrayerSyncManager
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerRepository(private val context: Context) {
  private val db = PrayerDatabase.getDatabase(context)
  private val prayerDao = db.prayerDao()
  private val prefs = context.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)

  private val KEY_ENABLED_PRAYERS = "key_enabled_prayers"
  private val KEY_ENABLED_FORBIDDEN_TIMES = "key_enabled_forbidden_times"
  private val KEY_HAS_REQUESTED_INITIAL_PERMISSIONS = "key_has_requested_initial_permissions"
  private val KEY_APP_LANGUAGE = "key_app_language"
  private val KEY_APP_THEME = "key_app_theme"
  private val KEY_SUHOOR_NOTIF_ENABLED = "key_suhoor_notif_enabled"
  private val KEY_IFTAR_NOTIF_ENABLED = "key_iftar_notif_enabled"
  private val KEY_PREVIEW_RAMADAN_MODE = "key_preview_ramadan_mode"

  fun isSuhoorNotificationEnabled(): Boolean {
    return prefs.getBoolean(KEY_SUHOOR_NOTIF_ENABLED, true)
  }

  fun setSuhoorNotificationEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SUHOOR_NOTIF_ENABLED, enabled).apply()
  }

  fun isIftarNotificationEnabled(): Boolean {
    return prefs.getBoolean(KEY_IFTAR_NOTIF_ENABLED, true)
  }

  fun setIftarNotificationEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_IFTAR_NOTIF_ENABLED, enabled).apply()
  }

  fun isPreviewRamadanMode(): Boolean {
    return prefs.getBoolean(KEY_PREVIEW_RAMADAN_MODE, false)
  }

  fun setPreviewRamadanMode(preview: Boolean) {
    prefs.edit().putBoolean(KEY_PREVIEW_RAMADAN_MODE, preview).apply()
  }

  fun getAppLanguage(): String {
    return prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
  }

  fun setAppLanguage(language: String) {
    prefs.edit().putString(KEY_APP_LANGUAGE, language).apply()
  }

  fun getAppTheme(): String {
    return prefs.getString(KEY_APP_THEME, "system") ?: "system"
  }

  fun setAppTheme(theme: String) {
    prefs.edit().putString(KEY_APP_THEME, theme).apply()
  }

  fun hasRequestedInitialPermissions(): Boolean {
    return prefs.getBoolean(KEY_HAS_REQUESTED_INITIAL_PERMISSIONS, false)
  }

  fun setInitialPermissionsRequested(requested: Boolean) {
    prefs.edit().putBoolean(KEY_HAS_REQUESTED_INITIAL_PERMISSIONS, requested).apply()
  }

  fun getTodayPrayerTimes(cityLocation: CityLocation? = null): Flow<PrayerEntity?> {
    val city = cityLocation ?: getSelectedCity()
    val cityTz = java.util.TimeZone.getTimeZone(city.timeZoneId)
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = cityTz
    }.format(Date())
    return prayerDao.getPrayerTimesForDate(todayDateStr)
  }

  fun getLatestPrayerTimes(): Flow<PrayerEntity?> {
    val city = getSelectedCity()
    val cityTz = java.util.TimeZone.getTimeZone(city.timeZoneId)
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = cityTz
    }.format(Date())
    return prayerDao.getLatestPrayerTimesOnOrBefore(todayDateStr)
  }

  suspend fun syncPrayerTimes(city: CityLocation): PrayerEntity {
    return PrayerSyncManager.syncNow(context, city)
  }

  fun getSelectedCity(): CityLocation {
    return PrayerSyncManager.getSelectedCity(context)
  }

  fun getEnabledPrayers(): Set<String> {
    return prefs.getStringSet(
      KEY_ENABLED_PRAYERS,
      setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    ) ?: setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
  }

  fun setPrayerNotificationEnabled(prayerName: String, enabled: Boolean) {
    val current = getEnabledPrayers().toMutableSet()
    if (enabled) {
      current.add(prayerName)
    } else {
      current.remove(prayerName)
    }
    prefs.edit().putStringSet(KEY_ENABLED_PRAYERS, current).apply()
  }

  fun getEnabledForbiddenTimes(): Set<String> {
    return prefs.getStringSet(
      KEY_ENABLED_FORBIDDEN_TIMES,
      emptySet()
    ) ?: emptySet()
  }

  fun setForbiddenTimeNotificationEnabled(forbiddenName: String, enabled: Boolean) {
    val current = getEnabledForbiddenTimes().toMutableSet()
    if (enabled) {
      current.add(forbiddenName)
    } else {
      current.remove(forbiddenName)
    }
    prefs.edit().putStringSet(KEY_ENABLED_FORBIDDEN_TIMES, current).apply()
  }

  fun triggerImmediateTestNotification(prayerName: String = "Maghrib") {
    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    PrayerNotificationHelper.showPrayerNotification(
      context = context,
      prayerName = prayerName,
      prayerTime = currentTime,
      notificationId = 888
    )
  }

  fun scheduleTestAlarm(delaySeconds: Int = 10) {
    PrayerAlarmScheduler.scheduleTestAlarm(context, delaySeconds)
  }

  fun triggerEidMubarakNotification() {
    PrayerNotificationHelper.showEidMubarakNotification(context)
  }
}
