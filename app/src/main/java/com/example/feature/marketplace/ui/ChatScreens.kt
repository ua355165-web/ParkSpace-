package com.example.feature.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.feature.booking.ui.formatDateTimePkt
import com.example.feature.marketplace.BookingChatThreadUiState
import com.example.feature.marketplace.ChatInboxUiState
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInboxScreen(
  uiState: ChatInboxUiState,
  onBack: () -> Unit,
  onOpenBookingChat: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Booking Messages", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("chat_inbox_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
      Column(modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = ParkSpaceSpacing.screenHorizontal)) {
          ParkSpaceModeBanner(mode = AppEnvironment.currentMode)
        }

        if (uiState.threads.isEmpty() && uiState.eligibleBookings.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("chat_inbox_empty_state"),
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
                  imageVector = Icons.Outlined.ChatBubbleOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(36.dp),
                )
                Text(stringResource(R.string.empty_chat), style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Booking-scoped chat threads open automatically once you have a booking with a Host or Customer.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("chat_inbox_list"),
            contentPadding = PaddingValues(ParkSpaceSpacing.screenHorizontal, ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            if (uiState.threads.isEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.empty_chat),
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.padding(vertical = ParkSpaceSpacing.xs).testTag("chat_no_active_threads_text"),
                )
              }
            } else {
              items(uiState.threads, key = { it.bookingId }) { thread ->
                Surface(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clickable { onOpenBookingChat(thread.bookingId) }
                      .testTag("chat_thread_card_${thread.bookingId}"),
                  shape = MaterialTheme.shapes.small,
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                      Text(
                        text = "${thread.bookingCode} • ${thread.spaceTitle}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                      )
                      if (thread.unreadCount > 0) {
                        ParkSpaceStatusPill(
                          label = "${thread.unreadCount} new",
                          tone = StatusTone.AVAILABLE,
                        )
                      }
                    }
                    Text(
                      text = "${thread.lastSenderDisplayName}: ${thread.lastMessageBody}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }

            val existingThreadIds = uiState.threads.map { it.bookingId }.toSet()
            val unstarted = uiState.eligibleBookings.filter { it.first.id !in existingThreadIds }
            if (unstarted.isNotEmpty()) {
              item {
                Text(
                  text = "Start Chat on Your Bookings",
                  style = MaterialTheme.typography.labelLarge,
                  modifier = Modifier.padding(top = ParkSpaceSpacing.sm),
                )
              }
              items(unstarted, key = { "start_${it.first.id}" }) { (booking, title) ->
                OutlinedButton(
                  onClick = { onOpenBookingChat(booking.id) },
                  modifier =
                    Modifier.fillMaxWidth()
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("start_chat_booking_${booking.id}"),
                ) {
                  Text("Message on ${booking.bookingCode} ($title)")
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingChatScreen(
  bookingId: String,
  uiState: BookingChatThreadUiState,
  onBack: () -> Unit,
  onMarkThreadRead: (String) -> Unit,
  onSendMessage: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var composerText by rememberSaveable { mutableStateOf("") }

  LaunchedEffect(bookingId, uiState.messages.size) {
    if (uiState.isAuthorized) {
      onMarkThreadRead(bookingId)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().imePadding(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(uiState.booking?.bookingCode ?: "Booking Chat", style = ParkSpaceMonoCodeStyle)
            Text(
              uiState.spaceTitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("booking_chat_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
      Column(modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = ParkSpaceSpacing.screenHorizontal)) {
          ParkSpaceModeBanner(mode = AppEnvironment.currentMode)
        }

        if (!uiState.isAuthorized) {
          Box(
            modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = uiState.errorMessage ?: "Booking chat is restricted to the Customer and Host of this booking.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag("booking_chat_unauthorized_text"),
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("booking_chat_messages_list"),
            contentPadding = PaddingValues(ParkSpaceSpacing.screenHorizontal, ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            if (uiState.messages.isEmpty()) {
              item {
                Text(
                  text = "No messages in this booking yet. Coordinate gate arrival or entry questions here.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(vertical = ParkSpaceSpacing.md),
                )
              }
            } else {
              items(uiState.messages, key = { it.id }) { msg ->
                val isMine = msg.senderUserId == uiState.currentUserId
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                ) {
                  Surface(
                    modifier = Modifier.widthIn(max = 300.dp).testTag("chat_message_${msg.id}"),
                    shape = MaterialTheme.shapes.medium,
                    color =
                      if (isMine) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
                  ) {
                    Column(modifier = Modifier.padding(ParkSpaceSpacing.sm)) {
                      Text(
                        text = "${msg.senderDisplayName} (${msg.senderRole.displayName})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Text(text = msg.body, style = MaterialTheme.typography.bodyMedium)
                      Text(
                        text = formatDateTimePkt(msg.sentAtEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }

          if (uiState.errorMessage != null) {
            Text(
              text = uiState.errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(horizontal = ParkSpaceSpacing.screenHorizontal),
            )
          }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = ParkSpaceSpacing.screenHorizontal, vertical = ParkSpaceSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            OutlinedTextField(
              value = composerText,
              onValueChange = { composerText = it },
              placeholder = { Text("Message about this booking...") },
              singleLine = false,
              maxLines = 3,
              modifier = Modifier.weight(1f).testTag("booking_chat_composer_input"),
            )
            IconButton(
              onClick = {
                val trimmed = composerText.trim()
                if (trimmed.isNotEmpty()) {
                  onSendMessage(bookingId, trimmed)
                  composerText = ""
                }
              },
              modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("booking_chat_send_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Send message",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}
