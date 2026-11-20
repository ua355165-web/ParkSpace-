package com.example.feature.booking.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.common.UiState
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceCompactDemoPill
import com.example.core.designsystem.components.ParkSpaceGlassIconButton
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceStateContainer
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DisputeCategory
import com.example.feature.booking.BookingHistoryTab
import com.example.feature.booking.CustomerBookingDisplayItem
import com.example.feature.booking.CustomerBookingsScreenData
import com.example.feature.marketplace.ui.DemoPaymentMethodDialog
import com.example.feature.marketplace.ui.FileBookingDisputeDialog
import com.example.feature.marketplace.ui.SubmitVerifiedReviewDialog
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingsScreen(
  uiState: UiState<CustomerBookingsScreenData>,
  actionFeedback: String?,
  onBack: () -> Unit,
  onSelectTab: (BookingHistoryTab) -> Unit,
  onOpenBookingDetail: (String) -> Unit,
  onConfirmPendingPayment: (String) -> Unit,
  onCheckIn: (String) -> Unit,
  onCheckOut: (String) -> Unit,
  onCancelBooking: (String) -> Unit,
  onOpenDiscovery: () -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
  onConfirmPendingPaymentWithMethod: ((String, DemoPaymentMethod) -> Unit)? = null,
  onOpenChat: ((String) -> Unit)? = null,
  onSubmitReview: ((bookingId: String, rating: Int, comment: String) -> Unit)? = null,
  onFileDispute: ((bookingId: String, category: DisputeCategory, description: String) -> Unit)? = null,
  bottomBar: @Composable () -> Unit = {},
  isDarkTheme: Boolean = false,
) {
  var pendingCancelItem by remember { mutableStateOf<CustomerBookingDisplayItem?>(null) }
  var pendingPayItem by remember { mutableStateOf<CustomerBookingDisplayItem?>(null) }
  var pendingReviewItem by remember { mutableStateOf<CustomerBookingDisplayItem?>(null) }
  var pendingDisputeItem by remember { mutableStateOf<CustomerBookingDisplayItem?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = bottomBar,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "My Bookings",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "bookings_back_button",
              modifier = Modifier.padding(start = ParkSpaceSpacing.xs),
            )
          },
          actions = {
            ParkSpaceCompactDemoPill(
              modifier = Modifier.padding(end = ParkSpaceSpacing.xs).testTag("mode_banner"),
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
            ),
        )
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize(),
        ) {
          if (actionFeedback != null) {
            Column(
              modifier =
                Modifier.fillMaxWidth().padding(horizontal = ParkSpaceSpacing.screenHorizontal),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("bookings_feedback_banner"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, semantic.glassBorder),
              ) {
                Row(
                  modifier = Modifier.padding(ParkSpaceSpacing.sm),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = actionFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                  )
                  TextButton(onClick = onDismissFeedback) { Text("OK") }
                }
              }
            }
          }

        ParkSpaceStateContainer(
          state = uiState,
          modifier = Modifier.weight(1f),
        ) { data ->
          Column(modifier = Modifier.fillMaxSize()) {
            LazyRow(
              modifier = Modifier.fillMaxWidth().padding(vertical = ParkSpaceSpacing.xs),
              contentPadding = PaddingValues(horizontal = ParkSpaceSpacing.screenHorizontal),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              items(BookingHistoryTab.entries) { tab ->
                val count =
                  when (tab) {
                    BookingHistoryTab.UPCOMING -> data.upcoming.size
                    BookingHistoryTab.ACTIVE -> data.active.size
                    BookingHistoryTab.COMPLETED -> data.completed.size
                    BookingHistoryTab.CANCELLED -> data.cancelled.size
                  }
                FilterChip(
                  selected = data.selectedTab == tab,
                  onClick = { onSelectTab(tab) },
                  label = { Text("${tab.label} ($count)") },
                  shape = CircleShape,
                  modifier =
                    Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("bookings_tab_${tab.name.lowercase()}"),
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
              }
            }

            val itemsForTab = data.itemsForSelectedTab()
            val hasAnyBookingAtAll =
              data.upcoming.isNotEmpty() ||
                data.active.isNotEmpty() ||
                data.completed.isNotEmpty() ||
                data.cancelled.isNotEmpty()
            if (itemsForTab.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
                contentAlignment = Alignment.Center,
              ) {
                ParkSpaceGlassSurface(
                  modifier = Modifier.testTag("bookings_empty_state"),
                  accentGlow = semantic.customerGlow,
                  contentPaddingDp = 22,
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.ReceiptLong,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(36.dp),
                    )
                    Text(
                      text =
                        if (!hasAnyBookingAtAll) "No bookings yet."
                        else "No ${data.selectedTab.label} bookings yet.",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                      text = "Explore Host-created parking spaces across Lahore, Islamabad, Faisalabad, and Karachi.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ParkSpaceLiquidGlassButton(
                      text = if (!hasAnyBookingAtAll) "Make Your First Booking" else "Explore Parking",
                      onClick = onOpenDiscovery,
                      leadingIcon = Icons.Outlined.DirectionsCar,
                      modifier = Modifier.testTag("empty_bookings_discover_button"),
                    )
                  }
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("bookings_history_list"),
                contentPadding =
                  PaddingValues(
                    horizontal = ParkSpaceSpacing.screenHorizontal,
                    vertical = ParkSpaceSpacing.sm,
                  ),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
              ) {
                items(itemsForTab, key = { it.booking.id }) { item ->
                  BookingHistoryItemCard(
                    item = item,
                    onOpenDetail = { onOpenBookingDetail(item.booking.id) },
                    onConfirmPendingPayment = {
                      if (onConfirmPendingPaymentWithMethod != null) {
                        pendingPayItem = item
                      } else {
                        onConfirmPendingPayment(item.booking.id)
                      }
                    },
                    onCheckIn = { onCheckIn(item.booking.id) },
                    onCheckOut = { onCheckOut(item.booking.id) },
                    onRequestCancel = { pendingCancelItem = item },
                    onOpenChat = onOpenChat?.let { cb -> { cb(item.booking.id) } },
                    onWriteReview =
                      if (onSubmitReview != null &&
                        (item.booking.status == BookingStatus.CHECKED_OUT ||
                          item.booking.status == BookingStatus.COMPLETED) &&
                        !item.hasSubmittedReview
                      ) {
                        { pendingReviewItem = item }
                      } else null,
                    onReportIssue = onFileDispute?.let { { pendingDisputeItem = item } },
                  )
                }
              }
            }
          }
        }
      }

      val payTarget = pendingPayItem
      if (payTarget != null && onConfirmPendingPaymentWithMethod != null) {
        val creditBalance = (uiState as? UiState.Success)?.data?.availableDemoCreditPkr ?: 0
        DemoPaymentMethodDialog(
          bookingCode = payTarget.booking.bookingCode,
          amountPkr = payTarget.booking.totalAmountPkr,
          availableDemoCreditPkr = creditBalance,
          onDismiss = { pendingPayItem = null },
          onConfirmPayment = { method ->
            val id = payTarget.booking.id
            pendingPayItem = null
            onConfirmPendingPaymentWithMethod(id, method)
          },
        )
      }

      val revTarget = pendingReviewItem
      if (revTarget != null && onSubmitReview != null) {
        SubmitVerifiedReviewDialog(
          bookingCode = revTarget.booking.bookingCode,
          spaceTitle = revTarget.spaceTitle,
          onDismiss = { pendingReviewItem = null },
          onSubmit = { rating, comment ->
            val id = revTarget.booking.id
            pendingReviewItem = null
            onSubmitReview(id, rating, comment)
          },
        )
      }

      val dispTarget = pendingDisputeItem
      if (dispTarget != null && onFileDispute != null) {
        FileBookingDisputeDialog(
          bookingCode = dispTarget.booking.bookingCode,
          spaceTitle = dispTarget.spaceTitle,
          onDismiss = { pendingDisputeItem = null },
          onSubmit = { cat, desc ->
            val id = dispTarget.booking.id
            pendingDisputeItem = null
            onFileDispute(id, cat, desc)
          },
        )
      }

      val targetCancel = pendingCancelItem
      if (targetCancel != null && targetCancel.cancellationQuote != null) {
        val quote = targetCancel.cancellationQuote
        Dialog(
          onDismissRequest = { pendingCancelItem = null },
          properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
          Surface(
            modifier =
              Modifier
                .padding(horizontal = ParkSpaceSpacing.screenHorizontal)
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = semantic.glassSurfaceElevated,
            border = BorderStroke(1.dp, semantic.glassBorder),
            tonalElevation = 6.dp,
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.lg),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              Text(
                text = "Cancel ${targetCancel.booking.bookingCode}?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
              Text(quote.policyRuleExplanation, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = "Calculated Demo Refund: ${formatPkr(quote.refundAmountPkr)} (${quote.refundPercentageLabel})",
                style = ParkSpaceMonoPriceStyle,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Simulated Demo calculation only — no real refund is processed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
              ) {
                TextButton(
                  onClick = { pendingCancelItem = null },
                  modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
                ) {
                  Text("Keep")
                }
                Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
                TextButton(
                  onClick = {
                    val id = targetCancel.booking.id
                    pendingCancelItem = null
                    onCancelBooking(id)
                  },
                  modifier =
                    Modifier
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("history_confirm_cancel_button"),
                ) {
                  Text("Confirm Cancellation", color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }
  }
  }
}

@Composable
private fun BookingHistoryItemCard(
  item: CustomerBookingDisplayItem,
  onOpenDetail: () -> Unit,
  onConfirmPendingPayment: () -> Unit,
  onCheckIn: () -> Unit,
  onCheckOut: () -> Unit,
  onRequestCancel: () -> Unit,
  onOpenChat: (() -> Unit)? = null,
  onWriteReview: (() -> Unit)? = null,
  onReportIssue: (() -> Unit)? = null,
) {
  val booking = item.booking
  val semantic = ParkSpaceThemeTokens.semantic
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onOpenDetail)
        .testTag("booking_history_card_${booking.id}"),
    shape = RoundedCornerShape(22.dp),
    color = semantic.glassSurfaceElevated,
    border = BorderStroke(1.dp, semantic.glassBorder),
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ParkSpaceStatusPill(
          label = booking.status.displayName,
          tone = bookingStatusTone(booking.status),
        )
        Text(
          text = booking.bookingCode,
          style = ParkSpaceMonoCodeStyle,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Text(text = item.spaceTitle, style = MaterialTheme.typography.titleMedium)
      Text(
        text = "${item.areaLabel}, ${booking.city.displayName} • ${item.vehicleLabel} (${item.vehiclePlateMasked})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "${booking.bookingType.displayName} (${formatDurationSummary(booking)}) • ${formatBookingWindowPkt(booking.startEpochMs, booking.endEpochMs)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = booking.demoPaymentStatus.displayLabel +
            (booking.demoPaymentMethod?.let { " • ${it.displayName}" } ?: ""),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = formatPkr(booking.totalAmountPkr),
          style = ParkSpaceMonoPriceStyle,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (booking.status == BookingStatus.CANCELLED && booking.refundAmountPkr != null) {
        Text(
          text = "Demo Refund: ${formatPkr(booking.refundAmountPkr)} (${booking.cancellationPolicySummary ?: "Cancelled"})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        ParkSpaceLiquidGlassSecondaryButton(
          text = if (booking.hasActiveGateAccess()) "Entry Pass" else "Details",
          onClick = onOpenDetail,
          modifier = Modifier.weight(1f).testTag("open_booking_detail_${booking.id}"),
        )

        when (booking.status) {
          BookingStatus.PENDING_PAYMENT -> {
            ParkSpaceLiquidGlassButton(
              text = "Pay Demo",
              onClick = onConfirmPendingPayment,
              modifier = Modifier.weight(1f).testTag("history_pay_${booking.id}"),
            )
          }
          BookingStatus.CONFIRMED -> {
            ParkSpaceLiquidGlassButton(
              text = "Check In",
              onClick = onCheckIn,
              modifier = Modifier.weight(1f).testTag("history_check_in_${booking.id}"),
            )
          }
          BookingStatus.CHECKED_IN -> {
            ParkSpaceLiquidGlassButton(
              text = "Check Out",
              onClick = onCheckOut,
              modifier = Modifier.weight(1f).testTag("history_check_out_${booking.id}"),
            )
          }
          else -> Unit
        }

        if (item.cancellationQuote?.isEligibleToCancel == true) {
          OutlinedButton(
            onClick = onRequestCancel,
            shape = CircleShape,
            modifier =
              Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("history_cancel_${booking.id}"),
            colors =
              ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
              ),
          ) {
            Text("Cancel")
          }
        }
      }

      if (onOpenChat != null || onWriteReview != null || onReportIssue != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          if (onOpenChat != null) {
            TextButton(
              onClick = onOpenChat,
              modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("history_chat_${booking.id}"),
            ) {
              Text("Message Host")
            }
          }
          if (onWriteReview != null) {
            TextButton(
              onClick = onWriteReview,
              modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("history_review_${booking.id}"),
            ) {
              Text("Write Review")
            }
          }
          if (onReportIssue != null) {
            TextButton(
              onClick = onReportIssue,
              modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("history_dispute_${booking.id}"),
            ) {
              Text("Report Issue")
            }
          }
        }
      }
    }
  }
}
