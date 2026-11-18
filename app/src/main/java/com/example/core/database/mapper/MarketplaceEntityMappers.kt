package com.example.core.database.mapper

import com.example.core.database.entity.DemoChatMessageEntity
import com.example.core.database.entity.DemoDisputeEntity
import com.example.core.database.entity.DemoNotificationEntity
import com.example.core.database.entity.DemoPaymentTransactionEntity
import com.example.core.database.entity.DemoReferralEntity
import com.example.core.database.entity.DemoReviewEntity
import com.example.core.domain.model.AppNotification
import com.example.core.domain.model.BookingDispute
import com.example.core.domain.model.ChatMessage
import com.example.core.domain.model.DemoPaymentTransaction
import com.example.core.domain.model.DemoReferralRecord
import com.example.core.domain.model.ParkingReview

fun DemoPaymentTransactionEntity.toDomain(): DemoPaymentTransaction =
  DemoPaymentTransaction(
    id = id,
    bookingId = bookingId,
    bookingCode = bookingCode,
    customerId = customerId,
    hostId = hostId,
    spaceId = spaceId,
    method = method,
    transactionType = transactionType,
    amountPkr = amountPkr,
    referenceCode = referenceCode,
    summary = summary,
    isDemoOnly = isDemoOnly,
    createdAtEpochMs = createdAtEpochMs,
  )

fun DemoPaymentTransaction.toEntity(): DemoPaymentTransactionEntity =
  DemoPaymentTransactionEntity(
    id = id,
    bookingId = bookingId,
    bookingCode = bookingCode,
    customerId = customerId,
    hostId = hostId,
    spaceId = spaceId,
    method = method,
    transactionType = transactionType,
    amountPkr = amountPkr,
    referenceCode = referenceCode,
    summary = summary,
    isDemoOnly = isDemoOnly,
    createdAtEpochMs = createdAtEpochMs,
  )

fun DemoChatMessageEntity.toDomain(): ChatMessage =
  ChatMessage(
    id = id,
    bookingId = bookingId,
    spaceId = spaceId,
    customerId = customerId,
    hostId = hostId,
    senderUserId = senderUserId,
    senderRole = senderRole,
    senderDisplayName = senderDisplayName,
    body = body,
    sentAtEpochMs = sentAtEpochMs,
    isReadByCustomer = isReadByCustomer,
    isReadByHost = isReadByHost,
  )

fun ChatMessage.toEntity(): DemoChatMessageEntity =
  DemoChatMessageEntity(
    id = id,
    bookingId = bookingId,
    spaceId = spaceId,
    customerId = customerId,
    hostId = hostId,
    senderUserId = senderUserId,
    senderRole = senderRole,
    senderDisplayName = senderDisplayName,
    body = body,
    sentAtEpochMs = sentAtEpochMs,
    isReadByCustomer = isReadByCustomer,
    isReadByHost = isReadByHost,
  )

fun DemoReviewEntity.toDomain(): ParkingReview =
  ParkingReview(
    id = id,
    bookingId = bookingId,
    spaceId = spaceId,
    customerId = customerId,
    customerDisplayName = customerDisplayName,
    hostId = hostId,
    rating = rating,
    comment = comment,
    hostReply = hostReply,
    hostRepliedAtEpochMs = hostRepliedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
  )

fun ParkingReview.toEntity(): DemoReviewEntity =
  DemoReviewEntity(
    id = id,
    bookingId = bookingId,
    spaceId = spaceId,
    customerId = customerId,
    customerDisplayName = customerDisplayName,
    hostId = hostId,
    rating = rating,
    comment = comment,
    hostReply = hostReply,
    hostRepliedAtEpochMs = hostRepliedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
  )

fun DemoDisputeEntity.toDomain(): BookingDispute =
  BookingDispute(
    id = id,
    bookingId = bookingId,
    bookingCode = bookingCode,
    spaceId = spaceId,
    customerId = customerId,
    hostId = hostId,
    reporterUserId = reporterUserId,
    reporterRole = reporterRole,
    category = category,
    description = description,
    status = status,
    hostResponseNote = hostResponseNote,
    resolutionNote = resolutionNote,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

fun BookingDispute.toEntity(): DemoDisputeEntity =
  DemoDisputeEntity(
    id = id,
    bookingId = bookingId,
    bookingCode = bookingCode,
    spaceId = spaceId,
    customerId = customerId,
    hostId = hostId,
    reporterUserId = reporterUserId,
    reporterRole = reporterRole,
    category = category,
    description = description,
    status = status,
    hostResponseNote = hostResponseNote,
    resolutionNote = resolutionNote,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

fun DemoReferralEntity.toDomain(): DemoReferralRecord =
  DemoReferralRecord(
    id = id,
    referrerUserId = referrerUserId,
    referredUserId = referredUserId,
    referralCodeUsed = referralCodeUsed,
    rewardAmountPkr = rewardAmountPkr,
    status = status,
    isDemoOnly = isDemoOnly,
    createdAtEpochMs = createdAtEpochMs,
    creditedAtEpochMs = creditedAtEpochMs,
  )

fun DemoReferralRecord.toEntity(): DemoReferralEntity =
  DemoReferralEntity(
    id = id,
    referrerUserId = referrerUserId,
    referredUserId = referredUserId,
    referralCodeUsed = referralCodeUsed,
    rewardAmountPkr = rewardAmountPkr,
    status = status,
    isDemoOnly = isDemoOnly,
    createdAtEpochMs = createdAtEpochMs,
    creditedAtEpochMs = creditedAtEpochMs,
  )

fun DemoNotificationEntity.toDomain(): AppNotification =
  AppNotification(
    id = id,
    recipientUserId = recipientUserId,
    type = type,
    title = title,
    message = message,
    relatedBookingId = relatedBookingId,
    relatedSpaceId = relatedSpaceId,
    isRead = isRead,
    createdAtEpochMs = createdAtEpochMs,
  )

fun AppNotification.toEntity(): DemoNotificationEntity =
  DemoNotificationEntity(
    id = id,
    recipientUserId = recipientUserId,
    type = type,
    title = title,
    message = message,
    relatedBookingId = relatedBookingId,
    relatedSpaceId = relatedSpaceId,
    isRead = isRead,
    createdAtEpochMs = createdAtEpochMs,
  )
