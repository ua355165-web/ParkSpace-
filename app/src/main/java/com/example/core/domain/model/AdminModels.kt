package com.example.core.domain.model

import com.example.core.config.SupportedCity

data class DemoCommissionRule(
  val commissionPercent: Int = DEFAULT_COMMISSION_PERCENT,
  val minFeePkr: Int = DEFAULT_MIN_FEE_PKR,
  val maxFeePkr: Int = DEFAULT_MAX_FEE_PKR,
  val note: String = "Default Demo 8% platform fee rule",
  val updatedByAdminId: String? = null,
  val updatedAtEpochMs: Long? = null,
  val isDefaultBaseline: Boolean = true,
) {
  init {
    require(commissionPercent in MIN_ALLOWED_PERCENT..MAX_ALLOWED_PERCENT) {
      "Commission percentage must be between $MIN_ALLOWED_PERCENT% and $MAX_ALLOWED_PERCENT%."
    }
    require(minFeePkr in 0..MAX_ALLOWED_MIN_FEE_PKR) {
      "Minimum fee must be between Rs. 0 and Rs. $MAX_ALLOWED_MIN_FEE_PKR."
    }
    require(maxFeePkr in minFeePkr..MAX_ALLOWED_CAP_PKR) {
      "Maximum cap must be between minimum fee (Rs. $minFeePkr) and Rs. $MAX_ALLOWED_CAP_PKR."
    }
  }

  companion object {
    const val DEFAULT_COMMISSION_PERCENT = 8
    const val DEFAULT_MIN_FEE_PKR = 20
    const val DEFAULT_MAX_FEE_PKR = 1_500

    const val MIN_ALLOWED_PERCENT = 1
    const val MAX_ALLOWED_PERCENT = 30
    const val MAX_ALLOWED_MIN_FEE_PKR = 5_000
    const val MAX_ALLOWED_CAP_PKR = 50_000

    val DEFAULT = DemoCommissionRule()
  }
}

enum class AdminAuditAction(val displayName: String) {
  HOST_KYC_APPROVED("Host KYC Approved"),
  HOST_KYC_REJECTED("Host KYC Rejected"),
  LISTING_APPROVED("Listing Approved"),
  LISTING_REJECTED("Listing Rejected"),
  DISPUTE_UNDER_REVIEW("Dispute Marked Under Review"),
  DISPUTE_RESOLVED("Dispute Resolved"),
  DISPUTE_DISMISSED("Dispute Dismissed"),
  COMMISSION_RULE_UPDATED("Demo Commission Rule Updated"),
}

enum class AdminAuditTargetType(val displayName: String) {
  HOST("Host Account"),
  LISTING("Parking Listing"),
  DISPUTE("Booking Dispute"),
  COMMISSION_CONFIG("Demo Commission Rule"),
}

data class AdminAuditLogEntry(
  val id: String,
  val actorAdminId: String,
  val actorLabel: String,
  val actionType: AdminAuditAction,
  val targetType: AdminAuditTargetType,
  val targetId: String,
  val targetLabel: String,
  val outcomeSummary: String,
  val reasonOrNote: String? = null,
  val createdAtEpochMs: Long,
)

data class HostKycQueueItem(
  val user: UserProfile,
  val hostProfile: HostProfile,
  val listingsCount: Int,
  val approvedListingsCount: Int,
)

enum class AdminListingModerationState(val displayName: String) {
  PENDING_REVIEW("Pending Review"),
  APPROVED("Approved Live"),
  REJECTED("Rejected"),
}

data class AdminListingQueueItem(
  val space: ParkingSpace,
  val hostUser: UserProfile?,
  val hostProfile: HostProfile?,
  val moderationState: AdminListingModerationState,
  val baysCount: Int,
)

data class AdminDisputeQueueItem(
  val dispute: BookingDispute,
  val spaceTitle: String,
  val city: SupportedCity?,
  val customerName: String,
  val hostName: String,
)

data class CityAnalyticsMetric(
  val city: SupportedCity,
  val customersCount: Int,
  val hostsCount: Int,
  val approvedHostsCount: Int,
  val totalListingsCount: Int,
  val approvedListingsCount: Int,
  val pendingListingsCount: Int,
  val rejectedListingsCount: Int,
  val totalBaysCount: Int,
  val operationalBaysCount: Int,
  val totalBookingsCount: Int,
  val activeBookingsCount: Int,
  val completedBookingsCount: Int,
  val cancelledOrExpiredBookingsCount: Int,
  val demoGrossVolumePkr: Int,
  val demoCommissionFeesPkr: Int,
  val demoHostNetPkr: Int,
  val demoRefundsPkr: Int,
  val openDisputesCount: Int,
  val resolvedDisputesCount: Int,
)

data class PlatformAnalyticsSnapshot(
  val totalCustomersCount: Int,
  val totalHostsCount: Int,
  val pendingHostsCount: Int,
  val approvedHostsCount: Int,
  val rejectedHostsCount: Int,
  val totalListingsCount: Int,
  val approvedListingsCount: Int,
  val pendingListingsCount: Int,
  val rejectedListingsCount: Int,
  val totalBookingsCount: Int,
  val activeBookingsCount: Int,
  val completedBookingsCount: Int,
  val totalDemoGrossVolumePkr: Int,
  val totalDemoCommissionFeesPkr: Int,
  val totalDemoHostNetPkr: Int,
  val totalDemoRefundsPkr: Int,
  val openDisputesCount: Int,
  val resolvedDisputesCount: Int,
  val cityMetrics: List<CityAnalyticsMetric>,
)
