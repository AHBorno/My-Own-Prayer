package com.example.util

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.widget.TasbeehWidgetProvider

data class DhikrItem(
  val id: Int,
  val arabic: String,
  val transliteration: String,
  val meaningEn: String,
  val meaningBn: String,
  val meaningAr: String,
  val defaultTarget: Int = 33
)

data class IncrementResult(
  val newCount: Int,
  val newTotal: Long,
  val newRounds: Int,
  val targetReached: Boolean
)

object TasbeehPreferences {
  private const val TAG = "TasbeehPreferences"
  private const val PREFS_NAME = "tasbeeh_prefs"

  private const val KEY_COUNT = "tasbeeh_current_count"
  private const val KEY_TOTAL = "tasbeeh_total_count"
  private const val KEY_ROUNDS = "tasbeeh_round_count"
  private const val KEY_TARGET = "tasbeeh_target_count"
  private const val KEY_DHIKR_INDEX = "tasbeeh_dhikr_index"
  private const val KEY_VIBRATION = "tasbeeh_vibration_enabled"
  private const val KEY_SOUND = "tasbeeh_sound_enabled"

  val DHIKR_PRESETS: List<DhikrItem> = listOf(
    DhikrItem(
      id = 0,
      arabic = "سُبْحَانَ ٱللَّٰهِ",
      transliteration = "SubhanAllah",
      meaningEn = "Glory be to Allah",
      meaningBn = "আল্লাহ পবিত্র ও মহামহিম",
      meaningAr = "تنزيهاً لله تعالى عن كل نقص",
      defaultTarget = 33
    ),
    DhikrItem(
      id = 1,
      arabic = "ٱلْحَمْدُ لِلَّٰهِ",
      transliteration = "Alhamdulillah",
      meaningEn = "Praise be to Allah",
      meaningBn = "সকল প্রশংসা আল্লাহর জন্য",
      meaningAr = "الثناء على الله بصفات الكمال",
      defaultTarget = 33
    ),
    DhikrItem(
      id = 2,
      arabic = "ٱللَّٰهُ أَكْبَرُ",
      transliteration = "Allahu Akbar",
      meaningEn = "Allah is the Greatest",
      meaningBn = "আল্লাহ মহান",
      meaningAr = "الله أعظم وأكبر من كل شيء",
      defaultTarget = 34
    ),
    DhikrItem(
      id = 3,
      arabic = "لَا إِلٰهَ إِلَّا ٱللَّٰهُ",
      transliteration = "La ilaha illallah",
      meaningEn = "There is no deity except Allah",
      meaningBn = "আল্লাহ ছাড়া কোনো উপাস্য নেই",
      meaningAr = "لا معبود بحق إلا الله وحده",
      defaultTarget = 100
    ),
    DhikrItem(
      id = 4,
      arabic = "أَسْتَغْفِرُ ٱللَّٰهَ",
      transliteration = "Astaghfirullah",
      meaningEn = "I seek forgiveness from Allah",
      meaningBn = "আমি আল্লাহর নিকট ক্ষমা প্রার্থনা করছি",
      meaningAr = "طلب المغفرة والستر من الله",
      defaultTarget = 100
    ),
    DhikrItem(
      id = 5,
      arabic = "سُبْحَانَ ٱللَّٰهِ وَبِحَمْدِهِ",
      transliteration = "SubhanAllahi wa bihamdihi",
      meaningEn = "Glory be to Allah and His is the praise",
      meaningBn = "আল্লাহর পবিত্রতা ঘোষণা করছি ও তাঁর প্রশংসা করছি",
      meaningAr = "تسبيح لله مقروناً بحمده",
      defaultTarget = 100
    ),
    DhikrItem(
      id = 6,
      arabic = "سُبْحَانَ ٱللَّٰهِ ٱلْعَظِيمِ",
      transliteration = "SubhanAllahil Azeem",
      meaningEn = "Glory be to Allah, the Almighty",
      meaningBn = "মহান আল্লাহর পবিত্রতা ঘোষণা করছি",
      meaningAr = "تسبيح الله العظيم ذي الجلال",
      defaultTarget = 100
    ),
    DhikrItem(
      id = 7,
      arabic = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِٱللَّٰهِ",
      transliteration = "La hawla wa la quwwata illa billah",
      meaningEn = "There is no power nor strength except with Allah",
      meaningBn = "আল্লাহর সাহায্য ছাড়া কোনো শক্তি বা ক্ষমতা নেই",
      meaningAr = "كنز من كنوز الجنة",
      defaultTarget = 33
    ),
    DhikrItem(
      id = 8,
      arabic = "ٱللَّٰهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
      transliteration = "Allahumma salli 'ala Muhammad",
      meaningEn = "O Allah, send blessings upon Muhammad",
      meaningBn = "হে আল্লাহ, মুহাম্মদ (সাঃ)-এর উপর রহমত বর্ষণ করুন",
      meaningAr = "الصلاة والسلام على النبي المختار",
      defaultTarget = 100
    ),
    DhikrItem(
      id = 9,
      arabic = "حَسْبُنَا ٱللَّٰهُ وَنِعْمَ ٱلْوَكِيلُ",
      transliteration = "HasbunAllahu wa ni'mal wakeel",
      meaningEn = "Allah is sufficient for us and He is the best Disposer of affairs",
      meaningBn = "আল্লাহই আমাদের জন্য যথেষ্ট এবং তিনি উত্তম কর্মবিধায়ক",
      meaningAr = "التوكل الكامل على الله تعالى",
      defaultTarget = 100
    ),
    DhikrItem(
      id = 10,
      arabic = "لَا إِلٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ ٱلظَّالِمِينَ",
      transliteration = "La ilaha illa anta subhanaka inni kuntu minaz-zalimeen",
      meaningEn = "None has the right to be worshipped but You, Glory be to You, I was of the wrongdoers",
      meaningBn = "তুমি ছাড়া কোনো উপাস্য নেই, তুমি পবিত্র; আমি অপরাধী ছিলাম",
      meaningAr = "دعاء ذي النون في بطن الحوت",
      defaultTarget = 40
    )
  )

  fun getCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_COUNT, 0)
  }

  fun getTotalCount(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_TOTAL, 0L)
  }

  fun getRoundCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_ROUNDS, 0)
  }

  fun getTargetCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_TARGET, 33)
  }

  fun getCurrentDhikrIndex(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val idx = prefs.getInt(KEY_DHIKR_INDEX, 0)
    return if (idx in DHIKR_PRESETS.indices) idx else 0
  }

  fun getCurrentDhikr(context: Context): DhikrItem {
    val index = getCurrentDhikrIndex(context)
    return DHIKR_PRESETS.getOrElse(index) { DHIKR_PRESETS[0] }
  }

  fun isVibrationEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_VIBRATION, true)
  }

  fun isSoundEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_SOUND, true)
  }

  /**
   * Increments the count persistently.
   * Never resets until user explicitly resets it.
   */
  @Synchronized
  fun increment(context: Context): IncrementResult {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getInt(KEY_COUNT, 0)
    val total = prefs.getLong(KEY_TOTAL, 0L)
    var rounds = prefs.getInt(KEY_ROUNDS, 0)
    val target = prefs.getInt(KEY_TARGET, 33)

    val nextCount = current + 1
    val nextTotal = total + 1
    var targetReached = false

    if (target > 0 && nextCount >= target) {
      targetReached = true
      rounds += 1
      prefs.edit()
        .putInt(KEY_COUNT, 0)
        .putLong(KEY_TOTAL, nextTotal)
        .putInt(KEY_ROUNDS, rounds)
        .apply()

      triggerHapticFeedback(context, isTargetReached = true)
      notifyWidgetUpdate(context)
      return IncrementResult(
        newCount = 0,
        newTotal = nextTotal,
        newRounds = rounds,
        targetReached = true
      )
    } else {
      prefs.edit()
        .putInt(KEY_COUNT, nextCount)
        .putLong(KEY_TOTAL, nextTotal)
        .apply()

      triggerHapticFeedback(context, isTargetReached = false)
      notifyWidgetUpdate(context)
      return IncrementResult(
        newCount = nextCount,
        newTotal = nextTotal,
        newRounds = rounds,
        targetReached = false
      )
    }
  }

  /**
   * Decrements the count (if misclicked, min 0).
   */
  @Synchronized
  fun decrement(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getInt(KEY_COUNT, 0)
    val total = prefs.getLong(KEY_TOTAL, 0L)

    if (current > 0) {
      val nextCount = current - 1
      val nextTotal = if (total > 0) total - 1 else 0L
      prefs.edit()
        .putInt(KEY_COUNT, nextCount)
        .putLong(KEY_TOTAL, nextTotal)
        .apply()
      triggerHapticFeedback(context, isTargetReached = false)
      notifyWidgetUpdate(context)
      return nextCount
    }
    return 0
  }

  /**
   * Explicit user reset. Resets current count (and optionally round count).
   * Total cumulative count is preserved unless resetTotal is explicitly requested.
   */
  @Synchronized
  fun reset(context: Context, resetRounds: Boolean = true, resetTotal: Boolean = false) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit().putInt(KEY_COUNT, 0)
    if (resetRounds) {
      editor.putInt(KEY_ROUNDS, 0)
    }
    if (resetTotal) {
      editor.putLong(KEY_TOTAL, 0L)
    }
    editor.apply()
    triggerHapticFeedback(context, isTargetReached = false)
    notifyWidgetUpdate(context)
  }

  fun setTargetCount(context: Context, target: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_TARGET, target).apply()
    notifyWidgetUpdate(context)
  }

  fun setDhikrIndex(context: Context, index: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val safeIndex = if (index in DHIKR_PRESETS.indices) index else 0
    val target = DHIKR_PRESETS[safeIndex].defaultTarget
    prefs.edit()
      .putInt(KEY_DHIKR_INDEX, safeIndex)
      .putInt(KEY_TARGET, target)
      .apply()
    notifyWidgetUpdate(context)
  }

  fun nextDhikr(context: Context): DhikrItem {
    val current = getCurrentDhikrIndex(context)
    val nextIndex = (current + 1) % DHIKR_PRESETS.size
    setDhikrIndex(context, nextIndex)
    return DHIKR_PRESETS[nextIndex]
  }

  fun setVibrationEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
  }

  fun setSoundEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
  }

  private fun triggerHapticFeedback(context: Context, isTargetReached: Boolean) {
    if (!isVibrationEnabled(context)) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        if (isTargetReached) {
          vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), -1))
        } else {
          vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          if (isTargetReached) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), -1))
          } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
          }
        } else {
          @Suppress("DEPRECATION")
          if (isTargetReached) {
            vibrator?.vibrate(longArrayOf(0, 100, 80, 150), -1)
          } else {
            vibrator?.vibrate(35)
          }
        }
      }
    } catch (_: Exception) {}
  }

  /**
   * Prompts launcher to pin the Tasbeeh widget to Home Screen directly (Android 8.0+).
   */
  fun requestPinWidget(context: Context): Boolean {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
        val myProvider = ComponentName(context, TasbeehWidgetProvider::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
          val successCallback = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, TasbeehWidgetProvider::class.java).apply {
              action = TasbeehWidgetProvider.ACTION_UPDATE_WIDGET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
          appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
          true
        } else {
          false
        }
      } else {
        false
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error requesting pin widget", e)
      false
    }
  }

  /**
   * Broadcasts widget update so all active Tasbeeh widgets refresh their UI instantly.
   */
  fun notifyWidgetUpdate(context: Context) {
    try {
      val intent = Intent(context, TasbeehWidgetProvider::class.java).apply {
        action = TasbeehWidgetProvider.ACTION_UPDATE_WIDGET
      }
      context.sendBroadcast(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Error notifying widget update", e)
    }
  }
}
