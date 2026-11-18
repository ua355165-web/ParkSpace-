package com.example.feature.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.UiState
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.Booking
import com.example.core.domain.model.CustomerProfile
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.VehicleProfile
import com.example.core.domain.model.VehicleType
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoundationDashboardData(
  val session: DemoSessionState,
  val approvedSpacesInSelectedCity: List<ParkingSpace>,
  val allSpacesCount: Int,
  val approvedSpacesCount: Int,
  val pendingSpacesCount: Int,
  val activeCustomerProfile: CustomerProfile?,
  val activeUserVehicles: List<VehicleProfile>,
  val activeHostProfile: HostProfile?,
  val activeHostSpaces: List<ParkingSpace>,
  val activeUserBookings: List<Booking>,
  val unreadNotificationsCount: Int = 0,
  val unreadChatsCount: Int = 0,
  val availableDemoCreditPkr: Int = 0,
) {
  /**
   * Returns only the ~200m privacy-masked locations for public pre-booking previews.
   */
  fun maskedLocationFor(space: ParkingSpace): ParkingLocation =
    space.location.toPublicMaskedLocation()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ParkSpaceFoundationViewModel(
  private val container: ParkSpaceAppContainer,
) : ViewModel() {

  private val _seedState = MutableStateFlow<SeedBootstrapState>(SeedBootstrapState.Seeding)
  val seedState: StateFlow<SeedBootstrapState> = _seedState.asStateFlow()

  private val _sandboxFeedback = MutableStateFlow<String?>(null)
  val sandboxFeedback: StateFlow<String?> = _sandboxFeedback.asStateFlow()

  val sessionState: StateFlow<DemoSessionState> = container.demoSessionManager.session

  private val activeCustomerProfileFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId != null && state.activeRole == UserRole.CUSTOMER) {
        container.userProfileRepository.observeCustomerProfile(userId)
      } else {
        flowOf(null)
      }
    }

  private val activeVehiclesFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId != null && state.activeRole == UserRole.CUSTOMER) {
        container.userProfileRepository.observeVehiclesForUser(userId)
      } else {
        flowOf(emptyList())
      }
    }

  private val activeHostProfileFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId != null && state.activeRole == UserRole.HOST) {
        container.userProfileRepository.observeHostProfile(userId)
      } else {
        flowOf(null)
      }
    }

  private val activeHostSpacesFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId != null && state.activeRole == UserRole.HOST) {
        container.parkingRepository.observeSpacesByHost(userId)
      } else {
        flowOf(emptyList())
      }
    }

  private val activeBookingsFlow =
    sessionState.flatMapLatest { state ->
      val user = state.activeUser
      when (user?.role) {
        UserRole.CUSTOMER -> container.bookingRepository.observeBookingsForCustomer(user.id)
        UserRole.HOST -> container.bookingRepository.observeBookingsForHost(user.id)
        UserRole.SUPER_ADMIN -> container.bookingRepository.observeAllBookings()
        null -> flowOf(emptyList())
      }
    }

  private val selectedCitySpacesFlow =
    sessionState.flatMapLatest { state ->
      container.parkingRepository.observeApprovedSpacesByCity(state.selectedCity)
    }

  private data class CoreCatalogSlice(
    val session: DemoSessionState,
    val citySpaces: List<ParkingSpace>,
    val allSpaces: List<ParkingSpace>,
  )

  private data class ActiveUserSlice(
    val customerProfile: CustomerProfile?,
    val vehicles: List<VehicleProfile>,
    val hostProfile: HostProfile?,
    val hostSpaces: List<ParkingSpace>,
    val bookings: List<Booking>,
  )

  private val unreadNotificationsFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId == null) flowOf(0)
      else container.notificationRepository.observeUnreadCount(userId)
    }

  private val unreadChatsFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId == null) flowOf(0)
      else combine(
        container.chatRepository.observeThreadSummariesForUser(userId),
        flowOf(Unit),
      ) { threads, _ -> threads.sumOf { it.unreadCount } }
    }

  private val demoCreditFlow =
    sessionState.flatMapLatest { state ->
      val userId = state.activeUser?.id
      if (userId == null || state.activeRole != UserRole.CUSTOMER) flowOf(0)
      else container.paymentTransactionRepository.observeAvailableDemoCreditPkr(userId)
    }

  private val marketplaceMetaFlow =
    combine(unreadNotificationsFlow, unreadChatsFlow, demoCreditFlow) { notifs, chats, credits ->
      Triple(notifs, chats, credits)
    }

  val uiState: StateFlow<UiState<FoundationDashboardData>> =
    combine(
        combine(
          sessionState,
          selectedCitySpacesFlow,
          container.parkingRepository.observeAllSpacesIncludingPending(),
        ) { session, citySpaces, allSpaces ->
          CoreCatalogSlice(session, citySpaces, allSpaces)
        },
        combine(
          activeCustomerProfileFlow,
          activeVehiclesFlow,
          activeHostProfileFlow,
          activeHostSpacesFlow,
          activeBookingsFlow,
        ) { customerProfile, vehicles, hostProfile, hostSpaces, bookings ->
          ActiveUserSlice(customerProfile, vehicles, hostProfile, hostSpaces, bookings)
        },
        marketplaceMetaFlow,
        _seedState,
      ) { catalog, userSlice, meta, seed ->
        when (seed) {
          is SeedBootstrapState.Seeding -> UiState.Loading
          is SeedBootstrapState.Failed ->
            UiState.Error(
              title = "Demo Database Initialization Failed",
              message = seed.reason,
              retryable = true,
            )
          is SeedBootstrapState.Ready -> {
            val approvedCount = catalog.allSpaces.count { it.isApproved }
            val pendingCount = catalog.allSpaces.count { !it.isApproved }
            UiState.Success(
              FoundationDashboardData(
                session = catalog.session,
                approvedSpacesInSelectedCity = catalog.citySpaces,
                allSpacesCount = catalog.allSpaces.size,
                approvedSpacesCount = approvedCount,
                pendingSpacesCount = pendingCount,
                activeCustomerProfile = userSlice.customerProfile,
                activeUserVehicles = userSlice.vehicles,
                activeHostProfile = userSlice.hostProfile,
                activeHostSpaces = userSlice.hostSpaces,
                activeUserBookings = userSlice.bookings,
                unreadNotificationsCount = meta.first,
                unreadChatsCount = meta.second,
                availableDemoCreditPkr = meta.third,
              )
            )
          }
        }
      }
      .catch { err ->
        emit(
          UiState.Error(
            title = "Unable to load local Demo state",
            message = err.message ?: "Unexpected Room error",
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
      )

  init {
    bootstrapDemoSeed()
  }

  fun bootstrapDemoSeed() {
    viewModelScope.launch {
      _seedState.value = SeedBootstrapState.Seeding
      runCatching {
        container.demoSeedInitializer.seedIfNeeded()
        container.demoSessionManager.restorePersistedSession()
      }
        .onSuccess { _seedState.value = SeedBootstrapState.Ready }
        .onFailure { err ->
          _seedState.value = SeedBootstrapState.Failed(err.message ?: "Room initialization error")
        }
    }
  }

  fun selectCity(city: SupportedCity) {
    container.demoSessionManager.selectCity(city)
  }

  fun updateActiveCustomerPreferredCity(city: SupportedCity) {
    container.demoSessionManager.selectCity(city)
    val userId = sessionState.value.activeUser?.id ?: return
    if (sessionState.value.activeRole != UserRole.CUSTOMER) return
    viewModelScope.launch {
      container.userProfileRepository.updateCustomerPreferredCity(userId, city)
      _sandboxFeedback.value = "Preferred city updated to ${city.displayName}."
    }
  }

  fun addVehicleForActiveCustomer(
    vehicleType: VehicleType,
    label: String,
    plateNumber: String,
  ) {
    val userId = sessionState.value.activeUser?.id ?: return
    viewModelScope.launch {
      container.userProfileRepository
        .addVehicleForUser(
          ownerUserId = userId,
          vehicleType = vehicleType,
          label = label,
          plateNumber = plateNumber,
          makeDefault = true,
        )
        .onSuccess { created ->
          _sandboxFeedback.value = "Added ${created.label} (${created.plateNumberMasked}) to your account."
        }
        .onFailure { err ->
          _sandboxFeedback.value = err.message ?: "Could not add vehicle."
        }
    }
  }

  fun updateActiveHostOnboarding(
    businessAlias: String,
    operatingCities: Set<SupportedCity>,
  ) {
    val userId = sessionState.value.activeUser?.id ?: return
    if (sessionState.value.activeRole != UserRole.HOST) return
    viewModelScope.launch {
      container.userProfileRepository
        .updateHostOnboardingProfile(userId, businessAlias, operatingCities)
        .onSuccess {
          _sandboxFeedback.value =
            "Host onboarding profile saved (Verification: ${it.verificationStatus.displayName})."
        }
        .onFailure { err ->
          _sandboxFeedback.value = err.message ?: "Could not update host onboarding profile."
        }
    }
  }

  fun unlockPrivateDemoAdmin(onUnlocked: () -> Unit) {
    viewModelScope.launch {
      when (
        val result =
          container.demoSessionManager.unlockPrivateDemoAdminSandbox(
            acknowledgedLocalTestingOnly = true
          )
      ) {
        is DemoSandboxActionResult.Success -> {
          _sandboxFeedback.value = null
          onUnlocked()
        }
        is DemoSandboxActionResult.Rejected -> _sandboxFeedback.value = result.reason
      }
    }
  }

  fun signOutDemoSandbox() {
    container.demoSessionManager.signOutDemoSandbox()
    _sandboxFeedback.value = null
  }

  fun dismissFeedback() {
    _sandboxFeedback.value = null
  }

  sealed interface SeedBootstrapState {
    data object Seeding : SeedBootstrapState

    data object Ready : SeedBootstrapState

    data class Failed(val reason: String) : SeedBootstrapState
  }

  class Factory(
    private val container: ParkSpaceAppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(ParkSpaceFoundationViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return ParkSpaceFoundationViewModel(container) as T
    }
  }
}
