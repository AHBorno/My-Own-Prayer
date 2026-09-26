package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RamadanStage
import com.example.data.model.RamadanTiming
import com.example.util.AppLanguageHelper

@Composable
fun RamadanIftarSuhoorCard(
  ramadanTiming: RamadanTiming,
  appLanguage: String = "en",
  onToggleSuhoorNotification: (Boolean) -> Unit,
  onToggleIftarNotification: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    shape = RoundedCornerShape(20.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("ramadan_iftar_suhoor_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Ramadan title, crescent icon, and day badge
      if (ramadanTiming.isDayBeforeRamadan) {
        // Day Before Ramadan: Full Title Row + Dedicated Announcement Banner below
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Nightlight,
              contentDescription = "Ramadan Moon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLanguageHelper.getString("ramadan_iftar_suhoor_title", appLanguage),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = AppLanguageHelper.getString("ramadan_today_schedule", appLanguage),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day-Before-Ramadan Full Notice Banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Nightlight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = AppLanguageHelper.getString("day_before_ramadan_notice", appLanguage),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )
          }
        }
      } else {
        // Active Ramadan Day: Compact Row with Day Pill on the right
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Nightlight,
                contentDescription = "Ramadan Moon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
              Text(
                text = AppLanguageHelper.getString("ramadan_iftar_suhoor_title", appLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = AppLanguageHelper.getString("ramadan_today_schedule", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Ramadan Day Pill
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
          ) {
            Text(
              text = "${AppLanguageHelper.localizeNumbers(ramadanTiming.hijriDay.toString(), appLanguage)} ${AppLanguageHelper.getString("ramadan_day_prefix", appLanguage)}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Two cards: Suhoor Ends and Iftar Time
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val suhoorSoonLabel = when (appLanguage) {
          "bn" -> "~১৫ মি."
          "ar" -> "~١٥ د"
          else -> "~15m"
        }
        val iftarSoonLabel = when (appLanguage) {
          "bn" -> "~১৪ মি."
          "ar" -> "~١٤ د"
          else -> "~14m"
        }

        // SUHOOR CARD
        TimingSubCard(
          title = AppLanguageHelper.getString("suhoor_end_label", appLanguage),
          subtitle = AppLanguageHelper.getString("suhoor_desc", appLanguage),
          time = AppLanguageHelper.localizeTime(ramadanTiming.suhoorEndTime, appLanguage),
          soonTime = AppLanguageHelper.localizeTime(ramadanTiming.suhoorSoonTime, appLanguage),
          soonLabel = suhoorSoonLabel,
          countdown = if (ramadanTiming.suhoorCountdown != "--:--:--") {
            "${AppLanguageHelper.getString("suhoor_countdown_label", appLanguage)} ${AppLanguageHelper.localizeCountdown(ramadanTiming.suhoorCountdown, appLanguage)}"
          } else {
            AppLanguageHelper.getString("suhoor_ended_badge", appLanguage)
          },
          isActive = ramadanTiming.activeStage == RamadanStage.SUHOOR_TIME,
          notificationEnabled = ramadanTiming.suhoorNotificationEnabled,
          onToggleNotification = { onToggleSuhoorNotification(!ramadanTiming.suhoorNotificationEnabled) },
          icon = Icons.Outlined.Bedtime,
          modifier = Modifier.weight(1f),
          testTag = "suhoor_card"
        )

        // IFTAR CARD
        TimingSubCard(
          title = AppLanguageHelper.getString("iftar_label", appLanguage),
          subtitle = AppLanguageHelper.getString("iftar_desc", appLanguage),
          time = AppLanguageHelper.localizeTime(ramadanTiming.iftarTime, appLanguage),
          soonTime = AppLanguageHelper.localizeTime(ramadanTiming.iftarSoonTime, appLanguage),
          soonLabel = iftarSoonLabel,
          countdown = if (ramadanTiming.iftarCountdown != "--:--:--") {
            "${AppLanguageHelper.getString("iftar_countdown_label", appLanguage)} ${AppLanguageHelper.localizeCountdown(ramadanTiming.iftarCountdown, appLanguage)}"
          } else {
            if (ramadanTiming.activeStage == RamadanStage.IFTAR_TIME) {
              AppLanguageHelper.getString("iftar_time_active", appLanguage)
            } else {
              AppLanguageHelper.getString("iftar_completed_badge", appLanguage)
            }
          },
          isActive = ramadanTiming.activeStage == RamadanStage.FASTING_DAY || ramadanTiming.activeStage == RamadanStage.IFTAR_TIME,
          notificationEnabled = ramadanTiming.iftarNotificationEnabled,
          onToggleNotification = { onToggleIftarNotification(!ramadanTiming.iftarNotificationEnabled) },
          icon = Icons.Default.WbSunny,
          modifier = Modifier.weight(1f),
          testTag = "iftar_card"
        )
      }
    }
  }
}

@Composable
private fun TimingSubCard(
  title: String,
  subtitle: String,
  time: String,
  soonTime: String,
  soonLabel: String = "~15m",
  countdown: String,
  isActive: Boolean,
  notificationEnabled: Boolean,
  onToggleNotification: () -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
  testTag: String = ""
) {
  val containerColor = if (isActive) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  val borderColor = if (isActive) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.testTag(testTag)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(
          onClick = onToggleNotification,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = if (notificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
            contentDescription = if (notificationEnabled) "Notifications On (2 Alerts)" else "Notifications Off",
            tint = if (notificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Main Timing
      Text(
        text = time,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        fontSize = 21.sp
      )

      // Warning pill (soon time)
      Text(
        text = "🔔 $soonLabel: $soonTime",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Countdown / status badge
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) {
          MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
      ) {
        Text(
          text = countdown,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
      }
    }
  }
}
