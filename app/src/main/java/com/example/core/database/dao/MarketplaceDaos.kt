package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.DemoChatMessageEntity
import com.example.core.database.entity.DemoDisputeEntity
import com.example.core.database.entity.DemoNotificationEntity
import com.example.core.database.entity.DemoPaymentTransactionEntity
import com.example.core.database.entity.DemoReferralEntity
import com.example.core.database.entity.DemoReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DemoPaymentTransactionDao {
  @Query("SELECT * FROM demo_payment_transactions WHERE customerId = :customerId ORDER BY createdAtEpochMs DESC")
  fun observeTransactionsForCustomer(customerId: String): Flow<List<DemoPaymentTransactionEntity>>

  @Query("SELECT * FROM demo_payment_transactions WHERE customerId = :customerId ORDER BY createdAtEpochMs DESC")
  suspend fun getTransactionsForCustomerOnce(customerId: String): List<DemoPaymentTransactionEntity>

  @Query("SELECT * FROM demo_payment_transactions WHERE hostId = :hostId ORDER BY createdAtEpochMs DESC")
  fun observeTransactionsForHost(hostId: String): Flow<List<DemoPaymentTransactionEntity>>

  @Query("SELECT * FROM demo_payment_transactions WHERE bookingId = :bookingId ORDER BY createdAtEpochMs DESC")
  fun observeTransactionsForBooking(bookingId: String): Flow<List<DemoPaymentTransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(tx: DemoPaymentTransactionEntity)

  @Query("SELECT COUNT(*) FROM demo_payment_transactions")
  suspend fun countTransactions(): Int
}

@Dao
interface DemoChatDao {
  @Query("SELECT * FROM demo_chat_messages WHERE customerId = :userId OR hostId = :userId ORDER BY sentAtEpochMs ASC")
  fun observeAllMessagesForUser(userId: String): Flow<List<DemoChatMessageEntity>>

  @Query("SELECT * FROM demo_chat_messages WHERE bookingId = :bookingId AND (customerId = :requesterUserId OR hostId = :requesterUserId) ORDER BY sentAtEpochMs ASC")
  fun observeMessagesForBooking(bookingId: String, requesterUserId: String): Flow<List<DemoChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertMessage(msg: DemoChatMessageEntity)

  @Query("UPDATE demo_chat_messages SET isReadByCustomer = 1 WHERE bookingId = :bookingId AND customerId = :customerId")
  suspend fun markReadByCustomer(bookingId: String, customerId: String)

  @Query("UPDATE demo_chat_messages SET isReadByHost = 1 WHERE bookingId = :bookingId AND hostId = :hostId")
  suspend fun markReadByHost(bookingId: String, hostId: String)

  @Query("SELECT COUNT(*) FROM demo_chat_messages")
  suspend fun countMessages(): Int
}

@Dao
interface DemoReviewDao {
  @Query("SELECT * FROM demo_reviews WHERE spaceId = :spaceId ORDER BY createdAtEpochMs DESC")
  fun observeReviewsForSpace(spaceId: String): Flow<List<DemoReviewEntity>>

  @Query("SELECT * FROM demo_reviews ORDER BY createdAtEpochMs DESC")
  fun observeAllReviews(): Flow<List<DemoReviewEntity>>

  @Query("SELECT * FROM demo_reviews WHERE hostId = :hostId ORDER BY createdAtEpochMs DESC")
  fun observeReviewsForHost(hostId: String): Flow<List<DemoReviewEntity>>

  @Query("SELECT * FROM demo_reviews WHERE customerId = :customerId ORDER BY createdAtEpochMs DESC")
  fun observeReviewsByCustomer(customerId: String): Flow<List<DemoReviewEntity>>

  @Query("SELECT * FROM demo_reviews WHERE bookingId = :bookingId LIMIT 1")
  suspend fun getReviewByBookingId(bookingId: String): DemoReviewEntity?

  @Query("SELECT * FROM demo_reviews WHERE id = :reviewId LIMIT 1")
  suspend fun getReviewById(reviewId: String): DemoReviewEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertReview(review: DemoReviewEntity)

  @Update
  suspend fun updateReview(review: DemoReviewEntity)

  @Query("SELECT COUNT(*) FROM demo_reviews")
  suspend fun countReviews(): Int
}

@Dao
interface DemoDisputeDao {
  @Query("SELECT * FROM demo_disputes ORDER BY updatedAtEpochMs DESC")
  fun observeAllDisputes(): Flow<List<DemoDisputeEntity>>

  @Query("SELECT * FROM demo_disputes WHERE customerId = :customerId ORDER BY updatedAtEpochMs DESC")
  fun observeDisputesForCustomer(customerId: String): Flow<List<DemoDisputeEntity>>

  @Query("SELECT * FROM demo_disputes WHERE hostId = :hostId ORDER BY updatedAtEpochMs DESC")
  fun observeDisputesForHost(hostId: String): Flow<List<DemoDisputeEntity>>

  @Query("SELECT * FROM demo_disputes WHERE bookingId = :bookingId ORDER BY updatedAtEpochMs DESC")
  fun observeDisputesForBooking(bookingId: String): Flow<List<DemoDisputeEntity>>

  @Query("SELECT * FROM demo_disputes WHERE id = :disputeId LIMIT 1")
  suspend fun getDisputeById(disputeId: String): DemoDisputeEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertDispute(dispute: DemoDisputeEntity)

  @Update
  suspend fun updateDispute(dispute: DemoDisputeEntity)

  @Query("SELECT COUNT(*) FROM demo_disputes")
  suspend fun countDisputes(): Int
}

@Dao
interface DemoReferralDao {
  @Query("SELECT * FROM demo_referrals WHERE referrerUserId = :referrerUserId ORDER BY createdAtEpochMs DESC")
  fun observeReferralsByReferrer(referrerUserId: String): Flow<List<DemoReferralEntity>>

  @Query("SELECT * FROM demo_referrals WHERE referredUserId = :referredUserId LIMIT 1")
  fun observeReferralAsReferred(referredUserId: String): Flow<DemoReferralEntity?>

  @Query("SELECT * FROM demo_referrals WHERE referredUserId = :referredUserId LIMIT 1")
  suspend fun getReferralAsReferredOnce(referredUserId: String): DemoReferralEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertReferral(referral: DemoReferralEntity)

  @Update
  suspend fun updateReferral(referral: DemoReferralEntity)

  @Query("SELECT COUNT(*) FROM demo_referrals")
  suspend fun countReferrals(): Int
}

@Dao
interface DemoNotificationDao {
  @Query("SELECT * FROM demo_notifications WHERE recipientUserId = :recipientUserId ORDER BY createdAtEpochMs DESC")
  fun observeNotificationsForUser(recipientUserId: String): Flow<List<DemoNotificationEntity>>

  @Query("SELECT COUNT(*) FROM demo_notifications WHERE recipientUserId = :recipientUserId AND isRead = 0")
  fun observeUnreadCount(recipientUserId: String): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: DemoNotificationEntity)

  @Query("UPDATE demo_notifications SET isRead = 1 WHERE id = :notificationId AND recipientUserId = :recipientUserId")
  suspend fun markAsRead(notificationId: String, recipientUserId: String)

  @Query("UPDATE demo_notifications SET isRead = 1 WHERE recipientUserId = :recipientUserId")
  suspend fun markAllAsRead(recipientUserId: String)

  @Query("SELECT COUNT(*) FROM demo_notifications")
  suspend fun countNotifications(): Int
}
