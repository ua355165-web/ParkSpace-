package com.example.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.UiState
import com.example.core.config.BookingType
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.Booking
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.VehicleProfile
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.AdminRepository
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.PaymentTransactionRepository
import com.example.core.domain.repository.UserProfileRepository
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.booking.domain.AvailabilityCheckResult
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.booking.domain.BookingPriceBreakdown
import com.example.feature.booking.domain.BookingPricingEngine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingCheckoutFormState(
  val selectedVehicleId: String? = null,
  val bookingType: BookingType = BookingType.HOURLY,
  val dayOffset: Int = 0,
  val startHour: Int = 10,
  val startMinute: Int = 0,
  val units: Int = 2,
  val confirmDemoPaymentImmediately: Boolean = true,
  val selectedPaymentMethod: DemoPaymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
  val isSubmitting: Boolean = false,
  val submissionError: String? = null,
)

data class BookingCheckoutUiData(
  val publicSpace: ParkingSpace,
  val maskedLocation: ParkingLocation,
  val vehicles: List<VehicleProfile>,
  val selectedVehicle: VehicleProfile?,
  val form: BookingCheckoutFormState,
  val startEpochMs: Long,
  val endEpochMs: Long,
  val selectedDateLabel: String,
  val pricing: BookingPriceBreakdown?,
  val pricingError: String?,
  val availability: AvailabilityCheckResult?,
  val availableDemoCreditPkr: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookingCheckoutViewModel(
  private val spaceId: String,
  private val parkingRepository: ParkingRepository,
  private val userProfileRepository: UserProfileRepository,
  private val bookingRepository: BookingRepository,
  private val demoSessionManager: DemoSessionManager,
  private val nowProvider: () -> Long = { System.currentTimeMillis() },
  private val paymentTransactionRepository: PaymentTransactionRepository? = null,
  private val adminRepository: AdminRepository? = null,
) : ViewModel() {

  private val _form = MutableStateFlow(initialFormState(nowProvider()))
  private var initializedSlotFromSpace = false

  private val customerVehiclesFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val userId = session.activeUser?.id
      if (userId == null) flowOf(emptyList())
      else userProfileRepository.observeVehiclesForUser(userId)
    }

  private val availableDemoCreditFlow =
    demoSessionManager.session.flatMapLatest { session ->
      val userId = session.activeUser?.id
      if (userId == null || paymentTransactionRepository == null) flowOf(0)
      else paymentTransactionRepository.observeAvailableDemoCreditPkr(userId)
    }

  private val commissionRuleFlow =
    adminRepository?.observeActiveCommissionRule() ?: flowOf(DemoCommissionRule.DEFAULT)

  private val coreDataFlow =
    combine(
      combine(
        parkingRepository.observeAllApprovedSpaces(),
        parkingRepository.observeBaysForSpace(spaceId),
        bookingRepository.observeBookingsForSpace(spaceId),
      ) { spaces, bays, spaceBookings -> Triple(spaces, bays, spaceBookings) },
      combine(
        customerVehiclesFlow,
        availableDemoCreditFlow,
        commissionRuleFlow,
      ) { vehicles, demoCreditPkr, commissionRule -> Triple(vehicles, demoCreditPkr, commissionRule) },
    ) { (spaces, bays, spaceBookings), (vehicles, demoCreditPkr, commissionRule) ->
      CoreCheckoutSnapshot(
        space = spaces.firstOrNull { it.id == spaceId },
        bays = bays,
        existingBookings = spaceBookings,
        vehicles = vehicles,
        availableDemoCreditPkr = demoCreditPkr,
        commissionRule = commissionRule,
      )
    }

  val uiState: StateFlow<UiState<BookingCheckoutUiData>> =
    combine(coreDataFlow, _form) { snapshot, form ->
        val rawSpace = snapshot.space
          ?: return@combine UiState.Error(
            title = "Listing Unavailable",
            message = "This parking listing could not be found or is no longer approved.",
            retryable = false,
          )

        // Strict privacy rule: never expose exact gate coordinates or instructions before booking confirmation.
        val maskedLocation = rawSpace.location.toPublicMaskedLocation()
        val publicSpace = rawSpace.copy(location = maskedLocation)

        if (!initializedSlotFromSpace) {
          initializedSlotFromSpace = true
          syncDefaultSlotForSchedule(rawSpace, nowProvider())
        }

        val vehicles = snapshot.vehicles
        val selectedVehicle =
          vehicles.firstOrNull { it.id == form.selectedVehicleId }
            ?: vehicles.firstOrNull { it.isDefault && it.vehicleType in rawSpace.supportedVehicles }
            ?: vehicles.firstOrNull { it.vehicleType in rawSpace.supportedVehicles }
            ?: vehicles.firstOrNull()

        val activeType =
          if (form.bookingType == BookingType.MONTHLY && !rawSpace.monthlyEnabled) {
            BookingType.HOURLY
          } else {
            form.bookingType
          }
        val clampedUnits = clampUnitsForType(activeType, form.units)
        val effectiveForm =
          form.copy(
            selectedVehicleId = selectedVehicle?.id,
            bookingType = activeType,
            units = clampedUnits,
          )

        val nowMs = nowProvider()
        val startZdt = computeStartZonedDateTime(effectiveForm, nowMs)
        val startEpochMs = startZdt.toInstant().toEpochMilli()

        val pricingResult =
          BookingPricingEngine.calculate(
            space = publicSpace,
            bookingType = activeType,
            units = clampedUnits,
            commissionRule = snapshot.commissionRule,
          )
        val pricing = pricingResult.getOrNull()
        val pricingError = pricingResult.exceptionOrNull()?.message

        val endEpochMs =
          if (pricing != null) startEpochMs + pricing.durationMillis
          else startEpochMs + Booking.MILLIS_PER_HOUR

        val activeCustomerId = demoSessionManager.session.value.activeUser?.id ?: ""
        val availability =
          if (selectedVehicle != null && pricing != null && activeCustomerId.isNotBlank()) {
            BookingAvailabilityValidator.validate(
              space = publicSpace,
              bays = snapshot.bays,
              existingBookingsForSpace = snapshot.existingBookings,
              vehicle = selectedVehicle,
              bookingType = activeType,
              startEpochMs = startEpochMs,
              endEpochMs = endEpochMs,
              nowEpochMs = nowMs,
              customerId = activeCustomerId,
            )
          } else {
            null
          }

        UiState.Success(
          BookingCheckoutUiData(
            publicSpace = publicSpace,
            maskedLocation = maskedLocation,
            vehicles = vehicles,
            selectedVehicle = selectedVehicle,
            form = effectiveForm,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            selectedDateLabel = startZdt.toLocalDate().toString(),
            pricing = pricing,
            pricingError = pricingError,
            availability = availability,
            availableDemoCreditPkr = snapshot.availableDemoCreditPkr,
          )
        ) as UiState<BookingCheckoutUiData>
      }
      .catch { err ->
        emit(
          UiState.Error(
            title = "Booking Unavailable",
            message = err.message ?: "Unable to prepare booking details.",
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
      )

  fun onSelectVehicle(vehicleId: String) {
    _form.update { it.copy(selectedVehicleId = vehicleId, submissionError = null) }
  }

  fun onSelectBookingType(bookingType: BookingType) {
    _form.update { current ->
      val defaultUnits =
        when (bookingType) {
          BookingType.HOURLY -> 2
          BookingType.DAILY -> 1
          BookingType.MONTHLY -> 1
        }
      current.copy(bookingType = bookingType, units = defaultUnits, submissionError = null)
    }
  }

  fun onSelectDayOffset(offset: Int) {
    _form.update {
      it.copy(dayOffset = offset.coerceIn(0, 30), submissionError = null)
    }
  }

  fun onAdjustStartHour(delta: Int) {
    _form.update { current ->
      val nextHour = (current.startHour + delta + 24) % 24
      current.copy(startHour = nextHour, submissionError = null)
    }
  }

  fun onSetStartTime(hour: Int, minute: Int) {
    _form.update {
      it.copy(
        startHour = hour.coerceIn(0, 23),
        startMinute = if (minute == 30) 30 else 0,
        submissionError = null,
      )
    }
  }

  fun onToggleStartMinute() {
    _form.update {
      it.copy(startMinute = if (it.startMinute == 0) 30 else 0, submissionError = null)
    }
  }

  fun onAdjustUnits(delta: Int) {
    _form.update { current ->
      val next = clampUnitsForType(current.bookingType, current.units + delta)
      current.copy(units = next, submissionError = null)
    }
  }

  fun onSetUnits(units: Int) {
    _form.update { current ->
      current.copy(units = clampUnitsForType(current.bookingType, units), submissionError = null)
    }
  }

  fun onToggleImmediateDemoPayment(immediate: Boolean) {
    _form.update { it.copy(confirmDemoPaymentImmediately = immediate, submissionError = null) }
  }

  fun onSelectPaymentMethod(method: DemoPaymentMethod) {
    _form.update { it.copy(selectedPaymentMethod = method, submissionError = null) }
  }

  fun clearSubmissionError() {
    _form.update { it.copy(submissionError = null) }
  }

  fun addVehicle(
    vehicleType: VehicleType,
    label: String,
    plateNumber: String,
    onResult: (Result<VehicleProfile>) -> Unit = {},
  ) {
    val customerId = demoSessionManager.session.value.activeUser?.id
    if (customerId.isNullOrBlank()) {
      onResult(Result.failure(IllegalStateException("Sign in as a Customer first.")))
      return
    }
    viewModelScope.launch {
      val result =
        userProfileRepository.addVehicleForUser(
          ownerUserId = customerId,
          vehicleType = vehicleType,
          label = label,
          plateNumber = plateNumber,
          makeDefault = true,
        )
      result.onSuccess { created ->
        _form.update { it.copy(selectedVehicleId = created.id, submissionError = null) }
      }
      result.onFailure { err ->
        _form.update { it.copy(submissionError = err.message) }
      }
      onResult(result)
    }
  }

  fun confirmBooking(onCreated: (String) -> Unit) {
    val currentState = (uiState.value as? UiState.Success)?.data ?: return
    if (currentState.form.isSubmitting) return

    val customerId = demoSessionManager.session.value.activeUser?.id
    val vehicle = currentState.selectedVehicle
    if (customerId.isNullOrBlank() || vehicle == null) {
      _form.update {
        it.copy(submissionError = "Select an active Customer Demo profile and vehicle first.")
      }
      return
    }

    _form.update { it.copy(isSubmitting = true, submissionError = null) }
    viewModelScope.launch {
      val result =
        bookingRepository.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = currentState.publicSpace.id,
            customerId = customerId,
            vehicleId = vehicle.id,
            bookingType = currentState.form.bookingType,
            units = currentState.form.units,
            startEpochMs = currentState.startEpochMs,
            confirmDemoPaymentImmediately = currentState.form.confirmDemoPaymentImmediately,
            paymentMethod = currentState.form.selectedPaymentMethod,
            nowEpochMs = nowProvider(),
          )
        )

      when (result) {
        is BookingMutationResult.Success -> {
          _form.update { it.copy(isSubmitting = false, submissionError = null) }
          onCreated(result.booking.id)
        }
        is BookingMutationResult.Rejected -> {
          _form.update {
            it.copy(isSubmitting = false, submissionError = result.reason)
          }
        }
      }
    }
  }

  private fun syncDefaultSlotForSchedule(space: ParkingSpace, nowMs: Long) {
    val nowZdt = Instant.ofEpochMilli(nowMs).atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
    var candidateHour = (nowZdt.hour + 1)
    var candidateDayOffset = 0
    if (candidateHour >= 22 || (!space.schedule.is24Hours && candidateHour + 2 > space.schedule.closeHour)) {
      candidateDayOffset = 1
      candidateHour = if (space.schedule.is24Hours) 9 else space.schedule.openHour.coerceAtLeast(8)
    } else if (!space.schedule.is24Hours && candidateHour < space.schedule.openHour) {
      candidateHour = space.schedule.openHour
    }
    _form.update {
      it.copy(
        dayOffset = candidateDayOffset,
        startHour = candidateHour.coerceIn(0, 23),
        startMinute = 0,
      )
    }
  }

  private fun initialFormState(nowMs: Long): BookingCheckoutFormState {
    val zdt = Instant.ofEpochMilli(nowMs).atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
    val nextHour = zdt.hour + 1
    return if (nextHour >= 22) {
      BookingCheckoutFormState(dayOffset = 1, startHour = 9, startMinute = 0)
    } else {
      BookingCheckoutFormState(dayOffset = 0, startHour = nextHour, startMinute = 0)
    }
  }

  private fun computeStartZonedDateTime(
    form: BookingCheckoutFormState,
    nowMs: Long,
  ): ZonedDateTime {
    val zone = BookingAvailabilityValidator.PAKISTAN_ZONE
    val baseDate: LocalDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val targetDate = baseDate.plusDays(form.dayOffset.toLong())
    val targetTime = LocalTime.of(form.startHour.coerceIn(0, 23), if (form.startMinute == 30) 30 else 0)
    return ZonedDateTime.of(targetDate, targetTime, zone)
  }

  private fun clampUnitsForType(type: BookingType, units: Int): Int =
    when (type) {
      BookingType.HOURLY -> units.coerceIn(1, 23)
      BookingType.DAILY -> units.coerceIn(1, 29)
      BookingType.MONTHLY -> units.coerceIn(1, 12)
    }

  private data class CoreCheckoutSnapshot(
    val space: ParkingSpace?,
    val bays: List<ParkingBay>,
    val existingBookings: List<Booking>,
    val vehicles: List<VehicleProfile>,
    val availableDemoCreditPkr: Int = 0,
    val commissionRule: DemoCommissionRule = DemoCommissionRule.DEFAULT,
  )

  class Factory(
    private val spaceId: String,
    private val container: ParkSpaceAppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(BookingCheckoutViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return BookingCheckoutViewModel(
        spaceId = spaceId,
        parkingRepository = container.parkingRepository,
        userProfileRepository = container.userProfileRepository,
        bookingRepository = container.bookingRepository,
        demoSessionManager = container.demoSessionManager,
        paymentTransactionRepository = container.paymentTransactionRepository,
        adminRepository = container.adminRepository,
      ) as T
    }
  }
}
