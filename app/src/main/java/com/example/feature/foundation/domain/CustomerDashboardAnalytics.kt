package com.example.feature.foundation.domain

import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentTransaction
import com.example.core.domain.model.DemoPaymentTransactionType
import com.example.core.domain.model.ParkingSpace
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CustomerSummaryMetrics(
  val upcomingCount: Int,
  val activeCount: Int,
  val totalBookings: Int,
  val hasSpendingRecords: Boolean,
  val recentSpendingPkr: Int?,
)

data class CustomerBookingPreviewItem(
  val booking: Booking,
  val spaceTitle: String,
  val areaLabel: String,
  val hasLiveGatePass: Boolean,
)

data class BookingActivityPoint(
  val dayLabel: String,
  val count: Int,
)

enum class CustomerStatusBucket(val label: String) {
  UPCOMING("Upcoming"),
  ACTIVE("Active"),
  COMPLETED("Completed"),
  CLOSED("Cancelled / Closed"),
}

data class BookingStatusSlice(
  val bucket: CustomerStatusBucket,
  val count: Int,
)

data class CustomerSpendingPoint(
  val label: String,
  val amountPkr: Int,
)

data class CustomerDashboardAnalytics(
  val summary: CustomerSummaryMetrics,
  val spotlightBooking: CustomerBookingPreviewItem?,
  val recentBookings: List<CustomerBookingPreviewItem>,
  val activityTrend: List<BookingActivityPoint>,
  val statusDistribution: List<BookingStatusSlice>,
  val spendingTrend: List<CustomerSpendingPoint>,
)

object CustomerDashboardAnalyticsCalculator {
  private val PKT_ZONE: ZoneId = ZoneId.of("Asia/Karachi")
  private val DAY_LABEL_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.US).withZone(PKT_ZONE)

  fun compute(
    bookings: List<Booking>,
    transactions: List<DemoPaymentTransaction>,
    spacesById: Map<String, ParkingSpace>,
  ): CustomerDashboardAnalytics {
    val upcomingBookings =
      bookings.filter {
        it.status == BookingStatus.PENDING_PAYMENT || it.status == BookingStatus.CONFIRMED
      }
    val activeBookings = bookings.filter { it.status == BookingStatus.CHECKED_IN }
    val completedBookings =
      bookings.filter {
        it.status == BookingStatus.CHECKED_OUT || it.status == BookingStatus.COMPLETED
      }
    val closedBookings =
      bookings.filter {
        it.status == BookingStatus.CANCELLED ||
          it.status == BookingStatus.EXPIRED ||
          it.status == BookingStatus.DISPUTED
      }

    val escrowTxs =
      transactions.filter { it.transactionType == DemoPaymentTransactionType.ESCROW_PAYMENT }
    val refundTxs =
      transactions.filter { it.transactionType == DemoPaymentTransactionType.CUSTOMER_REFUND }
    val hasSpending = escrowTxs.isNotEmpty()
    val netSpendingPkr =
      if (hasSpending) {
        (escrowTxs.sumOf { it.amountPkr } - refundTxs.sumOf { it.amountPkr }).coerceAtLeast(0)
      } else {
        null
      }

    val summary =
      CustomerSummaryMetrics(
        upcomingCount = upcomingBookings.size,
        activeCount = activeBookings.size,
        totalBookings = bookings.size,
        hasSpendingRecords = hasSpending,
        recentSpendingPkr = netSpendingPkr,
      )

    fun toPreview(booking: Booking): CustomerBookingPreviewItem {
      val space = spacesById[booking.spaceId]
      return CustomerBookingPreviewItem(
        booking = booking,
        spaceTitle = space?.title ?: "Parking Space (${booking.bookingCode})",
        areaLabel = space?.location?.areaLabel ?: booking.city.displayName,
        hasLiveGatePass = booking.hasActiveGateAccess(),
      )
    }

    val spotlightRaw =
      activeBookings.minByOrNull { it.startEpochMs }
        ?: upcomingBookings
          .sortedWith(
            compareBy<Booking> { it.status != BookingStatus.CONFIRMED }.thenBy { it.startEpochMs }
          )
          .firstOrNull()

    val recentList =
      bookings
        .sortedWith(
          compareByDescending<Booking> { it.createdAtEpochMs }.thenByDescending { it.startEpochMs }
        )
        .take(3)
        .map(::toPreview)

    // Group real bookings chronologically by PKT calendar date (no fabricated zero-fill when empty)
    val activityPoints =
      if (bookings.isEmpty()) {
        emptyList()
      } else {
        bookings
          .groupBy { Instant.ofEpochMilli(it.startEpochMs).atZone(PKT_ZONE).toLocalDate() }
          .toSortedMap()
          .entries
          .toList()
          .takeLast(7)
          .map { (date, list) ->
            BookingActivityPoint(
              dayLabel = DAY_LABEL_FMT.format(date.atStartOfDay(PKT_ZONE).toInstant()),
              count = list.size,
            )
          }
      }

    val statusSlices =
      buildList {
        if (upcomingBookings.isNotEmpty()) {
          add(BookingStatusSlice(CustomerStatusBucket.UPCOMING, upcomingBookings.size))
        }
        if (activeBookings.isNotEmpty()) {
          add(BookingStatusSlice(CustomerStatusBucket.ACTIVE, activeBookings.size))
        }
        if (completedBookings.isNotEmpty()) {
          add(BookingStatusSlice(CustomerStatusBucket.COMPLETED, completedBookings.size))
        }
        if (closedBookings.isNotEmpty()) {
          add(BookingStatusSlice(CustomerStatusBucket.CLOSED, closedBookings.size))
        }
      }

    val spendingPoints =
      if (!hasSpending) {
        emptyList()
      } else {
        escrowTxs
          .groupBy { Instant.ofEpochMilli(it.createdAtEpochMs).atZone(PKT_ZONE).toLocalDate() }
          .toSortedMap()
          .entries
          .toList()
          .takeLast(7)
          .map { (date, dayTxs) ->
            CustomerSpendingPoint(
              label = DAY_LABEL_FMT.format(date.atStartOfDay(PKT_ZONE).toInstant()),
              amountPkr = dayTxs.sumOf { it.amountPkr },
            )
          }
      }

    return CustomerDashboardAnalytics(
      summary = summary,
      spotlightBooking = spotlightRaw?.let(::toPreview),
      recentBookings = recentList,
      activityTrend = activityPoints,
      statusDistribution = statusSlices,
      spendingTrend = spendingPoints,
    )
  }
}
