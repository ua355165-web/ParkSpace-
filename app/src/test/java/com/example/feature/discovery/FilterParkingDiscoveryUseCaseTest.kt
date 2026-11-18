package com.example.feature.discovery

import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.demo.seed.DemoSeedDataset
import com.example.feature.discovery.domain.model.CoveredFilter
import com.example.feature.discovery.domain.model.ParkingDiscoveryFilter
import com.example.feature.discovery.domain.model.SecurityFilterState
import com.example.feature.discovery.domain.usecase.FilterParkingDiscoveryUseCase
import com.example.feature.discovery.ui.buildMarkerFeatureCollection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.geojson.Point

class FilterParkingDiscoveryUseCaseTest {

  private val useCase = FilterParkingDiscoveryUseCase()

  @Test
  fun `filters strictly by selected city and excludes unapproved spaces`() {
    val fsdResult =
      useCase.execute(
        allSpaces = DemoSeedDataset.parkingSpaces,
        filter = ParkingDiscoveryFilter(city = SupportedCity.FAISALABAD),
      )
    // Faisalabad has 2 approved + 1 pending space in DemoSeedDataset
    assertEquals(2, fsdResult.totalApprovedInCity)
    assertEquals(2, fsdResult.listings.size)
    assertTrue(fsdResult.listings.all { it.city == SupportedCity.FAISALABAD })
    assertFalse(fsdResult.listings.any { it.id == "demo-fsd-03" })
  }

  @Test
  fun `excludes zero-capacity unavailable spaces from discovery`() {
    val zeroBaySpace =
      DemoSeedDataset.parkingSpaces.first().copy(id = "demo-zero-bays", availableBays = 0)
    val result =
      useCase.execute(
        allSpaces = DemoSeedDataset.parkingSpaces + zeroBaySpace,
        filter = ParkingDiscoveryFilter(city = SupportedCity.LAHORE),
      )
    assertFalse(result.listings.any { it.id == "demo-zero-bays" })
  }

  @Test
  fun `search matches title and locality accurately`() {
    val byTitle =
      useCase.execute(
        allSpaces = DemoSeedDataset.parkingSpaces,
        filter = ParkingDiscoveryFilter(city = SupportedCity.LAHORE, searchQuery = "Anarkali"),
      )
    assertEquals(listOf("demo-lhr-03"), byTitle.listings.map { it.id })

    val byLocality =
      useCase.execute(
        allSpaces = DemoSeedDataset.parkingSpaces,
        filter = ParkingDiscoveryFilter(city = SupportedCity.KARACHI, searchQuery = "PECHS"),
      )
    assertEquals(listOf("demo-khi-02"), byLocality.listings.map { it.id })
  }

  @Test
  fun `combined filters work together and reset restores city listings`() {
    val combinedFilter =
      ParkingDiscoveryFilter(
        city = SupportedCity.ISLAMABAD,
        vehicleType = VehicleType.SUV,
        coveredFilter = CoveredFilter.COVERED_ONLY,
        security = SecurityFilterState(requireCctv = true, requireEvCharging = true),
        bookingType = BookingType.MONTHLY,
        minPricePkr = 20_000,
        maxPricePkr = 28_000,
        minAvailableBays = 5,
      )
    val filtered = useCase.execute(DemoSeedDataset.parkingSpaces, combinedFilter)
    assertEquals(listOf("demo-isb-02"), filtered.listings.map { it.id })
    assertTrue(combinedFilter.activeFilterCount() >= 6)

    val resetFilter = combinedFilter.resetPreservingCity()
    assertEquals(0, resetFilter.activeFilterCount())
    val restored = useCase.execute(DemoSeedDataset.parkingSpaces, resetFilter)
    assertEquals(3, restored.listings.size)
  }

  @Test
  fun `vehicle compatibility and price range filters return empty when unmatched`() {
    val bikeInKarachi =
      useCase.execute(
        allSpaces = DemoSeedDataset.parkingSpaces,
        filter =
          ParkingDiscoveryFilter(
            city = SupportedCity.KARACHI,
            vehicleType = VehicleType.BIKE,
            maxPricePkr = 100,
          ),
      )
    assertEquals(listOf("demo-khi-03"), bikeInKarachi.listings.map { it.id })

    val emptyResult =
      useCase.execute(
        allSpaces = DemoSeedDataset.parkingSpaces,
        filter =
          ParkingDiscoveryFilter(
            city = SupportedCity.KARACHI,
            vehicleType = VehicleType.BIKE,
            maxPricePkr = 10,
          ),
      )
    assertTrue(emptyResult.listings.isEmpty())
    assertTrue(emptyResult.markers.isEmpty())
  }

  @Test
  fun `map markers and geojson strictly expose 200m masked coordinates and never private details`() {
    val secretLat = 31.5199
    val secretLng = 74.3599
    val maskedLat = 31.5112
    val maskedLng = 74.3490
    val secretGate = "PRIVATE GATE 99 PIN 4321"

    val space =
      ParkingSpace(
        id = "privacy-check-space",
        hostId = "demo-host-lhr",
        title = "[Demo] Privacy Guard Lot",
        description = "Demo privacy test",
        parkingType = ParkingType.GATED_COMPOUND,
        location =
          ParkingLocation(
            city = SupportedCity.LAHORE,
            areaLabel = "Gulberg Demo Zone",
            publicMaskedLatitude = maskedLat,
            publicMaskedLongitude = maskedLng,
            privateGateSummary = secretGate,
            privateExactLatitude = secretLat,
            privateExactLongitude = secretLng,
          ),
        hourlyRatePkr = 150,
        dailyRatePkr = 900,
        monthlyRatePkr = null,
        monthlyEnabled = false,
        totalBays = 5,
        availableBays = 3,
        supportedVehicles = setOf(VehicleType.CAR),
        amenities = setOf(ParkingAmenity.CCTV),
        schedule =
          AvailabilitySchedule(
            openDays = setOf(ParkingDayOfWeek.MON),
            openHour = 8,
            closeHour = 20,
            is24Hours = false,
          ),
        isApproved = true,
        isDemoListing = true,
      )

    val result =
      useCase.execute(
        allSpaces = listOf(space),
        filter = ParkingDiscoveryFilter(city = SupportedCity.LAHORE),
        selectedSpaceId = space.id,
      )

    val marker = result.markers.single()
    assertEquals(200, marker.maskRadiusMeters)
    assertEquals(maskedLat, marker.latitude, 0.00001)
    assertEquals(maskedLng, marker.longitude, 0.00001)
    assertNotEquals(secretLat, marker.latitude, 0.0001)
    assertNotEquals(secretLng, marker.longitude, 0.0001)
    assertTrue(marker.isSelected)

    val featureCollection = buildMarkerFeatureCollection(result.markers)
    val feature = featureCollection.features()!!.single()
    val point = feature.geometry() as Point
    assertEquals(maskedLng, point.longitude(), 0.00001)
    assertEquals(maskedLat, point.latitude(), 0.00001)
    val json = featureCollection.toJson()
    assertFalse(json.contains(secretGate))
    assertFalse(json.contains(secretLat.toString()))
    assertFalse(json.contains(secretLng.toString()))
  }
}
