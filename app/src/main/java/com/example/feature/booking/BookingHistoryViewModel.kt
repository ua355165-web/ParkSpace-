package com.example.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.UiState
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.VehicleProfile
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.PaymentTransactionRepository
import com.example.core.domain.repository.ReviewRepository
import com.example.core.domain.repository.UserProfileRepository
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.booking.domain.BookingCancellationCalculator
import com.example.feature.booking.domain.CancellationRefundQuote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BookingHistoryTab(val label: String) {
  UPCOMING("Upcoming"),
  ACTIVE("Active"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled"),
}

data class CustomerBookingDisplayItem(
  val booking: Booking,
  val spaceTitle: String,
  val areaLabel: String,
  val maskedCoordinatesLabel: String,
  val vehicleLabel: String,
  val vehiclePlateMasked: String,
  val unlockedGateSummary: String?,
  val unlockedExactCoordinatesLabel: String?,
  val simulatedQrPayload: String?,
  val simulatedPin: String?,
  val overstayMinutes: Long,
  val cancellationQuote: CancellationRefundQuote?,
  val hasSubmittedReview: Boolean = false,
)

data class CustomerBookingsScreenData(
  val selectedTab: BookingHistoryTab,
  val upcoming: List<CustomerBookingDisplayItem>,
  val active: List<CustomerBookingDisplayItem>,
  val completed: List<CustomerBookingDisplayItem>,
  val cancelled: List<CustomerBookingDisplayItem>,
  val availableDemoCreditPkr: Int = 0,
) {
  fun itemsForSelectedTab(): List<CustomerBookingDisplayItem> =
    when (selectedTab) {
      BookingHistoryTab.UPCOMING -> upcoming
      BookingHistoryTab.ACTIVE -> active
      BookingHistoryTab.COMPLETED -> completed
      BookingHistoryTab.CANCELLED -> cancelled
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BookingHistoryViewModel(
  private val parkingRepository: ParkingRepository,
  private val userProfileRepository: UserProfileRepository,
  private val bookingRepository: BookingRepository,
  private val demoSessionManager: DemoSessionManager,
  private val nowProvider: () -> Long = { System.currentTimeMillis() },
  private val reviewRepository: ReviewRepository? = null,
  private val paymentTransactionRepository: PaymentTransactionRepository? = null,
) : ViewModel() {

  private val _selectedTab = MutableStateFlow(BookingHistoryTab.UPCOMING)
  private val _actionFeedback = MutableStateFlow<String?>(null)
  val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

  private val bookingsFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val userId = session.activeUser?.id
      if (userId == null) flowOf(emptyList())
      else bookingRepository.observeBookingsForCustomer(userId)
    }

  private val vehiclesFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val userId = session.activeUser?.id
      if (userId == null) flowOf(emptyList())
      else userProfileRepository.observeVehiclesForUser(userId)
    }

  private val reviewedBookingIdsFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val userId = session.activeUser?.id
      if (userId == null || reviewRepository == null) flowOf(emptySet())
      else reviewRepository.observeReviewsByCustomer(userId).map { list -> list.map { it.bookingId }.toSet() }
    }

  private val availableDemoCreditFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val userId = session.activeUser?.id
      if (userId == null || paymentTransactionRepository == null) flowOf(0)
      else paymentTransactionRepository.observeAvailableDemoCreditPkr(userId)
    }

  private val allItemsFlow =
    combine(
      bookingsFlow,
      parkingRepository.observeAllSpacesIncludingPending(),
      vehiclesFlow,
      reviewedBookingIdsFlow,
    ) { bookings, spaces, vehicles, reviewedIds ->
      val spaceMap = spaces.associateBy { it.id }
      val vehicleMap = vehicles.associateBy { it.id }
      val nowMs = nowProvider()
      bookings.map { booking ->
        buildDisplayItem(
          booking = booking,
          space = spaceMap[booking.spaceId],
          vehicle = vehicleMap[booking.vehicleId],
          nowMs = nowMs,
          hasSubmittedReview = booking.id in reviewedIds,
        )
      }
    }

  val historyUiState: StateFlow<UiState<CustomerBookingsScreenData>> =
    combine(allItemsFlow, _selectedTab, availableDemoCreditFlow) { items, tab, demoCreditPkr ->
        val upcoming =
          items.filter {
            it.booking.status == BookingStatus.PENDING_PAYMENT ||
              it.booking.status == BookingStatus.CONFIRMED
          }
        val active = items.filter { it.booking.status == BookingStatus.CHECKED_IN }
        val completed =
          items.filter {
            it.booking.status == BookingStatus.CHECKED_OUT ||
              it.booking.status == BookingStatus.COMPLETED
          }
        val cancelled =
          items.filter {
            it.booking.status == BookingStatus.CANCELLED ||
              it.booking.status == BookingStatus.EXPIRED ||
              it.booking.status == BookingStatus.DISPUTED
          }

        UiState.Success(
          CustomerBookingsScreenData(
            selectedTab = tab,
            upcoming = upcoming,
            active = active,
            completed = completed,
            cancelled = cancelled,
            availableDemoCreditPkr = demoCreditPkr,
          )
        ) as UiState<CustomerBookingsScreenData>
      }
      .catch { err ->
        emit(
          UiState.Error(
            title = "Unable to Load Bookings",
            message = err.message ?: "Failed to load local Demo bookings.",
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
      )

  init {
    refreshExpiredBookings()
  }

  fun observeBookingDetail(bookingId: String): StateFlow<UiState<CustomerBookingDisplayItem>> =
    combine(
        demoSessionManager.session,
        bookingRepository.observeBookingById(bookingId),
        parkingRepository.observeAllSpacesIncludingPending(),
        vehiclesFlow,
        reviewedBookingIdsFlow,
      ) { session, booking, spaces, vehicles, reviewedIds ->
        val activeCustomerId = session.activeUser?.id
        when {
          booking == null ->
            UiState.Error(
              title = "Booking Not Found",
              message = "This Demo booking record could not be located.",
              retryable = false,
            )
          activeCustomerId == null || booking.customerId != activeCustomerId ->
            UiState.Error(
              title = "Account Access Restricted",
              message = "This booking belongs to a different Demo account.",
              retryable = false,
            )
          else -> {
            val space = spaces.firstOrNull { it.id == booking.spaceId }
            val vehicle = vehicles.firstOrNull { it.id == booking.vehicleId }
            UiState.Success(
              buildDisplayItem(
                booking = booking,
                space = space,
                vehicle = vehicle,
                nowMs = nowProvider(),
                hasSubmittedReview = booking.id in reviewedIds,
              )
            )
          }
        }
      }
      .catch { err ->
        emit(
          UiState.Error(
            title = "Booking Unavailable",
            message = err.message ?: "Unable to load booking confirmation details.",
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
      )

  val availableDemoCreditPkr: StateFlow<Int> =
    availableDemoCreditFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = 0,
    )

  fun onSelectTab(tab: BookingHistoryTab) {
    _selectedTab.value = tab
  }

  fun refreshExpiredBookings() {
    viewModelScope.launch {
      bookingRepository.expireStaleBookings(nowProvider())
    }
  }

  fun confirmPendingPayment(
    bookingId: String,
    paymentMethod: DemoPaymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
  ) {
    val customerId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      applyMutationResult(
        bookingRepository.confirmPendingBookingPayment(
          bookingId = bookingId,
          customerId = customerId,
          paymentMethod = paymentMethod,
          nowEpochMs = nowProvider(),
        )
      )
    }
  }

  fun checkIn(bookingId: String) {
    val customerId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      applyMutationResult(bookingRepository.checkInBooking(bookingId, customerId, nowProvider()))
    }
  }

  fun checkOut(bookingId: String) {
    val customerId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      applyMutationResult(bookingRepository.checkOutBooking(bookingId, customerId, nowProvider()))
    }
  }

  fun cancelBooking(bookingId: String) {
    val customerId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      applyMutationResult(bookingRepository.cancelBooking(bookingId, customerId, nowProvider()))
    }
  }

  fun dismissFeedback() {
    _actionFeedback.value = null
  }

  private fun applyMutationResult(result: BookingMutationResult) {
    _actionFeedback.value =
      when (result) {
        is BookingMutationResult.Success -> result.message
        is BookingMutationResult.Rejected -> result.reason
      }
  }

  private fun buildDisplayItem(
    booking: Booking,
    space: ParkingSpace?,
    vehicle: VehicleProfile?,
    nowMs: Long,
    hasSubmittedReview: Boolean = false,
  ): CustomerBookingDisplayItem {
    // Enforce exact gate & PIN isolation: revealed ONLY while Confirmed or Checked In
    val gateUnlocked = booking.hasActiveGateAccess()
    val unlockedGateSummary =
      if (gateUnlocked) space?.location?.privateGateSummary else null
    val unlockedCoords =
      if (gateUnlocked &&
        space?.location?.privateExactLatitude != null &&
        space.location.privateExactLongitude != null
      ) {
        "${"%.5f".format(space.location.privateExactLatitude)}, ${"%.5f".format(space.location.privateExactLongitude)}"
      } else {
        null
      }

    val pin =
      if (gateUnlocked) {
        booking.demoEntryPin ?: "DEMO-${booking.bookingCode.takeLast(4)}01"
      } else {
        null
      }
    val qrPayload =
      if (gateUnlocked) {
        "PARKSPACE_DEMO|${booking.bookingCode}|${booking.spaceId}|${booking.bayId}|$pin"
      } else {
        null
      }

    val maskedCoords =
      if (space != null) {
        "~${space.location.maskRadiusMeters}m zone (${"%.4f".format(space.location.publicMaskedLatitude)}, ${"%.4f".format(space.location.publicMaskedLongitude)})"
      } else {
        booking.city.displayName
      }

    val overstayMin =
      if (booking.status == BookingStatus.CHECKED_IN && nowMs > booking.endEpochMs) {
        ((nowMs - booking.endEpochMs) / 60_000L).coerceAtLeast(1L)
      } else {
        0L
      }

    val quote =
      if (booking.status in setOf(
          BookingStatus.PENDING_PAYMENT,
          BookingStatus.CONFIRMED,
          BookingStatus.CHECKED_IN,
        )
      ) {
        BookingCancellationCalculator.calculate(booking, space, nowMs)
      } else {
        null
      }

    return CustomerBookingDisplayItem(
      booking = booking,
      spaceTitle = space?.title ?: "Parking Space (${booking.spaceId})",
      areaLabel = space?.location?.areaLabel ?: booking.city.displayName,
      maskedCoordinatesLabel = maskedCoords,
      vehicleLabel = vehicle?.label ?: "Demo Vehicle",
      vehiclePlateMasked = vehicle?.plateNumberMasked ?: "••-••",
      unlockedGateSummary = unlockedGateSummary,
      unlockedExactCoordinatesLabel = unlockedCoords,
      simulatedQrPayload = qrPayload,
      simulatedPin = pin,
      overstayMinutes = overstayMin,
      cancellationQuote = quote,
      hasSubmittedReview = hasSubmittedReview,
    )
  }

  class Factory(
    private val container: ParkSpaceAppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(BookingHistoryViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return BookingHistoryViewModel(
        parkingRepository = container.parkingRepository,
        userProfileRepository = container.userProfileRepository,
        bookingRepository = container.bookingRepository,
        demoSessionManager = container.demoSessionManager,
        reviewRepository = container.reviewRepository,
        paymentTransactionRepository = container.paymentTransactionRepository,
      ) as T
    }
  }
}
