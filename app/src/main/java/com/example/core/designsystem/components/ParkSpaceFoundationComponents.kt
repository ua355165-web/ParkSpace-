package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.common.UiState
import com.example.core.config.AppMode
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

enum class StatusTone {
  AVAILABLE,
  PENDING,
  INFO,
  ERROR,
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ParkSpaceModeBanner(
  mode: AppMode,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  Surface(
    modifier = modifier.testTag("mode_banner"),
    color = semantic.glassSurface,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    shape = CircleShape,
    border = BorderStroke(1.dp, semantic.glassBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = mode.displayLabel,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = "Local Demo",
        style = ParkSpaceMonoCodeStyle,
      )
    }
  }
}

@Composable
fun ParkSpaceStatusPill(
  label: String,
  tone: StatusTone,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val (containerColor, contentColor) =
    when (tone) {
      StatusTone.AVAILABLE -> semantic.availableContainer to semantic.available
      StatusTone.PENDING -> semantic.pendingContainer to semantic.pending
      StatusTone.INFO -> semantic.infoContainer to semantic.info
      StatusTone.ERROR ->
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
    }

  Surface(
    modifier = modifier,
    color = containerColor,
    contentColor = contentColor,
    shape = MaterialTheme.shapes.extraSmall,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs, vertical = ParkSpaceSpacing.xxs),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}

@Composable
fun ParkSpacePrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  leadingIcon: ImageVector? = null,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
    shape = CircleShape,
    contentPadding =
      PaddingValues(horizontal = ParkSpaceSpacing.lg, vertical = ParkSpaceSpacing.sm),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
  ) {
    if (leadingIcon != null) {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun <T> ParkSpaceStateContainer(
  state: UiState<T>,
  onRetry: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  content: @Composable (T) -> Unit,
) {
  when (state) {
    is UiState.Loading -> {
      Box(
        modifier = modifier.fillMaxWidth().padding(ParkSpaceSpacing.xl).testTag("state_loading"),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(ParkSpaceSpacing.sm))
          Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    is UiState.Empty -> {
      ParkSpaceMessageCard(
        icon = Icons.Outlined.LocalParking,
        iconTint = MaterialTheme.colorScheme.primary,
        title = state.title,
        message = state.message,
        modifier = modifier.testTag("state_empty"),
      )
    }
    is UiState.Error -> {
      ParkSpaceMessageCard(
        icon = Icons.Outlined.ErrorOutline,
        iconTint = MaterialTheme.colorScheme.error,
        title = state.title,
        message = state.message,
        actionLabel = if (state.retryable && onRetry != null) "Try Again" else null,
        onAction = onRetry,
        modifier = modifier.testTag("state_error"),
      )
    }
    is UiState.Success -> {
      content(state.data)
    }
  }
}

@Composable
private fun ParkSpaceMessageCard(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  ParkSpaceGlassSurface(
    modifier = modifier.fillMaxWidth(),
    accentGlow = iconTint.copy(alpha = 0.2f),
    contentPaddingDp = 24,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconTint,
        modifier = Modifier.size(36.dp),
      )
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.sm))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(ParkSpaceSpacing.md))
        ParkSpaceLiquidGlassSecondaryButton(
          text = actionLabel,
          onClick = onAction,
        )
      }
    }
  }
}
