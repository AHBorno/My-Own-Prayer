package com.example.ui

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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RamadanDisplayMode
import com.example.util.AppLanguageHelper

/**
 * Dialog for managing Ramadan fasting timings visibility mode and testing Ramadan alerts.
 */
@Composable
fun RamadanSettingsDialog(
  currentMode: RamadanDisplayMode,
  appLanguage: String,
  onSelectMode: (RamadanDisplayMode) -> Unit,
  onTestAlert: (String) -> Unit,
  onTestPrayerNotification: (prayerName: String, isRamadan: Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFF0F5132).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Brightness3,
          contentDescription = "Ramadan Settings",
          tint = Color(0xFF0F5132),
          modifier = Modifier.size(24.dp)
        )
      }
    },
    title = {
      Text(
        text = AppLanguageHelper.getString("ramadan_mode_drawer_title", appLanguage),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = when (appLanguage) {
            "bn" -> "সেহরি ও ইফতারের সময়সূচি প্রদর্শনের মোড নির্বাচন করুন:"
            "ar" -> "اختر طريقة ظهور مواقيت السحور والإفطار:"
            else -> "Choose how Ramadan Suhoor & Iftar timings appear:"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Option 1: AUTO (Default)
        ModeOptionRow(
          title = when (appLanguage) {
            "bn" -> "স্বয়ংক্রিয় (হিজরি ক্যালেন্ডার)"
            "ar" -> "تلقائي (التقويم الهجري)"
            else -> "Auto (Hijri Calendar)"
          },
          desc = AppLanguageHelper.getString("ramadan_mode_auto_desc", appLanguage),
          selected = currentMode == RamadanDisplayMode.AUTO,
          onClick = { onSelectMode(RamadanDisplayMode.AUTO) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Option 2: ALWAYS SHOW / PREVIEW
        ModeOptionRow(
          title = when (appLanguage) {
            "bn" -> "প্রিভিউ মোড (সবসময় দৃশ্যমান)"
            "ar" -> "وضع المعاينة (إظهار دائم)"
            else -> "Always Show (Preview Mode)"
          },
          desc = AppLanguageHelper.getString("ramadan_mode_preview_desc", appLanguage),
          selected = currentMode == RamadanDisplayMode.ALWAYS_VISIBLE,
          onClick = { onSelectMode(RamadanDisplayMode.ALWAYS_VISIBLE) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Option 3: OFF
        ModeOptionRow(
          title = when (appLanguage) {
            "bn" -> "বন্ধ (লুকানো)"
            "ar" -> "إيقاف (مخفي)"
            else -> "Off (Always Hidden)"
          },
          desc = AppLanguageHelper.getString("ramadan_mode_off_desc", appLanguage),
          selected = currentMode == RamadanDisplayMode.OFF,
          onClick = { onSelectMode(RamadanDisplayMode.OFF) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Test Ramadan Notifications section
        Text(
          text = when (appLanguage) {
            "bn" -> "🔔 নোটিফিকেশন পরীক্ষা করুন:"
            "ar" -> "🔔 تجربة الإشعارات والتنبيهات:"
            else -> "🔔 Test Ramadan Notifications:"
          },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = { onTestAlert("IFTAR_SOON") },
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = when (appLanguage) {
                "bn" -> "ইফতার পূর্ব (১৪ মি.)"
                "ar" -> "قبل الإفطار (١٤ د)"
                else -> "Iftar Soon (14m)"
              },
              fontSize = 11.sp
            )
          }
          OutlinedButton(
            onClick = { onTestAlert("SUHOOR_END_SOON") },
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = when (appLanguage) {
                "bn" -> "সেহরি পূর্ব (১৫ মি.)"
                "ar" -> "قبل السحور (١٥ د)"
                else -> "Suhoor Soon (15m)"
              },
              fontSize = 11.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = { onTestPrayerNotification("Maghrib", true) },
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = when (appLanguage) {
                "bn" -> "ইফতারের সময় (মাগরিব)"
                "ar" -> "وقت الإفطار (المغرب)"
                else -> "Iftar (Maghrib)"
              },
              fontSize = 11.sp
            )
          }
          OutlinedButton(
            onClick = { onTestPrayerNotification("Fajr", true) },
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = when (appLanguage) {
                "bn" -> "সেহরি শেষ (ফজর)"
                "ar" -> "نهاية السحور (الفجر)"
                else -> "Suhoor Ended (Fajr)"
              },
              fontSize = 11.sp
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text(text = when (appLanguage) {
          "bn" -> "সম্পন্ন"
          "ar" -> "تم"
          else -> "Done"
        })
      }
    }
  )
}

@Composable
private fun ModeOptionRow(
  title: String,
  desc: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      RadioButton(
        selected = selected,
        onClick = onClick
      )
      Spacer(modifier = Modifier.width(6.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = desc,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
