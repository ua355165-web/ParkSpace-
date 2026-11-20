package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.domain.model.VehicleType
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDemoVehicleDialog(
  onDismiss: () -> Unit,
  onSaveVehicle: (VehicleType, String, String) -> Unit,
) {
  var selectedType by rememberSaveable { mutableStateOf(VehicleType.CAR) }
  var label by rememberSaveable { mutableStateOf("") }
  var plate by rememberSaveable { mutableStateOf("") }
  var validationError by rememberSaveable { mutableStateOf<String?>(null) }
  val semantic = ParkSpaceThemeTokens.semantic

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        Modifier
          .padding(horizontal = ParkSpaceSpacing.screenHorizontal)
          .widthIn(max = 460.dp)
          .fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text(
          text = "Add Local Demo Vehicle",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = "Saved privately to your local Customer Demo account.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Vehicle Type",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          VehicleType.entries.forEach { type ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(type.displayName) },
              shape = CircleShape,
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                  selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
              modifier =
                Modifier
                  .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                  .testTag("add_vehicle_type_${type.name.lowercase()}"),
            )
          }
        }
        OutlinedTextField(
          value = label,
          onValueChange = {
            label = it
            validationError = null
          },
          label = { Text("Vehicle Name / Model") },
          placeholder = { Text("e.g. Honda City 1.2") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("add_vehicle_label_input"),
        )
        OutlinedTextField(
          value = plate,
          onValueChange = {
            plate = it
            validationError = null
          },
          label = { Text("Registration Plate") },
          placeholder = { Text("e.g. LEA-4821") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("add_vehicle_plate_input"),
        )
        if (validationError != null) {
          Text(
            text = validationError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
          ParkSpaceLiquidGlassButton(
            text = "Save Vehicle",
            onClick = {
              when {
                label.trim().length < 2 ->
                  validationError = "Enter a vehicle name (at least 2 characters)."
                plate.trim().length < 3 ->
                  validationError = "Enter a registration plate (at least 3 characters)."
                else -> {
                  onSaveVehicle(selectedType, label.trim(), plate.trim())
                }
              }
            },
            modifier = Modifier.testTag("confirm_add_vehicle_button"),
          )
        }
      }
    }
  }
}
