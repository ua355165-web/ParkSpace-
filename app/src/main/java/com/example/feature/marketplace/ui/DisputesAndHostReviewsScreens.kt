package com.example.feature.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.core.config.UserRole
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceCompactDemoPill
import com.example.core.designsystem.components.ParkSpaceGlassIconButton
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.BookingDispute
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.feature.booking.ui.formatDateTimePkt
import com.example.feature.marketplace.DisputesCenterUiState
import com.example.feature.marketplace.HostReviewItem
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisputesCenterScreen(
  uiState: DisputesCenterUiState,
  feedback: String?,
  prefillBookingId: String?,
  onBack: () -> Unit,
  onFileDispute: (bookingId: String, category: DisputeCategory, description: String) -> Unit,
  onRespondAsHost: (disputeId: String, note: String) -> Unit,
  onResolveDispute: (disputeId: String, note: String) -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = false,
) {
  var showFilePickerBookingDialog by rememberSaveable(prefillBookingId) { mutableStateOf(false) }
  var selectedBookingForNewDispute by remember(prefillBookingId, uiState.eligibleBookings) {
    mutableStateOf(
      if (prefillBookingId != null) uiState.eligibleBookings.firstOrNull { it.first.id == prefillBookingId }
      else null
    )
  }
  var hostRespondTarget by remember { mutableStateOf<BookingDispute?>(null) }
  var resolveTarget by remember { mutableStateOf<BookingDispute?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = { Text("Complaints & Disputes", style = MaterialTheme.typography.titleLarge) },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "disputes_back_button",
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
              scrolledContainerColor = semantic.glassSurfaceElevated,
            ),
        )
      },
      floatingActionButton = {
        if (uiState.eligibleBookings.isNotEmpty()) {
          FloatingActionButton(
            onClick = { showFilePickerBookingDialog = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("disputes_fab_file"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = "Report Issue")
          }
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize()) {
          if (feedback != null) {
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = ParkSpaceSpacing.screenHorizontal, vertical = ParkSpaceSpacing.xs)
                  .testTag("disputes_feedback_banner"),
              shape = RoundedCornerShape(16.dp),
              color = semantic.glassSurfaceElevated,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            ) {
              Row(
                modifier = Modifier.padding(ParkSpaceSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = feedback,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissFeedback) { Text("OK") }
              }
            }
          }

          if (uiState.disputes.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
              contentAlignment = Alignment.Center,
            ) {
              ParkSpaceGlassSurface(
                modifier = Modifier.fillMaxWidth().testTag("disputes_empty_state"),
                contentPaddingDp = 24,
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
                ) {
                  Icon(
                    Icons.Outlined.ReportProblem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                  )
                  Text(stringResource(R.string.empty_disputes), style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "Issues reported by Customers or Hosts on an existing booking appear here with live resolution status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize().testTag("disputes_list"),
              contentPadding = PaddingValues(ParkSpaceSpacing.screenHorizontal, ParkSpaceSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              items(uiState.disputes, key = { it.id }) { dispute ->
                val tone =
                  when (dispute.status) {
                    DisputeStatus.OPEN -> StatusTone.ERROR
                    DisputeStatus.UNDER_REVIEW -> StatusTone.PENDING
                    DisputeStatus.RESOLVED,
                    DisputeStatus.DISMISSED -> StatusTone.AVAILABLE
                  }
                ParkSpaceGlassSurface(
                  modifier = Modifier.fillMaxWidth().testTag("dispute_card_${dispute.id}"),
                  cornerRadiusDp = 18,
                  contentPaddingDp = 16,
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(dispute.bookingCode, style = ParkSpaceMonoCodeStyle, color = MaterialTheme.colorScheme.primary)
                    ParkSpaceStatusPill(label = dispute.status.displayName, tone = tone)
                  }
                  Text(dispute.category.displayName, style = MaterialTheme.typography.titleSmall)
                  Text(dispute.description, style = MaterialTheme.typography.bodyMedium)
                  if (dispute.hostResponseNote != null) {
                    Text(
                      text = "Host Response: ${dispute.hostResponseNote}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  if (dispute.resolutionNote != null) {
                    Text(
                      text = "Resolution: ${dispute.resolutionNote}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = "Updated: ${formatDateTimePkt(dispute.updatedAtEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (dispute.status == DisputeStatus.OPEN || dispute.status == DisputeStatus.UNDER_REVIEW) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                    ) {
                      if (uiState.activeRole == UserRole.HOST) {
                        ParkSpaceLiquidGlassSecondaryButton(
                          text = "Host Response",
                          onClick = { hostRespondTarget = dispute },
                          modifier = Modifier.weight(1f).testTag("host_respond_dispute_${dispute.id}"),
                        )
                      }
                      ParkSpaceLiquidGlassButton(
                        text = "Mark Resolved",
                        onClick = { resolveTarget = dispute },
                        modifier = Modifier.weight(1f).testTag("resolve_dispute_${dispute.id}"),
                      )
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

  if (showFilePickerBookingDialog) {
    Dialog(onDismissRequest = { showFilePickerBookingDialog = false }) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = semantic.glassSurfaceElevated,
        border = BorderStroke(1.dp, semantic.glassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(ParkSpaceSpacing.lg),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        ) {
          Text("Select Booking to Report", style = MaterialTheme.typography.titleLarge)
          Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
            uiState.eligibleBookings.forEach { pair ->
              ParkSpaceLiquidGlassSecondaryButton(
                text = "${pair.first.bookingCode} • ${pair.second}",
                onClick = {
                  showFilePickerBookingDialog = false
                  selectedBookingForNewDispute = pair
                },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = { showFilePickerBookingDialog = false }) { Text("Cancel") }
          }
        }
      }
    }
  }

  val targetBookingPair = selectedBookingForNewDispute
  if (targetBookingPair != null) {
    FileBookingDisputeDialog(
      bookingCode = targetBookingPair.first.bookingCode,
      spaceTitle = targetBookingPair.second,
      onDismiss = { selectedBookingForNewDispute = null },
      onSubmit = { cat, desc ->
        onFileDispute(targetBookingPair.first.id, cat, desc)
        selectedBookingForNewDispute = null
      },
    )
  }

  val respondTarget = hostRespondTarget
  if (respondTarget != null) {
    SimpleNoteInputDialog(
      title = "Host Response (${respondTarget.bookingCode})",
      label = "Response note (min 5 chars) *",
      confirmLabel = "Post Response",
      onDismiss = { hostRespondTarget = null },
      onConfirm = { note ->
        onRespondAsHost(respondTarget.id, note)
        hostRespondTarget = null
      },
    )
  }

  val resTarget = resolveTarget
  if (resTarget != null) {
    SimpleNoteInputDialog(
      title = "Resolve Complaint (${resTarget.bookingCode})",
      label = "Resolution summary (min 4 chars) *",
      confirmLabel = "Mark Resolved",
      onDismiss = { resolveTarget = null },
      onConfirm = { note ->
        onResolveDispute(resTarget.id, note)
        resolveTarget = null
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostReviewsScreen(
  reviews: List<HostReviewItem>,
  feedback: String?,
  onBack: () -> Unit,
  onReplyToReview: (reviewId: String, reply: String) -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = false,
) {
  var replyTarget by remember { mutableStateOf<HostReviewItem?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = { Text("Customer Reviews (${reviews.size})", style = MaterialTheme.typography.titleLarge) },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "host_reviews_back_button",
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
              scrolledContainerColor = semantic.glassSurfaceElevated,
            ),
        )
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize()) {
          if (feedback != null) {
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = ParkSpaceSpacing.screenHorizontal, vertical = ParkSpaceSpacing.xs)
                  .testTag("host_reviews_feedback_banner"),
              shape = RoundedCornerShape(16.dp),
              color = semantic.glassSurfaceElevated,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            ) {
              Row(
                modifier = Modifier.padding(ParkSpaceSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = feedback,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissFeedback) { Text("OK") }
              }
            }
          }

          if (reviews.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
              contentAlignment = Alignment.Center,
            ) {
              ParkSpaceGlassSurface(
                modifier = Modifier.fillMaxWidth().testTag("host_reviews_empty_state"),
                accentGlow = semantic.hostGlow,
                contentPaddingDp = 24,
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
                ) {
                  Icon(
                    Icons.Outlined.RateReview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                  )
                  Text(stringResource(R.string.empty_reviews), style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "Verified reviews appear only after Customers complete a Demo check-out at your parking spaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize().testTag("host_reviews_list"),
              contentPadding = PaddingValues(ParkSpaceSpacing.screenHorizontal, ParkSpaceSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              items(reviews, key = { it.review.id }) { item ->
                val rev = item.review
                ParkSpaceGlassSurface(
                  modifier = Modifier.fillMaxWidth().testTag("host_review_card_${rev.id}"),
                  accentGlow = semantic.hostGlow,
                  cornerRadiusDp = 18,
                  contentPaddingDp = 16,
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(item.spaceTitle, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                      Text(" ${rev.rating}/5", style = MaterialTheme.typography.labelLarge)
                    }
                  }
                  Text(
                    text = "By ${rev.customerDisplayName} • ${formatDateTimePkt(rev.createdAtEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                  if (rev.hostReply != null) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = semantic.glassSurface,
                      border = BorderStroke(1.dp, semantic.glassBorder),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text(
                        text = "Your Reply: ${rev.hostReply}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(ParkSpaceSpacing.xs),
                      )
                    }
                  } else {
                    ParkSpaceLiquidGlassSecondaryButton(
                      text = "Reply to Customer",
                      onClick = { replyTarget = item },
                      modifier = Modifier.fillMaxWidth().testTag("reply_review_button_${rev.id}"),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  val target = replyTarget
  if (target != null) {
    SimpleNoteInputDialog(
      title = "Reply to ${target.review.customerDisplayName}",
      label = "Your reply *",
      confirmLabel = "Post Reply",
      onDismiss = { replyTarget = null },
      onConfirm = { reply ->
        onReplyToReview(target.review.id, reply)
        replyTarget = null
      },
    )
  }
}

@Composable
private fun SimpleNoteInputDialog(
  title: String,
  label: String,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var note by rememberSaveable { mutableStateOf("") }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
          value = note,
          onValueChange = {
            note = it
            error = null
          },
          label = { Text(label) },
          minLines = 3,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("simple_note_input"),
        )
        if (error != null) {
          Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("Cancel")
          }
          ParkSpaceLiquidGlassButton(
            text = confirmLabel,
            onClick = {
              if (note.trim().length < 4) {
                error = "Enter at least 4 characters."
              } else {
                onConfirm(note.trim())
              }
            },
            modifier = Modifier.testTag("simple_note_confirm_button"),
          )
        }
      }
    }
  }
}
