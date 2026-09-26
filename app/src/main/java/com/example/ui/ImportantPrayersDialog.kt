package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ImportantPrayerItem
import com.example.data.model.ImportantPrayersRepository
import com.example.data.model.PrayerStep
import com.example.util.AppLanguageHelper

@Composable
fun ImportantPrayersDialog(
  initialLanguage: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedLang by remember { mutableStateOf(initialLanguage) }
  val prayers = remember { ImportantPrayersRepository.prayers }
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val currentPrayer = prayers.getOrElse(selectedTabIndex) { prayers[0] }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false
    )
  ) {
    Surface(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface),
      color = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // Top Emerald Header
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
            .padding(top = 36.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Column {
                  Text(
                    text = when (selectedLang) {
                      AppLanguageHelper.LANG_BN -> "কিছু গুরুত্বপূর্ণ সালাত"
                      AppLanguageHelper.LANG_AR -> "صلوات مهمة ومأثورة"
                      else -> "Important Special Prayers"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = when (selectedLang) {
                      AppLanguageHelper.LANG_BN -> "ইস্তিখারা, সালাতুল হাজাত ও সালাতুত তাসবীহ"
                      AppLanguageHelper.LANG_AR -> "الاستخارة، صلاة الحاجة، صلاة التسابيح"
                      else -> "Istikhara, Salatul Hajat & Salatut Tasbih"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD1FAE5)
                  )
                }
              }

              IconButton(
                onClick = onDismiss,
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.2f))
                  .testTag("important_prayers_close_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // In-dialog Quick Language Switcher Bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = when (selectedLang) {
                  AppLanguageHelper.LANG_BN -> "ভাষা:"
                  AppLanguageHelper.LANG_AR -> "اللغة:"
                  else -> "Language:"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
              )
              LanguageSmallChip(
                label = "বাংলা",
                isSelected = selectedLang == AppLanguageHelper.LANG_BN,
                onClick = { selectedLang = AppLanguageHelper.LANG_BN },
                testTag = "important_prayer_lang_bn"
              )
              LanguageSmallChip(
                label = "English",
                isSelected = selectedLang == AppLanguageHelper.LANG_EN,
                onClick = { selectedLang = AppLanguageHelper.LANG_EN },
                testTag = "important_prayer_lang_en"
              )
              LanguageSmallChip(
                label = "العربية",
                isSelected = selectedLang == AppLanguageHelper.LANG_AR,
                onClick = { selectedLang = AppLanguageHelper.LANG_AR },
                testTag = "important_prayer_lang_ar"
              )
            }
          }
        }

        // Tabs for the 3 Prayers
        ScrollableTabRow(
          selectedTabIndex = selectedTabIndex,
          edgePadding = 16.dp,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary,
          indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
              TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colorScheme.primary,
                height = 3.dp
              )
            }
          }
        ) {
          prayers.forEachIndexed { index, prayer ->
            val title = when (selectedLang) {
              AppLanguageHelper.LANG_BN -> prayer.titleBn
              AppLanguageHelper.LANG_AR -> prayer.titleAr
              else -> prayer.titleEn
            }
            Tab(
              selected = selectedTabIndex == index,
              onClick = { selectedTabIndex = index },
              text = {
                Text(
                  text = title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                  color = if (selectedTabIndex == index) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  }
                )
              },
              modifier = Modifier.testTag("prayer_tab_$index")
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Content Body
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // 1. Prayer Title & Rak'ah Pill
          item {
            PrayerHeroCard(
              prayer = currentPrayer,
              lang = selectedLang
            )
          }

          // 2. Virtues / ফযীলত Card
          item {
            PrayerVirtuesCard(
              prayer = currentPrayer,
              lang = selectedLang
            )
          }

          // 3. Method & Rules / সুন্নাত তরীকা ও নিয়ম Card
          item {
            PrayerMethodCard(
              prayer = currentPrayer,
              lang = selectedLang
            )
          }

          // 4. Arabic Dua, বিশেষ দ্রষ্টব্য, Bangla Pronunciation, English Transliteration & Translation ("মূল দু'আ ও তাসবীহ")
          item {
            PrayerDuaDetailCard(
              prayer = currentPrayer,
              lang = selectedLang
            )
          }

          // 5. Special Step Breakdown (For Salatut Tasbih: "প্রতি রাকা'আতে ৭৫ বার তাসবীহ বন্টনের নিয়ম")
          if (currentPrayer.steps.isNotEmpty()) {
            item {
              SalatutTasbihStepsCard(
                steps = currentPrayer.steps,
                lang = selectedLang
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LanguageSmallChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
    contentColor = if (isSelected) Color(0xFF064E3B) else Color.White,
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun PrayerHeroCard(
  prayer: ImportantPrayerItem,
  lang: String
) {
  val title = when (lang) {
    AppLanguageHelper.LANG_BN -> prayer.titleBn
    AppLanguageHelper.LANG_AR -> prayer.titleAr
    else -> prayer.titleEn
  }
  val subtitle = when (lang) {
    AppLanguageHelper.LANG_BN -> prayer.subtitleBn
    AppLanguageHelper.LANG_AR -> prayer.subtitleAr
    else -> prayer.subtitleEn
  }
  val rakahs = when (lang) {
    AppLanguageHelper.LANG_BN -> prayer.rakahsBn
    AppLanguageHelper.LANG_AR -> prayer.rakahsAr
    else -> prayer.rakahs
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Text(
            text = rakahs,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
      )
    }
  }
}

@Composable
private fun PrayerVirtuesCard(
  prayer: ImportantPrayerItem,
  lang: String
) {
  val sectionTitle = when (lang) {
    AppLanguageHelper.LANG_BN -> "ফযীলত ও গুরুত্ব"
    AppLanguageHelper.LANG_AR -> "الفضل والأهمية"
    else -> "Virtues & Significance"
  }
  val content = when (lang) {
    AppLanguageHelper.LANG_BN -> prayer.virtuesBn
    AppLanguageHelper.LANG_AR -> prayer.virtuesAr
    else -> prayer.virtuesEn
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = null,
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(22.dp)
        )
        Text(
          text = sectionTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 15.5.sp,
          lineHeight = 25.sp
        ),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun PrayerMethodCard(
  prayer: ImportantPrayerItem,
  lang: String
) {
  val sectionTitle = when (lang) {
    AppLanguageHelper.LANG_BN -> "আদায়ের সুন্নাত নিয়ম ও তরীকা"
    AppLanguageHelper.LANG_AR -> "طريقة وكيفية الأداء"
    else -> "Step-by-Step Method & Guidelines"
  }
  val content = when (lang) {
    AppLanguageHelper.LANG_BN -> prayer.methodBn
    AppLanguageHelper.LANG_AR -> prayer.methodAr
    else -> prayer.methodEn
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Mosque,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Text(
          text = sectionTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 15.5.sp,
          lineHeight = 25.sp
        ),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun SalatutTasbihStepsCard(
  steps: List<PrayerStep>,
  lang: String
) {
  val sectionTitle = when (lang) {
    AppLanguageHelper.LANG_BN -> "প্রতি রাকা'আতে ৭৫ বার তাসবীহ বণ্টনের নিয়ম"
    AppLanguageHelper.LANG_AR -> "توزيع ٧٥ تسبيحة في مواضع الركعة"
    else -> "Breakdown: 75 Tasbihs per Rak'ah"
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Numbers,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Text(
          text = sectionTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      steps.forEach { step ->
        val posTitle = when (lang) {
          AppLanguageHelper.LANG_BN -> step.positionTitleBn
          AppLanguageHelper.LANG_AR -> step.positionTitleAr
          else -> step.positionTitleEn
        }
        val instruction = when (lang) {
          AppLanguageHelper.LANG_BN -> step.instructionBn
          AppLanguageHelper.LANG_AR -> step.instructionAr
          else -> step.instructionEn
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = posTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = instruction,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF047857),
              contentColor = Color.White
            ) {
              Text(
                text = when (lang) {
                  AppLanguageHelper.LANG_BN -> "${AppLanguageHelper.localizeNumbers(step.count.toString(), "bn")} বার"
                  AppLanguageHelper.LANG_AR -> "${AppLanguageHelper.localizeNumbers(step.count.toString(), "ar")} مرات"
                  else -> "${step.count}x"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}


/**
 * Detailed Dua Card containing Arabic Text, Bangla Pronunciation, English Transliteration, and Full Translation.
 */
@Composable
private fun PrayerDuaDetailCard(
  prayer: ImportantPrayerItem,
  lang: String
) {
  val context = LocalContext.current

  fun copyDuaToClipboard() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipText = buildString {
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> prayer.titleBn
        AppLanguageHelper.LANG_AR -> prayer.titleAr
        else -> prayer.titleEn
      })
      appendLine()
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> "আরবি দু'আ:"
        AppLanguageHelper.LANG_AR -> "النص العربي:"
        else -> "Arabic Dua:"
      })
      appendLine(prayer.duaArabic)
      appendLine()
      if (lang == AppLanguageHelper.LANG_BN) {
        appendLine("বাংলা উচ্চারণ:")
        appendLine(prayer.duaPronunciationBn)
        appendLine()
      } else if (lang == AppLanguageHelper.LANG_EN) {
        appendLine("English Transliteration:")
        appendLine(prayer.duaPronunciationEn)
        appendLine()
      }
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> "অর্থ ও অনুবাদ:"
        AppLanguageHelper.LANG_AR -> "معنى الدعاء والترجمة:"
        else -> "Translation & Meaning:"
      })
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> prayer.duaMeaningBn
        AppLanguageHelper.LANG_AR -> prayer.duaMeaningAr
        else -> prayer.duaMeaningEn
      })
    }
    val clip = ClipData.newPlainText("Prayer Dua", clipText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, if (lang == "bn") "দু'আ কপি করা হয়েছে" else "Dua copied to clipboard", Toast.LENGTH_SHORT).show()
  }

  fun shareDua() {
    val shareTitle = when (lang) {
      AppLanguageHelper.LANG_BN -> prayer.titleBn
      AppLanguageHelper.LANG_AR -> prayer.titleAr
      else -> prayer.titleEn
    }
    val shareText = buildString {
      appendLine(shareTitle)
      appendLine()
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> "আরবি দু'আ:"
        AppLanguageHelper.LANG_AR -> "النص العربي:"
        else -> "Arabic Dua:"
      })
      appendLine(prayer.duaArabic)
      appendLine()
      if (lang == AppLanguageHelper.LANG_BN) {
        appendLine("বাংলা উচ্চারণ:")
        appendLine(prayer.duaPronunciationBn)
        appendLine()
      } else if (lang == AppLanguageHelper.LANG_EN) {
        appendLine("English Transliteration:")
        appendLine(prayer.duaPronunciationEn)
        appendLine()
      }
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> "অর্থ ও অনুবাদ:"
        AppLanguageHelper.LANG_AR -> "معنى الدعاء والترجمة:"
        else -> "Translation & Meaning:"
      })
      appendLine(when (lang) {
        AppLanguageHelper.LANG_BN -> prayer.duaMeaningBn
        AppLanguageHelper.LANG_AR -> prayer.duaMeaningAr
        else -> prayer.duaMeaningEn
      })
      appendLine()
      appendLine("— My Own Prayer App")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, shareTitle)
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Dua"))
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.5.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header with Copy & Share
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = when (lang) {
              AppLanguageHelper.LANG_BN -> "মূল দু'আ ও তাসবীহ"
              AppLanguageHelper.LANG_AR -> "الدعاء والتسبيح المأثور"
              else -> "The Core Dua & Supplication"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = { copyDuaToClipboard() },
            modifier = Modifier.size(36.dp).testTag("copy_dua_btn")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy Dua",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = { shareDua() },
            modifier = Modifier.size(36.dp).testTag("share_dua_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share Dua",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // 1. Arabic Dua Box with Clear, High-Contrast Readability
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
          ) {
            Text(
              text = "النص العربي (الدعاء)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Text(
            text = prayer.duaArabic,
            style = MaterialTheme.typography.headlineSmall.copy(
              fontFamily = FontFamily.Default,
              fontSize = 22.sp,
              lineHeight = 40.sp,
              letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // বিশেষ দ্রষ্টব্য / Important Note (Directly below the Dua)
      if (prayer.importantNoteBn != null) {
        PrayerNoteCard(
          prayer = prayer,
          lang = lang
        )
      }

      // 2. বাংলা উচ্চারণ (Bengali Pronunciation) - Shown when Bangla is selected
      if (lang == AppLanguageHelper.LANG_BN) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "বাংলা উচ্চারণ:",
              style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = prayer.duaPronunciationBn,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 26.sp
              ),
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // 3. English Transliteration / Pronunciation - Shown ONLY when English is selected (hidden for Bangla)
      if (lang == AppLanguageHelper.LANG_EN) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "English Pronunciation / Transliteration:",
              style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = prayer.duaPronunciationEn,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.5.sp,
                lineHeight = 25.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // 4. অর্থ ও তাৎপর্য (Translation / Meaning)
      val translationLabel = when (lang) {
        AppLanguageHelper.LANG_BN -> "অনুবাদ ও অর্থ:"
        AppLanguageHelper.LANG_AR -> "معنى الدعاء والترجمة:"
        else -> "Translation & Meaning:"
      }
      val translationText = when (lang) {
        AppLanguageHelper.LANG_BN -> prayer.duaMeaningBn
        AppLanguageHelper.LANG_AR -> prayer.duaMeaningAr
        else -> prayer.duaMeaningEn
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = translationLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = translationText,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontSize = 15.5.sp,
              lineHeight = 25.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
private fun PrayerNoteCard(
  prayer: ImportantPrayerItem,
  lang: String
) {
  val noteText = when (lang) {
    AppLanguageHelper.LANG_BN -> prayer.importantNoteBn
    AppLanguageHelper.LANG_AR -> prayer.importantNoteAr
    else -> prayer.importantNoteEn
  } ?: return

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    )
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(24.dp)
      )
      Text(
        text = noteText,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = 15.5.sp,
          lineHeight = 24.sp
        ),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onTertiaryContainer
      )
    }
  }
}
