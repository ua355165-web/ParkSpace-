package com.example.feature.discovery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.domain.model.ParkingType
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing

/**
 * Renders the Host's uploaded parking photo when present, or falls back to
 * the architectural placeholder banner.
 */
@Composable
fun ParkingSpaceMediaBanner(
  photoUris: List<String>,
  parkingType: ParkingType,
  isCovered: Boolean,
  contentDescription: String,
  modifier: Modifier = Modifier,
  height: Dp = 104.dp,
) {
  val primaryUri = photoUris.firstOrNull()
  if (primaryUri.isNullOrBlank()) {
    ParkingVisualPlaceholder(
      parkingType = parkingType,
      isCovered = isCovered,
      modifier = modifier,
      height = height,
    )
  } else {
    Box(
      modifier =
        modifier
          .fillMaxWidth()
          .height(height)
          .clip(MaterialTheme.shapes.small)
          .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
      AsyncImage(
        model = primaryUri,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.align(Alignment.BottomEnd).padding(ParkSpaceSpacing.xs),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs, vertical = ParkSpaceSpacing.xxs),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
        ) {
          Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = "${photoUris.size} Photo${if (photoUris.size > 1) "s" else ""}",
            style = ParkSpaceMonoCodeStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

/**
 * Polished architectural parking banner placeholder used when no host photos are uploaded.
 */
@Composable
fun ParkingVisualPlaceholder(
  parkingType: ParkingType,
  isCovered: Boolean,
  modifier: Modifier = Modifier,
  height: Dp = 104.dp,
) {
  val primary = MaterialTheme.colorScheme.primary
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val secondary = MaterialTheme.colorScheme.secondary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

  val icon =
    when (parkingType) {
      ParkingType.COVERED_GARAGE,
      ParkingType.PRIVATE_DRIVEWAY -> Icons.Outlined.Garage
      ParkingType.COMMERCIAL_PLAZA -> Icons.Outlined.Apartment
      ParkingType.GATED_COMPOUND -> Icons.Outlined.Security
      ParkingType.OPEN_LOT -> Icons.Outlined.LocalParking
    }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .clip(MaterialTheme.shapes.small)
        .background(
          Brush.linearGradient(
            colors = listOf(primaryContainer, surfaceVariant),
          )
        ),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val laneColor = primary.copy(alpha = 0.14f)
      val bayWidth = size.width / 5f
      for (i in 1..4) {
        val x = bayWidth * i
        drawLine(
          color = laneColor,
          start = Offset(x, 12f),
          end = Offset(x, size.height - 12f),
          strokeWidth = 3f,
        )
      }
      drawCircle(
        color = secondary.copy(alpha = 0.16f),
        radius = size.height * 0.65f,
        center = Offset(size.width * 0.88f, size.height * 0.25f),
      )
    }

    Row(
      modifier =
        Modifier.align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(ParkSpaceSpacing.sm),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.extraSmall,
      ) {
        Row(
          modifier =
            Modifier.padding(
              horizontal = ParkSpaceSpacing.xs,
              vertical = ParkSpaceSpacing.xxs,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = parkingType.displayName,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = parkingType.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.extraSmall,
      ) {
        Row(
          modifier =
            Modifier.padding(
              horizontal = ParkSpaceSpacing.xs,
              vertical = ParkSpaceSpacing.xxs,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
        ) {
          if (!isCovered) {
            Icon(
              imageVector = Icons.Outlined.WbSunny,
              contentDescription = null,
              modifier = Modifier.size(13.dp),
            )
          }
          Text(
            text = if (isCovered) "COVERED • DEMO" else "OPEN LOT • DEMO",
            style = ParkSpaceMonoCodeStyle,
          )
        }
      }
    }
  }
}
