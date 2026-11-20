package com.example.feature.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.domain.model.DisputeCategory
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@Composable
fun SubmitVerifiedReviewDialog(
  bookingCode: String,
  spaceTitle: String,
  onDismiss: () -> Unit,
  onSubmit: (rating: Int, comment: String) -> Unit,
) {
  var rating by rememberSaveable { mutableIntStateOf(5) }
  var comment by rememberSaveable { mutableStateOf("") }
  var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  Dialog(
    onDismissRequest = onDismiss,
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
          text = "Rate $spaceTitle",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = "Verified Booking $bookingCode",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs)) {
          (1..5).forEach { star ->
            IconButton(
              onClick = { rating = star },
              modifier =
                Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("review_star_$star"),
            ) {
              Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "$star stars",
                tint =
                  if (star <= rating) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        OutlinedTextField(
          value = comment,
          onValueChange = {
            comment = it
            errorMsg = null
          },
          label = { Text("Share your parking experience *") },
          placeholder = { Text("Entry ease, lighting, guard support...") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("review_comment_input"),
        )
        if (errorMsg != null) {
          Text(
            text = errorMsg.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
          ParkSpaceLiquidGlassButton(
            text = "Submit Review",
            onClick = {
              if (comment.trim().length < 3) {
                errorMsg = "Enter at least 3 characters in your review."
              } else {
                onSubmit(rating, comment.trim())
              }
            },
            modifier = Modifier.testTag("submit_review_confirm_button"),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileBookingDisputeDialog(
  bookingCode: String,
  spaceTitle: String,
  onDismiss: () -> Unit,
  onSubmit: (category: DisputeCategory, description: String) -> Unit,
) {
  var selectedCategory by rememberSaveable { mutableStateOf(DisputeCategory.GATE_ACCESS_ISSUE) }
  var description by rememberSaveable { mutableStateOf("") }
  var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  Dialog(
    onDismissRequest = onDismiss,
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
        modifier = Modifier.padding(ParkSpaceSpacing.lg).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text(
          text = "Report Booking Issue ($bookingCode)",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = spaceTitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Issue Category",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
        ) {
          DisputeCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
              shape = CircleShape,
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                  selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
              modifier =
                Modifier
                  .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                  .testTag("dispute_category_${cat.name.lowercase()}"),
            )
          }
        }
        OutlinedTextField(
          value = description,
          onValueChange = {
            description = it
            errorMsg = null
          },
          label = { Text("Describe what happened (min 10 chars) *") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("dispute_description_input"),
        )
        if (errorMsg != null) {
          Text(
            text = errorMsg.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
          ParkSpaceLiquidGlassButton(
            text = "Submit Complaint",
            onClick = {
              if (description.trim().length < 10) {
                errorMsg = "Provide at least 10 characters explaining the issue."
              } else {
                onSubmit(selectedCategory, description.trim())
              }
            },
            modifier = Modifier.testTag("submit_dispute_confirm_button"),
          )
        }
      }
    }
  }
}
