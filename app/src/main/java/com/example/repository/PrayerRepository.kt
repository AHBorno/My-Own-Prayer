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

  fun hasRequestedInitialPermissions(): Boolean {
    return prefs.getBoolean(KEY_HAS_REQUESTED_INITIAL_PERMISSIONS, false)
  }

  fun setInitialPermissionsRequested(requested: Boolean) {
    prefs.edit().putBoolean(KEY_HAS_REQUESTED_INITIAL_PERMISSIONS, requested).apply()
  }

  fun getTodayPrayerTimes(): Flow<PrayerEntity?> {
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return prayerDao.getPrayerTimesForDate(todayDateStr)
  }

  fun getLatestPrayerTimes(): Flow<PrayerEntity?> {
    return prayerDao.getLatestPrayerTimes()
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
}
