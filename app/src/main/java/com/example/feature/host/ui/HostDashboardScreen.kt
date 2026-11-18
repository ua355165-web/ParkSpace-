package com.example.feature.host.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingSpace
import com.example.feature.auth.domain.DemoAuthValidator
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.discovery.ui.ParkingSpaceMediaBanner
import com.example.feature.host.HostDashboardTab
import com.example.feature.host.HostDashboardUiState
import com.example.feature.host.HostDashboardViewModel
import com.example.feature.host.domain.HostLedgerCalculator
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import java.time.Instant
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
) {
  var selectedBookingForAction by remember { mutableStateOf<Booking?>(null) }
  var bookingFilter by rememberSaveable { mutableStateOf("ALL") }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("host_dashboard_screen"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = uiState.hostProfile?.displayBusinessAlias ?: "Host Console",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Owner: ${uiState.activeUser?.fullName.orEmpty()} • ${uiState.activeUser?.city?.displayName.orEmpty()}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("host_dashboard_back_button")) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("host_dashboard_notifications_button"),
          ) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications (${uiState.unreadNotificationsCount} unread)")
          }
          IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("host_dashboard_settings_button")) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
      )
    },
    floatingActionButton = {
      if (uiState.activeTab == HostDashboardTab.LISTINGS || uiState.activeTab == HostDashboardTab.OVERVIEW) {
        FloatingActionButton(
          onClick = viewModel::startCreateListing,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("host_fab_create_listing"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = "Create New Listing")
        }
      }
    },
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = ParkSpaceSpacing.md),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      // Feedback banners
      if (uiState.actionFeedback != null) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth().testTag("host_feedback_banner"),
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
              Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(
                text = uiState.actionFeedback.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            IconButton(
              onClick = viewModel::dismissFeedback,
              modifier = Modifier.size(24.dp).testTag("host_dismiss_feedback_button"),
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      if (uiState.errorMessage != null) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().testTag("host_error_banner"),
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
              Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Text(
                text = uiState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
            IconButton(
              onClick = viewModel::dismissFeedback,
              modifier = Modifier.size(24.dp).testTag("host_dismiss_error_button"),
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Verification Status Banner
      val verificationStatus = uiState.hostProfile?.verificationStatus ?: HostVerificationStatus.PENDING_REVIEW
      val statusTone = when (verificationStatus) {
        HostVerificationStatus.PENDING_REVIEW -> StatusTone.PENDING
        HostVerificationStatus.APPROVED -> StatusTone.AVAILABLE
        HostVerificationStatus.REJECTED -> StatusTone.ERROR
      }

      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("host_verification_card"),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = ParkSpaceSpacing.sm, vertical = ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              ParkSpaceStatusPill(
                label = "Status: ${verificationStatus.displayName}",
                tone = statusTone,
                modifier = Modifier.testTag("host_status_badge"),
              )
              if (!verificationStatus.canPublishListings) {
                Text(
                  text = "• Publishing Locked",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.error,
                )
              } else {
                Text(
                  text = "• Publishing Enabled",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            ParkSpaceStatusPill(
              label = "Demo Host Mode",
              tone = StatusTone.INFO,
            )
          }
          val verificationNote = uiState.hostProfile?.verificationNote
          if (!verificationNote.isNullOrBlank()) {
            Text(
              text = "Admin review note: $verificationNote",
              style = MaterialTheme.typography.bodySmall,
              color =
                if (verificationStatus == HostVerificationStatus.REJECTED) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              modifier = Modifier.testTag("host_verification_note_text"),
            )
          }
        }
      }

      // Tabs: Overview, My Listings, Bookings, Demo Ledger
      PrimaryTabRow(
        selectedTabIndex = uiState.activeTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
      ) {
        HostDashboardTab.entries.forEach { tab ->
          Tab(
            selected = uiState.activeTab == tab,
            onClick = { viewModel.selectTab(tab) },
            text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("host_tab_${tab.name.lowercase()}"),
          )
        }
      }

      // Tab Content
      when (uiState.activeTab) {
        HostDashboardTab.OVERVIEW -> {
          HostOverviewTabContent(
            uiState = uiState,
            onSelectTab = viewModel::selectTab,
            onStartCreateListing = viewModel::startCreateListing,
            onEditSpace = viewModel::startEditListing,
            onManageBays = viewModel::openBaysManager,
            onManageSchedule = viewModel::openScheduleManager,
            onSelectBooking = { selectedBookingForAction = it },
            onOpenMessages = onOpenMessages,
            onOpenReviews = onOpenReviews,
            onOpenDisputes = onOpenDisputes,
          )
        }
        HostDashboardTab.LISTINGS -> {
          HostListingsTabContent(
            spaces = uiState.spaces,
            canPublish = verificationStatus.canPublishListings,
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
            onSelectFilter = { bookingFilter = it },
            onQuickCheckIn = viewModel::hostCheckIn,
            onQuickCheckOut = viewModel::hostCheckOut,
            onSelectBooking = { selectedBookingForAction = it },
          )
        }
        HostDashboardTab.LEDGER -> {
          HostLedgerTabContent(
            ledger = uiState.ledger,
          )
        }
      }
    }
  }

  // Listing Editor Dialog
  if (uiState.isCreatingNewListing || uiState.editingListing != null) {
    val hostId = uiState.activeUser?.id.orEmpty()
    val verification = uiState.hostProfile?.verificationStatus ?: HostVerificationStatus.PENDING_REVIEW
    HostListingEditorDialog(
      hostId = hostId,
      hostVerificationStatus = verification,
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
}

@Composable
private fun HostOverviewTabContent(
  uiState: HostDashboardUiState,
  onSelectTab: (HostDashboardTab) -> Unit,
  onStartCreateListing: () -> Unit,
  onEditSpace: (ParkingSpace) -> Unit,
  onManageBays: (ParkingSpace) -> Unit,
  onManageSchedule: (ParkingSpace) -> Unit,
  onSelectBooking: (Booking) -> Unit,
  onOpenMessages: () -> Unit,
  onOpenReviews: () -> Unit,
  onOpenDisputes: () -> Unit,
) {
  val totalBays = uiState.spaces.sumOf { it.totalBays }
  val activeBookings = uiState.bookings.count { it.status in setOf(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN) }
  val settledPkr = uiState.ledger?.settledPkr ?: 0

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      // Summary KPIs
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("host_overview_kpis_card"),
      ) {
        Column(
          modifier = Modifier.padding(ParkSpaceSpacing.md),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          Text(text = "Workspace Overview", style = MaterialTheme.typography.titleMedium)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            KpiItem(
              label = "Listings",
              value = "${uiState.spaces.size}",
              tag = "host_kpi_total_spaces",
            )
            KpiItem(
              label = "Total Bays",
              value = "$totalBays",
              tag = "host_kpi_total_bays",
            )
            KpiItem(
              label = "Active Bookings",
              value = "$activeBookings",
              tag = "host_kpi_active_bookings",
            )
            KpiItem(
              label = "Settled Rev.",
              value = "Rs. $settledPkr",
              tag = "host_kpi_settled_rev",
            )
          }
        }
      }
    }

    item {
      // Quick Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        OutlinedButton(
          onClick = onStartCreateListing,
          modifier = Modifier.weight(1f).testTag("host_overview_add_listing_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("New Listing", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = { onSelectTab(HostDashboardTab.BOOKINGS) },
          modifier = Modifier.weight(1f).testTag("host_overview_open_bookings_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("Bookings", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = { onSelectTab(HostDashboardTab.LEDGER) },
          modifier = Modifier.weight(1f).testTag("host_overview_open_ledger_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("Ledger", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        OutlinedButton(
          onClick = onOpenMessages,
          modifier = Modifier.weight(1f).testTag("host_overview_messages_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text(
            if (uiState.unreadChatsCount > 0) "Chat (${uiState.unreadChatsCount})" else "Messages",
            style = MaterialTheme.typography.labelMedium,
          )
        }
        OutlinedButton(
          onClick = onOpenReviews,
          modifier = Modifier.weight(1f).testTag("host_overview_reviews_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("Reviews", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
          onClick = onOpenDisputes,
          modifier = Modifier.weight(1f).testTag("host_overview_disputes_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text(
            if (uiState.openDisputesCount > 0) "Disputes (${uiState.openDisputesCount})" else "Disputes",
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "My Parking Properties", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = { onSelectTab(HostDashboardTab.LISTINGS) },
          shape = MaterialTheme.shapes.extraSmall,
        ) {
          Text("View All (${uiState.spaces.size})")
        }
      }
    }

    if (uiState.spaces.isEmpty()) {
      item {
        EmptyStateNotice(
          message = "You haven't created any parking listings yet.",
          actionText = "Create Listing",
          onAction = onStartCreateListing,
        )
      }
    } else {
      items(uiState.spaces.take(3), key = { it.id }) { space ->
        HostSpaceListingCard(
          space = space,
          onEdit = { onEditSpace(space) },
          onManageBays = { onManageBays(space) },
          onManageSchedule = { onManageSchedule(space) },
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "Recent Incoming Bookings", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = { onSelectTab(HostDashboardTab.BOOKINGS) },
          shape = MaterialTheme.shapes.extraSmall,
        ) {
          Text("Console (${uiState.bookings.size})")
        }
      }
    }

    if (uiState.bookings.isEmpty()) {
      item {
        EmptyStateNotice(
          message = "No bookings received yet. Confirmed customer bookings will appear here.",
        )
      }
    } else {
      items(uiState.bookings.take(3), key = { it.id }) { booking ->
        val space = uiState.spaces.find { it.id == booking.spaceId }
        HostBookingRowCard(
          booking = booking,
          spaceTitle = space?.title ?: "Listing #${booking.spaceId}",
          onSelect = { onSelectBooking(booking) },
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
    }
  }
}

@Composable
private fun HostListingsTabContent(
  spaces: List<ParkingSpace>,
  canPublish: Boolean,
  onStartCreateListing: () -> Unit,
  onEditSpace: (ParkingSpace) -> Unit,
  onManageBays: (ParkingSpace) -> Unit,
  onManageSchedule: (ParkingSpace) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("host_listings_tab_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${spaces.size} Owned Listing(s)",
          style = MaterialTheme.typography.titleMedium,
        )
        ParkSpacePrimaryButton(
          text = "+ New Listing",
          onClick = onStartCreateListing,
          modifier = Modifier.testTag("host_create_listing_button"),
        )
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
          onEdit = { onEditSpace(space) },
          onManageBays = { onManageBays(space) },
          onManageSchedule = { onManageSchedule(space) },
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
    }
  }
}

@Composable
private fun HostBookingsTabContent(
  bookings: List<Booking>,
  spaces: List<ParkingSpace>,
  selectedFilter: String,
  onSelectFilter: (String) -> Unit,
  onQuickCheckIn: (String) -> Unit,
  onQuickCheckOut: (String) -> Unit,
  onSelectBooking: (Booking) -> Unit,
) {
  val spaceMap = remember(spaces) { spaces.associateBy { it.id } }

  val filteredBookings = remember(bookings, selectedFilter) {
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
      // Filter Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        listOf("ALL" to "All (${bookings.size})", "ACTIVE" to "Active / Upcoming", "COMPLETED" to "Completed", "CANCELLED" to "Cancelled")
          .forEach { (key, label) ->
            FilterChip(
              selected = selectedFilter == key,
              onClick = { onSelectFilter(key) },
              label = { Text(label, style = MaterialTheme.typography.labelSmall) },
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
          onSelect = { onSelectBooking(booking) },
          onQuickCheckIn = if (booking.status == BookingStatus.CONFIRMED) { { onQuickCheckIn(booking.id) } } else null,
          onQuickCheckOut = if (booking.status == BookingStatus.CHECKED_IN) { { onQuickCheckOut(booking.id) } } else null,
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
    }
  }
}

@Composable
private fun HostLedgerTabContent(
  ledger: com.example.feature.host.domain.HostDemoLedgerSummary?,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("host_ledger_tab_list"),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
  ) {
    item {
      // Transparency Notice Card
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("host_ledger_transparency_card"),
      ) {
        Row(
          modifier = Modifier.padding(ParkSpaceSpacing.sm),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Column {
            Text(
              text = HostLedgerCalculator.DEMO_LEDGER_DISCLAIMER,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "All revenues and Demo platform fees are calculated strictly from local Demo booking records using the active Admin Demo commission rule. No actual payments or banking rails are connected.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item {
      // Ledger Financial KPI Card
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("host_ledger_kpis_card"),
      ) {
        Column(
          modifier = Modifier.padding(ParkSpaceSpacing.md),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          Text(text = "Earnings & Platform Fee Breakdown", style = MaterialTheme.typography.titleMedium)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            KpiItem(
              label = "Gross Volume",
              value = "Rs. ${ledger?.totalLifetimeGrossPkr ?: 0}",
              tag = "ledger_gross_volume",
            )
            KpiItem(
              label = "Demo Platform Fee",
              value = "Rs. ${ledger?.totalPlatformCommissionPkr ?: 0}",
              tag = "ledger_platform_fee",
            )
            KpiItem(
              label = "Host Net Revenue",
              value = "Rs. ${ledger?.totalHostNetEarningsPkr ?: 0}",
              tag = "ledger_host_net",
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            KpiItem(
              label = "Held in Escrow",
              value = "Rs. ${ledger?.escrowHeldPkr ?: 0}",
              tag = "ledger_escrow_held",
            )
            KpiItem(
              label = "Settled in Ledger",
              value = "Rs. ${ledger?.settledPkr ?: 0}",
              tag = "ledger_settled_rev",
            )
            KpiItem(
              label = "Refunded",
              value = "Rs. ${ledger?.totalRefundedPkr ?: 0}",
              tag = "ledger_refunded",
            )
          }
        }
      }
    }

    item {
      Text(
        text = "Demo Transaction History (${ledger?.transactions?.size ?: 0})",
        style = MaterialTheme.typography.titleMedium,
      )
    }

    val transactions = ledger?.transactions.orEmpty()
    if (transactions.isEmpty()) {
      item {
        EmptyStateNotice(
          message = "No transactions yet.",
        )
      }
    } else {
      items(transactions, key = { it.bookingId }) { tx ->
        Surface(
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().testTag("host_ledger_tx_${tx.bookingCode}"),
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
              Text(text = tx.bookingCode, style = ParkSpaceMonoCodeStyle, color = MaterialTheme.colorScheme.primary)
              ParkSpaceStatusPill(label = tx.settlementBadgeLabel, tone = tx.settlementTone)
            }
            Text(
              text = tx.spaceTitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Customer Paid: Rs. ${tx.totalPaidByCustomerPkr} (Fee: Rs. ${tx.platformFeePkr})",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(
                text = "Host Share: Rs. ${tx.hostNetSharePkr}",
                style = ParkSpaceMonoCodeStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
    }
  }
}

@Composable
private fun HostSpaceListingCard(
  space: ParkingSpace,
  onEdit: () -> Unit,
  onManageBays: () -> Unit,
  onManageSchedule: () -> Unit,
  modifier: Modifier = Modifier,
) {
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

  Surface(
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.fillMaxWidth().testTag("host_space_card_${space.id}"),
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
            text = "${space.location.areaLabel}, ${space.location.city.displayName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
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
        text = "Rates: Rs. ${space.hourlyRatePkr}/hr • Rs. ${space.dailyRatePkr}/day" +
          if (space.monthlyEnabled && space.monthlyRatePkr != null) " • Monthly: Rs. ${space.monthlyRatePkr}" else "",
        style = MaterialTheme.typography.bodySmall,
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
        OutlinedButton(
          onClick = onEdit,
          modifier = Modifier.weight(1f).testTag("host_edit_listing_button_${space.id}"),
          shape = MaterialTheme.shapes.extraSmall,
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("Edit", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onManageBays,
          modifier = Modifier.weight(1f).testTag("host_manage_bays_button_${space.id}"),
          shape = MaterialTheme.shapes.extraSmall,
        ) {
          Icon(Icons.Outlined.LocalParking, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("Bays", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onManageSchedule,
          modifier = Modifier.weight(1f).testTag("host_manage_schedule_button_${space.id}"),
          shape = MaterialTheme.shapes.extraSmall,
        ) {
          Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
          Text("Schedule", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
private fun HostBookingRowCard(
  booking: Booking,
  spaceTitle: String,
  onSelect: () -> Unit,
  onQuickCheckIn: (() -> Unit)? = null,
  onQuickCheckOut: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val tone = when (booking.status) {
    BookingStatus.CONFIRMED -> StatusTone.AVAILABLE
    BookingStatus.CHECKED_IN -> StatusTone.PENDING
    BookingStatus.CHECKED_OUT, BookingStatus.COMPLETED -> StatusTone.AVAILABLE
    BookingStatus.CANCELLED -> StatusTone.ERROR
    else -> StatusTone.INFO
  }

  Surface(
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.fillMaxWidth().testTag("host_booking_card_${booking.bookingCode}"),
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
        Text(text = booking.bookingCode, style = ParkSpaceMonoCodeStyle, color = MaterialTheme.colorScheme.primary)
        ParkSpaceStatusPill(label = booking.status.displayName, tone = tone)
      }

      Text(
        text = spaceTitle,
        style = MaterialTheme.typography.bodyMedium,
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
          style = ParkSpaceMonoCodeStyle,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        if (onQuickCheckIn != null) {
          ParkSpacePrimaryButton(
            text = "Check In",
            onClick = onQuickCheckIn,
            modifier = Modifier.weight(1f).testTag("host_quick_checkin_${booking.id}"),
          )
        }
        if (onQuickCheckOut != null) {
          ParkSpacePrimaryButton(
            text = "Check Out",
            onClick = onQuickCheckOut,
            modifier = Modifier.weight(1f).testTag("host_quick_checkout_${booking.id}"),
          )
        }
        OutlinedButton(
          onClick = onSelect,
          modifier = Modifier.weight(1f).testTag("host_view_booking_details_${booking.id}"),
          shape = MaterialTheme.shapes.extraSmall,
        ) {
          Text("Details", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
private fun KpiItem(
  label: String,
  value: String,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
private fun EmptyStateNotice(
  message: String,
  actionText: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.md),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionText != null && onAction != null) {
        OutlinedButton(
          onClick = onAction,
          shape = MaterialTheme.shapes.small,
        ) {
          Text(actionText)
        }
      }
    }
  }
}
