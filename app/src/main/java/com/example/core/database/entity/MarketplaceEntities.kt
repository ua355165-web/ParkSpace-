package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.config.UserRole
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DemoPaymentTransactionType
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.ParkSpaceNotificationType
import com.example.core.domain.model.ReferralRewardStatus

@Entity(tableName = "demo_payment_transactions")
data class DemoPaymentTransactionEntity(
  @PrimaryKey val id: String,
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
)

@Entity(tableName = "demo_chat_messages")
data class DemoChatMessageEntity(
  @PrimaryKey val id: String,
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
)

@Entity(
  tableName = "demo_reviews",
  indices = [Index(value = ["bookingId"], unique = true)],
)
data class DemoReviewEntity(
  @PrimaryKey val id: String,
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
)

@Entity(tableName = "demo_disputes")
data class DemoDisputeEntity(
  @PrimaryKey val id: String,
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
)

@Entity(
  tableName = "demo_referrals",
  indices = [Index(value = ["referredUserId"], unique = true)],
)
data class DemoReferralEntity(
  @PrimaryKey val id: String,
  val referrerUserId: String,
  val referredUserId: String,
  val referralCodeUsed: String,
  val rewardAmountPkr: Int,
  val status: ReferralRewardStatus,
  val isDemoOnly: Boolean = true,
  val createdAtEpochMs: Long,
  val creditedAtEpochMs: Long? = null,
)

@Entity(tableName = "demo_notifications")
data class DemoNotificationEntity(
  @PrimaryKey val id: String,
  val recipientUserId: String,
  val type: ParkSpaceNotificationType,
  val title: String,
  val message: String,
  val relatedBookingId: String? = null,
  val relatedSpaceId: String? = null,
  val isRead: Boolean = false,
  val createdAtEpochMs: Long,
)
