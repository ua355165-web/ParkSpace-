package com.example.core.domain.model

import com.example.core.config.UserRole

enum class DemoPaymentMethod(
  val displayName: String,
  val subtitle: String,
) {
  JAZZCASH_DEMO(
    displayName = "JazzCash (Demo Simulation)",
    subtitle = "Simulated local mobile wallet — no real debit occurs",
  ),
  EASYPAISA_DEMO(
    displayName = "Easypaisa (Demo Simulation)",
    subtitle = "Simulated local mobile wallet — no real debit occurs",
  ),
  CARD_DEMO(
    displayName = "Visa / Mastercard (Demo Simulation)",
    subtitle = "Simulated test card — no bank gateway connected",
  ),
  BANK_IBFT_DEMO(
    displayName = "Bank IBFT / Raast (Demo Simulation)",
    subtitle = "Simulated Pakistani bank transfer — local device only",
  ),
  DEMO_CREDIT_WALLET(
    displayName = "ParkSpace Demo Credits (Simulated)",
    subtitle = "Uses local Demo referral credits — zero real funds",
  ),
}

enum class DemoPaymentTransactionType(val displayName: String) {
  ESCROW_PAYMENT("Demo Escrow Payment"),
  HOST_SETTLEMENT("Demo Host Settlement"),
  CUSTOMER_REFUND("Demo Refund"),
  REFERRAL_DEMO_CREDIT("Demo Referral Credit"),
}

data class DemoPaymentTransaction(
  val id: String,
  val bookingId: String?,
  val bookingCode: String?,
  val customerId: String,
  val hostId: String?,
  val spaceId: String?,
  val method: DemoPaymentMethod,
  val transactionType: DemoPaymentTransactionType,
  val amountPkr: Int,
  val referenceCode: String,
  val summary: String,
  val isDemoOnly: Boolean = true,
  val createdAtEpochMs: Long,
) {
  init {
    require(id.isNotBlank()) { "DemoPaymentTransaction.id must not be blank" }
    require(customerId.isNotBlank()) { "DemoPaymentTransaction.customerId must not be blank" }
    require(amountPkr >= 0) { "DemoPaymentTransaction.amountPkr cannot be negative" }
    require(referenceCode.isNotBlank()) { "DemoPaymentTransaction.referenceCode must not be blank" }
    require(isDemoOnly) { "All Milestone 5 transactions must remain explicitly Demo-only" }
  }
}

data class ChatMessage(
  val id: String,
  val bookingId: String,
  val spaceId: String,
  val customerId: String,
  val hostId: String,
  val senderUserId: String,
  val senderRole: UserRole,
  val senderDisplayName: String,
  val body: String,
  val sentAtEpochMs: Long,
  val isReadByCustomer: Boolean,
  val isReadByHost: Boolean,
) {
  init {
    require(id.isNotBlank()) { "ChatMessage.id must not be blank" }
    require(bookingId.isNotBlank()) { "ChatMessage.bookingId must not be blank" }
    require(body.isNotBlank()) { "ChatMessage.body must not be blank" }
  }
}

data class ChatThreadSummary(
  val bookingId: String,
  val bookingCode: String,
  val spaceId: String,
  val spaceTitle: String,
  val customerId: String,
  val hostId: String,
  val lastMessageBody: String,
  val lastSenderDisplayName: String,
  val lastSentAtEpochMs: Long,
  val unreadCount: Int,
)

data class ParkingReview(
  val id: String,
  val bookingId: String,
  val spaceId: String,
  val customerId: String,
  val customerDisplayName: String,
  val hostId: String,
  val rating: Int,
  val comment: String,
  val hostReply: String? = null,
  val hostRepliedAtEpochMs: Long? = null,
  val createdAtEpochMs: Long,
) {
  init {
    require(id.isNotBlank()) { "ParkingReview.id must not be blank" }
    require(bookingId.isNotBlank()) { "ParkingReview.bookingId must not be blank" }
    require(rating in 1..5) { "ParkingReview.rating must be between 1 and 5" }
    require(comment.isNotBlank()) { "ParkingReview.comment must not be blank" }
  }
}

data class SpaceRatingSummary(
  val spaceId: String,
  val averageRating: Double?,
  val reviewCount: Int,
)

enum class DisputeCategory(val displayName: String) {
  GATE_ACCESS_ISSUE("Gate / Entry PIN Issue"),
  BAY_OCCUPIED("Assigned Bay Occupied / Blocked"),
  SAFETY_OR_DAMAGE("Safety or Facility Issue"),
  BILLING_OR_OVERSTAY("Demo Billing / Refund Dispute"),
  CONDUCT("Host / Customer Conduct"),
  OTHER("Other Booking Complaint"),
}

enum class DisputeStatus(val displayName: String) {
  OPEN("Open"),
  UNDER_REVIEW("Under Review"),
  RESOLVED("Resolved"),
  DISMISSED("Dismissed"),
}

data class BookingDispute(
  val id: String,
  val bookingId: String,
  val bookingCode: String,
  val spaceId: String,
  val customerId: String,
  val hostId: String,
  val reporterUserId: String,
  val reporterRole: UserRole,
  val category: DisputeCategory,
  val description: String,
  val status: DisputeStatus,
  val hostResponseNote: String? = null,
  val resolutionNote: String? = null,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  init {
    require(id.isNotBlank()) { "BookingDispute.id must not be blank" }
    require(bookingId.isNotBlank()) { "BookingDispute.bookingId must not be blank" }
    require(description.isNotBlank()) { "BookingDispute.description must not be blank" }
  }
}

enum class ReferralRewardStatus(val displayName: String) {
  PENDING_FIRST_BOOKING("Awaiting First Demo Booking"),
  CREDITED_DEMO("Demo Credit Unlocked (Simulated)"),
}

data class DemoReferralRecord(
  val id: String,
  val referrerUserId: String,
  val referredUserId: String,
  val referralCodeUsed: String,
  val rewardAmountPkr: Int,
  val status: ReferralRewardStatus,
  val isDemoOnly: Boolean = true,
  val createdAtEpochMs: Long,
  val creditedAtEpochMs: Long? = null,
) {
  init {
    require(id.isNotBlank()) { "DemoReferralRecord.id must not be blank" }
    require(referrerUserId != referredUserId) { "Self-referral is not allowed" }
    require(rewardAmountPkr > 0) { "rewardAmountPkr must be positive" }
    require(isDemoOnly) { "Referral reward must remain Demo-only" }
  }
}

enum class ParkSpaceNotificationType(val displayName: String) {
  BOOKING_UPDATE("Booking"),
  PAYMENT_DEMO("Demo Payment"),
  CHAT_MESSAGE("Message"),
  REVIEW_POSTED("Review"),
  DISPUTE_UPDATE("Complaint / Dispute"),
  REFERRAL_UPDATE("Referral"),
  ADMIN_MODERATION("Moderation / Verification"),
}

data class AppNotification(
  val id: String,
  val recipientUserId: String,
  val type: ParkSpaceNotificationType,
  val title: String,
  val message: String,
  val relatedBookingId: String? = null,
  val relatedSpaceId: String? = null,
  val isRead: Boolean = false,
  val createdAtEpochMs: Long,
) {
  init {
    require(id.isNotBlank()) { "AppNotification.id must not be blank" }
    require(recipientUserId.isNotBlank()) { "AppNotification.recipientUserId must not be blank" }
    require(title.isNotBlank()) { "AppNotification.title must not be blank" }
    require(message.isNotBlank()) { "AppNotification.message must not be blank" }
  }
}
