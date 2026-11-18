package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Strict 8dp spacing grid with 4dp half-step for compact badges/chips.
 */
object ParkSpaceSpacing {
  val xxs = 4.dp
  val xs = 8.dp
  val sm = 12.dp
  val md = 16.dp
  val lg = 24.dp
  val xl = 32.dp
  val xxl = 48.dp

  val screenHorizontal = 16.dp
  val sectionGap = 24.dp
  val cardPadding = 16.dp
  val minTouchTarget = 48.dp
  val maxReadableWidth = 600.dp
}

val ParkSpaceShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )
