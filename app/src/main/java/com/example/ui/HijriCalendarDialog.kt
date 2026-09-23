package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.HijriCalendarDay
import com.example.data.model.HijriMonthData
import com.example.util.AppLanguageHelper
import com.example.util.HijriDateHelper
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@Composable
fun HijriCalendarDialog(
  appLanguage: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var adjustmentDays by remember { mutableIntStateOf(HijriDateHelper.getAdjustment(context)) }

  // Initial today Hijri date
  val initialToday = remember(adjustmentDays) {
    HijriDateHelper.getHijriDate(Date(), adjustmentDays)
  }

  var currentHijriYear by remember { mutableIntStateOf(initialToday.year) }
  var currentHijriMonth by remember { mutableIntStateOf(initialToday.month) }
  var selectedDay by remember { mutableStateOf<HijriCalendarDay?>(null) }
  var monthData by remember { mutableStateOf<HijriMonthData?>(null) }
  var isLoading by remember { mutableStateOf(true) }

  fun loadMonth(forceRefresh: Boolean = false) {
    isLoading = true
    coroutineScope.launch {
      val data = HijriDateHelper.getHijriMonthCalendar(
        targetHijriYear = currentHijriYear,
        targetHijriMonth = currentHijriMonth,
        adjustmentDays = adjustmentDays,
        forceRefresh = forceRefresh
      )
      monthData = data
      // Select today if in current month, otherwise first day
      val todayMatch = data.days.firstOrNull { it.isToday && it.isCurrentMonth }
      selectedDay = todayMatch ?: data.days.firstOrNull { it.isCurrentMonth }
      isLoading = false
    }
  }

  LaunchedEffect(currentHijriYear, currentHijriMonth, adjustmentDays) {
    loadMonth(forceRefresh = false)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .heightIn(max = 760.dp)
        .padding(vertical = 16.dp)
        .testTag("hijri_calendar_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // --- Top Bar ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = AppLanguageHelper.getString("hijri_calendar_title", appLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = AppLanguageHelper.getString("hijri_calendar_subtitle", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Month Header with Navigation & Today Button ---
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Previous Month Button
            IconButton(
              onClick = {
                if (currentHijriMonth == 1) {
                  currentHijriMonth = 12
                  currentHijriYear -= 1
                } else {
                  currentHijriMonth -= 1
                }
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }

            // Month & Year Label + Gregorian Span
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              val monthName = when (appLanguage) {
                AppLanguageHelper.LANG_BN -> monthData?.hijriMonthNameBn ?: ""
                AppLanguageHelper.LANG_AR -> monthData?.hijriMonthNameAr ?: ""
                else -> monthData?.hijriMonthNameEn ?: ""
              }
              val yearStr = AppLanguageHelper.localizeNumbers(currentHijriYear.toString(), appLanguage)
              val suffix = AppLanguageHelper.getString("day_suffix", appLanguage)

              Text(
                text = "$monthName $yearStr $suffix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )

              val gregSpan = when (appLanguage) {
                AppLanguageHelper.LANG_BN -> monthData?.gregorianSpanBn ?: ""
                AppLanguageHelper.LANG_AR -> monthData?.gregorianSpanAr ?: ""
                else -> monthData?.gregorianSpanEn ?: ""
              }
              if (gregSpan.isNotEmpty()) {
                Text(
                  text = gregSpan,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }

            // Next Month Button
            IconButton(
              onClick = {
                if (currentHijriMonth == 12) {
                  currentHijriMonth = 1
                  currentHijriYear += 1
                } else {
                  currentHijriMonth += 1
                }
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Web Sync Status & Today Quick Button ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Web sync status pill
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (monthData?.isFromWeb == true) Icons.Default.CloudDone else Icons.Default.DarkMode,
                contentDescription = null,
                tint = if (monthData?.isFromWeb == true) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (monthData?.isFromWeb == true) {
                  AppLanguageHelper.getString("synced_live", appLanguage)
                } else {
                  "Umm al-Qura"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                onClick = { loadMonth(forceRefresh = true) },
                modifier = Modifier.size(16.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh from Web",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(12.dp)
                )
              }
            }
          }

          // Jump to Today button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.clickable {
              val todayHijri = HijriDateHelper.getHijriDate(Date(), adjustmentDays)
              currentHijriYear = todayHijri.year
              currentHijriMonth = todayHijri.month
            }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Today,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = AppLanguageHelper.getString("today_button", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Moon Sighting Adjustment Bar (-2, -1, 0, +1, +2) ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = AppLanguageHelper.getString("moon_adjustment", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(-2, -1, 0, 1, 2).forEach { adj ->
              val isSelected = (adj == adjustmentDays)
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable {
                  adjustmentDays = adj
                  HijriDateHelper.setAdjustment(context, adj)
                }
              ) {
                Text(
                  text = if (adj > 0) "+$adj" else "$adj",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Weekday Headers ---
        val weekdayLabels = HijriDateHelper.getWeekdayLabels(appLanguage)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          weekdayLabels.forEachIndexed { index, label ->
            val isFriday = (index == 5) // Friday
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (isFriday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- Calendar Day Grid (7 Columns) ---
        if (isLoading) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
          }
        } else {
          val days = monthData?.days ?: emptyList()
          LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 280.dp),
            userScrollEnabled = false,
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            items(days) { day ->
              val isSelected = (selectedDay?.hijriDay == day.hijriDay && selectedDay?.isCurrentMonth == day.isCurrentMonth)
              CalendarDayCell(
                day = day,
                isSelected = isSelected,
                appLanguage = appLanguage,
                onClick = {
                  if (day.isCurrentMonth) {
                    selectedDay = day
                  }
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Selected Day Detailed Information Card ---
        selectedDay?.let { day ->
          SelectedDayDetailCard(day = day, appLanguage = appLanguage)
        }
      }
    }
  }
}

@Composable
private fun CalendarDayCell(
  day: HijriCalendarDay,
  isSelected: Boolean,
  appLanguage: String,
  onClick: () -> Unit
) {
  val hasSpecialEvent = day.events.isNotEmpty()
  val isWhiteDay = day.isWhiteDay && day.isCurrentMonth

  val containerColor = when {
    isSelected -> MaterialTheme.colorScheme.primary
    day.isToday -> MaterialTheme.colorScheme.primaryContainer
    isWhiteDay -> Color(0xFFFEF3C7) // Light amber for Ayyam al-Beed
    hasSpecialEvent -> Color(0xFFD1FAE5) // Light emerald for Islamic event
    else -> Color.Transparent
  }

  val textColor = when {
    isSelected -> MaterialTheme.colorScheme.onPrimary
    !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    day.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
    isWhiteDay -> Color(0xFF92400E)
    hasSpecialEvent -> Color(0xFF065F46)
    day.isFriday -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
  }

  val gregColor = when {
    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    day.isToday -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
  }

  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(10.dp))
      .background(containerColor)
      .then(
        if (day.isToday && !isSelected) {
          Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
        } else {
          Modifier
        }
      )
      .clickable(enabled = day.isCurrentMonth) { onClick() }
      .padding(2.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Hijri Day
      Text(
        text = AppLanguageHelper.localizeNumbers(day.hijriDay.toString(), appLanguage),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (day.isToday || isSelected || isWhiteDay || hasSpecialEvent) FontWeight.Bold else FontWeight.Medium,
        color = textColor,
        fontSize = 13.sp,
        maxLines = 1
      )

      // Gregorian Day
      Text(
        text = AppLanguageHelper.localizeNumbers(day.gregorianDay.toString(), appLanguage),
        style = MaterialTheme.typography.labelSmall,
        color = gregColor,
        fontSize = 9.sp,
        maxLines = 1
      )

      // Indicator dot for Events or White Day
      if (day.isCurrentMonth && (hasSpecialEvent || isWhiteDay)) {
        Box(
          modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White else if (hasSpecialEvent) Color(0xFF059669) else Color(0xFFD97706))
        )
      }
    }
  }
}

@Composable
private fun SelectedDayDetailCard(
  day: HijriCalendarDay,
  appLanguage: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Hijri & Gregorian Date Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          val cal = Calendar.getInstance().apply {
            set(day.gregorianYear, day.gregorianMonth - 1, day.gregorianDay)
          }
          val hijriDate = HijriDateHelper.getHijriDate(cal.time)
          Text(
            text = HijriDateHelper.formatHijriDate(hijriDate, appLanguage),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = HijriDateHelper.formatGregorianDate(cal.time, appLanguage),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (day.isToday) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
          ) {
            Text(
              text = AppLanguageHelper.getString("today_button", appLanguage),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Events and Sunnah Fasting Information
      if (day.events.isNotEmpty() || day.isWhiteDay || day.isMondayOrThursday || day.isFriday) {
        Spacer(modifier = Modifier.height(8.dp))

        // Islamic Occasions
        day.events.forEach { event ->
          val eventTitle = when (appLanguage) {
            AppLanguageHelper.LANG_BN -> event.titleBn
            AppLanguageHelper.LANG_AR -> event.titleAr
            else -> event.titleEn
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = Color(0xFF059669),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = eventTitle,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF065F46)
            )
          }
        }

        // White Days (Ayyam al-Beed) recommendation
        if (day.isWhiteDay) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.DarkMode,
              contentDescription = null,
              tint = Color(0xFFD97706),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = AppLanguageHelper.getString("white_days", appLanguage),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF92400E)
            )
          }
        }

        // Sunnah Monday / Thursday Fasting
        if (day.isMondayOrThursday && !day.isWhiteDay && day.events.isEmpty()) {
          Text(
            text = when (appLanguage) {
              AppLanguageHelper.LANG_BN -> "• সোমবার ও বৃহস্পতিবার নফল রোজা রাখা সুন্নাত"
              AppLanguageHelper.LANG_AR -> "• يُستحب صيام الإثنين والخميس سنة"
              else -> "• Sunnah fasting recommended on Monday & Thursday"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    }
  }
}
