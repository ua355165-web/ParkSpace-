package com.example.feature.booking.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.config.AppEnvironment
import com.example.core.config.BookingType
import com.example.core.domain.model.Booking
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import java.text.NumberFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PKT_DATE_TIME_FORMATTER: DateTimeFormatter =
  DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)

private val PKT_SHORT_TIME_FORMATTER: DateTimeFormatter =
  DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

fun formatPkr(amount: Int): String {
  val formatted = NumberFormat.getIntegerInstance(Locale.US).format(amount)
  return "${AppEnvironment.CURRENCY_SYMBOL} $formatted"
}

fun formatDateTimePkt(epochMs: Long): String {
  val zdt = Instant.ofEpochMilli(epochMs).atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
  return "${zdt.format(PKT_DATE_TIME_FORMATTER)} PKT"
}

fun formatBookingWindowPkt(startEpochMs: Long, endEpochMs: Long): String {
  val start = Instant.ofEpochMilli(startEpochMs).atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
  val end = Instant.ofEpochMilli(endEpochMs).atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
  return if (start.toLocalDate() == end.toLocalDate()) {
    "${start.format(PKT_DATE_TIME_FORMATTER)} – ${end.format(PKT_SHORT_TIME_FORMATTER)} PKT"
  } else {
    "${start.format(PKT_DATE_TIME_FORMATTER)} → ${end.format(PKT_DATE_TIME_FORMATTER)} PKT"
  }
}

fun formatDurationSummary(booking: Booking): String {
  val hours = ((booking.endEpochMs - booking.startEpochMs) / Booking.MILLIS_PER_HOUR).coerceAtLeast(1L)
  return when (booking.bookingType) {
    BookingType.HOURLY -> if (hours == 1L) "1 hour" else "$hours hours"
    BookingType.DAILY -> {
      val days = (hours / 24L).coerceAtLeast(1L)
      if (days == 1L) "1 day (24 hrs)" else "$days days"
    }
    BookingType.MONTHLY -> {
      val cycles = (hours / (30L * 24L)).coerceAtLeast(1L)
      if (cycles == 1L) "1 × 30-day cycle" else "$cycles × 30-day cycles"
    }
  }
}

@Composable
fun SimulatedDemoQrPass(
  payload: String,
  bookingCode: String,
  modifier: Modifier = Modifier,
) {
  val gridSize = 15
  val matrix = remember(payload) { buildDeterministicQrMatrix(payload, gridSize) }
  val moduleColor = MaterialTheme.colorScheme.onSurface
  val quietZoneColor = MaterialTheme.colorScheme.surface

  Surface(
    modifier = modifier.testTag("simulated_demo_qr_pass"),
    shape = MaterialTheme.shapes.small,
    color = quietZoneColor,
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.sm),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Canvas(
        modifier =
          Modifier.size(132.dp)
            .background(quietZoneColor)
            .padding(6.dp),
      ) {
        val cell = size.minDimension / gridSize.toFloat()
        for (row in 0 until gridSize) {
          for (col in 0 until gridSize) {
            if (matrix[row][col]) {
              drawRect(
                color = moduleColor,
                topLeft = Offset(col * cell, row * cell),
                size = Size(cell, cell),
              )
            }
          }
        }
      }
      Text(
        text = "SIMULATED DEMO QR • $bookingCode",
        style = ParkSpaceMonoCodeStyle,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Local Demo pass only — not a live physical barrier credential.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

private fun buildDeterministicQrMatrix(payload: String, size: Int): Array<BooleanArray> {
  val grid = Array(size) { BooleanArray(size) }

  fun stampFinder(topRow: Int, leftCol: Int) {
    for (r in 0..4) {
      for (c in 0..4) {
        val isBorder = r == 0 || r == 4 || c == 0 || c == 4
        val isCenter = r == 2 && c == 2
        grid[topRow + r][leftCol + c] = isBorder || isCenter
      }
    }
  }

  stampFinder(0, 0)
  stampFinder(0, size - 5)
  stampFinder(size - 5, 0)

  var seed = payload.hashCode().toLong() and 0xFFFFFFFFL
  for (r in 0 until size) {
    for (c in 0 until size) {
      val inTopLeft = r < 6 && c < 6
      val inTopRight = r < 6 && c >= size - 6
      val inBottomLeft = r >= size - 6 && c < 6
      if (!inTopLeft && !inTopRight && !inBottomLeft) {
        seed = (seed * 1664525L + 1013904223L) and 0xFFFFFFFFL
        grid[r][c] = ((seed ushr 16) and 1L) == 1L
      }
    }
  }
  return grid
}
