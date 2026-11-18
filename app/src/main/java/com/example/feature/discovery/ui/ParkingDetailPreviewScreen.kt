package com.example.feature.discovery.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.ParkingReview
import com.example.feature.discovery.domain.model.ParkingPublicListing
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParkingDetailPreviewScreen(
  listing: ParkingPublicListing?,
  onBack: () -> Unit,
  onBookNow: ((String) -> Unit)? = null,
  reviews: List<ParkingReview> = emptyList(),
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Parking Preview", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
              horizontal = ParkSpaceSpacing.screenHorizontal,
              vertical = ParkSpaceSpacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        ParkSpaceModeBanner(mode = AppEnvironment.currentMode)

        if (listing == null) {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("detail_missing_card"),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.lg),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Text("Listing Unavailable", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "This parking listing could not be loaded or is currently unavailable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          ParkingSpaceMediaBanner(
            photoUris = listing.photoUris,
            parkingType = listing.parkingType,
            isCovered = listing.isCovered,
            contentDescription = "${listing.title} main photo",
            height = 160.dp,
          )

          if (listing.photoUris.size > 1) {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              modifier = Modifier.fillMaxWidth().testTag("detail_photo_strip"),
            ) {
              items(listing.photoUris) { uri ->
                AsyncImage(
                  model = uri,
                  contentDescription = "Parking facility photo",
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier.size(width = 112.dp, height = 76.dp).clip(MaterialTheme.shapes.small),
                )
              }
            }
          }

          Surface(
            modifier = Modifier.fillMaxWidth().testTag("detail_overview_card"),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                ParkSpaceStatusPill(
                  label = "${listing.availableBays}/${listing.totalBays} bays open",
                  tone = StatusTone.AVAILABLE,
                )
                ParkSpaceStatusPill(
                  label = if (listing.isCovered) "Covered" else "Open Air",
                  tone = StatusTone.INFO,
                )
              }

              Text(text = listing.title, style = MaterialTheme.typography.headlineSmall)
              Text(
                text = "${listing.areaLabel}, ${listing.city.displayName} (${listing.city.provinceLabel})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(text = listing.description, style = MaterialTheme.typography.bodyMedium)
            }
          }

          // Public Masked Location Policy Notice
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("detail_privacy_card"),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Shield,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = "Location Privacy (~200m Mask)", style = MaterialTheme.typography.titleMedium)
              }
              Text(
                text = stringResource(R.string.discovery_privacy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text =
                  "Public Zone Center: ${"%.4f".format(listing.publicMaskedLatitude)}, ${"%.4f".format(listing.publicMaskedLongitude)} (Radius ${listing.maskRadiusMeters}m)",
                style = ParkSpaceMonoCodeStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          // Pricing & Schedule
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Text(text = "Rates & Schedule", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "Hourly: ${AppEnvironment.CURRENCY_SYMBOL} ${listing.hourlyRatePkr}",
                style = ParkSpaceMonoPriceStyle,
              )
              Text(
                text = "Daily: ${AppEnvironment.CURRENCY_SYMBOL} ${listing.dailyRatePkr}",
                style = ParkSpaceMonoPriceStyle,
              )
              Text(
                text =
                  if (listing.monthlyEnabled && listing.monthlyRatePkr != null) {
                    "Monthly (30 days): ${AppEnvironment.CURRENCY_SYMBOL} ${listing.monthlyRatePkr}"
                  } else {
                    "Monthly: Not available for this space"
                  },
                style = MaterialTheme.typography.bodyMedium,
              )

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Schedule,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text =
                    "${listing.schedule.formattedHours()} • ${listing.schedule.openDays.joinToString(", ") { it.shortLabel }}",
                  style = MaterialTheme.typography.bodySmall,
                )
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
              ) {
                listing.supportedVehicles.forEach { vehicle ->
                  ParkSpaceStatusPill(label = vehicle.displayName, tone = StatusTone.INFO)
                }
                listing.amenities.forEach { amenity ->
                  ParkSpaceStatusPill(label = amenity.displayName, tone = StatusTone.PENDING)
                }
              }
            }
          }

          // Demo Checkout Privacy & Escrow Notice + Booking CTA
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("detail_booking_milestone_notice"),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text(
                  text = "Demo Booking & Gate Unlock Policy",
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text =
                    "Exact entrance coordinates and simulated Demo PIN unlock only after Demo booking confirmation.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Verified Booking Reviews Section
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("detail_reviews_card"),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  Icon(Icons.Outlined.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Text("Verified Customer Reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (listing.averageRating != null && listing.reviewCount > 0) {
                  ParkSpaceStatusPill(
                    label = "${"%.1f".format(listing.averageRating)} / 5",
                    tone = StatusTone.AVAILABLE,
                    icon = Icons.Filled.Star,
                  )
                }
              }

              if (reviews.isEmpty()) {
                Text(
                  text = stringResource(R.string.empty_reviews),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag("detail_reviews_empty_text"),
                )
              } else {
                reviews.forEach { review ->
                  Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().testTag("detail_review_item_${review.id}"),
                  ) {
                    Column(
                      modifier = Modifier.padding(ParkSpaceSpacing.sm),
                      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Text(review.customerDisplayName, style = MaterialTheme.typography.labelLarge)
                        Text("${review.rating}/5 ★", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                      }
                      Text(review.comment, style = MaterialTheme.typography.bodySmall)
                      if (review.hostReply != null) {
                        Text(
                          text = "Host Reply: ${review.hostReply}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          if (onBookNow != null) {
            ParkSpacePrimaryButton(
              text = "Book This Space (Demo)",
              onClick = { onBookNow(listing.id) },
              modifier = Modifier.fillMaxWidth().testTag("detail_book_now_button"),
            )
          }
        }
      }
    }
  }
}
