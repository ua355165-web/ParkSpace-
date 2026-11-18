package com.example.core.domain.model

import com.example.core.config.BookingType
import com.example.core.config.SupportedCity

enum class BookingStatus(val displayName: String) {
  DRAFT("Draft"),
  PENDING_PAYMENT("Pending Payment"),
  CONFIRMED("Confirmed"),
  CHECKED_IN("Checked In"),
  CHECKED_OUT("Checked Out"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled"),
  EXPIRED("Expired"),
  DISPUTED("Disputed"),
}

enum class DemoPaymentStatus(val displayLabel: String) {
  DEMO_PENDING_HOLD("Demo Payment Pending (Simulated)"),
  DEMO_PAID_ESCROW_HOLD("Demo Escrow Paid (Simulated)"),
  DEMO_SETTLED("Demo Settled (Simulated)"),
  DEMO_PARTIAL_REFUND("Demo Partial Refund (Simulated)"),
  DEMO_FULL_REFUND("Demo Full Refund (Simulated)"),
  DEMO_NO_REFUND("Demo Non-Refundable (Simulated)"),
}

data class Booking(
  val id: String,
  val bookingCode: String,
  val spaceId: String,
  val bayId: String,
  val customerId: String,
  val hostId: String,
  val vehicleId: String,
  val city: SupportedCity,
  val bookingType: BookingType,
  val status: BookingStatus,
  val startEpochMs: Long,
  val endEpochMs: Long,
  val baseAmountPkr: Int,
  val serviceFeePkr: Int,
  val totalAmountPkr: Int,
  val gatePinReleased: Boolean,
  val isDemoRecord: Boolean,
  val demoPaymentStatus: DemoPaymentStatus = defaultDemoPaymentStatus(status),
  val demoEntryPin: String? = null,
  val checkedInAtEpochMs: Long? = null,
  val checkedOutAtEpochMs: Long? = null,
  val cancelledAtEpochMs: Long? = null,
  val refundAmountPkr: Int? = null,
  val cancellationPolicySummary: String? = null,
  val createdAtEpochMs: Long = startEpochMs,
  val demoPaymentMethod: DemoPaymentMethod? = null,
) {
  init {
    require(id.isNotBlank()) { "Booking.id must not be blank" }
    require(bookingCode.isNotBlank()) { "Booking.bookingCode must not be blank" }
    require(spaceId.isNotBlank()) { "Booking.spaceId must not be blank" }
    require(bayId.isNotBlank()) { "Booking.bayId must not be blank" }
    require(customerId.isNotBlank()) { "Booking.customerId must not be blank" }
    require(hostId.isNotBlank()) { "Booking.hostId must not be blank" }
    require(vehicleId.isNotBlank()) { "Booking.vehicleId must not be blank" }
    require(endEpochMs > startEpochMs) { "Booking.endEpochMs must be after startEpochMs" }
    require(baseAmountPkr > 0) { "baseAmountPkr must be positive" }
    require(serviceFeePkr >= 0) { "serviceFeePkr cannot be negative" }
    require(totalAmountPkr == baseAmountPkr + serviceFeePkr) {
      "totalAmountPkr must equal baseAmountPkr + serviceFeePkr"
    }
    if (refundAmountPkr != null) {
      require(refundAmountPkr in 0..totalAmountPkr) {
        "refundAmountPkr must be within 0..totalAmountPkr"
      }
    }

    val durationHours = (endEpochMs - startEpochMs) / MILLIS_PER_HOUR
    when (bookingType) {
      BookingType.HOURLY -> {
        require(durationHours in 1L..23L) {
          "Hourly booking duration must be between 1 and 23 hours"
        }
      }
      BookingType.DAILY -> {
        val durationDays = durationHours / 24L
        require(durationHours % 24L == 0L && durationDays in 1L..29L) {
          "Daily booking duration must be 1 to 29 full days"
        }
      }
      BookingType.MONTHLY -> {
        val cycleHours = 30L * 24L
        val cycles = durationHours / cycleHours
        require(durationHours % cycleHours == 0L && cycles in 1L..12L) {
          "Monthly booking duration must be 1 to 12 cycles of 30 days"
        }
      }
    }

    if (gatePinReleased || demoEntryPin != null) {
      require(status in CONFIRMED_OR_ACTIVE_STATUSES) {
        "gatePinReleased cannot be true before booking is confirmed"
      }
    }
  }

  fun hasActiveGateAccess(): Boolean =
    gatePinReleased && status in LIVE_GATE_ACCESS_STATUSES

  companion object {
    const val MILLIS_PER_HOUR: Long = 3_600_000L
    const val MILLIS_PER_DAY: Long = 24L * MILLIS_PER_HOUR
    const val MILLIS_PER_MONTHLY_CYCLE: Long = 30L * MILLIS_PER_DAY

    val LIVE_GATE_ACCESS_STATUSES: Set<BookingStatus> =
      setOf(
        BookingStatus.CONFIRMED,
        BookingStatus.CHECKED_IN,
      )

    val CONFIRMED_OR_ACTIVE_STATUSES: Set<BookingStatus> =
      setOf(
        BookingStatus.CONFIRMED,
        BookingStatus.CHECKED_IN,
        BookingStatus.CHECKED_OUT,
        BookingStatus.COMPLETED,
        BookingStatus.CANCELLED,
        BookingStatus.DISPUTED,
      )

    val CAPACITY_CONSUMING_STATUSES: Set<BookingStatus> =
      setOf(
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.CONFIRMED,
        BookingStatus.CHECKED_IN,
      )

    fun defaultDemoPaymentStatus(status: BookingStatus): DemoPaymentStatus =
      when (status) {
        BookingStatus.DRAFT,
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.EXPIRED -> DemoPaymentStatus.DEMO_PENDING_HOLD
        BookingStatus.CONFIRMED,
        BookingStatus.CHECKED_IN -> DemoPaymentStatus.DEMO_PAID_ESCROW_HOLD
        BookingStatus.CHECKED_OUT,
        BookingStatus.COMPLETED -> DemoPaymentStatus.DEMO_SETTLED
        BookingStatus.CANCELLED -> DemoPaymentStatus.DEMO_NO_REFUND
        BookingStatus.DISPUTED -> DemoPaymentStatus.DEMO_PAID_ESCROW_HOLD
      }
  }
}
