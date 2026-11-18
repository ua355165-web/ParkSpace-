package com.example.feature.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.AppNotification
import com.example.core.domain.repository.NotificationRepository
import com.example.demo.sandbox.DemoSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
  val notifications: List<AppNotification> = emptyList(),
  val unreadCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
  private val notificationRepository: NotificationRepository,
  private val demoSessionManager: DemoSessionManager,
) : ViewModel() {

  val uiState: StateFlow<NotificationsUiState> =
    demoSessionManager.session
      .flatMapLatest { session ->
        val userId = session.activeUser?.id
        if (userId == null) {
          flowOf(NotificationsUiState())
        } else {
          combine(
            notificationRepository.observeNotificationsForUser(userId),
            notificationRepository.observeUnreadCount(userId),
          ) { list, unread ->
            NotificationsUiState(notifications = list, unreadCount = unread)
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = NotificationsUiState(),
      )

  fun markAsRead(notificationId: String) {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      notificationRepository.markAsRead(notificationId, userId)
    }
  }

  fun markAllAsRead() {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      notificationRepository.markAllAsRead(userId)
    }
  }

  class Factory(private val container: ParkSpaceAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      NotificationsViewModel(
        notificationRepository = container.notificationRepository,
        demoSessionManager = container.demoSessionManager,
      ) as T
  }
}
