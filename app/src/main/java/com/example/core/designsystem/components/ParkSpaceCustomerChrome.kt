package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.core.config.SupportedCity
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

enum class CustomerBottomTab(
  val label: String,
  val tag: String,
  val icon: ImageVector,
) {
  DISCOVER("Discover", "customer_nav_discover", Icons.Outlined.Search),
  BOOKINGS("Bookings", "customer_nav_bookings", Icons.Outlined.EventNote),
  MESSAGES("Messages", "customer_nav_messages", Icons.Outlined.ChatBubbleOutline),
  PROFILE("Profile", "customer_nav_profile", Icons.Outlined.PersonOutline),
}

@Composable
fun ParkSpaceCustomerBottomBar(
  selectedTab: CustomerBottomTab?,
  unreadMessagesCount: Int,
  onSelectTab: (CustomerBottomTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val activeColor = MaterialTheme.colorScheme.primary
  val shape = RoundedCornerShape(26.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = ParkSpaceSpacing.md, vertical = ParkSpaceSpacing.xs),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .widthIn(max = 560.dp)
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
          .testTag("customer_bottom_nav"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CustomerBottomTab.entries.forEach { tab ->
          val isSelected = selectedTab == tab
          val tint =
            if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

          Box(
            modifier =
              Modifier
                .weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .clip(RoundedCornerShape(18.dp))
                .background(
                  if (isSelected) {
                    Brush.verticalGradient(
                      colors = listOf(activeColor.copy(alpha = 0.22f), activeColor.copy(alpha = 0.10f))
                    )
                  } else {
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                  }
                )
                .border(
                  width = if (isSelected) 1.dp else 0.dp,
                  color = if (isSelected) activeColor.copy(alpha = 0.40f) else Color.Transparent,
                  shape = RoundedCornerShape(18.dp),
                )
                .clickable(
                  role = Role.Tab,
                  indication = ripple(color = activeColor),
                  interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) { onSelectTab(tab) }
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .testTag(tab.tag),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              if (tab == CustomerBottomTab.MESSAGES && unreadMessagesCount > 0) {
                BadgedBox(
                  badge = {
                    Badge { Text("$unreadMessagesCount") }
                  }
                ) {
                  Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(21.dp),
                  )
                }
              } else {
                Icon(
                  imageVector = tab.icon,
                  contentDescription = tab.label,
                  tint = tint,
                  modifier = Modifier.size(21.dp),
                )
              }
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ParkSpaceCustomerHeader(
  cityName: String,
  isDarkTheme: Boolean,
  hasCompanionHostAccount: Boolean,
  onToggleTheme: () -> Unit,
  onOpenCustomerDashboard: () -> Unit,
  onOpenHostDashboardOrActivate: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenHelpSupport: () -> Unit,
  onOpenPrivacyTerms: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
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
      testTag = "theme_toggle_button",
    )

    // Center: Customer Workspace Title + Active City Pill
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Customer • $cityName",
        style = MaterialTheme.typography.labelSmall,
        color = if (isDarkTheme) Jade400 else Emerald900,
      )
    }

    // Right: Customer Global Menu
    Box {
      ParkSpaceGlassIconButton(
        onClick = { menuExpanded = true },
        icon = Icons.Outlined.Menu,
        contentDescription = stringResource(R.string.global_menu_cd),
        testTag = "customer_menu_button",
      )

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier.testTag("customer_menu_dropdown"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
      ) {
        DropdownMenuItem(
          text = { Text("Customer Dashboard", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)) },
          leadingIcon = {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          },
          onClick = {
            menuExpanded = false
            onOpenCustomerDashboard()
          },
          modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("menu_customer_dashboard"),
        )

        DropdownMenuItem(
          text = {
            Column {
              Text(
                text = if (hasCompanionHostAccount) "Host Dashboard" else "Activate Host Role",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              )
              Text(
                text =
                  if (hasCompanionHostAccount) "Switch to your linked Host account"
                  else "List parking spaces on this email",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          leadingIcon = {
            Icon(
              imageVector = if (hasCompanionHostAccount) Icons.Outlined.SwapHoriz else Icons.Outlined.Storefront,
              contentDescription = null,
              tint = if (isDarkTheme) Brass400 else Brass600,
            )
          },
          onClick = {
            menuExpanded = false
            onOpenHostDashboardOrActivate()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag(if (hasCompanionHostAccount) "menu_switch_host_dashboard" else "menu_activate_host_role"),
        )

        DropdownMenuItem(
          text = { Text("Profile", style = MaterialTheme.typography.bodyMedium) },
          leadingIcon = {
            Icon(Icons.Outlined.DirectionsCar, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onOpenProfile()
          },
          modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("menu_customer_profile"),
        )

        DropdownMenuItem(
          text = { Text("Settings", style = MaterialTheme.typography.bodyMedium) },
          leadingIcon = {
            Icon(Icons.Outlined.Settings, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onOpenSettings()
          },
          modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("menu_customer_settings"),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

        DropdownMenuItem(
          text = { Text("Help & Support", style = MaterialTheme.typography.bodyMedium) },
          leadingIcon = {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onOpenHelpSupport()
          },
          modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("menu_help_support"),
        )

        DropdownMenuItem(
          text = { Text("Privacy & Terms", style = MaterialTheme.typography.bodyMedium) },
          leadingIcon = {
            Icon(Icons.Outlined.PrivacyTip, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onOpenPrivacyTerms()
          },
          modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("menu_privacy_terms"),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

        DropdownMenuItem(
          text = {
            Text(
              text = "Sign Out",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.error,
            )
          },
          leadingIcon = {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          },
          onClick = {
            menuExpanded = false
            onSignOut()
          },
          modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget).testTag("menu_sign_out_button"),
        )
      }
    }
  }
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerPreferencesDialog(
  preferredCity: SupportedCity,
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  onSelectCity: (SupportedCity) -> Unit,
  onOpenProfile: () -> Unit,
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
          .testTag("customer_settings_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        Text("Customer Settings", style = MaterialTheme.typography.titleLarge)

        Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          Text("Preferred Launch City", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            SupportedCity.entries.forEach { city ->
              FilterChip(
                selected = city == preferredCity,
                onClick = { onSelectCity(city) },
                label = { Text(city.displayName) },
                modifier = Modifier.testTag("customer_pref_city_${city.name.lowercase()}"),
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = {
              onDismiss()
              onOpenProfile()
            },
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("Full Profile")
          }
          Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
          ParkSpaceLiquidGlassButton(
            text = "Done",
            onClick = onDismiss,
            modifier = Modifier.testTag("customer_settings_close_button"),
          )
        }
      }
    }
  }
}

@Composable
fun CustomerHelpSupportDialog(
  onOpenDisputes: () -> Unit,
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
          .testTag("customer_help_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text("Help & Support", style = MaterialTheme.typography.titleLarge)
        Text(
          text =
            "• Gate PIN & QR Pass: Unlocked once a booking is Confirmed in Demo mode.\n• Cancellations: Free refund before buffer window according to booking policy.\n• Booking Issues: File or review an active complaint directly in Complaints.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) { Text("Close") }
          Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
          ParkSpaceLiquidGlassButton(
            text = "Open Complaints",
            onClick = {
              onDismiss()
              onOpenDisputes()
            },
            modifier = Modifier.testTag("help_open_disputes_button"),
          )
        }
      }
    }
  }
}

@Composable
fun CustomerPrivacyTermsDialog(
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
          .testTag("customer_privacy_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text("Privacy & Demo Terms", style = MaterialTheme.typography.titleLarge)
        Text(
          text =
            "• Location Privacy: Public discovery masks exact parking coordinates within ~200m until a booking is confirmed.\n• Credential Storage: Local Demo accounts are stored strictly on this device using salted PBKDF2 hashes.\n• Demo Transactions: All payments, wallet credits, and gate passes are local simulations — no real funds are charged.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          ParkSpaceLiquidGlassButton(
            text = "Close",
            onClick = onDismiss,
            modifier = Modifier.testTag("privacy_dialog_close_button"),
          )
        }
      }
    }
  }
}
