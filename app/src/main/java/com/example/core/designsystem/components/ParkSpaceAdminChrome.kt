package com.example.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.feature.admin.AdminConsoleTab
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonDark
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

internal fun AdminConsoleTab.shortMobileLabel(): String =
  when (this) {
    AdminConsoleTab.KYC_QUEUE -> "KYC"
    AdminConsoleTab.LISTINGS -> "Listings"
    AdminConsoleTab.DISPUTES -> "Disputes"
    AdminConsoleTab.ANALYTICS -> "Analytics"
    AdminConsoleTab.CONFIG_AND_LOGS -> "Rules"
  }

internal fun AdminConsoleTab.navIcon(): ImageVector =
  when (this) {
    AdminConsoleTab.KYC_QUEUE -> Icons.Outlined.VerifiedUser
    AdminConsoleTab.LISTINGS -> Icons.Outlined.LocalParking
    AdminConsoleTab.DISPUTES -> Icons.Outlined.Gavel
    AdminConsoleTab.ANALYTICS -> Icons.Outlined.Analytics
    AdminConsoleTab.CONFIG_AND_LOGS -> Icons.Outlined.History
  }

@Composable
fun ParkSpaceAdminHeader(
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onBack: () -> Unit,
  onSelectTab: (AdminConsoleTab) -> Unit,
  onOpenSettings: () -> Unit,
  onSignOut: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val adminAccent = if (isDarkTheme) ErrorCrimsonDark else ErrorCrimson
  val emeraldAccent = if (isDarkTheme) Jade400 else Emerald900
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Left: Persisted Light/Dark Theme Toggle
    ParkSpaceGlassIconButton(
      onClick = onToggleTheme,
      icon = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
      contentDescription = stringResource(R.string.theme_toggle_cd),
      accentGlow = adminAccent.copy(alpha = 0.25f),
      testTag = "theme_toggle_button",
    )

    // Center: ParkSpace Brand + Private Admin Identity
    Column(
      modifier = Modifier.weight(1f).padding(horizontal = ParkSpaceSpacing.xs),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.AdminPanelSettings,
          contentDescription = null,
          tint = adminAccent,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = "Private Admin • Sandbox",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = adminAccent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // Right: Private Admin Global Menu (No Customer/Host switchers)
    Box {
      ParkSpaceGlassIconButton(
        onClick = { menuExpanded = true },
        icon = Icons.Outlined.Menu,
        contentDescription = stringResource(R.string.global_menu_cd),
        accentGlow = adminAccent.copy(alpha = 0.25f),
        testTag = "admin_menu_button",
      )

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier.testTag("admin_menu_dropdown"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
      ) {
        AdminConsoleTab.entries.forEach { tab ->
          DropdownMenuItem(
            text = {
              Text(
                text = tab.label,
                style = MaterialTheme.typography.bodyMedium,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = tab.navIcon(),
                contentDescription = null,
                tint = emeraldAccent,
              )
            },
            onClick = {
              menuExpanded = false
              onSelectTab(tab)
            },
            modifier =
              Modifier
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("admin_menu_tab_${tab.name.lowercase()}"),
          )
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 4.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )

        DropdownMenuItem(
          text = {
            Text("Profile & Settings", style = MaterialTheme.typography.bodyMedium)
          },
          leadingIcon = {
            Icon(Icons.Outlined.Settings, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onOpenSettings()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("admin_settings_button"),
        )

        DropdownMenuItem(
          text = {
            Text("Return to Welcome", style = MaterialTheme.typography.bodyMedium)
          },
          leadingIcon = {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onBack()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("admin_back_button"),
        )

        if (onSignOut != null) {
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
          )
          DropdownMenuItem(
            text = {
              Text(
                text = "Lock & Sign Out Admin",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.error,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
              )
            },
            onClick = {
              menuExpanded = false
              onSignOut()
            },
            modifier =
              Modifier
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("admin_menu_sign_out_button"),
          )
        }
      }
    }
  }
}

@Composable
fun ParkSpaceAdminBottomBar(
  selectedTab: AdminConsoleTab,
  isDarkTheme: Boolean,
  pendingKycCount: Int,
  pendingListingsCount: Int,
  openDisputesCount: Int,
  onSelectTab: (AdminConsoleTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val activeColor = if (isDarkTheme) Jade400 else Emerald900
  val badgeAccent = if (isDarkTheme) Brass400 else Brass600
  val shape = RoundedCornerShape(26.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = ParkSpaceSpacing.sm, vertical = ParkSpaceSpacing.xs),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .widthIn(max = 580.dp)
          .fillMaxWidth()
          .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = semantic.customerGlow,
            spotColor = semantic.customerGlow,
          )
          .clip(shape)
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
            shape = shape,
          )
          .testTag("admin_bottom_nav"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AdminConsoleTab.entries.forEach { tab ->
          val isSelected = selectedTab == tab
          val tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
          val badgeCount =
            when (tab) {
              AdminConsoleTab.KYC_QUEUE -> pendingKycCount
              AdminConsoleTab.LISTINGS -> pendingListingsCount
              AdminConsoleTab.DISPUTES -> openDisputesCount
              AdminConsoleTab.ANALYTICS -> 0
              AdminConsoleTab.CONFIG_AND_LOGS -> 0
            }

          Box(
            modifier =
              Modifier
                .weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .clip(RoundedCornerShape(18.dp))
                .background(
                  if (isSelected) {
                    Brush.verticalGradient(
                      colors =
                        listOf(activeColor.copy(alpha = 0.22f), activeColor.copy(alpha = 0.10f))
                    )
                  } else {
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                  }
                )
                .border(
                  width = if (isSelected) 1.dp else 0.dp,
                  color = if (isSelected) activeColor.copy(alpha = 0.42f) else Color.Transparent,
                  shape = RoundedCornerShape(18.dp),
                )
                .clickable(
                  role = Role.Tab,
                  indication = ripple(color = activeColor),
                  interactionSource = remember { MutableInteractionSource() },
                ) { onSelectTab(tab) }
                .padding(vertical = 6.dp, horizontal = 2.dp)
                .semantics { contentDescription = tab.label }
                .testTag("admin_tab_${tab.name.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              if (badgeCount > 0) {
                BadgedBox(
                  badge = {
                    Badge(
                      containerColor = badgeAccent,
                      contentColor = Color.White,
                    ) { Text("$badgeCount") }
                  }
                ) {
                  Icon(
                    imageVector = tab.navIcon(),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                  )
                }
              } else {
                Icon(
                  imageVector = tab.navIcon(),
                  contentDescription = null,
                  tint = tint,
                  modifier = Modifier.size(20.dp),
                )
              }
              Text(
                text = tab.shortMobileLabel(),
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  ),
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
