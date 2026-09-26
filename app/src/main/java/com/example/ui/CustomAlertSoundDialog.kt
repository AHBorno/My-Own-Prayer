package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.util.AlertSoundManager
import com.example.util.AppLanguageHelper

@Composable
fun CustomAlertSoundDialog(
  appLanguage: String,
  onDismiss: () -> Unit,
  onSoundUpdated: () -> Unit = {}
) {
  val context = LocalContext.current
  var currentCustomName by remember { mutableStateOf(AlertSoundManager.getCustomSoundName(context)) }
  var isCustomEnabled by remember { mutableStateOf(AlertSoundManager.isCustomSoundEnabled(context)) }
  var isPlaying by remember { mutableStateOf(false) }
  var isImporting by remember { mutableStateOf(false) }

  // Clean up any playing audio when dialog is dismissed
  DisposableEffect(Unit) {
    onDispose {
      AlertSoundManager.stopSound()
    }
  }

  // Audio file picker launcher (audio/*)
  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      isImporting = true
      val result = AlertSoundManager.setCustomSoundFromUri(context, uri)
      isImporting = false
      if (result.isSuccess) {
        val fileName = result.getOrNull() ?: "custom_sound.mp3"
        currentCustomName = fileName
        isCustomEnabled = true
        onSoundUpdated()
        val successMsg = AppLanguageHelper.getString("sound_set_success", appLanguage)
        Toast.makeText(context, "$successMsg\n$fileName", Toast.LENGTH_SHORT).show()
      } else {
        val errorMsg = AppLanguageHelper.getString("invalid_audio_file", appLanguage)
        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp)
        .testTag("custom_alert_sound_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        // Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Audiotrack,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLanguageHelper.getString("custom_alert_sound", appLanguage),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = AppLanguageHelper.getString("custom_alert_sound_sub", appLanguage),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Current Sound Status Card
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isCustomEnabled) {
              Color(0xFF047857).copy(alpha = 0.12f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.dp,
              color = if (isCustomEnabled) Color(0xFF047857).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
              shape = RoundedCornerShape(16.dp)
            )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = if (isCustomEnabled) Icons.Default.MusicNote else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (isCustomEnabled) Color(0xFF047857) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (isCustomEnabled) {
                    currentCustomName ?: "custom_alert_sound.mp3"
                  } else {
                    AppLanguageHelper.getString("system_default_sound", appLanguage)
                  },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = if (isCustomEnabled) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (isCustomEnabled) {
                    when (appLanguage) {
                      "bn" -> "কাস্টম MP3 সক্রিয় রয়েছে"
                      "ar" -> "الصوت المخصص مفعل"
                      else -> "Custom MP3 is active"
                    }
                  } else {
                    when (appLanguage) {
                      "bn" -> "ডিফল্ট সিস্টেম রিংটোন"
                      "ar" -> "نغمة النظام الافتراضية"
                      else -> "System default notification ringtone"
                    }
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (isCustomEnabled) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFF047857).copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "MP3",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row (Select MP3 & Preview)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Select MP3 Button
          Button(
            onClick = {
              AlertSoundManager.stopSound()
              isPlaying = false
              audioPickerLauncher.launch("audio/*")
            },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("select_mp3_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isImporting) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Spacer(modifier = Modifier.width(8.dp))
            } else {
              Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = AppLanguageHelper.getString("select_mp3_sound", appLanguage),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Preview / Play Sound Button
          FilledTonalButton(
            onClick = {
              if (isPlaying) {
                AlertSoundManager.stopSound()
                isPlaying = false
              } else {
                isPlaying = true
                AlertSoundManager.playAlertSound(context) {
                  isPlaying = false
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("preview_sound_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isPlaying) {
                AppLanguageHelper.getString("stop_sound", appLanguage)
              } else {
                AppLanguageHelper.getString("preview_sound", appLanguage)
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Reset to default button (if custom sound is enabled)
          AnimatedVisibility(
            visible = isCustomEnabled,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            OutlinedButton(
              onClick = {
                AlertSoundManager.resetToDefaultSound(context)
                isCustomEnabled = false
                currentCustomName = null
                isPlaying = false
                onSoundUpdated()
                val resetMsg = AppLanguageHelper.getString("sound_reset_success", appLanguage)
                Toast.makeText(context, resetMsg, Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("reset_sound_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = AppLanguageHelper.getString("reset_to_default_sound", appLanguage),
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Explanatory Note
        Text(
          text = when (appLanguage) {
            "bn" -> "পরামর্শ: আপনার ডিভাইসে সংরক্ষিত যে কোনো আযান বা অ্যালার্টের MP3 ফাইল বেছে নিতে পারেন। ফাইলটি অফলাইনে স্থায়ীভাবে সংরক্ষিত থাকবে।"
            "ar" -> "ملاحظة: يمكنك اختيار أي ملف صوتي MP3 مثل الأذان من جهازك وسيعمل تلقائياً دون اتصال بالإنترنت."
            else -> "Tip: You can pick any Azan or alert MP3 stored on your device. The file is saved offline for all scheduled prayer reminders."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Close button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = {
              AlertSoundManager.stopSound()
              onDismiss()
            },
            modifier = Modifier.testTag("close_sound_dialog_button")
          ) {
            Text(
              text = AppLanguageHelper.getString("close", appLanguage),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
