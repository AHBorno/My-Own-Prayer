package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.util.AppLanguageHelper
import com.example.util.TasbeehPreferences

class TasbeehWidgetProvider : AppWidgetProvider() {

  companion object {
    const val ACTION_INCREMENT = "com.example.widget.TASBEEH_INCREMENT"
    const val ACTION_RESET = "com.example.widget.TASBEEH_RESET"
    const val ACTION_NEXT_DHIKR = "com.example.widget.TASBEEH_NEXT_DHIKR"
    const val ACTION_UPDATE_WIDGET = "com.example.widget.TASBEEH_UPDATE_ALL"

    fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
      val thisWidget = ComponentName(context, TasbeehWidgetProvider::class.java)
      val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
      if (allWidgetIds != null && allWidgetIds.isNotEmpty()) {
        for (widgetId in allWidgetIds) {
          updateAppWidget(context, appWidgetManager, widgetId)
        }
      }
    }

    fun updateAppWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetId: Int
    ) {
      val views = RemoteViews(context.packageName, R.layout.widget_tasbeeh)

      val count = TasbeehPreferences.getCount(context)
      val rounds = TasbeehPreferences.getRoundCount(context)
      val target = TasbeehPreferences.getTargetCount(context)
      val dhikr = TasbeehPreferences.getCurrentDhikr(context)
      val lang = AppLanguageHelper.getSavedLanguage(context)

      val targetStr = if (target > 0) "$target" else "∞"
      val titleStr = when (lang) {
        "bn" -> "তাসবিহ"
        "ar" -> "مسبحة"
        else -> "Tasbeeh"
      }
      val transliteration = when (lang) {
        "bn" -> dhikr.meaningBn
        "ar" -> dhikr.meaningAr
        else -> "${dhikr.transliteration} • ${dhikr.meaningEn}"
      }

      views.setTextViewText(R.id.widget_title, titleStr)
      views.setTextViewText(R.id.widget_dhikr_arabic, dhikr.arabic)
      views.setTextViewText(R.id.widget_dhikr_transliteration, transliteration)
      views.setTextViewText(R.id.widget_count_text, count.toString())

      val subtext = when (lang) {
        "bn" -> "লক্ষ্য: $targetStr • রাউন্ড: $rounds"
        "ar" -> "الهدف: $targetStr • الجولة: $rounds"
        else -> "Target: $targetStr • R $rounds"
      }
      views.setTextViewText(R.id.widget_subtext, subtext)

      // 1. Increment click pending intent
      val incIntent = Intent(context, TasbeehWidgetProvider::class.java).apply {
        action = ACTION_INCREMENT
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      val incPendingIntent = PendingIntent.getBroadcast(
        context,
        1001,
        incIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.btn_widget_increment, incPendingIntent)

      // 2. Reset click pending intent
      val resetIntent = Intent(context, TasbeehWidgetProvider::class.java).apply {
        action = ACTION_RESET
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      val resetPendingIntent = PendingIntent.getBroadcast(
        context,
        1002,
        resetIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.btn_widget_reset, resetPendingIntent)

      // 3. Next Dhikr click pending intent
      val swapIntent = Intent(context, TasbeehWidgetProvider::class.java).apply {
        action = ACTION_NEXT_DHIKR
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      val swapPendingIntent = PendingIntent.getBroadcast(
        context,
        1003,
        swapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.btn_widget_swap_dhikr, swapPendingIntent)

      // 4. Open App click pending intent
      val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra("SOURCE", "TASBEEH")
      }
      val openAppPendingIntent = PendingIntent.getActivity(
        context,
        1004,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_dhikr_container, openAppPendingIntent)
      views.setOnClickPendingIntent(R.id.widget_count_container, openAppPendingIntent)
      views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    for (appWidgetId in appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId)
    }
  }

  override fun onReceive(context: Context, intent: Intent?) {
    super.onReceive(context, intent)
    if (intent == null) return

    when (intent.action) {
      ACTION_INCREMENT -> {
        TasbeehPreferences.increment(context)
        updateAllWidgets(context)
      }
      ACTION_RESET -> {
        TasbeehPreferences.reset(context, resetRounds = false)
        updateAllWidgets(context)
      }
      ACTION_NEXT_DHIKR -> {
        TasbeehPreferences.nextDhikr(context)
        updateAllWidgets(context)
      }
      ACTION_UPDATE_WIDGET,
      AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
        updateAllWidgets(context)
      }
    }
  }
}
