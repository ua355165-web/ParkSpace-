package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.AppEnvironment
import com.example.core.config.AppMode
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ParkSpace", appName)
  }

  @Test
  fun `verify initial foundation constraints`() {
    assertEquals(AppMode.DEMO, AppEnvironment.currentMode)
    assertEquals("pk.parkspace.marketplace", AppEnvironment.APPLICATION_ID)
    assertEquals(200, AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS)
    assertEquals(
      listOf(BookingType.HOURLY, BookingType.DAILY, BookingType.MONTHLY),
      BookingType.entries,
    )
    assertEquals(
      listOf("Lahore", "Islamabad", "Faisalabad", "Karachi"),
      SupportedCity.entries.map { it.displayName },
    )
    assertFalse(UserRole.SUPER_ADMIN.allowsPublicRegistration)
  }
}
