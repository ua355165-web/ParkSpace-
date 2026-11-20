package com.example.feature.booking.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.UiState
import com.example.core.config.BookingType
import com.example.core.designsystem.components.AddDemoVehicleDialog
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceCompactDemoPill
import com.example.core.designsystem.components.ParkSpaceGlassIconButton
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSwitch
import com.example.core.designsystem.components.ParkSpaceStateContainer
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.VehicleType
import com.example.feature.booking.BookingCheckoutUiData
import com.example.feature.marketplace.ui.DemoPaymentMethodSelector
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingCheckoutScreen(
  uiState: UiState<BookingCheckoutUiData>,
  onBack: () -> Unit,
  onSelectVehicle: (String) -> Unit,
  onSelectBookingType: (BookingType) -> Unit,
  onSelectDayOffset: (Int) -> Unit,
  onAdjustStartHour: (Int) -> Unit,
  onToggleStartMinute: () -> Unit,
  onAdjustUnits: (Int) -> Unit,
  onSetUnits: (Int) -> Unit,
  onToggleImmediateDemoPayment: (Boolean) -> Unit,
  onConfirmBooking: () -> Unit,
  onAddVehicle: (VehicleType, String, String) -> Unit = { _, _, _ -> },
  onSelectPaymentMethod: (DemoPaymentMethod) -> Unit = {},
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = false,
) {
  var showAddVehicleDialog by rememberSaveable { mutableStateOf(false) }
  val semantic = ParkSpaceThemeTokens.semantic

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.booking_checkout_title),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "checkout_back_button",
              modifier = Modifier.padding(start = ParkSpaceSpacing.xs),
            )
          },
          actions = {
            ParkSpaceCompactDemoPill(
              modifier = Modifier.padding(end = ParkSpaceSpacing.xs).testTag("mode_banner"),
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
            ),
        )
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        ParkSpaceStateContainer(
          state = uiState,
          modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize(),
        ) { data ->
          Column(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                  horizontal = ParkSpaceSpacing.screenHorizontal,
                  vertical = ParkSpaceSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
          ) {
            // 1. Privacy-Safe Space Summary
            ParkSpaceGlassSurface(
              modifier = Modifier.testTag("checkout_space_summary_card"),
              accentGlow = semantic.customerGlow,
              contentPaddingDp = 18,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ParkSpaceStatusPill(
                  label = "${data.publicSpace.availableBays}/${data.publicSpace.totalBays} bays open",
                  tone = StatusTone.AVAILABLE,
                )
                ParkSpaceStatusPill(
                  label = data.publicSpace.schedule.formattedHours(),
                  tone = StatusTone.INFO,
                )
              }
              Text(
                text = data.publicSpace.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
              Text(
                text =
                  "${data.maskedLocation.areaLabel}, ${data.maskedLocation.city.displayName} • ~${data.maskedLocation.maskRadiusMeters}m masked until confirmed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            // 2. Vehicle Selection
            ParkSpaceGlassSurface(
              modifier = Modifier.testTag("checkout_vehicle_card"),
              accentGlow = semantic.customerGlow,
              contentPaddingDp = 18,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Text("Select Demo Vehicle", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                  onClick = { showAddVehicleDialog = true },
                  modifier =
                    Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("checkout_add_vehicle_button"),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.size(4.dp))
                  Text("Add")
                }
              }
              if (data.vehicles.isEmpty()) {
                Text(
                  text = "No vehicles saved on this Customer Demo account yet. Add your own vehicle to book.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.error,
                )
                ParkSpaceLiquidGlassSecondaryButton(
                  text = "Add My Vehicle to Book",
                  leadingIcon = Icons.Outlined.Add,
                  onClick = { showAddVehicleDialog = true },
                  modifier = Modifier.fillMaxWidth().testTag("checkout_empty_add_vehicle_button"),
                )
              } else {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  data.vehicles.forEach { vehicle ->
                    val compatible = vehicle.vehicleType in data.publicSpace.supportedVehicles
                    FilterChip(
                      selected = data.selectedVehicle?.id == vehicle.id,
                      onClick = { onSelectVehicle(vehicle.id) },
                      label = {
                        Text(
                          text =
                            "${vehicle.label} (${vehicle.vehicleType.displayName}) • ${vehicle.plateNumberMasked}" +
                              if (!compatible) " [Unsupported]" else "",
                        )
                      },
                      shape = CircleShape,
                      modifier =
                        Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                          .testTag("checkout_vehicle_${vehicle.id}"),
                      colors =
                        FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                  }
                }
              }
            }

            // 3. Booking Type + Date / Time / Duration Controls
            ParkSpaceGlassSurface(
              modifier = Modifier.testTag("checkout_schedule_card"),
              accentGlow = semantic.customerGlow,
              contentPaddingDp = 18,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Schedule,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text("Plan, Start Time & Duration", style = MaterialTheme.typography.titleMedium)
              }

              SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().testTag("checkout_booking_type_selector"),
              ) {
                BookingType.entries.forEachIndexed { index, type ->
                  val enabled = type != BookingType.MONTHLY || data.publicSpace.monthlyEnabled
                  SegmentedButton(
                    selected = data.form.bookingType == type,
                    onClick = { if (enabled) onSelectBookingType(type) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, BookingType.entries.size),
                    modifier =
                      Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("booking_type_${type.name.lowercase()}"),
                  ) {
                    Text(type.displayName)
                  }
                }
              }

              if (!data.publicSpace.monthlyEnabled) {
                Text(
                  text = "Monthly 30-day cycle bookings are disabled for this space by its host.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              // Start Date Offsets
              Text("Start Date", style = MaterialTheme.typography.labelLarge)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                listOf(
                  0 to "Today",
                  1 to "Tomorrow",
                  2 to "+2 Days",
                  7 to "+7 Days",
                ).forEach { (offset, label) ->
                  FilterChip(
                    selected = data.form.dayOffset == offset,
                    onClick = { onSelectDayOffset(offset) },
                    label = { Text(label) },
                    modifier =
                      Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("day_offset_$offset"),
                  )
                }
              }

              // Start Time Stepper
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text("Start Time (PKT)", style = MaterialTheme.typography.labelLarge)
                  Text(
                    text =
                      "%02d:%02d (${data.selectedDateLabel})".format(
                        data.form.startHour,
                        data.form.startMinute,
                      ),
                    style = ParkSpaceMonoCodeStyle,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Row(
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  IconButton(
                    onClick = { onAdjustStartHour(-1) },
                    modifier =
                      Modifier.size(ParkSpaceSpacing.minTouchTarget)
                        .testTag("start_hour_minus"),
                  ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Earlier hour")
                  }
                  IconButton(
                    onClick = { onAdjustStartHour(1) },
                    modifier =
                      Modifier.size(ParkSpaceSpacing.minTouchTarget)
                        .testTag("start_hour_plus"),
                  ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Later hour")
                  }
                  OutlinedButton(
                    onClick = onToggleStartMinute,
                    modifier =
                      Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("toggle_half_hour_button"),
                  ) {
                    Text(if (data.form.startMinute == 0) ":00" else ":30")
                  }
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

              // Duration Stepper + Presets
              val durationTitle =
                when (data.form.bookingType) {
                  BookingType.HOURLY -> "Duration (1–23 hours)"
                  BookingType.DAILY -> "Duration (1–29 days)"
                  BookingType.MONTHLY -> "Duration (1–12 × 30-day cycles)"
                }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(durationTitle, style = MaterialTheme.typography.labelLarge)
                  Text(
                    text = data.pricing?.unitSummary ?: "${data.form.units} unit(s)",
                    style = ParkSpaceMonoCodeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("checkout_duration_label"),
                  )
                }
                Row(
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  IconButton(
                    onClick = { onAdjustUnits(-1) },
                    modifier =
                      Modifier.size(ParkSpaceSpacing.minTouchTarget)
                        .testTag("duration_minus_button"),
                  ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease duration")
                  }
                  Text(
                    text = "${data.form.units}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs),
                  )
                  IconButton(
                    onClick = { onAdjustUnits(1) },
                    modifier =
                      Modifier.size(ParkSpaceSpacing.minTouchTarget)
                        .testTag("duration_plus_button"),
                  ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase duration")
                  }
                }
              }

              FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
                val presets =
                  when (data.form.bookingType) {
                    BookingType.HOURLY -> listOf(1, 2, 4, 8)
                    BookingType.DAILY -> listOf(1, 3, 7, 14)
                    BookingType.MONTHLY -> listOf(1, 3, 6)
                  }
                presets.forEach { preset ->
                  FilterChip(
                    selected = data.form.units == preset,
                    onClick = { onSetUnits(preset) },
                    label = { Text("$preset") },
                    modifier =
                      Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("duration_preset_$preset"),
                  )
                }
              }

              Text(
                text = "Window: ${formatBookingWindowPkt(data.startEpochMs, data.endEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            // 4. Live Slot Availability Banner
            val avail = data.availability
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("checkout_availability_card"),
              shape = RoundedCornerShape(18.dp),
            color =
              if (avail?.isAvailable == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
              else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
            border =
              BorderStroke(
                width = 1.dp,
                color =
                  if (avail?.isAvailable == true) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.error,
              ),
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              Icon(
                imageVector =
                  if (avail?.isAvailable == true) Icons.Outlined.CheckCircle
                  else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint =
                  if (avail?.isAvailable == true) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.error,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    if (avail?.isAvailable == true) {
                      "Slot Available (${avail.remainingBaysForWindow}/${avail.effectiveCapacity} bays free)"
                    } else {
                      "Slot Unavailable"
                    },
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text =
                    avail?.blockingReason
                      ?: "Half-open interval check passed. Reserved bay ID will be assigned on confirmation.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // 5. Price Breakdown & Demo Escrow Option
          ParkSpaceGlassSurface(
            modifier = Modifier.testTag("checkout_price_breakdown_card"),
            accentGlow = semantic.customerGlow,
            contentPaddingDp = 18,
          ) {
            Text(
              text = "PKR Price Breakdown (Demo)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
              val pricing = data.pricing
              if (pricing != null) {
                PriceRow(
                  label = "${pricing.unitSummary} × ${formatPkr(pricing.unitRatePkr)}",
                  value = formatPkr(pricing.baseAmountPkr),
                )
                PriceRow(
                  label = pricing.feePolicySummary,
                  value = formatPkr(pricing.serviceFeePkr),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Total Payable (Demo)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = formatPkr(pricing.totalAmountPkr),
                    style = ParkSpaceMonoPriceStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("checkout_total_pkr_text"),
                  )
                }
              } else if (data.pricingError != null) {
                Text(
                  text = data.pricingError,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Simulate Demo Payment Immediately",
                    style = MaterialTheme.typography.labelLarge,
                  )
                  Text(
                    text =
                      if (data.form.confirmDemoPaymentImmediately) {
                        "Places booking in CONFIRMED with simulated Demo Escrow & unlocks Demo gate pass."
                      } else {
                        "Saves booking as PENDING_PAYMENT; exact gate details remain locked."
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                ParkSpaceLiquidGlassSwitch(
                  checked = data.form.confirmDemoPaymentImmediately,
                  onCheckedChange = onToggleImmediateDemoPayment,
                  modifier = Modifier.testTag("checkout_immediate_payment_switch"),
                )
              }

              if (data.form.confirmDemoPaymentImmediately) {
                DemoPaymentMethodSelector(
                  selectedMethod = data.form.selectedPaymentMethod,
                  availableDemoCreditPkr = data.availableDemoCreditPkr,
                  requiredTotalPkr = data.pricing?.totalAmountPkr,
                  onSelectMethod = onSelectPaymentMethod,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "No real payment, card charge, or bank transfer occurs in Demo Mode.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            // Policy summary
            ParkSpaceGlassSurface(
              accentGlow = semantic.customerGlow,
              contentPaddingDp = 14,
            ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text =
                  when (data.form.bookingType) {
                    BookingType.HOURLY ->
                      "Hourly cancellation: >= 2 hrs = 100% Demo refund; < 2 hrs = 50%; after start = 0%."
                    BookingType.DAILY ->
                      "Daily cancellation: >= 24 hrs = 100% Demo refund; < 24 hrs = 1-day fee retained."
                    BookingType.MONTHLY ->
                      "Monthly cancellation: >= 72 hrs = 100% Demo refund; active cycle non-refundable, future cycles 100%."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (data.form.submissionError != null) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("checkout_submission_error"),
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.errorContainer,
            ) {
              Text(
                text = data.form.submissionError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(ParkSpaceSpacing.sm),
              )
            }
          }

          ParkSpaceLiquidGlassButton(
            text =
              if (data.form.isSubmitting) "Checking Availability..."
              else if (data.form.confirmDemoPaymentImmediately) "Confirm Demo Booking"
              else "Save Pending Demo Booking",
            onClick = onConfirmBooking,
            enabled =
              !data.form.isSubmitting &&
                data.availability?.isAvailable == true &&
                data.pricing != null,
            modifier = Modifier.fillMaxWidth().testTag("confirm_booking_button"),
          )

          Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
        }
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
}

@Composable
private fun PriceRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = value,
      style = ParkSpaceMonoPriceStyle,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
