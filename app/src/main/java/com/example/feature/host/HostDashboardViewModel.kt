package com.example.feature.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.Booking
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.ChatRepository
import com.example.core.domain.repository.DemoAuthRepository
import com.example.core.domain.repository.DisputeRepository
import com.example.core.domain.repository.HostBayMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.domain.repository.HostScheduleMutationResult
import com.example.core.domain.repository.NotificationRepository
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.UserProfileRepository
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.DemoSessionState
import com.example.feature.host.domain.HostDemoLedgerSummary
import com.example.feature.host.domain.HostLedgerCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HostDashboardTab(val label: String) {
  OVERVIEW("Overview"),
  LISTINGS("My Listings"),
  BOOKINGS("Bookings"),
  LEDGER("Demo Ledger"),
}

data class HostDashboardUiState(
  val activeUser: UserProfile? = null,
  val hostProfile: HostProfile? = null,
  val spaces: List<ParkingSpace> = emptyList(),
  val bookings: List<Booking> = emptyList(),
  val ledger: HostDemoLedgerSummary? = null,
  val activeTab: HostDashboardTab = HostDashboardTab.OVERVIEW,
  val selectedSpaceForBays: ParkingSpace? = null,
  val baysForSelectedSpace: List<ParkingBay> = emptyList(),
  val selectedSpaceForSchedule: ParkingSpace? = null,
  val editingListing: ParkingSpace? = null,
  val isCreatingNewListing: Boolean = false,
  val actionFeedback: String? = null,
  val errorMessage: String? = null,
  val isLoading: Boolean = false,
  val unreadNotificationsCount: Int = 0,
  val unreadChatsCount: Int = 0,
  val openDisputesCount: Int = 0,
  val hasCompanionCustomerAccount: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HostDashboardViewModel(
  private val parkingRepository: ParkingRepository,
  private val bookingRepository: BookingRepository,
  private val userProfileRepository: UserProfileRepository,
  private val demoSessionManager: DemoSessionManager,
  private val notificationRepository: NotificationRepository? = null,
  private val chatRepository: ChatRepository? = null,
  private val disputeRepository: DisputeRepository? = null,
  private val demoAuthRepository: DemoAuthRepository? = null,
) : ViewModel() {

  private val _activeTab = MutableStateFlow(HostDashboardTab.OVERVIEW)
  private val _selectedSpaceForBays = MutableStateFlow<ParkingSpace?>(null)
  private val _selectedSpaceForSchedule = MutableStateFlow<ParkingSpace?>(null)
  private val _editingListing = MutableStateFlow<ParkingSpace?>(null)
  private val _isCreatingNewListing = MutableStateFlow(false)
  private val _actionFeedback = MutableStateFlow<String?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)

  private val hostDataFlow: Flow<Pair<UserProfile?, Triple<HostProfile?, List<ParkingSpace>, List<Booking>>>> =
    demoSessionManager.session.flatMapLatest { session: DemoSessionState ->
      val user = session.activeUser
      val hostId = if (user?.role == UserRole.HOST) user.id else null
      if (hostId == null) {
        flowOf(null to Triple<HostProfile?, List<ParkingSpace>, List<Booking>>(null, emptyList(), emptyList()))
      } else {
        combine(
          userProfileRepository.observeHostProfile(hostId),
          parkingRepository.observeSpacesByHost(hostId),
          bookingRepository.observeBookingsForHost(hostId),
        ) { profile, spaces, bookings ->
          user to Triple(
            profile,
            spaces.filter { it.hostId == hostId },
            bookings.filter { it.hostId == hostId },
          )
        }
      }
    }

  private val baysFlow =
    _selectedSpaceForBays.flatMapLatest { space ->
      if (space == null) flowOf(emptyList())
      else parkingRepository.observeBaysForSpace(space.id)
    }

  private val hasCompanionCustomerFlow: Flow<Boolean> =
    demoSessionManager.session.flatMapLatest { session ->
      val user = session.activeUser
      if (
        user == null ||
          user.role != UserRole.HOST ||
          !user.isSelfRegisteredDemoAccount ||
          demoAuthRepository == null
      ) {
        flowOf(false)
      } else {
        flow {
          emit(
            demoAuthRepository.hasAccountForEmailAndRole(
              email = user.emailMasked,
              role = UserRole.CUSTOMER,
            )
          )
        }
      }
    }

  private val hostCountsFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val hostId = session.activeUser?.id
      if (hostId == null) {
        flowOf(Triple(0, 0, 0) to false)
      } else {
        combine(
          notificationRepository?.observeUnreadCount(hostId) ?: flowOf(0),
          chatRepository?.observeThreadSummariesForUser(hostId) ?: flowOf(emptyList()),
          disputeRepository?.observeDisputesForHost(hostId) ?: flowOf(emptyList()),
          hasCompanionCustomerFlow,
        ) { unreadNotifs, threads, disputes, hasCompanionCustomer ->
          val unreadChats = threads.sumOf { it.unreadCount }
          val openDisputes =
            disputes.count {
              it.status == com.example.core.domain.model.DisputeStatus.OPEN ||
                it.status == com.example.core.domain.model.DisputeStatus.UNDER_REVIEW
            }
          Triple(unreadNotifs, unreadChats, openDisputes) to hasCompanionCustomer
        }
      }
    }

  val uiState: StateFlow<HostDashboardUiState> =
    combine(
      hostDataFlow,
      _activeTab,
      _selectedSpaceForBays,
      baysFlow,
      _selectedSpaceForSchedule,
      _editingListing,
      _isCreatingNewListing,
      _actionFeedback,
      _errorMessage,
      hostCountsFlow,
    ) { args ->
      @Suppress("UNCHECKED_CAST")
      val sessionAndData = args[0] as Pair<UserProfile?, Triple<HostProfile?, List<ParkingSpace>, List<Booking>>>
      val tab = args[1] as HostDashboardTab
      val spaceForBays = args[2] as ParkingSpace?
      @Suppress("UNCHECKED_CAST")
      val bays = args[3] as List<ParkingBay>
      val spaceForSchedule = args[4] as ParkingSpace?
      val editListing = args[5] as ParkingSpace?
      val isCreating = args[6] as Boolean
      val feedback = args[7] as String?
      val err = args[8] as String?
      @Suppress("UNCHECKED_CAST")
      val countsAndCompanion = args[9] as Pair<Triple<Int, Int, Int>, Boolean>
      val counts = countsAndCompanion.first
      val hasCompanionCustomer = countsAndCompanion.second

      val activeUser = sessionAndData.first
      val (profile, spaces, bookings) = sessionAndData.second
      val activeHostId = activeUser?.id
      val ledger = HostLedgerCalculator.compute(bookings, spaces)

      HostDashboardUiState(
        activeUser = activeUser,
        hostProfile = profile,
        spaces = spaces,
        bookings = bookings,
        ledger = ledger,
        activeTab = tab,
        selectedSpaceForBays = spaceForBays?.takeIf { it.hostId == activeHostId },
        baysForSelectedSpace = if (spaceForBays?.hostId == activeHostId) bays else emptyList(),
        selectedSpaceForSchedule = spaceForSchedule?.takeIf { it.hostId == activeHostId },
        editingListing = editListing?.takeIf { it.hostId == activeHostId },
        isCreatingNewListing = isCreating,
        actionFeedback = feedback,
        errorMessage = err,
        isLoading = false,
        unreadNotificationsCount = counts.first,
        unreadChatsCount = counts.second,
        openDisputesCount = counts.third,
        hasCompanionCustomerAccount = hasCompanionCustomer,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue = HostDashboardUiState(isLoading = true),
    )

  fun selectTab(tab: HostDashboardTab) {
    _activeTab.value = tab
  }

  fun startCreateListing() {
    _editingListing.value = null
    _isCreatingNewListing.value = true
    _actionFeedback.value = null
    _errorMessage.value = null
  }

  fun startEditListing(space: ParkingSpace) {
    _editingListing.value = space
    _isCreatingNewListing.value = false
    _actionFeedback.value = null
    _errorMessage.value = null
  }

  fun dismissListingEditor() {
    _editingListing.value = null
    _isCreatingNewListing.value = false
  }

  fun saveListing(space: ParkingSpace, publishImmediately: Boolean) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = parkingRepository.createOrUpdateListing(hostId, space, publishImmediately)) {
        is HostListingMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
          _isCreatingNewListing.value = false
          _editingListing.value = null
        }
        is HostListingMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun openBaysManager(space: ParkingSpace) {
    _selectedSpaceForBays.value = space
    _actionFeedback.value = null
    _errorMessage.value = null
  }

  fun dismissBaysManager() {
    _selectedSpaceForBays.value = null
  }

  fun addBay(
    spaceId: String,
    bayCode: String,
    supportedVehicles: Set<VehicleType>,
    isCovered: Boolean,
  ) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = parkingRepository.addBayToSpace(hostId, spaceId, bayCode, supportedVehicles, isCovered)) {
        is HostBayMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
        }
        is HostBayMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun updateBay(spaceId: String, bay: ParkingBay) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = parkingRepository.updateBay(hostId, spaceId, bay)) {
        is HostBayMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
        }
        is HostBayMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun toggleBayActive(spaceId: String, bayId: String, isActive: Boolean) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = parkingRepository.toggleBayActive(hostId, spaceId, bayId, isActive)) {
        is HostBayMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
        }
        is HostBayMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun openScheduleManager(space: ParkingSpace) {
    _selectedSpaceForSchedule.value = space
    _actionFeedback.value = null
    _errorMessage.value = null
  }

  fun dismissScheduleManager() {
    _selectedSpaceForSchedule.value = null
  }

  fun saveSchedule(spaceId: String, schedule: AvailabilitySchedule) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = parkingRepository.updateListingScheduleAndBlackouts(hostId, spaceId, schedule)) {
        is HostScheduleMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
          _selectedSpaceForSchedule.value = null
        }
        is HostScheduleMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun hostCheckIn(bookingId: String) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = bookingRepository.hostCheckInBooking(bookingId, hostId)) {
        is BookingMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
        }
        is BookingMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun hostCheckOut(bookingId: String) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = bookingRepository.hostCheckOutBooking(bookingId, hostId)) {
        is BookingMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
        }
        is BookingMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun hostCancelBooking(bookingId: String, reason: String) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = bookingRepository.hostCancelBooking(bookingId, hostId, reason)) {
        is BookingMutationResult.Success -> {
          _actionFeedback.value = res.message
          _errorMessage.value = null
        }
        is BookingMutationResult.Rejected -> {
          _errorMessage.value = res.reason
        }
      }
    }
  }

  fun updateHostOnboardingProfile(
    businessAlias: String,
    operatingCities: Set<SupportedCity>,
  ) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      userProfileRepository
        .updateHostOnboardingProfile(hostId, businessAlias, operatingCities)
        .onSuccess {
          _actionFeedback.value = "Host settings updated (${it.displayBusinessAlias})."
          _errorMessage.value = null
        }
        .onFailure { err ->
          _errorMessage.value = err.message ?: "Could not update host settings."
        }
    }
  }

  fun showActionFeedback(message: String) {
    _actionFeedback.value = message
    _errorMessage.value = null
  }

  fun dismissFeedback() {
    _actionFeedback.value = null
    _errorMessage.value = null
  }

  class Factory(private val container: ParkSpaceAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      HostDashboardViewModel(
        parkingRepository = container.parkingRepository,
        bookingRepository = container.bookingRepository,
        userProfileRepository = container.userProfileRepository,
        demoSessionManager = container.demoSessionManager,
        notificationRepository = container.notificationRepository,
        chatRepository = container.chatRepository,
        disputeRepository = container.disputeRepository,
        demoAuthRepository = container.demoAuthRepository,
      ) as T
  }
}
