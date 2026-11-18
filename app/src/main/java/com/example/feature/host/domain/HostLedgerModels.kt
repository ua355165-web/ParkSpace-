package com.example.feature.host.domain

import com.example.core.config.BookingType
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.ParkingSpace

data class HostLedgerTransaction(
  val bookingId: String,
  val bookingCode: String,
  val spaceId: String,
  val spaceTitle: String,
  val bookingType: BookingType,
  val startEpochMs: Long,
  val endEpochMs: Long,
  val totalPaidByCustomerPkr: Int,
  val platformFeePkr: Int,
  val hostNetSharePkr: Int,
  val bookingStatus: BookingStatus,
  val demoPaymentStatus: DemoPaymentStatus,
  val refundAmountPkr: Int?,
  val settlementBadgeLabel: String,
  val settlementTone: StatusTone,
  val recordedAtEpochMs: Long,
)

data class HostDemoLedgerSummary(
  val totalLifetimeGrossPkr: Int,
  val totalPlatformCommissionPkr: Int,
  val totalHostNetEarningsPkr: Int,
  val escrowHeldPkr: Int,
  val settledPkr: Int,
  val totalRefundedPkr: Int,
  val totalBookingsCount: Int,
  val confirmedActiveCount: Int,
  val completedCount: Int,
  val cancelledCount: Int,
  val transactions: List<HostLedgerTransaction>,
)

/**
 * Pure Kotlin Host Demo Ledger Calculator.
 * Strictly derives all platform fee (8%), host net parking revenue, and simulated escrow
 * from real local Demo booking records.
 */
object HostLedgerCalculator {
  const val DEMO_LEDGER_DISCLAIMER =
    "Simulated Demo Ledger • Local Device Only • No Real Bank Transfers"

  fun compute(
    bookings: List<Booking>,
    spaces: List<ParkingSpace>,
  ): HostDemoLedgerSummary {
    val spaceMap = spaces.associateBy { it.id }

    var grossPkr = 0
    var commissionPkr = 0
    var netPkr = 0
    var escrowPkr = 0
    var settledPkr = 0
    var refundedPkr = 0

    var confirmedActive = 0
    var completed = 0
    var cancelled = 0

    val txList = mutableListOf<HostLedgerTransaction>()

    for (b in bookings.sortedByDescending { it.createdAtEpochMs }) {
      val space = spaceMap[b.spaceId]
      val title = space?.title ?: "Listing #${b.spaceId.takeLast(4)}"

      val isCancelled = b.status == BookingStatus.CANCELLED
      val isSettled = b.status == BookingStatus.CHECKED_OUT || b.status == BookingStatus.COMPLETED
      val isEscrow = b.status == BookingStatus.CONFIRMED || b.status == BookingStatus.CHECKED_IN

      val (badgeLabel, tone) = when {
        isCancelled -> {
          cancelled++
          val refund = b.refundAmountPkr ?: b.totalAmountPkr
          refundedPkr += refund
          "Cancelled (Rs. $refund Refunded)" to StatusTone.ERROR
        }
        isSettled -> {
          completed++
          settledPkr += b.baseAmountPkr
          grossPkr += b.totalAmountPkr
          commissionPkr += b.serviceFeePkr
          netPkr += b.baseAmountPkr
          "Demo Settled" to StatusTone.AVAILABLE
        }
        isEscrow -> {
          confirmedActive++
          escrowPkr += b.baseAmountPkr
          grossPkr += b.totalAmountPkr
          commissionPkr += b.serviceFeePkr
          netPkr += b.baseAmountPkr
          "Escrow Held (Simulated)" to StatusTone.PENDING
        }
        b.status == BookingStatus.PENDING_PAYMENT -> {
          "Awaiting Customer Demo Payment" to StatusTone.INFO
        }
        else -> {
          b.status.displayName to StatusTone.INFO
        }
      }

      txList.add(
        HostLedgerTransaction(
          bookingId = b.id,
          bookingCode = b.bookingCode,
          spaceId = b.spaceId,
          spaceTitle = title,
          bookingType = b.bookingType,
          startEpochMs = b.startEpochMs,
          endEpochMs = b.endEpochMs,
          totalPaidByCustomerPkr = b.totalAmountPkr,
          platformFeePkr = b.serviceFeePkr,
          hostNetSharePkr = b.baseAmountPkr,
          bookingStatus = b.status,
          demoPaymentStatus = b.demoPaymentStatus,
          refundAmountPkr = b.refundAmountPkr,
          settlementBadgeLabel = badgeLabel,
          settlementTone = tone,
          recordedAtEpochMs = b.createdAtEpochMs,
        )
      )
    }

    return HostDemoLedgerSummary(
      totalLifetimeGrossPkr = grossPkr,
      totalPlatformCommissionPkr = commissionPkr,
      totalHostNetEarningsPkr = netPkr,
      escrowHeldPkr = escrowPkr,
      settledPkr = settledPkr,
      totalRefundedPkr = refundedPkr,
      totalBookingsCount = bookings.size,
      confirmedActiveCount = confirmedActive,
      completedCount = completed,
      cancelledCount = cancelled,
      transactions = txList,
    )
  }
}
