package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PrayerScreen
import com.example.ui.PrayerViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.update.AppUpdateManager
import com.example.util.AlertSoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var viewModelInstance: PrayerViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AlertSoundManager.stopSound()
    enableHighRefreshRate()
    enableEdgeToEdge()

    // Automatically clean up previously downloaded update APKs to reclaim disk space
    CoroutineScope(Dispatchers.IO).launch {
      AppUpdateManager.cleanUpOldApkFiles(applicationContext)
    }

    setContent {
      val viewModel: PrayerViewModel = viewModel()
      viewModelInstance = viewModel
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val isDark = when (uiState.appTheme) {
        "light" -> false
        "dark" -> true
        else -> systemDark
      }

      // Check if launched from Tasbeeh widget
      if (intent?.getStringExtra("SOURCE") == "TASBEEH") {
        viewModel.openTasbeeh()
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
          PrayerScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    AlertSoundManager.stopSound()
    if (intent.getStringExtra("SOURCE") == "TASBEEH") {
      viewModelInstance?.openTasbeeh()
    }
  }

  override fun onResume() {
    super.onResume()
    // Resume any pending install if user was prompted for unknown sources permission
    AppUpdateManager.resumePendingInstallIfAny(this)
  }

  /**
   * Requests the display panel's highest native refresh rate (e.g. 90Hz, 120Hz, 144Hz)
   * on devices with high-refresh screens to ensure buttery-smooth scrolling, animations,
   * and gestures without device-specific 60Hz throttling.
   */
  private fun enableHighRefreshRate() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val currentDisplay = display
        val maxMode = currentDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
        if (maxMode != null) {
          val params = window.attributes
          params.preferredDisplayModeId = maxMode.modeId
          window.attributes = params
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        @Suppress("DEPRECATION")
        val modes = window.windowManager.defaultDisplay.supportedModes
        val maxMode = modes?.maxByOrNull { it.refreshRate }
        if (maxMode != null) {
          val params = window.attributes
          params.preferredDisplayModeId = maxMode.modeId
          window.attributes = params
        }
      }
    } catch (_: Exception) {
      // Graceful fallback on devices that restrict display mode override
    }
  }
}

