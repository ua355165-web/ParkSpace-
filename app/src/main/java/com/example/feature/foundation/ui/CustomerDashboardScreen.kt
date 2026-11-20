package com.example.feature.foundation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity
import com.example.core.designsystem.components.AddDemoVehicleDialog
import com.example.core.designsystem.components.CustomerActivityLineChartCard
import com.example.core.designsystem.components.CustomerHelpSupportDialog
import com.example.core.designsystem.components.CustomerPreferencesDialog
import com.example.core.designsystem.components.CustomerPrivacyTermsDialog
import com.example.core.designsystem.components.CustomerSpendingTrendCard
import com.example.core.designsystem.components.CustomerStatusDonutChartCard
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceCustomerHeader
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassChip
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.VehicleType
import com.example.feature.booking.ui.formatDateTimePkt
import com.example.feature.foundation.FoundationDashboardData
import com.example.feature.foundation.domain.CustomerBookingPreviewItem
import com.example.feature.foundation.domain.CustomerDashboardAnalyticsCalculator
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.InfoCobalt
import com.example.ui.theme.InfoCobaltDark
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerDashboardScreen(
  data: FoundationDashboardData,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onOpenDiscovery: () -> Unit,
  onOpenBookings: () -> Unit,
  onOpenBookingDetail: (String) -> Unit,
  onOpenProfile: () -> Unit,
  onAddVehicle: (VehicleType, String, String) -> Unit,
  onUpdatePreferredCity: (SupportedCity) -> Unit,
  onOpenPaymentsAndReferrals: () -> Unit,
  onOpenDisputes: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenHostDashboardOrActivate: () -> Unit,
  onSignOut: () -> Unit,
  sandboxFeedback: String? = null,
  onDismissFeedback: () -> Unit = {},
  bottomBar: @Composable () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val user = data.session.activeUser
  val preferredCity = data.activeCustomerProfile?.preferredCity ?: data.session.selectedCity

  val analytics =
    remember(data.activeUserBookings, data.activeUserTransactions, data.spacesById) {
      CustomerDashboardAnalyticsCalculator.compute(
        bookings = data.activeUserBookings,
        transactions = data.activeUserTransactions,
        spacesById = data.spacesById,
      )
    }

  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()

  var showAddVehicleDialog by rememberSaveable { mutableStateOf(false) }
  var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
  var showHelpDialog by rememberSaveable { mutableStateOf(false) }
  var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }

  val primaryAccent = if (isDarkTheme) Jade400 else Emerald900
  val primaryContent = if (isDarkTheme) Emerald950 else Color.White

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = bottomBar,
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier
              .widthIn(max = ParkSpaceSpacing.maxReadableWidth)
              .fillMaxWidth()
              .verticalScroll(scrollState)
              .padding(
                horizontal = ParkSpaceSpacing.screenHorizontal,
                vertical = ParkSpaceSpacing.sm,
              ),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
        ) {
          // 1. Clean Global Customer Header
          ParkSpaceCustomerHeader(
            cityName = preferredCity.displayName,
            isDarkTheme = isDarkTheme,
            hasCompanionHostAccount = data.hasCompanionHostAccount,
            onToggleTheme = onToggleTheme,
            onOpenCustomerDashboard = {
              coroutineScope.launch { scrollState.animateScrollTo(0) }
            },
            onOpenHostDashboardOrActivate = onOpenHostDashboardOrActivate,
            onOpenProfile = onOpenProfile,
            onOpenSettings = { showSettingsDialog = true },
            onOpenHelpSupport = { showHelpDialog = true },
            onOpenPrivacyTerms = { showPrivacyDialog = true },
            onSignOut = onSignOut,
          )

          if (sandboxFeedback != null) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("customer_feedback_banner"),
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = ParkSpaceSpacing.md, vertical = ParkSpaceSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = sandboxFeedback,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = onDismissFeedback,
                  modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
                ) {
                  Text("OK")
                }
              }
            }
          }

          // 2. Hero Greeting & Sandbox Strip + Quick Utility Shortcuts
          ParkSpaceGlassSurface(
            modifier = Modifier.testTag("customer_shell_card"),
            accentGlow = semantic.customerGlow,
            contentPaddingDp = 18,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = semantic.glassSurface,
                border = BorderStroke(1.dp, semantic.glassBorder),
                modifier = Modifier.testTag("mode_banner"),
              ) {
                Text(
                  text = "Local Demo",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }

              Surface(
                shape = CircleShape,
                color = primaryAccent.copy(alpha = if (isDarkTheme) 0.18f else 0.10f),
                modifier =
                  Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOpenPaymentsAndReferrals)
                    .testTag("customer_wallet_pill"),
              ) {
                Text(
                  text = "Credit: ${AppEnvironment.CURRENCY_SYMBOL} ${data.availableDemoCreditPkr}",
                  style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.SemiBold),
                  color = primaryAccent,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Welcome back, ${user?.fullName ?: "Driver"}",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Live local garage overview for ${preferredCity.displayName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Liquid Glass Action CTA
            ParkSpaceLiquidGlassButton(
              text = "Find Parking",
              onClick = onOpenDiscovery,
              leadingIcon = Icons.Outlined.Search,
              accentColor = primaryAccent,
              contentColor = primaryContent,
              modifier = Modifier.fillMaxWidth().testTag("open_discovery_button"),
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Compact secondary Liquid Glass utility chips (no clutter)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              ParkSpaceLiquidGlassChip(
                label =
                  if (data.unreadNotificationsCount > 0) "Alerts (${data.unreadNotificationsCount})"
                  else "Alerts",
                leadingIcon = Icons.Outlined.Notifications,
                accentColor = primaryAccent,
                onClick = onOpenNotifications,
                modifier = Modifier.testTag("open_customer_notifications_button"),
              )
              ParkSpaceLiquidGlassChip(
                label = "Payments & Referrals",
                leadingIcon = Icons.Outlined.AccountBalanceWallet,
                accentColor = primaryAccent,
                onClick = onOpenPaymentsAndReferrals,
                modifier = Modifier.testTag("open_customer_payments_referrals_button"),
              )
              ParkSpaceLiquidGlassChip(
                label = "Complaints",
                leadingIcon = Icons.Outlined.ReportProblem,
                accentColor = primaryAccent,
                onClick = onOpenDisputes,
                modifier = Modifier.testTag("open_customer_disputes_button"),
              )
            }
          }

          // 3. Compact 2x2 Summary KPI Cards (Strictly Real Room DB Data)
          Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              CustomerKpiGlassCard(
                title = "Upcoming",
                value = "${analytics.summary.upcomingCount}",
                subtitle = "Pending or confirmed",
                icon = Icons.Outlined.Schedule,
                accentColor = if (isDarkTheme) Jade400 else Emerald900,
                glowColor = semantic.customerGlow,
                tag = "kpi_upcoming_bookings",
                modifier = Modifier.weight(1f),
              )
              CustomerKpiGlassCard(
                title = "Active Now",
                value = "${analytics.summary.activeCount}",
                subtitle = "Checked-in bays",
                icon = Icons.Outlined.EventAvailable,
                accentColor = if (isDarkTheme) Jade400 else Emerald700,
                glowColor = semantic.customerGlow,
                tag = "kpi_active_bookings",
                modifier = Modifier.weight(1f),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              CustomerKpiGlassCard(
                title = "Total Bookings",
                value = "${analytics.summary.totalBookings}",
                subtitle = "All account reservations",
                icon = Icons.Outlined.ReceiptLong,
                accentColor = if (isDarkTheme) InfoCobaltDark else InfoCobalt,
                glowColor = semantic.customerGlow,
                tag = "kpi_total_bookings",
                modifier = Modifier.weight(1f),
              )
              CustomerKpiGlassCard(
                title = "Recent Spending",
                value =
                  if (analytics.summary.hasSpendingRecords && analytics.summary.recentSpendingPkr != null) {
                    "${AppEnvironment.CURRENCY_SYMBOL} ${analytics.summary.recentSpendingPkr}"
                  } else {
                    "—"
                  },
                subtitle =
                  if (analytics.summary.hasSpendingRecords) "Simulated Demo ledger"
                  else "No Demo payments yet",
                icon = Icons.Outlined.Payments,
                accentColor = if (isDarkTheme) Brass400 else Brass600,
                glowColor = semantic.hostGlow,
                tag = "kpi_recent_spending",
                modifier = Modifier.weight(1f),
              )
            }
          }

          // 4. Upcoming / Active Booking Spotlight Preview (shown when a live or upcoming reservation exists)
          val spotlight = analytics.spotlightBooking
          if (spotlight != null) {
            SpotlightBookingCard(
              item = spotlight,
              matchedVehicleLabel =
                data.activeUserVehicles.firstOrNull { it.id == spotlight.booking.vehicleId }?.let {
                  "${it.label} • ${it.plateNumberMasked}"
                },
              isDarkTheme = isDarkTheme,
              onOpenDetail = { onOpenBookingDetail(spotlight.booking.id) },
            )
          }

          // 5. Native Compose Canvas Analytics (Real Repository Metrics Only)
          CustomerActivityLineChartCard(
            points = analytics.activityTrend,
            isDarkTheme = isDarkTheme,
          )

          CustomerStatusDonutChartCard(
            slices = analytics.statusDistribution,
            isDarkTheme = isDarkTheme,
          )

          // Shown strictly when real ESCROW_PAYMENT transactions exist
          CustomerSpendingTrendCard(
            points = analytics.spendingTrend,
            netSpendingPkr = analytics.summary.recentSpendingPkr ?: 0,
            isDarkTheme = isDarkTheme,
          )

          // 6. Vehicle Summary Card
          ParkSpaceGlassSurface(
            modifier = Modifier.testTag("customer_vehicles_card"),
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
                  imageVector = Icons.Outlined.DirectionsCar,
                  contentDescription = null,
                  tint = primaryAccent,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "My Vehicles (${data.activeUserVehicles.size})",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
              }
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Add Vehicle",
                leadingIcon = Icons.Outlined.Add,
                accentColor = primaryAccent,
                onClick = { showAddVehicleDialog = true },
                modifier = Modifier.testTag("customer_add_vehicle_button"),
              )
            }

            if (data.activeUserVehicles.isEmpty()) {
              Text(
                text = "No vehicles saved to your account yet. Add a car or bike for faster booking checkout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("customer_empty_vehicles_text"),
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
                data.activeUserVehicles.forEach { vehicle ->
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .testTag("customer_vehicle_row_${vehicle.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = vehicle.label,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                        Text(
                          text = vehicle.vehicleType.displayName,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Text(
                        text = vehicle.plateNumberMasked,
                        style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = primaryAccent,
                      )
                    }
                  }
                }
              }
            }
          }

          // 7. Recent Booking Activity
          ParkSpaceGlassSurface(
            modifier = Modifier.testTag("customer_recent_bookings_card"),
            accentGlow = semantic.customerGlow,
            contentPaddingDp = 18,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              )
              TextButton(
                onClick = onOpenBookings,
                modifier =
                  Modifier
                    .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                    .testTag("open_customer_bookings_button"),
              ) {
                Text("All Bookings")
              }
            }

            if (analytics.recentBookings.isEmpty()) {
              Text(
                text = "No bookings yet. Your recent reservations and payment receipts will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("customer_recent_bookings_empty"),
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
                analytics.recentBookings.forEach { item ->
                  RecentBookingRowCard(
                    item = item,
                    onClick = { onOpenBookingDetail(item.booking.id) },
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))
        }
      }
    }
  }

  if (showAddVehicleDialog) {
    AddDemoVehicleDialog(
      onDismiss = { showAddVehicleDialog = false },
      onSaveVehicle = { type, label, plate ->
        onAddVehicle(type, label, plate)
        showAddVehicleDialog = false
      },
    )
  }

  if (showSettingsDialog) {
    CustomerPreferencesDialog(
      preferredCity = preferredCity,
      isDarkTheme = isDarkTheme,
      onToggleTheme = onToggleTheme,
      onSelectCity = onUpdatePreferredCity,
      onOpenProfile = onOpenProfile,
      onDismiss = { showSettingsDialog = false },
    )
  }

  if (showHelpDialog) {
    CustomerHelpSupportDialog(
      onOpenDisputes = onOpenDisputes,
      onDismiss = { showHelpDialog = false },
    )
  }

  if (showPrivacyDialog) {
    CustomerPrivacyTermsDialog(
      onDismiss = { showPrivacyDialog = false },
    )
  }
}

@Composable
private fun CustomerKpiGlassCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  glowColor: Color,
  tag: String,
  modifier: Modifier = Modifier,
) {
  ParkSpaceGlassSurface(
    modifier = modifier.testTag(tag),
    accentGlow = glowColor,
    cornerRadiusDp = 20,
    contentPaddingDp = 14,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(18.dp),
      )
    }
    Text(
      text = value,
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      color = accentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SpotlightBookingCard(
  item: CustomerBookingPreviewItem,
  matchedVehicleLabel: String?,
  isDarkTheme: Boolean,
  onOpenDetail: () -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val booking = item.booking
  val statusTone =
    when (booking.status) {
      BookingStatus.CONFIRMED,
      BookingStatus.CHECKED_IN -> StatusTone.AVAILABLE
      BookingStatus.PENDING_PAYMENT -> StatusTone.PENDING
      else -> StatusTone.INFO
    }
  val accentColor = if (isDarkTheme) Jade400 else Emerald900
  val contentColor = if (isDarkTheme) Emerald950 else Color.White

  ParkSpaceGlassSurface(
    modifier = Modifier.testTag("customer_upcoming_preview_card"),
    accentGlow = semantic.customerGlow,
    borderColor = accentColor.copy(alpha = 0.45f),
    contentPaddingDp = 18,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ParkSpaceStatusPill(
        label = booking.status.displayName,
        tone = statusTone,
      )
      Text(
        text = booking.bookingCode,
        style = ParkSpaceMonoCodeStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Text(
      text = item.spaceTitle,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.LocationOn,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = "${item.areaLabel} • ${booking.city.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Text(
      text = "${formatDateTimePkt(booking.startEpochMs)} -> ${formatDateTimePkt(booking.endEpochMs)}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = matchedVehicleLabel ?: booking.bookingType.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "${AppEnvironment.CURRENCY_SYMBOL} ${booking.totalAmountPkr}",
        style = ParkSpaceMonoPriceStyle.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
      )
    }

    Spacer(modifier = Modifier.height(2.dp))
    ParkSpaceLiquidGlassButton(
      text = if (item.hasLiveGatePass) "View Gate Pass" else "View Booking",
      onClick = onOpenDetail,
      leadingIcon = if (item.hasLiveGatePass) Icons.Outlined.Key else Icons.Outlined.ReceiptLong,
      accentColor = accentColor,
      contentColor = contentColor,
      modifier = Modifier.fillMaxWidth().testTag("spotlight_booking_action_button"),
    )
  }
}

@Composable
private fun RecentBookingRowCard(
  item: CustomerBookingPreviewItem,
  onClick: () -> Unit,
) {
  val booking = item.booking
  val statusTone =
    when (booking.status) {
      BookingStatus.CONFIRMED,
      BookingStatus.CHECKED_IN,
      BookingStatus.CHECKED_OUT,
      BookingStatus.COMPLETED -> StatusTone.AVAILABLE
      BookingStatus.PENDING_PAYMENT -> StatusTone.PENDING
      BookingStatus.CANCELLED,
      BookingStatus.EXPIRED,
      BookingStatus.DISPUTED -> StatusTone.ERROR
      BookingStatus.DRAFT -> StatusTone.INFO
    }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag("recent_booking_row_${booking.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = item.spaceTitle,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${booking.bookingCode} • ${formatDateTimePkt(booking.startEpochMs)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        ParkSpaceStatusPill(label = booking.status.displayName, tone = statusTone)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "${AppEnvironment.CURRENCY_SYMBOL} ${booking.totalAmountPkr}",
            style = ParkSpaceMonoCodeStyle.copy(fontWeight = FontWeight.SemiBold),
          )
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Open booking",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
