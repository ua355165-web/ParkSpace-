package com.example.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.config.UserRole
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Jade400
import com.example.ui.theme.Jade500
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@Composable
fun ParkSpaceAtmosphericBackground(
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val topGlow = if (isDarkTheme) Jade400.copy(alpha = 0.14f) else Emerald700.copy(alpha = 0.08f)
  val sideGlow = if (isDarkTheme) Emerald700.copy(alpha = 0.16f) else Jade500.copy(alpha = 0.06f)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .background(
          brush =
            Brush.radialGradient(
              colors = listOf(topGlow, Color.Transparent),
              center = Offset(320f, 120f),
              radius = 760f,
            )
        )
        .background(
          brush =
            Brush.radialGradient(
              colors = listOf(sideGlow, semantic.customerGlow.copy(alpha = 0.04f), Color.Transparent),
              center = Offset(820f, 540f),
              radius = 680f,
            )
        ),
  ) {
    content()
  }
}

@Composable
fun ParkSpaceGlobalHeader(
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  isSignedIn: Boolean,
  activeRole: UserRole?,
  onOpenSettings: () -> Unit,
  onOpenAdminGate: () -> Unit,
  onSignOut: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Left: Theme Toggle Pill Button
    ParkSpaceGlassIconButton(
      onClick = onToggleTheme,
      icon = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
      contentDescription = stringResource(R.string.theme_toggle_cd),
      testTag = "theme_toggle_button",
    )

    // Center: ParkSpace Brand Emblem + Wordmark
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Box(
        modifier =
          Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
              Brush.linearGradient(
                colors =
                  if (isDarkTheme) listOf(Emerald700, Emerald900)
                  else listOf(Emerald900, Emerald700)
              )
            )
            .border(
              width = 1.dp,
              color = if (isDarkTheme) Jade400.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f),
              shape = RoundedCornerShape(11.dp),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.LocalParking,
          contentDescription = stringResource(R.string.app_name),
          tint = if (isDarkTheme) Jade400 else Color.White,
          modifier = Modifier.size(21.dp),
        )
      }

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // Right: Menu Anchor Button + DropdownMenu (Settings lives cleanly inside Menu)
    Box {
      ParkSpaceGlassIconButton(
        onClick = { menuExpanded = true },
        icon = Icons.Outlined.Menu,
        contentDescription = stringResource(R.string.global_menu_cd),
        testTag = "global_menu_button",
      )

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier.testTag("global_menu_dropdown"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
      ) {
        DropdownMenuItem(
          text = {
            Text(
              text = "Settings",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          },
          onClick = {
            menuExpanded = false
            onOpenSettings()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("open_settings_button"),
        )

        if (isSignedIn && onSignOut != null) {
          DropdownMenuItem(
            text = {
              val roleLabel = activeRole?.displayName ?: "Account"
              Text(
                text = "Sign Out ($roleLabel)",
                style = MaterialTheme.typography.bodyMedium,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            onClick = {
              menuExpanded = false
              onSignOut()
            },
            modifier =
              Modifier
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("menu_sign_out_button"),
          )
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 4.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )

        DropdownMenuItem(
          text = {
            Column {
              Text(
                text = "Private Admin Gate",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              )
              Text(
                text = "Local sandbox • No public signup",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
            )
          },
          onClick = {
            menuExpanded = false
            onOpenAdminGate()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("private_admin_gate_button"),
        )
      }
    }
  }
}

@Composable
fun ParkSpaceCompactDemoPill(
  modifier: Modifier = Modifier,
  label: String = "Local Demo",
) {
  val semantic = ParkSpaceThemeTokens.semantic
  Surface(
    shape = CircleShape,
    color = semantic.glassSurface,
    border = BorderStroke(1.dp, semantic.glassBorder),
    modifier = modifier,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun ParkSpaceGlassIconButton(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
  testTag: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tint: Color = MaterialTheme.colorScheme.onSurface,
  accentGlow: Color? = null,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.92f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "glass_icon_press",
  )
  val glow = accentGlow ?: semantic.customerGlow

  Box(
    modifier =
      modifier
        .size(ParkSpaceSpacing.minTouchTarget)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = if (enabled) 4.dp else 0.dp,
          shape = CircleShape,
          clip = false,
          ambientColor = glow,
          spotColor = glow,
        )
        .clip(CircleShape)
        .background(
          Brush.verticalGradient(
            colors = listOf(semantic.glassSurfaceElevated, semantic.glassSurface)
          )
        )
        .border(
          width = 1.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(semantic.glassSpecularRim, semantic.glassBorder)
            ),
          shape = CircleShape,
        ),
    contentAlignment = Alignment.Center,
  ) {
    IconButton(
      onClick = onClick,
      enabled = enabled,
      interactionSource = interactionSource,
      modifier = Modifier.fillMaxSize().testTag(testTag),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(21.dp),
      )
    }
  }
}

@Composable
fun ParkSpaceGlassSurface(
  modifier: Modifier = Modifier,
  cornerRadiusDp: Int = 22,
  accentGlow: Color? = null,
  borderColor: Color? = null,
  contentPaddingDp: Int = 18,
  content: @Composable ColumnScope.() -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val shape = RoundedCornerShape(cornerRadiusDp.dp)
  val effectiveBorder = borderColor ?: semantic.glassBorder
  val glowColor = accentGlow ?: semantic.customerGlow

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(elevation = 6.dp, shape = shape, clip = false, ambientColor = glowColor, spotColor = glowColor)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            colors = listOf(semantic.glassSurfaceElevated, semantic.glassSurface)
          )
        )
        .background(
          Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = glowColor.alpha * 0.65f), Color.Transparent),
            center = Offset(120f, 40f),
            radius = 520f,
          )
        )
        .border(
          width = 1.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(semantic.glassSpecularRim, effectiveBorder)
            ),
          shape = shape,
        ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(contentPaddingDp.dp),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      content = content,
    )
  }
}

@Composable
fun ParkSpaceLiquidGlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  leadingIcon: ImageVector? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.97f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "liquid_pill_press",
  )

  val topHighlight =
    if (enabled) accentColor else MaterialTheme.colorScheme.surfaceVariant
  val bottomFill =
    if (enabled) accentColor.copy(alpha = 0.86f) else MaterialTheme.colorScheme.surfaceVariant
  val effectiveContentColor =
    if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = 52.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = if (enabled) 6.dp else 0.dp,
          shape = CircleShape,
          clip = false,
          ambientColor = accentColor.copy(alpha = 0.45f),
          spotColor = accentColor.copy(alpha = 0.45f),
        )
        .clip(CircleShape)
        .background(
          Brush.verticalGradient(
            colors = listOf(topHighlight, bottomFill)
          )
        )
        .border(
          width = 1.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(semantic.glassSpecularRim, accentColor.copy(alpha = 0.55f))
            ),
          shape = CircleShape,
        )
        .clickable(
          enabled = enabled && !isLoading,
          role = Role.Button,
          interactionSource = interactionSource,
          indication = ripple(color = effectiveContentColor),
          onClick = onClick,
        )
        .padding(horizontal = ParkSpaceSpacing.lg, vertical = ParkSpaceSpacing.sm),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = effectiveContentColor,
        )
        Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
      } else if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = effectiveContentColor,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = effectiveContentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ParkSpaceLiquidGlassSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  leadingIcon: ImageVector? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.96f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "liquid_secondary_press",
  )
  val labelColor =
    if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = if (enabled) 3.dp else 0.dp,
          shape = CircleShape,
          clip = false,
          ambientColor = semantic.customerGlow,
          spotColor = semantic.customerGlow,
        )
        .clip(CircleShape)
        .background(
          Brush.verticalGradient(
            colors = listOf(semantic.glassSurfaceElevated, semantic.glassSurface)
          )
        )
        .border(
          width = 1.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(semantic.glassSpecularRim, accentColor.copy(alpha = 0.35f))
            ),
          shape = CircleShape,
        )
        .clickable(
          enabled = enabled,
          role = Role.Button,
          interactionSource = interactionSource,
          indication = ripple(color = accentColor),
          onClick = onClick,
        )
        .padding(horizontal = ParkSpaceSpacing.md, vertical = ParkSpaceSpacing.xs),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = if (enabled) accentColor else labelColor,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ParkSpaceLiquidGlassChip(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  enabled: Boolean = true,
  leadingIcon: ImageVector? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.95f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "liquid_chip_press",
  )

  val bgBrush =
    if (selected) {
      Brush.verticalGradient(
        colors = listOf(accentColor.copy(alpha = 0.24f), accentColor.copy(alpha = 0.14f))
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(semantic.glassSurfaceElevated, semantic.glassSurface)
      )
    }
  val borderColor = if (selected) accentColor.copy(alpha = 0.65f) else semantic.glassBorder
  val contentTint =
    when {
      !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
      selected -> accentColor
      else -> MaterialTheme.colorScheme.onSurface
    }

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(CircleShape)
        .background(bgBrush)
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(listOf(semantic.glassSpecularRim, borderColor)),
          shape = CircleShape,
        )
        .clickable(
          enabled = enabled,
          role = Role.Button,
          interactionSource = interactionSource,
          indication = ripple(color = accentColor),
          onClick = onClick,
        )
        .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = if (enabled) accentColor else contentTint,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        ),
        color = contentTint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ParkSpaceLiquidGlassSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minWidth = ParkSpaceSpacing.minTouchTarget, minHeight = ParkSpaceSpacing.minTouchTarget),
    colors =
      androidx.compose.material3.SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = accentColor,
        checkedBorderColor = accentColor.copy(alpha = 0.8f),
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = ParkSpaceThemeTokens.semantic.glassSurface,
        uncheckedBorderColor = ParkSpaceThemeTokens.semantic.glassBorder,
      ),
  )
}

@Composable
fun ParkSpaceLiquidGlassSegmentedTabs(
  tabs: List<String>,
  selectedIndex: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(CircleShape)
        .background(semantic.glassSurface)
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(listOf(semantic.glassSpecularRim, semantic.glassBorder)),
          shape = CircleShape,
        )
        .padding(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      tabs.forEachIndexed { index, label ->
        val isSelected = index == selectedIndex
        Box(
          modifier =
            Modifier
              .weight(1f)
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .clip(CircleShape)
              .background(if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent)
              .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = CircleShape,
              )
              .clickable(role = Role.Tab) { onSelectTab(index) }
              .padding(horizontal = 10.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun ParkSpaceLiquidGlassSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  androidx.compose.material3.OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = { Text(placeholder) },
    leadingIcon =
      leadingIcon?.let { icon ->
        { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
      },
    trailingIcon = trailingContent,
    singleLine = true,
    shape = RoundedCornerShape(20.dp),
    colors =
      androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedContainerColor = semantic.glassSurfaceElevated,
        unfocusedContainerColor = semantic.glassSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        unfocusedBorderColor = semantic.glassBorder,
      ),
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
fun ParkSpaceLiquidGlassRoleCard(
  title: String,
  subtitle: String,
  pillLabel: String,
  icon: ImageVector,
  isDarkTheme: Boolean,
  isPrimaryAccent: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "role_card_press",
  )

  val accentColor =
    if (isPrimaryAccent) {
      if (isDarkTheme) Jade400 else Emerald900
    } else {
      if (isDarkTheme) Brass400 else Brass600
    }
  val glowColor = if (isPrimaryAccent) semantic.customerGlow else semantic.hostGlow
  val shape = RoundedCornerShape(24.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 92.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .shadow(
          elevation = if (isDarkTheme) 10.dp else 4.dp,
          shape = shape,
          clip = false,
          ambientColor = glowColor,
          spotColor = glowColor,
        )
        .clip(shape)
        .background(
          Brush.verticalGradient(
            colors = listOf(semantic.glassSurfaceElevated, semantic.glassSurface)
          )
        )
        .background(
          Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = Offset(110f, 90f),
            radius = 500f,
          )
        )
        .border(
          width = 1.2.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(
                if (isDarkTheme) accentColor.copy(alpha = 0.55f) else semantic.glassSpecularRim,
                accentColor.copy(alpha = 0.25f),
              )
            ),
          shape = shape,
        )
        .clickable(
          role = Role.Button,
          interactionSource = interactionSource,
          indication = ripple(color = accentColor),
          onClick = onClick,
        )
        .padding(horizontal = 18.dp, vertical = 18.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
    ) {
      // Glowing Role Icon Container
      Box(
        modifier =
          Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  accentColor.copy(alpha = if (isDarkTheme) 0.24f else 0.14f),
                  accentColor.copy(alpha = if (isDarkTheme) 0.10f else 0.06f),
                )
              )
            )
            .border(
              width = 1.dp,
              color = accentColor.copy(alpha = 0.40f),
              shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(26.dp),
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f),
          ) {
            Text(
              text = pillLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = accentColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Box(
        modifier =
          Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
          contentDescription = "Open $title",
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ParkSpaceGuestSettingsDialog(
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  onDismiss: () -> Unit,
) {
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
          .fillMaxWidth()
          .testTag("guest_settings_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Preferences & Sandbox",
            style = MaterialTheme.typography.titleLarge,
          )
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
          border = BorderStroke(1.dp, semantic.glassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(ParkSpaceSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Outlined.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text =
                  "ParkSpace runs in Local Demo Mode with salted device credentials and an empty initial marketplace. Sign in as Customer or Host to access account profile settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Appearance follows your header Light/Dark mode toggle and persists across restarts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          ParkSpaceLiquidGlassButton(
            text = "Done",
            onClick = onDismiss,
            modifier = Modifier.testTag("guest_settings_close_button"),
          )
        }
      }
    }
  }
}

@Composable
fun ParkSpaceAdminSandboxGateDialog(
  acknowledged: Boolean,
  onToggleAcknowledge: (Boolean) -> Unit,
  onConfirmUnlock: () -> Unit,
  onDismiss: () -> Unit,
) {
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
          .fillMaxWidth()
          .testTag("admin_gate_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          Icon(
            imageVector = Icons.Outlined.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Text(
            text = "Private Super Admin Demo Gate",
            style = MaterialTheme.typography.titleLarge,
          )
        }

        Text(
          text =
            "Super Admin has NO public registration, NO Customer/Host access, and NO pre-created database account. Unlocking enters a private local Demo Governance Sandbox.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = semantic.glassSurface,
          border = BorderStroke(1.dp, semantic.glassBorder),
          modifier =
            Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .clip(RoundedCornerShape(14.dp))
              .clickable { onToggleAcknowledge(!acknowledged) },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = ParkSpaceSpacing.xs, vertical = ParkSpaceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            androidx.compose.material3.Checkbox(
              checked = acknowledged,
              onCheckedChange = onToggleAcknowledge,
              modifier = Modifier.testTag("admin_gate_acknowledge_checkbox"),
            )
            Text(
              text = "I acknowledge this is a private local Demo Admin Sandbox.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
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
            text = "Open Sandbox",
            enabled = acknowledged,
            accentColor = if (acknowledged) MaterialTheme.colorScheme.error else Emerald950,
            contentColor = Color.White,
            onClick = onConfirmUnlock,
            modifier = Modifier.testTag("confirm_admin_sandbox_button"),
          )
        }
      }
    }
  }
}
