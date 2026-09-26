package com.example.util

import android.content.Context
import com.example.data.model.PrayerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLanguageHelper {
  const val LANG_EN = "en"
  const val LANG_BN = "bn"
  const val LANG_AR = "ar"

  fun getSavedLanguage(context: Context): String {
    val prefs = context.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)
    return prefs.getString("key_app_language", LANG_EN) ?: LANG_EN
  }

  fun setLanguage(context: Context, lang: String) {
    val prefs = context.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("key_app_language", lang).apply()
  }

  /**
   * Translates Western Arabic digits (0-9) to Bengali (০-৯) or Eastern Arabic (٠-٩) digits.
   */
  fun localizeNumbers(input: String, lang: String): String {
    if (input.isEmpty()) return input
    return when (lang) {
      LANG_BN -> {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (c in input) {
          if (c in '0'..'9') {
            sb.append(bnDigits[c - '0'])
          } else {
            sb.append(c)
          }
        }
        sb.toString()
      }
      LANG_AR -> {
        val arDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder(input.length)
        for (c in input) {
          if (c in '0'..'9') {
            sb.append(arDigits[c - '0'])
          } else {
            sb.append(c)
          }
        }
        sb.toString()
      }
      else -> input
    }
  }

  /**
   * Localizes 12-hour time strings like "08:49 AM" or "03:17 PM".
   */
  fun localizeTime(time12h: String, lang: String): String {
    if (time12h.isEmpty() || time12h == "--:--") return time12h
    val localizedDigits = localizeNumbers(time12h, lang)
    return when (lang) {
      LANG_BN -> localizedDigits.replace("AM", "AM").replace("PM", "PM")
      LANG_AR -> localizedDigits.replace("AM", "ص").replace("PM", "م")
      else -> localizedDigits
    }
  }

  /**
   * Localizes time intervals like "05:40 AM - 05:55 AM".
   */
  fun localizeInterval(interval: String, lang: String): String {
    if (interval.isEmpty()) return interval
    val parts = interval.split("-")
    return if (parts.size == 2) {
      val start = localizeTime(parts[0].trim(), lang)
      val end = localizeTime(parts[1].trim(), lang)
      when (lang) {
        LANG_BN -> "$start - $end"
        LANG_AR -> "$start إلى $end"
        else -> "$start - $end"
      }
    } else {
      localizeTime(interval, lang)
    }
  }

  /**
   * Localizes countdown timer text (e.g. "01h 52m 43s" or "00:45:12").
   */
  fun localizeCountdown(countdown: String, lang: String): String {
    if (countdown.isEmpty()) return countdown
    val localized = localizeNumbers(countdown, lang)
    return when (lang) {
      LANG_BN -> localized
        .replace("h", " ঘণ্টা")
        .replace("m", " মি.")
        .replace("s", " সে.")
      LANG_AR -> localized
        .replace("h", " س")
        .replace("m", " د")
        .replace("s", " ث")
      else -> localized
    }
  }

  /**
   * Formats the current date in the chosen language.
   */
  fun formatFormattedDate(date: Date = Date(), lang: String): String {
    val locale = when (lang) {
      LANG_BN -> Locale("bn", "BD")
      LANG_AR -> Locale("ar", "SA")
      else -> Locale.ENGLISH
    }
    val pattern = when (lang) {
      LANG_BN -> "EEEE, d MMMM, yyyy"
      LANG_AR -> "EEEE، d MMMM yyyy"
      else -> "EEE, MMM d, yyyy"
    }
    val sdf = SimpleDateFormat(pattern, locale)
    return localizeNumbers(sdf.format(date), lang)
  }

  /**
   * Localized name for prayers.
   */
  fun getPrayerDisplayName(prayerName: String, lang: String): String {
    val type = PrayerType.fromName(prayerName)
    if (type != null) {
      return getPrayerDisplayName(type, lang)
    }
    return when (prayerName.lowercase().trim()) {
      "fajr" -> when (lang) { LANG_BN -> "ফজর"; LANG_AR -> "الفجر"; else -> "Fajr" }
      "sunrise" -> when (lang) { LANG_BN -> "সূর্যোদয়"; LANG_AR -> "الشروق"; else -> "Sunrise" }
      "ishraq" -> when (lang) { LANG_BN -> "ইশরাক"; LANG_AR -> "الإشراق"; else -> "Ishraq" }
      "duha", "chasht" -> when (lang) { LANG_BN -> "চাশত (দুহা)"; LANG_AR -> "الضحى"; else -> "Duha" }
      "dhuhr" -> when (lang) { LANG_BN -> "যোহর"; LANG_AR -> "الظهر"; else -> "Dhuhr" }
      "asr" -> when (lang) { LANG_BN -> "আসর"; LANG_AR -> "العصر"; else -> "Asr" }
      "maghrib" -> when (lang) { LANG_BN -> "মাগরিব"; LANG_AR -> "المغرب"; else -> "Maghrib" }
      "isha" -> when (lang) { LANG_BN -> "এশা"; LANG_AR -> "العشاء"; else -> "Isha" }
      "tahajjud" -> when (lang) { LANG_BN -> "তাহাজ্জুদ"; LANG_AR -> "التهجد"; else -> "Tahajjud" }
      "sunset" -> when (lang) { LANG_BN -> "সূর্যাস্ত"; LANG_AR -> "الغروب"; else -> "Sunset" }
      else -> prayerName
    }
  }

  fun getPrayerDisplayName(type: PrayerType, lang: String): String {
    return when (lang) {
      LANG_BN -> type.bengaliName
      LANG_AR -> type.arabicName
      else -> type.displayName
    }
  }

  /**
   * Localized description for prayers.
   */
  fun getPrayerDescription(type: PrayerType, lang: String): String {
    return when (type) {
      PrayerType.FAJR -> when (lang) {
        LANG_BN -> "ভোরের ফরজ সালাত (সূর্যোদয় পর্যন্ত)"
        LANG_AR -> "صلاة الفجر حتى طلوع الشمس"
        else -> "Dawn prayer until sunrise"
      }
      PrayerType.SUNRISE -> when (lang) {
        LANG_BN -> "দিগন্তে সূর্যের প্রকাশ"
        LANG_AR -> "شروق الشمس في الأفق"
        else -> "Solar horizon transit"
      }
      PrayerType.ISHRAQ -> when (lang) {
        LANG_BN -> "সূর্যোদয়ের ১৫ মিনিট পর (সুন্নত)"
        LANG_AR -> "بعد الشروق بـ ١٥ دقيقة (سنة)"
        else -> "15 min after sunrise (Sunnah)"
      }
      PrayerType.DUHA -> when (lang) {
        LANG_BN -> "চাশতের সালাত (যাওয়ালের পূর্ব পর্যন্ত)"
        LANG_AR -> "صلاة الضحى حتى استواء الشمس"
        else -> "Forenoon prayer until solar zenith (Sunnah)"
      }
      PrayerType.DHUHR -> when (lang) {
        LANG_BN -> "দুপুরের ফরজ সালাত"
        LANG_AR -> "صلاة الظهر المفروضة"
        else -> "Midday obligatory prayer"
      }
      PrayerType.ASR -> when (lang) {
        LANG_BN -> "বিকেলের ফরজ সালাত"
        LANG_AR -> "صلاة العصر المفروضة"
        else -> "Late afternoon prayer until sunset transition"
      }
      PrayerType.MAGHRIB -> when (lang) {
        LANG_BN -> "সূর্যাস্তের ফরজ সালাত"
        LANG_AR -> "صلاة المغرب المفروضة"
        else -> "Sunset obligatory prayer"
      }
      PrayerType.ISHA -> when (lang) {
        LANG_BN -> "রাত্রিকালীন ফরজ সালাত"
        LANG_AR -> "صلاة العشاء المفروضة"
        else -> "Night obligatory prayer"
      }
      PrayerType.TAHAJJUD -> when (lang) {
        LANG_BN -> "রাতের শেষ তৃতীয়াংশের নফল সালাত (কিয়ামুল লাইল)"
        LANG_AR -> "الثلث الأخير من الليل (قيام الليل)"
        else -> "Last third of night (Qiyam al-Layl)"
      }
    }
  }

  /**
   * Localized names for Forbidden times.
   */
  fun getForbiddenName(name: String, lang: String): String {
    return when {
      name.contains("Sunrise", ignoreCase = true) -> when (lang) {
        LANG_BN -> "সূর্যোদয় বিরতি"
        LANG_AR -> "شروق الشمس"
        else -> "Sunrise Transition"
      }
      name.contains("Zenith", ignoreCase = true) -> when (lang) {
        LANG_BN -> "দ্বিপ্রহরের সূর্য চূড়া (যাওয়াল)"
        LANG_AR -> "استواء الشمس (الزوال)"
        else -> "Midday Solar Zenith"
      }
      else -> when (lang) {
        LANG_BN -> "সূর্যাস্ত বিরতি"
        LANG_AR -> "غروب الشمس"
        else -> "Sunset Transition"
      }
    }
  }

  /**
   * Localized descriptions for Forbidden times.
   */
  fun getForbiddenDescription(name: String, lang: String): String {
    return when {
      name.contains("Sunrise", ignoreCase = true) -> when (lang) {
        LANG_BN -> "সূর্যোদয় থেকে দিগন্তে সূর্য পুরোপুরি ওঠার মধ্যবর্তী সময় (~১৫ মিনিট)"
        LANG_AR -> "من طلوع الشمس حتى ترتفع قيد رمح (~١٥ دقيقة)"
        else -> "From sunrise until the sun has risen above the horizon (~15 mins)"
      }
      name.contains("Zenith", ignoreCase = true) -> when (lang) {
        LANG_BN -> "সূর্য যখন ঠিক মাথার উপরে থাকে যতক্ষণ না যোহর শুরু হয় (~১৫ মিনিট)"
        LANG_AR -> "عندما تكون الشمس في كبد السماء حتى تميل للظهر (~١٥ دقيقة)"
        else -> "When the sun is at its exact zenith until it declines into Dhuhr (~15 mins)"
      }
      else -> when (lang) {
        LANG_BN -> "সূর্যের আলো ফ্যাকাশে হয়ে মাগরিব পর্যন্ত ডোবার সময় (~১৫ মিনিট)"
        LANG_AR -> "عند اصفرار الشمس حتى تغرب ويبدأ المغرب (~١٥ دقيقة)"
        else -> "When the sun pales and sets into the horizon before Maghrib (~15 mins)"
      }
    }
  }

  fun getNotificationTitle(
    prayerName: String,
    prayerTime: String,
    lang: String,
    isRamadan: Boolean = false
  ): String {
    if (isRamadan) {
      if (prayerName.equals("Maghrib", ignoreCase = true)) {
        val locTime = localizeTime(prayerTime, lang)
        return when (lang) {
          LANG_BN -> "✨ ইফতারের সময় (মাগরিব) ($locTime)"
          LANG_AR -> "✨ موعد الإفطار (المغرب) ($locTime)"
          else -> "✨ Iftar time (Maghrib) ($prayerTime)"
        }
      } else if (prayerName.equals("Fajr", ignoreCase = true)) {
        val locTime = localizeTime(prayerTime, lang)
        return when (lang) {
          LANG_BN -> "⏳ সেহরির সময় শেষ (ফজর) ($locTime)"
          LANG_AR -> "⏳ نهاية وقت السحور (الفجر) ($locTime)"
          else -> "⏳ Suhoor Ended (Fajr) ($prayerTime)"
        }
      }
    }
    val localizedName = getPrayerDisplayName(prayerName, lang)
    val localizedTimeStr = localizeTime(prayerTime, lang)
    return when (lang) {
      LANG_BN -> "🕌 $localizedName সালাতের ওয়াক্ত হয়েছে ($localizedTimeStr)"
      LANG_AR -> "🕌 حان الآن وقت صلاة $localizedName ($localizedTimeStr)"
      else -> "🕌 Time for $localizedName Prayer ($prayerTime)"
    }
  }

  fun getNotificationSubtext(lang: String): String {
    return when (lang) {
      LANG_BN -> "সময়মতো সালাত আদায় করতে সংক্ষিপ্ত বিরতি নিন।"
      LANG_AR -> "أرحنا بها يا بلال.. أقم الصلاة لوقتها."
      else -> "Take a mindful break to establish your prayer on time."
    }
  }

  fun getPrayerQuote(prayerName: String, lang: String, isRamadan: Boolean = false): String {
    if (isRamadan) {
      if (prayerName.equals("Maghrib", ignoreCase = true)) {
        return when (lang) {
          LANG_BN -> "পিপাসা দূরীভূত হলো, শিরা-উপশিরা সিক্ত হলো এবং ইনশাআল্লাহ সওয়াব নির্ধারিত হলো। (আবু দাউদ ২৩৫৭)"
          LANG_AR -> "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ (أبو داود)"
          else -> "The thirst is gone, the veins are moistened, and the reward is confirmed, if Allah wills. (Abu Dawud)"
        }
      }
    }
    val key = prayerName.lowercase()
    return when (lang) {
      LANG_BN -> when (key) {
        "fajr" -> "নিশ্চয়ই ভোরের কুরআন তিলাওয়াত বিশেষভাবে উপস্থিতির সময়। (সূরা আল-ইসরা ১৭:৭৮)"
        "ishraq" -> "যে ব্যক্তি ফজরের পর সূর্যোদয় পর্যন্ত আল্লাহর জিকিরে রত থেকে দুই রাকাত নফল পড়ে, সে পূর্ণ হজ্জ ও উমরার সওয়াব পায়। (তিরমিযী)"
        "duha", "chasht" -> "মানবদেহের প্রতিটি গ্রন্থির উপর সদকা রয়েছে; দুই রাকাত চাশতের সালাত তার জন্য যথেষ্ট। (সহীহ মুসলিম)"
        "dhuhr" -> "সূর্য ঢলে পড়ার পর থেকে সালাত কায়েম করুন। (সূরা আল-ইসরা ১৭:৭৮)"
        "asr" -> "তোমরা সকল সালাতের প্রতি যত্নবান হও, বিশেষ করে মধ্যবর্তী আসর সালাতের। (সূরা আল-বাকারা ২:২৩৮)"
        "maghrib" -> "এবং সকাল-সন্ধ্যায় আপনার প্রতিপালকের পবিত্র নাম স্মরণ করুন। (সূরা আল-ইনসান ৭৬:২৫)"
        "isha" -> "এবং রাতের একাংশে তাঁর উদ্দেশ্যে সিজদাহ করুন ও দীর্ঘ রাত তাঁর মহিমা ঘোষণা করুন। (সূরা আল-ইনসান ৭৬:২৬)"
        "tahajjud" -> "ফরজ সালাতের পর সর্বোত্তম সালাত হলো রাতের তাহাজ্জুদ সালাত। (সহীহ মুসলিম)"
        else -> "নিশ্চয়ই সালাত মুমিনদের উপর নির্দিষ্ট সময়ে ফরজ করা হয়েছে। (সূরা আন-নিসা ৪:১০৩)"
      }
      LANG_AR -> when (key) {
        "fajr" -> "إِنَّ قُرْآنَ الْفَجْرِ كَانَ مَشْهُودًا (سورة الإسراء: 78)"
        "ishraq" -> "من صلى الغداة في جماعة ثم قعد يذكر الله حتى تطلع الشمس ثم صلى ركعتين كانت له كأجر حجة وعمرة. (الترمذي)"
        "duha", "chasht" -> "يُصبح على كل سُلامى من أحدكم صدقة.. ويُجزئ من ذلك ركعتان يركعهما من الضحى. (صحيح مسلم)"
        "dhuhr" -> "أَقِمِ الصَّلَاةَ لِدُلُوكِ الشَّمْسِ (سورة الإسراء: 78)"
        "asr" -> "حَافِظُوا عَلَى الصَّلَوَاتِ وَالصَّلَاةِ الْوُسْطَىٰ وَقُومُوا لِلَّهِ قَانِتِينَ (سورة البقرة: 238)"
        "maghrib" -> "وَاذْكُرِ اسْمَ رَبِّكَ بُكْرَةً وَأَصِيلًا (سورة الإنسان: 25)"
        "isha" -> "وَمِنَ اللَّيْلِ فَاسْجُدْ لَهُ وَسَبِّحْهُ لَيْلًا طَوِيلًا (سورة الإنسان: 26)"
        "tahajjud" -> "أَفْضَلُ الصَّلَاةِ بَعْدَ الصَّلَاةِ الْمَكْتُوبَةِ الصَّلَاةُ فِي جَوْفِ اللَّيْلِ (صحيح مسلم)"
        else -> "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا (سورة النساء: 103)"
      }
      else -> when (key) {
        "fajr" -> "Indeed, the recitation of dawn is ever witnessed. (Surah Al-Isra 17:78)"
        "ishraq" -> "Whoever prays Fajr, remembers Allah until sunrise, and prays two rak'ahs gets the reward of Hajj and Umrah. (Tirmidhi)"
        "duha", "chasht" -> "Charity is due upon every joint of your body; and two rak'ahs of Duha suffices for all of that. (Sahih Muslim)"
        "dhuhr" -> "Establish prayer at the decline of the sun. (Surah Al-Isra 17:78)"
        "asr" -> "Maintain with care the [obligatory] prayers and [in particular] the middle prayer. (Surah Al-Baqarah 2:238)"
        "maghrib" -> "And remember the name of your Lord morning and evening. (Surah Al-Insan 76:25)"
        "isha" -> "And during a part of the night, prostrate to Him and exalt Him a long night. (Surah Al-Insan 76:26)"
        "tahajjud" -> "The best prayer after the obligatory prayers is the night prayer (Tahajjud / Qiyam al-Layl). (Sahih Muslim)"
        else -> "Indeed, prayer has been decreed upon the believers a decree of specified times. (Surah An-Nisa 4:103)"
      }
    }
  }

  fun getForbiddenNotificationTitle(forbiddenName: String, lang: String): String {
    val localizedName = getForbiddenName(forbiddenName, lang)
    return when (lang) {
      LANG_BN -> "⚠️ সালাত আদায় নিষিদ্ধ সময়: $localizedName"
      LANG_AR -> "⚠️ وقت كراهة الصلاة: $localizedName"
      else -> "⚠️ Forbidden Prayer Time: $localizedName"
    }
  }

  fun getForbiddenNotificationBody(
    forbiddenName: String,
    intervalFormatted: String,
    lang: String
  ): String {
    val locInterval = localizeInterval(intervalFormatted, lang)
    return when (lang) {
      LANG_BN -> "এই সময়টিতে সকল প্রকার নফল নামায আদায় করা নিষিদ্ধ ($locInterval)।"
      LANG_AR -> "يُنهى عن أداء صلوات النوافل خلال هذه الفترة ($locInterval)."
      else -> "Voluntary (Nafl) prayers are prohibited during this interval ($locInterval)."
    }
  }

  fun getRamadanNotificationTitle(eventType: String, lang: String): String {
    return when (eventType) {
      "SUHOOR_SOON" -> when (lang) {
        LANG_BN -> "🌙 সাহরির শেষ সময় আসন্ন (~১৫ মিনিট)"
        LANG_AR -> "🌙 قرب انتهاء وقت السحور (~١٥ دقيقة)"
        else -> "🌙 Suhoor Ending Soon (~15 Mins)"
      }
      "SUHOOR_EXACT" -> when (lang) {
        LANG_BN -> "⏳ সাহরির সময় সমাপ্ত (ফজর শুরু)"
        LANG_AR -> "⏳ انتهى وقت السحور (بدأ الفجر)"
        else -> "⏳ Suhoor Ended (Fajr Begins)"
      }
      "IFTAR_SOON" -> when (lang) {
        LANG_BN -> "🤲 ইফতারের প্রস্তুতি নিন (~১৪ মিনিট বাকি)"
        LANG_AR -> "🤲 اقترب موعد الإفطار (~١٤ دقيقة)"
        else -> "🤲 Iftar Approaching (~14 Mins)"
      }
      "IFTAR_EXACT" -> when (lang) {
        LANG_BN -> "✨ ইফতারের সময় হয়েছে — আলহামদুলিল্লাহ!"
        LANG_AR -> "✨ حان موعد الإفطار — تقبل الله!"
        else -> "✨ It is Time for Iftar — Alhamdulillah!"
      }
      else -> "Ramadan Reminder"
    }
  }

  fun getRamadanAlertTitle(alertType: String, lang: String): String {
    return when (alertType) {
      "IFTAR_SOON" -> when (lang) {
        LANG_BN -> "🤲 ইফতারের প্রস্তুতি নিন (~১৪ মিনিট বাকি)"
        LANG_AR -> "🤲 اقترب موعد الإفطار (~١٤ دقيقة)"
        else -> "🤲 Iftar soon (~14 mins)"
      }
      "SUHOOR_END_SOON", "SUHOOR_SOON" -> when (lang) {
        LANG_BN -> "🌙 সেহরির শেষ সময় আসন্ন (~১৫ মিনিট)"
        LANG_AR -> "🌙 قرب انتهاء وقت السحور (~١٥ دقيقة)"
        else -> "🌙 Suhoor end soon (~15 mins)"
      }
      else -> getRamadanNotificationTitle(alertType, lang)
    }
  }

  fun getRamadanAlertBody(alertType: String, targetTime: String, lang: String): String {
    return getRamadanNotificationBody(alertType, targetTime, lang)
  }

  fun getRamadanNotificationBody(
    eventType: String,
    timeFormatted: String,
    lang: String
  ): String {
    val locTime = localizeTime(timeFormatted, lang)
    return when (eventType) {
      "SUHOOR_SOON" -> when (lang) {
        LANG_BN -> "সাহরি শেষ হতে প্রায় ১৫ মিনিট বাকি ($locTime)। পানাহার সমাপ্ত করে রোযার নিয়ত করুন।"
        LANG_AR -> "متبقي نحو ١٥ دقيقة على انتهاء وقت السحور ($locTime). بادر بإنهاء سحورك والاستعداد للصيام."
        else -> "Suhoor ends in about 15 minutes ($locTime). Complete your meal and prepare for fasting."
      }
      "SUHOOR_EXACT" -> when (lang) {
        LANG_BN -> "সাহরির সময় সমাপ্ত হয়েছে ($locTime)। ফজরের ওয়াক্ত শুরু এবং আজকের রোযা শুরু হলো।"
        LANG_AR -> "انتهى وقت السحور الآن ($locTime). حان وقت أذان الفجر وبدأ صيام اليوم المبارك."
        else -> "Suhoor time has ended ($locTime). Fajr has begun and today's fast starts now."
      }
      "IFTAR_SOON" -> when (lang) {
        LANG_BN -> "ইফতারের আর মাত্র ১৪ মিনিট বাকি ($locTime)। ইফতার সামনে নিয়ে বেশি বেশি ইস্তিগফার ও দোয়া করুন।"
        LANG_AR -> "متبقي نحو ١٤ دقيقة على موعد الإفطار ($locTime). هذا وقت استجابة الدعاء فأكثر من التضرع والاستغفار."
        else -> "About 14 minutes remaining until Iftar ($locTime). A blessed time for making heartfelt dua."
      }
      "IFTAR_EXACT" -> when (lang) {
        LANG_BN -> "সূর্যাস্ত হয়েছে ($locTime)। দোয়া পড়ে ইফতার করুন: 'যাহাবায জামাউ ওয়াবতাল্লাতিল উরূক্বু ওয়া ছাবাতাল আজরু ইনশাআল্লাহ'।"
        LANG_AR -> "حان الآن موعد الإفطار ($locTime): ذهب الظمأ وابتلت العروق وثبت الأجر إن شاء الله."
        else -> "The sun has set ($locTime). Break your fast: 'Dhahaba adh-dhama'u wabtallat al-'urooq wa thabat al-ajru insha'Allah'."
      }
      else -> "Time: $locTime"
    }
  }

  fun getEidMubarakNotificationTitle(lang: String): String {
    return when (lang) {
      LANG_BN -> "🌙 ঈদ মোবারক! (Eid Mubarak)"
      LANG_AR -> "🌙 عيد فطر مبارك!"
      else -> "🌙 Eid Mubarak!"
    }
  }

  fun getEidMubarakNotificationBody(lang: String): String {
    return when (lang) {
      LANG_BN -> "তাক্বাব্বালাল্লাহু মিন্না ওয়া মিনকুম। পবিত্র মাহে রমজানের সিয়াম সাধনার পর আপনার ও আপনার পরিবারের জন্য আনন্দময় এবং বরকতময় ঈদের শুভেচ্ছা।"
      LANG_AR -> "تقبل الله منا ومنكم صالح الأعمال والطاعات، وكل عام وأنتم بخير بمناسبة حلول عيد الفطر المبارك."
      else -> "Taqabbal Allahu minna wa minkum. Wishing you and your loved ones a blessed, joyful, and peaceful Eid al-Fitr!"
    }
  }

  /**
   * General UI Strings.
   * NOTE: The app name NEVER changes regardless of language selection.
   */
  fun getString(key: String, lang: String): String {
    return when (key) {
      // App name is invariant
      "app_name" -> "My Own Prayer"

      // Drawer & Navigation
      "side_menu" -> when (lang) { LANG_BN -> "মেনু"; LANG_AR -> "القائمة"; else -> "Menu" }
      "language" -> when (lang) { LANG_BN -> "ভাষা নির্বাচন"; LANG_AR -> "اللغة"; else -> "Language" }
      "theme" -> when (lang) { LANG_BN -> "থিম ও ডিসপ্লে"; LANG_AR -> "المظهر والسمة"; else -> "Theme" }
      "light_theme" -> when (lang) { LANG_BN -> "লাইট থিম"; LANG_AR -> "الوضع الفاتح"; else -> "Light Theme" }
      "dark_theme" -> when (lang) { LANG_BN -> "ডার্ক থিম"; LANG_AR -> "الوضع الداكن"; else -> "Dark Theme" }
      "system_theme" -> when (lang) { LANG_BN -> "সিস্টেম ডিফল্ট"; LANG_AR -> "تلقائي حسب النظام"; else -> "System Default" }
      "refresh_schedule" -> when (lang) { LANG_BN -> "নামাজের সময় রিফ্রেশ"; LANG_AR -> "تحديث مواقيت الصلاة"; else -> "Refresh Schedule" }
      "refreshing" -> when (lang) { LANG_BN -> "রিফ্রেশ হচ্ছে..."; LANG_AR -> "جاري التحديث..."; else -> "Refreshing..." }
      "pull_to_refresh_hint" -> when (lang) { LANG_BN -> "রিফ্রেশ করতে নিচে টানুন"; LANG_AR -> "اسحب للأسفل للتحديث"; else -> "Pull down to refresh schedule" }
      "qibla_compass" -> when (lang) { LANG_BN -> "কিবলা কম্পাস"; LANG_AR -> "بوصلة القبلة"; else -> "Qibla Compass" }
      "select_city" -> when (lang) { LANG_BN -> "শহর নির্বাচন"; LANG_AR -> "تحديد المدينة"; else -> "Select City" }
      "check_updates" -> when (lang) { LANG_BN -> "আপডেট পরীক্ষা করুন"; LANG_AR -> "التحقق من التحديثات"; else -> "Check for Updates" }
      "tasbeeh_counter" -> when (lang) { LANG_BN -> "তাসবিহ কাউন্টার"; LANG_AR -> "المسبحة الإلكترونية"; else -> "Tasbeeh Counter" }
      "tasbeeh_counter_sub" -> when (lang) { LANG_BN -> "ডিজিটাল তাসবিহ ও জিকির গণনা"; LANG_AR -> "عداد الأذكار والتسبيح اليومي"; else -> "Digital Dhikr & Tasbeeh Counter" }
      "add_widget_to_home" -> when (lang) { LANG_BN -> "হোম স্ক্রিনে উইজেট যুক্ত করুন"; LANG_AR -> "إضافة الأداة للشاشة الرئيسية"; else -> "Add Widget to Home Screen" }
      "widget_added_success" -> when (lang) { LANG_BN -> "হোম স্ক্রিনে উইজেট পিন করার অনুরোধ পাঠানো হয়েছে!"; LANG_AR -> "تم إرسال طلب إضافة الأداة للشاشة الرئيسية!"; else -> "Widget pin request sent to Home Screen!" }
      "widget_manual_guide" -> when (lang) { LANG_BN -> "আপনার হোম স্ক্রিনের ফাঁকা জায়গায় লং-প্রেস করে উইজেটটি যুক্ত করুন"; LANG_AR -> "اضغط مطولاً على الشاشة الرئيسية لإضافة الأداة يدوياً"; else -> "Long press on your home screen to add the Tasbeeh widget" }
      "target" -> when (lang) { LANG_BN -> "লক্ষ্য"; LANG_AR -> "الهدف"; else -> "Target" }
      "rounds" -> when (lang) { LANG_BN -> "রাউন্ড"; LANG_AR -> "الجولات"; else -> "Rounds" }
      "total_dhikr" -> when (lang) { LANG_BN -> "সর্বমোট জিকির"; LANG_AR -> "مجموع التسبيحات"; else -> "Total Dhikr" }
      "reset_counter" -> when (lang) { LANG_BN -> "কাউন্টার রিসেট"; LANG_AR -> "إعادة ضبط"; else -> "Reset Counter" }
      "reset_all" -> when (lang) { LANG_BN -> "সব রিসেট"; LANG_AR -> "إعادة ضبط الكل"; else -> "Reset All" }
      "tap_to_count" -> when (lang) { LANG_BN -> "গণনা করতে চাপুন"; LANG_AR -> "اضغط للتسبيح"; else -> "Tap to Count" }
      "vibration" -> when (lang) { LANG_BN -> "ভাইব্রেশন"; LANG_AR -> "الاهتزاز"; else -> "Vibration" }
      "sound" -> when (lang) { LANG_BN -> "সাউন্ড"; LANG_AR -> "الصوت"; else -> "Sound" }
      "select_dhikr" -> when (lang) { LANG_BN -> "জিকির নির্বাচন করুন"; LANG_AR -> "اختر الذكر"; else -> "Select Dhikr" }
      "unlimited" -> when (lang) { LANG_BN -> "আনলিমিটেড"; LANG_AR -> "غير محدود"; else -> "Unlimited" }
      "custom_alert_sound" -> when (lang) { LANG_BN -> "কাস্টম এলার্ট সাউন্ড"; LANG_AR -> "صوت التنبيه المخصص"; else -> "Custom Alert Sound" }
      "custom_alert_sound_sub" -> when (lang) { LANG_BN -> "নোটিফিকেশনের জন্য নিজস্ব MP3 সাউন্ড সেট করুন"; LANG_AR -> "تعيين ملف MP3 مخصص لإشعارات الصلاة"; else -> "Set custom MP3 sound for prayer notifications" }
      "select_mp3_sound" -> when (lang) { LANG_BN -> "MP3 ফাইল নির্বাচন করুন"; LANG_AR -> "اختر ملف MP3"; else -> "Select MP3 File" }
      "preview_sound" -> when (lang) { LANG_BN -> "সাউন্ড শুনুন"; LANG_AR -> "تشغيل الصوت"; else -> "Preview Sound" }
      "stop_sound" -> when (lang) { LANG_BN -> "সাউন্ড বন্ধ করুন"; LANG_AR -> "إيقاف الصوت"; else -> "Stop Sound" }
      "reset_to_default_sound" -> when (lang) { LANG_BN -> "ডিফল্ট সাউন্ডে ফিরে যান"; LANG_AR -> "استعادة الصوت الافتراضي"; else -> "Reset to Default Sound" }
      "system_default_sound" -> when (lang) { LANG_BN -> "সিস্টেম ডিফল্ট সাউন্ড"; LANG_AR -> "نغمة النظام الافتراضية"; else -> "System Default Sound" }
      "sound_set_success" -> when (lang) { LANG_BN -> "কাস্টম এলার্ট সাউন্ড সফলভাবে সেট করা হয়েছে!"; LANG_AR -> "تم تعيين صوت التنبيه بنجاح!"; else -> "Custom alert sound set successfully!" }
      "sound_reset_success" -> when (lang) { LANG_BN -> "সিস্টেম ডিফল্ট সাউন্ডে ফিরে যাওয়া হয়েছে"; LANG_AR -> "تمت استعادة الصوت الافتراضي"; else -> "Reverted to system default sound" }
      "invalid_audio_file" -> when (lang) { LANG_BN -> "অনুগ্রহ করে একটি সঠিক অডিও/MP3 ফাইল নির্বাচন করুন"; LANG_AR -> "يرجى اختيار ملف صوتي صالح"; else -> "Please select a valid audio/MP3 file" }
      "test_notification" -> when (lang) { LANG_BN -> "টেস্ট নোটিফিকেশন"; LANG_AR -> "إشعار تجريبي"; else -> "Test Prayer Notification" }
      "test_notification_sent" -> when (lang) { LANG_BN -> "নোটিফিকেশন পাঠানো হয়েছে"; LANG_AR -> "تم إرسال الإشعار"; else -> "Notification triggered!" }
      "detect_gps" -> when (lang) { LANG_BN -> "জিপিএস অবস্থান সনাক্ত করুন"; LANG_AR -> "تحديد الموقع عبر GPS"; else -> "Detect GPS Location" }
      "detecting_location" -> when (lang) { LANG_BN -> "অবস্থান সনাক্ত করা হচ্ছে..."; LANG_AR -> "جاري تحديد الموقع..."; else -> "Detecting location..." }
      "about_app" -> when (lang) { LANG_BN -> "অ্যাপ সম্পর্কিত"; LANG_AR -> "حول التطبيق"; else -> "About App" }
      "close" -> when (lang) { LANG_BN -> "বন্ধ করুন"; LANG_AR -> "إغلاق"; else -> "Close" }
      "english" -> when (lang) { LANG_BN -> "ইংরেজি (English)"; LANG_AR -> "الإنجليزية (English)"; else -> "English" }
      "bengali" -> when (lang) { LANG_BN -> "বাংলা (Bengali)"; LANG_AR -> "البنغالية (বাংলা)"; else -> "Bengali (বাংলা)" }
      "arabic" -> when (lang) { LANG_BN -> "আরবি (Arabic)"; LANG_AR -> "العربية (Arabic)"; else -> "Arabic (العربية)" }
      "select_language_title" -> when (lang) { LANG_BN -> "অ্যাপের ভাষা নির্বাচন করুন"; LANG_AR -> "اختر لغة التطبيق"; else -> "Select App Language" }
      "language_note" -> when (lang) {
        LANG_BN -> "ভাষা পরিবর্তন করলে অ্যাপ এবং নোটিফিকেশনের ভাষাও পরিবর্তিত হবে।"
        LANG_AR -> "سيؤدي تغيير اللغة أيضًا إلى تحديث لغة الإشعارات والتنبيهات."
        else -> "Changing the language also updates all future prayer notifications."
      }
      "sync_now" -> when (lang) { LANG_BN -> "এখনই সিঙ্ক করুন"; LANG_AR -> "مزامنة الآن"; else -> "Sync Schedule Now" }
      "last_synced" -> when (lang) { LANG_BN -> "সর্বশেষ সিঙ্ক"; LANG_AR -> "آخر مزامنة"; else -> "Last synced" }

      // Hero tags & labels
      "current_prayer_tag" -> when (lang) { LANG_BN -> "চলতি সালাত"; LANG_AR -> "الصلاة الحالية"; else -> "CURRENT PRAYER" }
      "upcoming_prayer_tag" -> when (lang) { LANG_BN -> "পরবর্তী সালাত • শীঘ্রই"; LANG_AR -> "الصلاة القادمة • قريباً"; else -> "UPCOMING PRAYER • SOON" }
      "forbidden_time_tag" -> when (lang) { LANG_BN -> "নিষিদ্ধ সময় • সালাত নিষেধ"; LANG_AR -> "وقت كراهة • محظور"; else -> "FORBIDDEN TIME • PROHIBITED" }
      "prohibited_now_sub" -> when (lang) {
        LANG_BN -> "বর্তমানে সকল প্রকার সালাত আদায় নিষিদ্ধ"
        LANG_AR -> "تُنهى الصلاة في هذا الوقت"
        else -> "Prayers (Fard & Nafl) prohibited now"
      }
      "qibla" -> when (lang) { LANG_BN -> "কিবলা"; LANG_AR -> "القبلة"; else -> "Qibla" }
      "upcoming_prefix" -> when (lang) { LANG_BN -> "পরবর্তী: "; LANG_AR -> "القادمة: "; else -> "Upcoming: " }
      "current_prefix" -> when (lang) { LANG_BN -> "চলতি: "; LANG_AR -> "الحالية: "; else -> "Current: " }
      "in_prefix" -> when (lang) { LANG_BN -> "বাকি "; LANG_AR -> "خلال "; else -> "in " }
      "ending_soon" -> when (lang) { LANG_BN -> "ওয়াক্ত শেষ হতে চলেছে"; LANG_AR -> "ينتهي قريباً"; else -> "Ending soon" }
      "pray_soon" -> when (lang) { LANG_BN -> "দ্রুত আদায় করুন"; LANG_AR -> "صَلِّ قريباً"; else -> "Pray soon" }
      "time_remaining" -> when (lang) { LANG_BN -> "অবশিষ্ট সময়"; LANG_AR -> "الوقت المتبقي"; else -> "Time Remaining" }

      // Sections & Cards
      "obligatory_prayers_title" -> when (lang) { LANG_BN -> "ফরজ সালাতসমূহ"; LANG_AR -> "الصلوات المفروضة"; else -> "Obligatory Prayers (Fard)" }
      "tap_bell_hint" -> when (lang) { LANG_BN -> "অ্যালার্ট সেট করতে ঘণ্টা চাপুন"; LANG_AR -> "اضغط على الجرس لتفعيل التنبيه"; else -> "Tap bell to toggle alert" }
      "solar_transitions_title" -> when (lang) { LANG_BN -> "সূর্যোদয় ও সূর্যাস্ত"; LANG_AR -> "الشروق والغروب"; else -> "Solar Transitions" }
      "solar_transitions_subtitle" -> when (lang) { LANG_BN -> "দিগন্তে জ্যোতির্বিজ্ঞানিক সৌর অবস্থান"; LANG_AR -> "أوقات حركة الشمس في الأفق"; else -> "Astronomical horizon timings" }
      "sun_schedule_badge" -> when (lang) { LANG_BN -> "সৌর সময়সূচি"; LANG_AR -> "مواقيت الشمس"; else -> "Sun Schedule" }
      "sunrise" -> when (lang) { LANG_BN -> "সূর্যোদয়"; LANG_AR -> "الشروق"; else -> "Sunrise" }
      "sunset" -> when (lang) { LANG_BN -> "সূর্যাস্ত"; LANG_AR -> "الغروب"; else -> "Sunset" }
      "voluntary_prayers_title" -> when (lang) { LANG_BN -> "নফল সালাত"; LANG_AR -> "صلاة التطوع والنوافل"; else -> "Voluntary Prayers (Nawafil)" }
      "voluntary_prayers_subtitle" -> when (lang) { LANG_BN -> "ইশরাক, চাশত (দুহা) ও তাহাজ্জুদ"; LANG_AR -> "الإشراق والضحى والتهجد"; else -> "Ishraq, Duha & Tahajjud" }
      "sunnah_badge" -> when (lang) { LANG_BN -> "সুন্নত"; LANG_AR -> "سنة"; else -> "Sunnah" }
      "forbidden_prayers_title" -> when (lang) { LANG_BN -> "সালাত আদায় নিষিদ্ধ সময়"; LANG_AR -> "أوقات كراهة الصلاة"; else -> "Forbidden Prayer Times" }
      "forbidden_prayers_subtitle" -> when (lang) {
        LANG_BN -> "এই ৩টি সৌর সময়ে সকল প্রকার নফল ও সাধারণ সালাত আদায় নিষিদ্ধ:"
        LANG_AR -> "تُكره صلاة النوافل في هذه الأوقات الثلاثة:"
        else -> "Voluntary (Nafl) prayers are strictly prohibited during these 3 solar intervals:"
      }
      "prohibited_now" -> when (lang) { LANG_BN -> "এখন সালাত নিষেধ"; LANG_AR -> "محظورة الآن"; else -> "PROHIBITED NOW" }

      // Badges and status
      "now_badge" -> when (lang) { LANG_BN -> "এখন"; LANG_AR -> "الآن"; else -> "NOW" }
      "next_badge" -> when (lang) { LANG_BN -> "পরবর্তী"; LANG_AR -> "التالي"; else -> "NEXT" }
      "current_badge" -> when (lang) { LANG_BN -> "চলতি"; LANG_AR -> "الحالي"; else -> "CURRENT" }
      "makruh_badge" -> when (lang) { LANG_BN -> "মাকরূহ"; LANG_AR -> "مكروه"; else -> "Makruh" }
      "passed_label" -> when (lang) { LANG_BN -> "ওয়াক্ত অতিক্রান্ত"; LANG_AR -> "مضت"; else -> "Passed" }
      "passed_prefix" -> when (lang) { LANG_BN -> "অতিক্রান্ত • "; LANG_AR -> "مضت • "; else -> "Passed • " }
      "current_prayer_prefix" -> when (lang) { LANG_BN -> "চলতি ওয়াক্ত • "; LANG_AR -> "الوقت الحالي • "; else -> "Current Prayer • " }
      "ends_at_label" -> when (lang) { LANG_BN -> "শেষ:"; LANG_AR -> "ينتهي:"; else -> "Ends:" }
      "ends_at_prefix" -> when (lang) { LANG_BN -> "শেষ • "; LANG_AR -> "ينتهي • "; else -> "Ends • " }
      "isha_makruh_hero" -> when (lang) { LANG_BN -> "পাশাপাশি এশা (মাকরূহ)"; LANG_AR -> "وأيضاً العشاء (مكروه)"; else -> "Also Isha (Makruh)" }
      "isha_makruh_desc" -> when (lang) { LANG_BN -> "তাহাজ্জুদ চলাকালে এশার ওয়াক্ত মাকরূহ কিন্তু ফজর পর্যন্ত আদায় করা যাবে"; LANG_AR -> "وقت العشاء مكروه أثناء التهجد ولكن يصح أداؤها حتى الفجر"; else -> "Isha is valid until Fajr, but delayed past midnight/Tahajjud is Makruh" }

      // City Dialog
      "select_location" -> when (lang) { LANG_BN -> "শহর ও অবস্থান নির্বাচন"; LANG_AR -> "تحديد الموقع والمدينة"; else -> "Select Location" }
      "use_gps" -> when (lang) { LANG_BN -> "ফোনের লোকেশন ব্যবহার করুন (GPS)"; LANG_AR -> "استخدام موقع الهاتف (GPS)"; else -> "Use Phone Location (GPS)" }
      "auto_detect_gps" -> when (lang) { LANG_BN -> "জিপিএস দিয়ে স্বয়ংক্রিয়ভাবে শহর খুঁজুন"; LANG_AR -> "تحديد المدينة بدقة عبر GPS"; else -> "Auto-detect exact city via GPS" }
      "search_cities_placeholder" -> when (lang) { LANG_BN -> "২৭০+ বিশ্ব শহর অনুসন্ধান করুন..."; LANG_AR -> "ابحث بين ٢٧٠+ مدينة..."; else -> "Search 270+ world cities..." }
      "world_cities" -> when (lang) { LANG_BN -> "বিশ্বের শহরসমূহ"; LANG_AR -> "مدن العالم"; else -> "World Cities" }
      "filtered" -> when (lang) { LANG_BN -> "ফিল্টারকৃত"; LANG_AR -> "مصفاة"; else -> "Filtered" }
      "no_cities_found" -> when (lang) { LANG_BN -> "কোনো শহর পাওয়া যায়নি"; LANG_AR -> "لم يتم العثور على مدن"; else -> "No cities found" }

      // Important Special Prayers
      "important_prayers" -> when (lang) { LANG_BN -> "কিছু গুরুত্বপূর্ণ সালাত"; LANG_AR -> "صلوات مهمة ومأثورة"; else -> "Important Special Prayers" }
      "important_prayers_sub" -> when (lang) { LANG_BN -> "ইস্তিখারা, সালাতুল হাজাত ও সালাতুত তাসবীহ"; LANG_AR -> "الاستخارة، صلاة الحاجة، صلاة التسابيح"; else -> "Istikhara, Salatul Hajat & Salatut Tasbih" }

      // Hijri Calendar
      "hijri_calendar" -> when (lang) { LANG_BN -> "হিজরি ক্যালেন্ডার"; LANG_AR -> "التقويم الهجري"; else -> "Hijri Calendar" }
      "hijri_calendar_sub" -> when (lang) { LANG_BN -> "ইসলামিক দিন ও লাইভ ওয়েব সিঙ্ক"; LANG_AR -> "التقويم الإسلامي والأيام المباركة"; else -> "Islamic dates & live web sync" }
      "hijri_calendar_title" -> when (lang) { LANG_BN -> "হিজরি ক্যালেন্ডার"; LANG_AR -> "التقويم الهجري"; else -> "Hijri Calendar" }
      "hijri_calendar_subtitle" -> when (lang) { LANG_BN -> "ইসলামিক ক্যালেন্ডার ও গুরুত্বপূর্ণ দিনসমূহ"; LANG_AR -> "التقويم الإسلامي والمناسبات الدينية"; else -> "Islamic calendar & important dates" }
      "white_days" -> when (lang) { LANG_BN -> "আইয়ামে বিজ (সাদা দিনসমূহ - নফল রোজা)"; LANG_AR -> "الأيام البيض (صيام مستحب)"; else -> "Ayyam al-Beed (White Days Fasting)" }
      "moon_adjustment" -> when (lang) { LANG_BN -> "চাঁদ দেখার সমন্বয় (দিন):"; LANG_AR -> "تعديل رؤية الهلال:"; else -> "Moon Sighting Adj (days):" }
      "synced_live" -> when (lang) { LANG_BN -> "ওয়েব সিঙ্ক"; LANG_AR -> "محدث مباشر"; else -> "Web Sync" }
      "today_button" -> when (lang) { LANG_BN -> "আজ"; LANG_AR -> "اليوم"; else -> "Today" }
      "day_suffix" -> when (lang) { LANG_BN -> "হিজরি"; LANG_AR -> "هـ"; else -> "AH" }
      "gregorian" -> when (lang) { LANG_BN -> "ইংরেজি"; LANG_AR -> "ميلادي"; else -> "Gregorian" }
      "hijri" -> when (lang) { LANG_BN -> "হিজরি"; LANG_AR -> "هجري"; else -> "Hijri" }

      // Qibla Dialog
      "qibla_compass_title" -> when (lang) { LANG_BN -> "কিবলার দিক ও কাবার কম্পাস"; LANG_AR -> "اتجاه القبلة والبوصلة"; else -> "Qibla Direction & Kaaba Compass" }
      "facing_qibla" -> when (lang) { LANG_BN -> "✓ আপনি কাবার মুখোমুখি আছেন!"; LANG_AR -> "✓ أنت تواجه القبلة الآن!"; else -> "✓ You are facing the Kaaba!" }
      "turn_towards_arrow" -> when (lang) { LANG_BN -> "তীর চিহ্নের দিকে মোবাইল ঘুরান"; LANG_AR -> "أدر الهاتف باتجاه السهم"; else -> "Turn towards the green pointer" }
      "kaaba" -> when (lang) { LANG_BN -> "কাবা"; LANG_AR -> "الكعبة"; else -> "KAABA" }
      "heading" -> when (lang) { LANG_BN -> "দিক"; LANG_AR -> "الاتجاه"; else -> "Heading" }
      "qibla_angle" -> when (lang) { LANG_BN -> "কিবলার কোণ"; LANG_AR -> "زاوية القبلة"; else -> "Qibla Angle" }
      "distance_kaaba" -> when (lang) { LANG_BN -> "কাবা থেকে দূরত্ব"; LANG_AR -> "المسافة إلى الكعبة"; else -> "Distance to Kaaba" }
      "from_north" -> when (lang) { LANG_BN -> "উত্তর দিক থেকে"; LANG_AR -> "من الشمال"; else -> "from North" }
      "km" -> when (lang) { LANG_BN -> "কিমি"; LANG_AR -> "كم"; else -> "km" }
      "qibla_instructions" -> when (lang) {
        LANG_BN -> "মোবাইল সোজা ধরে তীর চিহ্নের সাথে সামঞ্জস্য করুন।"
        LANG_AR -> "أمسك هاتفك بشكل أفقي حتى يشير السهم إلى الأعلى."
        else -> "Hold your phone flat and turn until the arrow aligns with the top."
      }

      // Permissions & Updates
      "gps_location_access" -> when (lang) { LANG_BN -> "জিপিএস লোকেশন অনুমতি"; LANG_AR -> "إذن الوصول للموقع (GPS)"; else -> "GPS Location Access" }
      "gps_permission_desc" -> when (lang) {
        LANG_BN -> "স্বয়ংক্রিয়ভাবে শহর সনাক্ত করতে এবং সঠিক নামাজের সময় পেতে লোকেশন অনুমতি দিন।"
        LANG_AR -> "اسمح بالوصول إلى الموقع لتحديد مدينتك وحساب مواقيت الصلاة بدقة."
        else -> "Allow location access to automatically determine your city and compute exact prayer times via GPS."
      }
      "enable_gps_button" -> when (lang) { LANG_BN -> "জিপিএস চালু করুন"; LANG_AR -> "تفعيل موقع GPS"; else -> "Enable GPS Location" }
      "notif_permission_needed" -> when (lang) { LANG_BN -> "নোটিফিকেশন অনুমতি প্রয়োজন"; LANG_AR -> "إذن الإشعارات مطلوب"; else -> "Notification Permission Needed" }
      "notif_permission_desc" -> when (lang) {
        LANG_BN -> "সঠিক সময়ে নামাজের অ্যালার্ট পেতে নোটিফিকেশন অনুমতি প্রদান করুন।"
        LANG_AR -> "اسمح بالإشعارات لتصلك تنبيهات الصلاة بدقة في وقتها."
        else -> "Allow notifications so exact prayer alerts can be delivered on time."
      }
      "grant_permission" -> when (lang) { LANG_BN -> "অনুমতি দিন"; LANG_AR -> "منح الإذن"; else -> "Grant Permission" }
      "update_available_title" -> when (lang) { LANG_BN -> "নতুন আপডেট উপলভ্য"; LANG_AR -> "تحديث جديد متوفر"; else -> "Update Available" }
      "update_available_desc" -> when (lang) {
        LANG_BN -> "GitHub-এ নতুন রিলিজ রয়েছে। আপডেট করতে ট্যাপ করুন।"
        LANG_AR -> "يتوفر إصدار جديد على GitHub. انقر للتحديث والاطلاع على الملاحظات."
        else -> "A new release is available on GitHub. Tap to view notes & install."
      }
      "update_now" -> when (lang) { LANG_BN -> "এখনই আপডেট করুন"; LANG_AR -> "تحديث الآن"; else -> "Update Now" }
      "view_notes" -> when (lang) { LANG_BN -> "রিলিজ নোট"; LANG_AR -> "عرض الملاحظات"; else -> "View Notes" }
      "report_bug" -> when (lang) { LANG_BN -> "বাগ বা সমস্যা রিপোর্ট করুন"; LANG_AR -> "الإبلاغ عن خطأ"; else -> "Report a bug" }
      "report_bug_sub" -> when (lang) { LANG_BN -> "মতামত জানান বা সমস্যা রিপোর্ট করুন"; LANG_AR -> "شاركنا رأيك أو أبلغ عن مشكلة"; else -> "Share feedback or report an issue" }

      // Testing verification section
      "verify_notif_title" -> when (lang) { LANG_BN -> "ব্যাকগ্রাউন্ড-মুক্ত নোটিফিকেশন পরীক্ষা"; LANG_AR -> "اختبار إشعارات توفير البطارية"; else -> "Verify Background-Free Notifications" }
      "verify_notif_desc" -> when (lang) {
        LANG_BN -> "অ্যাপ ব্যাকগ্রাউন্ডে চালু না রেখেও সময়মতো নোটিফিকেশন আসা নিশ্চিত করুন:"
        LANG_AR -> "تأكد من وصول الإشعارات في وقتها دون تشغيل التطبيق في الخلفية:"
        else -> "Verify that notifications trigger on time without requiring the app to run in the background:"
      }
      "instant_alert_btn" -> when (lang) { LANG_BN -> "তাৎক্ষণিক অ্যালার্ট"; LANG_AR -> "تنبيه فوري"; else -> "Instant Alert" }
      "alarm_10s_btn" -> when (lang) { LANG_BN -> "১০ সে. অ্যালার্ম (অ্যাপ বন্ধ করুন)"; LANG_AR -> "منبه ١٠ ثوانٍ (اخرج)"; else -> "10s Alarm (Exit App)" }
      "test_flip_mode" -> when (lang) { LANG_BN -> "'আসন্ন সালাত' মোড টেস্ট"; LANG_AR -> "اختبار وضع اقتراب الصلاة"; else -> "Test 'Approaching Prayer' Flip Mode" }
      "reset_realtime_mode" -> when (lang) { LANG_BN -> "রিয়েল-টাইম মোডে ফিরুন"; LANG_AR -> "العودة للوضع المباشر"; else -> "Reset to Real-time View Mode" }

      // Battery card
      "battery_arch_title" -> when (lang) { LANG_BN -> "ব্যাটারি অপ্টিমাইজেশন প্রযুক্তি"; LANG_AR -> "معمارية توفير البطارية"; else -> "Battery Optimization Architecture" }
      "battery_bullet_1_title" -> when (lang) { LANG_BN -> "অ্যাপ ব্যাকগ্রাউন্ডে চালু থাকে না"; LANG_AR -> "التطبيق لا يعمل في الخلفية"; else -> "App Not Running in Background" }
      "battery_bullet_1_desc" -> when (lang) {
        LANG_BN -> "অ্যাপ বন্ধ করলে কোনো ব্যাকগ্রাউন্ড সার্ভিস চালু থাকে না। ফলে ব্যাটারি খরচ শূন্য।"
        LANG_AR -> "يغلق التطبيق بالكامل بدون خدمات خلفية أو استهلاك للبطارية."
        else -> "The app closes cleanly. Zero background services or persistent wake locks."
      }
      "battery_bullet_2_title" -> when (lang) { LANG_BN -> "অ্যান্ড্রয়েড অ্যালার্ম ম্যানেজার"; LANG_AR -> "منبه النظام الدقيق"; else -> "Android AlarmManager" }
      "battery_bullet_2_desc" -> when (lang) {
        LANG_BN -> "শুধুমাত্র সঠিক সময়ে অ্যান্ড্রয়েড সিস্টেম কর্তৃক অ্যালার্ম ট্রিগার হয়।"
        LANG_AR -> "يستخدم جدولة النظام الدقيقة لإيقاظ التنبيه في وقته بالضبط."
        else -> "Uses exact system-level alarms to wake only at the precise second of prayer."
      }
      "battery_bullet_3_title" -> when (lang) { LANG_BN -> "অফলাইন লোকাল ডাটাবেস"; LANG_AR -> "قاعدة بيانات محلية تعمل بدون إنترنت"; else -> "Offline Local Database" }
      "battery_bullet_3_desc" -> when (lang) {
        LANG_BN -> "নামাজের সময়সূচি লোকাল ডিভাইসে সংরক্ষিত থাকে, ইন্টারনেটের ব্যাটারি অপচয় হয় না।"
        LANG_AR -> "المواقيت مخزنة محلياً لتوفير الاتصال المستمر بالإنترنت."
        else -> "Prayer schedules cached locally in Room for battery-efficient zero-network wakeups."
      }
      "battery_bullet_4_title" -> when (lang) { LANG_BN -> "ইন্টারনেট পেলেই তাৎক্ষণিক আপডেট"; LANG_AR -> "تحديث فوري عند توفر الإنترنت"; else -> "Instant Update on Internet" }
      "battery_bullet_4_desc" -> when (lang) {
        LANG_BN -> "যারা সবসময় অনলাইনে থাকেন না, তাদের সুবিধার্থে ইন্টারনেট সংযোগ পাওয়ার সাথে সাথেই অ্যাপটি স্বয়ংক্রিয়ভাবে ক্লাউড থেকে সর্বশেষ সঠিক সময়সূচী আপডেট করে নেয়।"
        LANG_AR -> "للذين لا يتصلون بالإنترنت دائماً، يتم تحديث مواقيت الصلاة فور اتصال الجهاز بالإنترنت مباشرة وتلقائياً."
        else -> "For users who are not online all the time, timings are refreshed immediately and automatically as soon as internet connectivity is detected."
      }

      // Network & Offline / Online Status
      "internet_restored_updated" -> when (lang) {
        LANG_BN -> "ইন্টারনেট সংযোগ পাওয়া গেছে: নামাজের সময়সূচী তাৎক্ষণিকভাবে ক্লাউড থেকে আপডেট করা হয়েছে!"
        LANG_AR -> "تم الاتصال بالإنترنت: تم تحديث مواقيت الصلاة فوراً من السحابة!"
        else -> "Internet connected: Prayer timings updated immediately from cloud!"
      }
      "offline_badge" -> when (lang) { LANG_BN -> "অফলাইন"; LANG_AR -> "بدون إنترنت"; else -> "Offline" }
      "offline_mode_badge" -> when (lang) { LANG_BN -> "অফলাইন মোড"; LANG_AR -> "وضع بدون إنترنت"; else -> "Offline Mode" }
      "offline_mode_hint" -> when (lang) { LANG_BN -> "ইন্টারনেট পেলে স্বয়ংক্রিয়ভাবে আপডেট হবে"; LANG_AR -> "يتم التحديث تلقائياً فور توفر الإنترنت"; else -> "Auto-updates immediately upon internet connection" }
      "online_mode_badge" -> when (lang) { LANG_BN -> "অনলাইন মোড (ক্লাউড সিঙ্ক)"; LANG_AR -> "متصل بالإنترنت (محدث)"; else -> "Online Mode (Cloud Synced)" }
      "cloud_sync_active" -> when (lang) { LANG_BN -> "ক্লাউড সিঙ্ক সক্রিয়"; LANG_AR -> "محدث عبر السحابة"; else -> "Cloud Synced" }

      // Ramadan Iftar & Suhoor Section
      "ramadan_iftar_suhoor_title" -> when (lang) { LANG_BN -> "পবিত্র মাহে রমজান"; LANG_AR -> "شهر رمضان المبارك"; else -> "Holy Ramadan" }
      "ramadan_today_schedule" -> when (lang) { LANG_BN -> "ইফতার ও সাহরি সময়সূচী"; LANG_AR -> "مواقيت الإفطار والسحور"; else -> "Iftar & Suhoor Schedule" }
      "day_before_ramadan_notice" -> when (lang) {
        LANG_BN -> "আগামীকাল থেকে পবিত্র মাহে রমজান শুরু (ইনশাআল্লাহ)"
        LANG_AR -> "غداً أول أيام شهر رمضان المبارك إن شاء الله"
        else -> "Holy Ramadan begins tomorrow (Insha'Allah)"
      }
      "ramadan_day_prefix" -> when (lang) { LANG_BN -> "রমজান"; LANG_AR -> "رمضان"; else -> "Ramadan" }
      "suhoor_end_label" -> when (lang) { LANG_BN -> "সাহরির শেষ সময়"; LANG_AR -> "نهاية وقت السحور"; else -> "Suhoor Ends" }
      "suhoor_desc" -> when (lang) { LANG_BN -> "ফজরের ওয়াক্ত শুরু (ইমসাক)"; LANG_AR -> "أذان الفجر (الإمساك)"; else -> "Fajr dawn begins (Imsak)" }
      "iftar_label" -> when (lang) { LANG_BN -> "ইফতারের সময়"; LANG_AR -> "موعد الإفطار"; else -> "Iftar Time" }
      "iftar_desc" -> when (lang) { LANG_BN -> "মাগরিবের সূর্যাস্তের সময়"; LANG_AR -> "غروب الشمس وأذان المغرب"; else -> "At Maghrib sunset" }
      "suhoor_countdown_label" -> when (lang) { LANG_BN -> "সাহরি শেষ হতে বাকি"; LANG_AR -> "المتبقي لانتهاء السحور"; else -> "Suhoor ends in" }
      "iftar_countdown_label" -> when (lang) { LANG_BN -> "ইফতার হতে বাকি"; LANG_AR -> "المتبقي لموعد الإفطار"; else -> "Iftar in" }
      "suhoor_time_active" -> when (lang) { LANG_BN -> "সাহরির সময় চলছে"; LANG_AR -> "وقت السحور جارٍ الآن"; else -> "Suhoor Time Active" }
      "fasting_active" -> when (lang) { LANG_BN -> "রোযার সময় চলছে"; LANG_AR -> "الصيام جارٍ الآن"; else -> "Fasting in Progress" }
      "iftar_time_active" -> when (lang) { LANG_BN -> "ইফতারের সময় হয়েছে!"; LANG_AR -> "حان موعد الإفطار!"; else -> "Time for Iftar!" }
      "suhoor_ended_badge" -> when (lang) { LANG_BN -> "সাহরি সমাপ্ত"; LANG_AR -> "انتهى السحور"; else -> "Suhoor Ended" }
      "iftar_completed_badge" -> when (lang) { LANG_BN -> "ইফতার সম্পন্ন"; LANG_AR -> "تم الإفطار"; else -> "Iftar Done" }
      "iftar_dua_title" -> when (lang) { LANG_BN -> "ইফতারের দোয়া"; LANG_AR -> "دعاء الإفطار المأثور"; else -> "Iftar Dua" }
      "iftar_dua_arabic" -> "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ"
      "iftar_dua_meaning" -> when (lang) {
        LANG_BN -> "পিপাসা দূরীভূত হলো, শিরা-উপশিরা সিক্ত হলো এবং ইনশাআল্লাহ সওয়াব নির্ধারিত হলো। (আবু দাউদ ২৩৫৭)"
        LANG_AR -> "ذهب العطش، وابتلت العروق بالماء، وثبت الثواب عند الله تعالى (أبو داود)."
        else -> "The thirst is gone, the veins are moistened, and the reward is confirmed, if Allah wills. (Abu Dawud)"
      }
      "ramadan_notif_hint" -> when (lang) {
        LANG_BN -> "২টি পূর্ব সতর্কবার্তা: সাহরির শেষ সময় আসন্ন (~১৫ মি.) এবং ইফতার আসন্ন (~১৪ মি.)"
        LANG_AR -> "تنبيهان مسبقان: قرب انتهاء السحور (~١٥ د) وقرب الإفطار (~١٤ د)"
        else -> "2 advance alerts: Suhoor ending soon (~15m) & Iftar approaching soon (~14m)"
      }
      "preview_ramadan_toggle" -> when (lang) {
        LANG_BN -> "রমজান মোড প্রিভিউ টেস্ট"
        LANG_AR -> "معاينة وضع رمضان"
        else -> "Preview Ramadan Mode"
      }
      "test_suhoor_notification" -> when (lang) {
        LANG_BN -> "সাহরি টেস্ট অ্যালার্ট"
        LANG_AR -> "تجربة إشعار السحور"
        else -> "Test Suhoor Alert"
      }
      "test_iftar_notification" -> when (lang) {
        LANG_BN -> "ইফতার টেস্ট অ্যালার্ট"
        LANG_AR -> "تجربة إشعار الإفطار"
        else -> "Test Iftar Alert"
      }
      "eid_mubarak_greeting" -> when (lang) {
        LANG_BN -> "🌙 ঈদ মোবারক! তাক্বাব্বালাল্লাহু মিন্না ওয়া মিনকুম।"
        LANG_AR -> "🌙 عيد فطر مبارك! تقبل الله منا ومنكم صالح الأعمال."
        else -> "🌙 Eid Mubarak! Taqabbal Allahu minna wa minkum."
      }

      else -> key
    }
  }
}
