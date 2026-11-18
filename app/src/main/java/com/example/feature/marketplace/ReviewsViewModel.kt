package com.example.feature.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.ParkingReview
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.ReviewMutationResult
import com.example.core.domain.repository.ReviewRepository
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

data class HostReviewItem(
  val review: ParkingReview,
  val spaceTitle: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModel(
  private val reviewRepository: ReviewRepository,
  private val parkingRepository: ParkingRepository,
  private val demoSessionManager: DemoSessionManager,
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  val hostReviewsUiState: StateFlow<List<HostReviewItem>> =
    demoSessionManager.session
      .flatMapLatest { session ->
        val hostId = session.activeUser?.id
        if (hostId == null) {
          flowOf(emptyList())
        } else {
          combine(
            reviewRepository.observeReviewsForHost(hostId),
            parkingRepository.observeSpacesByHost(hostId),
          ) { reviews, spaces ->
            val spaceMap = spaces.associateBy { it.id }
            reviews.map { r ->
              HostReviewItem(review = r, spaceTitle = spaceMap[r.spaceId]?.title ?: "Parking Space")
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
      )

  fun observeReviewsForSpace(spaceId: String): StateFlow<List<ParkingReview>> =
    reviewRepository
      .observeReviewsForSpace(spaceId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
      )

  fun submitReview(bookingId: String, rating: Int, comment: String, onSuccess: () -> Unit = {}) {
    val customerId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = reviewRepository.submitReview(bookingId, customerId, rating, comment)) {
        is ReviewMutationResult.Success -> {
          _feedback.value = res.message
          onSuccess()
        }
        is ReviewMutationResult.Rejected -> {
          _feedback.value = res.reason
        }
      }
    }
  }

  fun replyToReview(reviewId: String, reply: String, onSuccess: () -> Unit = {}) {
    val hostId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = reviewRepository.replyToReview(reviewId, hostId, reply)) {
        is ReviewMutationResult.Success -> {
          _feedback.value = res.message
          onSuccess()
        }
        is ReviewMutationResult.Rejected -> {
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
      ReviewsViewModel(
        reviewRepository = container.reviewRepository,
        parkingRepository = container.parkingRepository,
        demoSessionManager = container.demoSessionManager,
      ) as T
  }
}
