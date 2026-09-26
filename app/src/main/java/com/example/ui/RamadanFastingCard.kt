package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RamadanTimingInfo
import com.example.util.AppLanguageHelper

/**
 * Ramadan Iftar and Suhoor timing section.
 * Appears below the obligatory prayers section starting a day before Ramadan,
 * remains active during Ramadan, and disappears on the last day of Ramadan.
 */
@Composable
fun RamadanFastingCard(
  ramadanTiming: RamadanTimingInfo,
  appLanguage: String = "en",
  onToggleIftarSoon: () -> Unit,
  onToggleSuhoorSoon: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showDuas by remember { mutableStateOf(false) }

  val suhoorTimeFormatted = AppLanguageHelper.localizeTime(ramadanTiming.suhoorEndTimeFormatted, appLanguage)
  val iftarTimeFormatted = AppLanguageHelper.localizeTime(ramadanTiming.iftarTimeFormatted, appLanguage)
  val countdownLocalized = AppLanguageHelper.localizeCountdown(ramadanTiming.countdownValue, appLanguage)
  val countdownPrefix = AppLanguageHelper.getString(ramadanTiming.countdownLabel, appLanguage)

  // Islamic golden / emerald decorative palette
  val ramadanPrimary = Color(0xFF104C38) // Deep Islamic green
  val ramadanAccent = Color(0xFFD4AF37)  // Islamic Gold
  val ramadanCardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(20.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Top Header Row with Crescent Moon Icon, Title and Season Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF0F5132).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Brightness3,
              contentDescription = "Ramadan Crescent",
              tint = Color(0xFF0F5132),
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = AppLanguageHelper.getString("ramadan_fasting_title", appLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = AppLanguageHelper.getString("ramadan_fasting_subtitle", appLanguage),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge: Tomorrow 1st Ramadan or Day X
        val badgeText = when {
          ramadanTiming.isDayBeforeRamadan -> AppLanguageHelper.getString("tomorrow_1st_ramadan", appLanguage)
          ramadanTiming.isRamadanActive -> {
            val dayLocalized = AppLanguageHelper.localizeNumbers(ramadanTiming.hijriDay.toString(), appLanguage)
            "${AppLanguageHelper.getString("ramadan_day_prefix", appLanguage)}$dayLocalized"
          }
          ramadanTiming.isPreviewMode -> "Preview Mode"
          else -> "Ramadan"
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (ramadanTiming.isDayBeforeRamadan) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
          modifier = Modifier.padding(start = 4.dp)
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (ramadanTiming.isDayBeforeRamadan) Color(0xFF92400E) else Color(0xFF065F46),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Live Fasting Countdown / Status Pill
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
          ramadanTiming.isFastingInProgress -> Color(0xFF065F46) // Green for fasting
          else -> MaterialTheme.colorScheme.secondaryContainer
        },
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
                .size(8.dp)
                .clip(CircleShape)
                .background(if (ramadanTiming.isFastingInProgress) Color(0xFF34D399) else ramadanAccent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val statusLabel = when {
              ramadanTiming.isFastingInProgress -> AppLanguageHelper.getString("fasting_in_progress", appLanguage)
              else -> AppLanguageHelper.getString("suhoor_time_active", appLanguage)
            }
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = if (ramadanTiming.isFastingInProgress) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = "$countdownPrefix$countdownLocalized",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (ramadanTiming.isFastingInProgress) Color(0xFFFDE68A) else MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Timing Tiles: Suhoor Ends (Left) & Iftar Time (Right)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // SUHOOR ENDS TILE
        FastTimingTile(
          title = AppLanguageHelper.getString("suhoor_ends_fajr", appLanguage),
          subtitle = AppLanguageHelper.getString("suhoor_desc", appLanguage),
          time = suhoorTimeFormatted,
          icon = Icons.Default.WbTwilight,
          iconBgColor = Color(0xFFE0F2FE),
          iconTint = Color(0xFF0369A1),
          alertActive = ramadanTiming.notificationSuhoorSoon,
          onToggleAlert = onToggleSuhoorSoon,
          alertLabel = AppLanguageHelper.getString("suhoor_soon_chip", appLanguage),
          modifier = Modifier.weight(1f)
        )

        // IFTAR TIME TILE
        FastTimingTile(
          title = AppLanguageHelper.getString("iftar_time_maghrib", appLanguage),
          subtitle = AppLanguageHelper.getString("iftar_desc", appLanguage),
          time = iftarTimeFormatted,
          icon = Icons.Default.Brightness3,
          iconBgColor = Color(0xFFFEF3C7),
          iconTint = Color(0xFFB45309),
          alertActive = ramadanTiming.notificationIftarSoon,
          onToggleAlert = onToggleIftarSoon,
          alertLabel = AppLanguageHelper.getString("iftar_soon_chip", appLanguage),
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Expandable Islamic Duas (Iftar Dua & Fasting Niyyah)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showDuas = !showDuas }
      ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = AppLanguageHelper.getString("dua_iftar_header", appLanguage),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Icon(
              imageVector = if (showDuas) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          AnimatedVisibility(
            visible = showDuas,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
          ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
              // Iftar Dua
              Text(
                text = AppLanguageHelper.getString("dua_iftar_arabic", appLanguage),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = AppLanguageHelper.getString("dua_iftar_trans", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Fasting Niyyah
              Text(
                text = AppLanguageHelper.getString("niyyah_fasting_header", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = AppLanguageHelper.getString("niyyah_fasting_arabic", appLanguage),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = AppLanguageHelper.getString("niyyah_fasting_trans", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FastTimingTile(
  title: String,
  subtitle: String,
  time: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconBgColor: Color,
  iconTint: Color,
  alertActive: Boolean,
  onToggleAlert: () -> Unit,
  alertLabel: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(iconBgColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(17.dp)
          )
        }

        IconButton(
          onClick = onToggleAlert,
          modifier = Modifier.size(30.dp)
        ) {
          Icon(
            imageVector = if (alertActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
            contentDescription = alertLabel,
            tint = if (alertActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = time,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Mini Pill indicating 15m notification status
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (alertActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onToggleAlert() }
      ) {
        Text(
          text = if (alertActive) "✓ $alertLabel" else alertLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = if (alertActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (alertActive) FontWeight.Bold else FontWeight.Normal,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}
