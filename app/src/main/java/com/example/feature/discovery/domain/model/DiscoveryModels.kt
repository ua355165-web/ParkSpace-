package com.example.feature.discovery.domain.model

import com.example.core.config.AppEnvironment
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType

enum class CoveredFilter(val displayName: String) {
  ALL("All"),
  COVERED_ONLY("Covered"),
  OPEN_ONLY("Open Air"),
}

enum class DiscoveryViewMode {
  MAP,
  LIST,
}

sealed interface MapRenderStatus {
  data object LoadingStyle : MapRenderStatus

  data object Ready : MapRenderStatus

  data class OfflineOrError(val message: String) : MapRenderStatus
}

data class SecurityFilterState(
  val requireCctv: Boolean = false,
  val requireSecurityGuard: Boolean = false,
  val requireEvCharging: Boolean = false,
  val requireNightLighting: Boolean = false,
) {
  fun activeCount(): Int =
    listOf(requireCctv, requireSecurityGuard, requireEvCharging, requireNightLighting).count { it }
}

data class ParkingDiscoveryFilter(
  val city: SupportedCity = SupportedCity.LAHORE,
  val searchQuery: String = "",
  val localityQuery: String = "",
  val vehicleType: VehicleType? = null,
  val parkingType: ParkingType? = null,
  val coveredFilter: CoveredFilter = CoveredFilter.ALL,
  val security: SecurityFilterState = SecurityFilterState(),
  val bookingType: BookingType? = null,
  val minPricePkr: Int? = null,
  val maxPricePkr: Int? = null,
  val minAvailableBays: Int = 1,
) {
  init {
    require(minAvailableBays >= 1) { "minAvailableBays must be >= 1 to hide unavailable spaces" }
    if (minPricePkr != null) require(minPricePkr >= 0) { "minPricePkr cannot be negative" }
    if (maxPricePkr != null) require(maxPricePkr >= 0) { "maxPricePkr cannot be negative" }
  }

  fun activeFilterCount(): Int {
    var count = 0
    if (searchQuery.isNotBlank()) count++
    if (localityQuery.isNotBlank()) count++
    if (vehicleType != null) count++
    if (parkingType != null) count++
    if (coveredFilter != CoveredFilter.ALL) count++
    count += security.activeCount()
    if (bookingType != null) count++
    if (minPricePkr != null || maxPricePkr != null) count++
    if (minAvailableBays > 1) count++
    return count
  }

  fun resetPreservingCity(): ParkingDiscoveryFilter = ParkingDiscoveryFilter(city = city)
}

/**
 * Privacy-safe public listing projection for pre-booking discovery.
 * Intentionally omits exact coordinates, street/gate details, and guard contacts.
 */
data class ParkingPublicListing(
  val id: String,
  val title: String,
  val description: String,
  val parkingType: ParkingType,
  val city: SupportedCity,
  val areaLabel: String,
  val publicMaskedLatitude: Double,
  val publicMaskedLongitude: Double,
  val maskRadiusMeters: Int,
  val hourlyRatePkr: Int,
  val dailyRatePkr: Int,
  val monthlyRatePkr: Int?,
  val monthlyEnabled: Boolean,
  val totalBays: Int,
  val availableBays: Int,
  val supportedVehicles: Set<VehicleType>,
  val amenities: Set<ParkingAmenity>,
  val schedule: AvailabilitySchedule,
  val isDemoListing: Boolean,
  val photoUris: List<String> = emptyList(),
  val photoTags: List<String> = emptyList(),
  val averageRating: Double? = null,
  val reviewCount: Int = 0,
) {
  val isCovered: Boolean
    get() =
      ParkingAmenity.COVERED in amenities ||
        parkingType == ParkingType.COVERED_GARAGE ||
        parkingType == ParkingType.COMMERCIAL_PLAZA

  fun effectiveRateFor(bookingType: BookingType?): Int =
    when (bookingType) {
      BookingType.DAILY -> dailyRatePkr
      BookingType.MONTHLY -> monthlyRatePkr ?: dailyRatePkr
      BookingType.HOURLY,
      null -> hourlyRatePkr
    }

  companion object {
    fun fromDomainSpace(
      space: ParkingSpace,
      averageRating: Double? = null,
      reviewCount: Int = 0,
    ): ParkingPublicListing {
      val masked = space.location.toPublicMaskedLocation()
      require(masked.privateExactLatitude == null && masked.privateExactLongitude == null) {
        "Public discovery projection cannot retain private exact coordinates"
      }
      require(masked.privateGateSummary == null) {
        "Public discovery projection cannot retain private gate summary"
      }
      return ParkingPublicListing(
        id = space.id,
        title = space.title,
        description = space.description,
        parkingType = space.parkingType,
        city = masked.city,
        areaLabel = masked.areaLabel,
        publicMaskedLatitude = masked.publicMaskedLatitude,
        publicMaskedLongitude = masked.publicMaskedLongitude,
        maskRadiusMeters = masked.maskRadiusMeters,
        hourlyRatePkr = space.hourlyRatePkr,
        dailyRatePkr = space.dailyRatePkr,
        monthlyRatePkr = space.monthlyRatePkr,
        monthlyEnabled = space.monthlyEnabled,
        totalBays = space.totalBays,
        availableBays = space.availableBays,
        supportedVehicles = space.supportedVehicles,
        amenities = space.amenities,
        schedule = space.schedule,
        isDemoListing = space.isDemoListing,
        photoUris = space.photoUris,
        photoTags = space.photoTags,
        averageRating = averageRating,
        reviewCount = reviewCount,
      )
    }
  }
}

/**
 * Map marker model derived solely from ParkingPublicListing (200m masked coordinates).
 */
data class PublicParkingMarker(
  val spaceId: String,
  val title: String,
  val city: SupportedCity,
  val areaLabel: String,
  val latitude: Double,
  val longitude: Double,
  val maskRadiusMeters: Int,
  val displayPriceLabel: String,
  val availableBays: Int,
  val isSelected: Boolean,
) {
  companion object {
    fun fromPublicListing(
      listing: ParkingPublicListing,
      bookingType: BookingType?,
      isSelected: Boolean,
    ): PublicParkingMarker {
      val rate = listing.effectiveRateFor(bookingType)
      val unit =
        when (bookingType) {
          BookingType.DAILY -> "/d"
          BookingType.MONTHLY -> "/mo"
          BookingType.HOURLY,
          null -> "/h"
        }
      return PublicParkingMarker(
        spaceId = listing.id,
        title = listing.title,
        city = listing.city,
        areaLabel = listing.areaLabel,
        latitude = listing.publicMaskedLatitude,
        longitude = listing.publicMaskedLongitude,
        maskRadiusMeters = listing.maskRadiusMeters,
        displayPriceLabel = "${AppEnvironment.CURRENCY_SYMBOL} $rate$unit",
        availableBays = listing.availableBays,
        isSelected = isSelected,
      )
    }
  }
}

data class DiscoveryFilterResult(
  val listings: List<ParkingPublicListing>,
  val markers: List<PublicParkingMarker>,
  val availableLocalities: List<String>,
  val totalApprovedInCity: Int,
)
