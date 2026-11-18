package com.example.core.data.demo

import androidx.room.withTransaction
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.entity.DemoAdminAuditLogEntity
import com.example.core.database.entity.DemoCommissionConfigEntity
import com.example.core.database.mapper.toDomain
import com.example.core.domain.model.AdminAuditAction
import com.example.core.domain.model.AdminAuditLogEntry
import com.example.core.domain.model.AdminAuditTargetType
import com.example.core.domain.model.AdminDisputeQueueItem
import com.example.core.domain.model.AdminListingModerationState
import com.example.core.domain.model.AdminListingQueueItem
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.CityAnalyticsMetric
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostKycQueueItem
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkSpaceNotificationType
import com.example.core.domain.model.PlatformAnalyticsSnapshot
import com.example.core.domain.repository.AdminMutationResult
import com.example.core.domain.repository.AdminRepository
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.DemoSessionState
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DemoAdminRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
  private val sessionProvider: (() -> DemoSessionState)? = null,
) : AdminRepository {

  private val userDao = database.userDao()
  private val parkingDao = database.parkingDao()
  private val bookingDao = database.bookingDao()
  private val disputeDao = database.disputeDao()
  private val notifDao = database.notificationDao()
  private val adminDao = database.adminDao()

  override fun observeHostKycQueue(): Flow<List<HostKycQueueItem>> =
    combine(
      userDao.observeUsersByRole(UserRole.HOST),
      userDao.observeAllHostProfiles(),
      parkingDao.observeAllSpacesIncludingPending(),
    ) { hostUsers, hostProfiles, spaces ->
      val profileMap = hostProfiles.associateBy { it.userId }
      val spacesByHost = spaces.groupBy { it.hostId }

      hostUsers
        .mapNotNull { userEntity ->
          val user = userEntity.toDomain()
          val profile = profileMap[user.id]?.toDomain() ?: return@mapNotNull null
          val hostSpaces = spacesByHost[user.id].orEmpty()
          HostKycQueueItem(
            user = user,
            hostProfile = profile,
            listingsCount = hostSpaces.size,
            approvedListingsCount = hostSpaces.count { it.isApproved },
          )
        }
        .sortedWith(
          compareBy<HostKycQueueItem> { item ->
            when (item.hostProfile.verificationStatus) {
              HostVerificationStatus.PENDING_REVIEW -> 0
              HostVerificationStatus.REJECTED -> 1
              HostVerificationStatus.APPROVED -> 2
            }
          }.thenByDescending { it.hostProfile.joinedEpochMs }
        )
    }

  override fun observeListingModerationQueue(): Flow<List<AdminListingQueueItem>> =
    combine(
      parkingDao.observeAllSpacesIncludingPending(),
      parkingDao.observeAllBays(),
      userDao.observeUsersByRole(UserRole.HOST),
      userDao.observeAllHostProfiles(),
    ) { spaceEntities, bayEntities, hostUsers, hostProfiles ->
      val baysBySpace = bayEntities.groupBy { it.spaceId }
      val userMap = hostUsers.associateBy { it.id }
      val hostProfileMap = hostProfiles.associateBy { it.userId }

      spaceEntities
        .map { entity ->
          val space = entity.toDomain()
          val moderationState =
            when {
              space.isApproved -> AdminListingModerationState.APPROVED
              space.isRejectedByAdmin -> AdminListingModerationState.REJECTED
              else -> AdminListingModerationState.PENDING_REVIEW
            }
          AdminListingQueueItem(
            space = space,
            hostUser = userMap[space.hostId]?.toDomain(),
            hostProfile = hostProfileMap[space.hostId]?.toDomain(),
            moderationState = moderationState,
            baysCount = baysBySpace[space.id]?.size ?: space.totalBays,
          )
        }
        .sortedWith(
          compareBy<AdminListingQueueItem> { item ->
            when (item.moderationState) {
              AdminListingModerationState.PENDING_REVIEW -> 0
              AdminListingModerationState.REJECTED -> 1
              AdminListingModerationState.APPROVED -> 2
            }
          }.thenBy { it.space.title.lowercase() }
        )
    }

  override fun observeAllDisputesQueue(): Flow<List<AdminDisputeQueueItem>> =
    combine(
      disputeDao.observeAllDisputes(),
      parkingDao.observeAllSpacesIncludingPending(),
      userDao.observeAllUsers(),
    ) { disputeEntities, spaces, users ->
      val spaceMap = spaces.associateBy { it.id }
      val userMap = users.associateBy { it.id }

      disputeEntities
        .map { entity ->
          val dispute = entity.toDomain()
          val space = spaceMap[dispute.spaceId]
          AdminDisputeQueueItem(
            dispute = dispute,
            spaceTitle = space?.title ?: "Listing #${dispute.spaceId.takeLast(4)}",
            city = space?.city,
            customerName = userMap[dispute.customerId]?.fullName ?: "Customer",
            hostName = userMap[dispute.hostId]?.fullName ?: "Host",
          )
        }
        .sortedWith(
          compareBy<AdminDisputeQueueItem> { item ->
            when (item.dispute.status) {
              DisputeStatus.OPEN -> 0
              DisputeStatus.UNDER_REVIEW -> 1
              DisputeStatus.RESOLVED -> 2
              DisputeStatus.DISMISSED -> 3
            }
          }.thenByDescending { it.dispute.updatedAtEpochMs }
        )
    }

  override fun observePlatformAnalytics(): Flow<PlatformAnalyticsSnapshot> =
    combine(
      combine(
        userDao.observeAllUsers(),
        userDao.observeAllHostProfiles(),
        parkingDao.observeAllSpacesIncludingPending(),
      ) { users, hostProfiles, spaces -> Triple(users, hostProfiles, spaces) },
      combine(
        parkingDao.observeAllBays(),
        bookingDao.observeAllBookings(),
        disputeDao.observeAllDisputes(),
      ) { bays, bookings, disputes -> Triple(bays, bookings, disputes) },
    ) { (users, hostProfiles, spaces), (bays, bookings, disputes) ->
      val hostProfileMap = hostProfiles.associateBy { it.userId }
      val baysBySpace = bays.groupBy { it.spaceId }
      val spaceMap = spaces.associateBy { it.id }

      val customers = users.filter { it.role == UserRole.CUSTOMER }
      val hosts = users.filter { it.role == UserRole.HOST }

      val cityMetrics =
        SupportedCity.entries.map { city ->
          val cityCustomers = customers.filter { it.city == city }
          val cityHosts = hosts.filter { it.city == city }
          val approvedCityHosts =
            cityHosts.count {
              hostProfileMap[it.id]?.verificationStatus == HostVerificationStatus.APPROVED
            }
          val citySpaces = spaces.filter { it.city == city }
          val approvedSpaces = citySpaces.count { it.isApproved }
          val rejectedSpaces = citySpaces.count { !it.isApproved && it.isRejectedByAdmin }
          val pendingSpaces = citySpaces.count { !it.isApproved && !it.isRejectedByAdmin }

          val cityBays = citySpaces.flatMap { space -> baysBySpace[space.id].orEmpty() }
          val totalBaysCount =
            if (cityBays.isNotEmpty()) cityBays.size else citySpaces.sumOf { it.totalBays }
          val operationalBaysCount =
            if (cityBays.isNotEmpty()) cityBays.count { it.isActiveInDemo }
            else citySpaces.sumOf { it.availableBays }

          val cityBookings = bookings.filter { it.city == city }
          val activeBookingsCount =
            cityBookings.count {
              it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.CHECKED_IN
            }
          val completedBookingsCount =
            cityBookings.count {
              it.status == BookingStatus.CHECKED_OUT || it.status == BookingStatus.COMPLETED
            }
          val cancelledOrExpiredCount =
            cityBookings.count {
              it.status == BookingStatus.CANCELLED || it.status == BookingStatus.EXPIRED
            }

          val revenueEligibleBookings =
            cityBookings.filter {
              it.status in
                setOf(
                  BookingStatus.CONFIRMED,
                  BookingStatus.CHECKED_IN,
                  BookingStatus.CHECKED_OUT,
                  BookingStatus.COMPLETED,
                )
            }
          val grossVolume = revenueEligibleBookings.sumOf { it.totalAmountPkr }
          val commissionFees = revenueEligibleBookings.sumOf { it.serviceFeePkr }
          val hostNet = revenueEligibleBookings.sumOf { it.baseAmountPkr }
          val refunds =
            cityBookings
              .filter { it.status == BookingStatus.CANCELLED }
              .sumOf { it.refundAmountPkr ?: 0 }

          val cityDisputes =
            disputes.filter { d -> spaceMap[d.spaceId]?.city == city }
          val openDisputes =
            cityDisputes.count {
              it.status == DisputeStatus.OPEN || it.status == DisputeStatus.UNDER_REVIEW
            }
          val resolvedDisputes =
            cityDisputes.count {
              it.status == DisputeStatus.RESOLVED || it.status == DisputeStatus.DISMISSED
            }

          CityAnalyticsMetric(
            city = city,
            customersCount = cityCustomers.size,
            hostsCount = cityHosts.size,
            approvedHostsCount = approvedCityHosts,
            totalListingsCount = citySpaces.size,
            approvedListingsCount = approvedSpaces,
            pendingListingsCount = pendingSpaces,
            rejectedListingsCount = rejectedSpaces,
            totalBaysCount = totalBaysCount,
            operationalBaysCount = operationalBaysCount,
            totalBookingsCount = cityBookings.size,
            activeBookingsCount = activeBookingsCount,
            completedBookingsCount = completedBookingsCount,
            cancelledOrExpiredBookingsCount = cancelledOrExpiredCount,
            demoGrossVolumePkr = grossVolume,
            demoCommissionFeesPkr = commissionFees,
            demoHostNetPkr = hostNet,
            demoRefundsPkr = refunds,
            openDisputesCount = openDisputes,
            resolvedDisputesCount = resolvedDisputes,
          )
        }

      PlatformAnalyticsSnapshot(
        totalCustomersCount = customers.size,
        totalHostsCount = hosts.size,
        pendingHostsCount =
          hostProfiles.count { it.verificationStatus == HostVerificationStatus.PENDING_REVIEW },
        approvedHostsCount =
          hostProfiles.count { it.verificationStatus == HostVerificationStatus.APPROVED },
        rejectedHostsCount =
          hostProfiles.count { it.verificationStatus == HostVerificationStatus.REJECTED },
        totalListingsCount = spaces.size,
        approvedListingsCount = spaces.count { it.isApproved },
        pendingListingsCount = spaces.count { !it.isApproved && !it.isRejectedByAdmin },
        rejectedListingsCount = spaces.count { !it.isApproved && it.isRejectedByAdmin },
        totalBookingsCount = bookings.size,
        activeBookingsCount = cityMetrics.sumOf { it.activeBookingsCount },
        completedBookingsCount = cityMetrics.sumOf { it.completedBookingsCount },
        totalDemoGrossVolumePkr = cityMetrics.sumOf { it.demoGrossVolumePkr },
        totalDemoCommissionFeesPkr = cityMetrics.sumOf { it.demoCommissionFeesPkr },
        totalDemoHostNetPkr = cityMetrics.sumOf { it.demoHostNetPkr },
        totalDemoRefundsPkr = cityMetrics.sumOf { it.demoRefundsPkr },
        openDisputesCount = cityMetrics.sumOf { it.openDisputesCount },
        resolvedDisputesCount = cityMetrics.sumOf { it.resolvedDisputesCount },
        cityMetrics = cityMetrics,
      )
    }

  override fun observeActiveCommissionRule(): Flow<DemoCommissionRule> =
    adminDao.observeCommissionConfig().map { entity ->
      entity?.toDomain() ?: DemoCommissionRule.DEFAULT
    }

  override fun observeAuditLogs(): Flow<List<AdminAuditLogEntry>> =
    adminDao.observeAllAuditLogs().map { list -> list.map { it.toDomain() } }

  override suspend fun approveHostKyc(
    actorAdminId: String,
    hostUserId: String,
    note: String,
    nowEpochMs: Long,
  ): AdminMutationResult {
    validateAdminActor(actorAdminId)?.let { return AdminMutationResult.Rejected(it) }
    val cleanNote = note.trim().ifBlank { "Verified by Private Super Admin" }

    return database.withTransaction {
      val hostUser = userDao.getUserById(hostUserId)
      if (hostUser == null || hostUser.role != UserRole.HOST) {
        return@withTransaction AdminMutationResult.Rejected("Host account not found.")
      }
      val profile = userDao.getHostProfileOnce(hostUserId)
        ?: return@withTransaction AdminMutationResult.Rejected("Host profile record not found.")

      val updated =
        profile.copy(
          verificationStatus = HostVerificationStatus.APPROVED,
          verificationNote = cleanNote,
          reviewedAtEpochMs = nowEpochMs,
        )
      userDao.upsertHostProfile(updated)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = hostUserId,
          type = ParkSpaceNotificationType.ADMIN_MODERATION,
          title = "Host Account Approved",
          message = "Your Host KYC application was approved ($cleanNote). You can now publish listings.",
          nowEpochMs = nowEpochMs,
        )
      )

      recordAuditLog(
        actorAdminId = actorAdminId,
        actionType = AdminAuditAction.HOST_KYC_APPROVED,
        targetType = AdminAuditTargetType.HOST,
        targetId = hostUserId,
        targetLabel = "${hostUser.fullName} (${profile.displayBusinessAlias})",
        outcomeSummary = "Host verification set to APPROVED",
        reasonOrNote = cleanNote,
        nowEpochMs = nowEpochMs,
      )

      AdminMutationResult.Success(
        "Approved Host '${profile.displayBusinessAlias}'. Publishing is now enabled."
      )
    }
  }

  override suspend fun rejectHostKyc(
    actorAdminId: String,
    hostUserId: String,
    reason: String,
    nowEpochMs: Long,
  ): AdminMutationResult {
    validateAdminActor(actorAdminId)?.let { return AdminMutationResult.Rejected(it) }
    val cleanReason = reason.trim()
    if (cleanReason.length < 4) {
      return AdminMutationResult.Rejected(
        "Enter a rejection reason (at least 4 characters) before rejecting a Host."
      )
    }

    return database.withTransaction {
      val hostUser = userDao.getUserById(hostUserId)
      if (hostUser == null || hostUser.role != UserRole.HOST) {
        return@withTransaction AdminMutationResult.Rejected("Host account not found.")
      }
      val profile = userDao.getHostProfileOnce(hostUserId)
        ?: return@withTransaction AdminMutationResult.Rejected("Host profile record not found.")

      val updated =
        profile.copy(
          verificationStatus = HostVerificationStatus.REJECTED,
          verificationNote = cleanReason,
          reviewedAtEpochMs = nowEpochMs,
        )
      userDao.upsertHostProfile(updated)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = hostUserId,
          type = ParkSpaceNotificationType.ADMIN_MODERATION,
          title = "Host Verification Rejected",
          message = "Your Host KYC review was rejected: $cleanReason",
          nowEpochMs = nowEpochMs,
        )
      )

      recordAuditLog(
        actorAdminId = actorAdminId,
        actionType = AdminAuditAction.HOST_KYC_REJECTED,
        targetType = AdminAuditTargetType.HOST,
        targetId = hostUserId,
        targetLabel = "${hostUser.fullName} (${profile.displayBusinessAlias})",
        outcomeSummary = "Host verification set to REJECTED",
        reasonOrNote = cleanReason,
        nowEpochMs = nowEpochMs,
      )

      AdminMutationResult.Success(
        "Rejected Host '${profile.displayBusinessAlias}' with recorded reason."
      )
    }
  }

  override suspend fun approveListing(
    actorAdminId: String,
    spaceId: String,
    note: String,
    nowEpochMs: Long,
  ): AdminMutationResult {
    validateAdminActor(actorAdminId)?.let { return AdminMutationResult.Rejected(it) }
    val cleanNote = note.trim().ifBlank { "Listing verified and approved for Discovery" }

    return database.withTransaction {
      val space = parkingDao.getSpaceById(spaceId)
        ?: return@withTransaction AdminMutationResult.Rejected("Parking listing not found.")
      val hostProfile = userDao.getHostProfileOnce(space.hostId)
      if (hostProfile?.verificationStatus == HostVerificationStatus.REJECTED) {
        return@withTransaction AdminMutationResult.Rejected(
          "Cannot approve listing while the Host's KYC status is REJECTED. Approve Host KYC first."
        )
      }

      val updated =
        space.copy(
          isApproved = true,
          isRejectedByAdmin = false,
          moderationNote = cleanNote,
          moderatedAtEpochMs = nowEpochMs,
        )
      parkingDao.upsertSpace(updated)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = space.hostId,
          type = ParkSpaceNotificationType.ADMIN_MODERATION,
          title = "Listing Approved (${space.title})",
          message = "Your listing in ${space.areaLabel}, ${space.city.displayName} is now live in Discovery. Note: $cleanNote",
          spaceId = space.id,
          nowEpochMs = nowEpochMs,
        )
      )

      recordAuditLog(
        actorAdminId = actorAdminId,
        actionType = AdminAuditAction.LISTING_APPROVED,
        targetType = AdminAuditTargetType.LISTING,
        targetId = space.id,
        targetLabel = "${space.title} (${space.city.displayName})",
        outcomeSummary = "Listing approved for public Customer Discovery",
        reasonOrNote = cleanNote,
        nowEpochMs = nowEpochMs,
      )

      AdminMutationResult.Success("Approved listing '${space.title}' for Customer Discovery.")
    }
  }

  override suspend fun rejectListing(
    actorAdminId: String,
    spaceId: String,
    reason: String,
    nowEpochMs: Long,
  ): AdminMutationResult {
    validateAdminActor(actorAdminId)?.let { return AdminMutationResult.Rejected(it) }
    val cleanReason = reason.trim()
    if (cleanReason.length < 4) {
      return AdminMutationResult.Rejected(
        "Enter a rejection reason (at least 4 characters) before rejecting a listing."
      )
    }

    return database.withTransaction {
      val space = parkingDao.getSpaceById(spaceId)
        ?: return@withTransaction AdminMutationResult.Rejected("Parking listing not found.")

      val updated =
        space.copy(
          isApproved = false,
          isRejectedByAdmin = true,
          moderationNote = cleanReason,
          moderatedAtEpochMs = nowEpochMs,
        )
      parkingDao.upsertSpace(updated)

      notifDao.insertNotification(
        DemoMarketplaceEvents.buildNotification(
          recipientUserId = space.hostId,
          type = ParkSpaceNotificationType.ADMIN_MODERATION,
          title = "Listing Rejected (${space.title})",
          message = "Your listing was removed from Discovery review: $cleanReason",
          spaceId = space.id,
          nowEpochMs = nowEpochMs,
        )
      )

      recordAuditLog(
        actorAdminId = actorAdminId,
        actionType = AdminAuditAction.LISTING_REJECTED,
        targetType = AdminAuditTargetType.LISTING,
        targetId = space.id,
        targetLabel = "${space.title} (${space.city.displayName})",
        outcomeSummary = "Listing rejected and hidden from Discovery",
        reasonOrNote = cleanReason,
        nowEpochMs = nowEpochMs,
      )

      AdminMutationResult.Success("Rejected listing '${space.title}' with recorded reason.")
    }
  }

  override suspend fun updateDisputeStatusAsAdmin(
    actorAdminId: String,
    disputeId: String,
    targetStatus: DisputeStatus,
    note: String,
    nowEpochMs: Long,
  ): AdminMutationResult {
    validateAdminActor(actorAdminId)?.let { return AdminMutationResult.Rejected(it) }
    if (targetStatus == DisputeStatus.OPEN) {
      return AdminMutationResult.Rejected("Admin cannot revert a dispute back to OPEN.")
    }
    val cleanNote = note.trim()
    if (cleanNote.length < 4) {
      return AdminMutationResult.Rejected(
        "Enter an Admin review/resolution note (at least 4 characters)."
      )
    }

    return database.withTransaction {
      val existing = disputeDao.getDisputeById(disputeId)
        ?: return@withTransaction AdminMutationResult.Rejected("Dispute record not found.")

      if (existing.status == DisputeStatus.RESOLVED || existing.status == DisputeStatus.DISMISSED) {
        return@withTransaction AdminMutationResult.Rejected(
          "Dispute ${existing.bookingCode} is already finalized (${existing.status.displayName})."
        )
      }

      val updated =
        existing.copy(
          status = targetStatus,
          resolutionNote = cleanNote,
          updatedAtEpochMs = nowEpochMs,
        )
      disputeDao.updateDispute(updated)

      val notifTitle = "Admin Dispute ${targetStatus.displayName} (${existing.bookingCode})"
      listOf(existing.customerId, existing.hostId).distinct().forEach { userId ->
        notifDao.insertNotification(
          DemoMarketplaceEvents.buildNotification(
            recipientUserId = userId,
            type = ParkSpaceNotificationType.DISPUTE_UPDATE,
            title = notifTitle,
            message = "Super Admin update: $cleanNote",
            bookingId = existing.bookingId,
            spaceId = existing.spaceId,
            nowEpochMs = nowEpochMs,
          )
        )
      }

      val auditAction =
        when (targetStatus) {
          DisputeStatus.UNDER_REVIEW -> AdminAuditAction.DISPUTE_UNDER_REVIEW
          DisputeStatus.RESOLVED -> AdminAuditAction.DISPUTE_RESOLVED
          DisputeStatus.DISMISSED -> AdminAuditAction.DISPUTE_DISMISSED
          DisputeStatus.OPEN -> AdminAuditAction.DISPUTE_UNDER_REVIEW
        }
      recordAuditLog(
        actorAdminId = actorAdminId,
        actionType = auditAction,
        targetType = AdminAuditTargetType.DISPUTE,
        targetId = existing.id,
        targetLabel = "${existing.bookingCode} (${existing.category.displayName})",
        outcomeSummary = "Dispute transitioned from ${existing.status.displayName} to ${targetStatus.displayName}",
        reasonOrNote = cleanNote,
        nowEpochMs = nowEpochMs,
      )

      AdminMutationResult.Success(
        "Dispute ${existing.bookingCode} updated to ${targetStatus.displayName}."
      )
    }
  }

  override suspend fun updateCommissionRule(
    actorAdminId: String,
    commissionPercent: Int,
    minFeePkr: Int,
    maxFeePkr: Int,
    note: String,
    nowEpochMs: Long,
  ): AdminMutationResult {
    validateAdminActor(actorAdminId)?.let { return AdminMutationResult.Rejected(it) }
    if (commissionPercent !in DemoCommissionRule.MIN_ALLOWED_PERCENT..DemoCommissionRule.MAX_ALLOWED_PERCENT) {
      return AdminMutationResult.Rejected(
        "Commission percentage must be between ${DemoCommissionRule.MIN_ALLOWED_PERCENT}% and ${DemoCommissionRule.MAX_ALLOWED_PERCENT}%."
      )
    }
    if (minFeePkr !in 0..DemoCommissionRule.MAX_ALLOWED_MIN_FEE_PKR) {
      return AdminMutationResult.Rejected(
        "Minimum fee must be between Rs. 0 and Rs. ${DemoCommissionRule.MAX_ALLOWED_MIN_FEE_PKR}."
      )
    }
    if (maxFeePkr < minFeePkr || maxFeePkr > DemoCommissionRule.MAX_ALLOWED_CAP_PKR) {
      return AdminMutationResult.Rejected(
        "Maximum fee cap must be between Rs. $minFeePkr and Rs. ${DemoCommissionRule.MAX_ALLOWED_CAP_PKR}."
      )
    }
    val cleanNote = note.trim().ifBlank { "Updated Demo-only platform commission rule" }

    return database.withTransaction {
      val entity =
        DemoCommissionConfigEntity(
          id = DemoCommissionConfigEntity.ACTIVE_CONFIG_ID,
          commissionPercent = commissionPercent,
          minFeePkr = minFeePkr,
          maxFeePkr = maxFeePkr,
          note = cleanNote,
          updatedByAdminId = actorAdminId,
          updatedAtEpochMs = nowEpochMs,
        )
      adminDao.upsertCommissionConfig(entity)

      recordAuditLog(
        actorAdminId = actorAdminId,
        actionType = AdminAuditAction.COMMISSION_RULE_UPDATED,
        targetType = AdminAuditTargetType.COMMISSION_CONFIG,
        targetId = entity.id,
        targetLabel = "Demo Fee: $commissionPercent% (Min Rs. $minFeePkr, Cap Rs. $maxFeePkr)",
        outcomeSummary = "Active Demo commission updated to $commissionPercent% (Demo-only)",
        reasonOrNote = cleanNote,
        nowEpochMs = nowEpochMs,
      )

      AdminMutationResult.Success(
        "Saved Demo commission rule: $commissionPercent% (min Rs. $minFeePkr, cap Rs. $maxFeePkr). Applies to new Demo bookings."
      )
    }
  }

  private suspend fun validateAdminActor(actorAdminId: String): String? {
    if (actorAdminId != DemoSessionManager.PRIVATE_DEMO_ADMIN_ID) {
      return "Unauthorized: Only Private Super Admin can perform administrative actions."
    }
    val persistedDbUser = userDao.getUserById(actorAdminId)
    if (persistedDbUser != null && persistedDbUser.role != UserRole.SUPER_ADMIN) {
      return "Unauthorized: Non-admin account cannot perform Super Admin actions."
    }
    val activeSession = sessionProvider?.invoke()
    if (activeSession != null &&
      (activeSession.activeRole != UserRole.SUPER_ADMIN ||
        !activeSession.isAdminSandboxUnlocked ||
        activeSession.activeUser?.id != actorAdminId)
    ) {
      return "Unauthorized: Unlock Private Super Admin Sandbox before performing admin actions."
    }
    return null
  }

  private suspend fun recordAuditLog(
    actorAdminId: String,
    actionType: AdminAuditAction,
    targetType: AdminAuditTargetType,
    targetId: String,
    targetLabel: String,
    outcomeSummary: String,
    reasonOrNote: String?,
    nowEpochMs: Long,
  ) {
    adminDao.insertAuditLog(
      DemoAdminAuditLogEntity(
        id = "audit-${UUID.randomUUID().toString().take(10)}",
        actorAdminId = actorAdminId,
        actorLabel = "Private Super Admin (Demo Sandbox)",
        actionType = actionType,
        targetType = targetType,
        targetId = targetId,
        targetLabel = targetLabel,
        outcomeSummary = outcomeSummary,
        reasonOrNote = reasonOrNote,
        createdAtEpochMs = nowEpochMs,
      )
    )
  }
}
