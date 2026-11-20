package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.core.domain.model.ParkingSpace
import com.example.feature.host.HostDashboardTab
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

enum class HostBottomTab(
  val label: String,
  val tag: String,
  val icon: ImageVector,
  val hostTab: HostDashboardTab?,
) {
  OVERVIEW("Overview", "host_tab_overview", Icons.Outlined.Dashboard, HostDashboardTab.OVERVIEW),
  LISTINGS("Listings", "host_tab_listings", Icons.Outlined.Apartment, HostDashboardTab.LISTINGS),
  BOOKINGS("Bookings", "host_tab_bookings", Icons.AutoMirrored.Outlined.EventNote, HostDashboardTab.BOOKINGS),
  LEDGER("Ledger", "host_tab_ledger", Icons.Outlined.AccountBalanceWallet, HostDashboardTab.LEDGER),
  PROFILE("Profile", "host_tab_profile", Icons.Outlined.PersonOutline, null),
}

@Composable
fun ParkSpaceHostBottomBar(
  selectedTab: HostBottomTab,
  isDarkTheme: Boolean,
  pendingBookingsBadge: Int = 0,
  onSelectTab: (HostBottomTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val activeColor = if (isDarkTheme) Brass400 else Brass600
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
          .widthIn(max = 580.dp)
          .fillMaxWidth()
          .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = semantic.hostGlow,
            spotColor = semantic.hostGlow,
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
          .testTag("host_bottom_nav"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HostBottomTab.entries.forEach { tab ->
          val isSelected = selectedTab == tab
          val tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

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
                  color = if (isSelected) activeColor.copy(alpha = 0.42f) else Color.Transparent,
                  shape = RoundedCornerShape(18.dp),
                )
                .clickable(
                  role = Role.Tab,
                  indication = ripple(color = activeColor),
                  interactionSource = remember { MutableInteractionSource() },
                ) { onSelectTab(tab) }
                .padding(vertical = 6.dp, horizontal = 2.dp)
                .testTag(tab.tag),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              if (tab == HostBottomTab.BOOKINGS && pendingBookingsBadge > 0) {
                BadgedBox(
                  badge = {
                    Badge { Text("$pendingBookingsBadge") }
                  }
                ) {
                  Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                  )
                }
              } else {
                Icon(
                  imageVector = tab.icon,
                  contentDescription = tab.label,
                  tint = tint,
                  modifier = Modifier.size(20.dp),
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
fun ParkSpaceHostHeader(
  hostContextLabel: String,
  isDarkTheme: Boolean,
  showDualRoleOption: Boolean,
  hasCompanionCustomerAccount: Boolean,
  onToggleTheme: () -> Unit,
  onOpenHostOverview: () -> Unit,
  onOpenCustomerDashboardOrActivate: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenHelpSupport: () -> Unit,
  onOpenPrivacyTerms: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val hostAccent = if (isDarkTheme) Brass400 else Brass600
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
      accentGlow = semantic.hostGlow,
      testTag = "theme_toggle_button",
    )

    // Center: Host Branding + Active Host Alias / City
    Column(
      modifier = Modifier.weight(1f).padding(horizontal = ParkSpaceSpacing.xs),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "Host • $hostContextLabel",
        style = MaterialTheme.typography.labelSmall,
        color = hostAccent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    // Right: Host Global Menu (Settings, Help, Privacy, Dual-Role Switch, Sign Out — NO Admin entry)
    Box {
      ParkSpaceGlassIconButton(
        onClick = { menuExpanded = true },
        icon = Icons.Outlined.Menu,
        contentDescription = stringResource(R.string.global_menu_cd),
        accentGlow = semantic.hostGlow,
        testTag = "host_menu_button",
      )

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier.testTag("host_menu_dropdown"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
      ) {
        DropdownMenuItem(
          text = {
            Text(
              text = "Host Overview",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
          },
          leadingIcon = {
            Icon(Icons.Outlined.Dashboard, contentDescription = null, tint = hostAccent)
          },
          onClick = {
            menuExpanded = false
            onOpenHostOverview()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("menu_host_dashboard"),
        )

        if (showDualRoleOption) {
          DropdownMenuItem(
            text = {
              Column {
                Text(
                  text = if (hasCompanionCustomerAccount) "Customer Dashboard" else "Activate Customer Role",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                  text =
                    if (hasCompanionCustomerAccount) "Switch to your linked Customer account"
                    else "Book parking spaces on this email",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            },
            leadingIcon = {
              Icon(
                imageVector = if (hasCompanionCustomerAccount) Icons.Outlined.SwapHoriz else Icons.Outlined.DirectionsCar,
                contentDescription = null,
                tint = if (isDarkTheme) Jade400 else Emerald900,
              )
            },
            onClick = {
              menuExpanded = false
              onOpenCustomerDashboardOrActivate()
            },
            modifier =
              Modifier
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag(
                  if (hasCompanionCustomerAccount) "menu_switch_customer_dashboard"
                  else "menu_activate_customer_role"
                ),
          )
        }

        DropdownMenuItem(
          text = { Text("Profile", style = MaterialTheme.typography.bodyMedium) },
          leadingIcon = {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null)
          },
          onClick = {
            menuExpanded = false
            onOpenProfile()
          },
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("menu_host_profile"),
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
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("menu_host_settings"),
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
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("menu_host_help_support"),
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
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("menu_host_privacy_terms"),
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
          modifier =
            Modifier
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("menu_sign_out_button"),
        )
      }
    }
  }
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostPreferencesDialog(
  initialBusinessAlias: String,
  initialOperatingCities: Set<SupportedCity>,
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  onSaveOnboarding: (String, Set<SupportedCity>) -> Unit,
  onOpenProfile: () -> Unit,
  onDismiss: () -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  var aliasInput by rememberSaveable(initialBusinessAlias) { mutableStateOf(initialBusinessAlias) }
  var selectedCities by rememberSaveable(initialOperatingCities) {
    mutableStateOf(
      if (initialOperatingCities.isEmpty()) listOf(SupportedCity.LAHORE)
      else initialOperatingCities.toList()
    )
  }
  var validationError by rememberSaveable { mutableStateOf<String?>(null) }

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
          .testTag("host_settings_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        Text("Host Settings", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
          value = aliasInput,
          onValueChange = {
            aliasInput = it
            validationError = null
          },
          label = { Text("Business Display Alias") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("host_settings_alias_input"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          Text("Operating Cities", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            SupportedCity.entries.forEach { city ->
              val selected = city in selectedCities
              FilterChip(
                selected = selected,
                onClick = {
                  selectedCities =
                    if (selected && selectedCities.size > 1) selectedCities - city
                    else if (!selected) selectedCities + city
                    else selectedCities
                },
                label = { Text(city.displayName) },
                modifier = Modifier.testTag("host_settings_city_${city.name.lowercase()}"),
              )
            }
          }
        }

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
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(ParkSpaceSpacing.xs))
          ParkSpaceLiquidGlassButton(
            text = "Save",
            onClick = {
              val trimmed = aliasInput.trim()
              if (trimmed.length < 2) {
                validationError = "Business alias must be at least 2 characters."
              } else {
                onSaveOnboarding(trimmed, selectedCities.toSet())
                onDismiss()
              }
            },
            accentColor = if (isDarkTheme) Brass400 else Brass600,
            contentColor = Color.White,
            modifier = Modifier.testTag("host_settings_save_button"),
          )
        }
      }
    }
  }
}

@Composable
fun HostHelpSupportDialog(
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
          .testTag("host_help_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text("Host Help & Support", style = MaterialTheme.typography.titleLarge)
        Text(
          text =
            "• Host Verification: Accounts begin as PENDING_REVIEW. You can build draft spaces, configure bays, and set availability while awaiting approval.\n• Bay & Blackout Conflicts: ParkSpace prevents deactivating bays or blocking dates that conflict with confirmed bookings.\n• Demo Payout Ledger: Host shares (92%) and Demo fees (8%) are computed locally from verified booking records.",
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
            text = "Open Disputes",
            onClick = {
              onDismiss()
              onOpenDisputes()
            },
            modifier = Modifier.testTag("host_help_open_disputes_button"),
          )
        }
      }
    }
  }
}

@Composable
fun HostPrivacyTermsDialog(
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
          .testTag("host_privacy_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text("Privacy & Demo Host Terms", style = MaterialTheme.typography.titleLarge)
        Text(
          text =
            "• Gate Location Privacy: Your exact entrance gate details and coordinates remain masked within ~200m in public discovery until a Customer booking is Confirmed.\n• Local Demo Mode: Account passwords stay on this device as salted PBKDF2 hashes.\n• Simulated Payouts: All Demo Ledger balances, commissions, and escrow holds are simulated locally with no external bank transfers.",
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
            modifier = Modifier.testTag("host_privacy_dialog_close_button"),
          )
        }
      }
    }
  }
}

@Composable
fun HostSelectSpaceForActionDialog(
  title: String,
  subtitle: String,
  spaces: List<ParkingSpace>,
  onSelectSpace: (ParkingSpace) -> Unit,
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
          .testTag("host_space_picker_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = semantic.glassSurfaceElevated,
      border = BorderStroke(1.dp, semantic.glassBorder),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs)) {
          spaces.forEach { space ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = semantic.glassSurface,
              border = BorderStroke(1.dp, semantic.glassBorder),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSelectSpace(space) }
                  .testTag("host_space_picker_row_${space.id}"),
            ) {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                  text = space.title,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text = "${space.location.areaLabel}, ${space.location.city.displayName} • ${space.totalBays} bays",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
          ) { Text("Cancel") }
        }
      }
    }
  }
}
