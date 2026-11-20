package com.example.feature.host.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.common.ListingPhotoStorage
import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.feature.discovery.ui.ParkingSpaceMediaBanner
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostListingEditorDialog(
  hostId: String,
  hostVerificationStatus: HostVerificationStatus,
  existingSpace: ParkingSpace?,
  onDismiss: () -> Unit,
  onSaveListing: (space: ParkingSpace, publishImmediately: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isEditMode = existingSpace != null

  var title by rememberSaveable { mutableStateOf(existingSpace?.title ?: "") }
  var description by rememberSaveable { mutableStateOf(existingSpace?.description ?: "") }
  var selectedCity by remember { mutableStateOf(existingSpace?.location?.city ?: SupportedCity.LAHORE) }
  var areaLabel by rememberSaveable { mutableStateOf(existingSpace?.location?.areaLabel ?: "") }
  var parkingType by remember { mutableStateOf(existingSpace?.parkingType ?: ParkingType.COVERED_GARAGE) }

  var hourlyRateStr by rememberSaveable { mutableStateOf(existingSpace?.hourlyRatePkr?.toString() ?: "150") }
  var dailyRateStr by rememberSaveable { mutableStateOf(existingSpace?.dailyRatePkr?.toString() ?: "1200") }
  var monthlyEnabled by rememberSaveable { mutableStateOf(existingSpace?.monthlyEnabled ?: true) }
  var monthlyRateStr by rememberSaveable { mutableStateOf(existingSpace?.monthlyRatePkr?.toString() ?: "18000") }

  var supportedVehicles by remember {
    mutableStateOf(existingSpace?.supportedVehicles ?: setOf(VehicleType.CAR, VehicleType.SUV))
  }
  var amenities by remember {
    mutableStateOf(
      existingSpace?.amenities
        ?: setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV, ParkingAmenity.SECURITY_GUARD)
    )
  }

  var privateGateSummary by rememberSaveable {
    mutableStateOf(existingSpace?.location?.privateGateSummary ?: "Enter via security gate barrier. Show Demo Booking PIN to guard on duty.")
  }

  var photoTags by remember {
    mutableStateOf(existingSpace?.photoTags ?: listOf("Entrance Gate", "Covered Bay"))
  }
  var photoUris by remember {
    mutableStateOf(existingSpace?.photoUris ?: emptyList())
  }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var validationError by rememberSaveable { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
    ) { uris ->
      if (uris.isNotEmpty()) {
        coroutineScope.launch {
          val remaining = (5 - photoUris.size).coerceAtLeast(0)
          val imported = mutableListOf<String>()
          uris.take(remaining).forEach { uri ->
            ListingPhotoStorage.copyPickedPhotoToInternalStorage(
                context = context,
                sourceUri = uri,
                spaceIdHint = existingSpace?.id ?: "draft",
              )
              .onSuccess { savedUri -> imported += savedUri }
              .onFailure { err -> validationError = err.message }
          }
          if (imported.isNotEmpty()) {
            photoUris = (photoUris + imported).distinct().take(5)
          }
        }
      }
    }

  val canPublish = hostVerificationStatus == HostVerificationStatus.APPROVED
  val semantic = ParkSpaceThemeTokens.semantic

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        modifier
          .fillMaxWidth(0.96f)
          .widthIn(max = 600.dp)
          .padding(ParkSpaceSpacing.sm)
          .testTag("host_listing_editor_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.md).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isEditMode) "Edit Parking Listing" else "Create New Parking Listing",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Pakistan Parking Marketplace • Local Demo",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("host_listing_editor_close_button")) {
            Icon(Icons.Outlined.Close, contentDescription = "Close dialog")
          }
        }

        if (!canPublish) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("host_editor_verification_lock_banner"),
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text =
                  if (hostVerificationStatus == HostVerificationStatus.PENDING_REVIEW) {
                    "Your Host account is PENDING_REVIEW. You can create and save Draft listings now. Super Admin approval is required before publishing."
                  } else {
                    "Host verification status is REJECTED. Listings cannot be published to live discovery."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }

        // Visual Preview (Uploaded photo or architectural banner)
        ParkingSpaceMediaBanner(
          photoUris = photoUris,
          parkingType = parkingType,
          isCovered = ParkingAmenity.COVERED in amenities || parkingType == ParkingType.COVERED_GARAGE,
          contentDescription = "Listing media preview",
          height = 104.dp,
        )

        // Title & Description
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            validationError = null
          },
          label = { Text("Listing Title *") },
          placeholder = { Text("e.g. Secure Covered Bay — Gulberg III") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("host_listing_title_input"),
        )

        OutlinedTextField(
          value = description,
          onValueChange = {
            description = it
            validationError = null
          },
          label = { Text("Property Description *") },
          placeholder = { Text("Detailed entry instructions, ceiling clearance, and security features...") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("host_listing_description_input"),
        )

        // City & Area
        Text(text = "Supported City", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          SupportedCity.entries.forEach { city ->
            FilterChip(
              selected = selectedCity == city,
              onClick = { selectedCity = city },
              label = { Text(city.displayName) },
              shape = CircleShape,
              modifier = Modifier.testTag("host_editor_city_chip_${city.name}"),
            )
          }
        }

        OutlinedTextField(
          value = areaLabel,
          onValueChange = {
            areaLabel = it
            validationError = null
          },
          label = { Text("Neighborhood / Locality *") },
          placeholder = { Text("e.g. Gulberg III, Main Boulevard") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("host_listing_area_input"),
        )

        // Parking Type
        Text(text = "Parking Facility Type", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          ParkingType.entries.forEach { pType ->
            FilterChip(
              selected = parkingType == pType,
              onClick = { parkingType = pType },
              label = { Text(pType.displayName) },
              shape = CircleShape,
              modifier = Modifier.testTag("host_editor_type_chip_${pType.name}"),
            )
          }
        }

        // Pricing Rates
        Text(text = "PKR Pricing Configuration", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          OutlinedTextField(
            value = hourlyRateStr,
            onValueChange = {
              hourlyRateStr = it
              validationError = null
            },
            label = { Text("Hourly (Rs.) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("host_listing_hourly_rate_input"),
          )
          OutlinedTextField(
            value = dailyRateStr,
            onValueChange = {
              dailyRateStr = it
              validationError = null
            },
            label = { Text("Daily (Rs.) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("host_listing_daily_rate_input"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = "Enable 30-Day Monthly Plan", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = monthlyEnabled,
            onCheckedChange = { monthlyEnabled = it },
            modifier = Modifier.testTag("host_listing_monthly_toggle"),
          )
        }

        if (monthlyEnabled) {
          OutlinedTextField(
            value = monthlyRateStr,
            onValueChange = {
              monthlyRateStr = it
              validationError = null
            },
            label = { Text("Monthly Rate (Rs. for 30 days) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("host_listing_monthly_rate_input"),
          )
        }

        // Vehicle Compatibility
        Text(text = "Compatible Vehicles", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          VehicleType.entries.forEach { vType ->
            val isSelected = vType in supportedVehicles
            FilterChip(
              selected = isSelected,
              onClick = {
                supportedVehicles =
                  if (isSelected) {
                    if (supportedVehicles.size > 1) supportedVehicles - vType else supportedVehicles
                  } else {
                    supportedVehicles + vType
                  }
              },
              label = { Text(vType.displayName) },
              shape = CircleShape,
              leadingIcon = {
                if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.testTag("host_editor_vehicle_${vType.name}"),
            )
          }
        }

        // Amenities
        Text(text = "Property Amenities & Security", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          ParkingAmenity.entries.forEach { amenity ->
            val isSelected = amenity in amenities
            FilterChip(
              selected = isSelected,
              onClick = {
                amenities = if (isSelected) amenities - amenity else amenities + amenity
              },
              label = { Text(amenity.displayName) },
              shape = CircleShape,
              leadingIcon = {
                if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.testTag("host_editor_amenity_${amenity.name}"),
            )
          }
        }

        // Private Gate Details & 200m Masking
        Text(text = "Private Gate Details (Unlocked After Confirmed Booking)", style = MaterialTheme.typography.titleSmall)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = semantic.glassSurface,
          border = BorderStroke(1.dp, semantic.glassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(ParkSpaceSpacing.sm), verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs)) {
            Text(
              text = "200-Metre Masking Policy Active",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Public discovery displays an approximate 200m neighborhood zone. Your exact gate coordinates and private instructions remain hidden until an eligible customer confirms a booking.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        OutlinedTextField(
          value = privateGateSummary,
          onValueChange = { privateGateSummary = it },
          label = { Text("Private Gate Instructions / Pin / Contact Note") },
          minLines = 2,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("host_listing_gate_summary_input"),
        )

        // Real Photo Upload via Android Photo Picker + Photo Tags (Up to 5 Photos)
        Text(text = "Listing Photos (${photoUris.size}/5 uploaded)", style = MaterialTheme.typography.titleSmall)
        Text(
          text = "Select up to 5 real property photos using the Android Photo Picker (no storage permission required).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ParkSpaceLiquidGlassSecondaryButton(
          text = if (photoUris.size >= 5) "Maximum 5 Photos Added" else "Upload Photos from Device",
          leadingIcon = Icons.Outlined.AddPhotoAlternate,
          enabled = photoUris.size < 5,
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier =
            Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("host_pick_photos_button"),
        )

        if (photoUris.isNotEmpty()) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            modifier = Modifier.fillMaxWidth().testTag("host_uploaded_photos_row"),
          ) {
            items(photoUris) { uri ->
              Box {
                AsyncImage(
                  model = uri,
                  contentDescription = "Uploaded listing photo",
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier.size(width = 96.dp, height = 72.dp).clip(MaterialTheme.shapes.small),
                )
                IconButton(
                  onClick = { photoUris = photoUris - uri },
                  modifier =
                    Modifier.align(Alignment.TopEnd)
                      .size(28.dp)
                      .testTag("host_remove_uploaded_photo"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove photo",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          photoTags.forEach { tag ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs, vertical = ParkSpaceSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
              ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(text = tag, style = MaterialTheme.typography.labelSmall)
                if (photoTags.size > 1) {
                  IconButton(
                    onClick = { photoTags = photoTags - tag },
                    modifier = Modifier.size(18.dp).testTag("host_remove_photo_tag_${tag.replace(" ", "_")}"),
                  ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove $tag", modifier = Modifier.size(12.dp))
                  }
                }
              }
            }
          }
        }

        if (photoTags.size < 5) {
          val presets = listOf("Entrance Gate", "Covered Bay", "Security Post", "EV Charger", "Basement Ramp", "CCTV Monitor", "Street View")
          val availablePresets = presets.filter { it !in photoTags }
          if (availablePresets.isNotEmpty()) {
            Text(text = "Add Photo Tag Preset:", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
              availablePresets.take(4).forEach { preset ->
                OutlinedButton(
                  onClick = {
                    if (photoTags.size < 5) photoTags = photoTags + preset
                  },
                  modifier = Modifier.testTag("host_add_photo_preset_${preset.replace(" ", "_")}"),
                  shape = MaterialTheme.shapes.extraSmall,
                ) {
                  Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
                  Text(preset, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }

        if (validationError != null) {
          Text(
            text = validationError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))

        // Action Buttons: Save Draft vs Publish Live Listing
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkSpaceLiquidGlassSecondaryButton(
            text = "Save as Draft",
            onClick = {
              val validatedSpace = validateAndBuildSpace(
                existingSpace = existingSpace,
                hostId = hostId,
                title = title,
                description = description,
                city = selectedCity,
                areaLabel = areaLabel,
                parkingType = parkingType,
                hourlyRateStr = hourlyRateStr,
                dailyRateStr = dailyRateStr,
                monthlyEnabled = monthlyEnabled,
                monthlyRateStr = monthlyRateStr,
                supportedVehicles = supportedVehicles,
                amenities = amenities,
                privateGateSummary = privateGateSummary,
                photoTags = photoTags,
                photoUris = photoUris,
                onError = { validationError = it },
              ) ?: return@ParkSpaceLiquidGlassSecondaryButton

              onSaveListing(validatedSpace, false)
            },
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("host_save_draft_button"),
          )

          ParkSpaceLiquidGlassButton(
            text = "Publish Listing",
            onClick = {
              if (!canPublish) {
                validationError =
                  "Cannot publish: Host account verification is ${hostVerificationStatus.displayName}. Super Admin approval is required. Save as Draft instead."
                return@ParkSpaceLiquidGlassButton
              }

              val validatedSpace = validateAndBuildSpace(
                existingSpace = existingSpace,
                hostId = hostId,
                title = title,
                description = description,
                city = selectedCity,
                areaLabel = areaLabel,
                parkingType = parkingType,
                hourlyRateStr = hourlyRateStr,
                dailyRateStr = dailyRateStr,
                monthlyEnabled = monthlyEnabled,
                monthlyRateStr = monthlyRateStr,
                supportedVehicles = supportedVehicles,
                amenities = amenities,
                privateGateSummary = privateGateSummary,
                photoTags = photoTags,
                photoUris = photoUris,
                onError = { validationError = it },
              ) ?: return@ParkSpaceLiquidGlassButton

              onSaveListing(validatedSpace, true)
            },
            modifier = Modifier.weight(1f).testTag("host_publish_listing_button"),
          )
        }
      }
    }
  }
}

private fun validateAndBuildSpace(
  existingSpace: ParkingSpace?,
  hostId: String,
  title: String,
  description: String,
  city: SupportedCity,
  areaLabel: String,
  parkingType: ParkingType,
  hourlyRateStr: String,
  dailyRateStr: String,
  monthlyEnabled: Boolean,
  monthlyRateStr: String,
  supportedVehicles: Set<VehicleType>,
  amenities: Set<ParkingAmenity>,
  privateGateSummary: String,
  photoTags: List<String>,
  photoUris: List<String>,
  onError: (String) -> Unit,
): ParkingSpace? {
  val cleanTitle = title.trim()
  if (cleanTitle.length < 4) {
    onError("Listing title must be at least 4 characters.")
    return null
  }
  val cleanDesc = description.trim()
  if (cleanDesc.isBlank()) {
    onError("Enter property description.")
    return null
  }
  val cleanArea = areaLabel.trim()
  if (cleanArea.isBlank()) {
    onError("Enter area or locality name.")
    return null
  }

  val hourly = hourlyRateStr.trim().toIntOrNull()
  if (hourly == null || hourly <= 0) {
    onError("Enter a valid positive hourly rate.")
    return null
  }
  val daily = dailyRateStr.trim().toIntOrNull()
  if (daily == null || daily < hourly) {
    onError("Daily rate must be >= hourly rate.")
    return null
  }

  val monthly = if (monthlyEnabled) {
    val m = monthlyRateStr.trim().toIntOrNull()
    if (m == null || m <= 0) {
      onError("Enter a valid monthly rate when monthly plan is enabled.")
      return null
    }
    m
  } else null

  if (supportedVehicles.isEmpty()) {
    onError("Select at least one supported vehicle type.")
    return null
  }

  val spaceId = existingSpace?.id ?: "space-${UUID.randomUUID().toString().take(8)}"
  val publicLat = existingSpace?.location?.publicMaskedLatitude ?: city.centerLatitude
  val publicLng = existingSpace?.location?.publicMaskedLongitude ?: city.centerLongitude

  val location =
    ParkingLocation(
      city = city,
      areaLabel = cleanArea,
      publicMaskedLatitude = publicLat,
      publicMaskedLongitude = publicLng,
      maskRadiusMeters = AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS,
      privateGateSummary = privateGateSummary.trim().ifEmpty { null },
      privateExactLatitude = publicLat + 0.0004,
      privateExactLongitude = publicLng + 0.0004,
    )

  val schedule =
    existingSpace?.schedule
      ?: AvailabilitySchedule(
        openDays = ParkingDayOfWeek.entries.toSet(),
        openHour = 0,
        closeHour = 0,
        is24Hours = true,
      )

  return ParkingSpace(
    id = spaceId,
    hostId = hostId,
    title = cleanTitle,
    description = cleanDesc,
    parkingType = parkingType,
    location = location,
    hourlyRatePkr = hourly,
    dailyRatePkr = daily,
    monthlyRatePkr = monthly,
    monthlyEnabled = monthlyEnabled,
    totalBays = existingSpace?.totalBays ?: 1,
    availableBays = existingSpace?.availableBays ?: 1,
    supportedVehicles = supportedVehicles,
    amenities = amenities,
    schedule = schedule,
    isApproved = existingSpace?.isApproved ?: false,
    isDemoListing = true,
    photoTags = photoTags.take(5),
    photoUris = photoUris.take(5),
  )
}
