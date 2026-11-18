package com.example.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.core.domain.model.VehicleType
import com.example.ui.theme.ParkSpaceSpacing

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

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Local Demo Vehicle") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm)) {
        Text(
          text = "Saved privately to your local Customer Demo account.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Vehicle Type", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          VehicleType.entries.forEach { type ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(type.displayName) },
              modifier = Modifier.testTag("add_vehicle_type_${type.name.lowercase()}"),
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
      }
    },
    confirmButton = {
      TextButton(
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
      ) {
        Text("Save Vehicle")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
