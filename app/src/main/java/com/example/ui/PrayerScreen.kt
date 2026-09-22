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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CityLocation
import com.example.data.model.ForbiddenTimeItem
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerType
import com.example.data.model.SolarTimes
import com.example.util.QiblaHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
  viewModel: PrayerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var showCityDialog by remember { mutableStateOf(false) }
  var showQiblaCompass by remember { mutableStateOf(false) }

  // Check notification permission for Android 13+
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

  // Location permissions launcher
  fun checkLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  var hasLocationPermission by remember { mutableStateOf(checkLocationPermission()) }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                  permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    hasLocationPermission = granted
    if (granted) {
      viewModel.detectCurrentLocation()
    }
  }

  // Show snackbar when test/status message updates
  LaunchedEffect(uiState.testMessage) {
    uiState.testMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearTestMessage()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
          val todayFormatted = remember {
            SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
          }
          Column {
            Text(
              text = "My Own Prayer",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = todayFormatted,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          // Qibla Compass button
          IconButton(
            onClick = { showQiblaCompass = true },
            modifier = Modifier.testTag("qibla_compass_button")
          ) {
            Icon(
              imageVector = Icons.Default.Explore,
              contentDescription = "Find Qibla Direction",
              tint = MaterialTheme.colorScheme.primary
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
            modifier = Modifier.testTag("gps_detect_button")
          ) {
            if (uiState.isDetectingLocation) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Detect location via phone GPS",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }

          // City selector button
          TextButton(
            onClick = { showCityDialog = true },
            modifier = Modifier.testTag("city_select_button")
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Select City",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = uiState.currentCity.name,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          // Sync refresh button
          IconButton(
            onClick = { viewModel.syncToday() },
            enabled = !uiState.isSyncing,
            modifier = Modifier.testTag("sync_refresh_button")
          ) {
            if (uiState.isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Sync prayer times",
                tint = MaterialTheme.colorScheme.primary
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
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp)
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
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
                    text = "Notification Permission Needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                  Text(
                    text = "Allow notifications so exact prayer alerts can be delivered on time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                    onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.testTag("grant_permission_button")
                  ) {
                    Text("Grant Permission")
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
            nextPrayer = uiState.nextPrayer,
            isCloseToNextPrayer = uiState.isCloseToNextPrayer,
            countdownText = uiState.countdownText,
            cityName = uiState.currentCity.displayName,
            cityLocation = uiState.currentCity,
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
              text = "Obligatory Prayers (Fard)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Tap bell to toggle alert",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Obligatory prayer list items (Fajr, Dhuhr, Asr, Maghrib, Isha)
        items(uiState.prayers, key = { "obligatory_${it.type.name}" }) { prayerItem ->
          PrayerRowCard(
            prayer = prayerItem,
            onToggleNotification = { enabled ->
              viewModel.togglePrayerNotification(prayerItem.type.displayName, enabled)
            }
          )
        }

        // SEPARATE SECTION: SOLAR EVENTS (SUNRISE & SUNSET)
        item(key = "solar_times_card") {
          SolarTimesCard(
            solarTimes = uiState.solarTimes
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
              Column {
                Text(
                  text = "Voluntary & Sunnah (Nawafil)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Ishraq, Duha & Tahajjud night vigil",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
              ) {
                Text(
                  text = "Sunnah",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
          }

          items(uiState.voluntaryPrayers, key = { "voluntary_${it.type.name}" }) { prayerItem ->
            PrayerRowCard(
              prayer = prayerItem,
              onToggleNotification = { enabled ->
                viewModel.togglePrayerNotification(prayerItem.type.displayName, enabled)
              }
            )
          }
        }

        // FORBIDDEN PRAYER TIMES SECTION (RED THEME)
        item(key = "forbidden_times_card") {
          ForbiddenPrayerTimesCard(
            forbiddenTimes = uiState.forbiddenTimes,
            onToggleNotification = { name, enabled ->
              viewModel.toggleForbiddenNotification(name, enabled)
            }
          )
        }

        // Battery Architecture Explanation Card (Moved to bottom)
        item(key = "battery_architecture_card") {
          BatteryArchitectureCard(
            syncSource = uiState.syncSource,
            lastSynced = uiState.lastSyncedFormatted
          )
        }

        // Testing & Verification Section (Moved to bottom)
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
                  text = "Verify Background-Free Notifications",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Verify that notifications trigger on time without requiring the app to run in the background:",
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
                  Text("Instant Alert", fontSize = 13.sp)
                }
                OutlinedButton(
                  onClick = { viewModel.scheduleTestAlarm(10) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("test_alarm_button")
                ) {
                  Text("10s Alarm (Exit App)", fontSize = 13.sp)
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
                  if (uiState.isPreviewCloseMode) "Reset to Real-time View Mode"
                  else "Test 'Approaching Prayer' Flip Mode",
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
              text = "©Ashiqul Haque Borno",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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
          }
        }
      }
    }
  }

  // Qibla Compass Dialog
  if (showQiblaCompass) {
    QiblaCompassDialog(
      cityLocation = uiState.currentCity,
      onDismiss = { showQiblaCompass = false }
    )
  }

  // City Selector Dialog with GPS Auto-detection option
  if (showCityDialog) {
    CitySelectionDialog(
      currentCity = uiState.currentCity,
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
}

/**
 * Interactive Qibla Compass Dialog using live device sensors.
 */
@Composable
fun QiblaCompassDialog(
  cityLocation: CityLocation,
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Qibla Compass",
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

        if (isFacingQibla) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF10B981)
          ) {
            Text(
              text = "FACING QIBLA",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
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
              .padding(16.dp)
              .graphicsLayer { rotationZ = -heading }
          ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Dial tick lines
            for (angle in 0 until 360 step 30) {
              val rad = Math.toRadians(angle.toDouble())
              val tickLen = if (angle % 90 == 0) 14f else 8f
              val startX = (center.x + (radius - tickLen) * Math.sin(rad)).toFloat()
              val startY = (center.y - (radius - tickLen) * Math.cos(rad)).toFloat()
              val endX = (center.x + radius * Math.sin(rad)).toFloat()
              val endY = (center.y - radius * Math.cos(rad)).toFloat()

              val tickColor = if (angle == 0) Color(0xFFEF4444) else Color.Gray.copy(alpha = 0.5f)
              drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (angle % 90 == 0) 3f else 1.5f,
                cap = StrokeCap.Round
              )
            }
          }

          // Qibla Pointer Arrow pointing towards relative Qibla angle
          Box(
            modifier = Modifier
              .size(210.dp)
              .graphicsLayer { rotationZ = relativeAngle },
            contentAlignment = Alignment.TopCenter
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(top = 10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Qibla Direction",
                tint = if (isFacingQibla) Color(0xFF10B981) else Color(0xFFD97706),
                modifier = Modifier.size(36.dp)
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isFacingQibla) Color(0xFF10B981) else Color(0xFFD97706)
              ) {
                Text(
                  text = "KAABA",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }

          // Center pivot circle with degrees
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
              .size(76.dp)
              .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "${heading.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Heading",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Info Grid
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Qibla Angle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${qiblaBearing.toInt()}° from North",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Distance to Kaaba",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$distanceKm km",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Hold your phone flat and turn until the arrow aligns with the top.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
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
  onToggleNotification: (String, Boolean) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier
) {
  val isDark = isSystemInDarkTheme()
  val redContainerBg = if (isDark) Color(0xFF381014) else Color(0xFFFEF2F2)
  val redBorderColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5)
  val redTitleColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
  val redBodyColor = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)
  val redActiveBadgeBg = Color(0xFFDC2626)

  val anyActive = forbiddenTimes.any { it.isActiveNow }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.5.dp, redBorderColor, RoundedCornerShape(20.dp)),
    colors = CardDefaults.cardColors(containerColor = redContainerBg),
    shape = RoundedCornerShape(20.dp)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Forbidden prayer time warning",
            tint = redTitleColor,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Forbidden Prayer Times",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = redTitleColor
          )
        }

        if (anyActive) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = redActiveBadgeBg
          ) {
            Text(
              text = "PROHIBITED NOW",
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
        text = "Voluntary (Nafl) prayers are strictly prohibited during these 3 solar intervals:",
        style = MaterialTheme.typography.bodySmall,
        color = redBodyColor.copy(alpha = 0.85f)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        forbiddenTimes.forEach { item ->
          ForbiddenRow(
            item = item,
            isDark = isDark,
            titleColor = redTitleColor,
            bodyColor = redBodyColor,
            onToggleNotification = { enabled ->
              onToggleNotification(item.name, enabled)
            }
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
  onToggleNotification: (Boolean) -> Unit = {}
) {
  val rowBg = if (item.isActiveNow) {
    if (isDark) Color(0xFF4C1D24) else Color(0xFFFEE2E2)
  } else {
    Color.Transparent
  }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (item.isActiveNow) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFDC2626)
            ) {
              Text(
                text = "NOW",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
        Text(
          text = item.description,
          style = MaterialTheme.typography.bodySmall,
          color = bodyColor.copy(alpha = 0.8f),
          fontSize = 11.sp,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.intervalFormatted.replace(" ", "\u00A0"),
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
              Color(0xFFDC2626).copy(alpha = 0.22f)
            } else {
              if (isDark) Color(0x22FFFFFF) else Color(0x14000000)
            },
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .clickable { onToggleNotification(!item.notificationEnabled) }
              .testTag("forbidden_notif_toggle_${item.name.replace(" ", "_").lowercase()}")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = if (item.notificationEnabled) Icons.Default.Notifications
                              else Icons.Default.NotificationsOff,
                contentDescription = "Toggle notification for ${item.name}",
                tint = if (item.notificationEnabled) Color(0xFFEF4444)
                       else titleColor.copy(alpha = 0.7f),
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
  modifier: Modifier = Modifier
) {
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
            text = "Solar Transitions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Astronomical horizon timings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = "Sun Schedule",
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Sunrise Column
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Brightness5,
                contentDescription = "Sunrise",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Sunrise",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = solarTimes.sunriseFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEDD5)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.WbTwilight,
                contentDescription = "Sunset",
                tint = Color(0xFFEA580C),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Sunset",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = solarTimes.sunsetFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
  nextPrayer: PrayerItem? = null,
  isCloseToNextPrayer: Boolean = false,
  countdownText: String = "--:--:--",
  cityName: String = "Makkah",
  cityLocation: CityLocation = CityLocation.DEFAULT_CITY,
  onOpenQibla: () -> Unit = {}
) {
  val qiblaBearing = remember(cityLocation) {
    QiblaHelper.calculateQiblaBearing(cityLocation.latitude, cityLocation.longitude)
  }

  // When almost close to next prayer: upcoming is on top and current is below.
  // Otherwise: current prayer is on top and upcoming prayer is moved below and smaller.
  val showUpcomingOnTop = isCloseToNextPrayer || currentPrayer == null

  val topPrayer = if (showUpcomingOnTop) nextPrayer else (currentPrayer ?: nextPrayer)
  val bottomPrayer = if (showUpcomingOnTop) currentPrayer else nextPrayer

  val topTag = if (showUpcomingOnTop) "UPCOMING PRAYER • SOON" else "CURRENT PRAYER"
  val topPrayerName = topPrayer?.type?.displayName ?: "Prayer"
  val topTimeFormatted = topPrayer?.timeFormatted ?: "--:--"
  val topArabicName = topPrayer?.type?.arabicName ?: ""

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            listOf(
              Color(0xFF064E3B),
              Color(0xFF047857),
              Color(0xFF0F5132)
            )
          )
        )
        .padding(20.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (showUpcomingOnTop) Color(0x3DFDE047) else Color(0x33FFFFFF)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (showUpcomingOnTop) {
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
                  color = if (showUpcomingOnTop) Color(0xFFFDE047) else Color(0xFFFEF3C7),
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = topPrayerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              if (topArabicName.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = topArabicName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Normal,
                  color = Color(0xCCFEF3C7)
                )
              }
            }

            Text(
              text = topTimeFormatted.replace(" ", "\u00A0"),
              style = MaterialTheme.typography.titleLarge,
              color = Color(0xFFFDE047),
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false
            )
          }

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
                  color = Color(0xFFFEF3C7)
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
                  text = "Qibla ${qiblaBearing.toInt()}°",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFDE047)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic placement based on closeness to next prayer:
        if (showUpcomingOnTop) {
          // Approaching next prayer: Countdown bar is on top, current prayer is smaller below
          CountdownTimerBar(countdownText = countdownText)

          if (bottomPrayer != null) {
            Spacer(modifier = Modifier.height(10.dp))
            CompactSecondaryPrayerBar(
              label = "Current",
              prayer = bottomPrayer,
              statusText = "Ending soon"
            )
          }
        } else {
          // Normal: Current prayer is on top, upcoming prayer is moved below and smaller
          if (bottomPrayer != null) {
            CompactUpcomingPrayerBar(
              prayer = bottomPrayer,
              countdownText = countdownText
            )
          } else {
            CountdownTimerBar(countdownText = countdownText)
          }
        }
      }
    }
  }
}

@Composable
private fun CompactUpcomingPrayerBar(
  prayer: PrayerItem,
  countdownText: String
) {
  val icon = getPrayerIcon(prayer.type)
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0x28000000),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
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
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Upcoming: ",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xCCFFFFFF)
            )
            Text(
              text = prayer.type.displayName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          Text(
            text = prayer.timeFormatted.replace(" ", "\u00A0"),
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
            text = "in $countdownText",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFDE047),
            letterSpacing = 0.3.sp
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
  statusText: String
) {
  val icon = getPrayerIcon(prayer.type)
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0x28000000),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
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
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xB3FFFFFF)
          )
          Text(
            text = "${prayer.type.displayName} (${prayer.timeFormatted.replace(" ", "\u00A0")})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0x28FFFFFF)
      ) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xEEFFFFFF),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun CountdownTimerBar(countdownText: String) {
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
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Brightness5,
          contentDescription = null,
          tint = Color(0xFFFDE047),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Time Remaining",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xE6FFFFFF)
        )
      }
      Text(
        text = countdownText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFDE047),
        letterSpacing = 0.5.sp
      )
    }
  }
}

@Composable
fun BatteryArchitectureCard(
  syncSource: String,
  lastSynced: String
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
          text = "Battery Optimization Architecture",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      ArchitectureBullet(
        title = "App Not Running in Background",
        desc = "The app closes cleanly. Zero background services or persistent wake locks."
      )
      Spacer(modifier = Modifier.height(4.dp))
      ArchitectureBullet(
        title = "Exact Wakeup via AlarmManager",
        desc = "Android hardware timer wakes the system for <1s only to post the notification, then stops."
      )
      Spacer(modifier = Modifier.height(4.dp))
      ArchitectureBullet(
        title = "Idle-Only Daily Sync",
        desc = "Prayer times saved via WorkManager when phone is idle: $syncSource ($lastSynced)."
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
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
  onToggleNotification: (Boolean) -> Unit
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
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
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
            contentDescription = prayer.type.displayName,
            tint = when {
              isHighlighted -> MaterialTheme.colorScheme.onPrimary
              isCurrent -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = prayer.type.displayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (isHighlighted || isCurrent) FontWeight.Bold else FontWeight.Medium,
              color = textColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (isHighlighted) {
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary
              ) {
                Text(
                  text = "NEXT",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            } else if (isCurrent) {
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondary
              ) {
                Text(
                  text = "CURRENT",
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
            isCurrent -> "Current Prayer Time • ${prayer.type.description}"
            prayer.type.description.isNotEmpty() -> if (isPassed) "Passed • ${prayer.type.description}" else prayer.type.description
            isPassed -> "Passed"
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
              modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = prayer.timeFormatted.replace(" ", "\u00A0"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = textColor,
          maxLines = 1,
          softWrap = false
        )

        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier.size(48.dp),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = CircleShape,
            color = if (prayer.notificationEnabled) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
              if (isSystemInDarkTheme()) Color(0x24FFFFFF) else Color(0x12000000)
            },
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .clickable { onToggleNotification(!prayer.notificationEnabled) }
              .testTag("prayer_notif_toggle_${prayer.type.name.lowercase()}")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = if (prayer.notificationEnabled) Icons.Default.Notifications
                              else Icons.Default.NotificationsOff,
                contentDescription = "Toggle alert for ${prayer.type.displayName}",
                tint = if (prayer.notificationEnabled) MaterialTheme.colorScheme.primary
                       else textColor.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
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
          text = "Select Location",
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
                text = "Use Phone Location (GPS)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Auto-detect exact city via GPS",
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
          placeholder = { Text("Search 270+ world cities...") },
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
            text = "World Cities (${filteredCities.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (searchQuery.isNotBlank()) {
            Text(
              text = "Filtered",
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
                text = "No cities found",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Try searching another city or country",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        Text("Close")
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
