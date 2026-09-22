package com.example.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
  val hasUpdate: Boolean,
  val latestVersionName: String,
  val currentVersionName: String,
  val releaseNotes: String,
  val apkDownloadUrl: String?,
  val htmlUrl: String,
  val apkFileName: String?,
  val errorMessage: String? = null
)

object AppUpdateManager {
  private const val TAG = "AppUpdateManager"
  private const val GITHUB_OWNER = "AHBorno"
  private const val GITHUB_REPO = "My-Own-Prayer"
  private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
  private const val ALL_RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=10"

  const val UPDATE_CHANNEL_ID = "app_updates_channel"
  private const val UPDATE_NOTIFICATION_ID = 9001

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  /**
   * Checks GitHub releases for a newer version compared to current BuildConfig.VERSION_NAME.
   * Checks /releases/latest first, and falls back to /releases to support Pre-releases (like 1.1-beta).
   */
  suspend fun checkForUpdates(context: Context): AppUpdateInfo = withContext(Dispatchers.IO) {
    val currentVersion = BuildConfig.VERSION_NAME
    try {
      // First try /releases (which includes Pre-releases and Drafts), then /releases/latest
      var releaseJson: JSONObject? = null
      var httpCode = 0

      val allReleasesRequest = Request.Builder()
        .url(ALL_RELEASES_URL)
        .header("Accept", "application/vnd.github.v3+json")
        .header("User-Agent", "My-Own-Prayer-App")
        .build()

      val allReleasesResp = httpClient.newCall(allReleasesRequest).execute()
      httpCode = allReleasesResp.code
      if (allReleasesResp.isSuccessful) {
        val body = allReleasesResp.body?.string().orEmpty()
        if (body.startsWith("[")) {
          val jsonArray = org.json.JSONArray(body)
          if (jsonArray.length() > 0) {
            releaseJson = jsonArray.getJSONObject(0)
          }
        }
      }

      // If /releases didn't yield a release (e.g. 404), try /releases/latest as fallback
      if (releaseJson == null && httpCode != 404) {
        val latestRequest = Request.Builder()
          .url(LATEST_RELEASE_URL)
          .header("Accept", "application/vnd.github.v3+json")
          .header("User-Agent", "My-Own-Prayer-App")
          .build()
        val latestResp = httpClient.newCall(latestRequest).execute()
        httpCode = latestResp.code
        if (latestResp.isSuccessful) {
          val body = latestResp.body?.string().orEmpty()
          if (body.startsWith("{")) {
            releaseJson = JSONObject(body)
          }
        }
      }

      if (releaseJson == null) {
        val msg = when (httpCode) {
          404 -> "Repository is private or URL incorrect (HTTP 404). If private, make the repo Public on GitHub so the app can read releases."
          403 -> "GitHub API rate limit exceeded (HTTP 403). Try again in a few minutes."
          0 -> "Could not connect to GitHub."
          else -> "GitHub returned HTTP $httpCode."
        }
        Log.w(TAG, msg)
        return@withContext AppUpdateInfo(
          hasUpdate = false,
          latestVersionName = currentVersion,
          currentVersionName = currentVersion,
          releaseNotes = "",
          apkDownloadUrl = null,
          htmlUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases",
          apkFileName = null,
          errorMessage = msg
        )
      }

      val rawTagName = releaseJson.optString("tag_name", "")
      val cleanTagName = rawTagName.trim().removePrefix("v").removePrefix("V")
      val releaseBody = releaseJson.optString("body", "A new version of the app is available.")
      val htmlUrl = releaseJson.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases")

      // Search assets for .apk file
      var apkUrl: String? = null
      var apkName: String? = null
      val assets = releaseJson.optJSONArray("assets")
      if (assets != null) {
        for (i in 0 until assets.length()) {
          val asset = assets.getJSONObject(i)
          val name = asset.optString("name", "")
          if (name.endsWith(".apk", ignoreCase = true)) {
            apkUrl = asset.optString("browser_download_url", null)
            apkName = name
            break
          }
        }
      }

      val isNewer = isVersionNewer(cleanTagName, currentVersion)

      AppUpdateInfo(
        hasUpdate = isNewer,
        latestVersionName = if (cleanTagName.isNotEmpty()) cleanTagName else rawTagName,
        currentVersionName = currentVersion,
        releaseNotes = releaseBody,
        apkDownloadUrl = apkUrl,
        htmlUrl = htmlUrl,
        apkFileName = apkName,
        errorMessage = null
      )
    } catch (e: Exception) {
      Log.w(TAG, "Error checking for updates: ${e.message}")
      AppUpdateInfo(
        hasUpdate = false,
        latestVersionName = currentVersion,
        currentVersionName = currentVersion,
        releaseNotes = "",
        apkDownloadUrl = null,
        htmlUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases",
        apkFileName = null,
        errorMessage = "Error: ${e.localizedMessage ?: e.message}"
      )
    }
  }

  /**
   * Compares versions like "1.0", "1.0.0", "1.0 Beta", "1.1" etc.
   * Returns true if remote is strictly greater or distinct newer release.
   */
  fun isVersionNewer(remote: String, local: String): Boolean {
    if (remote.isBlank()) return false
    val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")
    val cleanLocal = local.trim().removePrefix("v").removePrefix("V")
    if (cleanRemote.equals(cleanLocal, ignoreCase = true)) return false

    try {
      val remoteParts = cleanRemote.split(".", "-", " ")
        .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
      val localParts = cleanLocal.split(".", "-", " ")
        .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

      val maxLen = maxOf(remoteParts.size, localParts.size)
      for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val l = localParts.getOrElse(i) { 0 }
        if (r > l) return true
        if (r < l) return false
      }
      // If numeric prefixes are identical (e.g. "1.0" vs "1.0 Beta"), treat standard release as newer than beta
      if (cleanLocal.contains("beta", ignoreCase = true) && !cleanRemote.contains("beta", ignoreCase = true)) {
        return true
      }
      return false
    } catch (_: Exception) {
      return !cleanRemote.equals(cleanLocal, ignoreCase = true)
    }
  }

  /**
   * Posts an Android notification alerting the user of an available update.
   */
  fun showUpdateNotification(context: Context, updateInfo: AppUpdateInfo) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        UPDATE_CHANNEL_ID,
        "App Updates",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notifies when a new version of the app is available on GitHub"
      }
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("SHOW_UPDATE_DIALOG", true)
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      UPDATE_NOTIFICATION_ID,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("🚀 App Update Available: v${updateInfo.latestVersionName}")
      .setContentText("A new version is ready! Tap to download and update.")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle("🚀 App Update Available: v${updateInfo.latestVersionName}")
          .bigText("Current: v${updateInfo.currentVersionName} ➜ Latest: v${updateInfo.latestVersionName}\n\n${updateInfo.releaseNotes.take(300)}\n\nTap to download and install.")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    try {
      nm.notify(UPDATE_NOTIFICATION_ID, notification)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to post notification: ${e.message}")
    }
  }

  /**
   * Triggers download of the APK using Android's system DownloadManager.
   */
  fun startApkDownload(context: Context, downloadUrl: String, fileName: String?): Long {
    val actualFileName = fileName ?: "My-Own-Prayer-update.apk"
    val uri = Uri.parse(downloadUrl)

    val request = DownloadManager.Request(uri).apply {
      setTitle("Downloading $actualFileName")
      setDescription("Downloading latest app update...")
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, actualFileName)
      setMimeType("application/vnd.android.package-archive")
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return downloadManager.enqueue(request)
  }

  /**
   * Installs downloaded APK file using FileProvider.
   */
  fun installApk(context: Context, apkFile: File) {
    try {
      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer: ${e.message}")
    }
  }
}
