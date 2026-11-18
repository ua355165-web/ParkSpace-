package com.example.core.domain.repository

import com.example.core.domain.model.AdminAuditLogEntry
import com.example.core.domain.model.AdminDisputeQueueItem
import com.example.core.domain.model.AdminListingQueueItem
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostKycQueueItem
import com.example.core.domain.model.PlatformAnalyticsSnapshot
import kotlinx.coroutines.flow.Flow

sealed interface AdminMutationResult {
  data class Success(val message: String) : AdminMutationResult

  data class Rejected(val reason: String) : AdminMutationResult
}

interface AdminRepository {
  fun observeHostKycQueue(): Flow<List<HostKycQueueItem>>

  fun observeListingModerationQueue(): Flow<List<AdminListingQueueItem>>

  fun observeAllDisputesQueue(): Flow<List<AdminDisputeQueueItem>>

  fun observePlatformAnalytics(): Flow<PlatformAnalyticsSnapshot>

  fun observeActiveCommissionRule(): Flow<DemoCommissionRule>

  fun observeAuditLogs(): Flow<List<AdminAuditLogEntry>>

  suspend fun approveHostKyc(
    actorAdminId: String,
    hostUserId: String,
    note: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): AdminMutationResult

  suspend fun rejectHostKyc(
    actorAdminId: String,
    hostUserId: String,
    reason: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): AdminMutationResult

  suspend fun approveListing(
    actorAdminId: String,
    spaceId: String,
    note: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): AdminMutationResult

  suspend fun rejectListing(
    actorAdminId: String,
    spaceId: String,
    reason: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): AdminMutationResult

  suspend fun updateDisputeStatusAsAdmin(
    actorAdminId: String,
    disputeId: String,
    targetStatus: DisputeStatus,
    note: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): AdminMutationResult

  suspend fun updateCommissionRule(
    actorAdminId: String,
    commissionPercent: Int,
    minFeePkr: Int,
    maxFeePkr: Int,
    note: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): AdminMutationResult
}
