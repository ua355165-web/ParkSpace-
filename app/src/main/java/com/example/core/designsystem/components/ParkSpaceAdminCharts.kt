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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DonutLarge
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.config.AppEnvironment
import com.example.core.domain.model.PlatformAnalyticsSnapshot
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonDark
import com.example.ui.theme.InfoCobalt
import com.example.ui.theme.InfoCobaltDark
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@Composable
fun AdminModerationHealthChartCard(
  analytics: PlatformAnalyticsSnapshot,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val approvedColor = if (isDarkTheme) Jade400 else Emerald900
  val pendingColor = if (isDarkTheme) Brass400 else Brass600
  val rejectedColor = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson
  val resolvedColor = if (isDarkTheme) InfoCobaltDark else InfoCobalt
  val totalTracked =
    analytics.totalHostsCount +
      analytics.totalListingsCount +
      analytics.openDisputesCount +
      analytics.resolvedDisputesCount

  val accessibilitySummary =
    if (totalTracked == 0) {
      "Marketplace moderation health: no hosts, listings, or disputes recorded yet"
    } else {
      "Moderation health: ${analytics.approvedHostsCount} of ${analytics.totalHostsCount} hosts approved, " +
        "${analytics.approvedListingsCount} of ${analytics.totalListingsCount} listings live, " +
        "${analytics.openDisputesCount} open disputes"
    }

  ParkSpaceGlassSurface(
    modifier =
      modifier
        .testTag("admin_moderation_chart_card")
        .semantics { contentDescription = accessibilitySummary },
    accentGlow = semantic.customerGlow,
    contentPaddingDp = 16,
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
          imageVector = Icons.Outlined.DonutLarge,
          contentDescription = null,
          tint = approvedColor,
          modifier = Modifier.size(19.dp),
        )
        Text(
          text = "Marketplace Moderation Mix",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
      if (totalTracked > 0) {
        Text(
          text = "$totalTracked records",
          style = ParkSpaceMonoCodeStyle,
          color = approvedColor,
        )
      }
    }

    if (totalTracked == 0) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(vertical = ParkSpaceSpacing.sm)
            .testTag("admin_moderation_chart_empty"),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text =
            "No Host KYC records, parking listings, or booking disputes yet. Distribution bars appear automatically as users register.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      val hostSlices =
        listOf(
          approvedColor to analytics.approvedHostsCount,
          pendingColor to analytics.pendingHostsCount,
          rejectedColor to analytics.rejectedHostsCount,
        ).filter { it.second > 0 }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        // Native Compose Canvas Donut for Host Verification Mix
        Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.size(92.dp)) {
            val strokePx = 15f
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            if (hostSlices.isEmpty()) {
              drawArc(
                color = approvedColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
              )
            } else {
              val total = hostSlices.sumOf { it.second }.coerceAtLeast(1)
              val gapDeg = if (hostSlices.size > 1) 4f else 0f
              var startAngle = -90f
              hostSlices.forEach { (color, count) ->
                val rawSweep = (count.toFloat() / total.toFloat()) * 360f
                val sweep = (rawSweep - gapDeg).coerceAtLeast(6f)
                drawArc(
                  color = color,
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
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${analytics.totalHostsCount}",
              style = ParkSpaceMonoPriceStyle.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "Hosts",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ModerationStackedBarRow(
            title = "Hosts (${analytics.approvedHostsCount}/${analytics.totalHostsCount} approved)",
            segments =
              listOf(
                approvedColor to analytics.approvedHostsCount,
                pendingColor to analytics.pendingHostsCount,
                rejectedColor to analytics.rejectedHostsCount,
              ),
          )
          ModerationStackedBarRow(
            title = "Listings (${analytics.approvedListingsCount}/${analytics.totalListingsCount} live)",
            segments =
              listOf(
                approvedColor to analytics.approvedListingsCount,
                pendingColor to analytics.pendingListingsCount,
                rejectedColor to analytics.rejectedListingsCount,
              ),
          )
          ModerationStackedBarRow(
            title =
              "Disputes (${analytics.openDisputesCount} open / ${analytics.resolvedDisputesCount} closed)",
            segments =
              listOf(
                rejectedColor to analytics.openDisputesCount,
                resolvedColor to analytics.resolvedDisputesCount,
              ),
          )
        }
      }
    }
  }
}

@Composable
private fun ModerationStackedBarRow(
  title: String,
  segments: List<Pair<Color, Int>>,
) {
  val total = segments.sumOf { it.second }
  val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
      drawRect(color = trackColor)
      if (total > 0) {
        var offsetX = 0f
        segments.forEach { (color, count) ->
          if (count > 0) {
            val segW = (count.toFloat() / total.toFloat()) * size.width
            drawRect(
              color = color,
              topLeft = Offset(offsetX, 0f),
              size = Size(segW, size.height),
            )
            offsetX += segW
          }
        }
      }
    }
  }
}

@Composable
fun AdminCityActivityChartCard(
  analytics: PlatformAnalyticsSnapshot,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val listingsBarColor = if (isDarkTheme) Jade400 else Emerald900
  val bookingsBarColor = if (isDarkTheme) Brass400 else Brass600
  val hasCityActivity =
    analytics.cityMetrics.any {
      it.totalListingsCount > 0 || it.totalBookingsCount > 0 || it.demoGrossVolumePkr > 0
    }

  ParkSpaceGlassSurface(
    modifier =
      modifier
        .testTag("admin_city_chart_card")
        .semantics {
          contentDescription =
            if (hasCityActivity) {
              "Supported city activity across ${analytics.cityMetrics.size} cities"
            } else {
              "Supported city activity chart: no city listings or bookings yet"
            }
        },
    accentGlow = semantic.hostGlow,
    contentPaddingDp = 16,
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
          imageVector = Icons.Outlined.BarChart,
          contentDescription = null,
          tint = bookingsBarColor,
          modifier = Modifier.size(19.dp),
        )
        Text(
          text = "Supported City Activity",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
      if (hasCityActivity) {
        Text(
          text = "${AppEnvironment.CURRENCY_SYMBOL} ${analytics.totalDemoGrossVolumePkr}",
          style = ParkSpaceMonoCodeStyle,
          color = listingsBarColor,
        )
      }
    }

    if (!hasCityActivity) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(vertical = ParkSpaceSpacing.sm)
            .testTag("admin_city_chart_empty"),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text =
            "All 4 cities currently have 0 listings and 0 bookings. Real supply and booking bars populate automatically.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      val maxMetric =
        analytics.cityMetrics
          .maxOf { maxOf(it.totalListingsCount, it.totalBookingsCount) }
          .coerceAtLeast(1)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        ChartLegendItem(color = listingsBarColor, label = "Listings")
        ChartLegendItem(color = bookingsBarColor, label = "Bookings")
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth().height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
        verticalAlignment = Alignment.Bottom,
      ) {
        analytics.cityMetrics.forEach { metric ->
          val lFraction =
            if (metric.totalListingsCount == 0) 0.06f
            else (metric.totalListingsCount.toFloat() / maxMetric.toFloat()).coerceIn(0.16f, 1f)
          val bFraction =
            if (metric.totalBookingsCount == 0) 0.06f
            else (metric.totalBookingsCount.toFloat() / maxMetric.toFloat()).coerceIn(0.16f, 1f)

          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
          ) {
            Text(
              text = "${metric.totalListingsCount}/${metric.totalBookingsCount}",
              style = ParkSpaceMonoCodeStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.height(56.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.Bottom,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(width = 12.dp, height = (56f * lFraction).dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                      Brush.verticalGradient(
                        listOf(listingsBarColor, listingsBarColor.copy(alpha = 0.45f))
                      )
                    ),
              )
              Box(
                modifier =
                  Modifier
                    .size(width = 12.dp, height = (56f * bFraction).dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                      Brush.verticalGradient(
                        listOf(bookingsBarColor, bookingsBarColor.copy(alpha = 0.45f))
                      )
                    ),
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = metric.city.displayName.take(5),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
