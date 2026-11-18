package com.example.feature.booking.domain

import com.example.core.config.BookingType
import com.example.core.domain.model.Booking
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.ParkingSpace

data class BookingPriceBreakdown(
  val bookingType: BookingType,
  val units: Int,
  val unitRatePkr: Int,
  val baseAmountPkr: Int,
  val serviceFeePkr: Int,
  val totalAmountPkr: Int,
  val durationMillis: Long,
  val unitSummary: String,
  val feePolicySummary: String,
)

/**
 * Pure Kotlin PKR pricing engine for Demo Mode.
 * Uses Long intermediate arithmetic to prevent integer overflow across multi-cycle bookings.
 */
object BookingPricingEngine {

  private const val PLATFORM_FEE_BPS_DENOMINATOR = 100L

  fun calculate(
    space: ParkingSpace,
    bookingType: BookingType,
    units: Int,
    commissionRule: DemoCommissionRule = DemoCommissionRule.DEFAULT,
  ): Result<BookingPriceBreakdown> = runCatching {
    val unitRateLong: Long
    val durationMillis: Long
    val unitSummary: String

    when (bookingType) {
      BookingType.HOURLY -> {
        require(units in 1..23) { "Hourly bookings must be between 1 and 23 hours." }
        unitRateLong = space.hourlyRatePkr.toLong()
        durationMillis = units.toLong() * Booking.MILLIS_PER_HOUR
        unitSummary = if (units == 1) "1 hour" else "$units hours"
      }
      BookingType.DAILY -> {
        require(units in 1..29) { "Daily bookings must be between 1 and 29 days." }
        unitRateLong = space.dailyRatePkr.toLong()
        durationMillis = units.toLong() * Booking.MILLIS_PER_DAY
        unitSummary = if (units == 1) "1 day (24 hrs)" else "$units days"
      }
      BookingType.MONTHLY -> {
        require(units in 1..12) { "Monthly bookings must be between 1 and 12 cycles (30 days each)." }
        require(space.monthlyEnabled && space.monthlyRatePkr != null && space.monthlyRatePkr > 0) {
          "Monthly plan is not enabled for ${space.title}."
        }
        unitRateLong = space.monthlyRatePkr.toLong()
        durationMillis = units.toLong() * Booking.MILLIS_PER_MONTHLY_CYCLE
        unitSummary = if (units == 1) "1 × 30-day cycle" else "$units × 30-day cycles"
      }
    }

    require(unitRateLong > 0L) { "Rate must be positive." }
    val baseLong = unitRateLong * units.toLong()
    require(baseLong in 1L..Int.MAX_VALUE.toLong()) { "Calculated base amount out of range." }

    val percentLong = commissionRule.commissionPercent.toLong()
    val minFeeLong = commissionRule.minFeePkr.toLong()
    val maxFeeLong = commissionRule.maxFeePkr.toLong().coerceAtLeast(minFeeLong)

    val rawFeeLong =
      (baseLong * percentLong + (PLATFORM_FEE_BPS_DENOMINATOR / 2L)) /
        PLATFORM_FEE_BPS_DENOMINATOR
    val serviceFeeLong = rawFeeLong.coerceIn(minFeeLong, maxFeeLong)
    val totalLong = baseLong + serviceFeeLong
    require(totalLong in 1L..Int.MAX_VALUE.toLong()) { "Total payable PKR out of range." }

    BookingPriceBreakdown(
      bookingType = bookingType,
      units = units,
      unitRatePkr = unitRateLong.toInt(),
      baseAmountPkr = baseLong.toInt(),
      serviceFeePkr = serviceFeeLong.toInt(),
      totalAmountPkr = totalLong.toInt(),
      durationMillis = durationMillis,
      unitSummary = unitSummary,
      feePolicySummary =
        "Demo platform fee (${commissionRule.commissionPercent}%, min Rs. ${commissionRule.minFeePkr}, cap Rs. ${commissionRule.maxFeePkr})",
    )
  }
}
