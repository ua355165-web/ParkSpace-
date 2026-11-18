package com.example.feature.discovery.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.feature.discovery.domain.model.ParkingPublicListing
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParkingPreviewCard(
  listing: ParkingPublicListing,
  onViewDetails: (String) -> Unit,
  onDismiss: (() -> Unit)? = null,
  isSelectedHighlight: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { onViewDetails(listing.id) }
        .testTag("parking_card_${listing.id}"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        width = if (isSelectedHighlight) 2.dp else 1.dp,
        color =
          if (isSelectedHighlight) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    tonalElevation = if (isSelectedHighlight) 4.dp else 1.dp,
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
    ) {
      ParkingSpaceMediaBanner(
        photoUris = listing.photoUris,
        parkingType = listing.parkingType,
        isCovered = listing.isCovered,
        contentDescription = "${listing.title} parking space photo",
        height = if (onDismiss != null) 88.dp else 104.dp,
      )

      // Title + optional dismiss button (for Map bottom preview)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
          ) {
            Icon(
              imageVector = Icons.Outlined.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "${listing.areaLabel}, ${listing.city.displayName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (onDismiss != null) {
          IconButton(
            onClick = onDismiss,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("close_preview_button"),
          ) {
            Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close preview")
          }
        }
      }

      // ~200m Public Location Privacy Indicator
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier =
            Modifier.padding(
              horizontal = ParkSpaceSpacing.xs,
              vertical = ParkSpaceSpacing.xxs,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text =
              "~${listing.maskRadiusMeters}m masked area (${"%.4f".format(listing.publicMaskedLatitude)}, ${"%.4f".format(listing.publicMaskedLongitude)})",
            style = ParkSpaceMonoCodeStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Status Pills: Covered/Open, Capacity, Vehicles
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
      ) {
        ParkSpaceStatusPill(
          label = "${listing.availableBays}/${listing.totalBays} bays open",
          tone = StatusTone.AVAILABLE,
        )
        ParkSpaceStatusPill(
          label = if (listing.isCovered) "Covered" else "Open Air",
          tone = StatusTone.INFO,
        )
        ParkSpaceStatusPill(
          label = listing.supportedVehicles.joinToString(" / ") { it.name },
          tone = StatusTone.PENDING,
          icon = Icons.Outlined.DirectionsCar,
        )
        if (listing.reviewCount > 0 && listing.averageRating != null) {
          ParkSpaceStatusPill(
            label = "${"%.1f".format(listing.averageRating)} (${listing.reviewCount})",
            tone = StatusTone.INFO,
            icon = Icons.Filled.Star,
          )
        }
      }

      // Amenities & Schedule Summary
      if (listing.amenities.isNotEmpty()) {
        Text(
          text = listing.amenities.joinToString(" • ") { it.displayName },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
      ) {
        Icon(
          imageVector = Icons.Outlined.Schedule,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = "${listing.schedule.formattedHours()} (${listing.schedule.openDays.size} days/wk)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Pricing Breakdown (Hourly / Daily / Monthly) + View Details CTA
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${AppEnvironment.CURRENCY_SYMBOL} ${listing.hourlyRatePkr}/hr  •  ${AppEnvironment.CURRENCY_SYMBOL} ${listing.dailyRatePkr}/day",
            style = ParkSpaceMonoPriceStyle,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              if (listing.monthlyEnabled && listing.monthlyRatePkr != null) {
                "Monthly: ${AppEnvironment.CURRENCY_SYMBOL} ${listing.monthlyRatePkr}/mo"
              } else {
                "Monthly plan not offered"
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        ParkSpacePrimaryButton(
          text = stringResource(R.string.discovery_view_details),
          onClick = { onViewDetails(listing.id) },
          modifier = Modifier.testTag("view_details_${listing.id}"),
        )
      }
    }
  }
}
