package com.example.feature.host.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.VehicleType
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostBaysManagerDialog(
  space: ParkingSpace,
  bays: List<ParkingBay>,
  onDismiss: () -> Unit,
  onAddBay: (spaceId: String, bayCode: String, supportedVehicles: Set<VehicleType>, isCovered: Boolean) -> Unit,
  onUpdateBay: (spaceId: String, bay: ParkingBay) -> Unit,
  onToggleBayActive: (spaceId: String, bayId: String, isActive: Boolean) -> Unit,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
) {
  var showAddForm by rememberSaveable { mutableStateOf(false) }
  var newBayCode by rememberSaveable { mutableStateOf("") }
  var selectedVehicles by remember { mutableStateOf(setOf(VehicleType.CAR)) }
  var isCoveredBay by rememberSaveable { mutableStateOf(space.parkingType.name.contains("COVERED") || space.parkingType.name.contains("GARAGE")) }
  var editingBay by remember { mutableStateOf<ParkingBay?>(null) }
  var editBayCode by rememberSaveable { mutableStateOf("") }
  var editSelectedVehicles by remember { mutableStateOf(setOf(VehicleType.CAR)) }
  var editIsCovered by rememberSaveable { mutableStateOf(false) }
  var localError by rememberSaveable { mutableStateOf<String?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        modifier
          .fillMaxWidth(0.95f)
          .widthIn(max = 540.dp)
          .padding(ParkSpaceSpacing.sm)
          .testTag("host_bays_manager_dialog"),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
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
            Text(text = "Manage Parking Bays", style = MaterialTheme.typography.titleLarge)
            Text(
              text = space.title,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("host_bays_dialog_close_button")) {
            Icon(Icons.Outlined.Close, contentDescription = "Close dialog")
          }
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(ParkSpaceSpacing.sm).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Total Bays", style = MaterialTheme.typography.labelMedium)
              Text(
                text = "${bays.size}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("host_bays_total_count"),
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Operational / Active", style = MaterialTheme.typography.labelMedium)
              Text(
                text = "${bays.count { it.isActiveInDemo }}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("host_bays_active_count"),
              )
            }
          }
        }

        if (errorMessage != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("host_bays_error_banner"),
          ) {
            Row(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }

        Text(text = "Existing Bays in Listing", style = MaterialTheme.typography.titleMedium)

        if (bays.isEmpty()) {
          Text(
            text = "Add your first parking bay.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          bays.forEach { bay ->
            Surface(
              shape = MaterialTheme.shapes.small,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              color =
                if (bay.isActiveInDemo) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth().testTag("host_bay_item_${bay.id}"),
            ) {
              Row(
                modifier = Modifier.padding(ParkSpaceSpacing.sm).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  ) {
                    Text(text = bay.bayCode, style = ParkSpaceMonoCodeStyle)
                    ParkSpaceStatusPill(
                      label = if (bay.isActiveInDemo) "Active" else "Deactivated",
                      tone = if (bay.isActiveInDemo) StatusTone.AVAILABLE else StatusTone.ERROR,
                    )
                  }
                  Text(
                    text =
                      "Vehicles: ${bay.supportedVehicles.joinToString { it.displayName }} • ${if (bay.isCovered) "Covered" else "Open"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
                ) {
                  IconButton(
                    onClick = {
                      editingBay = bay
                      editBayCode = bay.bayCode
                      editSelectedVehicles = bay.supportedVehicles
                      editIsCovered = bay.isCovered
                      showAddForm = false
                      localError = null
                    },
                    modifier = Modifier.testTag("host_bay_edit_button_${bay.id}"),
                  ) {
                    Icon(
                      Icons.Outlined.Edit,
                      contentDescription = "Edit ${bay.bayCode}",
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }

                  Switch(
                    checked = bay.isActiveInDemo,
                    onCheckedChange = { isChecked ->
                      onToggleBayActive(space.id, bay.id, isChecked)
                    },
                    modifier = Modifier.testTag("host_bay_active_toggle_${bay.id}"),
                  )
                }
              }
            }
          }
        }

        val currentEditingBay = editingBay
        if (currentEditingBay != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth().testTag("host_edit_bay_form"),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Text(
                text = "Edit Bay: ${currentEditingBay.bayCode}",
                style = MaterialTheme.typography.titleMedium,
              )

              OutlinedTextField(
                value = editBayCode,
                onValueChange = {
                  editBayCode = it
                  localError = null
                },
                label = { Text("Bay Identifier (e.g. Bay A-1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("host_edit_bay_code_input"),
              )

              Text(text = "Vehicle Compatibility", style = MaterialTheme.typography.labelMedium)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                VehicleType.entries.forEach { vType ->
                  val isSelected = vType in editSelectedVehicles
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      editSelectedVehicles =
                        if (isSelected) {
                          if (editSelectedVehicles.size > 1) editSelectedVehicles - vType else editSelectedVehicles
                        } else {
                          editSelectedVehicles + vType
                        }
                      localError = null
                    },
                    label = { Text(vType.displayName) },
                    leadingIcon = {
                      if (isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                      } else {
                        val icon = when (vType) {
                          VehicleType.BIKE -> Icons.Outlined.TwoWheeler
                          VehicleType.CAR, VehicleType.SUV -> Icons.Outlined.DirectionsCar
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                      }
                    },
                    modifier = Modifier.testTag("host_edit_bay_vehicle_chip_${vType.name}"),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(text = "Covered / Overhead Shade", style = MaterialTheme.typography.bodyMedium)
                Switch(
                  checked = editIsCovered,
                  onCheckedChange = { editIsCovered = it },
                  modifier = Modifier.testTag("host_edit_bay_covered_switch"),
                )
              }

              if (localError != null) {
                Text(
                  text = localError.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.testTag("host_edit_bay_local_error"),
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                OutlinedButton(
                  onClick = {
                    editingBay = null
                    localError = null
                  },
                  modifier = Modifier.weight(1f).testTag("host_cancel_edit_bay_button"),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text("Cancel")
                }
                ParkSpacePrimaryButton(
                  text = "Save Changes",
                  onClick = {
                    val code = editBayCode.trim()
                    if (code.length < 2) {
                      localError = "Enter bay code (at least 2 chars)."
                      return@ParkSpacePrimaryButton
                    }
                    if (editSelectedVehicles.isEmpty()) {
                      localError = "Select at least one vehicle type."
                      return@ParkSpacePrimaryButton
                    }
                    if (bays.any { it.id != currentEditingBay.id && it.bayCode.equals(code, ignoreCase = true) }) {
                      localError = "Bay code '$code' already exists."
                      return@ParkSpacePrimaryButton
                    }
                    onUpdateBay(
                      space.id,
                      currentEditingBay.copy(
                        bayCode = code,
                        supportedVehicles = editSelectedVehicles,
                        isCovered = editIsCovered,
                      ),
                    )
                    editingBay = null
                    localError = null
                  },
                  modifier = Modifier.weight(1f).testTag("host_submit_edit_bay_button"),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))

        if (!showAddForm) {
          OutlinedButton(
            onClick = {
              showAddForm = true
              editingBay = null
              localError = null
            },
            modifier =
              Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("host_open_add_bay_form_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(ParkSpaceSpacing.xs))
            Text("Add New Parking Bay")
          }
        } else {
          Surface(
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Text(text = "Add New Bay Slot", style = MaterialTheme.typography.titleMedium)

              OutlinedTextField(
                value = newBayCode,
                onValueChange = {
                  newBayCode = it
                  localError = null
                },
                label = { Text("Bay Identifier (e.g. Bay B-02)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("host_new_bay_code_input"),
              )

              Text(text = "Vehicle Compatibility", style = MaterialTheme.typography.labelMedium)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                VehicleType.entries.forEach { vType ->
                  val isSelected = vType in selectedVehicles
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      selectedVehicles =
                        if (isSelected) {
                          if (selectedVehicles.size > 1) selectedVehicles - vType else selectedVehicles
                        } else {
                          selectedVehicles + vType
                        }
                    },
                    label = { Text(vType.displayName) },
                    leadingIcon = {
                      if (isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                      } else {
                        val icon = when (vType) {
                          VehicleType.BIKE -> Icons.Outlined.TwoWheeler
                          VehicleType.CAR, VehicleType.SUV -> Icons.Outlined.DirectionsCar
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                      }
                    },
                    modifier = Modifier.testTag("host_bay_vehicle_chip_${vType.name}"),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(text = "Covered / Overhead Shade", style = MaterialTheme.typography.bodyMedium)
                Switch(
                  checked = isCoveredBay,
                  onCheckedChange = { isCoveredBay = it },
                  modifier = Modifier.testTag("host_bay_covered_switch"),
                )
              }

              if (localError != null) {
                Text(
                  text = localError.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                OutlinedButton(
                  onClick = { showAddForm = false },
                  modifier = Modifier.weight(1f),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text("Cancel")
                }
                ParkSpacePrimaryButton(
                  text = "Save Bay",
                  onClick = {
                    val code = newBayCode.trim()
                    if (code.length < 2) {
                      localError = "Enter bay code (at least 2 chars)."
                      return@ParkSpacePrimaryButton
                    }
                    if (bays.any { it.bayCode.equals(code, ignoreCase = true) }) {
                      localError = "Bay code '$code' already exists."
                      return@ParkSpacePrimaryButton
                    }
                    onAddBay(space.id, code, selectedVehicles, isCoveredBay)
                    newBayCode = ""
                    showAddForm = false
                  },
                  modifier = Modifier.weight(1f).testTag("host_submit_add_bay_button"),
                )
              }
            }
          }
        }
      }
    }
  }
}
