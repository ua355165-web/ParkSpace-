package com.example.core.domain.model

import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity

enum class ParkingType(val displayName: String) {
  COVERED_GARAGE("Covered Garage"),
  COMMERCIAL_PLAZA("Commercial Plaza"),
  OPEN_LOT("Open Parking Lot"),
  GATED_COMPOUND("Gated Compound"),
  PRIVATE_DRIVEWAY("Private Driveway Bay"),
}

enum class ParkingAmenity(val displayName: String) {
  COVERED("Covered Shade"),
  CCTV("24/7 CCTV"),
  SECURITY_GUARD("Security Guard"),
  EV_CHARGING("EV Charging"),
  NIGHT_LIGHTING("Night Lighting"),
  WHEELCHAIR_ACCESS("Step-Free Access"),
  VALET_ASSIST("Attendant Assist"),
  BIKE_LOCKERS("Bike Lock Anchor"),
}

enum class ParkingDayOfWeek(val shortLabel: String) {
  MON("Mon"),
  TUE("Tue"),
  WED("Wed"),
  THU("Thu"),
  FRI("Fri"),
  SAT("Sat"),
  SUN("Sun"),
}

data class ParkingLocation(
  val city: SupportedCity,
  val areaLabel: String,
  val publicMaskedLatitude: Double,
  val publicMaskedLongitude: Double,
  val maskRadiusMeters: Int = AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS,
  val privateGateSummary: String? = null,
  val privateExactLatitude: Double? = null,
  val privateExactLongitude: Double? = null,
) {
  init {
    require(areaLabel.isNotBlank()) { "ParkingLocation.areaLabel must not be blank" }
    require(maskRadiusMeters == AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS) {
      "ParkingLocation.maskRadiusMeters must match ${AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS}m policy"
    }
    require(publicMaskedLatitude in PAKISTAN_MIN_LAT..PAKISTAN_MAX_LAT) {
      "publicMaskedLatitude out of Pakistan bounds"
    }
    require(publicMaskedLongitude in PAKISTAN_MIN_LNG..PAKISTAN_MAX_LNG) {
      "publicMaskedLongitude out of Pakistan bounds"
    }
    if (privateExactLatitude != null) {
      require(privateExactLatitude in PAKISTAN_MIN_LAT..PAKISTAN_MAX_LAT) {
        "privateExactLatitude out of Pakistan bounds"
      }
    }
    if (privateExactLongitude != null) {
      require(privateExactLongitude in PAKISTAN_MIN_LNG..PAKISTAN_MAX_LNG) {
        "privateExactLongitude out of Pakistan bounds"
      }
    }
  }

  /**
   * Returns a privacy-safe copy for pre-booking discovery views, stripping
   * exact street/gate coordinates and instructions.
   */
  fun toPublicMaskedLocation(): ParkingLocation =
    copy(
      privateGateSummary = null,
      privateExactLatitude = null,
      privateExactLongitude = null,
    )

  companion object {
    const val PAKISTAN_MIN_LAT = 23.5
    const val PAKISTAN_MAX_LAT = 37.2
    const val PAKISTAN_MIN_LNG = 60.8
    const val PAKISTAN_MAX_LNG = 77.5
  }
}

data class AvailabilitySchedule(
  val openDays: Set<ParkingDayOfWeek>,
  val openHour: Int,
  val closeHour: Int,
  val is24Hours: Boolean,
  val blackoutDatesIso: Set<String> = emptySet(),
) {
  init {
    require(openDays.isNotEmpty()) { "AvailabilitySchedule must have at least one open day" }
    require(openHour in 0..23) { "openHour must be between 0 and 23" }
    require(closeHour in 0..23) { "closeHour must be between 0 and 23" }
    if (!is24Hours) {
      require(openHour < closeHour) { "openHour must be less than closeHour when not 24h" }
    }
  }

  fun formattedHours(): String =
    if (is24Hours) {
      "Open 24 Hours"
    } else {
      "%02d:00 – %02d:00".format(openHour, closeHour)
    }
}

data class ParkingBay(
  val id: String,
  val spaceId: String,
  val bayCode: String,
  val supportedVehicles: Set<VehicleType>,
  val isCovered: Boolean,
  val isActiveInDemo: Boolean,
) {
  init {
    require(id.isNotBlank()) { "ParkingBay.id must not be blank" }
    require(spaceId.isNotBlank()) { "ParkingBay.spaceId must not be blank" }
    require(bayCode.isNotBlank()) { "ParkingBay.bayCode must not be blank" }
    require(supportedVehicles.isNotEmpty()) { "ParkingBay must support at least 1 vehicle type" }
  }
}

data class ParkingSpace(
  val id: String,
  val hostId: String,
  val title: String,
  val description: String,
  val parkingType: ParkingType,
  val location: ParkingLocation,
  val hourlyRatePkr: Int,
  val dailyRatePkr: Int,
  val monthlyRatePkr: Int?,
  val monthlyEnabled: Boolean,
  val totalBays: Int,
  val availableBays: Int,
  val supportedVehicles: Set<VehicleType>,
  val amenities: Set<ParkingAmenity>,
  val schedule: AvailabilitySchedule,
  val isApproved: Boolean,
  val isDemoListing: Boolean,
  val photoTags: List<String> = listOf("Entrance Gate", "Covered Bay"),
  val photoUris: List<String> = emptyList(),
  val moderationNote: String? = null,
  val moderatedAtEpochMs: Long? = null,
  val isRejectedByAdmin: Boolean = false,
) {
  init {
    require(id.isNotBlank()) { "ParkingSpace.id must not be blank" }
    require(hostId.isNotBlank()) { "ParkingSpace.hostId must not be blank" }
    require(title.trim().length >= 4) { "ParkingSpace.title must be at least 4 characters" }
    require(description.isNotBlank()) { "ParkingSpace.description must not be blank" }
    require(hourlyRatePkr > 0) { "hourlyRatePkr must be positive" }
    require(dailyRatePkr >= hourlyRatePkr) { "dailyRatePkr must be >= hourlyRatePkr" }
    if (monthlyEnabled) {
      require(monthlyRatePkr != null && monthlyRatePkr > 0) {
        "monthlyRatePkr must be positive when monthlyEnabled is true"
      }
    }
    require(totalBays >= 1) { "totalBays must be at least 1" }
    require(availableBays in 0..totalBays) { "availableBays must be within 0..totalBays" }
    require(supportedVehicles.isNotEmpty()) { "ParkingSpace must support at least 1 vehicle type" }
    require(photoTags.size in 0..5) { "ParkingSpace photoTags cannot exceed 5 items" }
    require(photoUris.size in 0..5) { "ParkingSpace photoUris cannot exceed 5 items" }
  }
}
