package com.example.feature.booking.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DisputeCategory
import com.example.feature.booking.CustomerBookingDisplayItem
import com.example.feature.marketplace.ui.DemoPaymentMethodDialog
import com.example.feature.marketplace.ui.FileBookingDisputeDialog
import com.example.feature.marketplace.ui.SubmitVerifiedReviewDialog
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
  uiState: UiState<CustomerBookingDisplayItem>,
  actionFeedback: String?,
  onBack: () -> Unit,
  onOpenBookingsHistory: () -> Unit,
  onConfirmPendingPayment: (String) -> Unit,
  onCheckIn: (String) -> Unit,
  onCheckOut: (String) -> Unit,
  onCancelBooking: (String) -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
  availableDemoCreditPkr: Int = 0,
  onConfirmPendingPaymentWithMethod: ((String, DemoPaymentMethod) -> Unit)? = null,
  onOpenChat: ((String) -> Unit)? = null,
  onSubmitReview: ((bookingId: String, rating: Int, comment: String) -> Unit)? = null,
  onFileDispute: ((bookingId: String, category: DisputeCategory, description: String) -> Unit)? = null,
  isDarkTheme: Boolean = false,
) {
  var showCancelDialog by rememberSaveable { mutableStateOf(false) }
  var showPaymentMethodDialog by rememberSaveable { mutableStateOf(false) }
  var showReviewDialog by rememberSaveable { mutableStateOf(false) }
  var showDisputeDialog by rememberSaveable { mutableStateOf(false) }
  val semantic = ParkSpaceThemeTokens.semantic

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Booking Pass & Summary",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "confirmation_back_button",
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
        ParkSpaceStateContainer(
          state = uiState,
          modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize(),
        ) { item ->
          val booking = item.booking

          Column(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                  horizontal = ParkSpaceSpacing.screenHorizontal,
                  vertical = ParkSpaceSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
          ) {
            if (actionFeedback != null) {
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("confirmation_feedback_banner"),
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

            // Hero Booking Reference & Status Card
            ParkSpaceGlassSurface(
              modifier = Modifier.testTag("booking_confirmation_hero_card"),
              accentGlow = semantic.customerGlow,
              contentPaddingDp = 18,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ParkSpaceStatusPill(
                  label = booking.status.displayName,
                  tone = bookingStatusTone(booking.status),
                  modifier = Modifier.testTag("booking_status_pill"),
                )
                Text(
                  text = booking.bookingCode,
                  style = ParkSpaceMonoCodeStyle,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.testTag("booking_reference_code"),
                )
              }

              Text(text = item.spaceTitle, style = MaterialTheme.typography.titleLarge)
              Text(
                text = "${item.areaLabel}, ${booking.city.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

              DetailRow("Vehicle", "${item.vehicleLabel} (${item.vehiclePlateMasked})")
              DetailRow("Plan & Duration", "${booking.bookingType.displayName} • ${formatDurationSummary(booking)}")
              DetailRow("Schedule", formatBookingWindowPkt(booking.startEpochMs, booking.endEpochMs))
              DetailRow("Assigned Bay", booking.bayId.substringAfterLast("-").uppercase())
              DetailRow("Demo Payment Status", booking.demoPaymentStatus.displayLabel)
              if (booking.demoPaymentMethod != null) {
                DetailRow("Demo Method", booking.demoPaymentMethod.displayName)
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

              DetailRow("Base Parking Charge", formatPkr(booking.baseAmountPkr))
              DetailRow("Demo Platform Fee", formatPkr(booking.serviceFeePkr))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Total PKR (Demo)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = formatPkr(booking.totalAmountPkr),
                  style = ParkSpaceMonoPriceStyle,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.testTag("confirmation_total_pkr"),
                )
              }
            }

            // Simulated Entry / Exit & Gate Security Card
            ParkSpaceGlassSurface(
              modifier = Modifier.testTag("booking_gate_access_card"),
              accentGlow = semantic.customerGlow,
              contentPaddingDp = 18,
            ) {
              if (booking.hasActiveGateAccess() && item.simulatedPin != null) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = "Simulated Demo Entry Pass Unlocked",
                    style = MaterialTheme.typography.titleMedium,
                  )
                }

                if (item.unlockedGateSummary != null) {
                  Text(
                    text = "Exact Entrance: ${item.unlockedGateSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("unlocked_gate_summary"),
                  )
                }
                if (item.unlockedExactCoordinatesLabel != null) {
                  Text(
                    text = "Exact Coordinates: ${item.unlockedExactCoordinatesLabel}",
                    style = ParkSpaceMonoCodeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("unlocked_exact_coords"),
                  )
                }

                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(ParkSpaceSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
                  ) {
                    Text(
                      text = "SIMULATED DEMO GATE PIN (NOT A REAL ACCESS CREDENTIAL)",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                      text = item.simulatedPin,
                      style = MaterialTheme.typography.headlineSmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.testTag("simulated_demo_pin_value"),
                    )
                  }
                }

                if (item.simulatedQrPayload != null) {
                  Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                  ) {
                    SimulatedDemoQrPass(
                      payload = item.simulatedQrPayload,
                      bookingCode = booking.bookingCode,
                    )
                  }
                }

                if (item.overstayMinutes > 0L) {
                  Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().testTag("booking_overstay_warning"),
                  ) {
                    Row(
                      modifier = Modifier.padding(ParkSpaceSpacing.sm),
                      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                      )
                      Text(
                        text =
                          "Overstay Notice: Booked slot ended ${item.overstayMinutes} min ago. Complete Demo check-out.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                      )
                    }
                  }
                }

                // Check-in / Check-out Controls
                when (booking.status) {
                  BookingStatus.CONFIRMED -> {
                    ParkSpaceLiquidGlassButton(
                      text = "Simulate Demo Check-In",
                      onClick = { onCheckIn(booking.id) },
                      leadingIcon = Icons.Outlined.Login,
                      modifier = Modifier.fillMaxWidth().testTag("simulate_check_in_button"),
                    )
                  }
                  BookingStatus.CHECKED_IN -> {
                    if (booking.checkedInAtEpochMs != null) {
                      Text(
                        text = "Checked in at ${formatDateTimePkt(booking.checkedInAtEpochMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    ParkSpaceLiquidGlassButton(
                      text = "Simulate Demo Check-Out",
                      onClick = { onCheckOut(booking.id) },
                      leadingIcon = Icons.Outlined.Logout,
                      modifier = Modifier.fillMaxWidth().testTag("simulate_check_out_button"),
                    )
                  }
                  else -> Unit
                }
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text =
                      when (booking.status) {
                        BookingStatus.PENDING_PAYMENT -> "Exact Gate & Demo PIN Locked"
                        BookingStatus.CHECKED_OUT,
                        BookingStatus.COMPLETED -> "Booking Completed"
                        BookingStatus.CANCELLED -> "Booking Cancelled • Access Revoked"
                        BookingStatus.EXPIRED -> "Booking Expired • Access Locked"
                        else -> "Gate Access Unavailable"
                      },
                    style = MaterialTheme.typography.titleMedium,
                  )
                }

                Text(
                  text =
                    when (booking.status) {
                      BookingStatus.PENDING_PAYMENT ->
                        "Approximate zone only (${item.maskedCoordinatesLabel}). Complete simulated Demo payment to unlock the Demo PIN, QR pass, and exact entrance instructions."
                      BookingStatus.CANCELLED ->
                        booking.cancellationPolicySummary
                          ?: "This booking was cancelled and Demo gate access has been revoked."
                      BookingStatus.EXPIRED ->
                        "The scheduled parking window ended before confirmation/check-in."
                      BookingStatus.CHECKED_OUT,
                      BookingStatus.COMPLETED ->
                        "Demo exit recorded. Simulated Demo settlement complete."
                      else -> "No active gate pass."
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag("gate_locked_or_past_notice"),
                )

                if (booking.refundAmountPkr != null && booking.status == BookingStatus.CANCELLED) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().testTag("cancellation_refund_summary_card"),
                  ) {
                    Column(modifier = Modifier.padding(ParkSpaceSpacing.sm)) {
                      Text(
                        text = "Demo Refund Outcome (Simulated — No Real Refund)",
                        style = MaterialTheme.typography.labelMedium,
                      )
                      Text(
                        text = "Calculated Demo Refund: ${formatPkr(booking.refundAmountPkr)}",
                        style = ParkSpaceMonoPriceStyle,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }

                if (booking.status == BookingStatus.PENDING_PAYMENT) {
                  ParkSpaceLiquidGlassButton(
                    text = "Complete Demo Payment (${formatPkr(booking.totalAmountPkr)})",
                    onClick = {
                      if (onConfirmPendingPaymentWithMethod != null) {
                        showPaymentMethodDialog = true
                      } else {
                        onConfirmPendingPayment(booking.id)
                      }
                    },
                    leadingIcon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.fillMaxWidth().testTag("complete_demo_payment_button"),
                  )
                }
              }

              if (item.cancellationQuote?.isEligibleToCancel == true) {
                OutlinedButton(
                  onClick = { showCancelDialog = true },
                  shape = CircleShape,
                  modifier =
                    Modifier.fillMaxWidth()
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("open_cancel_dialog_button"),
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.size(ParkSpaceSpacing.xs))
                  Text("Cancel Booking (${item.cancellationQuote.refundPercentageLabel} Demo Refund)")
                }
              }
            }

            // Milestone 5 Actions: Chat with Host, Verified Review, Report Issue
            if (onOpenChat != null) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Message Host About This Booking",
                leadingIcon = Icons.Outlined.ChatBubbleOutline,
                onClick = { onOpenChat(booking.id) },
                modifier = Modifier.fillMaxWidth().testTag("confirmation_message_host_button"),
              )
            }

            if ((booking.status == BookingStatus.CHECKED_OUT || booking.status == BookingStatus.COMPLETED) &&
              !item.hasSubmittedReview &&
              onSubmitReview != null
            ) {
              ParkSpaceLiquidGlassButton(
                text = "Write Verified Review",
                onClick = { showReviewDialog = true },
                leadingIcon = Icons.Outlined.RateReview,
                modifier = Modifier.fillMaxWidth().testTag("confirmation_write_review_button"),
              )
            } else if (item.hasSubmittedReview) {
              ParkSpaceStatusPill(
                label = "Verified Review Submitted",
                tone = StatusTone.AVAILABLE,
                modifier = Modifier.testTag("confirmation_review_submitted_pill"),
              )
            }

            if (onFileDispute != null) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Report an Issue / Complaint",
                leadingIcon = Icons.Outlined.ReportProblem,
                onClick = { showDisputeDialog = true },
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().testTag("confirmation_report_issue_button"),
              )
            }

            ParkSpaceLiquidGlassSecondaryButton(
              text = "View All My Bookings",
              leadingIcon = Icons.Outlined.ReceiptLong,
              onClick = onOpenBookingsHistory,
              modifier = Modifier.fillMaxWidth().testTag("view_my_bookings_button"),
            )

            Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
          }

        if (showPaymentMethodDialog && onConfirmPendingPaymentWithMethod != null) {
          DemoPaymentMethodDialog(
            bookingCode = booking.bookingCode,
            amountPkr = booking.totalAmountPkr,
            availableDemoCreditPkr = availableDemoCreditPkr,
            onDismiss = { showPaymentMethodDialog = false },
            onConfirmPayment = { method ->
              showPaymentMethodDialog = false
              onConfirmPendingPaymentWithMethod(booking.id, method)
            },
          )
        }

        if (showReviewDialog && onSubmitReview != null) {
          SubmitVerifiedReviewDialog(
            bookingCode = booking.bookingCode,
            spaceTitle = item.spaceTitle,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
              showReviewDialog = false
              onSubmitReview(booking.id, rating, comment)
            },
          )
        }

        if (showDisputeDialog && onFileDispute != null) {
          FileBookingDisputeDialog(
            bookingCode = booking.bookingCode,
            spaceTitle = item.spaceTitle,
            onDismiss = { showDisputeDialog = false },
            onSubmit = { category, description ->
              showDisputeDialog = false
              onFileDispute(booking.id, category, description)
            },
          )
        }

        if (showCancelDialog && item.cancellationQuote != null) {
          val quote = item.cancellationQuote
          Dialog(
            onDismissRequest = { showCancelDialog = false },
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
                  text = "Cancel Demo Booking?",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(quote.policyRuleExplanation, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "Estimated Demo Refund: ${formatPkr(quote.refundAmountPkr)} (${quote.refundPercentageLabel})",
                  style = ParkSpaceMonoPriceStyle,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "Simulated Demo calculation only — no real refund transaction occurs.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                ) {
                  TextButton(
                    onClick = { showCancelDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
                  ) {
                    Text("Keep Booking")
                  }
                  Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
                  TextButton(
                    onClick = {
                      showCancelDialog = false
                      onCancelBooking(booking.id)
                    },
                    modifier =
                      Modifier
                        .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("confirm_cancel_booking_button"),
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
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.45f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(0.55f),
    )
  }
}

fun bookingStatusTone(status: BookingStatus): StatusTone =
  when (status) {
    BookingStatus.CONFIRMED,
    BookingStatus.CHECKED_IN -> StatusTone.AVAILABLE
    BookingStatus.PENDING_PAYMENT,
    BookingStatus.DRAFT -> StatusTone.PENDING
    BookingStatus.CHECKED_OUT,
    BookingStatus.COMPLETED -> StatusTone.INFO
    BookingStatus.CANCELLED,
    BookingStatus.EXPIRED,
    BookingStatus.DISPUTED -> StatusTone.ERROR
  }
