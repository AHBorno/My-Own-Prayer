package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerType
import com.example.ui.NextPrayerHeroCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        NextPrayerHeroCard(
          currentPrayer = PrayerItem(PrayerType.ASR, "04:15 PM", System.currentTimeMillis() - 3600000L, isPassed = true, isCurrent = true),
          nextPrayer = PrayerItem(PrayerType.MAGHRIB, "06:20 PM", System.currentTimeMillis() + 5400000L),
          isCloseToNextPrayer = false,
          countdownText = "01h 30m 00s",
          cityName = "Makkah, Saudi Arabia"
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
