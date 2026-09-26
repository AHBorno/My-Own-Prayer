package com.example.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.AppLanguageHelper
import com.example.util.TasbeehPreferences

@Composable
fun TasbeehDialog(
  appLanguage: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  var count by remember { mutableStateOf(TasbeehPreferences.getCount(context)) }
  var totalCount by remember { mutableStateOf(TasbeehPreferences.getTotalCount(context)) }
  var roundCount by remember { mutableStateOf(TasbeehPreferences.getRoundCount(context)) }
  var targetCount by remember { mutableStateOf(TasbeehPreferences.getTargetCount(context)) }
  var currentDhikr by remember { mutableStateOf(TasbeehPreferences.getCurrentDhikr(context)) }
  var vibrationEnabled by remember { mutableStateOf(TasbeehPreferences.isVibrationEnabled(context)) }
  var soundEnabled by remember { mutableStateOf(TasbeehPreferences.isSoundEnabled(context)) }

  var showDhikrPicker by remember { mutableStateOf(false) }
  var showResetConfirmDialog by remember { mutableStateOf(false) }

  val playClickSound = remember {
    {
      if (soundEnabled) {
        try {
          val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30)
          toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (_: Exception) {}
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .heightIn(max = 760.dp)
        .padding(horizontal = 8.dp, vertical = 16.dp)
        .testTag("tasbeeh_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // 1. Top Header Row (Title, Sound, Vibration, Close)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF047857).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_tasbeeh),
                contentDescription = null,
                tint = Color(0xFF047857),
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = AppLanguageHelper.getString("tasbeeh_counter", appLanguage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = AppLanguageHelper.getString("tasbeeh_counter_sub", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            // Vibration Toggle
            IconButton(
              onClick = {
                val newState = !vibrationEnabled
                vibrationEnabled = newState
                TasbeehPreferences.setVibrationEnabled(context, newState)
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = AppLanguageHelper.getString("vibration", appLanguage),
                tint = if (vibrationEnabled) Color(0xFF047857) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
              )
            }

            // Sound Toggle
            IconButton(
              onClick = {
                val newState = !soundEnabled
                soundEnabled = newState
                TasbeehPreferences.setSoundEnabled(context, newState)
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = AppLanguageHelper.getString("sound", appLanguage),
                tint = if (soundEnabled) Color(0xFF047857) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
              )
            }

            // Close Dialog
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = AppLanguageHelper.getString("close", appLanguage),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Dhikr Selection Card
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = Color(0xFF064E3B).copy(alpha = 0.08f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.dp,
              color = Color(0xFF047857).copy(alpha = 0.3f),
              shape = RoundedCornerShape(18.dp)
            )
            .clickable { showDhikrPicker = true }
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF047857).copy(alpha = 0.15f)
              ) {
                Text(
                  text = AppLanguageHelper.getString("select_dhikr", appLanguage),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF047857),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }

              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Switch Dhikr",
                tint = Color(0xFF047857),
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = currentDhikr.arabic,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF047857),
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            val meaningText = when (appLanguage) {
              "bn" -> "${currentDhikr.transliteration} • ${currentDhikr.meaningBn}"
              "ar" -> currentDhikr.meaningAr
              else -> "${currentDhikr.transliteration} • ${currentDhikr.meaningEn}"
            }

            Text(
              text = meaningText,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Dhikr selection Dropdown Menu
        DropdownMenu(
          expanded = showDhikrPicker,
          onDismissRequest = { showDhikrPicker = false }
        ) {
          TasbeehPreferences.DHIKR_PRESETS.forEachIndexed { index, item ->
            DropdownMenuItem(
              text = {
                Column {
                  Text(
                    text = item.arabic,
                    fontWeight = FontWeight.Bold,
                    color = if (item.id == currentDhikr.id) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = when (appLanguage) {
                      "bn" -> "${item.transliteration} • ${item.meaningBn}"
                      "ar" -> item.meaningAr
                      else -> "${item.transliteration} • ${item.meaningEn}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              },
              onClick = {
                TasbeehPreferences.setDhikrIndex(context, index)
                currentDhikr = item
                targetCount = item.defaultTarget
                showDhikrPicker = false
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Target Chips Selector (33, 99, 100, 1000, Free/∞)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val targets = listOf(33, 99, 100, 0)
          targets.forEach { targetVal ->
            val isSelected = targetCount == targetVal
            val label = if (targetVal == 0) "∞ Free" else "$targetVal"
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) Color(0xFF047857) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  targetCount = targetVal
                  TasbeehPreferences.setTargetCount(context, targetVal)
                }
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Large Interactive Tasbeeh Circular Tap Button
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
          targetValue = if (isPressed) 0.93f else 1f,
          animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
          label = "tasbeeh_scale"
        )

        val progress = if (targetCount > 0) {
          (count.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
        } else {
          1f
        }

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(230.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  Color(0xFF047857).copy(alpha = 0.15f),
                  Color(0xFF047857).copy(alpha = 0.05f),
                  MaterialTheme.colorScheme.surface
                )
              )
            )
            .clickable(
              interactionSource = interactionSource,
              indication = null
            ) {
              val result = TasbeehPreferences.increment(context)
              count = result.newCount
              totalCount = result.newTotal
              roundCount = result.newRounds
              playClickSound()
            }
            .testTag("tasbeeh_tap_button")
        ) {
          // Circular Progress Track
          CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(220.dp),
            strokeWidth = 10.dp,
            color = Color(0xFF047857),
            trackColor = Color(0xFF047857).copy(alpha = 0.12f),
          )

          // Inner Decorative Bead Ring
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(190.dp)
              .clip(CircleShape)
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color(0xFF047857).copy(alpha = 0.22f),
                    Color(0xFF064E3B).copy(alpha = 0.35f)
                  )
                )
              )
              .border(
                width = 2.dp,
                color = Color(0xFF10B981).copy(alpha = 0.5f),
                shape = CircleShape
              )
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                  fontSize = 54.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (targetCount > 0) "/ $targetCount" else "∞",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF047857)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = AppLanguageHelper.getString("tap_to_count", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Stat Badges (Rounds & Total Dhikr & Manual +/- Controls)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Decrement -1 Button (for accidental tap)
          OutlinedIconButton(
            onClick = {
              val newC = TasbeehPreferences.decrement(context)
              count = newC
              totalCount = TasbeehPreferences.getTotalCount(context)
            },
            enabled = count > 0,
            modifier = Modifier
              .size(44.dp)
              .testTag("tasbeeh_decrement_button")
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "-1",
              tint = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
          }

          // Rounds Chip
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${AppLanguageHelper.getString("rounds", appLanguage)}: ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$roundCount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF047857)
              )
            }
          }

          // Total Count Chip
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${AppLanguageHelper.getString("total_dhikr", appLanguage)}: ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "$totalCount",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF047857)
              )
            }
          }

          // Reset Button
          OutlinedIconButton(
            onClick = { showResetConfirmDialog = true },
            modifier = Modifier
              .size(44.dp)
              .testTag("tasbeeh_reset_button")
          ) {
            Icon(
              imageVector = Icons.Default.RestartAlt,
              contentDescription = AppLanguageHelper.getString("reset_counter", appLanguage),
              tint = MaterialTheme.colorScheme.error
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. "Add Widget to Home Screen" Button
        Button(
          onClick = {
            val pinned = TasbeehPreferences.requestPinWidget(context)
            if (pinned) {
              Toast.makeText(
                context,
                AppLanguageHelper.getString("widget_added_success", appLanguage),
                Toast.LENGTH_LONG
              ).show()
            } else {
              Toast.makeText(
                context,
                AppLanguageHelper.getString("widget_manual_guide", appLanguage),
                Toast.LENGTH_LONG
              ).show()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("add_widget_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF047857)
          )
        ) {
          Icon(
            imageVector = Icons.Default.Widgets,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = AppLanguageHelper.getString("add_widget_to_home", appLanguage),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }

  // Reset Confirmation Alert Dialog
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = {
        Text(
          text = AppLanguageHelper.getString("reset_counter", appLanguage),
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = when (appLanguage) {
            "bn" -> "আপনি কি চলতি তাসবিহ কাউন্ট রিসেট করতে চান?"
            "ar" -> "هل ترغب في إعادة ضبط العداد الحالي؟"
            else -> "Do you want to reset the current count or reset everything including rounds?"
          }
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            TasbeehPreferences.reset(context, resetRounds = true, resetTotal = false)
            count = 0
            roundCount = 0
            showResetConfirmDialog = false
          }
        ) {
          Text(
            text = AppLanguageHelper.getString("reset_all", appLanguage),
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            TasbeehPreferences.reset(context, resetRounds = false, resetTotal = false)
            count = 0
            showResetConfirmDialog = false
          }
        ) {
          Text(
            text = AppLanguageHelper.getString("reset_counter", appLanguage)
          )
        }
      }
    )
  }
}
