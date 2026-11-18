package com.example.feature.booking.domain

import com.example.core.config.BookingType
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.ParkingSpace

data class CancellationRefundQuote(
  val isEligibleToCancel: Boolean,
  val refundPercentageLabel: String,
  val refundAmountPkr: Int,
  val retainedFeePkr: Int,
  val resultingPaymentStatus: DemoPaymentStatus,
  val policyRuleExplanation: String,
  val disputeEligible: Boolean,
)

/**
 * Implements the exact Demo Mode cancellation policy defined in docs/BASELINE_LOCK.md Section 4:
 * - Hourly: >= 2h before start = 100%; < 2h before start = 50%; after start/no-show = 0% (dispute eligible).
 * - Daily: >= 24h before start = 100%; < 24h before start = 1-day fee retained; mid-stay = unused full days minus 1-day penalty.
 * - Monthly: >= 72h before cycle start = 100%; active cycle = current 30-day cycle non-refundable except host fault/dispute; future pre-paid cycles 100% refundable.
 */
object BookingCancellationCalculator {

  private const val TWO_HOURS_MS = 2L * Booking.MILLIS_PER_HOUR
  private const val TWENTY_FOUR_HOURS_MS = 24L * Booking.MILLIS_PER_HOUR
  private const val SEVENTY_TWO_HOURS_MS = 72L * Booking.MILLIS_PER_HOUR

  fun calculate(
    booking: Booking,
    space: ParkingSpace?,
    nowEpochMs: Long,
  ): CancellationRefundQuote {
    if (booking.status !in setOf(
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.CHECKED_IN,
      )
    ) {
      return CancellationRefundQuote(
        isEligibleToCancel = false,
        refundPercentageLabel = "0%",
        refundAmountPkr = 0,
        retainedFeePkr = 0,
        resultingPaymentStatus = booking.demoPaymentStatus,
        policyRuleExplanation = "Booking in status ${booking.status.displayName} cannot be cancelled.",
        disputeEligible = false,
      )
    }

    // Pending Payment bookings never collected Demo escrow yet.
    if (booking.status == BookingStatus.PENDING_PAYMENT) {
      return CancellationRefundQuote(
        isEligibleToCancel = true,
        refundPercentageLabel = "No charge",
        refundAmountPkr = 0,
        retainedFeePkr = 0,
        resultingPaymentStatus = DemoPaymentStatus.DEMO_PENDING_HOLD,
        policyRuleExplanation =
          "Pending payment booking cancelled before Demo payment hold. Rs. 0 charged.",
        disputeEligible = false,
      )
    }

    val totalPkr = booking.totalAmountPkr
    val msUntilStart = booking.startEpochMs - nowEpochMs

    return when (booking.bookingType) {
      BookingType.HOURLY -> calculateHourly(booking, totalPkr, msUntilStart)
      BookingType.DAILY -> calculateDaily(booking, space, totalPkr, msUntilStart, nowEpochMs)
      BookingType.MONTHLY -> calculateMonthly(booking, space, totalPkr, msUntilStart, nowEpochMs)
    }
  }

  private fun calculateHourly(
    booking: Booking,
    totalPkr: Int,
    msUntilStart: Long,
  ): CancellationRefundQuote =
    when {
      msUntilStart >= TWO_HOURS_MS && booking.status == BookingStatus.CONFIRMED ->
        CancellationRefundQuote(
          isEligibleToCancel = true,
          refundPercentageLabel = "100%",
          refundAmountPkr = totalPkr,
          retainedFeePkr = 0,
          resultingPaymentStatus = DemoPaymentStatus.DEMO_FULL_REFUND,
          policyRuleExplanation =
            "Hourly policy: Cancelled >= 2 hours before start (100% Demo refund).",
          disputeEligible = false,
        )
      msUntilStart > 0L && booking.status == BookingStatus.CONFIRMED -> {
        val refund = (totalPkr + 1) / 2
        val retained = totalPkr - refund
        CancellationRefundQuote(
          isEligibleToCancel = true,
          refundPercentageLabel = "50%",
          refundAmountPkr = refund,
          retainedFeePkr = retained,
          resultingPaymentStatus =
            if (refund > 0) DemoPaymentStatus.DEMO_PARTIAL_REFUND else DemoPaymentStatus.DEMO_NO_REFUND,
          policyRuleExplanation =
            "Hourly policy: Cancelled < 2 hours before start (50% Demo refund, 50% retained).",
          disputeEligible = false,
        )
      }
      else ->
        CancellationRefundQuote(
          isEligibleToCancel = true,
          refundPercentageLabel = "0%",
          refundAmountPkr = 0,
          retainedFeePkr = totalPkr,
          resultingPaymentStatus = DemoPaymentStatus.DEMO_NO_REFUND,
          policyRuleExplanation =
            "Hourly policy: After start time / no-show (0% automatic Demo refund; eligible for dispute review).",
          disputeEligible = true,
        )
    }

  private fun calculateDaily(
    booking: Booking,
    space: ParkingSpace?,
    totalPkr: Int,
    msUntilStart: Long,
    nowEpochMs: Long,
  ): CancellationRefundQuote {
    val totalDays = ((booking.endEpochMs - booking.startEpochMs) / Booking.MILLIS_PER_DAY)
      .toInt()
      .coerceAtLeast(1)
    val oneDayRatePkr =
      space?.dailyRatePkr ?: (booking.baseAmountPkr / totalDays).coerceAtLeast(1)

    return when {
      msUntilStart >= TWENTY_FOUR_HOURS_MS && booking.status == BookingStatus.CONFIRMED ->
        CancellationRefundQuote(
          isEligibleToCancel = true,
          refundPercentageLabel = "100%",
          refundAmountPkr = totalPkr,
          retainedFeePkr = 0,
          resultingPaymentStatus = DemoPaymentStatus.DEMO_FULL_REFUND,
          policyRuleExplanation =
            "Daily policy: Cancelled >= 24 hours before start (100% Demo refund).",
          disputeEligible = false,
        )
      msUntilStart > 0L && booking.status == BookingStatus.CONFIRMED -> {
        val retained = minOf(totalPkr, oneDayRatePkr + booking.serviceFeePkr)
        val refund = (totalPkr - retained).coerceAtLeast(0)
        val pct = ((refund * 100L) / totalPkr.coerceAtLeast(1)).toInt()
        CancellationRefundQuote(
          isEligibleToCancel = true,
          refundPercentageLabel = "$pct%",
          refundAmountPkr = refund,
          retainedFeePkr = totalPkr - refund,
          resultingPaymentStatus =
            if (refund > 0) DemoPaymentStatus.DEMO_PARTIAL_REFUND else DemoPaymentStatus.DEMO_NO_REFUND,
          policyRuleExplanation =
            "Daily policy: Cancelled < 24 hours before start (1-day fee retained).",
          disputeEligible = false,
        )
      }
      else -> {
        val remainingMs = (booking.endEpochMs - nowEpochMs).coerceAtLeast(0L)
        val unusedFullDays = (remainingMs / Booking.MILLIS_PER_DAY).toInt()
        val refundableDays = (unusedFullDays - 1).coerceAtLeast(0)
        val refund = (refundableDays * oneDayRatePkr).coerceIn(0, totalPkr)
        val pct = ((refund * 100L) / totalPkr.coerceAtLeast(1)).toInt()
        CancellationRefundQuote(
          isEligibleToCancel = true,
          refundPercentageLabel = "$pct%",
          refundAmountPkr = refund,
          retainedFeePkr = totalPkr - refund,
          resultingPaymentStatus =
            if (refund > 0) DemoPaymentStatus.DEMO_PARTIAL_REFUND else DemoPaymentStatus.DEMO_NO_REFUND,
          policyRuleExplanation =
            "Daily policy: Mid-stay cancellation refunds unused full days minus a 1-day penalty.",
          disputeEligible = refund == 0,
        )
      }
    }
  }

  private fun calculateMonthly(
    booking: Booking,
    space: ParkingSpace?,
    totalPkr: Int,
    msUntilStart: Long,
    nowEpochMs: Long,
  ): CancellationRefundQuote {
    if (msUntilStart >= SEVENTY_TWO_HOURS_MS && booking.status == BookingStatus.CONFIRMED) {
      return CancellationRefundQuote(
        isEligibleToCancel = true,
        refundPercentageLabel = "100%",
        refundAmountPkr = totalPkr,
        retainedFeePkr = 0,
        resultingPaymentStatus = DemoPaymentStatus.DEMO_FULL_REFUND,
        policyRuleExplanation =
          "Monthly policy: Cancelled >= 72 hours before cycle start (100% Demo refund).",
        disputeEligible = false,
      )
    }

    val totalCycles =
      ((booking.endEpochMs - booking.startEpochMs) / Booking.MILLIS_PER_MONTHLY_CYCLE)
        .toInt()
        .coerceAtLeast(1)
    val perCycleRate =
      space?.monthlyRatePkr ?: (booking.baseAmountPkr / totalCycles).coerceAtLeast(1)
    val elapsedCycles =
      if (nowEpochMs < booking.startEpochMs) {
        1 // Current first cycle within <72h lock window
      } else {
        (((nowEpochMs - booking.startEpochMs) / Booking.MILLIS_PER_MONTHLY_CYCLE).toInt() + 1)
          .coerceAtMost(totalCycles)
      }
    val futureCycles = (totalCycles - elapsedCycles).coerceAtLeast(0)
    val refund = (futureCycles * perCycleRate).coerceIn(0, totalPkr)
    val pct = ((refund * 100L) / totalPkr.coerceAtLeast(1)).toInt()

    return CancellationRefundQuote(
      isEligibleToCancel = true,
      refundPercentageLabel = "$pct%",
      refundAmountPkr = refund,
      retainedFeePkr = totalPkr - refund,
      resultingPaymentStatus =
        if (refund > 0) DemoPaymentStatus.DEMO_PARTIAL_REFUND else DemoPaymentStatus.DEMO_NO_REFUND,
      policyRuleExplanation =
        if (futureCycles > 0) {
          "Monthly policy: Current 30-day cycle non-refundable; $futureCycles future cycle(s) 100% Demo refunded."
        } else {
          "Monthly policy: Active / <72h 30-day cycle is non-refundable except verified host fault or dispute."
        },
      disputeEligible = refund == 0,
    )
  }
}
