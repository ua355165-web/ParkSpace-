package com.example.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.UiState
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.SpaceRatingSummary
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.ReviewRepository
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.discovery.domain.model.CoveredFilter
import com.example.feature.discovery.domain.model.DiscoveryFilterResult
import com.example.feature.discovery.domain.model.DiscoveryViewMode
import com.example.feature.discovery.domain.model.MapRenderStatus
import com.example.feature.discovery.domain.model.ParkingDiscoveryFilter
import com.example.feature.discovery.domain.model.ParkingPublicListing
import com.example.feature.discovery.domain.usecase.FilterParkingDiscoveryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class DiscoveryScreenData(
  val filter: ParkingDiscoveryFilter,
  val viewMode: DiscoveryViewMode,
  val mapRenderStatus: MapRenderStatus,
  val result: DiscoveryFilterResult,
  val selectedListing: ParkingPublicListing?,
  val cameraTriggerVersion: Int,
)

class DiscoveryViewModel(
  private val parkingRepository: ParkingRepository,
  private val demoSessionManager: DemoSessionManager,
  private val filterUseCase: FilterParkingDiscoveryUseCase = FilterParkingDiscoveryUseCase(),
  private val reviewRepository: ReviewRepository? = null,
) : ViewModel() {

  private val _filter =
    MutableStateFlow(
      ParkingDiscoveryFilter(city = demoSessionManager.session.value.selectedCity)
    )
  private val _viewMode = MutableStateFlow(DiscoveryViewMode.MAP)
  private val _selectedSpaceId = MutableStateFlow<String?>(null)
  private val _mapRenderStatus = MutableStateFlow<MapRenderStatus>(MapRenderStatus.LoadingStyle)
  private val _cameraTriggerVersion = MutableStateFlow(0)

  private data class UiControlSlice(
    val filter: ParkingDiscoveryFilter,
    val viewMode: DiscoveryViewMode,
    val selectedSpaceId: String?,
    val mapStatus: MapRenderStatus,
    val cameraVersion: Int,
  )

  private val controlsFlow =
    combine(
      _filter,
      _viewMode,
      _selectedSpaceId,
      _mapRenderStatus,
      _cameraTriggerVersion,
    ) { filter, viewMode, selectedId, mapStatus, cameraVersion ->
      UiControlSlice(filter, viewMode, selectedId, mapStatus, cameraVersion)
    }

  private val ratingSummariesFlow =
    reviewRepository?.observeSpaceRatingSummaries() ?: flowOf(emptyList<SpaceRatingSummary>())

  val uiState: StateFlow<UiState<DiscoveryScreenData>> =
    combine(
        parkingRepository.observeAllApprovedSpaces(),
        ratingSummariesFlow,
        controlsFlow,
      ) { allSpaces, summaries, controls ->
        val ratingMap = summaries.associateBy { it.spaceId }
        val result =
          filterUseCase.execute(
            allSpaces = allSpaces,
            filter = controls.filter,
            selectedSpaceId = controls.selectedSpaceId,
            ratingSummaries = ratingMap,
          )
        val selectedListing = result.listings.firstOrNull { it.id == controls.selectedSpaceId }

        UiState.Success(
          DiscoveryScreenData(
            filter = controls.filter,
            viewMode = controls.viewMode,
            mapRenderStatus = controls.mapStatus,
            result = result,
            selectedListing = selectedListing,
            cameraTriggerVersion = controls.cameraVersion,
          )
        ) as UiState<DiscoveryScreenData>
      }
      .catch { err ->
        emit(
          UiState.Error(
            title = "Discovery Unavailable",
            message = err.message ?: "Unable to read local Demo parking inventory.",
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
      )

  fun onSelectCity(city: SupportedCity) {
    demoSessionManager.selectCity(city)
    _selectedSpaceId.value = null
    _filter.update { it.copy(city = city, localityQuery = "") }
    _cameraTriggerVersion.update { it + 1 }
  }

  fun onSearchQueryChange(query: String) {
    _filter.update { it.copy(searchQuery = query) }
  }

  fun onLocalitySelect(locality: String) {
    _filter.update { current ->
      val next = if (current.localityQuery == locality) "" else locality
      current.copy(localityQuery = next)
    }
  }

  fun onVehicleTypeSelect(vehicleType: VehicleType?) {
    _filter.update { current ->
      current.copy(vehicleType = if (current.vehicleType == vehicleType) null else vehicleType)
    }
  }

  fun onParkingTypeSelect(parkingType: ParkingType?) {
    _filter.update { current ->
      current.copy(parkingType = if (current.parkingType == parkingType) null else parkingType)
    }
  }

  fun onCoveredFilterSelect(coveredFilter: CoveredFilter) {
    _filter.update { it.copy(coveredFilter = coveredFilter) }
  }

  fun onToggleCctv() {
    _filter.update { it.copy(security = it.security.copy(requireCctv = !it.security.requireCctv)) }
  }

  fun onToggleSecurityGuard() {
    _filter.update {
      it.copy(
        security = it.security.copy(requireSecurityGuard = !it.security.requireSecurityGuard)
      )
    }
  }

  fun onToggleEvCharging() {
    _filter.update {
      it.copy(security = it.security.copy(requireEvCharging = !it.security.requireEvCharging))
    }
  }

  fun onToggleNightLighting() {
    _filter.update {
      it.copy(
        security = it.security.copy(requireNightLighting = !it.security.requireNightLighting)
      )
    }
  }

  fun onBookingTypeSelect(bookingType: BookingType?) {
    _filter.update { current ->
      current.copy(bookingType = if (current.bookingType == bookingType) null else bookingType)
    }
  }

  fun onPriceRangeChange(minPkr: Int?, maxPkr: Int?) {
    _filter.update {
      it.copy(
        minPricePkr = minPkr?.takeIf { value -> value >= 0 },
        maxPricePkr = maxPkr?.takeIf { value -> value >= 0 },
      )
    }
  }

  fun onMinCapacityChange(minBays: Int) {
    _filter.update { it.copy(minAvailableBays = minBays.coerceAtLeast(1)) }
  }

  fun onResetFilters() {
    _selectedSpaceId.value = null
    _filter.update { it.resetPreservingCity() }
  }

  fun onToggleViewMode(mode: DiscoveryViewMode) {
    _viewMode.value = mode
  }

  fun onSelectSpace(spaceId: String) {
    _selectedSpaceId.value = spaceId
  }

  fun onClearSelection() {
    _selectedSpaceId.value = null
  }

  fun onMapStyleLoaded() {
    _mapRenderStatus.value = MapRenderStatus.Ready
  }

  fun onMapLoadFailed(errorMessage: String) {
    _mapRenderStatus.value =
      MapRenderStatus.OfflineOrError(
        errorMessage.ifBlank { "Map tiles unavailable. Switch to List view or retry." }
      )
  }

  fun onRetryMapLoad() {
    _mapRenderStatus.value = MapRenderStatus.LoadingStyle
    _cameraTriggerVersion.update { it + 1 }
  }

  fun onRecenterCity() {
    _cameraTriggerVersion.update { it + 1 }
  }

  class Factory(
    private val container: ParkSpaceAppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(DiscoveryViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return DiscoveryViewModel(
        parkingRepository = container.parkingRepository,
        demoSessionManager = container.demoSessionManager,
        reviewRepository = container.reviewRepository,
      ) as T
    }
  }
}
