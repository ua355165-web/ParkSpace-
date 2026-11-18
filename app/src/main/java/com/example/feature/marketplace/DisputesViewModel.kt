package com.example.feature.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingDispute
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.DisputeMutationResult
import com.example.core.domain.repository.DisputeRepository
import com.example.core.domain.repository.ParkingRepository
import com.example.demo.sandbox.DemoSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisputesCenterUiState(
  val activeRole: UserRole? = null,
  val disputes: List<BookingDispute> = emptyList(),
  val eligibleBookings: List<Pair<Booking, String>> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class DisputesViewModel(
  private val disputeRepository: DisputeRepository,
  private val bookingRepository: BookingRepository,
  private val parkingRepository: ParkingRepository,
  private val demoSessionManager: DemoSessionManager,
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  val uiState: StateFlow<DisputesCenterUiState> =
    demoSessionManager.session
      .flatMapLatest { session ->
        val user = session.activeUser
        if (user == null) {
          flowOf(DisputesCenterUiState())
        } else {
          val disputesFlow =
            when (user.role) {
              UserRole.CUSTOMER -> disputeRepository.observeDisputesForCustomer(user.id)
              UserRole.HOST -> disputeRepository.observeDisputesForHost(user.id)
              UserRole.SUPER_ADMIN -> flowOf(emptyList())
            }
          val bookingsFlow =
            when (user.role) {
              UserRole.CUSTOMER -> bookingRepository.observeBookingsForCustomer(user.id)
              UserRole.HOST -> bookingRepository.observeBookingsForHost(user.id)
              UserRole.SUPER_ADMIN -> flowOf(emptyList())
            }
          combine(
            disputesFlow,
            bookingsFlow,
            parkingRepository.observeAllSpacesIncludingPending(),
          ) { disputes, bookings, spaces ->
            val spaceMap = spaces.associateBy { it.id }
            DisputesCenterUiState(
              activeRole = user.role,
              disputes = disputes,
              eligibleBookings = bookings.map { b -> b to (spaceMap[b.spaceId]?.title ?: "Parking Space") },
            )
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DisputesCenterUiState(),
      )

  fun fileDispute(
    bookingId: String,
    category: DisputeCategory,
    description: String,
    onSuccess: () -> Unit = {},
  ) {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = disputeRepository.fileDispute(bookingId, userId, category, description)) {
        is DisputeMutationResult.Success -> {
          _feedback.value = res.message
          onSuccess()
        }
        is DisputeMutationResult.Rejected -> {
          _feedback.value = res.reason
        }
      }
    }
  }

  fun respondAsHost(disputeId: String, responseNote: String, onSuccess: () -> Unit = {}) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = disputeRepository.respondToDisputeAsHost(disputeId, hostId, responseNote)) {
        is DisputeMutationResult.Success -> {
          _feedback.value = res.message
          onSuccess()
        }
        is DisputeMutationResult.Rejected -> {
          _feedback.value = res.reason
        }
      }
    }
  }

  fun resolveDispute(
    disputeId: String,
    resolutionNote: String,
    targetStatus: DisputeStatus = DisputeStatus.RESOLVED,
    onSuccess: () -> Unit = {},
  ) {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (
        val res =
          disputeRepository.resolveDisputeAsCustomerOrHost(
            disputeId = disputeId,
            actorUserId = userId,
            resolutionNote = resolutionNote,
            targetStatus = targetStatus,
          )
      ) {
        is DisputeMutationResult.Success -> {
          _feedback.value = res.message
          onSuccess()
        }
        is DisputeMutationResult.Rejected -> {
          _feedback.value = res.reason
        }
      }
    }
  }

  fun dismissFeedback() {
    _feedback.value = null
  }

  class Factory(private val container: ParkSpaceAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      DisputesViewModel(
        disputeRepository = container.disputeRepository,
        bookingRepository = container.bookingRepository,
        parkingRepository = container.parkingRepository,
        demoSessionManager = container.demoSessionManager,
      ) as T
  }
}
