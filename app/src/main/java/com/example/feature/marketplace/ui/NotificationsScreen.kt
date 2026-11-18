package com.example.feature.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.feature.booking.ui.formatDateTimePkt
import com.example.feature.marketplace.NotificationsUiState
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  uiState: NotificationsUiState,
  onBack: () -> Unit,
  onMarkAsRead: (String) -> Unit,
  onMarkAllAsRead: () -> Unit,
  onOpenRelatedBooking: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Notifications (${uiState.unreadCount} unread)", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("notifications_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (uiState.unreadCount > 0) {
            TextButton(
              onClick = onMarkAllAsRead,
              modifier = Modifier.testTag("notifications_mark_all_read_button"),
            ) {
              Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
              Text(" Mark all")
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize(),
      ) {
        Box(modifier = Modifier.padding(horizontal = ParkSpaceSpacing.screenHorizontal)) {
          ParkSpaceModeBanner(mode = AppEnvironment.currentMode)
        }

        if (uiState.notifications.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("notifications_empty_state"),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.padding(ParkSpaceSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
              ) {
                Icon(
                  imageVector = Icons.Outlined.NotificationsNone,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(36.dp),
                )
                Text(
                  text = stringResource(R.string.empty_notifications),
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Real notifications will appear here when bookings, Demo payments, messages, reviews, or disputes happen.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("notifications_list"),
            contentPadding = PaddingValues(ParkSpaceSpacing.screenHorizontal, ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            items(uiState.notifications, key = { it.id }) { notif ->
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      onMarkAsRead(notif.id)
                      val bId = notif.relatedBookingId
                      if (bId != null && onOpenRelatedBooking != null) {
                        onOpenRelatedBooking(bId)
                      }
                    }
                    .testTag("notification_card_${notif.id}"),
                shape = MaterialTheme.shapes.small,
                color =
                  if (notif.isRead) MaterialTheme.colorScheme.surface
                  else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color =
                      if (notif.isRead) MaterialTheme.colorScheme.outlineVariant
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                  ),
              ) {
                Column(
                  modifier = Modifier.padding(ParkSpaceSpacing.sm),
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    ParkSpaceStatusPill(
                      label = notif.type.displayName,
                      tone = if (notif.isRead) StatusTone.INFO else StatusTone.AVAILABLE,
                    )
                    Text(
                      text = formatDateTimePkt(notif.createdAtEpochMs),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = notif.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                  )
                  Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
