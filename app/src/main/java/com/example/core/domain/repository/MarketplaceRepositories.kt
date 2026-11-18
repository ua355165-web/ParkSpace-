package com.example.core.domain.repository

import com.example.core.domain.model.AppNotification
import com.example.core.domain.model.BookingDispute
import com.example.core.domain.model.ChatMessage
import com.example.core.domain.model.ChatThreadSummary
import com.example.core.domain.model.DemoPaymentTransaction
import com.example.core.domain.model.DemoReferralRecord
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.ParkingReview
import com.example.core.domain.model.SpaceRatingSummary
import kotlinx.coroutines.flow.Flow

sealed interface ChatMutationResult {
  data class Success(val message: ChatMessage) : ChatMutationResult

  data class Rejected(val reason: String) : ChatMutationResult
}

sealed interface ReviewMutationResult {
  data class Success(val review: ParkingReview, val message: String) : ReviewMutationResult

  data class Rejected(val reason: String) : ReviewMutationResult
}

sealed interface DisputeMutationResult {
  data class Success(val dispute: BookingDispute, val message: String) : DisputeMutationResult

  data class Rejected(val reason: String) : DisputeMutationResult
}

sealed interface ReferralMutationResult {
  data class Success(val record: DemoReferralRecord, val message: String) : ReferralMutationResult

  data class Rejected(val reason: String) : ReferralMutationResult
}

interface PaymentTransactionRepository {
  fun observeTransactionsForCustomer(customerId: String): Flow<List<DemoPaymentTransaction>>

  fun observeTransactionsForHost(hostId: String): Flow<List<DemoPaymentTransaction>>

  fun observeTransactionsForBooking(bookingId: String): Flow<List<DemoPaymentTransaction>>

  fun observeAvailableDemoCreditPkr(customerId: String): Flow<Int>
}

interface ChatRepository {
  fun observeThreadSummariesForUser(userId: String): Flow<List<ChatThreadSummary>>

  fun observeMessagesForBooking(bookingId: String, requesterUserId: String): Flow<List<ChatMessage>>

  suspend fun sendMessage(
    bookingId: String,
    senderUserId: String,
    body: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): ChatMutationResult

  suspend fun markThreadRead(bookingId: String, readerUserId: String)
}

interface ReviewRepository {
  fun observeReviewsForSpace(spaceId: String): Flow<List<ParkingReview>>

  fun observeSpaceRatingSummaries(): Flow<List<SpaceRatingSummary>>

  fun observeReviewsForHost(hostId: String): Flow<List<ParkingReview>>

  fun observeReviewsByCustomer(customerId: String): Flow<List<ParkingReview>>

  suspend fun submitReview(
    bookingId: String,
    customerId: String,
    rating: Int,
    comment: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): ReviewMutationResult

  suspend fun replyToReview(
    reviewId: String,
    hostId: String,
    reply: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): ReviewMutationResult
}

interface DisputeRepository {
  fun observeDisputesForCustomer(customerId: String): Flow<List<BookingDispute>>

  fun observeDisputesForHost(hostId: String): Flow<List<BookingDispute>>

  fun observeDisputesForBooking(bookingId: String): Flow<List<BookingDispute>>

  suspend fun fileDispute(
    bookingId: String,
    reporterUserId: String,
    category: DisputeCategory,
    description: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): DisputeMutationResult

  suspend fun respondToDisputeAsHost(
    disputeId: String,
    hostId: String,
    responseNote: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): DisputeMutationResult

  suspend fun resolveDisputeAsCustomerOrHost(
    disputeId: String,
    actorUserId: String,
    resolutionNote: String,
    targetStatus: DisputeStatus = DisputeStatus.RESOLVED,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): DisputeMutationResult
}

interface ReferralRepository {
  fun observeReferralsByReferrer(referrerUserId: String): Flow<List<DemoReferralRecord>>

  fun observeReferralAsReferred(referredUserId: String): Flow<DemoReferralRecord?>

  suspend fun redeemReferralCode(
    referredUserId: String,
    referralCode: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): ReferralMutationResult
}

interface NotificationRepository {
  fun observeNotificationsForUser(recipientUserId: String): Flow<List<AppNotification>>

  fun observeUnreadCount(recipientUserId: String): Flow<Int>

  suspend fun markAsRead(notificationId: String, recipientUserId: String)

  suspend fun markAllAsRead(recipientUserId: String)
}
