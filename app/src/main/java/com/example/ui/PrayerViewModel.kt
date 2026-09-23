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
import com.example.update.AppUpdateInfo
import com.example.update.AppUpdateManager
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
  val activeForbiddenTime: ForbiddenTimeItem? = null,
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
  val isBatteryOptimizedMode: Boolean = true,
  val appUpdateInfo: AppUpdateInfo? = null,
  val isCheckingUpdate: Boolean = false,
  val showUpdateDialog: Boolean = false,
  val appLanguage: String = "en",
  val appTheme: String = "system"
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = PrayerRepository(application)

  private val _uiState = MutableStateFlow(
    PrayerUiState(
      appLanguage = repository.getAppLanguage(),
      appTheme = repository.getAppTheme()
    )
  )
  val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

  private val _countdownText = MutableStateFlow("--:--:--")
  val countdownText: StateFlow<String> = _countdownText.asStateFlow()

  private var currentEntity: PrayerEntity? = null

  init {
    val savedCity = repository.getSelectedCity()
    _uiState.update {
      it.copy(
        currentCity = savedCity,
        appLanguage = repository.getAppLanguage(),
        appTheme = repository.getAppTheme()
      )
    }

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

    // Automatically check for app updates from GitHub releases on startup
    checkForAppUpdates(silent = true)
  }

  fun checkForAppUpdates(silent: Boolean = false) {
    viewModelScope.launch {
      _uiState.update { it.copy(isCheckingUpdate = true) }
      try {
        val updateInfo = AppUpdateManager.checkForUpdates(getApplication())
        _uiState.update {
          it.copy(
            appUpdateInfo = updateInfo,
            showUpdateDialog = updateInfo.hasUpdate
          )
        }
        if (updateInfo.hasUpdate) {
          AppUpdateManager.showUpdateNotification(getApplication(), updateInfo)
        } else if (!silent) {
          val feedbackText = if (updateInfo.errorMessage != null) {
            updateInfo.errorMessage
          } else {
            "You have the latest version (v${updateInfo.currentVersionName})"
          }
          _uiState.update {
            it.copy(testMessage = feedbackText)
          }
        }
      } catch (e: Exception) {
        if (!silent) {
          _uiState.update {
            it.copy(testMessage = "Update check failed: ${e.message}")
          }
        }
      } finally {
        _uiState.update { it.copy(isCheckingUpdate = false) }
      }
    }
  }

  fun dismissUpdateDialog() {
    _uiState.update { it.copy(showUpdateDialog = false) }
  }

  fun openUpdateDialog() {
    _uiState.update { it.copy(showUpdateDialog = true) }
  }

  fun setLanguage(lang: String) {
    repository.setAppLanguage(lang)
    com.example.util.AppLanguageHelper.setLanguage(getApplication(), lang)
    _uiState.update { it.copy(appLanguage = lang) }
    currentEntity?.let { entity ->
      PrayerAlarmScheduler.scheduleAlarmsForToday(
        getApplication(),
        entity,
        repository.getEnabledPrayers(),
        repository.getEnabledForbiddenTimes()
      )
      updatePrayerList(entity)
    }
  }

  fun setTheme(theme: String) {
    repository.setAppTheme(theme)
    _uiState.update { it.copy(appTheme = theme) }
  }

  fun triggerTestNotification(prayerName: String = "Maghrib") {
    repository.triggerImmediateTestNotification(prayerName)
    val msg = com.example.util.AppLanguageHelper.getString("test_notification_sent", _uiState.value.appLanguage)
    _uiState.update { it.copy(testMessage = msg) }
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

  fun hasRequestedInitialPermissions(): Boolean {
    return repository.hasRequestedInitialPermissions()
  }

  fun setInitialPermissionsRequested(requested: Boolean) {
    repository.setInitialPermissionsRequested(requested)
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
    val endMillis: Long,
    val formatted12h: String,
    val endFormatted12h: String
  )

  private data class ForbiddenInterval(
    val name: String,
    val arabicName: String = "",
    val bengaliName: String = "",
    val intervalFormatted: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long
  )

  private data class CachedDaySchedule(
    val entityKey: String,
    val obligatory: List<RawPrayerInfo>,
    val voluntary: List<RawPrayerInfo>,
    val fullTimeline: List<RawPrayerInfo>,
    val solar: SolarTimes,
    val forbiddenIntervals: List<ForbiddenInterval>,
    val lastSyncFormatted: String,
    val sunriseMillis: Long,
    val dhuhrMillis: Long,
    val asrMillis: Long,
    val maghribMillis: Long,
    val ishaMillis: Long,
    val fajrMillis: Long,
    val ishraqMillis: Long,
    val duhaMillis: Long,
    val middayZenithMillis: Long,
    val sunsetTransitionMillis: Long,
    val tahajjudEarlyMillis: Long,
    val tahajjudTonightMillis: Long,
    val tomorrowFajrMillis: Long
  )

  private var cachedDaySchedule: CachedDaySchedule? = null

  private fun getOrCreateCachedSchedule(entity: PrayerEntity): CachedDaySchedule {
    val key = "${entity.date}_${entity.city}_${entity.country}_${entity.lastSyncedAt}"
    val existing = cachedDaySchedule
    if (existing != null && existing.entityKey == key) {
      return existing
    }

    val todayDate = entity.date
    val fajrMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.fajr)
    val sunriseMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.sunrise)
    val dhuhrMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.dhuhr)
    val asrMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.asr)
    val maghribMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.maghrib)
    val ishaMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, entity.isha)

    val voluntaryTimes = PrayerAlarmScheduler.computeVoluntaryTimes(entity)
    val ishraqMillis = sunriseMillis + (15 * 60 * 1000L)
    val duhaMillis = PrayerAlarmScheduler.parsePrayerTimeToMillis(todayDate, voluntaryTimes.duha)
    val middayZenithMillis = dhuhrMillis - (15 * 60 * 1000L)
    val sunsetTransitionMillis = maghribMillis - (15 * 60 * 1000L)

    val tomorrowFajrMillis = fajrMillis + (24 * 3600 * 1000L)
    val nightDurationTonight = (tomorrowFajrMillis - maghribMillis).coerceAtLeast(6 * 3600 * 1000L)
    val tahajjudTonightMillis = tomorrowFajrMillis - (nightDurationTonight / 3)

    val yesterdayMaghribMillis = maghribMillis - (24 * 3600 * 1000L)
    val nightDurationEarly = (fajrMillis - yesterdayMaghribMillis).coerceAtLeast(6 * 3600 * 1000L)
    val tahajjudEarlyMorningMillis = fajrMillis - (nightDurationEarly / 3)

    val rawObligatory = listOf(
      RawPrayerInfo(PrayerType.FAJR, fajrMillis, sunriseMillis, format12h(entity.fajr), format12h(entity.sunrise)),
      RawPrayerInfo(PrayerType.DHUHR, dhuhrMillis, asrMillis, format12h(entity.dhuhr), format12h(entity.asr)),
      RawPrayerInfo(PrayerType.ASR, asrMillis, sunsetTransitionMillis, format12h(entity.asr), formatMillis12h(sunsetTransitionMillis)),
      RawPrayerInfo(PrayerType.MAGHRIB, maghribMillis, ishaMillis, format12h(entity.maghrib), format12h(entity.isha)),
      RawPrayerInfo(PrayerType.ISHA, ishaMillis, tomorrowFajrMillis, format12h(entity.isha), format12h(entity.fajr))
    )

    val rawVoluntary = listOf(
      RawPrayerInfo(PrayerType.ISHRAQ, ishraqMillis, duhaMillis, format12h(voluntaryTimes.ishraq), format12h(voluntaryTimes.duha)),
      RawPrayerInfo(PrayerType.DUHA, duhaMillis, middayZenithMillis, format12h(voluntaryTimes.duha), formatMillis12h(middayZenithMillis)),
      RawPrayerInfo(PrayerType.TAHAJJUD, tahajjudTonightMillis, tomorrowFajrMillis, format12h(voluntaryTimes.tahajjud), formatMillis12h(tomorrowFajrMillis))
    )

    val fullTimeline = listOf(
      RawPrayerInfo(PrayerType.TAHAJJUD, tahajjudEarlyMorningMillis, fajrMillis, formatMillis12h(tahajjudEarlyMorningMillis), format12h(entity.fajr)),
      RawPrayerInfo(PrayerType.FAJR, fajrMillis, sunriseMillis, format12h(entity.fajr), format12h(entity.sunrise)),
      RawPrayerInfo(PrayerType.ISHRAQ, ishraqMillis, duhaMillis, format12h(voluntaryTimes.ishraq), format12h(voluntaryTimes.duha)),
      RawPrayerInfo(PrayerType.DUHA, duhaMillis, middayZenithMillis, format12h(voluntaryTimes.duha), formatMillis12h(middayZenithMillis)),
      RawPrayerInfo(PrayerType.DHUHR, dhuhrMillis, asrMillis, format12h(entity.dhuhr), format12h(entity.asr)),
      RawPrayerInfo(PrayerType.ASR, asrMillis, sunsetTransitionMillis, format12h(entity.asr), formatMillis12h(sunsetTransitionMillis)),
      RawPrayerInfo(PrayerType.MAGHRIB, maghribMillis, ishaMillis, format12h(entity.maghrib), format12h(entity.isha)),
      RawPrayerInfo(PrayerType.ISHA, ishaMillis, tomorrowFajrMillis, format12h(entity.isha), format12h(entity.fajr)),
      RawPrayerInfo(PrayerType.TAHAJJUD, tahajjudTonightMillis, tomorrowFajrMillis, format12h(voluntaryTimes.tahajjud), formatMillis12h(tomorrowFajrMillis)),
      RawPrayerInfo(PrayerType.FAJR, tomorrowFajrMillis, tomorrowFajrMillis + (sunriseMillis - fajrMillis), format12h(entity.fajr), format12h(entity.sunrise))
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
        arabicName = "شروق الشمس",
        bengaliName = "সূর্যোদয় বিরতি",
        intervalFormatted = "${format12h(entity.sunrise)} - ${format12h(voluntaryTimes.ishraq)}",
        description = "From sunrise until the sun has risen above the horizon (~15 mins)",
        startMillis = sunriseMillis,
        endMillis = sunriseMillis + (15 * 60 * 1000L)
      ),
      ForbiddenInterval(
        name = "Midday Solar Zenith",
        arabicName = "استواء الشمس",
        bengaliName = "দ্বিপ্রহরের সূর্য চূড়া",
        intervalFormatted = "${formatMillis12h(dhuhrMillis - 15 * 60 * 1000L)} - ${format12h(entity.dhuhr)}",
        description = "When the sun is at its exact zenith until it declines into Dhuhr (~15 mins)",
        startMillis = dhuhrMillis - (15 * 60 * 1000L),
        endMillis = dhuhrMillis
      ),
      ForbiddenInterval(
        name = "Sunset Transition",
        arabicName = "غروب الشمس",
        bengaliName = "সূর্যাস্ত বিরতি",
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
      fullTimeline = fullTimeline,
      solar = solar,
      forbiddenIntervals = forbidden,
      lastSyncFormatted = lastSyncFormatted,
      sunriseMillis = sunriseMillis,
      dhuhrMillis = dhuhrMillis,
      asrMillis = asrMillis,
      maghribMillis = maghribMillis,
      ishaMillis = ishaMillis,
      fajrMillis = fajrMillis,
      ishraqMillis = ishraqMillis,
      duhaMillis = duhaMillis,
      middayZenithMillis = middayZenithMillis,
      sunsetTransitionMillis = sunsetTransitionMillis,
      tahajjudEarlyMillis = tahajjudEarlyMorningMillis,
      tahajjudTonightMillis = tahajjudTonightMillis,
      tomorrowFajrMillis = tomorrowFajrMillis
    )
    cachedDaySchedule = built
    return built
  }

  private fun updatePrayerList(entity: PrayerEntity) {
    val cached = getOrCreateCachedSchedule(entity)
    val enabledSet = repository.getEnabledPrayers()
    val enabledForbiddenSet = repository.getEnabledForbiddenTimes()
    val now = System.currentTimeMillis()

    // 1. Determine Current Prayer (among ALL obligatory and voluntary prayers)
    val currentRaw: RawPrayerInfo? = when {
      // If currently in early morning Tahajjud window
      now in cached.tahajjudEarlyMillis until cached.fajrMillis -> {
        cached.fullTimeline.firstOrNull { it.type == PrayerType.TAHAJJUD && now in it.timeMillis until it.endMillis }
      }
      // If before early morning Tahajjud, it's still yesterday's Isha time ending at today's Fajr
      now < cached.tahajjudEarlyMillis -> {
        val yesterdayIshaMillis = cached.ishaMillis - (24 * 3600 * 1000L)
        RawPrayerInfo(PrayerType.ISHA, yesterdayIshaMillis, cached.fajrMillis, format12h(entity.isha), format12h(entity.fajr))
      }
      // If currently in tonight's Tahajjud window
      now in cached.tahajjudTonightMillis until cached.tomorrowFajrMillis -> {
        cached.fullTimeline.firstOrNull { it.type == PrayerType.TAHAJJUD && now in it.timeMillis until it.endMillis }
      }
      // If currently in tonight's regular Isha window (before Tahajjud tonight)
      now in cached.ishaMillis until cached.tahajjudTonightMillis -> {
        RawPrayerInfo(PrayerType.ISHA, cached.ishaMillis, cached.tomorrowFajrMillis, format12h(entity.isha), format12h(entity.fajr))
      }
      else -> {
        // Find if now is inside any prayer's active window [startMillis .. endMillis)
        cached.fullTimeline.firstOrNull { now >= it.timeMillis && now < it.endMillis }
      }
    }

    val currentItem: PrayerItem? = currentRaw?.let { raw ->
      PrayerItem(
        type = raw.type,
        timeFormatted = raw.formatted12h,
        timeMillis = raw.timeMillis,
        endTimeFormatted = raw.endFormatted12h,
        endTimeMillis = raw.endMillis,
        isPassed = false,
        isCurrent = true,
        isNext = false,
        notificationEnabled = enabledSet.contains(raw.type.displayName)
      )
    }

    // 2. Determine Next Upcoming Prayer (among ALL obligatory and voluntary prayers)
    val nextRaw: RawPrayerInfo? = cached.fullTimeline.firstOrNull { it.timeMillis > now }
    val nextItem: PrayerItem? = nextRaw?.let { raw ->
      PrayerItem(
        type = raw.type,
        timeFormatted = raw.formatted12h,
        timeMillis = raw.timeMillis,
        endTimeFormatted = raw.endFormatted12h,
        endTimeMillis = raw.endMillis,
        isPassed = false,
        isCurrent = false,
        isNext = true,
        notificationEnabled = enabledSet.contains(raw.type.displayName)
      )
    }

    val minDiff = if (nextRaw != null) nextRaw.timeMillis - now else 0L

    val updatedForbidden = cached.forbiddenIntervals.map { interval ->
      ForbiddenTimeItem(
        name = interval.name,
        arabicName = interval.arabicName,
        bengaliName = interval.bengaliName,
        intervalFormatted = interval.intervalFormatted,
        description = interval.description,
        startMillis = interval.startMillis,
        endMillis = interval.endMillis,
        isActiveNow = now in interval.startMillis..interval.endMillis,
        notificationEnabled = enabledForbiddenSet.contains(interval.name)
      )
    }

    val activeForbidden = updatedForbidden.firstOrNull { it.isActiveNow }
    val effectiveCurrentItem: PrayerItem? = if (activeForbidden != null) null else currentItem

    val isTahajjudActiveNow = (now in cached.tahajjudEarlyMillis until cached.fajrMillis) ||
      (now in cached.tahajjudTonightMillis until cached.tomorrowFajrMillis)

    // 3. Map Obligatory Prayers list for UI
    val updatedObligatory = cached.obligatory.map { raw ->
      val isPassed = when (raw.type) {
        PrayerType.FAJR -> now >= cached.sunriseMillis // Fajr ends strictly at sunrise!
        PrayerType.ASR -> now >= cached.sunsetTransitionMillis // Asr ends at sunset transition!
        PrayerType.ISHA -> {
          if (now < cached.fajrMillis) false else now in cached.fajrMillis until cached.ishaMillis // Isha ends strictly when Fajr starts!
        }
        else -> now >= raw.endMillis
      }
      val isMakruh = (raw.type == PrayerType.ISHA) && isTahajjudActiveNow
      val isCurrent = effectiveCurrentItem?.type == raw.type
      val isNext = nextItem?.type == raw.type

      PrayerItem(
        type = raw.type,
        timeFormatted = raw.formatted12h,
        timeMillis = raw.timeMillis,
        endTimeFormatted = raw.endFormatted12h,
        endTimeMillis = raw.endMillis,
        isPassed = isPassed,
        isCurrent = isCurrent,
        isNext = isNext,
        isMakruh = isMakruh,
        notificationEnabled = enabledSet.contains(raw.type.displayName)
      )
    }

    // 4. Map Voluntary / Sunnah Prayers list for UI
    val updatedVoluntary = cached.voluntary.map { raw ->
      val isTahajjud = raw.type == PrayerType.TAHAJJUD
      val displayMillis = if (isTahajjud && now < cached.fajrMillis) cached.tahajjudEarlyMillis else raw.timeMillis
      val displayEndMillis = if (isTahajjud && now < cached.fajrMillis) cached.fajrMillis else raw.endMillis
      val displayFormatted = if (isTahajjud && now < cached.fajrMillis) formatMillis12h(cached.tahajjudEarlyMillis) else raw.formatted12h
      val displayEndFormatted = if (isTahajjud && now < cached.fajrMillis) format12h(entity.fajr) else raw.endFormatted12h

      val isPassed = when (raw.type) {
        PrayerType.DUHA -> now >= cached.middayZenithMillis // Duha ends at midday solar zenith!
        PrayerType.TAHAJJUD -> now in cached.fajrMillis until cached.tahajjudTonightMillis
        else -> now >= raw.endMillis
      }
      val isCurrent = effectiveCurrentItem?.type == raw.type
      val isNext = nextItem?.type == raw.type

      PrayerItem(
        type = raw.type,
        timeFormatted = displayFormatted,
        timeMillis = displayMillis,
        endTimeFormatted = displayEndFormatted,
        endTimeMillis = displayEndMillis,
        isPassed = isPassed,
        isCurrent = isCurrent,
        isNext = isNext,
        notificationEnabled = enabledSet.contains(raw.type.displayName)
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
        current.voluntaryPrayers == updatedVoluntary &&
        current.forbiddenTimes == updatedForbidden &&
        current.activeForbiddenTime == activeForbidden &&
        current.currentPrayer?.type == effectiveCurrentItem?.type &&
        current.currentPrayer?.timeMillis == effectiveCurrentItem?.timeMillis &&
        current.nextPrayer?.type == nextItem?.type &&
        current.nextPrayer?.timeMillis == nextItem?.timeMillis &&
        current.isCloseToNextPrayer == isCloseToNext &&
        current.lastSyncedFormatted == cached.lastSyncFormatted &&
        current.syncSource == entity.syncSource

    _countdownText.value = countdownStr

    if (!listsUnchanged) {
      // FULL UPDATE: Prayer transition, status change, or notification toggle occurred
      _uiState.update {
        it.copy(
          prayers = updatedObligatory,
          voluntaryPrayers = updatedVoluntary,
          solarTimes = cached.solar,
          forbiddenTimes = updatedForbidden,
          currentPrayer = effectiveCurrentItem,
          activeForbiddenTime = activeForbidden,
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
