package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AlertSoundManager {
  private const val TAG = "AlertSoundManager"
  private const val PREFS_NAME = "alert_sound_prefs"
  private const val KEY_CUSTOM_SOUND_ENABLED = "custom_sound_enabled"
  private const val KEY_CUSTOM_SOUND_NAME = "custom_sound_name"
  private const val CUSTOM_SOUND_FILE_NAME = "custom_alert_sound.mp3"

  private var previewMediaPlayer: MediaPlayer? = null

  /**
   * Returns whether a custom alert MP3 is enabled.
   */
  fun isCustomSoundEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val isEnabled = prefs.getBoolean(KEY_CUSTOM_SOUND_ENABLED, false)
    val file = File(context.filesDir, CUSTOM_SOUND_FILE_NAME)
    return isEnabled && file.exists() && file.length() > 0
  }

  /**
   * Returns the user-friendly file name of the currently selected custom alert sound,
   * or null if default sound is used.
   */
  fun getCustomSoundName(context: Context): String? {
    if (!isCustomSoundEnabled(context)) return null
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_CUSTOM_SOUND_NAME, "custom_alert.mp3")
  }

  /**
   * Imports an MP3/audio file from the provided content Uri into the app's internal storage
   * for permanent and offline access.
   */
  fun setCustomSoundFromUri(context: Context, uri: Uri): Result<String> {
    return try {
      var displayName = "custom_alert.mp3"
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            val retrievedName = cursor.getString(nameIndex)
            if (!retrievedName.isNullOrBlank()) {
              displayName = retrievedName
            }
          }
        }
      }

      val targetFile = File(context.filesDir, CUSTOM_SOUND_FILE_NAME)
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(targetFile).use { outputStream ->
          inputStream.copyTo(outputStream)
        }
      } ?: return Result.failure(Exception("Failed to open audio file stream"))

      if (!targetFile.exists() || targetFile.length() == 0L) {
        return Result.failure(Exception("Audio file is empty"))
      }

      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putBoolean(KEY_CUSTOM_SOUND_ENABLED, true)
        .putString(KEY_CUSTOM_SOUND_NAME, displayName)
        .apply()

      Log.d(TAG, "Saved custom alert sound: $displayName (${targetFile.length()} bytes)")
      Result.success(displayName)
    } catch (e: Exception) {
      Log.e(TAG, "Error importing custom sound file", e)
      Result.failure(e)
    }
  }

  /**
   * Clears the custom alert sound and reverts back to system default notification tone.
   */
  fun resetToDefaultSound(context: Context) {
    stopSound()
    try {
      val targetFile = File(context.filesDir, CUSTOM_SOUND_FILE_NAME)
      if (targetFile.exists()) {
        targetFile.delete()
      }
    } catch (_: Exception) {}

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_CUSTOM_SOUND_ENABLED, false)
      .remove(KEY_CUSTOM_SOUND_NAME)
      .apply()
  }

  /**
   * Plays the alert sound (custom MP3 if enabled, or system default ringtone).
   * Used for both preview and actual notification arrival.
   */
  @Synchronized
  fun playAlertSound(context: Context, onCompletion: (() -> Unit)? = null) {
    stopSound()
    try {
      val file = File(context.filesDir, CUSTOM_SOUND_FILE_NAME)
      if (isCustomSoundEnabled(context) && file.exists() && file.length() > 0) {
        val player = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .setUsage(AudioAttributes.USAGE_NOTIFICATION)
              .build()
          )
          setDataSource(file.absolutePath)
          prepare()
          setOnCompletionListener {
            try {
              it.release()
            } catch (_: Exception) {}
            if (previewMediaPlayer === it) {
              previewMediaPlayer = null
            }
            onCompletion?.invoke()
          }
          start()
        }
        previewMediaPlayer = player
      } else {
        // Fallback to default notification sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val player = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .setUsage(AudioAttributes.USAGE_NOTIFICATION)
              .build()
          )
          setDataSource(context, defaultSoundUri)
          prepare()
          setOnCompletionListener {
            try {
              it.release()
            } catch (_: Exception) {}
            if (previewMediaPlayer === it) {
              previewMediaPlayer = null
            }
            onCompletion?.invoke()
          }
          start()
        }
        previewMediaPlayer = player
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error playing alert sound", e)
      onCompletion?.invoke()
    }
  }

  /**
   * Stops any currently playing preview or notification sound.
   */
  @Synchronized
  fun stopSound() {
    try {
      previewMediaPlayer?.let { player ->
        if (player.isPlaying) {
          player.stop()
        }
        player.release()
      }
    } catch (_: Exception) {}
    previewMediaPlayer = null
  }

  /**
   * Returns whether a sound is currently playing.
   */
  fun isPlaying(): Boolean {
    return try {
      previewMediaPlayer?.isPlaying == true
    } catch (_: Exception) {
      false
    }
  }
}
