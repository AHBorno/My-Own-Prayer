package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SideMenuDrawerContent(
  uiState: PrayerUiState,
  onSelectLanguage: (String) -> Unit,
  onSelectTheme: (String) -> Unit,
  onRefresh: () -> Unit,
  onOpenCityDialog: () -> Unit,
  onOpenQibla: () -> Unit,
  onCheckUpdates: () -> Unit,
  onTestNotification: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val lang = uiState.appLanguage
  val currentTheme = uiState.appTheme

  ModalDrawerSheet(
    modifier = modifier
      .widthIn(max = 330.dp)
      .fillMaxHeight(),
    drawerContainerColor = MaterialTheme.colorScheme.surface,
    drawerTonalElevation = 3.dp
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      // 1. Decorative Emerald Header
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color(0xFF064E3B),
                  Color(0xFF047857),
                  Color(0xFF0F5132)
                )
              )
            )
            .padding(top = 40.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
        ) {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFFDE047).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Mosque,
                  contentDescription = null,
                  tint = Color(0xFFFDE047),
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = AppLanguageHelper.getString("app_name", lang),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = uiState.currentCity.name + ", " + uiState.currentCity.country,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFD1FAE5)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            val dateStr = remember {
              SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            }
            Text(
              text = dateStr,
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xCCFFFFFF)
            )
          }
        }
      }

      // 2. Language Selection Section
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = AppLanguageHelper.getString("language", lang),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = when (lang) {
              AppLanguageHelper.LANG_BN -> "অ্যাপ এবং নোটিফিকেশনের ভাষা পরিবর্তন হবে"
              AppLanguageHelper.LANG_AR -> "سيتم تغيير لغة التطبيق والإشعارات"
              else -> "Changes app and notification language"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          // 3 Language options (English, Bengali, Arabic)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            LanguageChip(
              name = "English",
              code = AppLanguageHelper.LANG_EN,
              isSelected = lang == AppLanguageHelper.LANG_EN,
              onClick = { onSelectLanguage(AppLanguageHelper.LANG_EN) },
              modifier = Modifier.weight(1f).testTag("lang_en_button")
            )
            LanguageChip(
              name = "বাংলা",
              code = AppLanguageHelper.LANG_BN,
              isSelected = lang == AppLanguageHelper.LANG_BN,
              onClick = { onSelectLanguage(AppLanguageHelper.LANG_BN) },
              modifier = Modifier.weight(1f).testTag("lang_bn_button")
            )
            LanguageChip(
              name = "العربية",
              code = AppLanguageHelper.LANG_AR,
              isSelected = lang == AppLanguageHelper.LANG_AR,
              onClick = { onSelectLanguage(AppLanguageHelper.LANG_AR) },
              modifier = Modifier.weight(1f).testTag("lang_ar_button")
            )
          }
        }
      }

      item {
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          color = DividerDefaults.color.copy(alpha = 0.5f)
        )
      }

      // 3. Theme Toggle Section (Light, Dark, System)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = AppLanguageHelper.getString("theme", lang),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ThemeChip(
              title = AppLanguageHelper.getString("light_theme", lang),
              icon = Icons.Default.LightMode,
              isSelected = currentTheme == "light",
              onClick = { onSelectTheme("light") },
              modifier = Modifier.weight(1f).testTag("theme_light_button")
            )
            ThemeChip(
              title = AppLanguageHelper.getString("dark_theme", lang),
              icon = Icons.Default.DarkMode,
              isSelected = currentTheme == "dark",
              onClick = { onSelectTheme("dark") },
              modifier = Modifier.weight(1f).testTag("theme_dark_button")
            )
            ThemeChip(
              title = AppLanguageHelper.getString("system_theme", lang),
              icon = Icons.Default.SettingsBrightness,
              isSelected = currentTheme == "system",
              onClick = { onSelectTheme("system") },
              modifier = Modifier.weight(1f).testTag("theme_system_button")
            )
          }
        }
      }

      item {
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          color = DividerDefaults.color.copy(alpha = 0.5f)
        )
      }

      // 4. Refresh Prayer Times Action Item
      item {
        val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
        val angle by infiniteTransition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
          ),
          label = "refresh_rotation"
        )

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = !uiState.isSyncing) { onRefresh() }
            .testTag("drawer_refresh_button"),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
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
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (uiState.isSyncing) {
                  AppLanguageHelper.getString("refreshing", lang)
                } else {
                  AppLanguageHelper.getString("refresh_schedule", lang)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = AppLanguageHelper.getString("pull_to_refresh_hint", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }
        }
      }

      // 5. Quick Tools & Navigation
      item {
        DrawerActionItem(
          icon = Icons.Default.Explore,
          title = AppLanguageHelper.getString("qibla_compass", lang),
          subtitle = "Accurate Kaaba direction",
          onClick = onOpenQibla,
          testTag = "drawer_qibla_button"
        )
      }

      item {
        DrawerActionItem(
          icon = Icons.Default.LocationOn,
          title = AppLanguageHelper.getString("select_city", lang),
          subtitle = "${uiState.currentCity.name}, ${uiState.currentCity.country}",
          onClick = onOpenCityDialog,
          testTag = "drawer_city_button"
        )
      }

      item {
        DrawerActionItem(
          icon = Icons.Default.NotificationsActive,
          title = AppLanguageHelper.getString("test_notification", lang),
          subtitle = "Preview sound & quote in $lang",
          onClick = onTestNotification,
          testTag = "drawer_test_notif_button"
        )
      }

      item {
        DrawerActionItem(
          icon = Icons.Default.SystemUpdate,
          title = AppLanguageHelper.getString("check_updates", lang),
          subtitle = "v1.0 Beta",
          onClick = onCheckUpdates,
          testTag = "drawer_check_update_button"
        )
      }

      // 6. Footer
      item {
        Spacer(modifier = Modifier.height(16.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "My Own Prayer • v1.0 Beta",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
          Text(
            text = "Battery-Optimized Offline Schedule",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
        }
      }
    }
  }
}

@Composable
private fun LanguageChip(
  name: String,
  code: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
  val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

  Card(
    modifier = modifier
      .clickable { onClick() }
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(12.dp)
      ),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (isSelected) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
      }
      Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
      )
    }
  }
}

@Composable
private fun ThemeChip(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
  val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

  Card(
    modifier = modifier
      .clickable { onClick() }
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(12.dp)
      ),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = tint,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun DrawerActionItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
