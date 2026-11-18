package com.example.core.domain

import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainModelsValidationTest {

  @Test(expected = IllegalArgumentException::class)
  fun `parking location rejects non-200m mask radius`() {
    ParkingLocation(
      city = SupportedCity.LAHORE,
      areaLabel = "Gulberg Demo",
      publicMaskedLatitude = 31.5204,
      publicMaskedLongitude = 74.3587,
      maskRadiusMeters = 500,
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `parking location rejects coordinates outside Pakistan`() {
    ParkingLocation(
      city = SupportedCity.LAHORE,
      areaLabel = "Invalid Geo",
      publicMaskedLatitude = 51.5074,
      publicMaskedLongitude = -0.1278,
    )
  }

  @Test
  fun `toPublicMaskedLocation strips exact coordinates and gate summary`() {
    val full =
      ParkingLocation(
        city = SupportedCity.ISLAMABAD,
        areaLabel = "F-7 Demo Area",
        publicMaskedLatitude = 33.7215,
        publicMaskedLongitude = 73.0561,
        privateGateSummary = "Secret Demo Gate 4",
        privateExactLatitude = 33.7221,
        privateExactLongitude = 73.0569,
      )
    val masked = full.toPublicMaskedLocation()
    assertEquals(200, masked.maskRadiusMeters)
    assertNull(masked.privateGateSummary)
    assertNull(masked.privateExactLatitude)
    assertNull(masked.privateExactLongitude)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `monthly enabled space requires positive monthly rate`() {
    ParkingSpace(
      id = "sp-invalid",
      hostId = "host-1",
      title = "Demo Plaza Space",
      description = "Demo description",
      parkingType = ParkingType.COMMERCIAL_PLAZA,
      location =
        ParkingLocation(
          city = SupportedCity.KARACHI,
          areaLabel = "Clifton Demo",
          publicMaskedLatitude = 24.8138,
          publicMaskedLongitude = 67.0299,
        ),
      hourlyRatePkr = 150,
      dailyRatePkr = 900,
      monthlyRatePkr = null,
      monthlyEnabled = true,
      totalBays = 4,
      availableBays = 4,
      supportedVehicles = setOf(VehicleType.CAR),
      amenities = emptySet(),
      schedule =
        AvailabilitySchedule(
          openDays = setOf(ParkingDayOfWeek.MON),
          openHour = 8,
          closeHour = 20,
          is24Hours = false,
        ),
      isApproved = true,
      isDemoListing = true,
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `booking rejects gate pin release when status is pending payment`() {
    val start = 1_760_000_000_000L
    Booking(
      id = "b-1",
      bookingCode = "PS-1",
      spaceId = "sp-1",
      bayId = "bay-1",
      customerId = "cust-1",
      hostId = "host-1",
      vehicleId = "veh-1",
      city = SupportedCity.LAHORE,
      bookingType = BookingType.HOURLY,
      status = BookingStatus.PENDING_PAYMENT,
      startEpochMs = start,
      endEpochMs = start + (2 * Booking.MILLIS_PER_HOUR),
      baseAmountPkr = 300,
      serviceFeePkr = 30,
      totalAmountPkr = 330,
      gatePinReleased = true,
      isDemoRecord = true,
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `hourly booking rejects duration over 23 hours`() {
    val start = 1_760_000_000_000L
    Booking(
      id = "b-2",
      bookingCode = "PS-2",
      spaceId = "sp-1",
      bayId = "bay-1",
      customerId = "cust-1",
      hostId = "host-1",
      vehicleId = "veh-1",
      city = SupportedCity.FAISALABAD,
      bookingType = BookingType.HOURLY,
      status = BookingStatus.CONFIRMED,
      startEpochMs = start,
      endEpochMs = start + (24 * Booking.MILLIS_PER_HOUR),
      baseAmountPkr = 1000,
      serviceFeePkr = 50,
      totalAmountPkr = 1050,
      gatePinReleased = true,
      isDemoRecord = true,
    )
  }

  @Test
  fun `monthly booking accepts valid 30 day cycle`() {
    val start = 1_760_000_000_000L
    val thirtyDaysMs = 30L * 24L * Booking.MILLIS_PER_HOUR
    val booking =
      Booking(
        id = "b-3",
        bookingCode = "PS-3",
        spaceId = "sp-1",
        bayId = "bay-1",
        customerId = "cust-1",
        hostId = "host-1",
        vehicleId = "veh-1",
        city = SupportedCity.KARACHI,
        bookingType = BookingType.MONTHLY,
        status = BookingStatus.CONFIRMED,
        startEpochMs = start,
        endEpochMs = start + thirtyDaysMs,
        baseAmountPkr = 19500,
        serviceFeePkr = 500,
        totalAmountPkr = 20000,
        gatePinReleased = true,
        isDemoRecord = true,
      )
    assertEquals(20000, booking.totalAmountPkr)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `user profile rejects invalid email`() {
    UserProfile(
      id = "u-1",
      fullName = "Test User",
      phoneMasked = "+92 300 0000000",
      emailMasked = "invalid-email",
      role = UserRole.CUSTOMER,
      city = SupportedCity.LAHORE,
      isDemoAccount = true,
      createdAtEpochMs = 1_760_000_000_000L,
    )
  }
}
