package com.example.feature.booking

import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleProfile
import com.example.core.domain.model.VehicleType
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.booking.domain.BookingCancellationCalculator
import com.example.feature.booking.domain.BookingPricingEngine
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingEnginesUnitTest {

  private fun sampleSpace(
    availableBays: Int = 2,
    monthlyEnabled: Boolean = true,
    blackoutDates: Set<String> = emptySet(),
  ): ParkingSpace =
    ParkingSpace(
      id = "sp-test",
      hostId = "host-1",
      title = "Demo Gulberg Garage",
      description = "Test covered parking space",
      parkingType = ParkingType.COVERED_GARAGE,
      location =
        ParkingLocation(
          city = SupportedCity.LAHORE,
          areaLabel = "Gulberg Demo",
          publicMaskedLatitude = 31.5112,
          publicMaskedLongitude = 74.3490,
          privateGateSummary = "Gate B Booth 2",
          privateExactLatitude = 31.5121,
          privateExactLongitude = 74.3498,
        ),
      hourlyRatePkr = 160,
      dailyRatePkr = 1100,
      monthlyRatePkr = if (monthlyEnabled) 19500 else null,
      monthlyEnabled = monthlyEnabled,
      totalBays = 2,
      availableBays = availableBays,
      supportedVehicles = setOf(VehicleType.CAR, VehicleType.BIKE),
      amenities = emptySet(),
      schedule =
        AvailabilitySchedule(
          openDays = ParkingDayOfWeek.entries.toSet(),
          openHour = 0,
          closeHour = 23,
          is24Hours = true,
          blackoutDatesIso = blackoutDates,
        ),
      isApproved = true,
      isDemoListing = true,
    )

  private val sampleVehicle =
    VehicleProfile(
      id = "veh-1",
      ownerUserId = "cust-1",
      vehicleType = VehicleType.CAR,
      label = "Demo Sedan",
      plateNumberMasked = "LE-42",
      isDefault = true,
    )

  @Test
  fun `pricing engine computes hourly daily and monthly PKR totals accurately`() {
    val space = sampleSpace()

    val hourly = BookingPricingEngine.calculate(space, BookingType.HOURLY, 3).getOrThrow()
    assertEquals(480, hourly.baseAmountPkr)
    assertEquals(38, hourly.serviceFeePkr)
    assertEquals(518, hourly.totalAmountPkr)

    val daily = BookingPricingEngine.calculate(space, BookingType.DAILY, 2).getOrThrow()
    assertEquals(2200, daily.baseAmountPkr)
    assertEquals(176, daily.serviceFeePkr)
    assertEquals(2376, daily.totalAmountPkr)

    val monthly = BookingPricingEngine.calculate(space, BookingType.MONTHLY, 2).getOrThrow()
    assertEquals(39000, monthly.baseAmountPkr)
    assertEquals(1500, monthly.serviceFeePkr) // Capped at Rs. 1,500
    assertEquals(40500, monthly.totalAmountPkr)

    val disabledMonthly =
      BookingPricingEngine.calculate(sampleSpace(monthlyEnabled = false), BookingType.MONTHLY, 1)
    assertTrue(disabledMonthly.isFailure)
  }

  @Test
  fun `availability validator enforces half-open intervals, capacity limit, and blackouts`() {
    val space = sampleSpace(availableBays = 2)
    val base = ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, BookingAvailabilityValidator.PAKISTAN_ZONE)
      .toInstant()
      .toEpochMilli()
    val now = base - Booking.MILLIS_PER_HOUR

    val existingA =
      Booking(
        id = "b-a",
        bookingCode = "PS-1001",
        spaceId = space.id,
        bayId = "bay-1",
        customerId = "cust-other-1",
        hostId = space.hostId,
        vehicleId = "veh-x1",
        city = space.location.city,
        bookingType = BookingType.HOURLY,
        status = BookingStatus.CONFIRMED,
        startEpochMs = base,
        endEpochMs = base + 2 * Booking.MILLIS_PER_HOUR,
        baseAmountPkr = 320,
        serviceFeePkr = 26,
        totalAmountPkr = 346,
        gatePinReleased = true,
        isDemoRecord = true,
      )
    val existingB =
      existingA.copy(id = "b-b", bookingCode = "PS-1002", bayId = "bay-2", customerId = "cust-other-2")

    // Overlapping all 2 bays at 11:00..13:00 -> rejected
    val overlapBlocked =
      BookingAvailabilityValidator.validate(
        space = space,
        bays = emptyList(),
        existingBookingsForSpace = listOf(existingA, existingB),
        vehicle = sampleVehicle,
        bookingType = BookingType.HOURLY,
        startEpochMs = base + Booking.MILLIS_PER_HOUR,
        endEpochMs = base + 3 * Booking.MILLIS_PER_HOUR,
        nowEpochMs = now,
        customerId = "cust-1",
      )
    assertFalse(overlapBlocked.isAvailable)
    assertEquals(0, overlapBlocked.remainingBaysForWindow)

    // Back-to-back [12:00, 14:00) does NOT overlap [10:00, 12:00) -> allowed!
    val backToBack =
      BookingAvailabilityValidator.validate(
        space = space,
        bays = emptyList(),
        existingBookingsForSpace = listOf(existingA, existingB),
        vehicle = sampleVehicle,
        bookingType = BookingType.HOURLY,
        startEpochMs = base + 2 * Booking.MILLIS_PER_HOUR,
        endEpochMs = base + 4 * Booking.MILLIS_PER_HOUR,
        nowEpochMs = now,
        customerId = "cust-1",
      )
    assertTrue(backToBack.isAvailable)
    assertEquals(2, backToBack.remainingBaysForWindow)

    // Blackout date inside window -> rejected
    val blackoutSpace = sampleSpace(blackoutDates = setOf("2026-11-20"))
    val blackoutCheck =
      BookingAvailabilityValidator.validate(
        space = blackoutSpace,
        bays = emptyList(),
        existingBookingsForSpace = emptyList(),
        vehicle = sampleVehicle,
        bookingType = BookingType.HOURLY,
        startEpochMs = base,
        endEpochMs = base + Booking.MILLIS_PER_HOUR,
        nowEpochMs = now,
        customerId = "cust-1",
      )
    assertFalse(blackoutCheck.isAvailable)
  }

  @Test
  fun `cancellation calculator follows exact BASELINE_LOCK refund policy`() {
    val space = sampleSpace()
    val start = 1_800_000_000_000L

    // Hourly >= 2h -> 100%, < 2h -> 50%, after start -> 0%
    val hourlyBooking =
      Booking(
        id = "b-h",
        bookingCode = "PS-H",
        spaceId = space.id,
        bayId = "bay-1",
        customerId = "cust-1",
        hostId = space.hostId,
        vehicleId = sampleVehicle.id,
        city = SupportedCity.LAHORE,
        bookingType = BookingType.HOURLY,
        status = BookingStatus.CONFIRMED,
        startEpochMs = start,
        endEpochMs = start + 2 * Booking.MILLIS_PER_HOUR,
        baseAmountPkr = 320,
        serviceFeePkr = 26,
        totalAmountPkr = 346,
        gatePinReleased = true,
        isDemoRecord = true,
      )
    assertEquals(
      346,
      BookingCancellationCalculator.calculate(hourlyBooking, space, start - 3 * Booking.MILLIS_PER_HOUR).refundAmountPkr,
    )
    assertEquals(
      173,
      BookingCancellationCalculator.calculate(hourlyBooking, space, start - Booking.MILLIS_PER_HOUR).refundAmountPkr,
    )
    assertEquals(
      0,
      BookingCancellationCalculator.calculate(hourlyBooking, space, start + 60_000L).refundAmountPkr,
    )

    // Daily >= 24h -> 100%; < 24h -> 1 day retained
    val dailyBooking =
      Booking(
        id = "b-d",
        bookingCode = "PS-D",
        spaceId = space.id,
        bayId = "bay-1",
        customerId = "cust-1",
        hostId = space.hostId,
        vehicleId = sampleVehicle.id,
        city = SupportedCity.LAHORE,
        bookingType = BookingType.DAILY,
        status = BookingStatus.CONFIRMED,
        startEpochMs = start,
        endEpochMs = start + 3 * Booking.MILLIS_PER_DAY,
        baseAmountPkr = 3300,
        serviceFeePkr = 264,
        totalAmountPkr = 3564,
        gatePinReleased = true,
        isDemoRecord = true,
      )
    assertEquals(
      3564,
      BookingCancellationCalculator.calculate(dailyBooking, space, start - 25 * Booking.MILLIS_PER_HOUR).refundAmountPkr,
    )
    val dailyLate =
      BookingCancellationCalculator.calculate(dailyBooking, space, start - 6 * Booking.MILLIS_PER_HOUR)
    assertEquals(3564 - (1100 + 264), dailyLate.refundAmountPkr)
    assertEquals(DemoPaymentStatus.DEMO_PARTIAL_REFUND, dailyLate.resultingPaymentStatus)

    // Monthly >= 72h -> 100%; active 2-cycle booking refunds 1 future cycle
    val monthlyBooking =
      Booking(
        id = "b-m",
        bookingCode = "PS-M",
        spaceId = space.id,
        bayId = "bay-1",
        customerId = "cust-1",
        hostId = space.hostId,
        vehicleId = sampleVehicle.id,
        city = SupportedCity.LAHORE,
        bookingType = BookingType.MONTHLY,
        status = BookingStatus.CONFIRMED,
        startEpochMs = start,
        endEpochMs = start + 2 * Booking.MILLIS_PER_MONTHLY_CYCLE,
        baseAmountPkr = 39000,
        serviceFeePkr = 1500,
        totalAmountPkr = 40500,
        gatePinReleased = true,
        isDemoRecord = true,
      )
    assertEquals(
      40500,
      BookingCancellationCalculator.calculate(monthlyBooking, space, start - 80 * Booking.MILLIS_PER_HOUR).refundAmountPkr,
    )
    assertEquals(
      19500,
      BookingCancellationCalculator.calculate(monthlyBooking, space, start + 2 * Booking.MILLIS_PER_DAY).refundAmountPkr,
    )
  }
}
