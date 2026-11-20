package com.example.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.config.AppEnvironment
import com.example.feature.foundation.domain.BookingActivityPoint
import com.example.feature.foundation.domain.BookingStatusSlice
import com.example.feature.foundation.domain.CustomerSpendingPoint
import com.example.feature.foundation.domain.CustomerStatusBucket
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.InfoCobalt
import com.example.ui.theme.InfoCobaltDark
import com.example.ui.theme.Jade400
import com.example.ui.theme.Jade500
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@Composable
fun CustomerActivityLineChartCard(
  points: List<BookingActivityPoint>,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val lineColor = if (isDarkTheme) Jade400 else Emerald900
  val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  val totalCount = points.sumOf { it.count }

  val chartDescription =
    if (points.isEmpty()) {
      "Booking activity trend chart: no booking activity recorded yet"
    } else {
      "Booking activity trend across ${points.size} days with $totalCount bookings"
    }

  ParkSpaceGlassSurface(
    modifier =
      modifier
        .testTag("customer_activity_chart_card")
        .semantics { contentDescription = chartDescription },
    accentGlow = semantic.customerGlow,
    contentPaddingDp = 18,
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
        Icon(
          imageVector = Icons.Outlined.ShowChart,
          contentDescription = null,
          tint = lineColor,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = "Booking Activity Trend",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }

      if (points.isNotEmpty()) {
        Text(
          text = "$totalCount total",
          style = ParkSpaceMonoCodeStyle,
          color = lineColor,
        )
      }
    }

    if (points.isEmpty()) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(110.dp)
            .testTag("customer_activity_chart_empty"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No booking activity yet. Your real reservation timeline will appear here.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      val maxVal = (points.maxOf { it.count }).coerceAtLeast(1)
      Spacer(modifier = Modifier.height(4.dp))
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(132.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp),
      ) {
        val w = size.width
        val h = size.height
        val padH = 14f
        val usableW = (w - padH * 2f).coerceAtLeast(1f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        // Horizontal dashed reference lines (0, mid, max)
        for (i in 0..2) {
          val y = h * (i / 2f)
          drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.5f,
            pathEffect = dashEffect,
          )
        }

        val coords =
          points.mapIndexed { idx, point ->
            val x =
              if (points.size == 1) w / 2f
              else padH + usableW * (idx.toFloat() / (points.size - 1).toFloat())
            val ratio = (point.count.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
            val y = h - (ratio * (h * 0.82f) + h * 0.08f)
            Offset(x, y)
          }

        if (coords.size >= 2) {
          val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
              val prev = coords[i - 1]
              val curr = coords[i]
              val ctrlX = (prev.x + curr.x) / 2f
              cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
            }
          }

          val fillPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, h)
            lineTo(coords.first().x, h)
            close()
          }

          drawPath(
            path = fillPath,
            brush =
              Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
              ),
          )

          drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round),
          )
        }

        coords.forEach { pt ->
          drawCircle(
            color = lineColor.copy(alpha = 0.24f),
            radius = 12f,
            center = pt,
          )
          drawCircle(
            color = lineColor,
            radius = 6f,
            center = pt,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement =
          if (points.size == 1) Arrangement.Center else Arrangement.SpaceBetween,
      ) {
        points.forEach { pt ->
          Text(
            text = "${pt.dayLabel} (${pt.count})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun CustomerStatusDonutChartCard(
  slices: List<BookingStatusSlice>,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val total = slices.sumOf { it.count }
  val ringTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

  fun colorForBucket(bucket: CustomerStatusBucket): Color =
    when (bucket) {
      CustomerStatusBucket.UPCOMING -> if (isDarkTheme) Jade400 else Emerald900
      CustomerStatusBucket.ACTIVE -> if (isDarkTheme) Jade500 else Emerald700
      CustomerStatusBucket.COMPLETED -> if (isDarkTheme) InfoCobaltDark else InfoCobalt
      CustomerStatusBucket.CLOSED -> if (isDarkTheme) Brass400 else Brass600
    }

  val description =
    if (slices.isEmpty()) {
      "Booking status distribution chart: no bookings yet"
    } else {
      "Booking status distribution: " + slices.joinToString { "${it.bucket.label} ${it.count}" }
    }

  ParkSpaceGlassSurface(
    modifier =
      modifier
        .testTag("customer_status_donut_card")
        .semantics { contentDescription = description },
    accentGlow = semantic.customerGlow,
    contentPaddingDp = 18,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Icon(
        imageVector = Icons.Outlined.DonutLarge,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = "Booking Status Mix",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      )
    }

    if (slices.isEmpty() || total == 0) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(vertical = ParkSpaceSpacing.sm)
            .testTag("customer_status_donut_empty"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.size(84.dp)) {
            drawArc(
              color = ringTrackColor,
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = 16f, cap = StrokeCap.Round),
            )
          }
          Text(
            text = "0",
            style = ParkSpaceMonoPriceStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "No reservations yet. Status proportions update live once you create a booking.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ParkSpaceSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.lg),
      ) {
        Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.size(104.dp)) {
            val strokePx = 18f
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val gapDeg = if (slices.size > 1) 4f else 0f
            var startAngle = -90f

            slices.forEach { slice ->
              val rawSweep = (slice.count.toFloat() / total.toFloat()) * 360f
              val sweep = (rawSweep - gapDeg).coerceAtLeast(6f)
              drawArc(
                color = colorForBucket(slice.bucket),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
              )
              startAngle += rawSweep
            }
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$total",
              style = ParkSpaceMonoPriceStyle.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "Total",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          slices.forEach { slice ->
            val sliceColor = colorForBucket(slice.bucket)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(sliceColor),
                )
                Text(
                  text = slice.bucket.label,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = "${slice.count}",
                style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                color = sliceColor,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CustomerSpendingTrendCard(
  points: List<CustomerSpendingPoint>,
  netSpendingPkr: Int,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  if (points.isEmpty()) return

  val semantic = ParkSpaceThemeTokens.semantic
  val barColor = if (isDarkTheme) Brass400 else Brass600
  val maxAmount = points.maxOf { it.amountPkr }.coerceAtLeast(1)

  ParkSpaceGlassSurface(
    modifier =
      modifier
        .testTag("customer_spending_chart_card")
        .semantics {
          contentDescription =
            "Simulated Demo spending trend totaling ${AppEnvironment.CURRENCY_SYMBOL} $netSpendingPkr"
        },
    accentGlow = semantic.hostGlow,
    contentPaddingDp = 18,
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
        Icon(
          imageVector = Icons.Outlined.Payments,
          contentDescription = null,
          tint = barColor,
          modifier = Modifier.size(20.dp),
        )
        Column {
          Text(
            text = "Demo Spending Trend",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            text = "Simulated local payment ledger",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = "${AppEnvironment.CURRENCY_SYMBOL} $netSpendingPkr",
        style = ParkSpaceMonoPriceStyle.copy(fontWeight = FontWeight.Bold),
        color = barColor,
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth().height(92.dp),
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      verticalAlignment = Alignment.Bottom,
    ) {
      points.forEach { pt ->
        val fraction = (pt.amountPkr.toFloat() / maxAmount.toFloat()).coerceIn(0.14f, 1f)
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom,
        ) {
          Text(
            text = "${pt.amountPkr}",
            style = ParkSpaceMonoCodeStyle,
            color = barColor,
            maxLines = 1,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier =
              Modifier
                .fillMaxWidth(0.55f)
                .height((50f * fraction).dp)
                .clip(CircleShape)
                .background(
                  Brush.verticalGradient(
                    colors = listOf(barColor, barColor.copy(alpha = 0.45f))
                  )
                ),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = pt.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
