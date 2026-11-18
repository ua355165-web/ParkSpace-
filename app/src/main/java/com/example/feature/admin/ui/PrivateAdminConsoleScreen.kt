package com.example.feature.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.AdminAuditLogEntry
import com.example.core.domain.model.AdminDisputeQueueItem
import com.example.core.domain.model.AdminListingModerationState
import com.example.core.domain.model.AdminListingQueueItem
import com.example.core.domain.model.CityAnalyticsMetric
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostKycQueueItem
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.PlatformAnalyticsSnapshot
import com.example.feature.admin.AdminConsoleTab
import com.example.feature.admin.AdminConsoleUiState
import com.example.feature.discovery.ui.ParkingSpaceMediaBanner
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface AdminModerationPrompt {
  data class HostDecision(
    val hostUserId: String,
    val title: String,
    val approve: Boolean,
  ) : AdminModerationPrompt

  data class ListingDecision(
    val spaceId: String,
    val title: String,
    val approve: Boolean,
  ) : AdminModerationPrompt

  data class DisputeDecision(
    val disputeId: String,
    val bookingCode: String,
    val targetStatus: DisputeStatus,
  ) : AdminModerationPrompt
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateAdminConsoleScreen(
  uiState: AdminConsoleUiState,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  onSelectTab: (AdminConsoleTab) -> Unit,
  onApproveHostKyc: (hostUserId: String, note: String) -> Unit,
  onRejectHostKyc: (hostUserId: String, reason: String) -> Unit,
  onApproveListing: (spaceId: String, note: String) -> Unit,
  onRejectListing: (spaceId: String, reason: String) -> Unit,
  onUpdateDisputeStatus: (disputeId: String, targetStatus: DisputeStatus, note: String) -> Unit,
  onUpdateCommissionRule: (percent: String, minFee: String, maxCap: String, note: String) -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var activePrompt by remember { mutableStateOf<AdminModerationPrompt?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("private_admin_console_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Private Super Admin Console",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Demo Sandbox • No Public Signup • Local Governance",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("admin_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = onOpenSettings,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("admin_settings_button"),
          ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Profile & Settings")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth)
            .fillMaxSize()
            .padding(horizontal = ParkSpaceSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        // Security boundary summary card
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("admin_shell_card"),
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = ParkSpaceSpacing.sm, vertical = ParkSpaceSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Icon(
                imageVector = Icons.Outlined.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text =
                  "Hosts Pending: ${uiState.hostKycQueue.count { it.hostProfile.verificationStatus == HostVerificationStatus.PENDING_REVIEW }} • Listings Pending: ${uiState.listingQueue.count { it.moderationState == AdminListingModerationState.PENDING_REVIEW }} • Open Disputes: ${uiState.disputesQueue.count { it.dispute.status == DisputeStatus.OPEN || it.dispute.status == DisputeStatus.UNDER_REVIEW }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            ParkSpaceStatusPill(label = "DEMO ADMIN", tone = StatusTone.ERROR)
          }
        }

        if (uiState.actionFeedback != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().testTag("admin_feedback_banner"),
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  Icons.Outlined.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = uiState.actionFeedback.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              IconButton(
                onClick = onDismissFeedback,
                modifier = Modifier.size(28.dp).testTag("admin_dismiss_feedback_button"),
              ) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        if (uiState.errorMessage != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("admin_error_banner"),
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  Icons.Outlined.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = uiState.errorMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
              IconButton(
                onClick = onDismissFeedback,
                modifier = Modifier.size(28.dp).testTag("admin_dismiss_error_button"),
              ) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        PrimaryScrollableTabRow(
          selectedTabIndex = uiState.activeTab.ordinal,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary,
          edgePadding = 0.dp,
        ) {
          AdminConsoleTab.entries.forEach { tab ->
            Tab(
              selected = uiState.activeTab == tab,
              onClick = { onSelectTab(tab) },
              text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}"),
            )
          }
        }

        when (uiState.activeTab) {
          AdminConsoleTab.KYC_QUEUE -> {
            HostKycQueueTabContent(
              items = uiState.hostKycQueue,
              onPromptApprove = { item ->
                activePrompt =
                  AdminModerationPrompt.HostDecision(
                    hostUserId = item.user.id,
                    title = "Approve Host: ${item.hostProfile.displayBusinessAlias}",
                    approve = true,
                  )
              },
              onPromptReject = { item ->
                activePrompt =
                  AdminModerationPrompt.HostDecision(
                    hostUserId = item.user.id,
                    title = "Reject Host: ${item.hostProfile.displayBusinessAlias}",
                    approve = false,
                  )
              },
            )
          }

          AdminConsoleTab.LISTINGS -> {
            ListingModerationTabContent(
              items = uiState.listingQueue,
              onPromptApprove = { item ->
                activePrompt =
                  AdminModerationPrompt.ListingDecision(
                    spaceId = item.space.id,
                    title = "Approve Listing: ${item.space.title}",
                    approve = true,
                  )
              },
              onPromptReject = { item ->
                activePrompt =
                  AdminModerationPrompt.ListingDecision(
                    spaceId = item.space.id,
                    title = "Reject Listing: ${item.space.title}",
                    approve = false,
                  )
              },
            )
          }

          AdminConsoleTab.DISPUTES -> {
            DisputesModerationTabContent(
              items = uiState.disputesQueue,
              onPromptDecision = { item, status ->
                activePrompt =
                  AdminModerationPrompt.DisputeDecision(
                    disputeId = item.dispute.id,
                    bookingCode = item.dispute.bookingCode,
                    targetStatus = status,
                  )
              },
            )
          }

          AdminConsoleTab.ANALYTICS -> {
            CityAnalyticsTabContent(analytics = uiState.analytics)
          }

          AdminConsoleTab.CONFIG_AND_LOGS -> {
            CommissionAndAuditTabContent(
              commissionRule = uiState.commissionRule,
              auditLogs = uiState.auditLogs,
              onSaveCommissionRule = onUpdateCommissionRule,
            )
          }
        }
      }
    }
  }

  activePrompt?.let { prompt ->
    AdminModerationReasonDialog(
      prompt = prompt,
      onDismiss = { activePrompt = null },
      onConfirm = { noteOrReason ->
        when (prompt) {
          is AdminModerationPrompt.HostDecision -> {
            if (prompt.approve) onApproveHostKyc(prompt.hostUserId, noteOrReason)
            else onRejectHostKyc(prompt.hostUserId, noteOrReason)
          }
          is AdminModerationPrompt.ListingDecision -> {
            if (prompt.approve) onApproveListing(prompt.spaceId, noteOrReason)
            else onRejectListing(prompt.spaceId, noteOrReason)
          }
          is AdminModerationPrompt.DisputeDecision -> {
            onUpdateDisputeStatus(prompt.disputeId, prompt.targetStatus, noteOrReason)
          }
        }
        activePrompt = null
      },
    )
  }
}

@Composable
private fun HostKycQueueTabContent(
  items: List<HostKycQueueItem>,
  onPromptApprove: (HostKycQueueItem) -> Unit,
  onPromptReject: (HostKycQueueItem) -> Unit,
) {
  var filter by rememberSaveable { mutableStateOf("ALL") }
  val filtered =
    remember(items, filter) {
      when (filter) {
        "PENDING" ->
          items.filter { it.hostProfile.verificationStatus == HostVerificationStatus.PENDING_REVIEW }
        "APPROVED" ->
          items.filter { it.hostProfile.verificationStatus == HostVerificationStatus.APPROVED }
        "REJECTED" ->
          items.filter { it.hostProfile.verificationStatus == HostVerificationStatus.REJECTED }
        else -> items
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_kyc_queue_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        listOf(
          "ALL" to "All (${items.size})",
          "PENDING" to "Pending (${items.count { it.hostProfile.verificationStatus == HostVerificationStatus.PENDING_REVIEW }})",
          "APPROVED" to "Approved (${items.count { it.hostProfile.verificationStatus == HostVerificationStatus.APPROVED }})",
          "REJECTED" to "Rejected (${items.count { it.hostProfile.verificationStatus == HostVerificationStatus.REJECTED }})",
        ).forEach { (key, label) ->
          FilterChip(
            selected = filter == key,
            onClick = { filter = key },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("admin_kyc_filter_$key"),
          )
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.VerifiedUser,
          title = "No Host KYC Records",
          message =
            "Self-registered Hosts awaiting review or previously moderated will appear here automatically.",
          tag = "admin_kyc_empty_state",
        )
      }
    } else {
      items(filtered, key = { it.user.id }) { item ->
        val status = item.hostProfile.verificationStatus
        val tone =
          when (status) {
            HostVerificationStatus.PENDING_REVIEW -> StatusTone.PENDING
            HostVerificationStatus.APPROVED -> StatusTone.AVAILABLE
            HostVerificationStatus.REJECTED -> StatusTone.ERROR
          }
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("admin_kyc_card_${item.user.id}"),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.hostProfile.displayBusinessAlias,
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Owner: ${item.user.fullName} • ${item.user.emailMasked}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              ParkSpaceStatusPill(
                label = status.displayName,
                tone = tone,
                modifier = Modifier.testTag("admin_kyc_status_${item.user.id}"),
              )
            }

            Text(
              text =
                "Cities: ${item.hostProfile.operatingCities.joinToString { it.displayName }} • Listings: ${item.listingsCount} (${item.approvedListingsCount} live) • Joined: ${formatAdminTimestamp(item.hostProfile.joinedEpochMs)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!item.hostProfile.verificationNote.isNullOrBlank()) {
              Text(
                text = "Admin KYC Note: ${item.hostProfile.verificationNote}",
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (status == HostVerificationStatus.REJECTED) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.primary,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              if (status != HostVerificationStatus.APPROVED) {
                ParkSpacePrimaryButton(
                  text = "Approve Host",
                  onClick = { onPromptApprove(item) },
                  modifier = Modifier.weight(1f).testTag("admin_approve_host_${item.user.id}"),
                )
              }
              if (status != HostVerificationStatus.REJECTED) {
                OutlinedButton(
                  onClick = { onPromptReject(item) },
                  modifier =
                    Modifier.weight(1f)
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("admin_reject_host_${item.user.id}"),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text("Reject Host")
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg)) }
  }
}

@Composable
private fun ListingModerationTabContent(
  items: List<AdminListingQueueItem>,
  onPromptApprove: (AdminListingQueueItem) -> Unit,
  onPromptReject: (AdminListingQueueItem) -> Unit,
) {
  var filter by rememberSaveable { mutableStateOf("ALL") }
  val filtered =
    remember(items, filter) {
      when (filter) {
        "PENDING" ->
          items.filter { it.moderationState == AdminListingModerationState.PENDING_REVIEW }
        "APPROVED" ->
          items.filter { it.moderationState == AdminListingModerationState.APPROVED }
        "REJECTED" ->
          items.filter { it.moderationState == AdminListingModerationState.REJECTED }
        else -> items
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_listings_queue_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        listOf(
          "ALL" to "All (${items.size})",
          "PENDING" to "Pending (${items.count { it.moderationState == AdminListingModerationState.PENDING_REVIEW }})",
          "APPROVED" to "Live (${items.count { it.moderationState == AdminListingModerationState.APPROVED }})",
          "REJECTED" to "Rejected (${items.count { it.moderationState == AdminListingModerationState.REJECTED }})",
        ).forEach { (key, label) ->
          FilterChip(
            selected = filter == key,
            onClick = { filter = key },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("admin_listing_filter_$key"),
          )
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.LocalParking,
          title = "No Host Listings Submitted",
          message =
            "When self-registered Hosts create parking spaces, you can inspect photos, pricing, and approve or reject them here.",
          tag = "admin_listings_empty_state",
        )
      }
    } else {
      items(filtered, key = { it.space.id }) { item ->
        val space = item.space
        val tone =
          when (item.moderationState) {
            AdminListingModerationState.PENDING_REVIEW -> StatusTone.PENDING
            AdminListingModerationState.APPROVED -> StatusTone.AVAILABLE
            AdminListingModerationState.REJECTED -> StatusTone.ERROR
          }
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("admin_listing_card_${space.id}"),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = space.title, style = MaterialTheme.typography.titleMedium)
                Text(
                  text =
                    "${space.location.areaLabel}, ${space.location.city.displayName} • ${space.parkingType.displayName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              ParkSpaceStatusPill(
                label = item.moderationState.displayName,
                tone = tone,
                modifier = Modifier.testTag("admin_listing_status_${space.id}"),
              )
            }

            ParkingSpaceMediaBanner(
              photoUris = space.photoUris,
              parkingType = space.parkingType,
              isCovered = ParkingAmenity.COVERED in space.amenities,
              contentDescription = "${space.title} photo preview",
              height = 104.dp,
            )

            Text(
              text = space.description,
              style = MaterialTheme.typography.bodySmall,
            )
            Text(
              text =
                "Host: ${item.hostProfile?.displayBusinessAlias ?: item.hostUser?.fullName ?: space.hostId} (${item.hostProfile?.verificationStatus?.displayName ?: "Unknown KYC"})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text =
                "Rates: Rs. ${space.hourlyRatePkr}/hr • Rs. ${space.dailyRatePkr}/day" +
                  (if (space.monthlyEnabled && space.monthlyRatePkr != null) " • Rs. ${space.monthlyRatePkr}/mo" else "") +
                  " • Bays: ${space.availableBays}/${item.baysCount} • Schedule: ${space.schedule.formattedHours()}",
              style = ParkSpaceMonoCodeStyle,
            )
            Text(
              text =
                "Vehicles: ${space.supportedVehicles.joinToString { it.displayName }} • Photos: ${space.photoUris.size} uploaded (${space.photoTags.joinToString()})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!space.moderationNote.isNullOrBlank()) {
              Text(
                text = "Moderation Note: ${space.moderationNote}",
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (item.moderationState == AdminListingModerationState.REJECTED) {
                    MaterialTheme.colorScheme.error
                  } else {
                    MaterialTheme.colorScheme.primary
                  },
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              if (!space.isApproved) {
                ParkSpacePrimaryButton(
                  text = "Approve Listing",
                  onClick = { onPromptApprove(item) },
                  modifier = Modifier.weight(1f).testTag("admin_approve_listing_${space.id}"),
                )
              }
              if (item.moderationState != AdminListingModerationState.REJECTED) {
                OutlinedButton(
                  onClick = { onPromptReject(item) },
                  modifier =
                    Modifier.weight(1f)
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("admin_reject_listing_${space.id}"),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text(if (space.isApproved) "Unpublish / Reject" else "Reject Listing")
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg)) }
  }
}

@Composable
private fun DisputesModerationTabContent(
  items: List<AdminDisputeQueueItem>,
  onPromptDecision: (AdminDisputeQueueItem, DisputeStatus) -> Unit,
) {
  var filter by rememberSaveable { mutableStateOf("ALL") }
  val filtered =
    remember(items, filter) {
      when (filter) {
        "OPEN" ->
          items.filter {
            it.dispute.status == DisputeStatus.OPEN || it.dispute.status == DisputeStatus.UNDER_REVIEW
          }
        "CLOSED" ->
          items.filter {
            it.dispute.status == DisputeStatus.RESOLVED || it.dispute.status == DisputeStatus.DISMISSED
          }
        else -> items
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_disputes_queue_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        listOf(
          "ALL" to "All (${items.size})",
          "OPEN" to "Open / Review (${items.count { it.dispute.status == DisputeStatus.OPEN || it.dispute.status == DisputeStatus.UNDER_REVIEW }})",
          "CLOSED" to "Resolved / Dismissed (${items.count { it.dispute.status == DisputeStatus.RESOLVED || it.dispute.status == DisputeStatus.DISMISSED }})",
        ).forEach { (key, label) ->
          FilterChip(
            selected = filter == key,
            onClick = { filter = key },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("admin_disputes_filter_$key"),
          )
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.Gavel,
          title = "No Booking Disputes Reported",
          message =
            "Genuine booking disputes filed by Customers or Hosts will appear here for Admin review and resolution.",
          tag = "admin_disputes_empty_state",
        )
      }
    } else {
      items(filtered, key = { it.dispute.id }) { item ->
        val dispute = item.dispute
        val tone =
          when (dispute.status) {
            DisputeStatus.OPEN -> StatusTone.ERROR
            DisputeStatus.UNDER_REVIEW -> StatusTone.PENDING
            DisputeStatus.RESOLVED -> StatusTone.AVAILABLE
            DisputeStatus.DISMISSED -> StatusTone.INFO
          }
        val canModerate =
          dispute.status == DisputeStatus.OPEN || dispute.status == DisputeStatus.UNDER_REVIEW

        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("admin_dispute_card_${dispute.id}"),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${dispute.bookingCode} • ${dispute.category.displayName}",
                  style = ParkSpaceMonoCodeStyle,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "${item.spaceTitle}${item.city?.let { " (${it.displayName})" }.orEmpty()}",
                  style = MaterialTheme.typography.titleSmall,
                )
              }
              ParkSpaceStatusPill(
                label = dispute.status.displayName,
                tone = tone,
                modifier = Modifier.testTag("admin_dispute_status_${dispute.id}"),
              )
            }

            Text(
              text =
                "Filed by ${dispute.reporterRole.displayName} • Customer: ${item.customerName} • Host: ${item.hostName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = dispute.description, style = MaterialTheme.typography.bodySmall)

            if (!dispute.hostResponseNote.isNullOrBlank()) {
              Text(
                text = "Host Note: ${dispute.hostResponseNote}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
            if (!dispute.resolutionNote.isNullOrBlank()) {
              Text(
                text = "Resolution Note: ${dispute.resolutionNote}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            if (canModerate) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                if (dispute.status == DisputeStatus.OPEN) {
                  OutlinedButton(
                    onClick = { onPromptDecision(item, DisputeStatus.UNDER_REVIEW) },
                    modifier =
                      Modifier.weight(1f)
                        .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("admin_review_dispute_${dispute.id}"),
                    shape = MaterialTheme.shapes.small,
                  ) {
                    Text("Review", style = MaterialTheme.typography.labelSmall)
                  }
                }
                ParkSpacePrimaryButton(
                  text = "Resolve",
                  onClick = { onPromptDecision(item, DisputeStatus.RESOLVED) },
                  modifier = Modifier.weight(1f).testTag("admin_resolve_dispute_${dispute.id}"),
                )
                OutlinedButton(
                  onClick = { onPromptDecision(item, DisputeStatus.DISMISSED) },
                  modifier =
                    Modifier.weight(1f)
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("admin_dismiss_dispute_${dispute.id}"),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg)) }
  }
}

@Composable
private fun CityAnalyticsTabContent(analytics: PlatformAnalyticsSnapshot?) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_analytics_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    if (analytics == null) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.Analytics,
          title = "Analytics Restricted",
          message = "Unlock Private Super Admin Sandbox to view live marketplace analytics.",
          tag = "admin_analytics_empty_state",
        )
      }
      return@LazyColumn
    }

    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("admin_analytics_summary_card"),
      ) {
        Column(
          modifier = Modifier.padding(ParkSpaceSpacing.md),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Live Platform Snapshot",
              style = MaterialTheme.typography.titleMedium,
            )
            ParkSpaceStatusPill(label = "DEMO-ONLY METRICS", tone = StatusTone.INFO)
          }
          Text(
            text =
              "All totals and simulated PKR figures below are aggregated directly from local Room tables.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            AnalyticsStat("Customers", "${analytics.totalCustomersCount}")
            AnalyticsStat("Hosts (Appr.)", "${analytics.totalHostsCount} (${analytics.approvedHostsCount})")
            AnalyticsStat("Listings (Live)", "${analytics.totalListingsCount} (${analytics.approvedListingsCount})")
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            AnalyticsStat("Bookings", "${analytics.totalBookingsCount}")
            AnalyticsStat("Demo Gross", "Rs. ${analytics.totalDemoGrossVolumePkr}")
            AnalyticsStat("Demo Fee", "Rs. ${analytics.totalDemoCommissionFeesPkr}")
          }
          Text(
            text =
              "Simulated Host Net: Rs. ${analytics.totalDemoHostNetPkr} • Simulated Refunds: Rs. ${analytics.totalDemoRefundsPkr} • Open Disputes: ${analytics.openDisputesCount}",
            style = ParkSpaceMonoCodeStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    items(analytics.cityMetrics, key = { it.city.name }) { metric ->
      CityAnalyticsCard(metric = metric)
    }

    item { Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg)) }
  }
}

@Composable
private fun CityAnalyticsCard(metric: CityAnalyticsMetric) {
  val hasAnyActivity =
    metric.customersCount > 0 ||
      metric.hostsCount > 0 ||
      metric.totalListingsCount > 0 ||
      metric.totalBookingsCount > 0

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier.fillMaxWidth().testTag("admin_city_analytics_${metric.city.name.lowercase()}"),
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${metric.city.displayName} (${metric.city.provinceLabel})",
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = "Demo GMV: Rs. ${metric.demoGrossVolumePkr}",
          style = ParkSpaceMonoPriceStyle,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (!hasAnyActivity) {
        Text(
          text = "No marketplace activity recorded in ${metric.city.displayName} yet (0 listings, 0 bookings).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text =
            "Users: ${metric.customersCount} Customers • ${metric.hostsCount} Hosts (${metric.approvedHostsCount} approved)",
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text =
            "Supply: ${metric.totalListingsCount} Listings (${metric.approvedListingsCount} live, ${metric.pendingListingsCount} pending, ${metric.rejectedListingsCount} rejected) • ${metric.operationalBaysCount}/${metric.totalBaysCount} active bays",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text =
            "Bookings: ${metric.totalBookingsCount} total (${metric.activeBookingsCount} active, ${metric.completedBookingsCount} completed, ${metric.cancelledOrExpiredBookingsCount} cancelled/exp) • Disputes: ${metric.openDisputesCount} open / ${metric.resolvedDisputesCount} closed",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text =
            "Demo Financials (Simulated): Fee Rs. ${metric.demoCommissionFeesPkr} • Host Net Rs. ${metric.demoHostNetPkr} • Refunds Rs. ${metric.demoRefundsPkr}",
          style = ParkSpaceMonoCodeStyle,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun CommissionAndAuditTabContent(
  commissionRule: DemoCommissionRule,
  auditLogs: List<AdminAuditLogEntry>,
  onSaveCommissionRule: (percent: String, minFee: String, maxCap: String, note: String) -> Unit,
) {
  var percentInput by
    rememberSaveable(commissionRule.commissionPercent, commissionRule.updatedAtEpochMs) {
      mutableStateOf(commissionRule.commissionPercent.toString())
    }
  var minFeeInput by
    rememberSaveable(commissionRule.minFeePkr, commissionRule.updatedAtEpochMs) {
      mutableStateOf(commissionRule.minFeePkr.toString())
    }
  var maxCapInput by
    rememberSaveable(commissionRule.maxFeePkr, commissionRule.updatedAtEpochMs) {
      mutableStateOf(commissionRule.maxFeePkr.toString())
    }
  var noteInput by
    rememberSaveable(commissionRule.note, commissionRule.updatedAtEpochMs) {
      mutableStateOf(commissionRule.note)
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_config_and_logs_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("admin_commission_config_card"),
      ) {
        Column(
          modifier = Modifier.padding(ParkSpaceSpacing.md),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Icon(
                Icons.Outlined.Percent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Demo-Only Commission Rules",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            ParkSpaceStatusPill(
              label = if (commissionRule.isDefaultBaseline) "BASELINE (8%)" else "CUSTOM ACTIVE",
              tone = StatusTone.INFO,
            )
          }

          Text(
            text =
              "Simulated Demo platform fee applied to new Customer bookings (${commissionRule.commissionPercent}%, min Rs. ${commissionRule.minFeePkr}, cap Rs. ${commissionRule.maxFeePkr}). No real money or bank rails are connected.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            OutlinedTextField(
              value = percentInput,
              onValueChange = { percentInput = it },
              label = { Text("Fee % (1-30)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("admin_commission_percent_input"),
            )
            OutlinedTextField(
              value = minFeeInput,
              onValueChange = { minFeeInput = it },
              label = { Text("Min Rs.") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("admin_commission_min_input"),
            )
            OutlinedTextField(
              value = maxCapInput,
              onValueChange = { maxCapInput = it },
              label = { Text("Cap Rs.") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("admin_commission_max_input"),
            )
          }

          OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text("Demo Policy Note") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_commission_note_input"),
          )

          ParkSpacePrimaryButton(
            text = "Save Demo Commission Rule",
            onClick = { onSaveCommissionRule(percentInput, minFeeInput, maxCapInput, noteInput) },
            modifier = Modifier.fillMaxWidth().testTag("admin_save_commission_button"),
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = ParkSpaceSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Icon(
            Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Super Admin Audit Logs (${auditLogs.size})",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
    }

    if (auditLogs.isEmpty()) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.History,
          title = "No Admin Actions Recorded Yet",
          message =
            "Every Host KYC decision, listing approval/rejection, dispute update, and Demo commission rule update is logged here.",
          tag = "admin_audit_logs_empty_state",
        )
      }
    } else {
      items(auditLogs, key = { it.id }) { log ->
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("admin_audit_log_card_${log.id}"),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ParkSpaceStatusPill(label = log.actionType.displayName, tone = StatusTone.INFO)
              Text(
                text = formatAdminTimestamp(log.createdAtEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(text = log.targetLabel, style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${log.outcomeSummary} • Actor: ${log.actorLabel}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!log.reasonOrNote.isNullOrBlank()) {
              Text(
                text = "Reason / Note: ${log.reasonOrNote}",
                style = ParkSpaceMonoCodeStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg)) }
  }
}

@Composable
private fun AdminModerationReasonDialog(
  prompt: AdminModerationPrompt,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  val requiresReason =
    when (prompt) {
      is AdminModerationPrompt.HostDecision -> !prompt.approve
      is AdminModerationPrompt.ListingDecision -> !prompt.approve
      is AdminModerationPrompt.DisputeDecision -> true
    }

  val dialogTitle =
    when (prompt) {
      is AdminModerationPrompt.HostDecision -> prompt.title
      is AdminModerationPrompt.ListingDecision -> prompt.title
      is AdminModerationPrompt.DisputeDecision ->
        "Dispute ${prompt.bookingCode}: ${prompt.targetStatus.displayName}"
    }

  var noteInput by rememberSaveable(prompt.hashCode()) { mutableStateOf("") }
  var localError by rememberSaveable(prompt.hashCode()) { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(dialogTitle) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        Text(
          text =
            if (requiresReason) {
              "Enter an audit-recorded reason or resolution note (at least 4 characters)."
            } else {
              "Add an optional verification/approval note for the audit log (or leave blank for default)."
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = noteInput,
          onValueChange = {
            noteInput = it
            localError = null
          },
          label = { Text(if (requiresReason) "Reason / Note *" else "Approval Note (Optional)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("admin_moderation_note_input"),
        )
        if (localError != null) {
          Text(
            text = localError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (requiresReason && noteInput.trim().length < 4) {
            localError = "Enter at least 4 characters."
          } else {
            onConfirm(noteInput)
          }
        },
        modifier = Modifier.testTag("admin_confirm_moderation_button"),
      ) {
        Text("Confirm")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun AnalyticsStat(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = ParkSpaceMonoPriceStyle,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun AdminEmptyCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  message: String,
  tag: String,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = Modifier.fillMaxWidth().testTag(tag),
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
      Text(text = title, style = MaterialTheme.typography.titleSmall)
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private val ADMIN_DATE_FORMATTER: DateTimeFormatter =
  DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.of("Asia/Karachi"))

private fun formatAdminTimestamp(epochMs: Long): String =
  ADMIN_DATE_FORMATTER.format(Instant.ofEpochMilli(epochMs))
