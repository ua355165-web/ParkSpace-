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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.core.config.UserRole
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpacePrimaryButton
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

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Complaints & Disputes", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("disputes_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
    floatingActionButton = {
      if (uiState.eligibleBookings.isNotEmpty()) {
        FloatingActionButton(
          onClick = { showFilePickerBookingDialog = true },
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
        Box(modifier = Modifier.padding(horizontal = ParkSpaceSpacing.screenHorizontal)) {
          ParkSpaceModeBanner(mode = AppEnvironment.currentMode)
        }

        if (feedback != null) {
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = ParkSpaceSpacing.screenHorizontal, vertical = ParkSpaceSpacing.xs)
                .testTag("disputes_feedback_banner"),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("disputes_empty_state"),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.padding(ParkSpaceSpacing.lg),
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
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("dispute_card_${dispute.id}"),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        OutlinedButton(
                          onClick = { hostRespondTarget = dispute },
                          modifier =
                            Modifier.weight(1f)
                              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                              .testTag("host_respond_dispute_${dispute.id}"),
                        ) {
                          Text("Host Response")
                        }
                      }
                      ParkSpacePrimaryButton(
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
    AlertDialog(
      onDismissRequest = { showFilePickerBookingDialog = false },
      title = { Text("Select Booking to Report") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          uiState.eligibleBookings.forEach { pair ->
            OutlinedButton(
              onClick = {
                showFilePickerBookingDialog = false
                selectedBookingForNewDispute = pair
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("${pair.first.bookingCode} • ${pair.second}")
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showFilePickerBookingDialog = false }) { Text("Cancel") }
      },
    )
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
) {
  var replyTarget by remember { mutableStateOf<HostReviewItem?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Customer Reviews (${reviews.size})", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("host_reviews_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = ParkSpaceSpacing.screenHorizontal)) {
          ParkSpaceModeBanner(mode = AppEnvironment.currentMode)
        }

        if (feedback != null) {
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = ParkSpaceSpacing.screenHorizontal, vertical = ParkSpaceSpacing.xs)
                .testTag("host_reviews_feedback_banner"),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("host_reviews_empty_state"),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.padding(ParkSpaceSpacing.lg),
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
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("host_review_card_${rev.id}"),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                      shape = MaterialTheme.shapes.extraSmall,
                      color = MaterialTheme.colorScheme.surfaceVariant,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text(
                        text = "Your Reply: ${rev.hostReply}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(ParkSpaceSpacing.xs),
                      )
                    }
                  } else {
                    OutlinedButton(
                      onClick = { replyTarget = item },
                      modifier =
                        Modifier.fillMaxWidth()
                          .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                          .testTag("reply_review_button_${rev.id}"),
                    ) {
                      Text("Reply to Customer")
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

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        OutlinedTextField(
          value = note,
          onValueChange = {
            note = it
            error = null
          },
          label = { Text(label) },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("simple_note_input"),
        )
        if (error != null) {
          Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      ParkSpacePrimaryButton(
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
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
      ) {
        Text("Cancel")
      }
    },
  )
}
