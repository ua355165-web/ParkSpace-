package com.example.feature.host.domain

import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.ParkingSpace
import com.example.feature.foundation.domain.BookingActivityPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HostSummaryMetrics(
  val totalListingsCount: Int,
  val activeListingsCount: Int,
  val totalBaysCount: Int,
  val operationalBaysCount: Int,
  val upcomingBookingsCount: Int,
  val activeBookingsCount: Int,
  val completedBookingsCount: Int,
  val totalBookingsCount: Int,
  val hasEarningsRecords: Boolean,
  val netEarningsPkr: Int?,
  val escrowHeldPkr: Int?,
  val settledEarningsPkr: Int?,
)

enum class HostStatusBucket(val label: String) {
  UPCOMING("Upcoming"),
  ACTIVE("Checked In"),
  COMPLETED("Completed"),
  CLOSED("Cancelled / Closed"),
}

data class HostBookingStatusSlice(
  val bucket: HostStatusBucket,
  val count: Int,
)

data class HostEarningsPoint(
  val label: String,
  val netAmountPkr: Int,
)

data class HostRecentActivityItem(
  val booking: Booking,
  val spaceTitle: String,
  val areaLabel: String,
)

data class HostDashboardAnalytics(
  val summary: HostSummaryMetrics,
  val activityTrend: List<BookingActivityPoint>,
  val statusDistribution: List<HostBookingStatusSlice>,
  val earningsTrend: List<HostEarningsPoint>,
  val recentActivity: List<HostRecentActivityItem>,
)

object HostDashboardAnalyticsCalculator {
  private val PKT_ZONE: ZoneId = ZoneId.of("Asia/Karachi")
  private val DAY_LABEL_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.US).withZone(PKT_ZONE)

  fun compute(
    spaces: List<ParkingSpace>,
    bookings: List<Booking>,
    ledger: HostDemoLedgerSummary?,
  ): HostDashboardAnalytics {
    val spacesById = spaces.associateBy { it.id }

    val activeListingsCount = spaces.count { it.isApproved }
    val totalBaysCount = spaces.sumOf { it.totalBays }
    val operationalBaysCount = spaces.sumOf { it.availableBays }

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

    val revenueTransactions =
      ledger
        ?.transactions
        ?.filter { tx ->
          tx.bookingStatus == BookingStatus.CONFIRMED ||
            tx.bookingStatus == BookingStatus.CHECKED_IN ||
            tx.bookingStatus == BookingStatus.CHECKED_OUT ||
            tx.bookingStatus == BookingStatus.COMPLETED
        }
        .orEmpty()

    val hasEarningsRecords = revenueTransactions.isNotEmpty()

    val summary =
      HostSummaryMetrics(
        totalListingsCount = spaces.size,
        activeListingsCount = activeListingsCount,
        totalBaysCount = totalBaysCount,
        operationalBaysCount = operationalBaysCount,
        upcomingBookingsCount = upcomingBookings.size,
        activeBookingsCount = activeBookings.size,
        completedBookingsCount = completedBookings.size,
        totalBookingsCount = bookings.size,
        hasEarningsRecords = hasEarningsRecords,
        netEarningsPkr = if (hasEarningsRecords) ledger?.totalHostNetEarningsPkr else null,
        escrowHeldPkr = if (hasEarningsRecords) ledger?.escrowHeldPkr else null,
        settledEarningsPkr = if (hasEarningsRecords) ledger?.settledPkr else null,
      )

    val activityTrend =
      if (bookings.isEmpty()) {
        emptyList()
      } else {
        bookings
          .groupBy { Instant.ofEpochMilli(it.startEpochMs).atZone(PKT_ZONE).toLocalDate() }
          .toSortedMap()
          .entries
          .toList()
          .takeLast(7)
          .map { (date, dayBookings) ->
            BookingActivityPoint(
              dayLabel = DAY_LABEL_FMT.format(date.atStartOfDay(PKT_ZONE).toInstant()),
              count = dayBookings.size,
            )
          }
      }

    val statusDistribution =
      buildList {
        if (upcomingBookings.isNotEmpty()) {
          add(HostBookingStatusSlice(HostStatusBucket.UPCOMING, upcomingBookings.size))
        }
        if (activeBookings.isNotEmpty()) {
          add(HostBookingStatusSlice(HostStatusBucket.ACTIVE, activeBookings.size))
        }
        if (completedBookings.isNotEmpty()) {
          add(HostBookingStatusSlice(HostStatusBucket.COMPLETED, completedBookings.size))
        }
        if (closedBookings.isNotEmpty()) {
          add(HostBookingStatusSlice(HostStatusBucket.CLOSED, closedBookings.size))
        }
      }

    val earningsTrend =
      if (!hasEarningsRecords) {
        emptyList()
      } else {
        revenueTransactions
          .groupBy { Instant.ofEpochMilli(it.recordedAtEpochMs).atZone(PKT_ZONE).toLocalDate() }
          .toSortedMap()
          .entries
          .toList()
          .takeLast(7)
          .map { (date, dayTxs) ->
            HostEarningsPoint(
              label = DAY_LABEL_FMT.format(date.atStartOfDay(PKT_ZONE).toInstant()),
              netAmountPkr = dayTxs.sumOf { it.hostNetSharePkr },
            )
          }
      }

    val recentActivity =
      bookings
        .sortedWith(
          compareByDescending<Booking> { it.createdAtEpochMs }.thenByDescending { it.startEpochMs }
        )
        .take(4)
        .map { booking ->
          val space = spacesById[booking.spaceId]
          HostRecentActivityItem(
            booking = booking,
            spaceTitle = space?.title ?: "Listing #${booking.spaceId.takeLast(4)}",
            areaLabel = space?.location?.areaLabel ?: booking.city.displayName,
          )
        }

    return HostDashboardAnalytics(
      summary = summary,
      activityTrend = activityTrend,
      statusDistribution = statusDistribution,
      earningsTrend = earningsTrend,
      recentActivity = recentActivity,
    )
  }
}
