package com.example.feature.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.AdminCityActivityChartCard
import com.example.core.designsystem.components.AdminModerationHealthChartCard
import com.example.core.designsystem.components.ParkSpaceAdminBottomBar
import com.example.core.designsystem.components.ParkSpaceAdminHeader
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassChip
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
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
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonDark
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens
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
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  onSignOut: (() -> Unit)? = null,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val adminAccent = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val onPrimaryAccent = if (isDarkTheme) Emerald950 else Color.White

  var activePrompt by remember { mutableStateOf<AdminModerationPrompt?>(null) }

  val pendingKycCount =
    uiState.hostKycQueue.count {
      it.hostProfile.verificationStatus == HostVerificationStatus.PENDING_REVIEW
    }
  val pendingListingsCount =
    uiState.listingQueue.count {
      it.moderationState == AdminListingModerationState.PENDING_REVIEW
    }
  val openDisputesCount =
    uiState.disputesQueue.count {
      it.dispute.status == DisputeStatus.OPEN || it.dispute.status == DisputeStatus.UNDER_REVIEW
    }

  ParkSpaceAtmosphericBackground(
    isDarkTheme = isDarkTheme,
    modifier = modifier.testTag("private_admin_console_screen"),
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (uiState.isAuthorizedAdmin) {
          ParkSpaceAdminBottomBar(
            selectedTab = uiState.activeTab,
            isDarkTheme = isDarkTheme,
            pendingKycCount = pendingKycCount,
            pendingListingsCount = pendingListingsCount,
            openDisputesCount = openDisputesCount,
            onSelectTab = onSelectTab,
          )
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier
              .widthIn(max = ParkSpaceSpacing.maxReadableWidth)
              .fillMaxSize()
              .padding(horizontal = ParkSpaceSpacing.md),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Spacer(modifier = Modifier.height(2.dp))

          // Compact Mobile-First Private Admin Header
          ParkSpaceAdminHeader(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onBack = onBack,
            onSelectTab = onSelectTab,
            onOpenSettings = onOpenSettings,
            onSignOut = onSignOut,
          )

          if (!uiState.isAuthorizedAdmin) {
            ParkSpaceGlassSurface(
              modifier = Modifier.fillMaxWidth().testTag("admin_unauthorized_card"),
              accentGlow = adminAccent.copy(alpha = 0.28f),
              borderColor = adminAccent.copy(alpha = 0.5f),
              contentPaddingDp = 20,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Lock,
                  contentDescription = null,
                  tint = adminAccent,
                )
                Text(
                  text = "Private Admin Access Required",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
              }
              Text(
                text =
                  "Governance queues are locked. Unlock via the Private Admin Gate in Demo Sandbox mode to moderate Hosts, listings, disputes, or rules.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              ParkSpaceLiquidGlassButton(
                text = "Return to Welcome",
                onClick = onBack,
                accentColor = primaryAccent,
                contentColor = onPrimaryAccent,
                modifier = Modifier.fillMaxWidth().testTag("admin_unauthorized_return_button"),
              )
            }
            return@Column
          }

          // Compact Governance Status Bar
          ParkSpaceGlassSurface(
            modifier = Modifier.fillMaxWidth().testTag("admin_shell_card"),
            cornerRadiusDp = 18,
            accentGlow = adminAccent.copy(alpha = 0.2f),
            borderColor = adminAccent.copy(alpha = 0.35f),
            contentPaddingDp = 12,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
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
                  tint = adminAccent,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text =
                    "KYC $pendingKycCount • Listings $pendingListingsCount • Disputes $openDisputesCount",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              ParkSpaceStatusPill(label = "DEMO ADMIN", tone = StatusTone.ERROR)
            }
          }

          if (uiState.actionFeedback != null) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth().testTag("admin_feedback_banner"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = ParkSpaceSpacing.md, vertical = ParkSpaceSpacing.xs),
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
                  modifier =
                    Modifier
                      .size(ParkSpaceSpacing.minTouchTarget)
                      .testTag("admin_dismiss_feedback_button"),
                ) {
                  Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          if (uiState.errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
              modifier = Modifier.fillMaxWidth().testTag("admin_error_banner"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = ParkSpaceSpacing.md, vertical = ParkSpaceSpacing.xs),
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
                  modifier =
                    Modifier
                      .size(ParkSpaceSpacing.minTouchTarget)
                      .testTag("admin_dismiss_error_button"),
                ) {
                  Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          // Active Governance Workspace Tab
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (uiState.activeTab) {
              AdminConsoleTab.KYC_QUEUE -> {
                HostKycQueueTabContent(
                  items = uiState.hostKycQueue,
                  isDarkTheme = isDarkTheme,
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
                  isDarkTheme = isDarkTheme,
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
                  isDarkTheme = isDarkTheme,
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
                CityAnalyticsTabContent(
                  analytics = uiState.analytics,
                  commissionRule = uiState.commissionRule,
                  isDarkTheme = isDarkTheme,
                )
              }

              AdminConsoleTab.CONFIG_AND_LOGS -> {
                CommissionAndAuditTabContent(
                  commissionRule = uiState.commissionRule,
                  auditLogs = uiState.auditLogs,
                  isDarkTheme = isDarkTheme,
                  onSaveCommissionRule = onUpdateCommissionRule,
                )
              }
            }
          }
        }
      }
    }
  }

  activePrompt?.let { prompt ->
    AdminModerationReasonDialog(
      prompt = prompt,
      isDarkTheme = isDarkTheme,
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
  isDarkTheme: Boolean,
  onPromptApprove: (HostKycQueueItem) -> Unit,
  onPromptReject: (HostKycQueueItem) -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val onPrimaryAccent = if (isDarkTheme) Emerald950 else Color.White
  val chipAccent = if (isDarkTheme) Brass400 else Brass600

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
    contentPadding = PaddingValues(bottom = ParkSpaceSpacing.xl),
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
          ParkSpaceLiquidGlassChip(
            label = label,
            selected = filter == key,
            accentColor = chipAccent,
            onClick = { filter = key },
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
            "Self-registered Hosts awaiting verification or already moderated will appear here automatically.",
          isDarkTheme = isDarkTheme,
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
        ParkSpaceGlassSurface(
          modifier = Modifier.fillMaxWidth().testTag("admin_kyc_card_${item.user.id}"),
          accentGlow = semantic.hostGlow,
          contentPaddingDp = 16,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = ParkSpaceSpacing.xs)) {
              Text(
                text = item.hostProfile.displayBusinessAlias,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${item.user.fullName} • ${item.user.emailMasked}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
              "${item.hostProfile.operatingCities.joinToString { it.displayName }} • ${item.listingsCount} spaces (${item.approvedListingsCount} live) • ${formatAdminTimestamp(item.hostProfile.joinedEpochMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          if (!item.hostProfile.verificationNote.isNullOrBlank()) {
            Text(
              text = "KYC Note: ${item.hostProfile.verificationNote}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color =
                if (status == HostVerificationStatus.REJECTED) MaterialTheme.colorScheme.error
                else primaryAccent,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            if (status != HostVerificationStatus.APPROVED) {
              ParkSpaceLiquidGlassButton(
                text = "Approve Host",
                onClick = { onPromptApprove(item) },
                accentColor = primaryAccent,
                contentColor = onPrimaryAccent,
                modifier = Modifier.weight(1f).testTag("admin_approve_host_${item.user.id}"),
              )
            }
            if (status != HostVerificationStatus.REJECTED) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Reject Host",
                onClick = { onPromptReject(item) },
                accentColor = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson,
                modifier = Modifier.weight(1f).testTag("admin_reject_host_${item.user.id}"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ListingModerationTabContent(
  items: List<AdminListingQueueItem>,
  isDarkTheme: Boolean,
  onPromptApprove: (AdminListingQueueItem) -> Unit,
  onPromptReject: (AdminListingQueueItem) -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val onPrimaryAccent = if (isDarkTheme) Emerald950 else Color.White

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
    contentPadding = PaddingValues(bottom = ParkSpaceSpacing.xl),
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
          ParkSpaceLiquidGlassChip(
            label = label,
            selected = filter == key,
            accentColor = primaryAccent,
            onClick = { filter = key },
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
            "When self-registered Hosts submit parking spaces, photos, pricing, and approve/reject actions appear here.",
          isDarkTheme = isDarkTheme,
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
        ParkSpaceGlassSurface(
          modifier = Modifier.fillMaxWidth().testTag("admin_listing_card_${space.id}"),
          accentGlow = semantic.customerGlow,
          contentPaddingDp = 16,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = ParkSpaceSpacing.xs)) {
              Text(
                text = space.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  "${space.location.areaLabel}, ${space.location.city.displayName} • ${space.parkingType.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            height = 96.dp,
          )

          Text(
            text = space.description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              "Host: ${item.hostProfile?.displayBusinessAlias ?: item.hostUser?.fullName ?: space.hostId} (${item.hostProfile?.verificationStatus?.displayName ?: "Unknown KYC"})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              "Rs. ${space.hourlyRatePkr}/hr • Rs. ${space.dailyRatePkr}/day" +
                (if (space.monthlyEnabled && space.monthlyRatePkr != null) " • Rs. ${space.monthlyRatePkr}/mo" else "") +
                " • ${space.availableBays}/${item.baysCount} bays • ${space.schedule.formattedHours()}",
            style = ParkSpaceMonoCodeStyle,
            color = primaryAccent,
          )
          Text(
            text =
              "${space.supportedVehicles.joinToString { it.displayName }} • ${space.photoUris.size} photos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          if (!space.moderationNote.isNullOrBlank()) {
            Text(
              text = "Moderation Note: ${space.moderationNote}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color =
                if (item.moderationState == AdminListingModerationState.REJECTED) {
                  MaterialTheme.colorScheme.error
                } else {
                  primaryAccent
                },
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            if (!space.isApproved) {
              ParkSpaceLiquidGlassButton(
                text = "Approve Listing",
                onClick = { onPromptApprove(item) },
                accentColor = primaryAccent,
                contentColor = onPrimaryAccent,
                modifier = Modifier.weight(1f).testTag("admin_approve_listing_${space.id}"),
              )
            }
            if (item.moderationState != AdminListingModerationState.REJECTED) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = if (space.isApproved) "Unpublish" else "Reject Listing",
                onClick = { onPromptReject(item) },
                accentColor = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson,
                modifier = Modifier.weight(1f).testTag("admin_reject_listing_${space.id}"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DisputesModerationTabContent(
  items: List<AdminDisputeQueueItem>,
  isDarkTheme: Boolean,
  onPromptDecision: (AdminDisputeQueueItem, DisputeStatus) -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val onPrimaryAccent = if (isDarkTheme) Emerald950 else Color.White
  val disputeAccent = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson

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
    contentPadding = PaddingValues(bottom = ParkSpaceSpacing.xl),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        listOf(
          "ALL" to "All (${items.size})",
          "OPEN" to "Open (${items.count { it.dispute.status == DisputeStatus.OPEN || it.dispute.status == DisputeStatus.UNDER_REVIEW }})",
          "CLOSED" to "Closed (${items.count { it.dispute.status == DisputeStatus.RESOLVED || it.dispute.status == DisputeStatus.DISMISSED }})",
        ).forEach { (key, label) ->
          ParkSpaceLiquidGlassChip(
            label = label,
            selected = filter == key,
            accentColor = disputeAccent,
            onClick = { filter = key },
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
          isDarkTheme = isDarkTheme,
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

        ParkSpaceGlassSurface(
          modifier = Modifier.fillMaxWidth().testTag("admin_dispute_card_${dispute.id}"),
          accentGlow = disputeAccent.copy(alpha = 0.22f),
          contentPaddingDp = 16,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = ParkSpaceSpacing.xs)) {
              Text(
                text = "${dispute.bookingCode} • ${dispute.category.displayName}",
                style = ParkSpaceMonoCodeStyle,
                color = primaryAccent,
              )
              Text(
                text = "${item.spaceTitle}${item.city?.let { " (${it.displayName})" }.orEmpty()}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
              "By ${dispute.reporterRole.displayName} • Customer: ${item.customerName} • Host: ${item.hostName}",
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
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = primaryAccent,
            )
          }

          if (canModerate) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              if (dispute.status == DisputeStatus.OPEN) {
                ParkSpaceLiquidGlassSecondaryButton(
                  text = "Review",
                  onClick = { onPromptDecision(item, DisputeStatus.UNDER_REVIEW) },
                  accentColor = if (isDarkTheme) Brass400 else Brass600,
                  modifier = Modifier.weight(1f).testTag("admin_review_dispute_${dispute.id}"),
                )
              }
              ParkSpaceLiquidGlassButton(
                text = "Resolve",
                onClick = { onPromptDecision(item, DisputeStatus.RESOLVED) },
                accentColor = primaryAccent,
                contentColor = onPrimaryAccent,
                modifier = Modifier.weight(1f).testTag("admin_resolve_dispute_${dispute.id}"),
              )
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Dismiss",
                onClick = { onPromptDecision(item, DisputeStatus.DISMISSED) },
                accentColor = disputeAccent,
                modifier = Modifier.weight(1f).testTag("admin_dismiss_dispute_${dispute.id}"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CityAnalyticsTabContent(
  analytics: PlatformAnalyticsSnapshot?,
  commissionRule: DemoCommissionRule,
  isDarkTheme: Boolean,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val brassAccent = if (isDarkTheme) Brass400 else Brass600

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_analytics_list"),
    contentPadding = PaddingValues(bottom = ParkSpaceSpacing.xl),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    if (analytics == null) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.Analytics,
          title = "Analytics Restricted",
          message = "Unlock Private Super Admin Sandbox to view live marketplace analytics.",
          isDarkTheme = isDarkTheme,
          tag = "admin_analytics_empty_state",
        )
      }
      return@LazyColumn
    }

    // 1. Real Room Summary Metrics Card
    item {
      ParkSpaceGlassSurface(
        modifier = Modifier.fillMaxWidth().testTag("admin_analytics_summary_card"),
        accentGlow = semantic.customerGlow,
        contentPaddingDp = 16,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Live Platform Snapshot",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          )
          ParkSpaceStatusPill(
            label = "${commissionRule.commissionPercent}% DEMO FEE",
            tone = StatusTone.INFO,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          AnalyticsStatTile(
            label = "Customers",
            value = "${analytics.totalCustomersCount}",
            accent = primaryAccent,
            modifier = Modifier.weight(1f),
          )
          AnalyticsStatTile(
            label = "Hosts (Appr.)",
            value = "${analytics.totalHostsCount} (${analytics.approvedHostsCount})",
            accent = brassAccent,
            modifier = Modifier.weight(1f),
          )
          AnalyticsStatTile(
            label = "Spaces (Live)",
            value = "${analytics.totalListingsCount} (${analytics.approvedListingsCount})",
            accent = primaryAccent,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          AnalyticsStatTile(
            label = "Bookings",
            value = "${analytics.totalBookingsCount}",
            accent = primaryAccent,
            modifier = Modifier.weight(1f),
          )
          AnalyticsStatTile(
            label = "Demo Gross",
            value = "${AppEnvironment.CURRENCY_SYMBOL} ${analytics.totalDemoGrossVolumePkr}",
            accent = brassAccent,
            modifier = Modifier.weight(1f),
          )
          AnalyticsStatTile(
            label = "Demo Fee",
            value = "${AppEnvironment.CURRENCY_SYMBOL} ${analytics.totalDemoCommissionFeesPkr}",
            accent = primaryAccent,
            modifier = Modifier.weight(1f),
          )
        }

        Text(
          text =
            "Simulated Host Net: ${AppEnvironment.CURRENCY_SYMBOL} ${analytics.totalDemoHostNetPkr} • Refunds: ${AppEnvironment.CURRENCY_SYMBOL} ${analytics.totalDemoRefundsPkr} • Open Disputes: ${analytics.openDisputesCount}",
          style = ParkSpaceMonoCodeStyle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // 2. Native Compose Canvas Moderation Health Chart
    item {
      AdminModerationHealthChartCard(
        analytics = analytics,
        isDarkTheme = isDarkTheme,
      )
    }

    // 3. Native Compose Canvas Supported-City Activity Chart
    item {
      AdminCityActivityChartCard(
        analytics = analytics,
        isDarkTheme = isDarkTheme,
      )
    }

    // 4. Per-City Breakdown Cards
    items(analytics.cityMetrics, key = { it.city.name }) { metric ->
      CityAnalyticsCard(metric = metric, isDarkTheme = isDarkTheme)
    }
  }
}

@Composable
private fun AnalyticsStatTile(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    border = BorderStroke(1.dp, ParkSpaceThemeTokens.semantic.glassBorder),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = ParkSpaceMonoPriceStyle.copy(fontWeight = FontWeight.Bold),
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CityAnalyticsCard(
  metric: CityAnalyticsMetric,
  isDarkTheme: Boolean,
) {
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val hasAnyActivity =
    metric.customersCount > 0 ||
      metric.hostsCount > 0 ||
      metric.totalListingsCount > 0 ||
      metric.totalBookingsCount > 0

  ParkSpaceGlassSurface(
    modifier =
      Modifier.fillMaxWidth().testTag("admin_city_analytics_${metric.city.name.lowercase()}"),
    accentGlow = ParkSpaceThemeTokens.semantic.customerGlow,
    contentPaddingDp = 14,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${metric.city.displayName} (${metric.city.provinceLabel})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      )
      Text(
        text = "Demo GMV: ${AppEnvironment.CURRENCY_SYMBOL} ${metric.demoGrossVolumePkr}",
        style = ParkSpaceMonoPriceStyle,
        color = primaryAccent,
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
          "${metric.customersCount} Customers • ${metric.hostsCount} Hosts (${metric.approvedHostsCount} approved) • ${metric.totalListingsCount} Spaces (${metric.approvedListingsCount} live)",
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        text =
          "Bays: ${metric.operationalBaysCount}/${metric.totalBaysCount} active • Bookings: ${metric.totalBookingsCount} (${metric.activeBookingsCount} active, ${metric.completedBookingsCount} done) • Disputes: ${metric.openDisputesCount} open",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text =
          "Fee: Rs. ${metric.demoCommissionFeesPkr} • Host Net: Rs. ${metric.demoHostNetPkr} • Refunds: Rs. ${metric.demoRefundsPkr}",
        style = ParkSpaceMonoCodeStyle,
        color = primaryAccent,
      )
    }
  }
}

@Composable
private fun CommissionAndAuditTabContent(
  commissionRule: DemoCommissionRule,
  auditLogs: List<AdminAuditLogEntry>,
  isDarkTheme: Boolean,
  onSaveCommissionRule: (percent: String, minFee: String, maxCap: String, note: String) -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val onPrimaryAccent = if (isDarkTheme) Emerald950 else Color.White

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

  val fieldColors =
    OutlinedTextFieldDefaults.colors(
      focusedContainerColor = semantic.glassSurfaceElevated,
      unfocusedContainerColor = semantic.glassSurface,
      focusedBorderColor = primaryAccent.copy(alpha = 0.75f),
      unfocusedBorderColor = semantic.glassBorder,
    )

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_config_and_logs_list"),
    contentPadding = PaddingValues(bottom = ParkSpaceSpacing.xl),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      ParkSpaceGlassSurface(
        modifier = Modifier.fillMaxWidth().testTag("admin_commission_config_card"),
        accentGlow = semantic.hostGlow,
        contentPaddingDp = 16,
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
              tint = primaryAccent,
            )
            Text(
              text = "Demo Commission Rules",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
          }
          ParkSpaceStatusPill(
            label = if (commissionRule.isDefaultBaseline) "BASELINE (8%)" else "CUSTOM ACTIVE",
            tone = StatusTone.INFO,
          )
        }

        Text(
          text =
            "Simulated Demo fee applied to new bookings (${commissionRule.commissionPercent}%, min Rs. ${commissionRule.minFeePkr}, cap Rs. ${commissionRule.maxFeePkr}). Demo-only.",
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
            label = { Text("Fee %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.weight(1f).testTag("admin_commission_percent_input"),
          )
          OutlinedTextField(
            value = minFeeInput,
            onValueChange = { minFeeInput = it },
            label = { Text("Min Rs.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.weight(1f).testTag("admin_commission_min_input"),
          )
          OutlinedTextField(
            value = maxCapInput,
            onValueChange = { maxCapInput = it },
            label = { Text("Cap Rs.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.weight(1f).testTag("admin_commission_max_input"),
          )
        }

        OutlinedTextField(
          value = noteInput,
          onValueChange = { noteInput = it },
          label = { Text("Demo Policy Note") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = fieldColors,
          modifier = Modifier.fillMaxWidth().testTag("admin_commission_note_input"),
        )

        ParkSpaceLiquidGlassButton(
          text = "Save Demo Commission Rule",
          onClick = { onSaveCommissionRule(percentInput, minFeeInput, maxCapInput, noteInput) },
          accentColor = primaryAccent,
          contentColor = onPrimaryAccent,
          modifier = Modifier.fillMaxWidth().testTag("admin_save_commission_button"),
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = ParkSpaceSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        Icon(
          Icons.Outlined.History,
          contentDescription = null,
          tint = primaryAccent,
          modifier = Modifier.size(19.dp),
        )
        Text(
          text = "Super Admin Audit Logs (${auditLogs.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
    }

    if (auditLogs.isEmpty()) {
      item {
        AdminEmptyCard(
          icon = Icons.Outlined.History,
          title = "No Admin Actions Recorded Yet",
          message =
            "Every Host KYC decision, listing moderation, dispute update, and Demo commission update is logged here.",
          isDarkTheme = isDarkTheme,
          tag = "admin_audit_logs_empty_state",
        )
      }
    } else {
      items(auditLogs, key = { it.id }) { log ->
        ParkSpaceGlassSurface(
          modifier = Modifier.fillMaxWidth().testTag("admin_audit_log_card_${log.id}"),
          cornerRadiusDp = 18,
          accentGlow = semantic.customerGlow,
          contentPaddingDp = 14,
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
          Text(
            text = log.targetLabel,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            text = "${log.outcomeSummary} • ${log.actorLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (!log.reasonOrNote.isNullOrBlank()) {
            Text(
              text = "Note: ${log.reasonOrNote}",
              style = ParkSpaceMonoCodeStyle,
              color = primaryAccent,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminModerationReasonDialog(
  prompt: AdminModerationPrompt,
  isDarkTheme: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val onPrimaryAccent = if (isDarkTheme) Emerald950 else Color.White
  val dangerAccent = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson

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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    ParkSpaceGlassSurface(
      modifier =
        Modifier
          .padding(horizontal = ParkSpaceSpacing.screenHorizontal)
          .widthIn(max = 460.dp)
          .fillMaxWidth(),
      cornerRadiusDp = 24,
      accentGlow =
        if (requiresReason) dangerAccent.copy(alpha = 0.25f)
        else ParkSpaceThemeTokens.semantic.customerGlow,
      contentPaddingDp = 20,
    ) {
      Text(
        text = dialogTitle,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
      Text(
        text =
          if (requiresReason) {
            "Enter an audit-recorded reason or resolution note (at least 4 characters)."
          } else {
            "Add an optional verification/approval note for the audit log."
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("admin_moderation_note_input"),
      )
      if (localError != null) {
        Text(
          text = localError.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        ParkSpaceLiquidGlassSecondaryButton(
          text = "Cancel",
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        )
        ParkSpaceLiquidGlassButton(
          text = "Confirm",
          onClick = {
            if (requiresReason && noteInput.trim().length < 4) {
              localError = "Enter at least 4 characters."
            } else {
              onConfirm(noteInput)
            }
          },
          accentColor = if (requiresReason) dangerAccent else primaryAccent,
          contentColor = if (requiresReason) Color.White else onPrimaryAccent,
          modifier = Modifier.weight(1f).testTag("admin_confirm_moderation_button"),
        )
      }
    }
  }
}

@Composable
private fun AdminEmptyCard(
  icon: ImageVector,
  title: String,
  message: String,
  isDarkTheme: Boolean,
  tag: String,
) {
  val accent = if (isDarkTheme) Jade400 else Emerald900
  ParkSpaceGlassSurface(
    modifier = Modifier.fillMaxWidth().testTag(tag),
    accentGlow = ParkSpaceThemeTokens.semantic.customerGlow,
    contentPaddingDp = 22,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(30.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      )
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
