package com.example.feature.discovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.feature.discovery.domain.model.CoveredFilter
import com.example.feature.discovery.domain.model.ParkingDiscoveryFilter
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParkingFilterBottomSheet(
  filter: ParkingDiscoveryFilter,
  availableLocalities: List<String>,
  resultCount: Int,
  onSelectCity: (SupportedCity) -> Unit,
  onSelectLocality: (String) -> Unit,
  onSelectVehicle: (VehicleType?) -> Unit,
  onSelectParkingType: (ParkingType?) -> Unit,
  onSelectCovered: (CoveredFilter) -> Unit,
  onToggleCctv: () -> Unit,
  onToggleGuard: () -> Unit,
  onToggleEv: () -> Unit,
  onToggleNightLighting: () -> Unit,
  onSelectBookingType: (BookingType?) -> Unit,
  onPriceRangeChange: (Int?, Int?) -> Unit,
  onMinCapacityChange: (Int) -> Unit,
  onResetFilters: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var minPriceText by remember(filter.minPricePkr) {
    mutableStateOf(filter.minPricePkr?.toString().orEmpty())
  }
  var maxPriceText by remember(filter.maxPricePkr) {
    mutableStateOf(filter.maxPricePkr?.toString().orEmpty())
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = ParkSpaceSpacing.md, vertical = ParkSpaceSpacing.xs),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Filter Parking (${filter.activeFilterCount()} active)",
          style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(
          onClick = {
            minPriceText = ""
            maxPriceText = ""
            onResetFilters()
          },
          modifier =
            Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("sheet_reset_filters_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Text("Reset All")
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // 1. City
      FilterSectionLabel("1. City")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        SupportedCity.entries.forEach { city ->
          FilterChip(
            selected = filter.city == city,
            onClick = { onSelectCity(city) },
            label = { Text(city.displayName) },
            modifier = Modifier.testTag("sheet_city_${city.name.lowercase()}"),
          )
        }
      }

      // 2. Locality
      if (availableLocalities.isNotEmpty()) {
        FilterSectionLabel("2. Locality in ${filter.city.displayName}")
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
        ) {
          availableLocalities.forEach { locality ->
            FilterChip(
              selected = filter.localityQuery == locality,
              onClick = { onSelectLocality(locality) },
              label = { Text(locality) },
            )
          }
        }
      }

      // 3. Vehicle Compatibility
      FilterSectionLabel("3. Vehicle Compatibility")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        VehicleType.entries.forEach { vehicle ->
          FilterChip(
            selected = filter.vehicleType == vehicle,
            onClick = { onSelectVehicle(vehicle) },
            label = { Text(vehicle.displayName) },
            modifier = Modifier.testTag("filter_vehicle_${vehicle.name.lowercase()}"),
          )
        }
      }

      // 4. Parking Type
      FilterSectionLabel("4. Parking Type")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
      ) {
        ParkingType.entries.forEach { type ->
          FilterChip(
            selected = filter.parkingType == type,
            onClick = { onSelectParkingType(type) },
            label = { Text(type.displayName) },
          )
        }
      }

      // 5. Covered / Open
      FilterSectionLabel("5. Shade & Enclosure")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        CoveredFilter.entries.forEach { option ->
          FilterChip(
            selected = filter.coveredFilter == option,
            onClick = { onSelectCovered(option) },
            label = { Text(option.displayName) },
            modifier = Modifier.testTag("filter_covered_${option.name.lowercase()}"),
          )
        }
      }

      // 6. Security & Features
      FilterSectionLabel("6. Security & Amenities")
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
      ) {
        FilterChip(
          selected = filter.security.requireCctv,
          onClick = onToggleCctv,
          label = { Text("24/7 CCTV") },
          modifier = Modifier.testTag("filter_cctv_chip"),
        )
        FilterChip(
          selected = filter.security.requireSecurityGuard,
          onClick = onToggleGuard,
          label = { Text("Security Guard") },
          modifier = Modifier.testTag("filter_guard_chip"),
        )
        FilterChip(
          selected = filter.security.requireEvCharging,
          onClick = onToggleEv,
          label = { Text("EV Charging") },
          modifier = Modifier.testTag("filter_ev_chip"),
        )
        FilterChip(
          selected = filter.security.requireNightLighting,
          onClick = onToggleNightLighting,
          label = { Text("Night Lighting") },
        )
      }

      // 7. Hourly / Daily / Monthly Availability
      FilterSectionLabel("7. Booking Plan Availability")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        BookingType.entries.forEach { plan ->
          FilterChip(
            selected = filter.bookingType == plan,
            onClick = { onSelectBookingType(plan) },
            label = { Text(plan.displayName) },
            modifier = Modifier.testTag("filter_plan_${plan.name.lowercase()}"),
          )
        }
      }

      // 8. PKR Price Range
      val priceUnitLabel =
        when (filter.bookingType) {
          BookingType.DAILY -> "Daily"
          BookingType.MONTHLY -> "Monthly"
          BookingType.HOURLY,
          null -> "Hourly"
        }
      FilterSectionLabel("8. $priceUnitLabel Price Range (PKR)")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        OutlinedTextField(
          value = minPriceText,
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }
            minPriceText = digits
            onPriceRangeChange(digits.toIntOrNull(), maxPriceText.toIntOrNull())
          },
          label = { Text("Min Rs.") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f).testTag("filter_min_price_input"),
        )
        OutlinedTextField(
          value = maxPriceText,
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }
            maxPriceText = digits
            onPriceRangeChange(minPriceText.toIntOrNull(), digits.toIntOrNull())
          },
          label = { Text("Max Rs.") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f).testTag("filter_max_price_input"),
        )
      }

      // 9. Available Capacity
      FilterSectionLabel("9. Minimum Available Bays")
      FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
        listOf(1, 5, 10, 15).forEach { cap ->
          FilterChip(
            selected = filter.minAvailableBays == cap,
            onClick = { onMinCapacityChange(cap) },
            label = { Text("$cap+ bays") },
            modifier = Modifier.testTag("filter_capacity_$cap"),
          )
        }
      }

      Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))

      ParkSpacePrimaryButton(
        text = "Show $resultCount Matching Spaces",
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().testTag("apply_filters_button"),
      )

      Spacer(modifier = Modifier.height(ParkSpaceSpacing.lg))
    }
  }
}

@Composable
private fun FilterSectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
  )
}
