package com.example.feature.booking.domain

import com.example.core.config.BookingType
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.VehicleProfile
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AvailabilityCheckResult(
  val isAvailable: Boolean,
  val effectiveCapacity: Int,
  val peakOverlappingBookings: Int,
  val remainingBaysForWindow: Int,
  val assignedBayId: String?,
  val blockingReason: String?,
)

/**
 * Pure Kotlin half-open interval overlap and bay-capacity validator.
 * Uses [startEpochMs, endEpochMs) semantics so back-to-back bookings do not conflict.
 */
object BookingAvailabilityValidator {

  val PAKISTAN_ZONE: ZoneId = ZoneId.of("Asia/Karachi")
  private val ISO_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun intervalsOverlap(
    startA: Long,
    endA: Long,
    startB: Long,
    endB: Long,
  ): Boolean = startA < endB && endA > startB

  fun validate(
    space: ParkingSpace,
    bays: List<ParkingBay>,
    existingBookingsForSpace: List<Booking>,
    vehicle: VehicleProfile,
    bookingType: BookingType,
    startEpochMs: Long,
    endEpochMs: Long,
    nowEpochMs: Long,
    customerId: String,
  ): AvailabilityCheckResult {
    if (!space.isApproved) {
      return unavailable(
        reason = "This parking space is pending approval and cannot accept bookings yet.",
        capacity = 0,
      )
    }

    if (space.availableBays <= 0 || space.totalBays <= 0) {
      return unavailable(
        reason = "This space currently has 0 operational bays.",
        capacity = 0,
      )
    }

    if (startEpochMs < nowEpochMs - 60_000L) {
      return unavailable(
        reason = "Start time must be in the future.",
        capacity = space.availableBays,
      )
    }

    if (endEpochMs <= startEpochMs) {
      return unavailable(
        reason = "End time must be after start time.",
        capacity = space.availableBays,
      )
    }

    if (vehicle.vehicleType !in space.supportedVehicles) {
      return unavailable(
        reason = "${vehicle.vehicleType.displayName} is not supported at ${space.title}.",
        capacity = space.availableBays,
      )
    }

    if (bookingType == BookingType.MONTHLY &&
      (!space.monthlyEnabled || space.monthlyRatePkr == null)
    ) {
      return unavailable(
        reason = "Monthly 30-day plans are not enabled for this space.",
        capacity = space.availableBays,
      )
    }

    val blackoutHit = findBlackoutDateInRange(space, startEpochMs, endEpochMs)
    if (blackoutHit != null) {
      return unavailable(
        reason = "Host blackout date ($blackoutHit) falls inside your selected parking window.",
        capacity = space.availableBays,
      )
    }

    val scheduleViolation = validateOperatingSchedule(space, bookingType, startEpochMs, endEpochMs)
    if (scheduleViolation != null) {
      return unavailable(
        reason = scheduleViolation,
        capacity = space.availableBays,
      )
    }

    val overlappingActive =
      existingBookingsForSpace.filter { existing ->
        existing.status in Booking.CAPACITY_CONSUMING_STATUSES &&
          intervalsOverlap(existing.startEpochMs, existing.endEpochMs, startEpochMs, endEpochMs)
      }

    // Duplicate booking submission check for the same customer, vehicle, and exact window
    val hasDuplicateSubmission =
      overlappingActive.any { existing ->
        existing.customerId == customerId &&
          existing.vehicleId == vehicle.id &&
          existing.bookingType == bookingType &&
          existing.startEpochMs == startEpochMs &&
          existing.endEpochMs == endEpochMs
      }
    if (hasDuplicateSubmission) {
      return unavailable(
        reason = "You already have an active or pending booking for this exact slot and vehicle.",
        capacity = space.availableBays,
      )
    }

    val effectiveCapacity = space.availableBays.coerceAtMost(space.totalBays)
    val peakOverlap = computePeakConcurrency(overlappingActive, startEpochMs, endEpochMs)
    val remaining = (effectiveCapacity - peakOverlap).coerceAtLeast(0)

    if (peakOverlap >= effectiveCapacity) {
      return AvailabilityCheckResult(
        isAvailable = false,
        effectiveCapacity = effectiveCapacity,
        peakOverlappingBookings = peakOverlap,
        remainingBaysForWindow = 0,
        assignedBayId = null,
        blockingReason =
          "All $effectiveCapacity bays are booked during part of this time window. Choose another time or space.",
      )
    }

    // Prefer an active compatible seeded/explicit ParkingBay without overlap; otherwise allocate deterministic overflow bay id within capacity
    val compatibleActiveBays =
      bays.filter { it.isActiveInDemo && vehicle.vehicleType in it.supportedVehicles }
    val occupiedBayIds = overlappingActive.map { it.bayId }.toSet()
    val freeExplicitBay = compatibleActiveBays.firstOrNull { it.id !in occupiedBayIds }
    val assignedBayId =
      freeExplicitBay?.id ?: "${space.id}-bay-slot-${peakOverlap + 1}"

    return AvailabilityCheckResult(
      isAvailable = true,
      effectiveCapacity = effectiveCapacity,
      peakOverlappingBookings = peakOverlap,
      remainingBaysForWindow = remaining,
      assignedBayId = assignedBayId,
      blockingReason = null,
    )
  }

  /**
   * Sweep-line algorithm calculating peak simultaneous active bookings across [reqStart, reqEnd).
   */
  fun computePeakConcurrency(
    overlappingBookings: List<Booking>,
    reqStart: Long,
    reqEnd: Long,
  ): Int {
    if (overlappingBookings.isEmpty()) return 0

    data class Event(val time: Long, val delta: Int)

    val events = ArrayList<Event>(overlappingBookings.size * 2)
    for (booking in overlappingBookings) {
      val clampedStart = maxOf(booking.startEpochMs, reqStart)
      val clampedEnd = minOf(booking.endEpochMs, reqEnd)
      if (clampedStart < clampedEnd) {
        events.add(Event(clampedStart, +1))
        events.add(Event(clampedEnd, -1))
      }
    }
    // Process departures (-1) before arrivals (+1) at identical timestamps (half-open semantics)
    events.sortWith(compareBy<Event> { it.time }.thenBy { it.delta })

    var active = 0
    var peak = 0
    for (event in events) {
      active += event.delta
      if (active > peak) peak = active
    }
    return peak
  }

  private fun findBlackoutDateInRange(
    space: ParkingSpace,
    startEpochMs: Long,
    endEpochMs: Long,
  ): String? {
    val blackouts = space.schedule.blackoutDatesIso
    if (blackouts.isEmpty()) return null

    var cursorDate = Instant.ofEpochMilli(startEpochMs).atZone(PAKISTAN_ZONE).toLocalDate()
    val endInclusiveInstant = Instant.ofEpochMilli((endEpochMs - 1L).coerceAtLeast(startEpochMs))
    val endDate = endInclusiveInstant.atZone(PAKISTAN_ZONE).toLocalDate()

    var guardDays = 0
    while (!cursorDate.isAfter(endDate) && guardDays <= 400) {
      val iso = cursorDate.format(ISO_DATE_FORMAT)
      if (iso in blackouts) return iso
      cursorDate = cursorDate.plusDays(1)
      guardDays++
    }
    return null
  }

  private fun validateOperatingSchedule(
    space: ParkingSpace,
    bookingType: BookingType,
    startEpochMs: Long,
    endEpochMs: Long,
  ): String? {
    val schedule = space.schedule
    val startZoned = Instant.ofEpochMilli(startEpochMs).atZone(PAKISTAN_ZONE)
    val startParkingDay = startZoned.dayOfWeek.toParkingDay()
    if (startParkingDay !in schedule.openDays) {
      return "${space.title} is closed on ${startParkingDay.shortLabel}."
    }

    if (bookingType == BookingType.HOURLY && !schedule.is24Hours) {
      val endZoned = Instant.ofEpochMilli(endEpochMs - 1L).atZone(PAKISTAN_ZONE)
      val endParkingDay = endZoned.dayOfWeek.toParkingDay()
      if (endParkingDay !in schedule.openDays) {
        return "${space.title} is closed on ${endParkingDay.shortLabel}."
      }
      val startHour = startZoned.hour
      val endHourInclusive = endZoned.hour
      if (startZoned.toLocalDate() != endZoned.toLocalDate()) {
        return "Overnight hourly slots require a 24-hour facility; choose Daily booking or daytime hours (${schedule.formattedHours()})."
      }
      if (startHour < schedule.openHour || endHourInclusive > schedule.closeHour) {
        return "Selected hours fall outside facility hours (${schedule.formattedHours()})."
      }
    }
    return null
  }

  private fun DayOfWeek.toParkingDay(): ParkingDayOfWeek =
    when (this) {
      DayOfWeek.MONDAY -> ParkingDayOfWeek.MON
      DayOfWeek.TUESDAY -> ParkingDayOfWeek.TUE
      DayOfWeek.WEDNESDAY -> ParkingDayOfWeek.WED
      DayOfWeek.THURSDAY -> ParkingDayOfWeek.THU
      DayOfWeek.FRIDAY -> ParkingDayOfWeek.FRI
      DayOfWeek.SATURDAY -> ParkingDayOfWeek.SAT
      DayOfWeek.SUNDAY -> ParkingDayOfWeek.SUN
    }

  private fun unavailable(
    reason: String,
    capacity: Int,
  ): AvailabilityCheckResult =
    AvailabilityCheckResult(
      isAvailable = false,
      effectiveCapacity = capacity,
      peakOverlappingBookings = 0,
      remainingBaysForWindow = 0,
      assignedBayId = null,
      blockingReason = reason,
    )
}
