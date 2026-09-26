package com.example.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.update.AppUpdateManager

class ApkDownloadReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "ApkDownloadReceiver"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
      val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
      Log.d(TAG, "Download completed for downloadId: $downloadId")
      if (downloadId != -1L) {
        AppUpdateManager.handleDownloadCompleted(context, downloadId)
      }
    }
  }
}
