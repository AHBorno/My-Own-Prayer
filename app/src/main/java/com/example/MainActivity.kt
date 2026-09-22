package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PrayerScreen
import com.example.ui.PrayerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableHighRefreshRate()
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val viewModel: PrayerViewModel = viewModel()
          PrayerScreen(viewModel = viewModel)
        }
      }
    }
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

