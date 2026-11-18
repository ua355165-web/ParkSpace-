package com.example.feature.discovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.UiState
import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStateContainer
import com.example.feature.discovery.DiscoveryScreenData
import com.example.feature.discovery.DiscoveryViewModel
import com.example.feature.discovery.domain.model.DiscoveryViewMode
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingDiscoveryScreen(
  uiState: UiState<DiscoveryScreenData>,
  viewModel: DiscoveryViewModel,
  onBack: () -> Unit,
  onViewDetails: (String) -> Unit,
  modifier: Modifier = Modifier,
  mapContentOverride: (@Composable (DiscoveryScreenData) -> Unit)? = null,
) {
  var showFilterSheet by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.discovery_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("discovery_back_button"),
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
        modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = ParkSpaceSpacing.screenHorizontal),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkSpaceModeBanner(mode = AppEnvironment.currentMode)

          when (uiState) {
            is UiState.Success -> {
              val data = uiState.data
              val activeFilters = data.filter.activeFilterCount()

              // Search input
              OutlinedTextField(
                value = data.filter.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.discovery_search_hint)) },
                leadingIcon = {
                  Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                  if (data.filter.searchQuery.isNotEmpty()) {
                    IconButton(
                      onClick = { viewModel.onSearchQueryChange("") },
                      modifier =
                        Modifier.size(ParkSpaceSpacing.minTouchTarget)
                          .testTag("clear_search_button"),
                    ) {
                      Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                  }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().testTag("discovery_search_input"),
              )

              // City Chips
              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                items(SupportedCity.entries) { city ->
                  FilterChip(
                    selected = data.filter.city == city,
                    onClick = { viewModel.onSelectCity(city) },
                    label = { Text(city.displayName) },
                    modifier = Modifier.testTag("discovery_city_${city.name.lowercase()}"),
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                  )
                }
              }

              // Map / List Toggle + Filter Button + Reset Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                SingleChoiceSegmentedButtonRow(
                  modifier = Modifier.testTag("discovery_view_toggle"),
                ) {
                  SegmentedButton(
                    selected = data.viewMode == DiscoveryViewMode.MAP,
                    onClick = { viewModel.onToggleViewMode(DiscoveryViewMode.MAP) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                      Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    modifier = Modifier.testTag("toggle_map_button"),
                  ) {
                    Text(stringResource(R.string.discovery_map_view))
                  }
                  SegmentedButton(
                    selected = data.viewMode == DiscoveryViewMode.LIST,
                    onClick = { viewModel.onToggleViewMode(DiscoveryViewMode.LIST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                      Icon(
                        imageVector = Icons.Outlined.ViewList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    modifier = Modifier.testTag("toggle_list_button"),
                  ) {
                    Text(stringResource(R.string.discovery_list_view))
                  }
                }

                Row(
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (activeFilters > 0) {
                    AssistChip(
                      onClick = viewModel::onResetFilters,
                      label = { Text("Reset") },
                      modifier = Modifier.testTag("discovery_quick_reset_chip"),
                    )
                  }
                  FilterChip(
                    selected = activeFilters > 0,
                    onClick = { showFilterSheet = true },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                    },
                    label = {
                      Text(
                        if (activeFilters > 0) "Filters ($activeFilters)"
                        else stringResource(R.string.discovery_filters)
                      )
                    },
                    modifier =
                      Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("open_filters_button"),
                  )
                }
              }

              // Active Summary Line
              Text(
                text =
                  "${data.result.listings.size} of ${data.result.totalApprovedInCity} spaces in ${data.filter.city.displayName} • ~${AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS}m masked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("discovery_summary_text"),
              )
            }
            else -> Unit
          }
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))

        // Main Map / List Area
        ParkSpaceStateContainer(
          state = uiState,
          modifier = Modifier.weight(1f),
        ) { data ->
          if (data.result.listings.isEmpty()) {
            val isMarketplaceEmptyForCity =
              data.result.totalApprovedInCity == 0 && data.filter.activeFilterCount() == 0
            Box(
              modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("discovery_empty_state"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
              ) {
                Column(
                  modifier = Modifier.padding(ParkSpaceSpacing.lg),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
                ) {
                  Text(
                    text =
                      if (isMarketplaceEmptyForCity) {
                        stringResource(R.string.empty_customer_marketplace)
                      } else {
                        "No matching parking in ${data.filter.city.displayName}"
                      },
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text =
                      if (isMarketplaceEmptyForCity) {
                        "Approved parking spaces listed by verified Hosts in ${data.filter.city.displayName} will appear here once created."
                      } else {
                        "Try resetting active filters or searching another locality."
                      },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (isMarketplaceEmptyForCity) {
                    ParkSpacePrimaryButton(
                      text = stringResource(R.string.action_explore_parking),
                      onClick = { showFilterSheet = true },
                      modifier = Modifier.testTag("empty_explore_cities_button"),
                    )
                  } else {
                    ParkSpacePrimaryButton(
                      text = stringResource(R.string.discovery_reset_filters),
                      onClick = viewModel::onResetFilters,
                      modifier = Modifier.testTag("empty_reset_filters_button"),
                    )
                  }
                }
              }
            }
          } else {
            when (data.viewMode) {
              DiscoveryViewMode.MAP -> {
                Box(modifier = Modifier.fillMaxSize()) {
                  if (mapContentOverride != null) {
                    mapContentOverride(data)
                  } else {
                    ParkSpaceMapLibreView(
                      city = data.filter.city,
                      markers = data.result.markers,
                      mapRenderStatus = data.mapRenderStatus,
                      cameraTriggerVersion = data.cameraTriggerVersion,
                      onMarkerSelected = viewModel::onSelectSpace,
                      onStyleLoaded = viewModel::onMapStyleLoaded,
                      onMapLoadFailed = viewModel::onMapLoadFailed,
                      onRetryMap = viewModel::onRetryMapLoad,
                      onRecenterCity = viewModel::onRecenterCity,
                      onSwitchToList = { viewModel.onToggleViewMode(DiscoveryViewMode.LIST) },
                    )
                  }

                  val selected = data.selectedListing
                  if (selected != null) {
                    Box(
                      modifier =
                        Modifier.align(Alignment.BottomCenter)
                          .padding(ParkSpaceSpacing.sm)
                          .widthIn(max = ParkSpaceSpacing.maxReadableWidth),
                    ) {
                      ParkingPreviewCard(
                        listing = selected,
                        onViewDetails = onViewDetails,
                        onDismiss = viewModel::onClearSelection,
                        isSelectedHighlight = true,
                        modifier = Modifier.testTag("map_selected_preview_card"),
                      )
                    }
                  }
                }
              }

              DiscoveryViewMode.LIST -> {
                LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag("discovery_list_view"),
                  contentPadding =
                    PaddingValues(
                      horizontal = ParkSpaceSpacing.screenHorizontal,
                      vertical = ParkSpaceSpacing.sm,
                    ),
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
                ) {
                  items(data.result.listings, key = { it.id }) { listing ->
                    ParkingPreviewCard(
                      listing = listing,
                      onViewDetails = onViewDetails,
                      isSelectedHighlight = listing.id == data.selectedListing?.id,
                    )
                  }
                }
              }
            }
          }

          if (showFilterSheet) {
            ParkingFilterBottomSheet(
              filter = data.filter,
              availableLocalities = data.result.availableLocalities,
              resultCount = data.result.listings.size,
              onSelectCity = viewModel::onSelectCity,
              onSelectLocality = viewModel::onLocalitySelect,
              onSelectVehicle = viewModel::onVehicleTypeSelect,
              onSelectParkingType = viewModel::onParkingTypeSelect,
              onSelectCovered = viewModel::onCoveredFilterSelect,
              onToggleCctv = viewModel::onToggleCctv,
              onToggleGuard = viewModel::onToggleSecurityGuard,
              onToggleEv = viewModel::onToggleEvCharging,
              onToggleNightLighting = viewModel::onToggleNightLighting,
              onSelectBookingType = viewModel::onBookingTypeSelect,
              onPriceRangeChange = viewModel::onPriceRangeChange,
              onMinCapacityChange = viewModel::onMinCapacityChange,
              onResetFilters = viewModel::onResetFilters,
              onDismiss = { showFilterSheet = false },
            )
          }
        }
      }
    }
  }
}
