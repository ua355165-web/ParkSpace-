package com.example.feature.discovery.domain.usecase

import com.example.core.config.BookingType
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.SpaceRatingSummary
import com.example.feature.discovery.domain.model.CoveredFilter
import com.example.feature.discovery.domain.model.DiscoveryFilterResult
import com.example.feature.discovery.domain.model.ParkingDiscoveryFilter
import com.example.feature.discovery.domain.model.ParkingPublicListing
import com.example.feature.discovery.domain.model.PublicParkingMarker

/**
 * Pure Kotlin search, filter, and privacy-safe marker projection use case.
 * Decoupled from Jetpack Compose UI and MapLibre rendering.
 */
class FilterParkingDiscoveryUseCase {

  fun execute(
    allSpaces: List<ParkingSpace>,
    filter: ParkingDiscoveryFilter,
    selectedSpaceId: String? = null,
    ratingSummaries: Map<String, SpaceRatingSummary> = emptyMap(),
  ): DiscoveryFilterResult {
    // Only approved spaces with positive capacity in the selected city are eligible.
    val cityEligible =
      allSpaces.filter { space ->
        space.isApproved &&
          space.availableBays > 0 &&
          space.schedule.openDays.isNotEmpty() &&
          space.location.city == filter.city
      }

    val availableLocalities =
      cityEligible.map { it.location.areaLabel }.distinct().sorted()

    val searchNeedle = filter.searchQuery.trim().lowercase()
    val localityNeedle = filter.localityQuery.trim().lowercase()

    val matchedListings =
      cityEligible
        .asSequence()
        .filter { space -> space.availableBays >= filter.minAvailableBays }
        .filter { space ->
          if (searchNeedle.isBlank()) {
            true
          } else {
            space.title.lowercase().contains(searchNeedle) ||
              space.location.areaLabel.lowercase().contains(searchNeedle) ||
              space.location.city.displayName.lowercase().contains(searchNeedle) ||
              space.parkingType.displayName.lowercase().contains(searchNeedle)
          }
        }
        .filter { space ->
          if (localityNeedle.isBlank()) {
            true
          } else {
            space.location.areaLabel.lowercase().contains(localityNeedle)
          }
        }
        .filter { space ->
          filter.vehicleType == null || filter.vehicleType in space.supportedVehicles
        }
        .filter { space ->
          filter.parkingType == null || space.parkingType == filter.parkingType
        }
        .filter { space ->
          val isCovered = ParkingAmenity.COVERED in space.amenities
          when (filter.coveredFilter) {
            CoveredFilter.ALL -> true
            CoveredFilter.COVERED_ONLY -> isCovered
            CoveredFilter.OPEN_ONLY -> !isCovered
          }
        }
        .filter { space ->
          (!filter.security.requireCctv || ParkingAmenity.CCTV in space.amenities) &&
            (!filter.security.requireSecurityGuard ||
              ParkingAmenity.SECURITY_GUARD in space.amenities) &&
            (!filter.security.requireEvCharging ||
              ParkingAmenity.EV_CHARGING in space.amenities) &&
            (!filter.security.requireNightLighting ||
              ParkingAmenity.NIGHT_LIGHTING in space.amenities)
        }
        .filter { space ->
          when (filter.bookingType) {
            BookingType.HOURLY -> space.hourlyRatePkr > 0
            BookingType.DAILY -> space.dailyRatePkr > 0
            BookingType.MONTHLY -> space.monthlyEnabled && space.monthlyRatePkr != null
            null -> true
          }
        }
        .map { space ->
          val rating = ratingSummaries[space.id]
          ParkingPublicListing.fromDomainSpace(
            space = space,
            averageRating = rating?.averageRating,
            reviewCount = rating?.reviewCount ?: 0,
          )
        }
        .filter { listing ->
          val effectivePrice = listing.effectiveRateFor(filter.bookingType)
          val minOk = filter.minPricePkr == null || effectivePrice >= filter.minPricePkr
          val maxOk = filter.maxPricePkr == null || effectivePrice <= filter.maxPricePkr
          minOk && maxOk
        }
        .sortedBy { it.effectiveRateFor(filter.bookingType) }
        .toList()

    val markers =
      matchedListings.map { listing ->
        PublicParkingMarker.fromPublicListing(
          listing = listing,
          bookingType = filter.bookingType,
          isSelected = listing.id == selectedSpaceId,
        )
      }

    return DiscoveryFilterResult(
      listings = matchedListings,
      markers = markers,
      availableLocalities = availableLocalities,
      totalApprovedInCity = cityEligible.size,
    )
  }
}
