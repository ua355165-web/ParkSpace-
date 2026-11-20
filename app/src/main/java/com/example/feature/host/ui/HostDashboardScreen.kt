package com.example.feature.host.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.HostActivityLineChartCard
import com.example.core.designsystem.components.HostBottomTab
import com.example.core.designsystem.components.HostEarningsTrendCard
import com.example.core.designsystem.components.HostHelpSupportDialog
import com.example.core.designsystem.components.HostPreferencesDialog
import com.example.core.designsystem.components.HostPrivacyTermsDialog
import com.example.core.designsystem.components.HostSelectSpaceForActionDialog
import com.example.core.designsystem.components.HostStatusDonutChartCard
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceHostBottomBar
import com.example.core.designsystem.components.ParkSpaceHostHeader
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassChip
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingSpace
import com.example.feature.booking.ui.formatDateTimePkt
import com.example.feature.discovery.ui.ParkingSpaceMediaBanner
import com.example.feature.host.HostDashboardTab
import com.example.feature.host.HostDashboardUiState
import com.example.feature.host.HostDashboardViewModel
import com.example.feature.host.domain.HostDashboardAnalytics
import com.example.feature.host.domain.HostDashboardAnalyticsCalculator
import com.example.feature.host.domain.HostDemoLedgerSummary
import com.example.feature.host.domain.HostLedgerCalculator
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.InfoCobalt
import com.example.ui.theme.InfoCobaltDark
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

private enum class HostSpacePickerMode {
  BAYS,
  SCHEDULE,
}

@Composable
fun HostDashboardScreen(
  uiState: HostDashboardUiState,
  viewModel: HostDashboardViewModel,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenNotifications: () -> Unit = {},
  onOpenMessages: () -> Unit = {},
  onOpenReviews: () -> Unit = {},
  onOpenDisputes: () -> Unit = {},
  onOpenBookingChat: (String) -> Unit = {},
  onOpenBookingDispute: (String) -> Unit = {},
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  onOpenCustomerDashboardOrActivate: () -> Unit = {},
  onSignOut: () -> Unit = onBack,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600

  var selectedBookingForAction by remember { mutableStateOf<Booking?>(null) }
  var bookingFilter by rememberSaveable { mutableStateOf("ALL") }
  var showHostSettingsDialog by rememberSaveable { mutableStateOf(false) }
  var showHelpDialog by rememberSaveable { mutableStateOf(false) }
  var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
  var spacePickerMode by rememberSaveable { mutableStateOf<HostSpacePickerMode?>(null) }

  val analytics =
    remember(uiState.spaces, uiState.bookings, uiState.ledger) {
      HostDashboardAnalyticsCalculator.compute(
        spaces = uiState.spaces,
        bookings = uiState.bookings,
        ledger = uiState.ledger,
      )
    }

  val selectedBottomTab =
    when (uiState.activeTab) {
      HostDashboardTab.OVERVIEW -> HostBottomTab.OVERVIEW
      HostDashboardTab.LISTINGS -> HostBottomTab.LISTINGS
      HostDashboardTab.BOOKINGS -> HostBottomTab.BOOKINGS
      HostDashboardTab.LEDGER -> HostBottomTab.LEDGER
    }

  val verificationStatus = uiState.hostProfile?.verificationStatus ?: HostVerificationStatus.PENDING_REVIEW
  val hostContextLabel =
    uiState.hostProfile?.displayBusinessAlias?.takeIf { it.isNotBlank() }
      ?: uiState.activeUser?.city?.displayName
      ?: "Host Console"

  val handleManageBaysQuickAction: () -> Unit = {
    when {
      uiState.spaces.isEmpty() -> {
        viewModel.showActionFeedback("Create a parking listing first before managing bays.")
      }
      uiState.spaces.size == 1 -> {
        viewModel.openBaysManager(uiState.spaces.first())
      }
      else -> {
        spacePickerMode = HostSpacePickerMode.BAYS
      }
    }
  }

  val handleManageAvailabilityQuickAction: () -> Unit = {
    when {
      uiState.spaces.isEmpty() -> {
        viewModel.showActionFeedback("Create a parking listing first before setting availability schedules.")
      }
      uiState.spaces.size == 1 -> {
        viewModel.openScheduleManager(uiState.spaces.first())
      }
      else -> {
        spacePickerMode = HostSpacePickerMode.SCHEDULE
      }
    }
  }

  ParkSpaceAtmosphericBackground(
    isDarkTheme = isDarkTheme,
    modifier = modifier.testTag("host_dashboard_screen"),
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        ParkSpaceHostBottomBar(
          selectedTab = selectedBottomTab,
          isDarkTheme = isDarkTheme,
          pendingBookingsBadge = analytics.summary.upcomingBookingsCount + analytics.summary.activeBookingsCount,
          onSelectTab = { bottomTab ->
            val targetHostTab = bottomTab.hostTab
            if (targetHostTab != null) {
              viewModel.selectTab(targetHostTab)
            } else {
              onOpenSettings()
            }
          },
        )
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
              .padding(horizontal = ParkSpaceSpacing.screenHorizontal),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Spacer(modifier = Modifier.height(2.dp))

          // 1. Clean Global Host Header (Theme Left • Branding Center • Menu Right)
          ParkSpaceHostHeader(
            hostContextLabel = hostContextLabel,
            isDarkTheme = isDarkTheme,
            showDualRoleOption = uiState.activeUser?.isSelfRegisteredDemoAccount == true,
            hasCompanionCustomerAccount = uiState.hasCompanionCustomerAccount,
            onToggleTheme = onToggleTheme,
            onOpenHostOverview = { viewModel.selectTab(HostDashboardTab.OVERVIEW) },
            onOpenCustomerDashboardOrActivate = onOpenCustomerDashboardOrActivate,
            onOpenProfile = onOpenSettings,
            onOpenSettings = { showHostSettingsDialog = true },
            onOpenHelpSupport = { showHelpDialog = true },
            onOpenPrivacyTerms = { showPrivacyDialog = true },
            onSignOut = onSignOut,
          )

          // Feedback Banner
          if (uiState.actionFeedback != null) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth().testTag("host_feedback_banner"),
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
                  Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Text(
                    text = uiState.actionFeedback.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                IconButton(
                  onClick = viewModel::dismissFeedback,
                  modifier =
                    Modifier
                      .size(ParkSpaceSpacing.minTouchTarget)
                      .testTag("host_dismiss_feedback_button"),
                ) {
                  Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          // Error Banner
          if (uiState.errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
              modifier = Modifier.fillMaxWidth().testTag("host_error_banner"),
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
                  Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                  Text(
                    text = uiState.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
                IconButton(
                  onClick = viewModel::dismissFeedback,
                  modifier =
                    Modifier
                      .size(ParkSpaceSpacing.minTouchTarget)
                      .testTag("host_dismiss_error_button"),
                ) {
                  Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          if (uiState.isLoading) {
            Box(
              modifier = Modifier.fillMaxSize().testTag("host_loading_state"),
              contentAlignment = Alignment.Center,
            ) {
              ParkSpaceGlassSurface(
                accentGlow = semantic.hostGlow,
                contentPaddingDp = 24,
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
                ) {
                  CircularProgressIndicator(color = hostAccent)
                  Text(
                    text = "Loading Host workspace...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            // Active Host Workspace Tab
            when (uiState.activeTab) {
              HostDashboardTab.OVERVIEW -> {
                HostOverviewTabContent(
                  uiState = uiState,
                  analytics = analytics,
                  verificationStatus = verificationStatus,
                  isDarkTheme = isDarkTheme,
                  onSelectTab = viewModel::selectTab,
                  onStartCreateListing = viewModel::startCreateListing,
                  onManageBaysQuickAction = handleManageBaysQuickAction,
                  onManageAvailabilityQuickAction = handleManageAvailabilityQuickAction,
                  onSelectBooking = { selectedBookingForAction = it },
                  onOpenNotifications = onOpenNotifications,
                  onOpenMessages = onOpenMessages,
                  onOpenReviews = onOpenReviews,
                  onOpenDisputes = onOpenDisputes,
                )
              }
              HostDashboardTab.LISTINGS -> {
                HostListingsTabContent(
                  spaces = uiState.spaces,
                  verificationStatus = verificationStatus,
                  isDarkTheme = isDarkTheme,
                  onStartCreateListing = viewModel::startCreateListing,
                  onEditSpace = viewModel::startEditListing,
                  onManageBays = viewModel::openBaysManager,
                  onManageSchedule = viewModel::openScheduleManager,
                )
              }
              HostDashboardTab.BOOKINGS -> {
                HostBookingsTabContent(
                  bookings = uiState.bookings,
                  spaces = uiState.spaces,
                  selectedFilter = bookingFilter,
                  isDarkTheme = isDarkTheme,
                  onSelectFilter = { bookingFilter = it },
                  onQuickCheckIn = viewModel::hostCheckIn,
                  onQuickCheckOut = viewModel::hostCheckOut,
                  onSelectBooking = { selectedBookingForAction = it },
                )
              }
              HostDashboardTab.LEDGER -> {
                HostLedgerTabContent(
                  ledger = uiState.ledger,
                  isDarkTheme = isDarkTheme,
                )
              }
            }
          }
        }
      }
    }
  }

  // Listing Editor Dialog
  if (uiState.isCreatingNewListing || uiState.editingListing != null) {
    val hostId = uiState.activeUser?.id.orEmpty()
    HostListingEditorDialog(
      hostId = hostId,
      hostVerificationStatus = verificationStatus,
      existingSpace = uiState.editingListing,
      onDismiss = viewModel::dismissListingEditor,
      onSaveListing = viewModel::saveListing,
    )
  }

  // Bays Manager Dialog
  if (uiState.selectedSpaceForBays != null) {
    HostBaysManagerDialog(
      space = uiState.selectedSpaceForBays,
      bays = uiState.baysForSelectedSpace,
      onDismiss = viewModel::dismissBaysManager,
      onAddBay = viewModel::addBay,
      onUpdateBay = viewModel::updateBay,
      onToggleBayActive = viewModel::toggleBayActive,
      errorMessage = uiState.errorMessage,
    )
  }

  // Schedule Manager Dialog
  if (uiState.selectedSpaceForSchedule != null) {
    HostScheduleManagerDialog(
      space = uiState.selectedSpaceForSchedule,
      onDismiss = viewModel::dismissScheduleManager,
      onSaveSchedule = viewModel::saveSchedule,
    )
  }

  // Booking Detail / Action Dialog
  if (selectedBookingForAction != null) {
    val b = selectedBookingForAction!!
    val space = uiState.spaces.find { it.id == b.spaceId }
    HostBookingActionDialog(
      booking = b,
      space = space,
      onDismiss = { selectedBookingForAction = null },
      onCheckIn = viewModel::hostCheckIn,
      onCheckOut = viewModel::hostCheckOut,
      onCancelBooking = viewModel::hostCancelBooking,
      onMessageCustomer = { onOpenBookingChat(b.id) },
      onReportDispute = { onOpenBookingDispute(b.id) },
    )
  }

  // Space Picker for Bays / Availability Quick Actions when Host owns 2+ spaces
  val currentPickerMode = spacePickerMode
  if (currentPickerMode != null && uiState.spaces.isNotEmpty()) {
    HostSelectSpaceForActionDialog(
      title =
        if (currentPickerMode == HostSpacePickerMode.BAYS) "Select Space for Bays"
        else "Select Space for Availability",
      subtitle = "Choose one of your parking spaces to configure directly.",
      spaces = uiState.spaces,
      onSelectSpace = { chosen ->
        spacePickerMode = null
        if (currentPickerMode == HostSpacePickerMode.BAYS) {
          viewModel.openBaysManager(chosen)
        } else {
          viewModel.openScheduleManager(chosen)
        }
      },
      onDismiss = { spacePickerMode = null },
    )
  }

  if (showHostSettingsDialog) {
    val profile = uiState.hostProfile
    HostPreferencesDialog(
      initialBusinessAlias = profile?.displayBusinessAlias ?: uiState.activeUser?.fullName.orEmpty(),
      initialOperatingCities =
        profile?.operatingCities?.takeIf { it.isNotEmpty() }
          ?: setOfNotNull(uiState.activeUser?.city),
      isDarkTheme = isDarkTheme,
      onToggleTheme = onToggleTheme,
      onSaveOnboarding = viewModel::updateHostOnboardingProfile,
      onOpenProfile = onOpenSettings,
      onDismiss = { showHostSettingsDialog = false },
    )
  }

  if (showHelpDialog) {
    HostHelpSupportDialog(
      onOpenDisputes = onOpenDisputes,
      onDismiss = { showHelpDialog = false },
    )
  }

  if (showPrivacyDialog) {
    HostPrivacyTermsDialog(
      onDismiss = { showPrivacyDialog = false },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostOverviewTabContent(
  uiState: HostDashboardUiState,
  analytics: HostDashboardAnalytics,
  verificationStatus: HostVerificationStatus,
  isDarkTheme: Boolean,
  onSelectTab: (HostDashboardTab) -> Unit,
  onStartCreateListing: () -> Unit,
  onManageBaysQuickAction: () -> Unit,
  onManageAvailabilityQuickAction: () -> Unit,
  onSelectBooking: (Booking) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenMessages: () -> Unit,
  onOpenReviews: () -> Unit,
  onOpenDisputes: () -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600
  val emeraldAccent = if (isDarkTheme) Jade400 else Emerald900
  val hostContentOnAccent = Color.White

  val statusTone =
    when (verificationStatus) {
      HostVerificationStatus.PENDING_REVIEW -> StatusTone.PENDING
      HostVerificationStatus.APPROVED -> StatusTone.AVAILABLE
      HostVerificationStatus.REJECTED -> StatusTone.ERROR
    }

  val statusGuidance =
    when (verificationStatus) {
      HostVerificationStatus.PENDING_REVIEW ->
        "Publishing locked while PENDING_REVIEW. You can create draft listings, configure bays, and prepare availability schedules."
      HostVerificationStatus.APPROVED ->
        "Verified Host account. Approved listings go live immediately in Customer Discovery across your cities."
      HostVerificationStatus.REJECTED ->
        "Publishing blocked (REJECTED). Review the Admin note below and update your Host onboarding details or drafts."
    }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
  ) {
    // 1. Hero Verification Status & Workspace Identity Card
    ParkSpaceGlassSurface(
        modifier = Modifier.testTag("host_verification_card"),
        accentGlow = semantic.hostGlow,
        borderColor = hostAccent.copy(alpha = 0.42f),
        contentPaddingDp = 18,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = CircleShape,
            color = semantic.glassSurface,
            border = BorderStroke(1.dp, semantic.glassBorder),
            modifier = Modifier.testTag("host_mode_banner"),
          ) {
            Text(
              text = "Local Demo",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          ParkSpaceStatusPill(
            label = "KYC: ${verificationStatus.displayName}",
            tone = statusTone,
            modifier = Modifier.testTag("host_status_badge"),
          )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = uiState.hostProfile?.displayBusinessAlias ?: uiState.activeUser?.fullName ?: "Host Console",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            "Owner: ${uiState.activeUser?.fullName.orEmpty()} • Cities: " +
              (uiState.hostProfile?.operatingCities?.joinToString { it.displayName }
                ?: uiState.activeUser?.city?.displayName.orEmpty()),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
          shape = RoundedCornerShape(14.dp),
          color =
            if (verificationStatus.canPublishListings) semantic.availableContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border =
            BorderStroke(
              1.dp,
              if (verificationStatus == HostVerificationStatus.REJECTED) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
              } else {
                MaterialTheme.colorScheme.outlineVariant
              },
            ),
          modifier = Modifier.fillMaxWidth().testTag("host_verification_guidance_box"),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = statusGuidance,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            val verificationNote = uiState.hostProfile?.verificationNote
            if (!verificationNote.isNullOrBlank()) {
              Text(
                text = "Admin review note: $verificationNote",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color =
                  if (verificationStatus == HostVerificationStatus.REJECTED) {
                    MaterialTheme.colorScheme.error
                  } else {
                    emeraldAccent
                  },
                modifier = Modifier.testTag("host_verification_note_text"),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Primary CTA + Utility Chips
        ParkSpaceLiquidGlassButton(
          text = "Add Parking Space",
          onClick = onStartCreateListing,
          leadingIcon = Icons.Outlined.Add,
          accentColor = hostAccent,
          contentColor = hostContentOnAccent,
          modifier = Modifier.fillMaxWidth().testTag("host_overview_add_listing_button"),
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkSpaceLiquidGlassChip(
            label =
              if (uiState.unreadNotificationsCount > 0) "Alerts (${uiState.unreadNotificationsCount})"
              else "Alerts",
            leadingIcon = Icons.Outlined.Notifications,
            accentColor = hostAccent,
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("host_dashboard_notifications_button"),
          )
          ParkSpaceLiquidGlassChip(
            label =
              if (uiState.unreadChatsCount > 0) "Messages (${uiState.unreadChatsCount})"
              else "Messages",
            leadingIcon = Icons.Outlined.ChatBubbleOutline,
            accentColor = emeraldAccent,
            onClick = onOpenMessages,
            modifier = Modifier.testTag("host_overview_messages_button"),
          )
          ParkSpaceLiquidGlassChip(
            label = "Reviews",
            leadingIcon = Icons.Outlined.RateReview,
            accentColor = hostAccent,
            onClick = onOpenReviews,
            modifier = Modifier.testTag("host_overview_reviews_button"),
          )
          ParkSpaceLiquidGlassChip(
            label =
              if (uiState.openDisputesCount > 0) "Disputes (${uiState.openDisputesCount})"
              else "Disputes",
            leadingIcon = Icons.Outlined.ReportProblem,
            accentColor = hostAccent,
            onClick = onOpenDisputes,
            modifier = Modifier.testTag("host_overview_disputes_button"),
          )
        }
      }

    // 2. Compact 2-Column Summary KPI Cards (Strictly Real Room DB Data)
    Column(
        modifier = Modifier.fillMaxWidth().testTag("host_overview_kpis_card"),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          HostKpiGlassCard(
            title = "Listings",
            value = "${analytics.summary.totalListingsCount}",
            subtitle = "${analytics.summary.activeListingsCount} live in Discovery",
            icon = Icons.Outlined.Apartment,
            accentColor = hostAccent,
            glowColor = semantic.hostGlow,
            tag = "host_kpi_total_spaces",
            modifier = Modifier.weight(1f),
          )
          HostKpiGlassCard(
            title = "Parking Bays",
            value = "${analytics.summary.totalBaysCount}",
            subtitle = "${analytics.summary.operationalBaysCount} operational",
            icon = Icons.Outlined.LocalParking,
            accentColor = emeraldAccent,
            glowColor = semantic.customerGlow,
            tag = "host_kpi_total_bays",
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          HostKpiGlassCard(
            title = "Upcoming",
            value = "${analytics.summary.upcomingBookingsCount}",
            subtitle = "Pending or confirmed",
            icon = Icons.Outlined.Schedule,
            accentColor = hostAccent,
            glowColor = semantic.hostGlow,
            tag = "host_kpi_upcoming_bookings",
            modifier = Modifier.weight(1f),
          )
          HostKpiGlassCard(
            title = "Active Now",
            value = "${analytics.summary.activeBookingsCount}",
            subtitle = "Checked-in bays",
            icon = Icons.Outlined.EventAvailable,
            accentColor = if (isDarkTheme) Jade400 else Emerald700,
            glowColor = semantic.customerGlow,
            tag = "host_kpi_active_bookings",
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          HostKpiGlassCard(
            title = "Completed",
            value = "${analytics.summary.completedBookingsCount}",
            subtitle = "Checked-out / done",
            icon = Icons.Outlined.CheckCircleOutline,
            accentColor = if (isDarkTheme) InfoCobaltDark else InfoCobalt,
            glowColor = semantic.customerGlow,
            tag = "host_kpi_completed_bookings",
            modifier = Modifier.weight(1f),
          )
          HostKpiGlassCard(
            title = "Demo Earnings",
            value =
              if (analytics.summary.hasEarningsRecords && analytics.summary.netEarningsPkr != null) {
                "${AppEnvironment.CURRENCY_SYMBOL} ${analytics.summary.netEarningsPkr}"
              } else {
                "—"
              },
            subtitle =
              if (analytics.summary.hasEarningsRecords) {
                "Settled ${analytics.summary.settledEarningsPkr ?: 0} • Escrow ${analytics.summary.escrowHeldPkr ?: 0}"
              } else {
                "No Demo earnings yet"
              },
            icon = Icons.Outlined.Payments,
            accentColor = hostAccent,
            glowColor = semantic.hostGlow,
            tag = "host_kpi_settled_rev",
            modifier = Modifier.weight(1f),
          )
        }
      }

    // 3. Host Quick Actions Grid (Every control wired to real workflows)
    ParkSpaceGlassSurface(
        modifier = Modifier.testTag("host_quick_actions_card"),
        accentGlow = semantic.hostGlow,
        contentPaddingDp = 18,
      ) {
        Text(
          text = "Host Quick Actions",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkSpaceLiquidGlassSecondaryButton(
            text = "Manage Listings",
            leadingIcon = Icons.Outlined.Apartment,
            accentColor = hostAccent,
            onClick = { onSelectTab(HostDashboardTab.LISTINGS) },
            modifier = Modifier.weight(1f).testTag("host_overview_manage_listings_button"),
          )
          ParkSpaceLiquidGlassSecondaryButton(
            text = "Manage Bays",
            leadingIcon = Icons.Outlined.LocalParking,
            accentColor = emeraldAccent,
            onClick = onManageBaysQuickAction,
            modifier = Modifier.weight(1f).testTag("host_overview_manage_bays_button"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkSpaceLiquidGlassSecondaryButton(
            text = "Availability",
            leadingIcon = Icons.Outlined.CalendarMonth,
            accentColor = emeraldAccent,
            onClick = onManageAvailabilityQuickAction,
            modifier = Modifier.weight(1f).testTag("host_overview_manage_availability_button"),
          )
          ParkSpaceLiquidGlassSecondaryButton(
            text = "View Bookings",
            leadingIcon = Icons.AutoMirrored.Outlined.EventNote,
            accentColor = hostAccent,
            onClick = { onSelectTab(HostDashboardTab.BOOKINGS) },
            modifier = Modifier.weight(1f).testTag("host_overview_open_bookings_button"),
          )
        }

        ParkSpaceLiquidGlassSecondaryButton(
          text = "View Demo Ledger",
          leadingIcon = Icons.Outlined.AccountBalanceWallet,
          accentColor = hostAccent,
          onClick = { onSelectTab(HostDashboardTab.LEDGER) },
          modifier = Modifier.fillMaxWidth().testTag("host_overview_open_ledger_button"),
        )
      }

    // 4. Native Compose Canvas Analytics (Real Room DB Data Only)
    HostActivityLineChartCard(
      points = analytics.activityTrend,
      isDarkTheme = isDarkTheme,
    )

    HostStatusDonutChartCard(
      slices = analytics.statusDistribution,
      isDarkTheme = isDarkTheme,
    )

    if (analytics.earningsTrend.isNotEmpty()) {
      HostEarningsTrendCard(
        points = analytics.earningsTrend,
        netEarningsPkr = analytics.summary.netEarningsPkr ?: 0,
        isDarkTheme = isDarkTheme,
      )
    }

    // 5. Recent Host Activity
    ParkSpaceGlassSurface(
        modifier = Modifier.testTag("host_recent_activity_card"),
        accentGlow = semantic.hostGlow,
        contentPaddingDp = 18,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Recent Host Activity",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          )
          TextButton(
            onClick = { onSelectTab(HostDashboardTab.BOOKINGS) },
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("All (${uiState.bookings.size})")
          }
        }

        if (analytics.recentActivity.isEmpty()) {
          Text(
            text = "No incoming bookings or bay activity yet. Once customers reserve your spaces, activity appears here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("host_recent_activity_empty"),
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
            analytics.recentActivity.forEach { item ->
              val b = item.booking
              val tone = bookingToneFor(b.status)
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectBooking(b) }
                    .testTag("host_recent_booking_row_${b.bookingCode}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    Text(
                      text = item.spaceTitle,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${b.bookingCode} • ${formatDateTimePkt(b.startEpochMs)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    ParkSpaceStatusPill(label = b.status.displayName, tone = tone)
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Text(
                        text = "${AppEnvironment.CURRENCY_SYMBOL} ${b.baseAmountPkr}",
                        style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = hostAccent,
                      )
                      Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Manage booking",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

    Spacer(modifier = Modifier.height(ParkSpaceSpacing.md))
  }
}

@Composable
private fun HostListingsTabContent(
  spaces: List<ParkingSpace>,
  verificationStatus: HostVerificationStatus,
  isDarkTheme: Boolean,
  onStartCreateListing: () -> Unit,
  onEditSpace: (ParkingSpace) -> Unit,
  onManageBays: (ParkingSpace) -> Unit,
  onManageSchedule: (ParkingSpace) -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("host_listings_tab_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      ParkSpaceGlassSurface(
        accentGlow = semantic.hostGlow,
        contentPaddingDp = 16,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${spaces.size} Owned Listing(s)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text =
                if (verificationStatus.canPublishListings) "Approved Host: live publishing enabled"
                else "Drafts allowed • Live publishing locked (${verificationStatus.displayName})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          ParkSpaceLiquidGlassButton(
            text = "New Listing",
            leadingIcon = Icons.Outlined.Add,
            accentColor = hostAccent,
            contentColor = Color.White,
            onClick = onStartCreateListing,
            modifier = Modifier.testTag("host_create_listing_button"),
          )
        }
      }
    }

    if (spaces.isEmpty()) {
      item {
        EmptyStateNotice(
          message = "You haven't created any parking listings yet.",
          actionText = "Create Listing",
          onAction = onStartCreateListing,
        )
      }
    } else {
      items(spaces, key = { it.id }) { space ->
        HostSpaceListingCard(
          space = space,
          isDarkTheme = isDarkTheme,
          onEdit = { onEditSpace(space) },
          onManageBays = { onManageBays(space) },
          onManageSchedule = { onManageSchedule(space) },
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.md))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostBookingsTabContent(
  bookings: List<Booking>,
  spaces: List<ParkingSpace>,
  selectedFilter: String,
  isDarkTheme: Boolean,
  onSelectFilter: (String) -> Unit,
  onQuickCheckIn: (String) -> Unit,
  onQuickCheckOut: (String) -> Unit,
  onSelectBooking: (Booking) -> Unit,
) {
  val hostAccent = if (isDarkTheme) Brass400 else Brass600
  val spaceMap = remember(spaces) { spaces.associateBy { it.id } }

  val filteredBookings =
    remember(bookings, selectedFilter) {
      when (selectedFilter) {
        "ACTIVE" -> bookings.filter { it.status in setOf(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN) }
        "COMPLETED" -> bookings.filter { it.status in setOf(BookingStatus.CHECKED_OUT, BookingStatus.COMPLETED) }
        "CANCELLED" -> bookings.filter { it.status in setOf(BookingStatus.CANCELLED, BookingStatus.EXPIRED) }
        else -> bookings
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("host_bookings_tab_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        listOf(
          "ALL" to "All (${bookings.size})",
          "ACTIVE" to "Active / Upcoming",
          "COMPLETED" to "Completed",
          "CANCELLED" to "Cancelled",
        ).forEach { (key, label) ->
          ParkSpaceLiquidGlassChip(
            label = label,
            selected = selectedFilter == key,
            accentColor = hostAccent,
            onClick = { onSelectFilter(key) },
            modifier = Modifier.testTag("host_booking_filter_$key"),
          )
        }
      }
    }

    if (filteredBookings.isEmpty()) {
      item {
        EmptyStateNotice(
          message = "No bookings found matching filter '$selectedFilter'.",
        )
      }
    } else {
      items(filteredBookings, key = { it.id }) { booking ->
        val space = spaceMap[booking.spaceId]
        HostBookingRowCard(
          booking = booking,
          spaceTitle = space?.title ?: "Listing #${booking.spaceId}",
          isDarkTheme = isDarkTheme,
          onSelect = { onSelectBooking(booking) },
          onQuickCheckIn = if (booking.status == BookingStatus.CONFIRMED) { { onQuickCheckIn(booking.id) } } else null,
          onQuickCheckOut = if (booking.status == BookingStatus.CHECKED_IN) { { onQuickCheckOut(booking.id) } } else null,
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.md))
    }
  }
}

@Composable
private fun HostLedgerTabContent(
  ledger: HostDemoLedgerSummary?,
  isDarkTheme: Boolean,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600
  val emeraldAccent = if (isDarkTheme) Jade400 else Emerald900
  val hasRecords = (ledger?.totalBookingsCount ?: 0) > 0

  fun fmtOrDash(amount: Int?): String =
    if (hasRecords && amount != null) "${AppEnvironment.CURRENCY_SYMBOL} $amount" else "—"

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("host_ledger_tab_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      ParkSpaceGlassSurface(
        modifier = Modifier.testTag("host_ledger_transparency_card"),
        accentGlow = semantic.hostGlow,
        borderColor = hostAccent.copy(alpha = 0.4f),
        contentPaddingDp = 16,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Icon(Icons.Outlined.Info, contentDescription = null, tint = hostAccent)
          Column {
            Text(
              text = HostLedgerCalculator.DEMO_LEDGER_DISCLAIMER,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = hostAccent,
            )
            Text(
              text =
                "All earnings and Demo platform fees are calculated strictly from local Demo booking records using the Admin commission rule. No banking rails are connected.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item {
      ParkSpaceGlassSurface(
        modifier = Modifier.testTag("host_ledger_kpis_card"),
        accentGlow = semantic.customerGlow,
        contentPaddingDp = 18,
      ) {
        Text(
          text = "Earnings & Platform Fee Breakdown",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          LedgerKpiItem(
            label = "Gross Volume",
            value = fmtOrDash(ledger?.totalLifetimeGrossPkr),
            tag = "ledger_gross_volume",
            accent = emeraldAccent,
            modifier = Modifier.weight(1f),
          )
          LedgerKpiItem(
            label = "Platform Fee",
            value = fmtOrDash(ledger?.totalPlatformCommissionPkr),
            tag = "ledger_platform_fee",
            accent = hostAccent,
            modifier = Modifier.weight(1f),
          )
          LedgerKpiItem(
            label = "Host Net",
            value = fmtOrDash(ledger?.totalHostNetEarningsPkr),
            tag = "ledger_host_net",
            accent = emeraldAccent,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          LedgerKpiItem(
            label = "Escrow Held",
            value = fmtOrDash(ledger?.escrowHeldPkr),
            tag = "ledger_escrow_held",
            accent = hostAccent,
            modifier = Modifier.weight(1f),
          )
          LedgerKpiItem(
            label = "Settled",
            value = fmtOrDash(ledger?.settledPkr),
            tag = "ledger_settled_rev",
            accent = emeraldAccent,
            modifier = Modifier.weight(1f),
          )
          LedgerKpiItem(
            label = "Refunded",
            value = fmtOrDash(ledger?.totalRefundedPkr),
            tag = "ledger_refunded",
            accent = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    item {
      Text(
        text = "Demo Transaction History (${ledger?.transactions?.size ?: 0})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      )
    }

    val transactions = ledger?.transactions.orEmpty()
    if (transactions.isEmpty()) {
      item {
        EmptyStateNotice(
          message = "No Demo transactions recorded yet.",
        )
      }
    } else {
      items(transactions, key = { it.bookingId }) { tx ->
        ParkSpaceGlassSurface(
          modifier = Modifier.testTag("host_ledger_tx_${tx.bookingCode}"),
          accentGlow = semantic.hostGlow,
          cornerRadiusDp = 16,
          contentPaddingDp = 14,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = tx.bookingCode, style = ParkSpaceMonoCodeStyle, color = hostAccent)
            ParkSpaceStatusPill(label = tx.settlementBadgeLabel, tone = tx.settlementTone)
          }
          Text(
            text = tx.spaceTitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Paid: Rs. ${tx.totalPaidByCustomerPkr} (Fee: Rs. ${tx.platformFeePkr})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Net: Rs. ${tx.hostNetSharePkr}",
              style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.Bold),
              color = emeraldAccent,
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.md))
    }
  }
}

@Composable
private fun HostSpaceListingCard(
  space: ParkingSpace,
  isDarkTheme: Boolean,
  onEdit: () -> Unit,
  onManageBays: () -> Unit,
  onManageSchedule: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600
  val emeraldAccent = if (isDarkTheme) Jade400 else Emerald900

  val approvalLabel =
    when {
      space.isApproved -> "Live in Discovery"
      space.isRejectedByAdmin -> "Rejected by Admin"
      else -> "Draft / Pending Review"
    }
  val approvalTone =
    when {
      space.isApproved -> StatusTone.AVAILABLE
      space.isRejectedByAdmin -> StatusTone.ERROR
      else -> StatusTone.PENDING
    }

  ParkSpaceGlassSurface(
    modifier = modifier.testTag("host_space_card_${space.id}"),
    accentGlow = semantic.hostGlow,
    cornerRadiusDp = 20,
    contentPaddingDp = 16,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = space.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
          text = "${space.location.areaLabel}, ${space.location.city.displayName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      ParkSpaceStatusPill(
        label = approvalLabel,
        tone = approvalTone,
        modifier = Modifier.testTag("host_space_status_pill_${space.id}"),
      )
    }

    val moderationNote = space.moderationNote
    if (!moderationNote.isNullOrBlank()) {
      Text(
        text = "Admin moderation note: $moderationNote",
        style = MaterialTheme.typography.bodySmall,
        color =
          if (space.isRejectedByAdmin) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.testTag("host_space_moderation_note_${space.id}"),
      )
    }

    if (space.photoUris.isNotEmpty()) {
      ParkingSpaceMediaBanner(
        photoUris = space.photoUris,
        parkingType = space.parkingType,
        isCovered = ParkingAmenity.COVERED in space.amenities,
        contentDescription = "${space.title} photo",
        height = 96.dp,
      )
    }

    Text(
      text =
        "Rates: Rs. ${space.hourlyRatePkr}/hr • Rs. ${space.dailyRatePkr}/day" +
          if (space.monthlyEnabled && space.monthlyRatePkr != null) " • Monthly: Rs. ${space.monthlyRatePkr}" else "",
      style = ParkSpaceMonoCodeStyle,
      color = hostAccent,
    )

    Text(
      text = "Capacity: ${space.totalBays} total bay(s) • ${space.availableBays} operational",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (space.photoTags.isNotEmpty()) {
      Text(
        text = "Photo Tags: ${space.photoTags.joinToString()}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      ParkSpaceLiquidGlassSecondaryButton(
        text = "Edit",
        leadingIcon = Icons.Outlined.Edit,
        accentColor = hostAccent,
        onClick = onEdit,
        modifier = Modifier.weight(1f).testTag("host_edit_listing_button_${space.id}"),
      )
      ParkSpaceLiquidGlassSecondaryButton(
        text = "Bays",
        leadingIcon = Icons.Outlined.LocalParking,
        accentColor = emeraldAccent,
        onClick = onManageBays,
        modifier = Modifier.weight(1f).testTag("host_manage_bays_button_${space.id}"),
      )
      ParkSpaceLiquidGlassSecondaryButton(
        text = "Schedule",
        leadingIcon = Icons.Outlined.Schedule,
        accentColor = emeraldAccent,
        onClick = onManageSchedule,
        modifier = Modifier.weight(1f).testTag("host_manage_schedule_button_${space.id}"),
      )
    }
  }
}

@Composable
private fun HostBookingRowCard(
  booking: Booking,
  spaceTitle: String,
  isDarkTheme: Boolean,
  onSelect: () -> Unit,
  onQuickCheckIn: (() -> Unit)? = null,
  onQuickCheckOut: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600
  val emeraldAccent = if (isDarkTheme) Jade400 else Emerald900
  val tone = bookingToneFor(booking.status)

  ParkSpaceGlassSurface(
    modifier = modifier.testTag("host_booking_card_${booking.bookingCode}"),
    accentGlow = semantic.hostGlow,
    cornerRadiusDp = 18,
    contentPaddingDp = 16,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = booking.bookingCode, style = ParkSpaceMonoCodeStyle, color = hostAccent)
      ParkSpaceStatusPill(label = booking.status.displayName, tone = tone)
    }

    Text(
      text = spaceTitle,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Customer Paid: Rs. ${booking.totalAmountPkr}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "Host Net: Rs. ${booking.baseAmountPkr}",
        style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.Bold),
        color = emeraldAccent,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      if (onQuickCheckIn != null) {
        ParkSpaceLiquidGlassButton(
          text = "Check In",
          onClick = onQuickCheckIn,
          accentColor = emeraldAccent,
          contentColor = Color.White,
          modifier = Modifier.weight(1f).testTag("host_quick_checkin_${booking.id}"),
        )
      }
      if (onQuickCheckOut != null) {
        ParkSpaceLiquidGlassButton(
          text = "Check Out",
          onClick = onQuickCheckOut,
          accentColor = hostAccent,
          contentColor = Color.White,
          modifier = Modifier.weight(1f).testTag("host_quick_checkout_${booking.id}"),
        )
      }
      ParkSpaceLiquidGlassSecondaryButton(
        text = "Details",
        accentColor = hostAccent,
        onClick = onSelect,
        modifier = Modifier.weight(1f).testTag("host_view_booking_details_${booking.id}"),
      )
    }
  }
}

@Composable
private fun HostKpiGlassCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  glowColor: Color,
  tag: String,
  modifier: Modifier = Modifier,
) {
  ParkSpaceGlassSurface(
    modifier = modifier.testTag(tag),
    accentGlow = glowColor,
    cornerRadiusDp = 20,
    contentPaddingDp = 14,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(18.dp),
      )
    }
    Text(
      text = value,
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      color = accentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun LedgerKpiItem(
  label: String,
  value: String,
  tag: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
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

@Composable
private fun EmptyStateNotice(
  message: String,
  actionText: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  ParkSpaceGlassSurface(
    modifier = modifier,
    accentGlow = semantic.hostGlow,
    cornerRadiusDp = 18,
    contentPaddingDp = 18,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionText != null && onAction != null) {
        ParkSpaceLiquidGlassSecondaryButton(
          text = actionText,
          onClick = onAction,
        )
      }
    }
  }
}

private fun bookingToneFor(status: BookingStatus): StatusTone =
  when (status) {
    BookingStatus.CONFIRMED,
    BookingStatus.CHECKED_OUT,
    BookingStatus.COMPLETED -> StatusTone.AVAILABLE
    BookingStatus.CHECKED_IN,
    BookingStatus.PENDING_PAYMENT -> StatusTone.PENDING
    BookingStatus.CANCELLED,
    BookingStatus.EXPIRED,
    BookingStatus.DISPUTED -> StatusTone.ERROR
    BookingStatus.DRAFT -> StatusTone.INFO
  }
