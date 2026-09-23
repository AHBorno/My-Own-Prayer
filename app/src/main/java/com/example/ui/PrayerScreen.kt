package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.CityLocation
import com.example.data.model.ForbiddenTimeItem
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerType
import com.example.data.model.SolarTimes
import com.example.update.AppUpdateManager
import com.example.util.AppLanguageHelper
import com.example.util.QiblaHelper
import java.util.Date
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
  viewModel: PrayerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  val onTogglePrayer = remember(viewModel) {
    { prayerName: String, enabled: Boolean ->
      viewModel.togglePrayerNotification(prayerName, enabled)
    }
  }
  val onToggleForbidden = remember(viewModel) {
    { forbiddenName: String, enabled: Boolean ->
      viewModel.toggleForbiddenNotification(forbiddenName, enabled)
    }
  }

  // Side Drawer state
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()

  // Modal dialog states
  var showCityDialog by remember { mutableStateOf(false) }
  var showQiblaCompass by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showLanguageDialog by remember { mutableStateOf(false) }

  // Check Location Permission State
  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    hasLocationPermission = fineGranted || coarseGranted
    if (hasLocationPermission) {
      viewModel.detectCurrentLocation()
    }
  }

  // Check Notification Permission (Android 13+)
  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermission = isGranted
  }

  fun checkLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  // Observe User Feedback events (testMessage in uiState)
  LaunchedEffect(uiState.testMessage) {
    uiState.testMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  // Observe auto GPS trigger on first start if permitted
  LaunchedEffect(Unit) {
    if (checkLocationPermission() && uiState.currentCity == CityLocation.DEFAULT_CITY) {
      viewModel.detectCurrentLocation()
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = true,
    drawerContent = {
      ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
      ) {
        PrayerSideMenuContent(
          uiState = uiState,
          onOpenLanguage = {
            coroutineScope.launch { drawerState.close() }
            showLanguageDialog = true
          },
          onRefreshSync = {
            coroutineScope.launch { drawerState.close() }
            viewModel.syncToday()
          },
          onOpenCitySelector = {
            coroutineScope.launch { drawerState.close() }
            showCityDialog = true
          },
          onDetectGps = {
            coroutineScope.launch { drawerState.close() }
            if (checkLocationPermission()) {
              viewModel.detectCurrentLocation()
            } else {
              locationPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION
                )
              )
            }
          },
          onOpenQibla = {
            coroutineScope.launch { drawerState.close() }
            showQiblaCompass = true
          },
          onOpenTheme = {
            coroutineScope.launch { drawerState.close() }
            showThemeDialog = true
          },
          onCheckUpdates = {
            coroutineScope.launch { drawerState.close() }
            viewModel.checkForAppUpdates(silent = false)
          },
          onTestNotification = {
            coroutineScope.launch { drawerState.close() }
            viewModel.triggerImmediateTestNotification()
          },
          onCloseDrawer = {
            coroutineScope.launch { drawerState.close() }
          }
        )
      }
    }
  ) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
          ),
          navigationIcon = {
            IconButton(
              onClick = {
                coroutineScope.launch {
                  if (drawerState.isClosed) drawerState.open() else drawerState.close()
                }
              },
              modifier = Modifier
                .size(44.dp)
                .testTag("side_menu_button")
            ) {
              Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = AppLanguageHelper.getString("side_menu", uiState.appLanguage),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
              )
            }
          },
          title = {
            val todayFormatted = remember(uiState.appLanguage) {
              AppLanguageHelper.formatFormattedDate(Date(), uiState.appLanguage)
            }
            Column(modifier = Modifier.padding(end = 4.dp)) {
              Text(
                text = "My Own Prayer", // App Name MUST NEVER CHANGE
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = todayFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          },
          actions = {
            // Qibla Compass button
            IconButton(
              onClick = { showQiblaCompass = true },
              modifier = Modifier
                .size(40.dp)
                .testTag("qibla_compass_button")
            ) {
              Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = AppLanguageHelper.getString("qibla_compass", uiState.appLanguage),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }

            // GPS Location detection button
            IconButton(
              onClick = {
                if (checkLocationPermission()) {
                  viewModel.detectCurrentLocation()
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                  )
                }
              },
              enabled = !uiState.isDetectingLocation,
              modifier = Modifier
                .size(40.dp)
                .testTag("gps_detect_button")
            ) {
              if (uiState.isDetectingLocation) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
              } else {
                Icon(
                  imageVector = Icons.Default.MyLocation,
                  contentDescription = AppLanguageHelper.getString("detect_gps", uiState.appLanguage),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            // City selector button (compact pill)
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { showCityDialog = true }
                .testTag("city_select_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = AppLanguageHelper.getString("select_city", uiState.appLanguage),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = uiState.currentCity.name,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.widthIn(max = 80.dp)
                )
              }
            }
          }
        )
      }
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.TopCenter
      ) {
        PullToRefreshBox(
          isRefreshing = uiState.isSyncing,
          onRefresh = { viewModel.syncToday() },
          modifier = Modifier
            .fillMaxSize()
            .testTag("pull_to_refresh_box")
        ) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = 640.dp)
              .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Location Permission Alert (if user denied or hasn't granted location)
            if (!hasLocationPermission) {
              item(key = "location_permission_alert") {
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                  ),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = "Location permission alert",
                      tint = MaterialTheme.colorScheme.onTertiaryContainer,
                      modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = AppLanguageHelper.getString("gps_location_access", uiState.appLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                      )
                      Text(
                        text = AppLanguageHelper.getString("gps_permission_desc", uiState.appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      Button(
                        onClick = {
                          locationPermissionLauncher.launch(
                            arrayOf(
                              Manifest.permission.ACCESS_FINE_LOCATION,
                              Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                          )
                        },
                        modifier = Modifier.testTag("grant_location_permission_button")
                      ) {
                        Text(AppLanguageHelper.getString("enable_gps_button", uiState.appLanguage))
                      }
                    }
                  }
                }
              }
            }

            // Notification Permission Alert
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              item(key = "notification_permission_alert") {
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                  ),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Notifications,
                      contentDescription = "Notification alert",
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = AppLanguageHelper.getString("notif_permission_needed", uiState.appLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                      )
                      Text(
                        text = AppLanguageHelper.getString("notif_permission_desc", uiState.appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      Button(
                        onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.testTag("grant_permission_button")
                      ) {
                        Text(AppLanguageHelper.getString("grant_permission", uiState.appLanguage))
                      }
                    }
                  }
                }
              }
            }

            // Available App Update Alert Banner
            if (uiState.appUpdateInfo?.hasUpdate == true) {
              val updateInfo = uiState.appUpdateInfo!!
              item(key = "app_update_available_banner") {
                val uriHandler = LocalUriHandler.current
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                  ),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openUpdateDialog() }
                    .testTag("app_update_banner")
                ) {
                  Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.SystemUpdate,
                      contentDescription = "Update available",
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${AppLanguageHelper.getString("update_available_title", uiState.appLanguage)} (v${updateInfo.latestVersionName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                      )
                      Text(
                        text = AppLanguageHelper.getString("update_available_desc", uiState.appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                      )
                      Spacer(modifier = Modifier.height(8.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                          onClick = {
                            if (updateInfo.apkDownloadUrl != null) {
                              AppUpdateManager.startApkDownload(
                                context,
                                updateInfo.apkDownloadUrl,
                                updateInfo.apkFileName
                              )
                            } else {
                              uriHandler.openUri(updateInfo.htmlUrl)
                            }
                          },
                          modifier = Modifier.testTag("banner_download_update_button")
                        ) {
                          Text(AppLanguageHelper.getString("update_now", uiState.appLanguage))
                        }
                        OutlinedButton(
                          onClick = { viewModel.openUpdateDialog() }
                        ) {
                          Text(AppLanguageHelper.getString("view_notes", uiState.appLanguage))
                        }
                      }
                    }
                  }
                }
              }
            }

            // Hero Card: Current/Next Prayer & Live Countdown (with Qibla button click)
            item(key = "hero_card") {
              NextPrayerHeroCard(
                currentPrayer = uiState.currentPrayer,
                activeForbiddenTime = uiState.activeForbiddenTime,
                nextPrayer = uiState.nextPrayer,
                isCloseToNextPrayer = uiState.isCloseToNextPrayer,
                countdownText = uiState.countdownText,
                countdownFlow = viewModel.countdownText,
                cityName = uiState.currentCity.displayName,
                cityLocation = uiState.currentCity,
                appLanguage = uiState.appLanguage,
                onOpenQibla = { showQiblaCompass = true }
              )
            }

            // Section Title: Obligatory Prayers
            item(key = "obligatory_section_header") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = AppLanguageHelper.getString("obligatory_prayers_title", uiState.appLanguage),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f, fill = false),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = AppLanguageHelper.getString("tap_bell_hint", uiState.appLanguage),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // Obligatory prayer list items (Fajr, Dhuhr, Asr, Maghrib, Isha)
            items(
              items = uiState.prayers,
              key = { "obligatory_${it.type.name}" },
              contentType = { "prayer_row" }
            ) { prayerItem ->
              PrayerRowCard(
                prayer = prayerItem,
                appLanguage = uiState.appLanguage,
                onToggleNotification = onTogglePrayer
              )
            }

            // SEPARATE SECTION: SOLAR EVENTS (SUNRISE & SUNSET)
            item(key = "solar_times_card") {
              SolarTimesCard(
                solarTimes = uiState.solarTimes,
                appLanguage = uiState.appLanguage
              )
            }

            // Voluntary Prayers Section (Ishraq, Duha, Tahajjud)
            if (uiState.voluntaryPrayers.isNotEmpty()) {
              item(key = "voluntary_section_header") {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = AppLanguageHelper.getString("voluntary_prayers_title", uiState.appLanguage),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = AppLanguageHelper.getString("voluntary_prayers_subtitle", uiState.appLanguage),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                  ) {
                    Text(
                      text = AppLanguageHelper.getString("sunnah_badge", uiState.appLanguage),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              items(
                items = uiState.voluntaryPrayers,
                key = { "voluntary_${it.type.name}" },
                contentType = { "prayer_row" }
              ) { prayerItem ->
                PrayerRowCard(
                  prayer = prayerItem,
                  appLanguage = uiState.appLanguage,
                  onToggleNotification = onTogglePrayer
                )
              }
            }

            // FORBIDDEN PRAYER TIMES SECTION (RED THEME)
            item(key = "forbidden_times_card") {
              ForbiddenPrayerTimesCard(
                forbiddenTimes = uiState.forbiddenTimes,
                appLanguage = uiState.appLanguage,
                onToggleNotification = onToggleForbidden
              )
            }

            // Battery Architecture Explanation Card
            item(key = "battery_architecture_card") {
              BatteryArchitectureCard(
                syncSource = uiState.syncSource,
                lastSynced = uiState.lastSyncedFormatted,
                appLanguage = uiState.appLanguage
              )
            }

            // Testing & Verification Section
            item(key = "testing_verification_card") {
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp)
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Outlined.Shield,
                      contentDescription = "Test notifications",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = AppLanguageHelper.getString("verify_notif_title", uiState.appLanguage),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = AppLanguageHelper.getString("verify_notif_desc", uiState.appLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = { viewModel.triggerImmediateTestNotification() },
                      modifier = Modifier
                        .weight(1f)
                        .testTag("test_notif_button")
                    ) {
                      Text(AppLanguageHelper.getString("instant_alert_btn", uiState.appLanguage), fontSize = 13.sp)
                    }
                    OutlinedButton(
                      onClick = { viewModel.scheduleTestAlarm(10) },
                      modifier = Modifier
                        .weight(1f)
                        .testTag("test_alarm_button")
                    ) {
                      Text(AppLanguageHelper.getString("alarm_10s_btn", uiState.appLanguage), fontSize = 13.sp)
                    }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(
                    onClick = { viewModel.togglePreviewCloseMode() },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("test_toggle_preview_mode_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      AppLanguageHelper.getString(
                        if (uiState.isPreviewCloseMode) "reset_realtime_mode" else "test_flip_mode",
                        uiState.appLanguage
                      ),
                      fontSize = 13.sp
                    )
                  }
                }
              }
            }

            // Copyright & Creator Attribution at Bottom
            item(key = "footer_attribution") {
              val uriHandler = LocalUriHandler.current
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "The Quran Site",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                  ),
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier
                    .clickable {
                      uriHandler.openUri("https://the-quran-site.blogspot.com")
                    }
                    .padding(4.dp)
                    .testTag("the_quran_site_link")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Version ${BuildConfig.VERSION_NAME}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier
                    .clickable {
                      viewModel.checkForAppUpdates(silent = false)
                    }
                    .padding(4.dp)
                    .testTag("app_version_text")
                )
                Text(
                  text = AppLanguageHelper.getString("check_updates", uiState.appLanguage),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier
                    .clickable {
                      viewModel.checkForAppUpdates(silent = false)
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("check_for_updates_button")
                )
              }
            }
          }
        }
      }
    }
  }

  // Language Selection Dialog
  if (showLanguageDialog) {
    LanguageSelectionDialog(
      currentLanguage = uiState.appLanguage,
      onLanguageSelected = { lang ->
        viewModel.setLanguage(lang)
      },
      onDismiss = { showLanguageDialog = false }
    )
  }

  // Theme Selection Dialog
  if (showThemeDialog) {
    ThemeSelectionDialog(
      currentTheme = uiState.appTheme,
      onThemeSelected = { theme ->
        viewModel.setTheme(theme)
      },
      onDismiss = { showThemeDialog = false },
      appLanguage = uiState.appLanguage
    )
  }

  // Qibla Compass Dialog
  if (showQiblaCompass) {
    QiblaCompassDialog(
      cityLocation = uiState.currentCity,
      appLanguage = uiState.appLanguage,
      onDismiss = { showQiblaCompass = false }
    )
  }

  // City Selector Dialog with GPS Auto-detection option
  if (showCityDialog) {
    CitySelectionDialog(
      currentCity = uiState.currentCity,
      appLanguage = uiState.appLanguage,
      onSelectGps = {
        showCityDialog = false
        if (checkLocationPermission()) {
          viewModel.detectCurrentLocation()
        } else {
          locationPermissionLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION
            )
          )
        }
      },
      onSelect = { selectedCity ->
        viewModel.selectCity(selectedCity)
        showCityDialog = false
      },
      onDismiss = { showCityDialog = false }
    )
  }

  // App Update Modal Dialog
  if (uiState.showUpdateDialog && uiState.appUpdateInfo != null) {
    AppUpdateDialog(
      updateInfo = uiState.appUpdateInfo!!,
      onDismiss = { viewModel.dismissUpdateDialog() }
    )
  }
}

/**
 * Dialog showing GitHub release update details and download action.
 */
@Composable
fun AppUpdateDialog(
  updateInfo: com.example.update.AppUpdateInfo,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.SystemUpdate,
        contentDescription = "App Update",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "New Update Available: v${updateInfo.latestVersionName}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Current Version: v${updateInfo.currentVersionName}\nLatest Version: v${updateInfo.latestVersionName}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "What's New:",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = updateInfo.releaseNotes.ifBlank { "Performance improvements, fixes, and updates." },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onDismiss()
          if (updateInfo.apkDownloadUrl != null) {
            AppUpdateManager.startApkDownload(
              context,
              updateInfo.apkDownloadUrl,
              updateInfo.apkFileName
            )
          } else {
            uriHandler.openUri(updateInfo.htmlUrl)
          }
        },
        modifier = Modifier.testTag("dialog_update_button")
      ) {
        Text("Download & Install")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Later")
      }
    }
  )
}

/**
 * Interactive Qibla Compass Dialog using live device sensors.
 */
@Composable
fun QiblaCompassDialog(
  cityLocation: CityLocation,
  appLanguage: String = "en",
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val heading by remember(context) {
    QiblaHelper.getCompassHeadingFlow(context)
  }.collectAsState(initial = 0f)

  val qiblaBearing = remember(cityLocation) {
    QiblaHelper.calculateQiblaBearing(cityLocation.latitude, cityLocation.longitude)
  }

  val distanceKm = remember(cityLocation) {
    QiblaHelper.calculateDistanceKm(cityLocation.latitude, cityLocation.longitude)
  }

  // Relative angle of Qibla to the device's current heading
  val relativeAngle = ((qiblaBearing - heading + 360f) % 360f)
  val isFacingQibla = relativeAngle in 356f..360f || relativeAngle in 0f..4f

  val animatedBorderColor by animateColorAsState(
    targetValue = if (isFacingQibla) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
    label = "compassBorder"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Explore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = AppLanguageHelper.getString("qibla_compass_title", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${cityLocation.name}, ${cityLocation.country}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Visual Compass Dial
        Box(
          modifier = Modifier
            .size(230.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(3.dp, animatedBorderColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          // Compass ring markings (N, E, S, W) rotated with device heading
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer(rotationZ = -heading)
          ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 12.dp.toPx()

            for (i in 0 until 360 step 30) {
              val rad = Math.toRadians(i.toDouble() - 90.0)
              val start = Offset(
                (center.x + (radius - 8.dp.toPx()) * Math.cos(rad)).toFloat(),
                (center.y + (radius - 8.dp.toPx()) * Math.sin(rad)).toFloat()
              )
              val end = Offset(
                (center.x + radius * Math.cos(rad)).toFloat(),
                (center.y + radius * Math.sin(rad)).toFloat()
              )
              drawLine(
                color = if (i % 90 == 0) Color.Gray else Color.LightGray,
                start = start,
                end = end,
                strokeWidth = if (i % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round
              )
            }
          }

          // Qibla direction pointer needle
          Canvas(
            modifier = Modifier
              .size(190.dp)
              .graphicsLayer(rotationZ = relativeAngle)
          ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val needleLength = size.minDimension / 2f - 8.dp.toPx()

            val pointerPath = Path().apply {
              moveTo(center.x, center.y - needleLength)
              lineTo(center.x - 14.dp.toPx(), center.y + 10.dp.toPx())
              lineTo(center.x, center.y - 4.dp.toPx())
              lineTo(center.x + 14.dp.toPx(), center.y + 10.dp.toPx())
              close()
            }
            drawPath(
              path = pointerPath,
              color = if (isFacingQibla) Color(0xFF10B981) else Color(0xFF059669)
            )

            // Tail of needle
            drawLine(
              color = Color.Gray.copy(alpha = 0.5f),
              start = Offset(center.x, center.y),
              end = Offset(center.x, center.y + needleLength * 0.7f),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )
          }

          // Center Kaaba Emblem
          Surface(
            shape = CircleShape,
            color = if (isFacingQibla) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(54.dp),
            shadowElevation = 4.dp
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Navigation,
                  contentDescription = "Qibla pointer",
                  tint = Color.White,
                  modifier = Modifier
                    .size(22.dp)
                    .rotate(relativeAngle)
                )
                Text(
                  text = AppLanguageHelper.getString("kaaba", appLanguage),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direction Status Banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isFacingQibla) Color(0xFF10B981).copy(alpha = 0.18f)
                 else MaterialTheme.colorScheme.surfaceVariant,
          border = if (isFacingQibla) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)) else null,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp)
          ) {
            Text(
              text = if (isFacingQibla) {
                AppLanguageHelper.getString("facing_qibla", appLanguage)
              } else {
                AppLanguageHelper.getString("turn_towards_arrow", appLanguage)
              },
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = if (isFacingQibla) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Info Cards (Angle & Distance)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = AppLanguageHelper.getString("qibla_angle", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${AppLanguageHelper.localizeNumbers("${qiblaBearing.toInt()}°", appLanguage)} ${AppLanguageHelper.getString("from_north", appLanguage)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = AppLanguageHelper.getString("distance_kaaba", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${AppLanguageHelper.localizeNumbers(distanceKm.toString(), appLanguage)} ${AppLanguageHelper.getString("km", appLanguage)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = AppLanguageHelper.getString("qibla_instructions", appLanguage),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(AppLanguageHelper.getString("close", appLanguage))
      }
    }
  )
}

/**
 * Dedicated Card for Forbidden Prayer Times in RED THEME.
 */
@Composable
fun ForbiddenPrayerTimesCard(
  forbiddenTimes: List<ForbiddenTimeItem>,
  appLanguage: String = "en",
  onToggleNotification: (String, Boolean) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val redContainerBg = if (isDark) Color(0xFF2A0D10) else Color(0xFFFEE2E2)
  val redBorderColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFF87171)
  val redTitleColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
  val redBodyColor = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)
  val redActiveBadgeBg = Color(0xFFDC2626)

  val anyActive = forbiddenTimes.any { it.isActiveNow }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.5.dp, redBorderColor, RoundedCornerShape(20.dp)),
    colors = CardDefaults.cardColors(containerColor = redContainerBg),
    shape = RoundedCornerShape(20.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f, fill = false),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Forbidden prayer time warning",
            tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = AppLanguageHelper.getString("forbidden_prayers_title", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = redTitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (anyActive) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = redActiveBadgeBg
          ) {
            Text(
              text = AppLanguageHelper.getString("prohibited_now", appLanguage),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = AppLanguageHelper.getString("forbidden_prayers_subtitle", appLanguage),
        style = MaterialTheme.typography.bodySmall,
        color = redBodyColor.copy(alpha = 0.9f)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        forbiddenTimes.forEach { item ->
          ForbiddenRow(
            item = item,
            isDark = isDark,
            titleColor = redTitleColor,
            bodyColor = redBodyColor,
            appLanguage = appLanguage,
            onToggleNotification = onToggleNotification
          )
        }
      }
    }
  }
}

@Composable
fun ForbiddenRow(
  item: ForbiddenTimeItem,
  isDark: Boolean,
  titleColor: Color,
  bodyColor: Color,
  appLanguage: String = "en",
  onToggleNotification: (String, Boolean) -> Unit = { _, _ -> }
) {
  val rowBg = if (item.isActiveNow) {
    if (isDark) Color(0xFF4C1D24) else Color(0xFFFECDD3)
  } else {
    if (isDark) Color.Transparent else Color(0x33FFFFFF)
  }

  val localizedName = item.getLocalizedName(appLanguage)
  val localizedDesc = item.getLocalizedDescription(appLanguage)
  val localizedInterval = AppLanguageHelper.localizeInterval(item.intervalFormatted, appLanguage)

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = rowBg,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = localizedName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
              .weight(1f, fill = false)
              .basicMarquee(iterations = Int.MAX_VALUE)
          )
          if (item.isActiveNow) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFDC2626)
            ) {
              Text(
                text = AppLanguageHelper.getString("now_badge", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = localizedDesc,
          style = MaterialTheme.typography.bodySmall,
          color = bodyColor.copy(alpha = 0.85f),
          fontSize = 11.sp,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier
            .fillMaxWidth()
            .basicMarquee(iterations = Int.MAX_VALUE)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = localizedInterval.replace(" ", "\u00A0"),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = titleColor,
          maxLines = 1,
          softWrap = false
        )

        Spacer(modifier = Modifier.width(6.dp))

        Box(
          modifier = Modifier.size(44.dp),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = CircleShape,
            color = if (item.notificationEnabled) {
              Color(0xFFDC2626).copy(alpha = if (isDark) 0.35f else 0.2f)
            } else {
              if (isDark) Color(0x22FFFFFF) else Color(0x1ADC2626)
            },
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .clickable { onToggleNotification(item.name, !item.notificationEnabled) }
              .testTag("forbidden_notif_toggle_${item.name.replace(" ", "_").lowercase()}")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = if (item.notificationEnabled) Icons.Default.Notifications
                              else Icons.Default.NotificationsOff,
                contentDescription = "Toggle notification for ${item.name}",
                tint = if (item.notificationEnabled) {
                  if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                } else {
                  titleColor.copy(alpha = 0.75f)
                },
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Dedicated Card for Solar Events (Sunrise & Sunset).
 */
@Composable
fun SolarTimesCard(
  solarTimes: SolarTimes,
  appLanguage: String = "en",
  modifier: Modifier = Modifier
) {
  val sunriseFormatted = AppLanguageHelper.localizeTime(solarTimes.sunriseFormatted, appLanguage)
  val sunsetFormatted = AppLanguageHelper.localizeTime(solarTimes.sunsetFormatted, appLanguage)

  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = AppLanguageHelper.getString("solar_transitions_title", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = AppLanguageHelper.getString("solar_transitions_subtitle", appLanguage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = AppLanguageHelper.getString("sun_schedule_badge", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Sunrise Column
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Brightness5,
                contentDescription = "Sunrise",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = AppLanguageHelper.getString("sunrise", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = sunriseFormatted.replace(" ", "\u00A0"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Sunset Column
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEDD5)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.WbTwilight,
                contentDescription = "Sunset",
                tint = Color(0xFFEA580C),
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = AppLanguageHelper.getString("sunset", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = sunsetFormatted.replace(" ", "\u00A0"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NextPrayerHeroCard(
  currentPrayer: PrayerItem? = null,
  activeForbiddenTime: ForbiddenTimeItem? = null,
  nextPrayer: PrayerItem? = null,
  isCloseToNextPrayer: Boolean = false,
  countdownText: String = "--:--:--",
  countdownFlow: StateFlow<String>? = null,
  cityName: String = "Makkah",
  cityLocation: CityLocation = CityLocation.DEFAULT_CITY,
  appLanguage: String = "en",
  onOpenQibla: () -> Unit = {}
) {
  val qiblaBearing = remember(cityLocation) {
    QiblaHelper.calculateQiblaBearing(cityLocation.latitude, cityLocation.longitude)
  }

  val isForbidden = activeForbiddenTime != null

  // When forbidden: Forbidden time is always prominently on top.
  // Otherwise, when almost close to next prayer: upcoming is on top and current is below.
  val showUpcomingOnTop = !isForbidden && (isCloseToNextPrayer || currentPrayer == null)

  val topPrayer = if (showUpcomingOnTop) nextPrayer else (currentPrayer ?: nextPrayer)
  val bottomPrayer = if (showUpcomingOnTop) currentPrayer else nextPrayer

  val topTag = when {
    isForbidden -> AppLanguageHelper.getString("forbidden_time_tag", appLanguage)
    showUpcomingOnTop -> AppLanguageHelper.getString("upcoming_prayer_tag", appLanguage)
    else -> AppLanguageHelper.getString("current_prayer_tag", appLanguage)
  }

  val topPrayerName = when {
    isForbidden -> activeForbiddenTime!!.getLocalizedName(appLanguage)
    else -> topPrayer?.type?.getLocalizedName(appLanguage) ?: "Prayer"
  }

  val topTimeFormatted = when {
    isForbidden -> AppLanguageHelper.localizeInterval(activeForbiddenTime!!.intervalFormatted, appLanguage)
    else -> AppLanguageHelper.localizeTime(topPrayer?.timeFormatted ?: "--:--", appLanguage)
  }

  val topArabicName = when {
    isForbidden -> activeForbiddenTime!!.arabicName
    else -> topPrayer?.type?.arabicName ?: ""
  }

  val cardGradient = if (isForbidden) {
    listOf(
      Color(0xFF58101C),
      Color(0xFF7F1D1D),
      Color(0xFF450A0A)
    )
  } else {
    listOf(
      Color(0xFF064E3B),
      Color(0xFF047857),
      Color(0xFF0F5132)
    )
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(Brush.linearGradient(cardGradient))
        .padding(18.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(modifier = Modifier.weight(1f)) {
            val tagBg = when {
              isForbidden -> Color(0x52EF4444)
              showUpcomingOnTop -> Color(0x3DFDE047)
              else -> Color(0x33FFFFFF)
            }
            val tagTextColor = when {
              isForbidden -> Color(0xFFFECACA)
              showUpcomingOnTop -> Color(0xFFFDE047)
              else -> Color(0xFFFEF3C7)
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = tagBg
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isForbidden) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFECACA),
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                } else if (showUpcomingOnTop) {
                  Icon(
                    imageVector = Icons.Outlined.Brightness5,
                    contentDescription = null,
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                } else {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF86EFAC))
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                  text = topTag,
                  style = MaterialTheme.typography.labelSmall,
                  color = tagTextColor,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = topPrayerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
              if (topArabicName.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = topArabicName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Normal,
                  color = if (isForbidden) Color(0xFFFECACA) else Color(0xCCFEF3C7),
                  maxLines = 1,
                  softWrap = false
                )
              }
            }

            Text(
              text = topTimeFormatted.replace(" ", "\u00A0"),
              style = MaterialTheme.typography.titleLarge,
              color = if (isForbidden) Color(0xFFFECACA) else Color(0xFFFDE047),
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false
            )

            if (isForbidden) {
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = AppLanguageHelper.getString("prohibited_now_sub", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xEEFECACA),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column(horizontalAlignment = Alignment.End) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0x33FFFFFF)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = Color(0xFFFEF3C7),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = cityName.split(",")[0],
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFFEF3C7),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.widthIn(max = 110.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Qibla button in hero card
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0x26FFFFFF),
              modifier = Modifier.clickable { onOpenQibla() }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Explore,
                  contentDescription = "Qibla direction",
                  tint = Color(0xFFFDE047),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${AppLanguageHelper.getString("qibla", appLanguage)} ${AppLanguageHelper.localizeNumbers("${qiblaBearing.toInt()}°", appLanguage)}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFDE047),
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic placement based on closeness / forbidden status:
        if (isForbidden) {
          // In forbidden time: Show next upcoming prayer & countdown until forbidden time ends / prayer begins
          if (nextPrayer != null) {
            CompactUpcomingPrayerBar(
              prayer = nextPrayer,
              countdownText = countdownText,
              countdownFlow = countdownFlow,
              appLanguage = appLanguage
            )
          } else {
            CountdownTimerBar(countdownText = countdownText, countdownFlow = countdownFlow, appLanguage = appLanguage)
          }
        } else if (showUpcomingOnTop) {
          // Approaching next prayer: Countdown bar is on top, current prayer is smaller below
          CountdownTimerBar(countdownText = countdownText, countdownFlow = countdownFlow, appLanguage = appLanguage)

          if (bottomPrayer != null) {
            Spacer(modifier = Modifier.height(10.dp))
            CompactSecondaryPrayerBar(
              label = AppLanguageHelper.getString("current_prefix", appLanguage).replace(":", "").trim(),
              prayer = bottomPrayer,
              statusText = AppLanguageHelper.getString("ending_soon", appLanguage),
              appLanguage = appLanguage
            )
          }
        } else {
          // Normal: Current prayer is on top, upcoming prayer is moved below and smaller
          if (bottomPrayer != null) {
            CompactUpcomingPrayerBar(
              prayer = bottomPrayer,
              countdownText = countdownText,
              countdownFlow = countdownFlow,
              appLanguage = appLanguage
            )
          } else {
            CountdownTimerBar(countdownText = countdownText, countdownFlow = countdownFlow, appLanguage = appLanguage)
          }
        }
      }
    }
  }
}

@Composable
private fun CompactUpcomingPrayerBar(
  prayer: PrayerItem,
  countdownText: String,
  countdownFlow: StateFlow<String>? = null,
  appLanguage: String = "en"
) {
  val icon = getPrayerIcon(prayer.type)
  val liveCountdown = countdownFlow?.collectAsStateWithLifecycle()?.value ?: countdownText
  val localizedName = prayer.type.getLocalizedName(appLanguage)
  val localizedTime = AppLanguageHelper.localizeTime(prayer.timeFormatted, appLanguage)
  val localizedCountdown = AppLanguageHelper.localizeCountdown(liveCountdown, appLanguage)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0x28000000),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFDE047),
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = buildAnnotatedString {
              append(AppLanguageHelper.getString("upcoming_prefix", appLanguage))
              withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                append(localizedName)
              }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xCCFFFFFF),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
          )
          Text(
            text = localizedTime.replace(" ", "\u00A0"),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFDE047),
            maxLines = 1,
            softWrap = false
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x38000000)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Brightness5,
            contentDescription = null,
            tint = Color(0xFFFDE047),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${AppLanguageHelper.getString("in_prefix", appLanguage)}$localizedCountdown",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFDE047),
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}

@Composable
private fun CompactSecondaryPrayerBar(
  label: String,
  prayer: PrayerItem,
  statusText: String,
  appLanguage: String = "en"
) {
  val icon = getPrayerIcon(prayer.type)
  val localizedName = prayer.type.getLocalizedName(appLanguage)
  val localizedTime = AppLanguageHelper.localizeTime(prayer.timeFormatted, appLanguage)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0x28000000),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xCCFEF3C7),
            modifier = Modifier.size(15.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = buildAnnotatedString {
            append("$label: ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)) {
              append(localizedName)
            }
            append(" (${localizedTime.replace(" ", "\u00A0")})")
          },
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xB3FFFFFF),
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0x28FFFFFF)
      ) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xEEFFFFFF),
          maxLines = 1,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun CountdownTimerBar(
  countdownText: String,
  countdownFlow: StateFlow<String>? = null,
  appLanguage: String = "en"
) {
  val liveCountdown = countdownFlow?.collectAsStateWithLifecycle()?.value ?: countdownText
  val localizedCountdown = AppLanguageHelper.localizeCountdown(liveCountdown, appLanguage)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0x24000000),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          imageVector = Icons.Outlined.Brightness5,
          contentDescription = null,
          tint = Color(0xFFFDE047),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = AppLanguageHelper.getString("time_remaining", appLanguage),
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xE6FFFFFF),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = localizedCountdown,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFDE047),
        letterSpacing = 0.5.sp,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
fun BatteryArchitectureCard(
  syncSource: String,
  lastSynced: String,
  appLanguage: String = "en"
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.BatterySaver,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = AppLanguageHelper.getString("battery_arch_title", appLanguage),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      ArchitectureBullet(
        title = AppLanguageHelper.getString("battery_bullet_1_title", appLanguage),
        desc = AppLanguageHelper.getString("battery_bullet_1_desc", appLanguage)
      )
      Spacer(modifier = Modifier.height(4.dp))
      ArchitectureBullet(
        title = AppLanguageHelper.getString("battery_bullet_2_title", appLanguage),
        desc = AppLanguageHelper.getString("battery_bullet_2_desc", appLanguage)
      )
      Spacer(modifier = Modifier.height(4.dp))
      ArchitectureBullet(
        title = AppLanguageHelper.getString("battery_bullet_3_title", appLanguage),
        desc = "${AppLanguageHelper.getString("battery_bullet_3_desc", appLanguage)} ($syncSource - $lastSynced)"
      )
    }
  }
}

@Composable
fun ArchitectureBullet(title: String, desc: String) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .size(16.dp)
        .padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
    }
  }
}

@Composable
fun PrayerRowCard(
  prayer: PrayerItem,
  appLanguage: String = "en",
  onToggleNotification: (String, Boolean) -> Unit = { _, _ -> }
) {
  val isHighlighted = prayer.isNext
  val isCurrent = prayer.isCurrent
  val isPassed = prayer.isPassed

  val cardBg = when {
    isHighlighted -> MaterialTheme.colorScheme.primaryContainer
    isCurrent -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    isPassed -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    else -> MaterialTheme.colorScheme.surface
  }

  val textColor = when {
    isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
    isCurrent -> MaterialTheme.colorScheme.onSurface
    isPassed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    else -> MaterialTheme.colorScheme.onSurface
  }

  val icon = getPrayerIcon(prayer.type)
  val localizedName = prayer.type.getLocalizedName(appLanguage)
  val localizedDesc = prayer.type.getLocalizedDescription(appLanguage)
  val localizedTime = AppLanguageHelper.localizeTime(prayer.timeFormatted, appLanguage)

  Card(
    modifier = Modifier.fillMaxWidth(),
    border = when {
      isHighlighted -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
      isCurrent -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
      else -> null
    },
    colors = CardDefaults.cardColors(containerColor = cardBg),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 2.dp else 0.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              when {
                isHighlighted -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = localizedName,
            tint = when {
              isHighlighted -> MaterialTheme.colorScheme.onPrimary
              isCurrent -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = localizedName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (isHighlighted || isCurrent) FontWeight.Bold else FontWeight.Medium,
              color = textColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (isHighlighted) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary
              ) {
                Text(
                  text = AppLanguageHelper.getString("next_badge", appLanguage),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            } else if (isCurrent) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondary
              ) {
                Text(
                  text = AppLanguageHelper.getString("current_badge", appLanguage),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
          val bottomText = when {
            isCurrent -> "${AppLanguageHelper.getString("current_prayer_prefix", appLanguage)}$localizedDesc"
            localizedDesc.isNotEmpty() -> if (isPassed) "${AppLanguageHelper.getString("passed_prefix", appLanguage)}$localizedDesc" else localizedDesc
            isPassed -> AppLanguageHelper.getString("passed_label", appLanguage)
            else -> ""
          }

          if (bottomText.isNotEmpty()) {
            Text(
              text = bottomText,
              style = MaterialTheme.typography.labelSmall,
              color = if (isCurrent) MaterialTheme.colorScheme.primary else textColor.copy(alpha = if (isPassed) 0.6f else 0.65f),
              fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = localizedTime.replace(" ", "\u00A0"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = textColor,
          maxLines = 1,
          softWrap = false
        )

        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier.size(40.dp),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = CircleShape,
            color = if (prayer.notificationEnabled) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
              if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0x24FFFFFF) else Color(0x12000000)
            },
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .clickable { onToggleNotification(prayer.type.displayName, !prayer.notificationEnabled) }
              .testTag("prayer_notif_toggle_${prayer.type.name.lowercase()}")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = if (prayer.notificationEnabled) Icons.Default.Notifications
                              else Icons.Default.NotificationsOff,
                contentDescription = "Toggle alert for $localizedName",
                tint = if (prayer.notificationEnabled) MaterialTheme.colorScheme.primary
                       else textColor.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CitySelectionDialog(
  currentCity: CityLocation,
  appLanguage: String = "en",
  onSelectGps: () -> Unit,
  onSelect: (CityLocation) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredCities = remember(searchQuery) {
    if (searchQuery.isBlank()) {
      CityLocation.PRESET_CITIES
    } else {
      val query = searchQuery.trim().lowercase()
      CityLocation.PRESET_CITIES.filter {
        it.name.lowercase().contains(query) || it.country.lowercase().contains(query)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = AppLanguageHelper.getString("select_location", appLanguage),
          style = MaterialTheme.typography.titleLarge
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(440.dp)
      ) {
        // GPS Auto-detect card
        Card(
          onClick = onSelectGps,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("gps_dialog_item")
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = AppLanguageHelper.getString("use_gps", appLanguage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = AppLanguageHelper.getString("auto_detect_gps", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }

        // Search Bar for 270+ cities
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("city_search_input"),
          placeholder = { Text(AppLanguageHelper.getString("search_cities_placeholder", appLanguage)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          )
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${AppLanguageHelper.getString("world_cities", appLanguage)} (${AppLanguageHelper.localizeNumbers(filteredCities.size.toString(), appLanguage)})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (searchQuery.isNotBlank()) {
            Text(
              text = AppLanguageHelper.getString("filtered", appLanguage),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        if (filteredCities.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = AppLanguageHelper.getString("no_cities_found", appLanguage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(filteredCities, key = { "${it.name}_${it.country}" }) { city ->
              val isSelected = city.name == currentCity.name && city.country == currentCity.country
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                  )
                  .clickable { onSelect(city) }
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = city.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = city.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(AppLanguageHelper.getString("close", appLanguage))
      }
    }
  )
}

fun getPrayerIcon(type: PrayerType): ImageVector {
  return when (type) {
    PrayerType.FAJR -> Icons.Outlined.WbTwilight
    PrayerType.SUNRISE -> Icons.Outlined.Brightness5
    PrayerType.ISHRAQ -> Icons.Outlined.Brightness5
    PrayerType.DUHA -> Icons.Default.WbSunny
    PrayerType.DHUHR -> Icons.Default.WbSunny
    PrayerType.ASR -> Icons.Outlined.Brightness6
    PrayerType.MAGHRIB -> Icons.Outlined.WbTwilight
    PrayerType.ISHA -> Icons.Outlined.Bedtime
    PrayerType.TAHAJJUD -> Icons.Outlined.Nightlight
  }
}

/**
 * Side menu drawer content providing easy access to Language selection,
 * Refresh prayer times, City selection, GPS, Qibla compass, Theme, and Updates.
 */
@Composable
fun PrayerSideMenuContent(
  uiState: PrayerUiState,
  onOpenLanguage: () -> Unit,
  onRefreshSync: () -> Unit,
  onOpenCitySelector: () -> Unit,
  onDetectGps: () -> Unit,
  onOpenQibla: () -> Unit,
  onOpenTheme: () -> Unit,
  onCheckUpdates: () -> Unit,
  onTestNotification: () -> Unit,
  onCloseDrawer: () -> Unit
) {
  val lang = uiState.appLanguage
  val currentLanguageName = when (lang) {
    "bn" -> "বাংলা (Bengali)"
    "ar" -> "العربية (Arabic)"
    else -> "English"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Top App Branding & Mosque Header
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "My Own Prayer", // App Name MUST NEVER CHANGE
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "${uiState.currentCity.name}, ${uiState.currentCity.country}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Navigation Menu Items
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // 1. Language Selection
      item(key = "drawer_lang") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.Translate,
              contentDescription = "Language",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Column {
              Text(
                text = AppLanguageHelper.getString("language", lang),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = currentLanguageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          },
          badge = {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
              Text(
                text = lang.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          },
          selected = false,
          onClick = onOpenLanguage,
          modifier = Modifier.testTag("drawer_item_language")
        )
      }

      // 2. Refresh Prayers / Sync Now
      item(key = "drawer_refresh") {
        NavigationDrawerItem(
          icon = {
            if (uiState.isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          },
          label = {
            Column {
              Text(
                text = AppLanguageHelper.getString("refresh_schedule", lang),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (uiState.isSyncing) {
                  AppLanguageHelper.getString("refreshing", lang)
                } else {
                  "${AppLanguageHelper.getString("last_synced", lang)}: ${uiState.lastSyncedFormatted}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          selected = false,
          onClick = onRefreshSync,
          modifier = Modifier.testTag("drawer_item_refresh")
        )
      }

      // 3. Select City
      item(key = "drawer_city") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Select City",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Text(
              text = AppLanguageHelper.getString("select_city", lang),
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = false,
          onClick = onOpenCitySelector,
          modifier = Modifier.testTag("drawer_item_city")
        )
      }

      // 4. GPS Auto-Detect Location
      item(key = "drawer_gps") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = "GPS Detect",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Text(
              text = AppLanguageHelper.getString("detect_gps", lang),
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = false,
          onClick = onDetectGps,
          modifier = Modifier.testTag("drawer_item_gps")
        )
      }

      // 5. Qibla Compass
      item(key = "drawer_qibla") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.Explore,
              contentDescription = "Qibla Compass",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Text(
              text = AppLanguageHelper.getString("qibla_compass", lang),
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = false,
          onClick = onOpenQibla,
          modifier = Modifier.testTag("drawer_item_qibla")
        )
      }

      // 6. Theme & Appearance
      item(key = "drawer_theme") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = "Theme",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Text(
              text = AppLanguageHelper.getString("theme", lang),
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = false,
          onClick = onOpenTheme,
          modifier = Modifier.testTag("drawer_item_theme")
        )
      }

      // 7. Check for Updates
      item(key = "drawer_updates") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.SystemUpdate,
              contentDescription = "Check Updates",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Text(
              text = AppLanguageHelper.getString("check_updates", lang),
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = false,
          onClick = onCheckUpdates,
          modifier = Modifier.testTag("drawer_item_updates")
        )
      }

      // 8. Test Prayer Notification
      item(key = "drawer_test_notif") {
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Test Notification",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          label = {
            Text(
              text = AppLanguageHelper.getString("test_notification", lang),
              style = MaterialTheme.typography.labelLarge
            )
          },
          selected = false,
          onClick = onTestNotification,
          modifier = Modifier.testTag("drawer_item_test_notif")
        )
      }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

    // Side Menu Footer
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "The Quran Site • Ashiqul Haque Borno",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

/**
 * Modal dialog for selecting application language:
 * English, Bengali (বাংলা), or Arabic (العربية).
 * Updates both app UI and notification language dynamically.
 */
@Composable
fun LanguageSelectionDialog(
  currentLanguage: String,
  onLanguageSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val languages = listOf(
    Triple("en", "English", "English"),
    Triple("bn", "বাংলা", "Bengali"),
    Triple("ar", "العربية", "Arabic")
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Translate,
        contentDescription = "Language selection",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = AppLanguageHelper.getString("select_language_title", currentLanguage),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = AppLanguageHelper.getString("language_note", currentLanguage),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 6.dp)
        )

        languages.forEach { (code, nativeName, englishName) ->
          val isSelected = currentLanguage == code
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onLanguageSelected(code)
                onDismiss()
              }
              .testTag("lang_option_$code")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = nativeName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                         else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = englishName,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                         else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              RadioButton(
                selected = isSelected,
                onClick = {
                  onLanguageSelected(code)
                  onDismiss()
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_language_dialog_button")
      ) {
        Text(AppLanguageHelper.getString("close", currentLanguage))
      }
    }
  )
}

/**
 * Modal dialog for selecting app theme (System default, Light, Dark).
 */
@Composable
fun ThemeSelectionDialog(
  currentTheme: String,
  onThemeSelected: (String) -> Unit,
  onDismiss: () -> Unit,
  appLanguage: String
) {
  val themes = listOf(
    Triple("system", AppLanguageHelper.getString("system_theme", appLanguage), Icons.Default.Palette),
    Triple("light", AppLanguageHelper.getString("light_theme", appLanguage), Icons.Default.LightMode),
    Triple("dark", AppLanguageHelper.getString("dark_theme", appLanguage), Icons.Default.DarkMode)
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Palette,
        contentDescription = "Theme selection",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = AppLanguageHelper.getString("theme", appLanguage),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        themes.forEach { (key, name, icon) ->
          val isSelected = currentTheme == key
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onThemeSelected(key)
                onDismiss()
              }
              .testTag("theme_option_$key")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                         else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = name,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                         else MaterialTheme.colorScheme.onSurface
                )
              }
              RadioButton(
                selected = isSelected,
                onClick = {
                  onThemeSelected(key)
                  onDismiss()
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_theme_dialog_button")
      ) {
        Text(AppLanguageHelper.getString("close", appLanguage))
      }
    }
  )
}
