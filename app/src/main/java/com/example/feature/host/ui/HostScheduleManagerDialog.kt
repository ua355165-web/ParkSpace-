package com.example.feature.host.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingSpace
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostScheduleManagerDialog(
  space: ParkingSpace,
  onDismiss: () -> Unit,
  onSaveSchedule: (spaceId: String, schedule: AvailabilitySchedule) -> Unit,
  modifier: Modifier = Modifier,
) {
  val initialSchedule = space.schedule
  var selectedDays by remember { mutableStateOf(initialSchedule.openDays) }
  var is24h by rememberSaveable { mutableStateOf(initialSchedule.is24Hours) }
  var openHour by rememberSaveable { mutableIntStateOf(initialSchedule.openHour) }
  var closeHour by rememberSaveable { mutableIntStateOf(initialSchedule.closeHour) }
  var blackouts by remember { mutableStateOf(initialSchedule.blackoutDatesIso) }

  var newBlackoutInput by rememberSaveable { mutableStateOf("") }
  var validationError by rememberSaveable { mutableStateOf<String?>(null) }

  val todayIso = remember {
    LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE)
  }
  val tomorrowIso = remember {
    LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE).plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
  }

  val semantic = ParkSpaceThemeTokens.semantic

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
          .testTag("host_schedule_manager_dialog"),
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
            Text(text = "Operating Schedule & Blackouts", style = MaterialTheme.typography.titleLarge)
            Text(
              text = "${space.title} (Asia/Karachi Timezone)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("host_schedule_dialog_close_button")) {
            Icon(Icons.Outlined.Close, contentDescription = "Close dialog")
          }
        }

        Text(text = "Weekly Operating Days", style = MaterialTheme.typography.titleMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkingDayOfWeek.entries.forEach { day ->
            val isSelected = day in selectedDays
            FilterChip(
              selected = isSelected,
              onClick = {
                selectedDays =
                  if (isSelected) {
                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                  } else {
                    selectedDays + day
                  }
              },
              label = { Text(day.shortLabel) },
              shape = CircleShape,
              modifier = Modifier.testTag("host_schedule_day_chip_${day.name}"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "24/7 Continuous Access", style = MaterialTheme.typography.titleMedium)
            Text(
              text = if (is24h) "Facility stays open all 24 hours on active days" else "Facility operates during daytime hours",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = is24h,
            onCheckedChange = { is24h = it },
            modifier = Modifier.testTag("host_schedule_24h_switch"),
          )
        }

        if (!is24h) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = semantic.glassSurface,
            border = BorderStroke(1.dp, semantic.glassBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(ParkSpaceSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Text(text = "Facility Operating Hours", style = MaterialTheme.typography.titleSmall)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(text = "Opening Hour (PKT):")
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { if (openHour > 0) openHour-- },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease open hour")
                  }
                  Text(
                    text = "%02d:00".format(openHour),
                    style = ParkSpaceMonoCodeStyle,
                    modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs).testTag("host_schedule_open_hour_text"),
                  )
                  IconButton(
                    onClick = { if (openHour < closeHour - 1) openHour++ },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase open hour")
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(text = "Closing Hour (PKT):")
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { if (closeHour > openHour + 1) closeHour-- },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease close hour")
                  }
                  Text(
                    text = "%02d:00".format(closeHour),
                    style = ParkSpaceMonoCodeStyle,
                    modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs).testTag("host_schedule_close_hour_text"),
                  )
                  IconButton(
                    onClick = { if (closeHour < 23) closeHour++ },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase close hour")
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))
        Text(text = "Blackout Dates (Facility Closed)", style = MaterialTheme.typography.titleMedium)
        Text(
          text = "Specify full dates when your parking property is closed for private events, holidays, or construction.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (blackouts.isEmpty()) {
          Text(
            text = "No blackout dates currently scheduled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            blackouts.sorted().forEach { dateIso ->
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs, vertical = ParkSpaceSpacing.xxs),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
                ) {
                  Text(
                    text = dateIso,
                    style = ParkSpaceMonoCodeStyle,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                  IconButton(
                    onClick = { blackouts = blackouts - dateIso },
                    modifier = Modifier.size(20.dp).testTag("host_remove_blackout_${dateIso}"),
                  ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove $dateIso", modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = newBlackoutInput,
            onValueChange = {
              newBlackoutInput = it
              validationError = null
            },
            label = { Text("Add Date (YYYY-MM-DD)") },
            placeholder = { Text("e.g. $tomorrowIso") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).testTag("host_new_blackout_input"),
          )
          OutlinedButton(
            onClick = {
              val raw = newBlackoutInput.trim()
              try {
                LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
                blackouts = blackouts + raw
                newBlackoutInput = ""
                validationError = null
              } catch (e: DateTimeParseException) {
                validationError = "Invalid date format. Use YYYY-MM-DD (e.g. $tomorrowIso)."
              }
            },
            modifier = Modifier.testTag("host_add_blackout_button"),
            shape = CircleShape,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(ParkSpaceSpacing.xxs))
            Text("Add")
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          OutlinedButton(
            onClick = { blackouts = blackouts + tomorrowIso },
            modifier = Modifier.testTag("host_quick_blackout_tomorrow"),
            shape = CircleShape,
          ) {
            Text("+ Tomorrow", style = MaterialTheme.typography.labelSmall)
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

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          ParkSpaceLiquidGlassSecondaryButton(
            text = "Cancel",
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
          )
          ParkSpaceLiquidGlassButton(
            text = "Save Schedule",
            onClick = {
              if (selectedDays.isEmpty()) {
                validationError = "Select at least one operating day."
                return@ParkSpaceLiquidGlassButton
              }
              if (!is24h && openHour >= closeHour) {
                validationError = "Opening hour must be before closing hour."
                return@ParkSpaceLiquidGlassButton
              }

              val schedule =
                AvailabilitySchedule(
                  openDays = selectedDays,
                  openHour = if (is24h) 0 else openHour,
                  closeHour = if (is24h) 0 else closeHour,
                  is24Hours = is24h,
                  blackoutDatesIso = blackouts,
                )
              onSaveSchedule(space.id, schedule)
            },
            modifier = Modifier.weight(1f).testTag("host_save_schedule_button"),
          )
        }
      }
    }
  }
}
