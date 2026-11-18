package com.example.feature.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.Booking
import com.example.core.domain.model.ChatMessage
import com.example.core.domain.model.ChatThreadSummary
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.ChatMutationResult
import com.example.core.domain.repository.ChatRepository
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

data class ChatInboxUiState(
  val threads: List<ChatThreadSummary> = emptyList(),
  val eligibleBookings: List<Pair<Booking, String>> = emptyList(),
)

data class BookingChatThreadUiState(
  val booking: Booking? = null,
  val spaceTitle: String = "Parking Booking",
  val currentUserId: String = "",
  val isAuthorized: Boolean = false,
  val messages: List<ChatMessage> = emptyList(),
  val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
  private val chatRepository: ChatRepository,
  private val bookingRepository: BookingRepository,
  private val parkingRepository: ParkingRepository,
  private val demoSessionManager: DemoSessionManager,
) : ViewModel() {

  private val _sendError = MutableStateFlow<String?>(null)
  val sendError: StateFlow<String?> = _sendError.asStateFlow()

  val inboxUiState: StateFlow<ChatInboxUiState> =
    demoSessionManager.session
      .flatMapLatest { session ->
        val user = session.activeUser
        if (user == null) {
          flowOf(ChatInboxUiState())
        } else {
          val bookingsFlow =
            when (user.role) {
              UserRole.CUSTOMER -> bookingRepository.observeBookingsForCustomer(user.id)
              UserRole.HOST -> bookingRepository.observeBookingsForHost(user.id)
              UserRole.SUPER_ADMIN -> flowOf(emptyList())
            }
          combine(
            chatRepository.observeThreadSummariesForUser(user.id),
            bookingsFlow,
            parkingRepository.observeAllSpacesIncludingPending(),
          ) { threads, bookings, spaces ->
            val spaceMap = spaces.associateBy { it.id }
            val eligible =
              bookings.map { b -> b to (spaceMap[b.spaceId]?.title ?: "Parking Space") }
            ChatInboxUiState(threads = threads, eligibleBookings = eligible)
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ChatInboxUiState(),
      )

  fun observeBookingThread(bookingId: String): StateFlow<BookingChatThreadUiState> =
    demoSessionManager.session
      .flatMapLatest { session ->
        val userId = session.activeUser?.id
        if (userId == null) {
          flowOf(BookingChatThreadUiState(errorMessage = "Sign in to view booking messages."))
        } else {
          combine(
            bookingRepository.observeBookingById(bookingId),
            parkingRepository.observeAllSpacesIncludingPending(),
            chatRepository.observeMessagesForBooking(bookingId, userId),
            _sendError,
          ) { booking, spaces, messages, sendErr ->
            val authorized =
              booking != null && (booking.customerId == userId || booking.hostId == userId)
            val spaceTitle = spaces.firstOrNull { it.id == booking?.spaceId }?.title ?: "Parking Space"
            BookingChatThreadUiState(
              booking = booking,
              spaceTitle = spaceTitle,
              currentUserId = userId,
              isAuthorized = authorized,
              messages = if (authorized) messages else emptyList(),
              errorMessage = sendErr,
            )
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BookingChatThreadUiState(),
      )

  fun markThreadRead(bookingId: String) {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      chatRepository.markThreadRead(bookingId, userId)
    }
  }

  fun sendMessage(bookingId: String, body: String, onSent: () -> Unit = {}) {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = chatRepository.sendMessage(bookingId, userId, body)) {
        is ChatMutationResult.Success -> {
          _sendError.value = null
          onSent()
        }
        is ChatMutationResult.Rejected -> {
          _sendError.value = res.reason
        }
      }
    }
  }

  fun clearError() {
    _sendError.value = null
  }

  class Factory(private val container: ParkSpaceAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      ChatViewModel(
        chatRepository = container.chatRepository,
        bookingRepository = container.bookingRepository,
        parkingRepository = container.parkingRepository,
        demoSessionManager = container.demoSessionManager,
      ) as T
  }
}
