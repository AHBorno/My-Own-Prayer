package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.PrayerEntity
import com.example.data.model.CityLocation
import com.example.data.model.ForbiddenTimeItem
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerType
import com.example.data.model.SolarTimes
import com.example.repository.PrayerRepository
import com.example.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrayerUiState(
  val prayers: List<PrayerItem> = emptyList(),
  val voluntaryPrayers: List<PrayerItem> = emptyList(),
  val solarTimes: SolarTimes = SolarTimes(),
  val forbiddenTimes: List<ForbiddenTimeItem> = emptyList(),
  val currentPrayer: PrayerItem? = null,
  val nextPrayer: PrayerItem? = null,
  val isCloseToNextPrayer: Boolean = false,
  val isPreviewCloseMode: Boolean = false,
  val countdownText: String = "--:--:--",
  val currentCity: CityLocation = CityLocation.DEFAULT_CITY,
  val syncSource: String = "Google & Aladhan Cloud API",
  val lastSyncedFormatted: String = "Today",
  val isSyncing: Boolean = false,
  val isDetectingLocation: Boolean = false,
  val testMessage: String? = null,
  val isBatteryOptimizedMode: Boolean = true
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = PrayerRepository(application)

  private val _uiState = MutableStateFlow(PrayerUiState())
  val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

  private var currentEntity: PrayerEntity? = null

  init {
    val savedCity = repository.getSelectedCity()
    _uiState.update { it.copy(currentCity = savedCity) }

    // Observe Room database for today's prayer times
    viewModelScope.launch {
      repository.getTodayPrayerTimes().collect { entity ->
        if (entity != null) {
          currentEntity = entity
          updatePrayerList(entity)
        } else {
          syncToday(savedCity)
        }
      }
    }

    // Start 1-second ticker for live countdown and forbidden times check
    viewModelScope.launch {
      while (true) {
        val entity = currentEntity
        if (entity != null) {
          updatePrayerList(entity)
        }
        delay(1000)
      }
    }
  }

  fun syncToday(city: CityLocation = _uiState.value.currentCity) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSyncing = true, currentCity = city) }
      try {
        val result = repository.syncPrayerTimes(city)
        currentEntity = result
        updatePrayerList(result)
      } catch (_: Exception) {
        // Fallback handled internally
      } finally {
        _uiState.update { it.copy(isSyncing = false) }
      }
    }
  }

  fun detectCurrentLocation() {
    viewModelScope.launch {
      _uiState.update { it.copy(isDetectingLocation = true) }
      try {
        val detected = LocationHelper.getCurrentCityLocation(getApplication())
        if (detected != null) {
          _uiState.update {
            it.copy(
              testMessage = "Location detected: ${detected.name}, ${detected.country}"
            )
          }
          syncToday(detected)
        } else {
          _uiState.update {
            it.copy(testMessage = "Unable to get current GPS location. Ensure location is enabled.")
          }
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(testMessage = "Location detection failed: ${e.message}")
        }
      } finally {
        _uiState.update { it.copy(isDetectingLocation = false) }
      }
    }
  }

  fun selectCity(city: CityLocation) {
    syncToday(city)
  }

  fun togglePrayerNotification(prayerName: String, enabled: Boolean) {
    repository.setPrayerNotificationEnabled(prayerName, enabled)
    currentEntity?.let { entity ->
      PrayerAlarmScheduler.scheduleAlarmsForToday(
        context = getApplication(),
        prayerEntity = entity,
        enabledPrayers = repository.getEnabledPrayers(),
        enabledForbidden = repository.getEnabledForbiddenTimes()
      )
      updatePrayerList(entity)
    }
  }

  fun toggleForbiddenNotification(forbiddenName: String, enabled: Boolean) {
    repository.setForbiddenTimeNotificationEnabled(forbiddenName, enabled)
    currentEntity?.let { entity ->
      PrayerAlarmScheduler.scheduleAlarmsForToday(
        context = getApplication(),
        prayerEntity = entity,
        enabledPrayers = repository.getEnabledPrayers(),
        enabledForbidden = repository.getEnabledForbiddenTimes()
      )
      updatePrayerList(entity)
    }
  }

  fun triggerImmediateTestNotification() {
    val nextName = _uiState.value.nextPrayer?.type?.displayName ?: "Prayer"
    repository.triggerImmediateTestNotification(nextName)
    _uiState.update {
      it.copy(testMessage = "Notification pushed! Check your system notification shade.")
    }
  }

  fun scheduleTestAlarm(seconds: Int = 10) {
    repository.scheduleTestAlarm(seconds)
    _uiState.update {
      it.copy(testMessage = "Exact Alarm scheduled in $seconds seconds! Lock phone or leave the app to test waking up.")
    }
  }

  fun clearTestMessage() {
    _uiState.update { it.copy(testMessage = null) }
  }

  private data class RawPrayerInfo(
    val type: PrayerType,
    val timeMillis: Long,
    val formatted12h: String
  )

  private data class ForbiddenInterval(
    val name: String,
    val intervalFormatted: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long
  )

  private data class CachedDaySchedule(
    val entityKey: String,
    val obligatory: List<RawPrayerInfo>,
    val voluntary: List<RawPrayerInfo>,
    val solar: SolarTimes,
    val forbiddenIntervals: List<ForbiddenInterval>,
    val lastSyncFormatted: String
  )

  private var cachedDaySchedule: CachedDaySchedule? = null

  private fun getOrCreateCachedSchedule(entity: PrayerEntity): CachedDaySchedule {
    val key = "${entity.date}_${entity.city}_${entity.country}_${entity.lastSyncedAt}"
    val existing = cachedDaySchedule
    if (existing != null && existing.entityKey == key) {
      return existing
    }

    val todayDate = entity.date
    val sunriseMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.sunrise)
    val dhuhrMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.dhuhr)
    val maghribMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.maghrib)

    val voluntaryTimes = PrayerAlarmScheduler.computeVoluntaryTimes(entity)

    val rawObligatory = listOf(
      RawPrayerInfo(PrayerType.FAJR, PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.fajr), format12h(entity.fajr)),
      RawPrayerInfo(PrayerType.DHUHR, dhuhrMillis, format12h(entity.dhuhr)),
      RawPrayerInfo(PrayerType.ASR, PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.asr), format12h(entity.asr)),
      RawPrayerInfo(PrayerType.MAGHRIB, maghribMillis, format12h(entity.maghrib)),
      RawPrayerInfo(PrayerType.ISHA, PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.isha), format12h(entity.isha))
    )

    val rawVoluntary = listOf(
      RawPrayerInfo(PrayerType.ISHRAQ, PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, voluntaryTimes.ishraq), format12h(voluntaryTimes.ishraq)),
      RawPrayerInfo(PrayerType.DUHA, PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, voluntaryTimes.duha), format12h(voluntaryTimes.duha)),
      RawPrayerInfo(PrayerType.TAHAJJUD, PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, voluntaryTimes.tahajjud), format12h(voluntaryTimes.tahajjud))
    )

    val solar = SolarTimes(
      sunriseFormatted = format12h(entity.sunrise),
      sunsetFormatted = format12h(entity.maghrib),
      sunriseMillis = sunriseMillis,
      sunsetMillis = maghribMillis
    )

    val forbidden = listOf(
      ForbiddenInterval(
        name = "Sunrise Transition",
        intervalFormatted = "${format12h(entity.sunrise)} - ${format12h(voluntaryTimes.ishraq)}",
        description = "From sunrise until the sun has risen above the horizon (~15 mins)",
        startMillis = sunriseMillis,
        endMillis = sunriseMillis + (15 * 60 * 1000L)
      ),
      ForbiddenInterval(
        name = "Midday Solar Zenith",
        intervalFormatted = "${formatMillis12h(dhuhrMillis - 15 * 60 * 1000L)} - ${format12h(entity.dhuhr)}",
        description = "When the sun is at its exact zenith until it declines into Dhuhr (~15 mins)",
        startMillis = dhuhrMillis - (15 * 60 * 1000L),
        endMillis = dhuhrMillis
      ),
      ForbiddenInterval(
        name = "Sunset Transition",
        intervalFormatted = "${formatMillis12h(maghribMillis - 15 * 60 * 1000L)} - ${format12h(entity.maghrib)}",
        description = "When the sun pales and sets into the horizon before Maghrib (~15 mins)",
        startMillis = maghribMillis - (15 * 60 * 1000L),
        endMillis = maghribMillis
      )
    )

    val lastSyncFormatted = formatTimestamp(entity.lastSyncedAt)

    val built = CachedDaySchedule(
      entityKey = key,
      obligatory = rawObligatory,
      voluntary = rawVoluntary,
      solar = solar,
      forbiddenIntervals = forbidden,
      lastSyncFormatted = lastSyncFormatted
    )
    cachedDaySchedule = built
    return built
  }

  private fun updatePrayerList(entity: PrayerEntity) {
    val cached = getOrCreateCachedSchedule(entity)
    val enabledSet = repository.getEnabledPrayers()
    val enabledForbiddenSet = repository.getEnabledForbiddenTimes()
    val now = System.currentTimeMillis()

    var nextItem: PrayerItem? = null
    var minDiff = Long.MAX_VALUE

    val obligatoryItems = cached.obligatory.map { raw ->
      val isPassed = now > raw.timeMillis
      val isEnabled = enabledSet.contains(raw.type.displayName)
      val item = PrayerItem(
        type = raw.type,
        timeFormatted = raw.formatted12h,
        timeMillis = raw.timeMillis,
        isPassed = isPassed,
        isCurrent = false,
        isNext = false,
        notificationEnabled = isEnabled
      )

      val diff = raw.timeMillis - now
      if (diff > 0 && diff < minDiff) {
        minDiff = diff
        nextItem = item
      }
      item
    }

    if (nextItem == null && obligatoryItems.isNotEmpty()) {
      val tomorrowFajrMillis = obligatoryItems.first().timeMillis + (24 * 3600 * 1000L)
      minDiff = tomorrowFajrMillis - now
      nextItem = obligatoryItems.first().copy(timeMillis = tomorrowFajrMillis)
    }

    // Determine current prayer among obligatory prayers (the latest prayer whose time has arrived)
    val passedPrayers = obligatoryItems.filter { now >= it.timeMillis }
    val currentItem: PrayerItem? = if (passedPrayers.isNotEmpty()) {
      passedPrayers.maxByOrNull { it.timeMillis }
    } else if (obligatoryItems.isNotEmpty()) {
      // Before today's Fajr -> current prayer is yesterday's Isha
      val yesterdayIshaMillis = obligatoryItems.last().timeMillis - (24 * 3600 * 1000L)
      obligatoryItems.last().copy(
        timeMillis = yesterdayIshaMillis,
        isPassed = true
      )
    } else {
      null
    }

    val updatedObligatory = obligatoryItems.map { item ->
      item.copy(
        isCurrent = item.type == currentItem?.type,
        isNext = item.type == nextItem?.type
      )
    }

    val voluntaryItems = cached.voluntary.map { raw ->
      var millis = raw.timeMillis
      if (raw.type == PrayerType.TAHAJJUD && now > millis) {
        millis += 24 * 3600 * 1000L
      }
      PrayerItem(
        type = raw.type,
        timeFormatted = raw.formatted12h,
        timeMillis = millis,
        isPassed = now > millis,
        isCurrent = false,
        isNext = false,
        notificationEnabled = enabledSet.contains(raw.type.displayName)
      )
    }

    val updatedForbidden = cached.forbiddenIntervals.map { interval ->
      ForbiddenTimeItem(
        name = interval.name,
        arabicName = "",
        intervalFormatted = interval.intervalFormatted,
        description = interval.description,
        startMillis = interval.startMillis,
        endMillis = interval.endMillis,
        isActiveNow = now in interval.startMillis..interval.endMillis,
        notificationEnabled = enabledForbiddenSet.contains(interval.name)
      )
    }

    val countdownStr = if (minDiff in 1 until Long.MAX_VALUE) {
      formatCountdown(minDiff)
    } else {
      "--:--:--"
    }

    // Is almost close to next prayer (e.g. within 30 minutes)
    val isCloseToNextReal = minDiff in 1..(30 * 60 * 1000L)
    val isCloseToNext = _uiState.value.isPreviewCloseMode || isCloseToNextReal

    val current = _uiState.value
    val listsUnchanged = current.prayers == updatedObligatory &&
        current.voluntaryPrayers == voluntaryItems &&
        current.forbiddenTimes == updatedForbidden &&
        current.currentPrayer?.type == currentItem?.type &&
        current.currentPrayer?.timeMillis == currentItem?.timeMillis &&
        current.nextPrayer?.type == nextItem?.type &&
        current.nextPrayer?.timeMillis == nextItem?.timeMillis &&
        current.isCloseToNextPrayer == isCloseToNext &&
        current.lastSyncedFormatted == cached.lastSyncFormatted &&
        current.syncSource == entity.syncSource

    if (listsUnchanged) {
      // FAST PATH: Lists are structurally identical, preserve previous references so LazyColumn cards skip recomposition
      if (current.countdownText != countdownStr) {
        _uiState.update { it.copy(countdownText = countdownStr) }
      }
    } else {
      // FULL UPDATE: Prayer transition, status change, or notification toggle occurred
      _uiState.update {
        it.copy(
          prayers = updatedObligatory,
          voluntaryPrayers = voluntaryItems,
          solarTimes = cached.solar,
          forbiddenTimes = updatedForbidden,
          currentPrayer = currentItem,
          nextPrayer = nextItem,
          isCloseToNextPrayer = isCloseToNext,
          countdownText = countdownStr,
          syncSource = entity.syncSource,
          lastSyncedFormatted = cached.lastSyncFormatted
        )
      }
    }
  }

  fun togglePreviewCloseMode() {
    val newMode = !_uiState.value.isPreviewCloseMode
    _uiState.update { it.copy(isPreviewCloseMode = newMode) }
    currentEntity?.let { updatePrayerList(it) }
  }

  private fun format12h(time24: String): String {
    return try {
      val clean = time24.trim().split(" ")[0]
      val parts = clean.split(":")
      val hour = parts[0].toInt()
      val min = parts[1].toInt()
      val isPm = hour >= 12
      val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
      }
      String.format(Locale.US, "%02d:%02d\u00A0%s", h12, min, if (isPm) "PM" else "AM")
    } catch (_: Exception) {
      time24
    }
  }

  private fun formatMillis12h(millis: Long): String {
    val sdf = SimpleDateFormat("hh:mm\u00A0a", Locale.US)
    return sdf.format(Date(millis))
  }

  private fun formatCountdown(diffMillis: Long): String {
    val totalSec = diffMillis / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
  }

  private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "Today at " + sdf.format(Date(millis))
  }
}
