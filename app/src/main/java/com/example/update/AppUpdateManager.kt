package com.example.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.util.AppLanguageHelper
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

  private const val PREFS_NAME = "app_update_prefs"
  private const val KEY_PENDING_DOWNLOAD_ID = "pending_update_download_id"
  private const val KEY_PENDING_APK_PATH = "pending_update_apk_path"

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
          404 -> "Repository is private or URL incorrect (HTTP 404)."
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
      if (cleanLocal.contains("beta", ignoreCase = true) && !cleanRemote.contains("beta", ignoreCase = true)) {
        return true
      }
      return false
    } catch (_: Exception) {
      return !cleanRemote.equals(cleanLocal, ignoreCase = true)
    }
  }

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
      .setSmallIcon(R.drawable.ic_notification)
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
   * Starts downloading the new APK file via DownloadManager.
   * Cleans up any previously downloaded leftover APKs before starting.
   */
  fun startApkDownload(context: Context, downloadUrl: String, fileName: String?): Long {
    // 1. Clean up old leftover APKs first
    cleanUpOldApkFiles(context)

    val actualFileName = fileName?.takeIf { it.isNotBlank() } ?: "My.Own.Prayer.apk"
    val uri = Uri.parse(downloadUrl)

    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val targetFile = File(downloadDir, actualFileName)

    // Remove existing file with the same name if present
    if (targetFile.exists()) {
      targetFile.delete()
    }

    val request = DownloadManager.Request(uri).apply {
      setTitle("My Own Prayer Update")
      setDescription("Downloading $actualFileName...")
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, actualFileName)
      setMimeType("application/vnd.android.package-archive")
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = downloadManager.enqueue(request)

    // Save download tracking in SharedPreferences
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putLong(KEY_PENDING_DOWNLOAD_ID, downloadId)
      .putString(KEY_PENDING_APK_PATH, targetFile.absolutePath)
      .apply()

    Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
    return downloadId
  }

  /**
   * Called when DownloadManager completes a download.
   * Immediately pops the Android system installation dialog.
   */
  fun handleDownloadCompleted(context: Context, downloadId: Long) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val savedId = prefs.getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
      val savedPath = prefs.getString(KEY_PENDING_APK_PATH, null)

      val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
      val query = DownloadManager.Query().setFilterById(downloadId)
      val cursor = downloadManager.query(query)

      var apkFile: File? = null

      if (cursor != null && cursor.moveToFirst()) {
        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1

        if (status == DownloadManager.STATUS_SUCCESSFUL) {
          val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
          val localUriStr = if (localUriIdx != -1) cursor.getString(localUriIdx) else null
          if (!localUriStr.isNullOrBlank()) {
            val uri = Uri.parse(localUriStr)
            val path = uri.path
            if (!path.isNullOrBlank()) {
              val f = File(path)
              if (f.exists() && f.length() > 0) {
                apkFile = f
              }
            }
          }
        }
        cursor.close()
      }

      if (apkFile == null && !savedPath.isNullOrBlank()) {
        val fallback = File(savedPath)
        if (fallback.exists() && fallback.length() > 0) {
          apkFile = fallback
        }
      }

      if (apkFile == null) {
        // Try searching in downloads directory
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val candidate = downloadDir?.listFiles()?.firstOrNull { it.extension.equals("apk", ignoreCase = true) }
        if (candidate != null && candidate.exists() && candidate.length() > 0) {
          apkFile = candidate
        }
      }

      if (apkFile != null && apkFile.exists() && apkFile.length() > 0) {
        Log.d(TAG, "Download finished successfully. Launching package installer for: ${apkFile.absolutePath}")
        installApk(context, apkFile)
      } else {
        Log.w(TAG, "Downloaded APK file not found or empty.")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error handling download completion", e)
    }
  }

  /**
   * Installs downloaded APK file using FileProvider and opens the installation dialogue.
   */
  fun installApk(context: Context, apkFile: File) {
    try {
      // Android 8.0+ Check for unknown sources installation permission
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
          // Save path so we can auto-install upon returning
          val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          prefs.edit().putString(KEY_PENDING_APK_PATH, apkFile.absolutePath).apply()

          Toast.makeText(
            context,
            "Please allow installing updates for My Own Prayer",
            Toast.LENGTH_LONG
          ).show()

          val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(settingsIntent)
          return
        }
      }

      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }

      Log.d(TAG, "Opening package installer dialog for ${apkFile.name}...")
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
      try {
        Toast.makeText(context, "Error opening installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
      } catch (_: Exception) {}
    }
  }

  /**
   * Checks if an install was pending due to permission request and triggers it.
   */
  fun resumePendingInstallIfAny(context: Context) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val pendingPath = prefs.getString(KEY_PENDING_APK_PATH, null) ?: return
      val file = File(pendingPath)
      if (file.exists() && file.length() > 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
          prefs.edit().remove(KEY_PENDING_APK_PATH).apply()
          installApk(context, file)
        }
      } else {
        prefs.edit().remove(KEY_PENDING_APK_PATH).apply()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error resuming pending install", e)
    }
  }

  /**
   * Cleans up previously downloaded APK files to free up device space.
   * Matches files such as:
   * - My.Own.Prayer.apk
   * - My.Own.Prayer (1).apk, My.Own.Prayer (2).apk
   * - My-Own-Prayer-update.apk
   * - Any other update APK in the app's download or cache folders.
   */
  fun cleanUpOldApkFiles(context: Context) {
    try {
      val directories = listOfNotNull(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        context.getExternalFilesDir(null),
        context.filesDir,
        context.cacheDir,
        context.externalCacheDir
      )

      // Regex matching standard downloaded APK naming schemes
      val apkPattern = Regex("(?i).*(my[._\\-\\s]*own[._\\-\\s]*prayer|update|app).*\\.apk$")

      var deletedCount = 0
      var freedBytes = 0L

      for (dir in directories) {
        if (!dir.exists() || !dir.isDirectory) continue

        val files = dir.listFiles() ?: continue
        for (file in files) {
          if (file.isFile && (file.extension.equals("apk", ignoreCase = true) || apkPattern.matches(file.name))) {
            val length = file.length()
            val name = file.name
            if (file.delete()) {
              deletedCount++
              freedBytes += length
              Log.d(TAG, "Cleaned up old downloaded APK: $name (${length / 1024} KB freed)")
            }
          }
        }
      }

      // Also clean public Downloads if accessible and created by our package
      try {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null && publicDownloads.exists()) {
          val publicFiles = publicDownloads.listFiles()
          if (publicFiles != null) {
            for (file in publicFiles) {
              if (file.isFile && apkPattern.matches(file.name)) {
                val length = file.length()
                val name = file.name
                if (file.delete()) {
                  deletedCount++
                  freedBytes += length
                  Log.d(TAG, "Cleaned up public downloaded APK: $name")
                }
              }
            }
          }
        }
      } catch (_: Exception) {
        // Handled gracefully on scoped-storage Android versions
      }

      if (deletedCount > 0) {
        val mb = "%.2f".format(freedBytes / (1024.0 * 1024.0))
        Log.i(TAG, "Total old APKs cleaned up: $deletedCount files ($mb MB freed)")
      }

      // Clear pending download IDs from preferences
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().remove(KEY_PENDING_DOWNLOAD_ID).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Error cleaning up old APK files", e)
    }
  }
}
