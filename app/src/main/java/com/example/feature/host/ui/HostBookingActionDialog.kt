package com.example.feature.host.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.ParkingSpace
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun HostBookingActionDialog(
  booking: Booking,
  space: ParkingSpace?,
  onDismiss: () -> Unit,
  onCheckIn: (bookingId: String) -> Unit,
  onCheckOut: (bookingId: String) -> Unit,
  onCancelBooking: (bookingId: String, reason: String) -> Unit,
  modifier: Modifier = Modifier,
  onMessageCustomer: (() -> Unit)? = null,
  onReportDispute: (() -> Unit)? = null,
) {
  var showCancelPrompt by rememberSaveable { mutableStateOf(false) }
  var cancelReason by rememberSaveable { mutableStateOf("") }

  val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm a")
  val startFormatted =
    Instant.ofEpochMilli(booking.startEpochMs)
      .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
      .format(formatter)
  val endFormatted =
    Instant.ofEpochMilli(booking.endEpochMs)
      .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
      .format(formatter)

  val tone = when (booking.status) {
    BookingStatus.CONFIRMED -> StatusTone.AVAILABLE
    BookingStatus.CHECKED_IN -> StatusTone.PENDING
    BookingStatus.CHECKED_OUT, BookingStatus.COMPLETED -> StatusTone.AVAILABLE
    BookingStatus.CANCELLED -> StatusTone.ERROR
    BookingStatus.PENDING_PAYMENT, BookingStatus.DRAFT, BookingStatus.EXPIRED, BookingStatus.DISPUTED -> StatusTone.INFO
  }

  val semantic = ParkSpaceThemeTokens.semantic

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        modifier
          .fillMaxWidth(0.95f)
          .widthIn(max = 520.dp)
          .padding(ParkSpaceSpacing.sm)
          .testTag("host_booking_action_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.md).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Booking Management", style = MaterialTheme.typography.titleLarge)
            Text(
              text = booking.bookingCode,
              style = ParkSpaceMonoCodeStyle,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("host_booking_dialog_close_button")) {
            Icon(Icons.Outlined.Close, contentDescription = "Close dialog")
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          ParkSpaceStatusPill(label = booking.status.displayName, tone = tone)
          ParkSpaceStatusPill(label = booking.demoPaymentStatus.displayLabel, tone = StatusTone.INFO)
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = semantic.glassSurface,
          border = BorderStroke(1.dp, semantic.glassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            Text(text = "Listing: ${space?.title ?: "Space #${booking.spaceId}"}", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "Time Window (PKT):\n$startFormatted\n→ $endFormatted",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Assigned Bay: ${booking.bayId}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (booking.demoEntryPin != null) {
              Text(
                text = "Simulated Gate PIN: ${booking.demoEntryPin}",
                style = ParkSpaceMonoCodeStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = semantic.glassSurface,
          border = BorderStroke(1.dp, semantic.glassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
          ) {
            Text(text = "Demo Financial Breakdown", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "Host Share (Parking Revenue):", style = MaterialTheme.typography.bodySmall)
              Text(text = "Rs. ${booking.baseAmountPkr}", style = ParkSpaceMonoCodeStyle, color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "Demo Platform Fee:", style = MaterialTheme.typography.bodySmall)
              Text(text = "Rs. ${booking.serviceFeePkr}", style = ParkSpaceMonoCodeStyle)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "Total Customer Paid:", style = MaterialTheme.typography.bodyMedium)
              Text(text = "Rs. ${booking.totalAmountPkr}", style = ParkSpaceMonoCodeStyle)
            }
            if (booking.refundAmountPkr != null) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Refunded to Customer:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Text(text = "Rs. ${booking.refundAmountPkr}", style = ParkSpaceMonoCodeStyle, color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        if (booking.cancellationPolicySummary != null) {
          Text(
            text = "Cancellation Note: ${booking.cancellationPolicySummary}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))

        // Action Buttons
        if (!showCancelPrompt) {
          Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
            if (booking.status == BookingStatus.CONFIRMED) {
              ParkSpaceLiquidGlassButton(
                text = "Check-In Arriving Customer",
                onClick = {
                  onCheckIn(booking.id)
                  onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("host_dialog_check_in_button"),
              )
            }

            if (booking.status == BookingStatus.CHECKED_IN) {
              ParkSpaceLiquidGlassButton(
                text = "Record Customer Check-Out",
                onClick = {
                  onCheckOut(booking.id)
                  onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("host_dialog_check_out_button"),
              )
            }

            if (onMessageCustomer != null) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Message Customer",
                leadingIcon = Icons.Outlined.ChatBubbleOutline,
                onClick = {
                  onDismiss()
                  onMessageCustomer()
                },
                modifier =
                  Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                    .testTag("host_dialog_message_customer_button"),
              )
            }

            if (onReportDispute != null) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "File / View Complaint",
                leadingIcon = Icons.Outlined.ReportProblem,
                onClick = {
                  onDismiss()
                  onReportDispute()
                },
                modifier =
                  Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                    .testTag("host_dialog_report_dispute_button"),
              )
            }

            if (booking.status in setOf(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT, BookingStatus.CHECKED_IN)) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Host Cancel (Issue 100% Demo Refund)",
                leadingIcon = Icons.Outlined.Cancel,
                accentColor = MaterialTheme.colorScheme.error,
                onClick = { showCancelPrompt = true },
                modifier =
                  Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                    .testTag("host_dialog_open_cancel_button"),
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(ParkSpaceSpacing.sm), verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
              Text(
                text = "Cancel Booking & Refund Customer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
              )
              Text(
                text = "Host-initiated cancellations provide a 100% Demo refund (Rs. ${booking.totalAmountPkr}) to the customer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              OutlinedTextField(
                value = cancelReason,
                onValueChange = { cancelReason = it },
                label = { Text("Reason for Cancellation *") },
                placeholder = { Text("e.g. Property maintenance, gate obstruction") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("host_cancel_reason_input"),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                ParkSpaceLiquidGlassSecondaryButton(
                  text = "Back",
                  onClick = { showCancelPrompt = false },
                  modifier = Modifier.weight(1f),
                )
                ParkSpaceLiquidGlassButton(
                  text = "Confirm Cancel",
                  accentColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                  onClick = {
                    onCancelBooking(booking.id, cancelReason)
                    onDismiss()
                  },
                  modifier = Modifier.weight(1f).testTag("host_confirm_cancel_button"),
                )
              }
            }
          }
        }
      }
    }
  }
}
