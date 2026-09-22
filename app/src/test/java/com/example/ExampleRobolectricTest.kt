package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Own Prayer", appName)
  }

  @Test
  fun `forbidden prayer notification toggle persists in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.repository.PrayerRepository(context)

    repo.setForbiddenTimeNotificationEnabled("Sunrise Transition", true)
    assertTrue(repo.getEnabledForbiddenTimes().contains("Sunrise Transition"))

    repo.setForbiddenTimeNotificationEnabled("Sunrise Transition", false)
    assertTrue(!repo.getEnabledForbiddenTimes().contains("Sunrise Transition"))
  }
}
