package com.example.core.data.demo

import androidx.room.withTransaction
import com.example.core.config.UserRole
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.entity.DemoChatMessageEntity
import com.example.core.database.entity.DemoDisputeEntity
import com.example.core.database.entity.DemoNotificationEntity
import com.example.core.database.entity.DemoPaymentTransactionEntity
import com.example.core.database.entity.DemoReferralEntity
import com.example.core.database.entity.DemoReviewEntity
import com.example.core.database.mapper.toDomain
import com.example.core.domain.model.AppNotification
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingDispute
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.ChatMessage
import com.example.core.domain.model.ChatThreadSummary
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DemoPaymentTransaction
import com.example.core.domain.model.DemoPaymentTransactionType
import com.example.core.domain.model.DemoReferralRecord
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.ParkSpaceNotificationType
import com.example.core.domain.model.ParkingReview
import com.example.core.domain.model.ReferralRewardStatus
import com.example.core.domain.model.SpaceRatingSummary
import com.example.core.domain.repository.ChatMutationResult
import com.example.core.domain.repository.ChatRepository
import com.example.core.domain.repository.DisputeMutationResult
import com.example.core.domain.repository.DisputeRepository
import com.example.core.domain.repository.NotificationRepository
import com.example.core.domain.repository.PaymentTransactionRepository
import com.example.core.domain.repository.ReferralMutationResult
import com.example.core.domain.repository.ReferralRepository
import com.example.core.domain.repository.ReviewMutationResult
import com.example.core.domain.repository.ReviewRepository
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Coordinates strictly event-driven Demo payment transactions, referral unlocks,
 * and recipient-scoped notifications whenever genuine user actions occur.
 */
internal object DemoMarketplaceEvents {
  const val REFERRAL_REWARD_PKR = 150

  fun computeDemoCreditBalancePkr(transactions: List<DemoPaymentTransactionEntity>): Int {
    val credited =
      transactions
        .filter { it.transactionType == DemoPaymentTransactionType.REFERRAL_DEMO_CREDIT }
        .sumOf { it.amountPkr }
    val spent =
      transactions
        .filter {
          it.transactionType == DemoPaymentTransactionType.ESCROW_PAYMENT &&
            it.method == DemoPaymentMethod.DEMO_CREDIT_WALLET
        }
        .sumOf { it.amountPkr }
    val refundedToCredits =
      transactions
        .filter {
          it.transactionType == DemoPaymentTransactionType.CUSTOMER_REFUND &&
            it.method == DemoPaymentMethod.DEMO_CREDIT_WALLET
        }
        .sumOf { it.amountPkr }
    return (credited - spent + refundedToCredits).coerceAtLeast(0)
  }

  suspend fun getAvailableDemoCreditPkr(
    database: ParkSpaceDemoDatabase,
    customerId: String,
  ): Int {
    val txs = database.paymentTransactionDao().getTransactionsForCustomerOnce(customerId)
    return computeDemoCreditBalancePkr(txs)
  }

  suspend fun onBookingCreated(
    database: ParkSpaceDemoDatabase?,
    booking: Booking,
    spaceTitle: String,
    nowEpochMs: Long,
  ) {
    if (database == null) return
    val notifDao = database.notificationDao()
    if (booking.status == BookingStatus.CONFIRMED) {
      val method = booking.demoPaymentMethod ?: DemoPaymentMethod.JAZZCASH_DEMO
      recordEscrowPaymentAndUnlockReferral(database, booking, spaceTitle, method, nowEpochMs)
    } else {
      notifDao.insertNotification(
        buildNotification(
          recipientUserId = booking.customerId,
          type = ParkSpaceNotificationType.BOOKING_UPDATE,
          title = "Demo Booking Saved (${booking.bookingCode})",
          message = "Complete simulated Demo payment to unlock exact gate details for $spaceTitle.",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )
      notifDao.insertNotification(
        buildNotification(
          recipientUserId = booking.hostId,
          type = ParkSpaceNotificationType.BOOKING_UPDATE,
          title = "New Pending Booking (${booking.bookingCode})",
          message = "A customer created a pending booking at $spaceTitle.",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )
    }
  }

  suspend fun onBookingPaymentConfirmed(
    database: ParkSpaceDemoDatabase?,
    booking: Booking,
    spaceTitle: String,
    nowEpochMs: Long,
  ) {
    if (database == null) return
    val method = booking.demoPaymentMethod ?: DemoPaymentMethod.JAZZCASH_DEMO
    recordEscrowPaymentAndUnlockReferral(database, booking, spaceTitle, method, nowEpochMs)
  }

  private suspend fun recordEscrowPaymentAndUnlockReferral(
    database: ParkSpaceDemoDatabase,
    booking: Booking,
    spaceTitle: String,
    method: DemoPaymentMethod,
    nowEpochMs: Long,
  ) {
    val txRef = "DEMO-TX-${abs((booking.id + nowEpochMs).hashCode()) % 900000 + 100000}"
    database.paymentTransactionDao().insertTransaction(
      DemoPaymentTransactionEntity(
        id = "tx-${UUID.randomUUID().toString().take(10)}",
        bookingId = booking.id,
        bookingCode = booking.bookingCode,
        customerId = booking.customerId,
        hostId = booking.hostId,
        spaceId = booking.spaceId,
        method = method,
        transactionType = DemoPaymentTransactionType.ESCROW_PAYMENT,
        amountPkr = booking.totalAmountPkr,
        referenceCode = txRef,
        summary = "Simulated Demo Escrow (${method.displayName}) — No real money transferred",
        isDemoOnly = true,
        createdAtEpochMs = nowEpochMs,
      )
    )

    val notifDao = database.notificationDao()
    notifDao.insertNotification(
      buildNotification(
        recipientUserId = booking.customerId,
        type = ParkSpaceNotificationType.PAYMENT_DEMO,
        title = "Demo Booking Confirmed (${booking.bookingCode})",
        message = "Simulated Rs. ${booking.totalAmountPkr} via ${method.displayName} for $spaceTitle. Gate PIN unlocked.",
        bookingId = booking.id,
        spaceId = booking.spaceId,
        nowEpochMs = nowEpochMs,
      )
    )
    notifDao.insertNotification(
      buildNotification(
        recipientUserId = booking.hostId,
        type = ParkSpaceNotificationType.BOOKING_UPDATE,
        title = "New Confirmed Booking (${booking.bookingCode})",
        message = "Customer confirmed booking at $spaceTitle (Rs. ${booking.baseAmountPkr} Demo escrow).",
        bookingId = booking.id,
        spaceId = booking.spaceId,
        nowEpochMs = nowEpochMs,
      )
    )

    // Unlock any pending referral reward upon customer's first confirmed Demo booking
    val referralDao = database.referralDao()
    val pendingRef = referralDao.getReferralAsReferredOnce(booking.customerId)
    if (pendingRef != null && pendingRef.status == ReferralRewardStatus.PENDING_FIRST_BOOKING) {
      val credited =
        pendingRef.copy(
          status = ReferralRewardStatus.CREDITED_DEMO,
          creditedAtEpochMs = nowEpochMs,
        )
      referralDao.updateReferral(credited)

      val creditRefCode = "DEMO-REF-${pendingRef.referralCodeUsed}"
      listOf(pendingRef.referrerUserId, pendingRef.referredUserId).forEach { beneficiaryId ->
        database.paymentTransactionDao().insertTransaction(
          DemoPaymentTransactionEntity(
            id = "tx-ref-${UUID.randomUUID().toString().take(8)}",
            bookingId = booking.id,
            bookingCode = booking.bookingCode,
            customerId = beneficiaryId,
            hostId = null,
            spaceId = null,
            method = DemoPaymentMethod.DEMO_CREDIT_WALLET,
            transactionType = DemoPaymentTransactionType.REFERRAL_DEMO_CREDIT,
            amountPkr = pendingRef.rewardAmountPkr,
            referenceCode = creditRefCode,
            summary = "Simulated Demo Referral Credit (${pendingRef.referralCodeUsed}) — Local Demo Only",
            isDemoOnly = true,
            createdAtEpochMs = nowEpochMs,
          )
        )
        notifDao.insertNotification(
          buildNotification(
            recipientUserId = beneficiaryId,
            type = ParkSpaceNotificationType.REFERRAL_UPDATE,
            title = "Rs. ${pendingRef.rewardAmountPkr} Demo Credit Unlocked",
            message = "Referral reward (${pendingRef.referralCodeUsed}) activated after qualifying Demo booking.",
            bookingId = booking.id,
            spaceId = null,
            nowEpochMs = nowEpochMs,
          )
        )
      }
    }
  }

  suspend fun onBookingCheckedIn(
    database: ParkSpaceDemoDatabase?,
    booking: Booking,
    byHost: Boolean,
    nowEpochMs: Long,
  ) {
    if (database == null) return
    val recipient = if (byHost) booking.customerId else booking.hostId
    val actorLabel = if (byHost) "Host marked arrival" else "Customer checked in"
    database.notificationDao().insertNotification(
      buildNotification(
        recipientUserId = recipient,
        type = ParkSpaceNotificationType.BOOKING_UPDATE,
        title = "Check-In Recorded (${booking.bookingCode})",
        message = "$actorLabel for booking ${booking.bookingCode}.",
        bookingId = booking.id,
        spaceId = booking.spaceId,
        nowEpochMs = nowEpochMs,
      )
    )
  }

  suspend fun onBookingCheckedOut(
    database: ParkSpaceDemoDatabase?,
    booking: Booking,
    nowEpochMs: Long,
  ) {
    if (database == null) return
    val txRef = "DEMO-SET-${abs((booking.id + nowEpochMs).hashCode()) % 900000 + 100000}"
    database.paymentTransactionDao().insertTransaction(
      DemoPaymentTransactionEntity(
        id = "tx-${UUID.randomUUID().toString().take(10)}",
        bookingId = booking.id,
        bookingCode = booking.bookingCode,
        customerId = booking.customerId,
        hostId = booking.hostId,
        spaceId = booking.spaceId,
        method = booking.demoPaymentMethod ?: DemoPaymentMethod.JAZZCASH_DEMO,
        transactionType = DemoPaymentTransactionType.HOST_SETTLEMENT,
        amountPkr = booking.baseAmountPkr,
        referenceCode = txRef,
        summary = "Simulated Demo Host Settlement (Rs. ${booking.baseAmountPkr} net after Rs. ${booking.serviceFeePkr} Demo fee)",
        isDemoOnly = true,
        createdAtEpochMs = nowEpochMs,
      )
    )
    database.notificationDao().insertNotification(
      buildNotification(
        recipientUserId = booking.customerId,
        type = ParkSpaceNotificationType.BOOKING_UPDATE,
        title = "Session Checked Out (${booking.bookingCode})",
        message = "Thanks for parking! You can now leave a verified review for this space.",
        bookingId = booking.id,
        spaceId = booking.spaceId,
        nowEpochMs = nowEpochMs,
      )
    )
    database.notificationDao().insertNotification(
      buildNotification(
        recipientUserId = booking.hostId,
        type = ParkSpaceNotificationType.PAYMENT_DEMO,
        title = "Demo Settlement Posted (${booking.bookingCode})",
        message = "Simulated Host net Rs. ${booking.baseAmountPkr} settled in Demo ledger.",
        bookingId = booking.id,
        spaceId = booking.spaceId,
        nowEpochMs = nowEpochMs,
      )
    )
  }

  suspend fun onBookingCancelled(
    database: ParkSpaceDemoDatabase?,
    booking: Booking,
    byHost: Boolean,
    nowEpochMs: Long,
  ) {
    if (database == null) return
    val refundPkr = booking.refundAmountPkr ?: 0
    if (refundPkr > 0) {
      val txRef = "DEMO-RFD-${abs((booking.id + nowEpochMs).hashCode()) % 900000 + 100000}"
      database.paymentTransactionDao().insertTransaction(
        DemoPaymentTransactionEntity(
          id = "tx-${UUID.randomUUID().toString().take(10)}",
          bookingId = booking.id,
          bookingCode = booking.bookingCode,
          customerId = booking.customerId,
          hostId = booking.hostId,
          spaceId = booking.spaceId,
          method = booking.demoPaymentMethod ?: DemoPaymentMethod.JAZZCASH_DEMO,
          transactionType = DemoPaymentTransactionType.CUSTOMER_REFUND,
          amountPkr = refundPkr,
          referenceCode = txRef,
          summary = "Simulated Demo Refund (Rs. $refundPkr) — Local Demo Only",
          isDemoOnly = true,
          createdAtEpochMs = nowEpochMs,
        )
      )
    }
    val recipient = if (byHost) booking.customerId else booking.hostId
    val title = if (byHost) "Booking Cancelled by Host (${booking.bookingCode})" else "Customer Cancelled (${booking.bookingCode})"
    database.notificationDao().insertNotification(
      buildNotification(
        recipientUserId = recipient,
        type = ParkSpaceNotificationType.BOOKING_UPDATE,
        title = title,
        message = booking.cancellationPolicySummary ?: "Demo refund: Rs. $refundPkr.",
        bookingId = booking.id,
        spaceId = booking.spaceId,
        nowEpochMs = nowEpochMs,
      )
    )
  }

  fun buildNotification(
    recipientUserId: String,
    type: ParkSpaceNotificationType,
    title: String,
    message: String,
    bookingId: String? = null,
    spaceId: String? = null,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): DemoNotificationEntity =
    DemoNotificationEntity(
      id = "notif-${UUID.randomUUID().toString().take(10)}",
      recipientUserId = recipientUserId,
      type = type,
      title = title,
      message = message,
      relatedBookingId = bookingId,
      relatedSpaceId = spaceId,
      isRead = false,
      createdAtEpochMs = nowEpochMs,
    )
}

class DemoPaymentTransactionRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
) : PaymentTransactionRepository {
  private val txDao = database.paymentTransactionDao()

  override fun observeTransactionsForCustomer(customerId: String): Flow<List<DemoPaymentTransaction>> =
    txDao.observeTransactionsForCustomer(customerId).map { list -> list.map { it.toDomain() } }

  override fun observeTransactionsForHost(hostId: String): Flow<List<DemoPaymentTransaction>> =
    txDao.observeTransactionsForHost(hostId).map { list -> list.map { it.toDomain() } }

  override fun observeTransactionsForBooking(bookingId: String): Flow<List<DemoPaymentTransaction>> =
    txDao.observeTransactionsForBooking(bookingId).map { list -> list.map { it.toDomain() } }

  override fun observeAvailableDemoCreditPkr(customerId: String): Flow<Int> =
    txDao.observeTransactionsForCustomer(customerId).map { list ->
      DemoMarketplaceEvents.computeDemoCreditBalancePkr(list)
    }
}

class DemoChatRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
) : ChatRepository {
  private val chatDao = database.chatDao()
  private val bookingDao = database.bookingDao()
  private val parkingDao = database.parkingDao()
  private val userDao = database.userDao()
  private val notifDao = database.notificationDao()

  override fun observeThreadSummariesForUser(userId: String): Flow<List<ChatThreadSummary>> =
    combine(
      chatDao.observeAllMessagesForUser(userId),
      bookingDao.observeAllBookings(),
      parkingDao.observeAllSpacesIncludingPending(),
    ) { messages, bookings, spaces ->
      val spaceMap = spaces.associateBy { it.id }
      val ownedBookings = bookings.filter { it.customerId == userId || it.hostId == userId }
      val msgsByBooking = messages.groupBy { it.bookingId }

      ownedBookings
        .mapNotNull { booking ->
          val threadMsgs = msgsByBooking[booking.id].orEmpty()
          if (threadMsgs.isEmpty()) return@mapNotNull null
          val last = threadMsgs.last()
          val unread =
            if (booking.customerId == userId) {
              threadMsgs.count { !it.isReadByCustomer }
            } else {
              threadMsgs.count { !it.isReadByHost }
            }
          ChatThreadSummary(
            bookingId = booking.id,
            bookingCode = booking.bookingCode,
            spaceId = booking.spaceId,
            spaceTitle = spaceMap[booking.spaceId]?.title ?: "Parking Space",
            customerId = booking.customerId,
            hostId = booking.hostId,
            lastMessageBody = last.body,
            lastSenderDisplayName = last.senderDisplayName,
            lastSentAtEpochMs = last.sentAtEpochMs,
            unreadCount = unread,
          )
        }
        .sortedByDescending { it.lastSentAtEpochMs }
    }

  override fun observeMessagesForBooking(
    bookingId: String,
    requesterUserId: String,
  ): Flow<List<ChatMessage>> =
    chatDao.observeMessagesForBooking(bookingId, requesterUserId).map { list ->
      list.map { it.toDomain() }
    }

  override suspend fun sendMessage(
    bookingId: String,
    senderUserId: String,
    body: String,
    nowEpochMs: Long,
  ): ChatMutationResult {
    val cleanBody = body.trim()
    if (cleanBody.isBlank()) {
      return ChatMutationResult.Rejected("Enter a message before sending.")
    }
    if (cleanBody.length > 1000) {
      return ChatMutationResult.Rejected("Message cannot exceed 1000 characters.")
    }

    return database.withTransaction {
      val booking = bookingDao.getBookingById(bookingId)
        ?: return@withTransaction ChatMutationResult.Rejected("Booking conversation not found.")
      if (senderUserId != booking.customerId && senderUserId != booking.hostId) {
        return@withTransaction ChatMutationResult.Rejected(
          "You can only message in bookings belonging to your account."
        )
      }
      val sender = userDao.getUserById(senderUserId)
        ?: return@withTransaction ChatMutationResult.Rejected("Sender account not found.")

      val isCustomer = senderUserId == booking.customerId
      val entity =
        DemoChatMessageEntity(
          id = "msg-${UUID.randomUUID().toString().take(10)}",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          customerId = booking.customerId,
          hostId = booking.hostId,
          senderUserId = senderUserId,
          senderRole = if (isCustomer) UserRole.CUSTOMER else UserRole.HOST,
          senderDisplayName = sender.fullName,
          body = cleanBody,
          sentAtEpochMs = nowEpochMs,
          isReadByCustomer = isCustomer,
          isReadByHost = !isCustomer,
        )
      chatDao.insertMessage(entity)

      val recipientId = if (isCustomer) booking.hostId else booking.customerId
      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = recipientId,
          type = ParkSpaceNotificationType.CHAT_MESSAGE,
          title = "New Message (${booking.bookingCode})",
          message = "${sender.fullName}: ${cleanBody.take(80)}",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )

      ChatMutationResult.Success(entity.toDomain())
    }
  }

  override suspend fun markThreadRead(bookingId: String, readerUserId: String) {
    val booking = bookingDao.getBookingById(bookingId) ?: return
    when (readerUserId) {
      booking.customerId -> chatDao.markReadByCustomer(bookingId, readerUserId)
      booking.hostId -> chatDao.markReadByHost(bookingId, readerUserId)
    }
  }
}

class DemoReviewRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
) : ReviewRepository {
  private val reviewDao = database.reviewDao()
  private val bookingDao = database.bookingDao()
  private val userDao = database.userDao()
  private val notifDao = database.notificationDao()

  override fun observeReviewsForSpace(spaceId: String): Flow<List<ParkingReview>> =
    reviewDao.observeReviewsForSpace(spaceId).map { list -> list.map { it.toDomain() } }

  override fun observeSpaceRatingSummaries(): Flow<List<SpaceRatingSummary>> =
    reviewDao.observeAllReviews().map { all ->
      all.groupBy { it.spaceId }.map { (spaceId, reviews) ->
        SpaceRatingSummary(
          spaceId = spaceId,
          averageRating = reviews.map { it.rating }.average(),
          reviewCount = reviews.size,
        )
      }
    }

  override fun observeReviewsForHost(hostId: String): Flow<List<ParkingReview>> =
    reviewDao.observeReviewsForHost(hostId).map { list -> list.map { it.toDomain() } }

  override fun observeReviewsByCustomer(customerId: String): Flow<List<ParkingReview>> =
    reviewDao.observeReviewsByCustomer(customerId).map { list -> list.map { it.toDomain() } }

  override suspend fun submitReview(
    bookingId: String,
    customerId: String,
    rating: Int,
    comment: String,
    nowEpochMs: Long,
  ): ReviewMutationResult {
    if (rating !in 1..5) {
      return ReviewMutationResult.Rejected("Select a rating between 1 and 5 stars.")
    }
    val cleanComment = comment.trim()
    if (cleanComment.length < 3) {
      return ReviewMutationResult.Rejected("Review comment must be at least 3 characters.")
    }

    return database.withTransaction {
      val booking = bookingDao.getBookingById(bookingId)
        ?: return@withTransaction ReviewMutationResult.Rejected("Booking not found.")
      if (booking.customerId != customerId) {
        return@withTransaction ReviewMutationResult.Rejected(
          "You can only review your own completed bookings."
        )
      }
      if (booking.hostId == customerId) {
        return@withTransaction ReviewMutationResult.Rejected("Hosts cannot review their own listings.")
      }
      if (booking.status != BookingStatus.CHECKED_OUT && booking.status != BookingStatus.COMPLETED) {
        return@withTransaction ReviewMutationResult.Rejected(
          "Reviews unlock after parking check-out or completion."
        )
      }
      if (reviewDao.getReviewByBookingId(bookingId) != null) {
        return@withTransaction ReviewMutationResult.Rejected(
          "A review has already been submitted for booking ${booking.bookingCode}."
        )
      }
      val customer = userDao.getUserById(customerId)
        ?: return@withTransaction ReviewMutationResult.Rejected("Customer account not found.")

      val entity =
        DemoReviewEntity(
          id = "rev-${UUID.randomUUID().toString().take(10)}",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          customerId = customerId,
          customerDisplayName = customer.fullName,
          hostId = booking.hostId,
          rating = rating,
          comment = cleanComment,
          createdAtEpochMs = nowEpochMs,
        )
      reviewDao.insertReview(entity)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = booking.hostId,
          type = ParkSpaceNotificationType.REVIEW_POSTED,
          title = "New ${rating}-Star Review (${booking.bookingCode})",
          message = "${customer.fullName}: \"${cleanComment.take(80)}\"",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )

      ReviewMutationResult.Success(entity.toDomain(), "Thank you! Verified booking review published.")
    }
  }

  override suspend fun replyToReview(
    reviewId: String,
    hostId: String,
    reply: String,
    nowEpochMs: Long,
  ): ReviewMutationResult {
    val cleanReply = reply.trim()
    if (cleanReply.length < 2) {
      return ReviewMutationResult.Rejected("Reply must be at least 2 characters.")
    }
    return database.withTransaction {
      val existing = reviewDao.getReviewById(reviewId)
        ?: return@withTransaction ReviewMutationResult.Rejected("Review not found.")
      if (existing.hostId != hostId) {
        return@withTransaction ReviewMutationResult.Rejected(
          "You do not have permission to reply to another Host's review."
        )
      }
      val updated = existing.copy(hostReply = cleanReply, hostRepliedAtEpochMs = nowEpochMs)
      reviewDao.updateReview(updated)
      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = existing.customerId,
          type = ParkSpaceNotificationType.REVIEW_POSTED,
          title = "Host Replied to Your Review",
          message = cleanReply.take(90),
          bookingId = existing.bookingId,
          spaceId = existing.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )
      ReviewMutationResult.Success(updated.toDomain(), "Host reply posted.")
    }
  }
}

class DemoDisputeRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
) : DisputeRepository {
  private val disputeDao = database.disputeDao()
  private val bookingDao = database.bookingDao()
  private val userDao = database.userDao()
  private val notifDao = database.notificationDao()

  override fun observeDisputesForCustomer(customerId: String): Flow<List<BookingDispute>> =
    disputeDao.observeDisputesForCustomer(customerId).map { list -> list.map { it.toDomain() } }

  override fun observeDisputesForHost(hostId: String): Flow<List<BookingDispute>> =
    disputeDao.observeDisputesForHost(hostId).map { list -> list.map { it.toDomain() } }

  override fun observeDisputesForBooking(bookingId: String): Flow<List<BookingDispute>> =
    disputeDao.observeDisputesForBooking(bookingId).map { list -> list.map { it.toDomain() } }

  override suspend fun fileDispute(
    bookingId: String,
    reporterUserId: String,
    category: DisputeCategory,
    description: String,
    nowEpochMs: Long,
  ): DisputeMutationResult {
    val cleanDesc = description.trim()
    if (cleanDesc.length < 10) {
      return DisputeMutationResult.Rejected("Provide at least 10 characters describing the issue.")
    }

    return database.withTransaction {
      val booking = bookingDao.getBookingById(bookingId)
        ?: return@withTransaction DisputeMutationResult.Rejected("Booking not found.")
      if (reporterUserId != booking.customerId && reporterUserId != booking.hostId) {
        return@withTransaction DisputeMutationResult.Rejected(
          "You can only file complaints on bookings belonging to your account."
        )
      }
      val reporter = userDao.getUserById(reporterUserId)
        ?: return@withTransaction DisputeMutationResult.Rejected("Reporter account not found.")

      val isCustomer = reporterUserId == booking.customerId
      val entity =
        DemoDisputeEntity(
          id = "dsp-${UUID.randomUUID().toString().take(10)}",
          bookingId = booking.id,
          bookingCode = booking.bookingCode,
          spaceId = booking.spaceId,
          customerId = booking.customerId,
          hostId = booking.hostId,
          reporterUserId = reporterUserId,
          reporterRole = if (isCustomer) UserRole.CUSTOMER else UserRole.HOST,
          category = category,
          description = cleanDesc,
          status = DisputeStatus.OPEN,
          createdAtEpochMs = nowEpochMs,
          updatedAtEpochMs = nowEpochMs,
        )
      disputeDao.insertDispute(entity)

      val recipientId = if (isCustomer) booking.hostId else booking.customerId
      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = recipientId,
          type = ParkSpaceNotificationType.DISPUTE_UPDATE,
          title = "Complaint Filed (${booking.bookingCode})",
          message = "${reporter.fullName} opened a ${category.displayName} report.",
          bookingId = booking.id,
          spaceId = booking.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )

      DisputeMutationResult.Success(
        entity.toDomain(),
        "Complaint filed for ${booking.bookingCode}. Both parties can track its status.",
      )
    }
  }

  override suspend fun respondToDisputeAsHost(
    disputeId: String,
    hostId: String,
    responseNote: String,
    nowEpochMs: Long,
  ): DisputeMutationResult {
    val cleanNote = responseNote.trim()
    if (cleanNote.length < 5) {
      return DisputeMutationResult.Rejected("Response note must be at least 5 characters.")
    }

    return database.withTransaction {
      val existing = disputeDao.getDisputeById(disputeId)
        ?: return@withTransaction DisputeMutationResult.Rejected("Complaint not found.")
      if (existing.hostId != hostId) {
        return@withTransaction DisputeMutationResult.Rejected(
          "You do not have permission to respond to another Host's complaint."
        )
      }
      val updated =
        existing.copy(
          hostResponseNote = cleanNote,
          status =
            if (existing.status == DisputeStatus.OPEN) DisputeStatus.UNDER_REVIEW
            else existing.status,
          updatedAtEpochMs = nowEpochMs,
        )
      disputeDao.updateDispute(updated)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = existing.customerId,
          type = ParkSpaceNotificationType.DISPUTE_UPDATE,
          title = "Host Responded (${existing.bookingCode})",
          message = cleanNote.take(90),
          bookingId = existing.bookingId,
          spaceId = existing.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )

      DisputeMutationResult.Success(updated.toDomain(), "Host response recorded.")
    }
  }

  override suspend fun resolveDisputeAsCustomerOrHost(
    disputeId: String,
    actorUserId: String,
    resolutionNote: String,
    targetStatus: DisputeStatus,
    nowEpochMs: Long,
  ): DisputeMutationResult {
    val cleanNote = resolutionNote.trim()
    if (cleanNote.length < 4) {
      return DisputeMutationResult.Rejected("Resolution note must be at least 4 characters.")
    }
    return database.withTransaction {
      val existing = disputeDao.getDisputeById(disputeId)
        ?: return@withTransaction DisputeMutationResult.Rejected("Complaint not found.")
      if (actorUserId != existing.customerId && actorUserId != existing.hostId) {
        return@withTransaction DisputeMutationResult.Rejected(
          "You do not have permission to update this complaint."
        )
      }
      val updated =
        existing.copy(
          status = targetStatus,
          resolutionNote = cleanNote,
          updatedAtEpochMs = nowEpochMs,
        )
      disputeDao.updateDispute(updated)

      val counterpart = if (actorUserId == existing.customerId) existing.hostId else existing.customerId
      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = counterpart,
          type = ParkSpaceNotificationType.DISPUTE_UPDATE,
          title = "Complaint ${targetStatus.displayName} (${existing.bookingCode})",
          message = cleanNote.take(90),
          bookingId = existing.bookingId,
          spaceId = existing.spaceId,
          nowEpochMs = nowEpochMs,
        )
      )

      DisputeMutationResult.Success(updated.toDomain(), "Complaint marked as ${targetStatus.displayName}.")
    }
  }
}

class DemoReferralRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
) : ReferralRepository {
  private val referralDao = database.referralDao()
  private val userDao = database.userDao()
  private val notifDao = database.notificationDao()

  override fun observeReferralsByReferrer(referrerUserId: String): Flow<List<DemoReferralRecord>> =
    referralDao.observeReferralsByReferrer(referrerUserId).map { list -> list.map { it.toDomain() } }

  override fun observeReferralAsReferred(referredUserId: String): Flow<DemoReferralRecord?> =
    referralDao.observeReferralAsReferred(referredUserId).map { it?.toDomain() }

  override suspend fun redeemReferralCode(
    referredUserId: String,
    referralCode: String,
    nowEpochMs: Long,
  ): ReferralMutationResult {
    val cleanCode = referralCode.trim().uppercase()
    if (cleanCode.length < 4) {
      return ReferralMutationResult.Rejected("Enter a valid referral code (at least 4 characters).")
    }

    return database.withTransaction {
      val referredUser = userDao.getUserById(referredUserId)
        ?: return@withTransaction ReferralMutationResult.Rejected("Customer account not found.")
      if (referredUser.role != UserRole.CUSTOMER) {
        return@withTransaction ReferralMutationResult.Rejected(
          "Referral codes can only be redeemed by Customer accounts."
        )
      }
      if (referralDao.getReferralAsReferredOnce(referredUserId) != null) {
        return@withTransaction ReferralMutationResult.Rejected(
          "You have already applied a referral code to this account."
        )
      }

      val referrerProfile = userDao.getCustomerProfileByReferralCode(cleanCode)
        ?: return@withTransaction ReferralMutationResult.Rejected(
          "No registered Customer owns referral code '$cleanCode'."
        )
      if (referrerProfile.userId == referredUserId) {
        return@withTransaction ReferralMutationResult.Rejected(
          "You cannot redeem your own referral code."
        )
      }

      val entity =
        DemoReferralEntity(
          id = "ref-${UUID.randomUUID().toString().take(10)}",
          referrerUserId = referrerProfile.userId,
          referredUserId = referredUserId,
          referralCodeUsed = referrerProfile.referralCode,
          rewardAmountPkr = DemoMarketplaceEvents.REFERRAL_REWARD_PKR,
          status = ReferralRewardStatus.PENDING_FIRST_BOOKING,
          isDemoOnly = true,
          createdAtEpochMs = nowEpochMs,
          creditedAtEpochMs = null,
        )
      referralDao.insertReferral(entity)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = referrerProfile.userId,
          type = ParkSpaceNotificationType.REFERRAL_UPDATE,
          title = "Referral Code Applied (${referrerProfile.referralCode})",
          message = "${referredUser.fullName} applied your code. Rs. ${entity.rewardAmountPkr} Demo Credit unlocks on their first confirmed booking.",
          nowEpochMs = nowEpochMs,
        )
      )

      ReferralMutationResult.Success(
        entity.toDomain(),
        "Referral code ${referrerProfile.referralCode} linked! Complete your first Demo booking to unlock Rs. ${entity.rewardAmountPkr} Demo Credit.",
      )
    }
  }
}

class DemoNotificationRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
) : NotificationRepository {
  private val notificationDao = database.notificationDao()

  override fun observeNotificationsForUser(recipientUserId: String): Flow<List<AppNotification>> =
    notificationDao.observeNotificationsForUser(recipientUserId).map { list ->
      list.map { it.toDomain() }
    }

  override fun observeUnreadCount(recipientUserId: String): Flow<Int> =
    notificationDao.observeUnreadCount(recipientUserId)

  override suspend fun markAsRead(notificationId: String, recipientUserId: String) {
    notificationDao.markAsRead(notificationId, recipientUserId)
  }

  override suspend fun markAllAsRead(recipientUserId: String) {
    notificationDao.markAllAsRead(recipientUserId)
  }
}
